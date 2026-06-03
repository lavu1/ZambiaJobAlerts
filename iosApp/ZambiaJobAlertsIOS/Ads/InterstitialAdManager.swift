import UIKit

#if canImport(GoogleMobileAds)
import GoogleMobileAds

@MainActor
final class InterstitialAdManager: NSObject, FullScreenContentDelegate {
    static let shared = InterstitialAdManager()

    private var interstitialAd: InterstitialAd?
    private var isLoading = false

    private override init() {}

    func loadAd() async {
        guard !isLoading, interstitialAd == nil else { return }
        isLoading = true
        defer { isLoading = false }

        do {
            interstitialAd = try await InterstitialAd.load(
                with: AdUnitIDs.interstitial,
                request: Request()
            )
            interstitialAd?.fullScreenContentDelegate = self
        } catch {
            interstitialAd = nil
            print("Interstitial load failed: \(error.localizedDescription)")
        }
    }

    func showIfAvailable() async {
        if interstitialAd == nil {
            await loadAd()
        }
        guard let interstitialAd,
              let rootViewController = UIApplication.shared.firstRootViewController else {
            return
        }
        interstitialAd.present(from: rootViewController)
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        interstitialAd = nil
        Task { await loadAd() }
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        interstitialAd = nil
        Task { await loadAd() }
    }
}

@MainActor
final class RewardedAdManager: NSObject, FullScreenContentDelegate {
    static let shared = RewardedAdManager()

    private var rewardedAd: RewardedAd?
    private var isLoading = false
    private var rewardContinuation: CheckedContinuation<Bool, Never>?
    private var didEarnReward = false

    private override init() {}

    func loadAd() async {
        guard !isLoading, rewardedAd == nil else { return }
        isLoading = true
        defer { isLoading = false }

        do {
            rewardedAd = try await RewardedAd.load(
                with: AdUnitIDs.rewarded,
                request: Request()
            )
            rewardedAd?.fullScreenContentDelegate = self
        } catch {
            rewardedAd = nil
            print("Rewarded ad load failed: \(error.localizedDescription)")
        }
    }

    func showIfAvailable() async -> Bool {
        if rewardedAd == nil {
            await loadAd()
        }
        guard let rewardedAd,
              let rootViewController = UIApplication.shared.firstRootViewController else {
            return false
        }

        return await withCheckedContinuation { continuation in
            rewardContinuation = continuation
            didEarnReward = false
            rewardedAd.present(from: rootViewController) { [weak self] in
                Task { @MainActor in
                    self?.didEarnReward = true
                }
            }
            self.rewardedAd = nil
        }
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        finishPresentation()
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        finishPresentation(rewarded: false)
    }

    private func finishPresentation(rewarded: Bool? = nil) {
        let result = rewarded ?? didEarnReward
        rewardContinuation?.resume(returning: result)
        rewardContinuation = nil
        didEarnReward = false
        rewardedAd = nil
        Task { await loadAd() }
    }
}

@MainActor
final class RewardedInterstitialAdManager: NSObject, FullScreenContentDelegate {
    static let shared = RewardedInterstitialAdManager()

    private var rewardedInterstitialAd: RewardedInterstitialAd?
    private var isLoading = false
    private var rewardContinuation: CheckedContinuation<Bool, Never>?
    private var didEarnReward = false

    private override init() {}

    func loadAd() async {
        guard !isLoading, rewardedInterstitialAd == nil else { return }
        isLoading = true
        defer { isLoading = false }

        do {
            rewardedInterstitialAd = try await RewardedInterstitialAd.load(
                with: AdUnitIDs.rewardedInterstitial,
                request: Request()
            )
            rewardedInterstitialAd?.fullScreenContentDelegate = self
        } catch {
            rewardedInterstitialAd = nil
            print("Rewarded interstitial ad load failed: \(error.localizedDescription)")
        }
    }

    func showIfAvailable() async -> Bool {
        if rewardedInterstitialAd == nil {
            await loadAd()
        }
        guard let rewardedInterstitialAd,
              let rootViewController = UIApplication.shared.firstRootViewController else {
            return false
        }

        return await withCheckedContinuation { continuation in
            rewardContinuation = continuation
            didEarnReward = false
            rewardedInterstitialAd.present(from: rootViewController) { [weak self] in
                Task { @MainActor in
                    self?.didEarnReward = true
                }
            }
            self.rewardedInterstitialAd = nil
        }
    }

    func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        finishPresentation()
    }

    func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        finishPresentation(rewarded: false)
    }

    private func finishPresentation(rewarded: Bool? = nil) {
        let result = rewarded ?? didEarnReward
        rewardContinuation?.resume(returning: result)
        rewardContinuation = nil
        didEarnReward = false
        rewardedInterstitialAd = nil
        Task { await loadAd() }
    }
}
#else
@MainActor
final class InterstitialAdManager {
    static let shared = InterstitialAdManager()
    func loadAd() async {}
    func showIfAvailable() async {}
}

@MainActor
final class RewardedAdManager {
    static let shared = RewardedAdManager()
    func loadAd() async {}
    func showIfAvailable() async -> Bool { true }
}

@MainActor
final class RewardedInterstitialAdManager {
    static let shared = RewardedInterstitialAdManager()
    func loadAd() async {}
    func showIfAvailable() async -> Bool { true }
}
#endif
