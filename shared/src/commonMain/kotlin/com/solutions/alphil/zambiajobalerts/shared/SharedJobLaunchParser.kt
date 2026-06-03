package com.solutions.alphil.zambiajobalerts.shared

object SharedJobLaunchParser {
    fun extractIdentifier(scheme: String?, host: String?, path: String?): String? {
        if (path.isNullOrBlank()) return null

        val normalizedScheme = scheme.normalizedPart()
        val normalizedHost = host.normalizedPart()

        if (normalizedScheme == "zambiajobalerts") {
            if (normalizedHost != "job" && normalizedHost != "jobs") return null
            return normalizeIdentifier(path)
        }

        if (!isWebScheme(normalizedScheme) || !isSupportedWebHost(normalizedHost)) return null

        return when {
            path.startsWith("/job/") -> normalizeIdentifier(path.removePrefix("/job/"))
            path.startsWith("/jobs/") -> normalizeIdentifier(path.removePrefix("/jobs/"))
            else -> null
        }
    }

    fun isHomeUri(scheme: String?, host: String?, path: String?): Boolean {
        val normalizedScheme = scheme.normalizedPart()
        val normalizedHost = host.normalizedPart()
        val normalizedPath = path?.trim().orEmpty()

        if (normalizedScheme == "zambiajobalerts") {
            return normalizedHost == "home"
        }

        return isWebScheme(normalizedScheme) &&
            isSupportedWebHost(normalizedHost) &&
            (normalizedPath.isEmpty() || normalizedPath == "/")
    }

    fun buildJobDetailsBySlugUrl(baseUrl: String, slug: String): String =
        "$baseUrl?slug=${encodeQueryValue(slug)}&_embed=1"

    fun slugFromPath(path: String?): String? {
        val parts = path.orEmpty()
            .split("/")
            .filter { it.isNotBlank() }
        if (parts.size < 2 || parts[0] != "job") return null
        return parts[1]
    }

    private fun normalizeIdentifier(rawValue: String?): String? {
        var value = rawValue?.trim().orEmpty()
        while (value.startsWith("/")) value = value.drop(1)
        while (value.endsWith("/")) value = value.dropLast(1)

        val nextSlash = value.indexOf("/")
        if (nextSlash >= 0) {
            value = value.substring(0, nextSlash)
        }

        return value.takeIf { it.isNotEmpty() }
    }

    private fun encodeQueryValue(value: String): String = buildString {
        for (byte in value.encodeToByteArray()) {
            val code = byte.toInt() and 0xff
            val char = code.toChar()
            if (char.isUnreservedUrlChar()) {
                append(char)
            } else if (char == ' ') {
                append('+')
            } else {
                append('%')
                append(code.toString(16).uppercase().padStart(2, '0'))
            }
        }
    }

    private fun String?.normalizedPart(): String = this?.trim()?.lowercase().orEmpty()

    private fun isWebScheme(scheme: String): Boolean = scheme == "http" || scheme == "https"

    private fun isSupportedWebHost(host: String): Boolean =
        host == "zambiajobalerts.com" || host == "www.zambiajobalerts.com"

    private fun Char.isUnreservedUrlChar(): Boolean =
        this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '-' || this == '_' || this == '.' || this == '~'
}
