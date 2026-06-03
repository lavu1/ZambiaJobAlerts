import Foundation

enum AppDestination: Equatable {
    case home
    case job(identifier: String, openedFromDeepLink: Bool)
}

@MainActor
final class AppLaunchStore: ObservableObject {
    static let shared = AppLaunchStore()

    @Published private(set) var pendingDestination: AppDestination?

    private init() {}

    func open(_ destination: AppDestination) {
        pendingDestination = destination
    }

    func open(url: URL, openedFromDeepLink: Bool) {
        guard let destination = KMPBridge.destination(from: url, openedFromDeepLink: openedFromDeepLink) else {
            return
        }
        open(destination)
    }

    func clear() {
        pendingDestination = nil
    }
}
