package com.solutions.alphil.zambiajobalerts.classes

import android.text.TextUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import kotlin.math.max

class ModelFallbackGenerator {
    private val client = OkHttpClient()

    fun generateText(prompt: String, listener: GenerationListener) {
        val providers = buildProviders()
        tryProvider(0, providers, prompt, listener)
    }

    private fun tryProvider(
        index: Int,
        providers: List<ModelProviderConfig>,
        prompt: String,
        listener: GenerationListener,
    ) {
        if (index >= providers.size) {
            fallbackToPollinations(prompt, listener)
            return
        }

        val config = providers[index]
        val body = try {
            val payload = JSONObject()
            payload.put("model", config.model)

            val systemMessage = JSONObject()
            systemMessage.put("role", "system")
            systemMessage.put("content", "You are a professional CV and cover letter writing assistant.")

            val userMessage = JSONObject()
            userMessage.put("role", "user")
            userMessage.put("content", prompt)

            val messages = JSONArray()
            messages.put(systemMessage)
            messages.put(userMessage)
            payload.put("messages", messages)

            payload.toString().toRequestBody(JSON)
        } catch (error: JSONException) {
            listener.onFailure("Failed to build request: ${error.message}")
            return
        }

        val requestBuilder = Request.Builder().url(config.endpoint).post(body)
        if (!TextUtils.isEmpty(config.apiKey)) {
            requestBuilder.header("Authorization", "Bearer ${config.apiKey}")
        }

        val request = requestBuilder.build()
        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val next = "Provider ${config.name} request failed. Trying next model."
                    tryProvider(index + 1, providers, prompt, DelegateListener(listener, next))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val bodyString = it.body.string()
                        if (!it.isSuccessful) {
                            val next = "Provider ${config.name} returned ${it.code}. Trying next model."
                            tryProvider(index + 1, providers, prompt, DelegateListener(listener, next))
                            return
                        }

                        val text = parseOpenAIStyleResponse(bodyString)
                        if (text.isNullOrBlank()) {
                            val next = "Provider ${config.name} returned empty output. Trying next model."
                            tryProvider(index + 1, providers, prompt, DelegateListener(listener, next))
                            return
                        }

                        listener.onSuccess(text.trim())
                    }
                }
            },
        )
    }

    private fun fallbackToPollinations(prompt: String, listener: GenerationListener) {
        val pollinationModels = splitCsvValues(ApiConfig.TEXT_POLLINATIONS_MODELS).toMutableList()
        if (pollinationModels.isEmpty()) {
            pollinationModels.add("openai")
        }

        tryPollinations(prompt, 0, pollinationModels, listener)
    }

    private fun tryPollinations(
        prompt: String,
        index: Int,
        models: List<String>,
        listener: GenerationListener,
    ) {
        if (index >= models.size) {
            listener.onSuccess(LocalDocumentTemplateGenerator.generate(prompt))
            return
        }

        val model = models[index]
        val base = ApiConfig.TEXT_POLLINATIONS_ENDPOINT
        val encodedPrompt = encodePrompt(prompt)
        val url = if (model.trim().isEmpty()) {
            base + encodedPrompt
        } else {
            base + model.trim() + "/" + encodedPrompt
        }

        val request = Request.Builder().url(url).get().build()
        val modelName = if (model.trim().isEmpty()) "default" else model.trim()
        val nextIndex = index + 1
        client.newCall(request).enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val next = "Pollinations model $modelName request failed. Trying next model."
                    tryPollinations(prompt, nextIndex, models, DelegateListener(listener, next))
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val bodyString = it.body.string()
                        if (!it.isSuccessful) {
                            val next = "Pollinations model $modelName returned ${it.code}. Trying next model."
                            tryPollinations(prompt, nextIndex, models, DelegateListener(listener, next))
                            return
                        }

                        if (bodyString.isBlank()) {
                            val next = "Pollinations model $modelName returned empty output. Trying next model."
                            tryPollinations(prompt, nextIndex, models, DelegateListener(listener, next))
                            return
                        }

                        if (isPollinationsErrorPayload(bodyString)) {
                            val next = "Pollinations model $modelName returned error payload. Trying next model."
                            tryPollinations(prompt, nextIndex, models, DelegateListener(listener, next))
                            return
                        }

                        if (isUnusablePollinationsPayload(bodyString)) {
                            val next = "Pollinations model $modelName returned non-text response. Trying next model."
                            tryPollinations(prompt, nextIndex, models, DelegateListener(listener, next))
                            return
                        }

                        listener.onSuccess(bodyString.trim())
                    }
                }
            },
        )
    }

    private fun isPollinationsErrorPayload(raw: String?): Boolean {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.startsWith("{")) {
            try {
                val json = JSONObject(trimmed)
                return json.has("error")
            } catch (ignored: JSONException) {
                return false
            }
        }
        return trimmed.contains("Queue full for IP") || trimmed.contains("\"status\":429")
    }

    private fun isUnusablePollinationsPayload(raw: String?): Boolean {
        val trimmed = raw?.trim().orEmpty().lowercase(Locale.ROOT)
        return trimmed.startsWith("<!doctype html>") || trimmed.startsWith("<html")
    }

    private fun parseOpenAIStyleResponse(raw: String): String? =
        try {
            val json = JSONObject(raw)
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                null
            } else {
                val choice = choices.optJSONObject(0)
                val message = choice?.optJSONObject("message")
                message?.optString("content", "") ?: choice?.optString("text", "")
            }
        } catch (ignored: JSONException) {
            null
        }

    private fun buildProviders(): List<ModelProviderConfig> {
        val providers = mutableListOf<ModelProviderConfig>()

        addProvidersIfAvailable(
            providers,
            ApiConfig.PRIMARY_TEXT_MODEL_URL,
            ApiConfig.PRIMARY_TEXT_MODEL_KEY,
            ApiConfig.PRIMARY_TEXT_MODEL_NAME,
            "Primary",
        )

        addProvidersIfAvailable(
            providers,
            ApiConfig.SECONDARY_TEXT_MODEL_URL,
            ApiConfig.SECONDARY_TEXT_MODEL_KEY,
            ApiConfig.SECONDARY_TEXT_MODEL_NAME,
            "Secondary",
        )

        return providers
    }

    private fun addProvidersIfAvailable(
        providers: MutableList<ModelProviderConfig>,
        endpoints: String?,
        apiKeys: String?,
        models: String?,
        label: String,
    ) {
        val endpointList = splitCsvValues(endpoints)
        if (endpointList.isEmpty()) {
            return
        }

        val keyList = splitCsvValues(apiKeys)
        val modelList = splitCsvValues(models)
        val providerCount = max(endpointList.size, max(keyList.size, modelList.size))

        for (index in 0 until providerCount) {
            val endpoint = pickValue(endpointList, index, endpointList[endpointList.size - 1])
            if (endpoint.isEmpty()) {
                continue
            }
            val apiKey = pickValue(keyList, index, "")
            val model = pickValue(modelList, index, "gpt-3.5-turbo")
            val providerLabel = if (endpointList.size > 1 || keyList.size > 1 || modelList.size > 1) {
                "$label #${index + 1}"
            } else {
                label
            }
            providers.add(ModelProviderConfig(endpoint, apiKey, model, providerLabel))
        }
    }

    private fun splitCsvValues(raw: String?): List<String> {
        if (raw.isNullOrBlank()) {
            return emptyList()
        }

        val values = mutableListOf<String>()
        raw.trim().split(Regex("\\s*[,;\\n]\\s*")).forEach { token ->
            if (token.isNotBlank()) {
                values.add(token.trim())
            }
        }
        return values
    }

    private fun pickValue(values: List<String>?, index: Int, fallback: String): String {
        if (values.isNullOrEmpty()) {
            return fallback
        }
        if (values.size == 1) {
            return values[0]
        }
        if (index < values.size) {
            return values[index]
        }
        return fallback
    }

    private fun encodePrompt(prompt: String): String =
        try {
            URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString()).replace("+", "%20")
        } catch (error: Exception) {
            ""
        }

    fun interface GenerationListener {
        fun onSuccess(generatedText: String)
        fun onFailure(message: String) = Unit
    }

    private data class ModelProviderConfig(
        val endpoint: String,
        val apiKey: String,
        val model: String,
        val name: String,
    )

    private class DelegateListener(
        private val target: GenerationListener,
        private val nextMessage: String,
    ) : GenerationListener {
        override fun onSuccess(generatedText: String) {
            target.onSuccess(generatedText)
        }

        override fun onFailure(message: String) {
            target.onFailure("$nextMessage $message")
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaTypeOrNull()
    }
}

