package com.solutions.alphil.zambiajobalerts.classes;

public final class ApiConfig {
    private static final String BASE_URL = "https://zambiajobalerts.com";

    public static final String WP_JOB_LISTINGS_URL = BASE_URL + "/wp-json/wp/v2/job-listings";
    public static final String LEGACY_SERVICES_URL = BASE_URL + "/system/api/services";
    public static final String LEGACY_GENERATE_TEXTS_URL = BASE_URL + "/system/api/generate-texts";

    // OpenAI-compatible model endpoints.
    // You can keep single values or comma/semicolon-separated lists.
    // Example:
    // PRIMARY_TEXT_MODEL_URL = "https://api.groq.com/openai/v1/chat/completions"
    // PRIMARY_TEXT_MODEL_NAME = "llama-3.1-8b-instant, llama-3.3-70b-versatile"
    // PRIMARY_TEXT_MODEL_KEY = "gsk_xxx"
    //
    // If you set multiple model names and a single endpoint/key, each model is tried in
    // sequence as a separate provider attempt.
    public static final String PRIMARY_TEXT_MODEL_URL = "";
    public static final String PRIMARY_TEXT_MODEL_KEY = "";
    public static final String PRIMARY_TEXT_MODEL_NAME = "";

    public static final String SECONDARY_TEXT_MODEL_URL = "";
    public static final String SECONDARY_TEXT_MODEL_KEY = "";
    public static final String SECONDARY_TEXT_MODEL_NAME = "";

    // Free-text fallback endpoint
    public static final String TEXT_POLLINATIONS_ENDPOINT = "https://text.pollinations.ai/";
    public static final String TEXT_POLLINATIONS_MODELS =
            "nova-fast, openai-fast, gemini-fast, qwen-coder, mistral, openai, deepseek, minimax, " +
            "kimi, qwen-large, mistral-large, claude-fast, gemini";

    private ApiConfig() {}
}
