package app.shutterup.domain.ai

interface LibraryPromptSource {
    suspend fun loadAll(): List<LibraryPrompt>
}