private object LocalDocumentTemplateGenerator {
    fun generate(prompt: String): String {
        val fields = parseFields(prompt)
        return if (prompt.lowercase(Locale.ROOT).contains("cover letter")) {
            buildCoverLetter(fields)
        } else {
            buildCv(fields)
        }
    }

    private fun parseFields(prompt: String): Map<String, String> {
        val fields = linkedMapOf<String, String>()
        prompt.lines().forEach { line ->
            val separator = line.indexOf(':')
            if (separator > 0 && separator < line.length - 1) {
                val key = line.substring(0, separator).trim().lowercase(Locale.ROOT)
                val value = clean(line.substring(separator + 1))
                if (value.isNotEmpty()) {
                    fields[key] = value
                }
            }
        }
        return fields
    }

    private fun buildCv(fields: Map<String, String>): String {
        val name = field(fields, "applicant name").ifBlank { "Applicant" }
        val email = field(fields, "email")
        val phone = field(fields, "phone")
        val role = field(fields, "write a modern cv/resume for the role", "job title", "role")
            .ifBlank { "the target role" }
        val education = field(fields, "education")
        val experience = field(fields, "work experience", "relevant experience")
        val skills = splitList(field(fields, "skills"))
        val notes = field(fields, "extra notes")
        val jobSummary = field(fields, "job summary")
        val jobLocation = field(fields, "job location")
        val contactLine = listOf(email, phone).filter { it.isNotBlank() }.joinToString(" | ")

        return buildString {
            appendLine(name)
            if (contactLine.isNotBlank()) {
                appendLine(contactLine)
            }
            appendLine()
            appendLine("Professional Summary")
            appendLine(
                "Interview-ready candidate targeting $role with a practical background aligned to the needs of the position.",
            )
            when {
                experience.isNotBlank() -> appendLine("Experience focus: $experience")
                jobSummary.isNotBlank() -> appendLine("Role focus: $jobSummary")
            }
            if (jobLocation.isNotBlank()) {
                appendLine("Preferred location: $jobLocation")
            }
            appendLine()
            appendLine("Key Skills")
            if (skills.isEmpty()) {
                appendLine("- Clear communication and professional conduct")
                appendLine("- Problem solving and reliable task execution")
                appendLine("- Team collaboration and customer-focused service")
            } else {
                skills.forEach { appendLine("- $it") }
            }
            appendLine()
            appendLine("Work Experience")
            appendLine(
                if (experience.isBlank()) {
                    "Relevant experience can be tailored further with previous employer names, dates, and measurable achievements."
                } else {
                    experience
                },
            )
            appendLine()
            appendLine("Education")
            appendLine(if (education.isBlank()) "Education details available on request." else education)
            if (notes.isNotBlank()) {
                appendLine()
                appendLine("Additional Information")
                appendLine(notes)
            }
            appendLine()
            appendLine("References")
            appendLine("Available on request.")
        }.trim()
    }

