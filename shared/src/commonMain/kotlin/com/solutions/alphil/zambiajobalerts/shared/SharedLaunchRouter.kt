package com.solutions.alphil.zambiajobalerts.shared

object SharedLaunchRouter {
    fun parseUri(
        scheme: String?,
        host: String?,
        path: String?,
        openedFromDeepLink: Boolean,
    ): SharedLaunchRequest? {
        if (SharedJobLaunchParser.isHomeUri(scheme, host, path)) {
            return SharedLaunchRequest.forHome()
        }

        val identifier = SharedJobLaunchParser.extractIdentifier(scheme, host, path)
        return if (identifier.isNullOrEmpty()) {
            null
        } else {
            SharedLaunchRequest.forJob(identifier, openedFromDeepLink)
        }
    }

    fun parseNotificationLaunch(
        jobId: String?,
        jobSlug: String?,
        deepLink: String?,
    ): SharedLaunchRequest? {
        val slug = jobSlug.normalizedValue()
        if (slug != null) return SharedLaunchRequest.forJob(slug, false)

        val identifier = jobId.normalizedValue()
        if (identifier != null && identifier != "-1") {
            return SharedLaunchRequest.forJob(identifier, false)
        }

        val link = deepLink.normalizedValue()
        if (link != null) {
            val parts = parseUrlParts(normalizeLaunchUrl(link))
            val request = parseUri(parts.scheme, parts.host, parts.path, true)
            if (request != null) return request
        }

        return null
    }

    fun normalizeLaunchUrl(value: String): String {
        val trimmedValue = value.trim()
        return if (trimmedValue.startsWith("zambiajobalerts.com") ||
            trimmedValue.startsWith("www.zambiajobalerts.com")
        ) {
            "https://$trimmedValue"
        } else {
            trimmedValue
        }
    }

    private fun parseUrlParts(url: String): UrlParts {
        val schemeSeparator = url.indexOf("://")
        if (schemeSeparator < 0) {
            val customSchemeSeparator = url.indexOf(":")
            if (customSchemeSeparator < 0) return UrlParts(null, null, url)

            val scheme = url.substring(0, customSchemeSeparator)
            val remainder = url.substring(customSchemeSeparator + 1)
            val hostAndPath = remainder.removePrefix("//")
            return splitHostAndPath(scheme, hostAndPath)
        }

        val scheme = url.substring(0, schemeSeparator)
        val hostAndPath = url.substring(schemeSeparator + 3)
        return splitHostAndPath(scheme, hostAndPath)
    }

    private fun splitHostAndPath(scheme: String, hostAndPath: String): UrlParts {
        val pathStart = hostAndPath.indexOf("/")
        if (pathStart < 0) return UrlParts(scheme, hostAndPath, "")

        val host = hostAndPath.substring(0, pathStart)
        val path = hostAndPath.substring(pathStart)
        return UrlParts(scheme, host, path)
    }

    private fun String?.normalizedValue(): String? {
        val value = this?.trim().orEmpty()
        return value.ifEmpty { null }
    }

    private data class UrlParts(
        val scheme: String?,
        val host: String?,
        val path: String?,
    )
}
