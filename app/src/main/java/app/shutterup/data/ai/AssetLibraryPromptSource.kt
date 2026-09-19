package app.shutterup.data.ai

import android.content.Context
import app.shutterup.domain.ai.LibraryPrompt
import app.shutterup.domain.ai.LibraryPromptSource
import app.shutterup.domain.ai.PromptParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssetLibraryPromptSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LibraryPromptSource {
    override suspend fun loadAll(): List<LibraryPrompt> = withContext(Dispatchers.IO) {
        val json = context.assets.open("prompt_library.json").bufferedReader().use { it.readText() }
        PromptParser.parseLibrary(json).getOrThrow()
    }
}
