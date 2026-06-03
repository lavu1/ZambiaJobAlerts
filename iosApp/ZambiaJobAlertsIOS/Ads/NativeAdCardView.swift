import SwiftUI
import UIKit

#if canImport(GoogleMobileAds)
import GoogleMobileAds

struct NativeAdCardView: View {
    let slot: Int
    @StateObject private var loader = NativeAdSlotLoader()

    var body: some View {
        Group {
            if let nativeAd = loader.nativeAd {
                NativeAdRepresentable(nativeAd: nativeAd)
                    .frame(height: 300)
            } else {
                EmptyView()
            }
        }
        .task(id: slot) {
            loader.loadIfNeeded()
        }
    }
}

@MainActor
final class NativeAdSlotLoader: NSObject, ObservableObject {
    @Published private(set) var nativeAd: NativeAd?

    private var adLoader: AdLoader?
    private var isLoading = false
    private var retryWorkItem: DispatchWorkItem?

    func loadIfNeeded() {
        guard !isLoading, nativeAd == nil else { return }

        isLoading = true
        adLoader = AdLoader(
            adUnitID: AdUnitIDs.native,
            rootViewController: UIApplication.shared.firstRootViewController,
            adTypes: [.native],
            options: nil
        )
        adLoader?.delegate = self
        adLoader?.load(Request())
    }

    private func scheduleRetry() {
        retryWorkItem?.cancel()
        let workItem = DispatchWorkItem { [weak self] in
            self?.loadIfNeeded()
        }
        retryWorkItem = workItem
        DispatchQueue.main.asyncAfter(deadline: .now() + 5.0, execute: workItem)
    }
}

extension NativeAdSlotLoader: NativeAdLoaderDelegate {
    func adLoader(_ adLoader: AdLoader, didReceive nativeAd: NativeAd) {
        retryWorkItem?.cancel()
        retryWorkItem = nil
        isLoading = false
        self.nativeAd = nativeAd
    }
}

extension NativeAdSlotLoader: AdLoaderDelegate {
    func adLoader(_ adLoader: AdLoader, didFailToReceiveAdWithError error: Error) {
        isLoading = false
        scheduleRetry()
    }
}

private struct NativeAdRepresentable: UIViewRepresentable {
    let nativeAd: NativeAd

    func makeUIView(context: Context) -> NativeAdView {
        let nativeAdView = NativeAdView()
        nativeAdView.translatesAutoresizingMaskIntoConstraints = false

        let container = UIView()
        container.backgroundColor = .secondarySystemBackground
        container.layer.cornerRadius = 8
        container.layer.masksToBounds = true
        container.translatesAutoresizingMaskIntoConstraints = false

        let adBadge = UILabel()
        adBadge.text = "Ad"
        adBadge.font = .preferredFont(forTextStyle: .caption1)
        adBadge.textColor = .secondaryLabel
        adBadge.translatesAutoresizingMaskIntoConstraints = false

        let headlineLabel = UILabel()
        headlineLabel.font = .preferredFont(forTextStyle: .headline)
        headlineLabel.numberOfLines = 2
        headlineLabel.translatesAutoresizingMaskIntoConstraints = false

        let bodyLabel = UILabel()
        bodyLabel.font = .preferredFont(forTextStyle: .subheadline)
        bodyLabel.textColor = .secondaryLabel
        bodyLabel.numberOfLines = 3
        bodyLabel.translatesAutoresizingMaskIntoConstraints = false

        let mediaView = MediaView()
        mediaView.translatesAutoresizingMaskIntoConstraints = false

        let callToActionButton = UIButton(type: .system)
        var configuration = UIButton.Configuration.filled()
        configuration.cornerStyle = .medium
        configuration.contentInsets = NSDirectionalEdgeInsets(
            top: 8,
            leading: 14,
            bottom: 8,
            trailing: 14
        )
        callToActionButton.configuration = configuration
        callToActionButton.isUserInteractionEnabled = false
        callToActionButton.translatesAutoresizingMaskIntoConstraints = false

        nativeAdView.addSubview(container)
        container.addSubview(adBadge)
        container.addSubview(headlineLabel)
        container.addSubview(mediaView)
        container.addSubview(bodyLabel)
        container.addSubview(callToActionButton)

        NSLayoutConstraint.activate([
            container.topAnchor.constraint(equalTo: nativeAdView.topAnchor),
            container.leadingAnchor.constraint(equalTo: nativeAdView.leadingAnchor),
            container.trailingAnchor.constraint(equalTo: nativeAdView.trailingAnchor),
            container.bottomAnchor.constraint(equalTo: nativeAdView.bottomAnchor),

            adBadge.topAnchor.constraint(equalTo: container.topAnchor, constant: 12),
            adBadge.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 12),

            headlineLabel.topAnchor.constraint(equalTo: adBadge.bottomAnchor, constant: 6),
            headlineLabel.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 12),
            headlineLabel.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -12),

            mediaView.topAnchor.constraint(equalTo: headlineLabel.bottomAnchor, constant: 10),
            mediaView.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 12),
            mediaView.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -12),
            mediaView.heightAnchor.constraint(equalToConstant: 140),

            bodyLabel.topAnchor.constraint(equalTo: mediaView.bottomAnchor, constant: 10),
            bodyLabel.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 12),
            bodyLabel.trailingAnchor.constraint(equalTo: container.trailingAnchor, constant: -12),

            callToActionButton.topAnchor.constraint(equalTo: bodyLabel.bottomAnchor, constant: 10),
            callToActionButton.leadingAnchor.constraint(equalTo: container.leadingAnchor, constant: 12),
            callToActionButton.bottomAnchor.constraint(lessThanOrEqualTo: container.bottomAnchor, constant: -12)
        ])

        nativeAdView.headlineView = headlineLabel
        nativeAdView.bodyView = bodyLabel
        nativeAdView.callToActionView = callToActionButton
        nativeAdView.mediaView = mediaView

        return nativeAdView
    }

    func updateUIView(_ uiView: NativeAdView, context: Context) {
        (uiView.headlineView as? UILabel)?.text = nativeAd.headline
        (uiView.bodyView as? UILabel)?.text = nativeAd.body
        if let callToActionButton = uiView.callToActionView as? UIButton {
            var configuration = callToActionButton.configuration
            configuration?.title = nativeAd.callToAction
            callToActionButton.configuration = configuration
            callToActionButton.isHidden = nativeAd.callToAction == nil
        }
        uiView.mediaView?.mediaContent = nativeAd.mediaContent
        uiView.nativeAd = nativeAd
    }
}
#else
struct NativeAdCardView: View {
    let slot: Int

    var body: some View {
        EmptyView()
    }
}
#endif
