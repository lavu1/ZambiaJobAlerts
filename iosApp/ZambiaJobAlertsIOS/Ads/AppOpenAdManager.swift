import UIKit

#if canImport(GoogleMobileAds)
import GoogleMobileAds

@MainActor
final class AppOpenAdManager: NSObject, FullScreenContentDelegate {
    static let shared = AppOpenAdManager()

    private var appOpenAd: AppOpenAd?
    private var isLoading = false
    private var isShowing = false
    private var loadTime: Date?

    private override init() {}

    func loadAd() async {
        guard !isLoading, !isAdAvailable else { return }
        isLoading = true
        defer { isLoading = false }

        do {
            appOpenAd = try await AppOpenAd.load(
                with: AdUnitIDs.appOpen,
                request: Request()
            )
            appOpenAd?.fullScreenContentDelegate = self
            loadTime = Date()
        } catch {
            appOpenAd = nil
            loadTime = nil
            print("App open load failed: \(error.localizedDescription)")
        }
    }

    func showAdIfAvailable() {
        guard !isShowing else { return }
        guard isAdAvailable, let appOpenAd else {
            Task { await loadAd() }
            return
        }
        appOpenAd.present(from: UIApplication.shared.firstRootViewController)
        isShowing = true
    }

    private var isAdAvailable: Bool {
        guard appOpenAd != nil, let loadTime else { return false }
        return Date().timeIntervalSince(loadTime) < 4 * 60 * 60
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        appOpenAd = nil
        isShowing = false
        Task { await loadAd() }
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        appOpenAd = nil
        isShowing = false
        Task { await loadAd() }
    }
}
#else
@MainActor
final class AppOpenAdManager {
    static let shared = AppOpenAdManager()
    func loadAd() async {}
    func showAdIfAvailable() {}
}
#endif
