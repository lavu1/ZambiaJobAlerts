package com.solutions.alphil.zambiajobalerts

import com.solutions.alphil.zambiajobalerts.shared.SharedJobLaunchParser

object JobLaunchParser {
    @JvmStatic
    fun extractIdentifier(scheme: String?, host: String?, path: String?): String? =
        SharedJobLaunchParser.extractIdentifier(scheme, host, path)

    @JvmStatic
    fun isHomeUri(scheme: String?, host: String?, path: String?): Boolean =
        SharedJobLaunchParser.isHomeUri(scheme, host, path)

    @JvmStatic
    fun buildJobDetailsBySlugUrl(baseUrl: String, slug: String): String =
        SharedJobLaunchParser.buildJobDetailsBySlugUrl(baseUrl, slug)
}
