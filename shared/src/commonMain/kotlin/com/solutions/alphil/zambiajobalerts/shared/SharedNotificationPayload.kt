package com.solutions.alphil.zambiajobalerts.shared

data class SharedNotificationPayload(
    val title: String,
    val message: String,
    val jobId: String? = null,
    val jobSlug: String? = null,
    val type: String? = null,
    val company: String? = null,
    val location: String? = null,
    val link: String? = null,
    val newWpPassword: String? = null,
)

object SharedNotificationParser {
    fun fromMap(
        data: Map<String, String>,
        notificationTitle: String?,
        notificationBody: String?,
    ): SharedNotificationPayload =
        fromValues(
            title = firstNonEmpty(data["title"], notificationTitle) ?: "New Job Alert!",
            message = firstNonEmpty(data["message"], notificationBody) ?: "Tap to view job details",
            jobId = data["job_id"],
            jobSlug = data["job_slug"],
            type = data["type"],
            company = data["company"],
            location = data["location"],
            link = firstNonEmpty(data["deep_link"], data["deeplink"], data["link"], data["url"]),
            newWpPassword = data["new_wp_password"],
        )

    fun fromValues(
        title: String?,
        message: String?,
        jobId: String?,
        jobSlug: String?,
        type: String?,
        company: String?,
        location: String?,
        link: String?,
        newWpPassword: String?,
    ): SharedNotificationPayload =
        SharedNotificationPayload(
            title = title.normalizedValue() ?: "New Job Alert!",
            message = message.normalizedValue() ?: "Tap to view job details",
            jobId = jobId.normalizedValue(),
            jobSlug = jobSlug.normalizedValue(),
            type = type.normalizedValue(),
            company = company.normalizedValue(),
            location = location.normalizedValue(),
            link = link.normalizedValue(),
            newWpPassword = newWpPassword.normalizedValue(),
        )

    fun launchRequest(payload: SharedNotificationPayload): SharedLaunchRequest =
        SharedLaunchRouter.parseNotificationLaunch(
            payload.jobId,
            payload.jobSlug,
            payload.link,
        ) ?: SharedLaunchRequest.forHome()

    fun shouldSuppressVisibleNotification(payload: SharedNotificationPayload): Boolean =
        payload.newWpPassword != null &&
            payload.title == "New Job Alert!" &&
            payload.message == "Tap to view job details" &&
            payload.jobId == null &&
            payload.jobSlug == null &&
            payload.link == null

    private fun firstNonEmpty(vararg values: String?): String? =
        values.firstNotNullOfOrNull { it.normalizedValue() }

    private fun String?.normalizedValue(): String? {
        val value = this?.trim().orEmpty()
        return value.ifEmpty { null }
    }
}
