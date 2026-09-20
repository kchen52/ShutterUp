package app.shutterup.ui.generation

import androidx.lifecycle.ViewModel
import app.shutterup.domain.ai.GeneratePromptUseCase
import app.shutterup.domain.ai.GenerationProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class PromptGenerationViewModel @Inject constructor(
    generatePrompt: GeneratePromptUseCase,
) : ViewModel() {
    val progress: StateFlow<GenerationProgress?> = generatePrompt.generation
}
