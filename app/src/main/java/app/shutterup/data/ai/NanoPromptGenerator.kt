package app.shutterup.data.ai

import android.util.Log
import app.shutterup.data.ai.nano.NanoMonthlyOutput
import app.shutterup.data.ai.nano.NanoPromptOutput
import app.shutterup.data.ai.nano.NanoPromptText
import app.shutterup.data.ai.nano.NanoSeriesOutput
import app.shutterup.domain.ai.Availability
import app.shutterup.domain.ai.GeneratedMonthlyIssue
import app.shutterup.domain.ai.GeneratedPrompt
import app.shutterup.domain.ai.GeneratedSeries
import app.shutterup.domain.ai.GenerationRequest
import app.shutterup.domain.ai.MonthlyIssueRequest
import app.shutterup.domain.ai.PromptGenerator
import app.shutterup.domain.ai.PromptParser
import app.shutterup.domain.ai.PromptSource
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentRequest
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateTypedContentRequest
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

/** Download/progress states surfaced in onboarding and Settings → AI status. */
sealed interface NanoDownloadState {
    data object Ready : NanoDownloadState
    data object Starting : NanoDownloadState
    data class Downloading(val bytesDownloaded: Long) : NanoDownloadState
    data object Unavailable : NanoDownloadState
    data class Failed(val message: String?) : NanoDownloadState
}

/**
 * Real on-device generator over Gemini Nano (SPEC §7.2). Written against the ML Kit
 * GenAI Prompt API docs (genai-prompt 1.0.0-beta4); device-verified only on the Fold 7.
 * Always behind [PromptGenerator]; the app is fully usable without it.
 */
class NanoPromptGenerator @Inject constructor(
    private val client: GenerativeModel,
) : PromptGenerator {

    override suspend fun availability(): Availability = try {
        when (client.checkStatus()) {
            FeatureStatus.AVAILABLE -> Availability.AVAILABLE
            FeatureStatus.DOWNLOADABLE -> Availability.DOWNLOADABLE
            FeatureStatus.DOWNLOADING -> Availability.DOWNLOADING
            else -> Availability.UNAVAILABLE
        }
    } catch (e: Exception) {
        Log.w(TAG, "checkStatus failed", e)
        Availability.UNAVAILABLE
    }

    override suspend fun generate(request: GenerationRequest): Result<GeneratedPrompt> = try {
        withTimeout(GENERATION_TIMEOUT_MS) {
            val modelName = baseModelName()
            if (client.isStructuredOutputFeatureAvailable()) {
                val base = GenerateContentRequest.Builder(TextPart(NanoPromptText.systemPrompt(request))).build()
                val typed = generateTypedContentRequest(base, NanoPromptOutput::class)
                val response = client.generateContent(typed).candidates.firstOrNull()?.response
                    ?: error("Nano returned no structured candidates")
                Result.success(NanoPromptText.map(response, modelName))
            } else {
                val text = client.generateContent(NanoPromptText.systemPrompt(request) + "\nReturn ONLY the JSON object.")
                    .candidates.firstOrNull()?.text
                    ?: error("Nano returned no text")
                PromptParser.parse(text, PromptSource.ON_DEVICE_AI).map { it.copy(modelName = modelName) }
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "generate failed", e)
        Result.failure(e)
    }

    override suspend fun generateSeries(request: GenerationRequest): Result<GeneratedSeries> = try {
        withTimeout(GENERATION_TIMEOUT_MS) {
            val modelName = baseModelName()
            if (client.isStructuredOutputFeatureAvailable()) {
                val base = GenerateContentRequest.Builder(TextPart(NanoPromptText.seriesSystemPrompt(request))).build()
                val typed = generateTypedContentRequest(base, NanoSeriesOutput::class)
                val response = client.generateContent(typed).candidates.firstOrNull()?.response
                    ?: error("Nano returned no structured series candidates")
                Result.success(NanoPromptText.mapSeries(response, modelName))
            } else {
                val text = client.generateContent(
                    NanoPromptText.seriesSystemPrompt(request) + "\nReturn ONLY the JSON object.",
                ).candidates.firstOrNull()?.text
                    ?: error("Nano returned no series text")
                PromptParser.parseSeries(text, PromptSource.ON_DEVICE_AI).map { series ->
                    series.copy(prompts = series.prompts.map { it.copy(modelName = modelName) })
                }
            }
        }
        } catch (e: Exception) {
        Log.w(TAG, "generateSeries failed", e)
        Result.failure(e)
    }

    override suspend fun generateMonthlyIssue(request: MonthlyIssueRequest): Result<GeneratedMonthlyIssue> = try {
        withTimeout(GENERATION_TIMEOUT_MS) {
            val modelName = baseModelName()
            if (client.isStructuredOutputFeatureAvailable()) {
                val base = GenerateContentRequest.Builder(TextPart(NanoPromptText.monthlySystemPrompt(request))).build()
                val typed = generateTypedContentRequest(base, NanoMonthlyOutput::class)
                val response = client.generateContent(typed).candidates.firstOrNull()?.response
                    ?: error("Nano returned no structured monthly candidates")
                Result.success(NanoPromptText.mapMonthly(response, modelName))
            } else {
                val text = client.generateContent(
                    NanoPromptText.monthlySystemPrompt(request) + "\nReturn ONLY the JSON object.",
                ).candidates.firstOrNull()?.text
                    ?: error("Nano returned no monthly text")
                PromptParser.parseMonthly(text).map { it.copy(modelName = modelName) }
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "generateMonthlyIssue failed", e)
        Result.failure(e)
    }

    /** Mirrors the download flow from the get-started guide; failures stay typed, never throw. */
    fun downloadState(): Flow<NanoDownloadState> = flow {
        try {
            client.download().collect { status ->
                emit(
                    when (status) {
                        is DownloadStatus.DownloadStarted -> NanoDownloadState.Starting
                        is DownloadStatus.DownloadProgress -> NanoDownloadState.Downloading(status.totalBytesDownloaded)
                        DownloadStatus.DownloadCompleted -> NanoDownloadState.Ready
                        is DownloadStatus.DownloadFailed -> NanoDownloadState.Failed(status.e.message)
                    },
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "download failed", e)
            emit(NanoDownloadState.Failed(e.message))
        }
    }

    suspend fun baseModelName(): String? = try {
        client.getBaseModelName()
    } catch (e: Exception) {
        Log.w(TAG, "getBaseModelName failed", e)
        null
    }

    companion object {
        const val GENERATION_TIMEOUT_MS = 20_000L
        private const val TAG = "NanoPromptGenerator"

        fun defaultClient(): GenerativeModel = Generation.getClient()
    }
}
