package com.solutions.alphil.zambiajobalerts.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedNotificationParserTest {
    @Test
    fun parsesVisibleJobNotification() {
        val payload = SharedNotificationParser.fromMap(
            mapOf(
                "job_id" to "25",
                "title" to "New opening",
                "message" to "Apply today",
                "company" to "Zambia Job Alerts",
            ),
            notificationTitle = null,
            notificationBody = null,
        )

        assertEquals("New opening", payload.title)
        assertEquals("Apply today", payload.message)
        assertEquals("25", payload.jobId)
        assertFalse(SharedNotificationParser.shouldSuppressVisibleNotification(payload))
    }

    @Test
    fun suppressesPasswordOnlyConfigUpdate() {
        val payload = SharedNotificationParser.fromMap(
            mapOf("new_wp_password" to "secret"),
            notificationTitle = null,
            notificationBody = null,
        )

        assertTrue(SharedNotificationParser.shouldSuppressVisibleNotification(payload))
    }
}
