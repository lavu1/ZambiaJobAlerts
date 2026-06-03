import SwiftUI
import UIKit

#if canImport(GoogleMobileAds)
import GoogleMobileAds

struct AdMobBannerView: UIViewRepresentable {
    let adUnitID: String
    let usesAdaptiveSizing: Bool
    let collapsiblePlacement: String?

    init(adUnitID: String, usesAdaptiveSizing: Bool = false, collapsiblePlacement: String? = nil) {
        self.adUnitID = adUnitID
        self.usesAdaptiveSizing = usesAdaptiveSizing
        self.collapsiblePlacement = collapsiblePlacement
    }

    func makeUIView(context: Context) -> BannerView {
        let adSize = usesAdaptiveSizing
            ? largeAnchoredAdaptiveBanner(width: UIScreen.main.bounds.width)
            : AdSizeBanner
        let banner = BannerView(adSize: adSize)
        banner.adUnitID = adUnitID
        banner.rootViewController = UIApplication.shared.firstRootViewController
        banner.load(adRequest())
        return banner
    }

    func updateUIView(_ uiView: BannerView, context: Context) {
        uiView.rootViewController = UIApplication.shared.firstRootViewController
        guard usesAdaptiveSizing else { return }
        let width = max(uiView.bounds.width, UIScreen.main.bounds.width)
        uiView.adSize = largeAnchoredAdaptiveBanner(width: width)
    }

    private func adRequest() -> Request {
        let request = Request()
        if let collapsiblePlacement {
            let extras = Extras()
            extras.additionalParameters = ["collapsible": collapsiblePlacement]
            request.register(extras)
        }
        return request
    }
}
#else
struct AdMobBannerView: View {
    let adUnitID: String
    let usesAdaptiveSizing: Bool
    let collapsiblePlacement: String?

    init(adUnitID: String, usesAdaptiveSizing: Bool = false, collapsiblePlacement: String? = nil) {
        self.adUnitID = adUnitID
        self.usesAdaptiveSizing = usesAdaptiveSizing
        self.collapsiblePlacement = collapsiblePlacement
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
