package com.solutions.alphil.zambiajobalerts.shared

enum class SharedLaunchDestination {
    HOME,
    JOB,
}

data class SharedLaunchRequest(
    val destination: SharedLaunchDestination,
    val identifier: String? = null,
    val openedFromDeepLink: Boolean = false,
) {
    companion object {
        fun forHome(): SharedLaunchRequest =
            SharedLaunchRequest(SharedLaunchDestination.HOME)

        fun forJob(identifier: String, openedFromDeepLink: Boolean): SharedLaunchRequest =
            SharedLaunchRequest(SharedLaunchDestination.JOB, identifier, openedFromDeepLink)
    }
}
