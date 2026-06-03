import Foundation

#if canImport(ZambiaJobAlertsShared)
import ZambiaJobAlertsShared
#endif

enum AdUnitIDs {
    #if canImport(ZambiaJobAlertsShared)
    static let banner = SharedAdConfig.shared.IOS_BANNER_AD_UNIT_ID
    static let fixedBanner = SharedAdConfig.shared.IOS_FIXED_BANNER_AD_UNIT_ID
    static let interstitial = SharedAdConfig.shared.IOS_INTERSTITIAL_AD_UNIT_ID
    static let appOpen = SharedAdConfig.shared.IOS_APP_OPEN_AD_UNIT_ID
    static let rewarded = SharedAdConfig.shared.IOS_REWARDED_AD_UNIT_ID
    static let rewardedInterstitial = SharedAdConfig.shared.IOS_REWARDED_INTERSTITIAL_AD_UNIT_ID
    static let native = SharedAdConfig.shared.IOS_NATIVE_AD_UNIT_ID
    static let nativeVideo = SharedAdConfig.shared.IOS_NATIVE_VIDEO_AD_UNIT_ID
    #else
    static let banner = "ca-app-pub-2168080105757285/3720563865"
    static let fixedBanner = "ca-app-pub-2168080105757285/9099638813"
    static let interstitial = "ca-app-pub-2168080105757285/3172002592"
    static let appOpen = "ca-app-pub-2168080105757285/8837159177"
    static let rewarded = "ca-app-pub-2168080105757285/7171994767"
    static let rewardedInterstitial = "ca-app-pub-2168080105757285/2215910501"
    static let native = "ca-app-pub-2168080105757285/8691922587"
    static let nativeVideo = "ca-app-pub-2168080105757285/8691922587"
    #endif
}

enum TextGenerationConfig {
    #if canImport(ZambiaJobAlertsShared)
    static let endpoint = SharedApiConfig.shared.TEXT_POLLINATIONS_ENDPOINT
    static let models = SharedApiConfig.shared.TEXT_POLLINATIONS_MODELS
        .split(separator: ",")
        .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        .filter { !$0.isEmpty }
    #else
    static let endpoint = "https://text.pollinations.ai/"
    static let models = [
        "nova-fast",
        "openai-fast",
        "gemini-fast",
        "qwen-coder",
        "mistral",
        "openai",
        "deepseek",
        "minimax",
        "kimi",
        "qwen-large",
        "mistral-large",
        "claude-fast",
        "gemini"
    ]
    #endif
}

enum KMPBridge {
    static func destination(from url: URL, openedFromDeepLink: Bool) -> AppDestination? {
        #if canImport(ZambiaJobAlertsShared)
        let request = SharedLaunchRouter.shared.parseUri(
            scheme: url.scheme,
            host: url.host,
            path: url.path,
            openedFromDeepLink: openedFromDeepLink
        )
        return destination(from: request)
        #else
        return fallbackDestination(from: url, openedFromDeepLink: openedFromDeepLink)
        #endif
    }

    static func destination(fromNotificationUserInfo userInfo: [AnyHashable: Any]) -> AppDestination? {
        let values = NotificationValues(userInfo: userInfo)

        #if canImport(ZambiaJobAlertsShared)
        let payload = SharedNotificationParser.shared.fromValues(
            title: values.title,
            message: values.message,
            jobId: values.jobId,
            jobSlug: values.jobSlug,
            type: values.type,
            company: values.company,
            location: values.location,
            link: values.link,
            newWpPassword: values.newWpPassword
        )
        return destination(from: SharedNotificationParser.shared.launchRequest(payload: payload))
        #else
        if let slug = values.jobSlug, !slug.isEmpty {
            return .job(identifier: slug, openedFromDeepLink: false)
        }
        if let jobId = values.jobId, !jobId.isEmpty, jobId != "-1" {
            return .job(identifier: jobId, openedFromDeepLink: false)
        }
        if let link = values.link,
           let url = URL(string: normalizeLaunchURL(link)) {
            return fallbackDestination(from: url, openedFromDeepLink: true)
        }
        return .home
        #endif
    }

    #if canImport(ZambiaJobAlertsShared)
    private static func destination(from request: SharedLaunchRequest?) -> AppDestination? {
        guard let request else { return nil }
        if request.destination.name == "HOME" {
            return .home
        }
        guard let identifier = request.identifier, !identifier.isEmpty else {
            return nil
        }
        return .job(identifier: identifier, openedFromDeepLink: request.openedFromDeepLink)
    }
    #endif

    private static func fallbackDestination(from url: URL, openedFromDeepLink: Bool) -> AppDestination? {
        let scheme = url.scheme?.lowercased()
        let host = url.host?.lowercased()
        let components = url.path.split(separator: "/").map(String.init)

        if scheme == "zambiajobalerts", host == "home" {
            return .home
        }
        if scheme == "zambiajobalerts", (host == "job" || host == "jobs") {
            return components.first.map { .job(identifier: $0, openedFromDeepLink: openedFromDeepLink) }
        }
        guard scheme == "https" || scheme == "http",
              host == "zambiajobalerts.com" || host == "www.zambiajobalerts.com" else {
            return nil
        }
        if components.isEmpty {
            return .home
        }
        guard components.count >= 2, components[0] == "job" || components[0] == "jobs" else {
            return nil
        }
        return .job(identifier: components[1], openedFromDeepLink: openedFromDeepLink)
    }

    private static func normalizeLaunchURL(_ value: String) -> String {
        let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.hasPrefix("zambiajobalerts.com") || trimmed.hasPrefix("www.zambiajobalerts.com") {
            return "https://\(trimmed)"
        }
        return trimmed
    }
}

private struct NotificationValues {
    let title: String?
    let message: String?
    let jobId: String?
    let jobSlug: String?
    let type: String?
    let company: String?
    let location: String?
    let link: String?
    let newWpPassword: String?

    init(userInfo: [AnyHashable: Any]) {
        let aps = userInfo["aps"] as? [String: Any]
        let alert = aps?["alert"] as? [String: Any]

        title = NotificationValues.string("title", in: userInfo) ?? alert?["title"] as? String
        message = NotificationValues.string("message", in: userInfo) ?? alert?["body"] as? String
        jobId = NotificationValues.string("job_id", in: userInfo)
        jobSlug = NotificationValues.string("job_slug", in: userInfo)
        type = NotificationValues.string("type", in: userInfo)
        company = NotificationValues.string("company", in: userInfo)
        location = NotificationValues.string("location", in: userInfo)
        link = NotificationValues.firstString(["deep_link", "deeplink", "link", "url"], in: userInfo)
        newWpPassword = NotificationValues.string("new_wp_password", in: userInfo)
    }

    private static func firstString(_ keys: [String], in userInfo: [AnyHashable: Any]) -> String? {
        for key in keys {
            if let value = string(key, in: userInfo) {
                return value
            }
        }
        return nil
    }

    private static func string(_ key: String, in userInfo: [AnyHashable: Any]) -> String? {
        guard let value = userInfo[key] else { return nil }
        let text = String(describing: value).trimmingCharacters(in: .whitespacesAndNewlines)
        return text.isEmpty ? nil : text
    }
}