    private fun buildCoverLetter(fields: Map<String, String>): String {
        val name = field(fields, "applicant name").ifBlank { "Applicant" }
        val email = field(fields, "email")
        val phone = field(fields, "phone")
        val company = field(fields, "company", "job company").ifBlank { "your organisation" }
        val role = field(fields, "role", "job title", "write a modern cv/resume for the role")
            .ifBlank { "the advertised position" }
        val experience = field(fields, "relevant experience", "work experience")
        val notes = field(fields, "extra notes")
        val jobSummary = field(fields, "job summary")
        val tone = field(fields, "tone").replace('_', ' ').ifBlank { "professional" }
        val contactLine = listOf(email, phone).filter { it.isNotBlank() }.joinToString(" | ")

        return buildString {
            appendLine(name)
            if (contactLine.isNotBlank()) {
                appendLine(contactLine)
            }
            appendLine()
            appendLine("Dear Hiring Manager,")
            appendLine()
            appendLine(
                "I am writing to apply for the $role position at $company. I am interested in this opportunity because it matches my skills, work ethic, and commitment to delivering dependable results.",
            )
            appendLine()
            if (experience.isNotBlank()) {
                appendLine(
                    "My relevant experience includes $experience. This background has prepared me to contribute quickly, communicate clearly, and handle responsibilities with care.",
                )
            } else {
                appendLine(
                    "I bring a strong willingness to learn, a professional approach to work, and the discipline needed to contribute effectively in this role.",
                )
            }
            if (jobSummary.isNotBlank()) {
                appendLine()
                appendLine("Based on the job details, I understand that the role requires focus in areas such as $jobSummary.")
            }
            if (notes.isNotBlank()) {
                appendLine()
                appendLine(notes)
            }
            appendLine()
            appendLine(
                "I would welcome the opportunity to discuss how my background and $tone approach can support $company. Thank you for considering my application.",
            )
            appendLine()
            appendLine("Sincerely,")
            appendLine(name)
        }.trim()
    }

    private fun field(fields: Map<String, String>, vararg keys: String): String {
        keys.forEach { key ->
            val value = fields[key]?.trim()
            if (!value.isNullOrEmpty()) {
                return clean(value)
            }
        }
        return ""
    }

    private fun splitList(raw: String): List<String> =
        raw.split(Regex("\\s*[,;\\n]\\s*"))
            .map { clean(it) }
            .filter { it.isNotBlank() }

    private fun clean(value: String): String =
        value
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
