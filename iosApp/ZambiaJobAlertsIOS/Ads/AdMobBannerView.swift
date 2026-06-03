import SwiftUI
import UIKit

#if canImport(GoogleMobileAds)
import GoogleMobileAds

struct AdMobBannerView: UIViewRepresentable {
    let adUnitID: String
    let usesAdaptiveSizing: Bool

    init(adUnitID: String, usesAdaptiveSizing: Bool = false) {
        self.adUnitID = adUnitID
        self.usesAdaptiveSizing = usesAdaptiveSizing
    }

    func makeUIView(context: Context) -> BannerView {
        let adSize = usesAdaptiveSizing
            ? largeAnchoredAdaptiveBanner(width: UIScreen.main.bounds.width)
            : AdSizeBanner
        let banner = BannerView(adSize: adSize)
        banner.adUnitID = adUnitID
        banner.rootViewController = UIApplication.shared.firstRootViewController
        banner.load(Request())
        return banner
    }

    func updateUIView(_ uiView: BannerView, context: Context) {
        uiView.rootViewController = UIApplication.shared.firstRootViewController
        guard usesAdaptiveSizing else { return }
        let width = max(uiView.bounds.width, UIScreen.main.bounds.width)
        uiView.adSize = largeAnchoredAdaptiveBanner(width: width)
    }
}
#else
struct AdMobBannerView: View {
    let adUnitID: String
    let usesAdaptiveSizing: Bool

    init(adUnitID: String, usesAdaptiveSizing: Bool = false) {
        self.adUnitID = adUnitID
        self.usesAdaptiveSizing = usesAdaptiveSizing
    }

    var body: some View {
        EmptyView()
    }
}
#endif

extension UIApplication {
    var firstRootViewController: UIViewController? {
        connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController
    }
}
