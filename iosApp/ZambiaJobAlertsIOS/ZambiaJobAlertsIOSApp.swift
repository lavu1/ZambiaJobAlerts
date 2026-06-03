import SwiftUI

@main
struct ZambiaJobAlertsIOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var launchStore = AppLaunchStore.shared
    @StateObject private var jobsStore = JobsStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(launchStore)
                .environmentObject(jobsStore)
                .onOpenURL { url in
                    launchStore.open(url: url, openedFromDeepLink: true)
                }
                .task {
                    AdBootstrap.start()
                    await AppOpenAdManager.shared.loadAd()
                    await InterstitialAdManager.shared.loadAd()
                    await RewardedAdManager.shared.loadAd()
                    await RewardedInterstitialAdManager.shared.loadAd()
                }
                .onChange(of: scenePhase) { phase in
                    guard phase == .active else { return }
                    AppOpenAdManager.shared.showAdIfAvailable()
                    Task {
                        await AppOpenAdManager.shared.loadAd()
                        await InterstitialAdManager.shared.loadAd()
                        await RewardedAdManager.shared.loadAd()
                        await RewardedInterstitialAdManager.shared.loadAd()
                    }
                }
        }
    }
}
