package com.solutions.alphil.zambiajobalerts.classes;

import android.text.TextUtils;

import com.solutions.alphil.zambiajobalerts.classes.ApiConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ModelFallbackGenerator {

    public interface GenerationListener {
        void onSuccess(String generatedText);
        void onFailure(String message);
    }

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();

    public void generateText(String prompt, GenerationListener listener) {
        List<ModelProviderConfig> providers = buildProviders();
        tryProvider(0, providers, prompt, listener);
    }

    private void tryProvider(int index, List<ModelProviderConfig> providers, String prompt,
                             GenerationListener listener) {
        if (index >= providers.size()) {
            fallbackToPollinations(prompt, listener);
            return;
        }

        ModelProviderConfig config = providers.get(index);
        RequestBody body;

        try {
            JSONObject payload = new JSONObject();
            payload.put("model", config.model);

            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a professional CV and cover letter writing assistant.");

            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            JSONArray messages = new JSONArray();
            messages.put(systemMessage);
            messages.put(userMessage);
            payload.put("messages", messages);

            body = RequestBody.create(payload.toString(), JSON);
        } catch (JSONException e) {
            listener.onFailure("Failed to build request: " + e.getMessage());
            return;
        }

        Request.Builder requestBuilder = new Request.Builder().url(config.endpoint).post(body);
        if (!TextUtils.isEmpty(config.apiKey)) {
            requestBuilder.header("Authorization", "Bearer " + config.apiKey);
        }

        Request request = requestBuilder.build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String next = "Provider " + config.name + " request failed. Trying next model.";
                tryProvider(index + 1, providers, prompt, new DelegateListener(listener, next));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bodyString = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    String next = "Provider " + config.name + " returned " + response.code() + ". Trying next model.";
                    tryProvider(index + 1, providers, prompt, new DelegateListener(listener, next));
                    return;
                }

                String text = parseOpenAIStyleResponse(bodyString);
                if (text == null || text.trim().isEmpty()) {
                    String next = "Provider " + config.name + " returned empty output. Trying next model.";
                    tryProvider(index + 1, providers, prompt, new DelegateListener(listener, next));
                    return;
                }

                listener.onSuccess(text.trim());
            }
        });
    }

    private void fallbackToPollinations(String prompt, GenerationListener listener) {
        List<String> pollinationModels = splitCsvValues(ApiConfig.TEXT_POLLINATIONS_MODELS);
        if (pollinationModels.isEmpty()) {
            pollinationModels.add("openai");
        }

        tryPollinations(prompt, 0, pollinationModels, listener);
    }

    private void tryPollinations(String prompt, int index, List<String> models,
                                GenerationListener listener) {
        if (index >= models.size()) {
            listener.onFailure("Pollinations fallback failed for all available models.");
            return;
        }

        String model = models.get(index);
        String base = ApiConfig.TEXT_POLLINATIONS_ENDPOINT;
        String encodedPrompt = encodePrompt(prompt);
        String url = model == null || model.trim().isEmpty()
                ? base + encodedPrompt
                : base + model.trim() + "/" + encodedPrompt;

        Request request = new Request.Builder().url(url).get().build();
        final String modelName = model == null || model.trim().isEmpty() ? "default" : model.trim();
        final int nextIndex = index + 1;
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                String next = "Pollinations model " + modelName + " request failed. Trying next model.";
                tryPollinations(prompt, nextIndex, models, new DelegateListener(listener, next));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bodyString = response.body() != null ? response.body().string() : "";
                if (!response.isSuccessful()) {
                    String next = "Pollinations model " + modelName + " returned " + response.code() + ". Trying next model.";
                    tryPollinations(prompt, nextIndex, models, new DelegateListener(listener, next));
                    return;
                }

                if (bodyString == null || bodyString.trim().isEmpty()) {
                    String next = "Pollinations model " + modelName + " returned empty output. Trying next model.";
                    tryPollinations(prompt, nextIndex, models, new DelegateListener(listener, next));
                    return;
                }

                if (isPollinationsErrorPayload(bodyString)) {
                    String next = "Pollinations model " + modelName + " returned error payload. Trying next model.";
                    tryPollinations(prompt, nextIndex, models, new DelegateListener(listener, next));
                    return;
                }

                if (isUnusablePollinationsPayload(bodyString)) {
                    String next = "Pollinations model " + modelName + " returned non-text response. Trying next model.";
                    tryPollinations(prompt, nextIndex, models, new DelegateListener(listener, next));
                    return;
                }

                listener.onSuccess(bodyString.trim());
            }
        });
    }

    private boolean isPollinationsErrorPayload(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(trimmed);
                return json.has("error");
            } catch (JSONException ignored) {
                return false;
            }
        }
        if (trimmed.contains("Queue full for IP") || trimmed.contains("\"status\":429")) {
            return true;
        }
        return false;
    }

    private boolean isUnusablePollinationsPayload(String raw) {
        String trimmed = raw == null ? "" : raw.trim().toLowerCase(java.util.Locale.ROOT);
        return trimmed.startsWith("<!doctype html>") || trimmed.startsWith("<html");
    }

    private String parseOpenAIStyleResponse(String raw) {
        try {
            JSONObject json = new JSONObject(raw);
            JSONArray choices = json.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return null;
            }

            JSONObject choice = choices.optJSONObject(0);
            if (choice == null) {
                return null;
            }

            JSONObject message = choice.optJSONObject("message");
            if (message != null) {
                return message.optString("content", "");
            }

            return choice.optString("text", "");
        } catch (JSONException ignored) {
            return null;
        }
    }

    private List<ModelProviderConfig> buildProviders() {
        List<ModelProviderConfig> providers = new ArrayList<>();

        addProvidersIfAvailable(providers,
                ApiConfig.PRIMARY_TEXT_MODEL_URL,
                ApiConfig.PRIMARY_TEXT_MODEL_KEY,
                ApiConfig.PRIMARY_TEXT_MODEL_NAME,
                "Primary");

        addProvidersIfAvailable(providers,
                ApiConfig.SECONDARY_TEXT_MODEL_URL,
                ApiConfig.SECONDARY_TEXT_MODEL_KEY,
                ApiConfig.SECONDARY_TEXT_MODEL_NAME,
                "Secondary");

        return providers;
    }

    private void addProvidersIfAvailable(List<ModelProviderConfig> providers,
                                        String endpoints,
                                        String apiKeys,
                                        String models,
                                        String label) {
        List<String> endpointList = splitCsvValues(endpoints);
        if (endpointList.isEmpty()) {
            return;
        }

        List<String> keyList = splitCsvValues(apiKeys);
        List<String> modelList = splitCsvValues(models);
        int providerCount = Math.max(endpointList.size(),
                Math.max(keyList.size(), modelList.size()));

        for (int i = 0; i < providerCount; i++) {
            String endpoint = pickValue(endpointList, i, endpointList.get(endpointList.size() - 1));
            if (endpoint.isEmpty()) {
                continue;
            }
            String apiKey = pickValue(keyList, i, "");
            String model = pickValue(modelList, i, "gpt-3.5-turbo");
            String providerLabel = endpointList.size() > 1 || keyList.size() > 1 || modelList.size() > 1
                    ? label + " #" + (i + 1)
                    : label;
            providers.add(new ModelProviderConfig(endpoint, apiKey, model, providerLabel));
        }
    }

    private List<String> splitCsvValues(String raw) {
        List<String> values = new ArrayList<>();
        if (raw == null || raw.trim().isEmpty()) {
            return values;
        }

        String[] tokens = raw.trim().split("\\s*[,;\\n]\\s*");
        for (String token : tokens) {
            if (token == null || token.trim().isEmpty()) {
                continue;
            }
            values.add(token.trim());
        }
        return values;
    }

    private String pickValue(List<String> values, int index, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }

        if (values.size() == 1) {
            return values.get(0);
        }

        if (index < values.size()) {
            return values.get(index);
        }

        return fallback;
    }

    private String encodePrompt(String prompt) {
        try {
            return URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static class ModelProviderConfig {
        private final String endpoint;
        private final String apiKey;
        private final String model;
        private final String name;

        private ModelProviderConfig(String endpoint, String apiKey, String model, String name) {
            this.endpoint = endpoint;
            this.apiKey = apiKey;
            this.model = model;
            this.name = name;
        }
    }

    private static class DelegateListener implements GenerationListener {
        private final GenerationListener target;
        private final String nextMessage;

        private DelegateListener(GenerationListener target, String nextMessage) {
            this.target = target;
            this.nextMessage = nextMessage;
        }

        @Override
        public void onSuccess(String generatedText) {
            target.onSuccess(generatedText);
        }

        @Override
        public void onFailure(String message) {
            target.onFailure(nextMessage + " " + message);
        }
    }
}
