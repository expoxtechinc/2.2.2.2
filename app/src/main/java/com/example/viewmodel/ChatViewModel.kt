package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.ChatMessage
import com.example.data.Content
import com.example.data.GenerateContentRequest
import com.example.data.MessageSender
import com.example.data.Part
import com.example.data.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Creator Metadata
    val appLogoUrl = "https://www.image2url.com/r2/default/images/1779709348867-d16338fb-19e0-4e1c-8909-d2c8128a2d55.png"
    val creatorPictureUrl = "https://www.image2url.com/r2/default/images/1779709278276-997a686a-5f7d-46d7-993a-0d829a1c9488.png"
    val creatorName = "Akin S. Sokpah"
    val creatorTitle = "Founder & Lead Architect"
    val creatorBio = "Akin S. Sokpah is a visionary software developer, entrepreneur, and product architect. Driven by a deep mission of digital craftsmanship, Akin created AkinAI as an emblem of polished design and helpful intelligence. He aims to make modern artificial intelligence accessible through beautiful, natural interfaces that respect design guidelines and user-centric architecture."

    init {
        // Welcome message
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "Hello! I am AkinAI, your personal conversational AI assistant designed and created by Akin S. Sokpah. Ask me anything, or query me about my creator to view his profile!",
                showCreatorProfile = false
            )
        )
    }

    fun onInputTextChange(text: String) {
        _inputText.value = text
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCustomApiKeyChange(key: String) {
        _customApiKey.value = key
    }

    fun toggleSettings(show: Boolean) {
        _showSettings.value = show
    }

    fun getEffectiveApiKey(): String {
        val userKey = _customApiKey.value.trim()
        if (userKey.isNotEmpty()) return userKey

        // Get from BuildConfig
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotEmpty() && !buildKey.contains("MY_GEMINI_API_KEY")) {
            return buildKey
        }
        return ""
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                sender = MessageSender.AI,
                text = "Chat cleared! How can AkinAI assist you today? Let me know if you would like to read more about my founder, Akin S. Sokpah.",
                showCreatorProfile = false
            )
        )
    }

    fun sendMessage() {
        val prompt = _inputText.value.trim()
        if (prompt.isEmpty()) return

        // Clear input box
        _inputText.value = ""

        // Add user message to state
        val userMsg = ChatMessage(sender = MessageSender.USER, text = prompt)
        _messages.update { it + userMsg }

        // Start request
        _isLoading.value = true

        // Append pending message for AI
        val pendingAiMsgId = java.util.UUID.randomUUID().toString()
        val pendingAiMsg = ChatMessage(
            id = pendingAiMsgId,
            sender = MessageSender.AI,
            text = "Thinking...",
            isPending = true
        )
        _messages.update { it + pendingAiMsg }

        viewModelScope.launch {
            try {
                val apiKey = getEffectiveApiKey()
                if (apiKey.isEmpty()) {
                    // Fail gracefully - tell user to set the key, or use offline mode answers about Akin S. Sokpah!
                    val responseText = if (isQueryAboutClockOrAkin(prompt)) {
                        generateOfflineAkinResponse(prompt)
                    } else {
                        "API Key missing: Please tap the Settings Cog icon at the top right to furnish your Gemini API Key or configure the AI Studio secrets with VITE_GEMINI_API_KEY!"
                    }
                    val belongsAboutCreator = promptContainsCreatorKeywords(prompt)

                    _messages.update { list ->
                        list.map { msg ->
                            if (msg.id == pendingAiMsgId) {
                                msg.copy(
                                    text = responseText,
                                    isPending = false,
                                    isError = apiKey.isEmpty() && !isQueryAboutClockOrAkin(prompt),
                                    showCreatorProfile = belongsAboutCreator
                                )
                            } else msg
                        }
                    }
                    _isLoading.value = false
                    return@launch
                }

                // Check if we should inject Akin S. Sokpah card inside chat timeline
                val belongsAboutCreator = promptContainsCreatorKeywords(prompt)

                // Populate history for standard model interaction
                val apiContents = _messages.value
                    .filter { !it.isPending && !it.isError }
                    .takeLast(10) // Conversational bounds
                    .map { msg ->
                        Content(
                            role = if (msg.sender == MessageSender.USER) "user" else "model",
                            parts = listOf(Part(text = msg.text))
                        )
                    }

                val systemPrompt = """
                    You are AkinAI, a beautiful and powerful conversational AI assistant named after and built by Akin S. Sokpah.
                    Your name is AkinAI. You are helpful, insightful, precise, and polite.
                    Whenever anyone asks about your creator, maker, founder, designer, or who built/developed you, you must proudly tell them that you were created by Akin S. Sokpah! Describe Akin as a highly skilled software engineer and tech entrepreneur based in West Africa who is extremely passionate about beautiful designs, artificial intelligence, and mobile apps. Let them know they can click the custom Creator Profile card displayed directly in the chat timeline to see his real picture and learn more about him.
                    Keep your design-focused and engineering credentials in mind. Always be professional, crisp, and beautifully expressive.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = apiContents,
                    systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
                )

                val response = RetrofitClient.geminiService.generateContent(apiKey, request)
                val aiText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: response.error?.let { "Gemini Error: ${it.message}" }
                    ?: "AkinAI did not produce an answer, please try asking again!"

                _messages.update { list ->
                    list.map { msg ->
                        if (msg.id == pendingAiMsgId) {
                            msg.copy(
                                text = aiText,
                                isPending = false,
                                showCreatorProfile = belongsAboutCreator
                            )
                        } else msg
                    }
                }

            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error fetching from Gemini", e)
                val isAkinQuery = isQueryAboutClockOrAkin(prompt)
                val responseText = if (isAkinQuery) {
                    generateOfflineAkinResponse(prompt)
                } else {
                    "Sorry, I ran into an connection error. ${e.localizedMessage ?: "Please ensure your API Key is correct and verify internet status."}"
                }

                _messages.update { list ->
                    list.map { msg ->
                        if (msg.id == pendingAiMsgId) {
                            msg.copy(
                                text = responseText,
                                isPending = false,
                                isError = !isAkinQuery,
                                showCreatorProfile = promptContainsCreatorKeywords(prompt)
                            )
                        } else msg
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun promptContainsCreatorKeywords(prompt: String): Boolean {
        val lowercase = prompt.lowercase()
        return lowercase.contains("creator") || 
               lowercase.contains("akin") || 
               lowercase.contains("sokpah") || 
               lowercase.contains("who made") || 
               lowercase.contains("who built") || 
               lowercase.contains("who designed") || 
               lowercase.contains("who created") ||
               lowercase.contains("founder") ||
               lowercase.contains("owner")
    }

    private fun isQueryAboutClockOrAkin(prompt: String): Boolean {
        return promptContainsCreatorKeywords(prompt)
    }

    private fun generateOfflineAkinResponse(prompt: String): String {
        return "AkinAI is proudly named after and created by Akin S. Sokpah. Akin is a visionary software engineer and tech entrepreneur. He designed this chat ecosystem with extraordinary craftsmanship to unite beautiful designs with high-performance artificial intelligence. You can view his photo and complete details here in our creator window!"
    }
}
