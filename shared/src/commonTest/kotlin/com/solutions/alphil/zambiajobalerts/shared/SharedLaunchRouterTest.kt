package com.solutions.alphil.zambiajobalerts.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedLaunchRouterTest {
    @Test
    fun parsesSupportedWebJobLink() {
        val request = SharedLaunchRouter.parseUri(
            scheme = "https",
            host = "zambiajobalerts.com",
            path = "/job/accounts-assistant/",
            openedFromDeepLink = true,
        )

        assertNotNull(request)
        assertEquals(SharedLaunchDestination.JOB, request.destination)
        assertEquals("accounts-assistant", request.identifier)
        assertTrue(request.openedFromDeepLink)
    }

    @Test
    fun parsesCustomSchemeHome() {
        val request = SharedLaunchRouter.parseUri(
            scheme = "zambiajobalerts",
            host = "home",
            path = "",
            openedFromDeepLink = true,
        )

        assertNotNull(request)
        assertEquals(SharedLaunchDestination.HOME, request.destination)
    }

    @Test
    fun rejectsUnsupportedHosts() {
        assertNull(
            SharedLaunchRouter.parseUri(
                scheme = "https",
                host = "example.com",
                path = "/job/accounts-assistant/",
                openedFromDeepLink = true,
            ),
        )
    }

    @Test
    fun notificationSlugWinsOverId() {
        val request = SharedLaunchRouter.parseNotificationLaunch(
            jobId = "123",
            jobSlug = "job-slug",
            deepLink = null,
        )

        assertNotNull(request)
        assertEquals("job-slug", request.identifier)
        assertFalse(request.openedFromDeepLink)
    }

    @Test
    fun notificationLinkParsesBareDomain() {
        val request = SharedLaunchRouter.parseNotificationLaunch(
            jobId = null,
            jobSlug = null,
            deepLink = "zambiajobalerts.com/job/job-slug/",
        )

        assertNotNull(request)
        assertEquals("job-slug", request.identifier)
        assertTrue(request.openedFromDeepLink)
    }
}
