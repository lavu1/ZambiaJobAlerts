package com.solutions.alphil.zambiajobalerts.shared

object SharedApiConfig {
    const val BASE_URL = "https://zambiajobalerts.com"
    const val WP_JOB_LISTINGS_URL = "$BASE_URL/wp-json/wp/v2/job-listings"
    const val LEGACY_SERVICES_URL = "$BASE_URL/system/api/services"
    const val LEGACY_GENERATE_TEXTS_URL = "$BASE_URL/system/api/generate-texts"

    const val PRIMARY_TEXT_MODEL_URL = ""
    const val PRIMARY_TEXT_MODEL_KEY = ""
    const val PRIMARY_TEXT_MODEL_NAME = ""

    const val SECONDARY_TEXT_MODEL_URL = ""
    const val SECONDARY_TEXT_MODEL_KEY = ""
    const val SECONDARY_TEXT_MODEL_NAME = ""

    const val TEXT_POLLINATIONS_ENDPOINT = "https://text.pollinations.ai/"
    const val TEXT_POLLINATIONS_MODELS =
        "nova-fast, openai-fast, gemini-fast, qwen-coder, mistral, openai, deepseek, minimax, " +
            "kimi, qwen-large, mistral-large, claude-fast, gemini"
}
