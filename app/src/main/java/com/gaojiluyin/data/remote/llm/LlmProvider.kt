package com.gaojiluyin.data.remote.llm

enum class ApiFormat { OPENAI_COMPATIBLE, CLAUDE }

data class LlmProvider(
    val id: String,
    val name: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val apiFormat: ApiFormat,
    val requiresKey: Boolean = true
)

object LlmProviders {
    val ALL = listOf(
        LlmProvider(
            id = "deepseek",
            name = "DeepSeek",
            defaultBaseUrl = "https://api.deepseek.com/v1",
            defaultModel = "deepseek-chat",
            apiFormat = ApiFormat.OPENAI_COMPATIBLE
        ),
        LlmProvider(
            id = "qwen",
            name = "通义千问",
            defaultBaseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
            defaultModel = "qwen-turbo",
            apiFormat = ApiFormat.OPENAI_COMPATIBLE
        ),
        LlmProvider(
            id = "minimax",
            name = "MiniMax",
            defaultBaseUrl = "https://api.minimax.chat/v1",
            defaultModel = "abab6.5s-chat",
            apiFormat = ApiFormat.OPENAI_COMPATIBLE
        ),
        LlmProvider(
            id = "mimo",
            name = "MiMo (小米)",
            defaultBaseUrl = "https://api.mimo.xiaomi.com/v1",
            defaultModel = "mimo-7b",
            apiFormat = ApiFormat.OPENAI_COMPATIBLE
        ),
        LlmProvider(
            id = "gpt",
            name = "OpenAI GPT",
            defaultBaseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-4o-mini",
            apiFormat = ApiFormat.OPENAI_COMPATIBLE
        ),
        LlmProvider(
            id = "claude",
            name = "Claude",
            defaultBaseUrl = "https://api.anthropic.com/v1",
            defaultModel = "claude-sonnet-4-20250514",
            apiFormat = ApiFormat.CLAUDE
        )
    )

    fun getById(id: String): LlmProvider? = ALL.find { it.id == id }

    fun getDefault(): LlmProvider = ALL.first()
}
