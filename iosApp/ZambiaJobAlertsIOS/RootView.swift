import SwiftUI
import StoreKit
import UIKit
import UserNotifications

private let nativeAdInterval = 3
private let bottomBannerHeight: CGFloat = 60

struct RootView: View {
    @EnvironmentObject private var launchStore: AppLaunchStore
    @EnvironmentObject private var jobsStore: JobsStore
    @StateObject private var documentsStore = GeneratedDocumentsStore()
    @StateObject private var savedJobsStore = SavedJobsStore()
    @StateObject private var updateStore = AppUpdateStore()
    @State private var selectedJob: JobSummary?
    @State private var generatorSeed: DocumentGeneratorSeed?
    @State private var selectedTab = 0
    @State private var query = ""
    @State private var globalQuery = ""
    @State private var isGlobalSearchPresented = false
    @State private var isSettingsPresented = false
    @AppStorage("ios_job_detail_open_count") private var jobDetailOpenCount = 0

    private var filteredJobs: [JobSummary] {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            return jobsStore.jobs
        }
        return jobsStore.jobs.filter { job in
            job.title.localizedCaseInsensitiveContains(query) ||
                job.company.localizedCaseInsensitiveContains(query) ||
                job.location.localizedCaseInsensitiveContains(query)
        }
    }

    var body: some View {
        VStack(spacing: 0) {
            if updateStore.isUpdateAvailable {
                AppUpdateBannerView(updateStore: updateStore)
            }

            TabView(selection: $selectedTab) {
                NavigationView {
                    jobsList
                        .navigationTitle("Zambia Job Alerts")
                        .navigationBarTitleDisplayMode(.inline)
                        .toolbar {
                            ToolbarItemGroup(placement: .navigationBarTrailing) {
                                Button {
                                    isSettingsPresented = true
                                } label: {
                                    Image(systemName: "gearshape")
                                }
                                .accessibilityLabel("Settings")

                                Button {
                                    Task {
                                        await InterstitialAdManager.shared.showIfAvailable()
                                        await jobsStore.reload()
                                    }
                                } label: {
                                    Image(systemName: "arrow.clockwise")
                                }
                                .accessibilityLabel("Refresh jobs")
                            }
                        }
                        .searchable(text: $query, prompt: "Search jobs")
                }
                .navigationViewStyle(.stack)
                .tabItem {
                    Label("Jobs", systemImage: "briefcase")
                }
                .tag(0)

                SavedJobsView(
                    savedJobsStore: savedJobsStore,
                    jobsStore: jobsStore,
                    onOpen: { job in
                        Task { await open(job) }
                    },
                    onGenerate: { job, type in
                        generatorSeed = DocumentGeneratorSeed(type: type, job: job)
                    },
                    onSearch: { isGlobalSearchPresented = true }
                )
                .tabItem {
                    Label("Saved", systemImage: "bookmark")
                }
                .tag(1)

                ServicesView(onSearch: { isGlobalSearchPresented = true })
                    .tabItem {
                        Label("Services", systemImage: "sparkles")
                    }
                    .tag(2)

                NavigationView {
                    DocumentGeneratorView(
                        seed: .empty,
                        documentStore: documentsStore,
                        onSearch: { isGlobalSearchPresented = true }
                        )
                }
                .navigationViewStyle(.stack)
                .tabItem {
                    Label("Generate", systemImage: "doc.badge.plus")
                }
                .tag(3)

                NavigationView {
                    DocumentLibraryView(
                        documentStore: documentsStore,
                        onSearch: { isGlobalSearchPresented = true }
                        )
                }
                .navigationViewStyle(.stack)
                .tabItem {
                    Label("Documents", systemImage: "folder")
                }
                .tag(4)
            }
        }
        .task {
            await jobsStore.loadIfNeeded()
            await updateStore.checkForUpdate()
        }
        .sheet(item: $selectedJob) { job in
            JobDetailView(
                job: job,
                isSaved: savedJobsStore.isSaved(job),
                onToggleSave: { _ = savedJobsStore.toggle(job) }
            ) { type in
                let seed = DocumentGeneratorSeed(type: type, job: job)
                selectedJob = nil
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    generatorSeed = seed
                }
            }
        }
        .sheet(item: $generatorSeed) { seed in
            NavigationView {
                DocumentGeneratorView(
                    seed: seed,
                    documentStore: documentsStore,
                    onSearch: { isGlobalSearchPresented = true }
                )
                    .toolbar {
                        ToolbarItem(placement: .navigationBarTrailing) {
                            Button("Done") {
                                generatorSeed = nil
                            }
                        }
                    }
            }
            .navigationViewStyle(.stack)
        }
        .sheet(isPresented: $isGlobalSearchPresented) {
            GlobalJobSearchView(jobsStore: jobsStore, query: $globalQuery) { job in
                query = globalQuery
                selectedTab = 0
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    Task {
                        await open(job)
                    }
                }
            }
        }
        .sheet(isPresented: $isSettingsPresented) {
            SettingsView(onSearch: {
                isSettingsPresented = false
                isGlobalSearchPresented = true
            })
        }
        .onReceive(launchStore.$pendingDestination.compactMap { $0 }) { destination in
            handle(destination)
            launchStore.clear()
        }
    }

    private var jobsList: some View {
        Group {
            if jobsStore.isLoading && jobsStore.jobs.isEmpty {
                ProgressView("Loading jobs")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let error = jobsStore.error, jobsStore.jobs.isEmpty {
                VStack(spacing: 12) {
                    Image(systemName: "wifi.exclamationmark")
                        .font(.largeTitle)
                        .foregroundStyle(.secondary)
                    Text("Unable to load jobs")
                        .font(.headline)
                    Text(error)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollViewReader { scrollProxy in
                    List {
                        ForEach(Array(filteredJobs.enumerated()), id: \.element.id) { index, job in
                            JobListItemView(
                                job: job,
                                isSaved: savedJobsStore.isSaved(job),
                                onDetails: {
                                    Task {
                                        await open(job)
                                    }
                                },
                                onGenerate: { type in
                                    generatorSeed = DocumentGeneratorSeed(type: type, job: job)
                                },
                                onToggleSave: {
                                    _ = savedJobsStore.toggle(job)
                                }
                            )
                            .id(job.id)

                            if (index + 1).isMultiple(of: nativeAdInterval) {
                                NativeAdCardView(slot: (index + 1) / nativeAdInterval)
                                    .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                            }
                        }

                        if let error = jobsStore.error {
                            Text(error)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                                .frame(maxWidth: .infinity, alignment: .center)
                                .padding(.vertical, 8)
                        }

                        if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                            if jobsStore.hasMore {
                                Button {
                                    Task {
                                        await loadMoreAndReveal(scrollProxy)
                                    }
                                } label: {
                                    HStack {
                                        Spacer()
                                        if jobsStore.isLoadingMore {
                                            ProgressView()
                                        } else {
                                            Label("Load More Jobs", systemImage: "arrow.down.circle")
                                        }
                                        Spacer()
                                    }
                                }
                                .disabled(jobsStore.isLoadingMore)
                            } else if !jobsStore.jobs.isEmpty {
                                Text("All available loaded jobs are showing.")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                                    .frame(maxWidth: .infinity, alignment: .center)
                            }
                        }

                        BottomBannerAdView()
                            .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
                    }
                    .refreshable {
                        await jobsStore.reload()
                    }
                }
            }
        }
        .safeAreaInset(edge: .top, spacing: 0) {
            AdMobBannerView(adUnitID: AdUnitIDs.banner)
                .frame(height: 60)
                .frame(maxWidth: .infinity)
                .background(Color(.systemBackground))
        }
    }

    @MainActor
    private func open(_ job: JobSummary) async {
        jobDetailOpenCount += 1
        if jobDetailOpenCount >= 5 {
            _ = await RewardedInterstitialAdManager.shared.showIfAvailable()
            jobDetailOpenCount = 0
        }
        selectedJob = job
    }

    @MainActor
    private func loadMoreAndReveal(_ scrollProxy: ScrollViewProxy) async {
        guard let firstNewJob = await jobsStore.loadMore() else { return }
        withAnimation {
            scrollProxy.scrollTo(firstNewJob.id, anchor: .top)
        }
    }

    private func handle(_ destination: AppDestination) {
        switch destination {
        case .home:
            selectedTab = 0
            selectedJob = nil
        case .job(let identifier, _):
            selectedTab = 0
            Task {
                if let job = await jobsStore.job(identifier: identifier) {
                    selectedJob = job
                }
            }
        }
    }
}

private struct BottomBannerAdView: View {
    var body: some View {
        AdMobBannerView(adUnitID: AdUnitIDs.fixedBanner)
        .frame(height: bottomBannerHeight)
        .frame(maxWidth: .infinity)
        .background(Color(.systemBackground))
        .clipped()
    }
}

private struct JobRow: View {
    let job: JobSummary

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(job.title)
                .font(.headline)
                .foregroundStyle(.primary)
            VStack(alignment: .leading, spacing: 6) {
                if !job.company.isEmpty {
                    Label(job.company, systemImage: "building.2")
                }
                if !job.location.isEmpty {
                    Label(job.location, systemImage: "mappin.and.ellipse")
                }
            }
            .font(.caption)
            .foregroundStyle(.secondary)
            if !job.excerpt.isEmpty {
                Text(job.excerpt)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(3)
            }
        }
        .padding(.vertical, 6)
    }
}

private struct JobListItemView: View {
    let job: JobSummary
    let isSaved: Bool
    let onDetails: () -> Void
    let onGenerate: (GeneratedDocumentType) -> Void
    let onToggleSave: () -> Void
    @State private var shareItem: ShareItem?

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            JobRow(job: job)

            VStack(spacing: 8) {
                HStack(spacing: 8) {
                    Button {
                        onGenerate(.cv)
                    } label: {
                        Label("CV", systemImage: "doc.text")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)

                    Button(action: onDetails) {
                        Label("Details", systemImage: "info.circle")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }

                HStack(spacing: 8) {
                    Button {
                        onGenerate(.coverLetter)
                    } label: {
                        Label("Cover Letter", systemImage: "envelope")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)

                    Button(action: onToggleSave) {
                        Label(isSaved ? "Saved" : "Save", systemImage: isSaved ? "bookmark.fill" : "bookmark")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }

                HStack(spacing: 8) {
                    if let applyURL = job.applicationURL {
                        Link(destination: applyURL) {
                            Label("Apply", systemImage: "paperplane")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                    }

                    Button {
                        shareItem = ShareItem(items: [shareText])
                    } label: {
                        Label("Share", systemImage: "square.and.arrow.up")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
            }
        }
        .padding(.vertical, 6)
        .contentShape(Rectangle())
        .onTapGesture(perform: onDetails)
        .accessibilityAddTraits(.isButton)
        .sheet(item: $shareItem) { item in
            ShareSheet(items: item.items)
        }
    }

    private var shareText: String {
        let appURL = "https://apps.apple.com/app/zambia-job-alerts"
        return [
            "Check out this job opportunity: \(job.title)",
            job.link,
            "Via Zambia Job Alerts App",
            "Download now: \(appURL)"
        ]
        .filter { !$0.isEmpty }
        .joined(separator: "\n\n")
    }
}

private struct JobDetailView: View {
    let job: JobSummary
    let isSaved: Bool
    let onToggleSave: () -> Void
    let onGenerate: (GeneratedDocumentType) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    Text(job.title)
                        .font(.title2.bold())
                    if !job.company.isEmpty || !job.location.isEmpty {
                        VStack(alignment: .leading, spacing: 6) {
                            if !job.company.isEmpty {
                                Label(job.company, systemImage: "building.2")
                            }
                            if !job.location.isEmpty {
                                Label(job.location, systemImage: "mappin.and.ellipse")
                            }
                            if !job.jobType.isEmpty {
                                Label(job.jobType, systemImage: "clock")
                            }
                        }
                        .foregroundStyle(.secondary)
                    }
                    if !job.content.isEmpty {
                        Text(job.content)
                            .font(.body)
                    } else if !job.excerpt.isEmpty {
                        Text(job.excerpt)
                            .font(.body)
                    }
                    if let applyURL = job.applicationURL {
                        Link(destination: applyURL) {
                            Label("Apply", systemImage: "paperplane")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                    }
                    Button {
                        onGenerate(.cv)
                        dismiss()
                    } label: {
                        Label("Generate CV", systemImage: "doc.text")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button {
                        onGenerate(.coverLetter)
                        dismiss()
                    } label: {
                        Label("Generate Cover Letter", systemImage: "envelope")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button(action: onToggleSave) {
                        Label(isSaved ? "Saved" : "Save Job", systemImage: isSaved ? "bookmark.fill" : "bookmark")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    NativeAdCardView(slot: 60)
                }
                .padding()
            }
            .navigationTitle("Job Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") { dismiss() }
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                BottomBannerAdView()
            }
        }
        .navigationViewStyle(.stack)
    }
}

private struct SavedJobsView: View {
    @ObservedObject var savedJobsStore: SavedJobsStore
    @ObservedObject var jobsStore: JobsStore
    let onOpen: (JobSummary) -> Void
    let onGenerate: (JobSummary, GeneratedDocumentType) -> Void
    let onSearch: () -> Void

    @State private var loadingJobId: Int?

    var body: some View {
        NavigationView {
            List {
                if savedJobsStore.savedJobs.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("No saved jobs yet.")
                            .font(.headline)
                        Text("Jobs you save on this device will appear here.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 8)

                    NativeAdCardView(slot: 210)
                        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                } else {
                    ForEach(Array(savedJobsStore.savedJobs.enumerated()), id: \.element.id) { index, job in
                        VStack(spacing: 8) {
                            JobListItemView(
                                job: job,
                                isSaved: true,
                                onDetails: { openLatest(job) },
                                onGenerate: { type in onGenerate(job, type) },
                                onToggleSave: { _ = savedJobsStore.toggle(job) }
                            )

                            if loadingJobId == job.id {
                                ProgressView("Loading latest job details")
                                    .font(.footnote)
                            }
                        }

                        if (index + 1).isMultiple(of: nativeAdInterval) {
                            NativeAdCardView(slot: 220 + index)
                                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                        }
                    }
                }
            }
            .navigationTitle("Saved Jobs")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onSearch) {
                        Image(systemName: "magnifyingglass")
                    }
                    .accessibilityLabel("Search jobs")
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                BottomBannerAdView()
            }
        }
        .navigationViewStyle(.stack)
    }

    private func openLatest(_ job: JobSummary) {
        Task {
            loadingJobId = job.id
            let latest = await jobsStore.job(identifier: "\(job.id)") ?? job
            savedJobsStore.replace(latest)
            loadingJobId = nil
            onOpen(latest)
        }
    }
}

@MainActor
private final class SavedJobsStore: ObservableObject {
    @Published private(set) var savedJobs: [JobSummary] = []

    private let defaultsKey = "saved_jobs"

    init() {
        load()
    }

    func isSaved(_ job: JobSummary) -> Bool {
        savedJobs.contains { $0.id == job.id }
    }

    @discardableResult
    func toggle(_ job: JobSummary) -> Bool {
        if isSaved(job) {
            savedJobs.removeAll { $0.id == job.id }
            persist()
            return false
        }

        savedJobs.insert(job, at: 0)
        persist()
        return true
    }

    func replace(_ job: JobSummary) {
        guard let index = savedJobs.firstIndex(where: { $0.id == job.id }) else { return }
        savedJobs[index] = job
        persist()
    }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: defaultsKey),
              let decoded = try? JSONDecoder().decode([JobSummary].self, from: data) else {
            savedJobs = []
            return
        }
        savedJobs = decoded
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(savedJobs) else { return }
        UserDefaults.standard.set(data, forKey: defaultsKey)
    }
}

private struct AppUpdateBannerView: View {
    @ObservedObject var updateStore: AppUpdateStore

    var body: some View {
        if let update = updateStore.availableUpdate {
            Button {
                updateStore.openStore()
            } label: {
                HStack(spacing: 10) {
                    Image(systemName: "arrow.down.circle.fill")
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Update available")
                            .font(.subheadline.bold())
                        Text("Version \(update.version) is ready to install.")
                            .font(.caption)
                    }
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.caption.bold())
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity)
                .foregroundStyle(.white)
                .background(Color(red: 1.0, green: 0.52, blue: 0.11))
            }
            .buttonStyle(.plain)
        }
    }
}

@MainActor
private final class AppUpdateStore: ObservableObject {
    @Published private(set) var availableUpdate: StoreUpdate?

    var isUpdateAvailable: Bool {
        availableUpdate != nil
    }

    func checkForUpdate() async {
        guard let bundleID = Bundle.main.bundleIdentifier else { return }
        let currentVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0"

        do {
            let zambiaResult = try await lookup(bundleID: bundleID, country: "ZM")
            let result: LookupResult?
            if let zambiaResult {
                result = zambiaResult
            } else {
                result = try await lookup(bundleID: bundleID, country: nil)
            }
            guard let result else {
                availableUpdate = nil
                return
            }

            guard isVersion(result.version, newerThan: currentVersion),
                  let url = result.storeURL else {
                availableUpdate = nil
                return
            }

            let update = StoreUpdate(version: result.version, storeURL: url)
            availableUpdate = update
            notifyIfNeeded(update)
        } catch {
            availableUpdate = nil
        }
    }

    func openStore() {
        guard let url = availableUpdate?.storeURL else { return }
        UIApplication.shared.open(url)
    }

    private func lookup(bundleID: String, country: String?) async throws -> LookupResult? {
        var components = URLComponents(string: "https://itunes.apple.com/lookup")!
        var queryItems = [URLQueryItem(name: "bundleId", value: bundleID)]
        if let country {
            queryItems.append(URLQueryItem(name: "country", value: country))
        }
        components.queryItems = queryItems

        let (data, response) = try await URLSession.shared.data(from: components.url!)
        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }

        let decoded = try JSONDecoder().decode(LookupResponse.self, from: data)
        return decoded.results.first
    }

    private func notifyIfNeeded(_ update: StoreUpdate) {
        let defaultsKey = "notified_ios_update_version"
        guard UserDefaults.standard.string(forKey: defaultsKey) != update.version else { return }

        UNUserNotificationCenter.current().getNotificationSettings { settings in
            let allowedStatuses: [UNAuthorizationStatus] = [.authorized, .provisional, .ephemeral]
            guard allowedStatuses.contains(settings.authorizationStatus) else { return }

            let content = UNMutableNotificationContent()
            content.title = "Zambia Job Alerts update available"
            content.body = "Version \(update.version) is ready to install."
            content.sound = .default

            let request = UNNotificationRequest(
                identifier: "zambia_job_alerts_update_available",
                content: content,
                trigger: UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
            )
            UNUserNotificationCenter.current().add(request)
            UserDefaults.standard.set(update.version, forKey: defaultsKey)
        }
    }

    private func isVersion(_ candidate: String, newerThan current: String) -> Bool {
        let candidateParts = candidate.split(separator: ".").map { Int($0) ?? 0 }
        let currentParts = current.split(separator: ".").map { Int($0) ?? 0 }
        let count = max(candidateParts.count, currentParts.count)

        for index in 0..<count {
            let candidateValue = index < candidateParts.count ? candidateParts[index] : 0
            let currentValue = index < currentParts.count ? currentParts[index] : 0
            if candidateValue > currentValue { return true }
            if candidateValue < currentValue { return false }
        }
        return false
    }
}

private struct StoreUpdate {
    let version: String
    let storeURL: URL
}

private struct LookupResponse: Decodable {
    let results: [LookupResult]
}

private struct LookupResult: Decodable {
    let version: String
    let trackViewUrl: String?
    let trackId: Int?

    var storeURL: URL? {
        if let trackViewUrl,
           let url = URL(string: trackViewUrl.replacingOccurrences(of: "https://", with: "itms-apps://")) {
            return url
        }
        guard let trackId else { return nil }
        return URL(string: "itms-apps://itunes.apple.com/app/id\(trackId)")
    }
}

private struct ServicesView: View {
    let onSearch: () -> Void

    var body: some View {
        NavigationView {
            List {
                ForEach(Array(SharedService.all.enumerated()), id: \.element.id) { index, service in
                    VStack(alignment: .leading, spacing: 6) {
                        Text(service.title)
                            .font(.headline)
                        Text("\(service.creditCost) credit\(service.creditCost == 1 ? "" : "s")")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 4)

                    if (index + 1).isMultiple(of: nativeAdInterval) {
                        NativeAdCardView(slot: 100 + index)
                            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                    }
                }
            }
            .navigationTitle("Services")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onSearch) {
                        Image(systemName: "magnifyingglass")
                    }
                    .accessibilityLabel("Search jobs")
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                BottomBannerAdView()
            }
        }
        .navigationViewStyle(.stack)
    }
}

private struct SettingsView: View {
    let onSearch: () -> Void
    @AppStorage("notifications_enabled") private var notificationsEnabled = true
    @State private var shareItem: ShareItem?

    var body: some View {
        NavigationView {
            Form {
                Toggle("Notifications", isOn: $notificationsEnabled)
                Section("App") {
                    Button {
                        rateApp()
                    } label: {
                        Label("Rate App", systemImage: "star")
                    }

                    Button {
                        shareItem = ShareItem(items: [shareAppText])
                    } label: {
                        Label("Share App", systemImage: "square.and.arrow.up")
                    }
                }
                Section("Information") {
                    NavigationLink {
                        AboutUsView()
                    } label: {
                        Label("About Us", systemImage: "info.circle")
                    }
                    NavigationLink {
                        TermsConditionsView()
                    } label: {
                        Label("Terms & Conditions", systemImage: "doc.text")
                    }
                }
                Section {
                    NativeAdCardView(slot: 410)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onSearch) {
                        Image(systemName: "magnifyingglass")
                    }
                    .accessibilityLabel("Search jobs")
                }
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                BottomBannerAdView()
            }
        }
        .sheet(item: $shareItem) { item in
            ShareSheet(items: item.items)
        }
        .navigationViewStyle(.stack)
    }

    private var shareAppText: String {
        "Check out Zambia Job Alerts app for the latest job opportunities in Zambia. Download: https://apps.apple.com/app/zambia-job-alerts"
    }

    private func rateApp() {
        if let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }) {
            SKStoreReviewController.requestReview(in: scene)
            return
        }

        if let url = URL(string: "https://apps.apple.com/app/zambia-job-alerts") {
            UIApplication.shared.open(url)
        }
    }
}

private struct AboutUsView: View {
    var body: some View {
        List {
            ForEach(Array(aboutUsItems.enumerated()), id: \.element.id) { index, item in
                InfoContentRow(item: item)

                if (index + 1).isMultiple(of: nativeAdInterval) {
                    NativeAdCardView(slot: 610 + index)
                        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                }
            }
        }
        .navigationTitle("About Us")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomBannerAdView()
        }
    }
}

private struct TermsConditionsView: View {
    var body: some View {
        List {
            ForEach(Array(termsSections.enumerated()), id: \.element.id) { index, section in
                InfoContentRow(item: section)

                if (index + 1).isMultiple(of: nativeAdInterval) {
                    NativeAdCardView(slot: 650 + index)
                        .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                }
            }
        }
        .navigationTitle("Terms & Conditions")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomBannerAdView()
        }
    }
}

private struct InfoContentRow: View {
    let item: InfoContentItem

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(item.title)
                .font(.headline)
            Text(item.body)
                .font(.body)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 6)
    }
}

private struct InfoContentItem: Identifiable, Hashable {
    let id: String
    let title: String
    let body: String
}

private let aboutUsItems: [InfoContentItem] = [
    InfoContentItem(
        id: "expert_engineers",
        title: "Expert Support",
        body: "Our team provides career guidance, resume writing, and interview coaching to help job seekers stand out."
    ),
    InfoContentItem(
        id: "experience_skills",
        title: "Experience Skills",
        body: "We understand the job market and help candidates navigate applications, interviews, and hiring requirements."
    ),
    InfoContentItem(
        id: "low_cost",
        title: "Low Cost",
        body: "We keep job placement and career support affordable for job seekers and practical for employers."
    ),
    InfoContentItem(
        id: "verified_jobs",
        title: "Reliable & Verified Job Listings",
        body: "Job posts are checked so job seekers can focus on genuine opportunities across Zambia."
    ),
    InfoContentItem(
        id: "transparent_work",
        title: "Trusted Work And Transparent",
        body: "We work with transparency and integrity in job search, hiring support, and career services."
    ),
    InfoContentItem(
        id: "success_rate",
        title: "High Success Rate",
        body: "Many job seekers use Zambia Job Alerts as a trusted partner for career growth and daily opportunities."
    )
]

private let termsSections: [InfoContentItem] = [
    InfoContentItem(
        id: "welcome",
        title: "Welcome",
        body: "By using Zambia Job Alerts, you agree to these terms and conditions. If you disagree with any part of these terms, please do not use the app."
    ),
    InfoContentItem(
        id: "information",
        title: "1. Information On This App",
        body: "The app content is provided for general information and use. It may change without notice. We do not guarantee that all information is complete, accurate, timely, or suitable for every purpose. You are responsible for checking that any opportunity, service, or information meets your needs."
    ),
    InfoContentItem(
        id: "links",
        title: "2. Links",
        body: "The app may include links to other websites or external services. Those links are provided for convenience. Zambia Job Alerts is not responsible for the content, availability, or accuracy of external sites."
    ),
    InfoContentItem(
        id: "content_rights",
        title: "3. Content Rights",
        body: "The app contains material owned by or licensed to Zambia Job Alerts, including design, layout, appearance, and graphics. Reproduction is prohibited unless permitted by the copyright notice or by written permission."
    ),
    InfoContentItem(
        id: "copyright",
        title: "4. Copyright Notice",
        body: "This app and its content are copyright of Zambia Job Alerts. All rights are reserved. You may not redistribute, reproduce, commercially exploit, transmit, or store app content in another system without written permission."
    ),
    InfoContentItem(
        id: "disclaimer",
        title: "5. Disclaimer",
        body: "Information is provided by Zambia Job Alerts and app users. We make no warranties about completeness, accuracy, reliability, suitability, or availability. Any reliance on the information is at your own risk."
    ),
    InfoContentItem(
        id: "law",
        title: "6. Law & Jurisdiction",
        body: "Recruiters, job seekers, and other users must comply with applicable employment, data, and equality laws. Use of the app and related disputes are subject to the laws of Zambia."
    ),
    InfoContentItem(
        id: "job_seekers",
        title: "7. Job Seekers",
        body: "Our jobs board operates as a venue only and does not generally introduce or supply candidates to recruiters. Job seekers should verify opportunities and are responsible for the accuracy and legality of information they submit."
    ),
    InfoContentItem(
        id: "employers",
        title: "8. Employers & Recruiters",
        body: "Employers and recruiters are responsible for their advertisements and for dealings with candidates who respond. We do not guarantee responses or candidate suitability, and discriminatory advertisements may be amended or removed."
    ),
    InfoContentItem(
        id: "communication",
        title: "9. Communication",
        body: "Posts and messages from users represent the views of the person posting. Communication through the app must be lawful. We may remove content that is offensive, misleading, or violates these terms. For concerns, contact support@zambiajobalerts.com."
    )
]

private struct DocumentGeneratorView: View {
    let seed: DocumentGeneratorSeed
    @ObservedObject var documentStore: GeneratedDocumentsStore
    let onSearch: () -> Void

    @State private var selectedType: GeneratedDocumentType
    @State private var selectedFormat: GeneratedDocumentFormat = .pdf
    @State private var selectedTone = "Formal"
    @State private var name = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var position = ""
    @State private var education = ""
    @State private var experience = ""
    @State private var skills = ""
    @State private var company = ""
    @State private var positionApplying = ""
    @State private var experienceSummary = ""
    @State private var notes = ""
    @State private var generatedText = ""
    @State private var statusMessage: String?
    @State private var isGenerating = false
    @State private var currentFileURL: URL?
    @State private var shareItem: ShareItem?

    private let tones = ["Formal", "Professional", "Confident", "Friendly", "Enthusiastic"]

    init(
        seed: DocumentGeneratorSeed,
        documentStore: GeneratedDocumentsStore,
        onSearch: @escaping () -> Void
    ) {
        self.seed = seed
        self.documentStore = documentStore
        self.onSearch = onSearch
        _selectedType = State(initialValue: seed.type)
        _position = State(initialValue: seed.job?.title ?? "")
        _company = State(initialValue: seed.job?.company ?? "")
        _positionApplying = State(initialValue: seed.job?.title ?? "")
    }

    var body: some View {
        Form {
            Section("Document") {
                Picker("Type", selection: $selectedType) {
                    ForEach(GeneratedDocumentType.allCases) { type in
                        Text(type.title).tag(type)
                    }
                }
                Picker("Format", selection: $selectedFormat) {
                    ForEach(GeneratedDocumentFormat.allCases) { format in
                        Text(format.title).tag(format)
                    }
                }
                if selectedType == .coverLetter {
                    Picker("Tone", selection: $selectedTone) {
                        ForEach(tones, id: \.self) { tone in
                            Text(tone).tag(tone)
                        }
                    }
                }
            }

            Section {
                NativeAdCardView(slot: 500)
            }

            Section("Applicant") {
                TextField("Full name", text: $name)
                    .textContentType(.name)
                TextField("Email", text: $email)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)
                TextField("Phone", text: $phone)
                    .textContentType(.telephoneNumber)
                    .keyboardType(.phonePad)
            }

            if selectedType == .cv {
                Section("CV Details") {
                    TextField("Target role", text: $position)
                    TextEditor(text: $education)
                        .frame(minHeight: 80)
                        .overlay(alignment: .topLeading) {
                            if education.isEmpty {
                                Text("Education")
                                    .foregroundStyle(.secondary)
                                    .padding(.top, 8)
                                    .padding(.leading, 4)
                            }
                        }
                    TextEditor(text: $experience)
                        .frame(minHeight: 100)
                        .overlay(alignment: .topLeading) {
                            if experience.isEmpty {
                                Text("Work experience")
                                    .foregroundStyle(.secondary)
                                    .padding(.top, 8)
                                    .padding(.leading, 4)
                            }
                        }
                    TextEditor(text: $skills)
                        .frame(minHeight: 80)
                        .overlay(alignment: .topLeading) {
                            if skills.isEmpty {
                                Text("Skills")
                                    .foregroundStyle(.secondary)
                                    .padding(.top, 8)
                                    .padding(.leading, 4)
                            }
                        }
                }
            } else {
                Section("Cover Letter Details") {
                    TextField("Company", text: $company)
                    TextField("Position", text: $positionApplying)
                    TextEditor(text: $experienceSummary)
                        .frame(minHeight: 110)
                        .overlay(alignment: .topLeading) {
                            if experienceSummary.isEmpty {
                                Text("Relevant experience")
                                    .foregroundStyle(.secondary)
                                    .padding(.top, 8)
                                    .padding(.leading, 4)
                            }
                        }
                }
            }

            Section("Extra Notes") {
                TextEditor(text: $notes)
                    .frame(minHeight: 90)
            }

            Section {
                Button {
                    Task {
                        await generate()
                    }
                } label: {
                    HStack {
                        Spacer()
                        if isGenerating {
                            ProgressView()
                        } else {
                            Label("Generate \(selectedType.title)", systemImage: "wand.and.stars")
                        }
                        Spacer()
                    }
                }
                .disabled(isGenerating)
            }

            if let statusMessage {
                Section("Status") {
                    Text(statusMessage)
                        .font(.subheadline)
                }
            }

            if !generatedText.isEmpty {
                Section("Generated Text") {
                    Text(generatedText)
                        .font(.body)
                        .textSelection(.enabled)

                    Button {
                        UIPasteboard.general.string = generatedText
                    } label: {
                        Label("Copy Text", systemImage: "doc.on.doc")
                    }

                    if let currentFileURL {
                        Button {
                            shareItem = ShareItem(items: [currentFileURL])
                        } label: {
                            Label("Share File", systemImage: "square.and.arrow.up")
                        }
                    }
                }
            }
        }
        .navigationTitle("Generate")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: onSearch) {
                    Image(systemName: "magnifyingglass")
                }
                .accessibilityLabel("Search jobs")
            }
        }
        .sheet(item: $shareItem) { item in
            ShareSheet(items: item.items)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomBannerAdView()
        }
    }

    private func generate() async {
        guard validateInputs() else { return }

        isGenerating = true
        statusMessage = "Preparing generation..."
        generatedText = ""
        currentFileURL = nil
        defer { isGenerating = false }

        _ = await RewardedAdManager.shared.showIfAvailable()

        do {
            let prompt = buildPrompt()
            statusMessage = "Generating document..."
            let text = try await TextDocumentGenerator.generate(prompt: prompt)
            let fileURL = try GeneratedDocumentFileWriter.write(
                text: text,
                format: selectedFormat,
                baseName: outputBaseName()
            )
            let record = GeneratedDocumentRecord(
                id: UUID(),
                jobId: seed.job?.id,
                jobTitle: seed.job?.title ?? "",
                company: seed.job?.company ?? company,
                type: selectedType,
                format: selectedFormat,
                fileName: fileURL.lastPathComponent,
                filePath: fileURL.path,
                createdAt: Date()
            )

            documentStore.save(record)
            generatedText = text
            currentFileURL = fileURL
            statusMessage = "\(selectedType.title) generated and saved as \(fileURL.lastPathComponent)."
        } catch {
            statusMessage = "Generation failed. Please refresh, check your connection, or try again shortly."
        }
    }

    private func validateInputs() -> Bool {
        guard !name.trimmed.isEmpty, !email.trimmed.isEmpty, !phone.trimmed.isEmpty else {
            statusMessage = "Enter name, email, and phone before generating."
            return false
        }

        if selectedType == .cv, position.trimmed.isEmpty {
            statusMessage = "Enter the target role for the CV."
            return false
        }

        if selectedType == .coverLetter,
           company.trimmed.isEmpty || positionApplying.trimmed.isEmpty {
            statusMessage = "Enter company and position for the cover letter."
            return false
        }

        return true
    }

    private func buildPrompt() -> String {
        var parts: [String] = [
            "Write the document as professional, concise, and interview-ready plain text.",
            "Applicant Name: \(name.trimmed)",
            "Email: \(email.trimmed)",
            "Phone: \(phone.trimmed)"
        ]

        if selectedType == .coverLetter {
            parts.append("Write a cover letter for this job application.")
            parts.append("Company: \(company.trimmed)")
            parts.append("Role: \(positionApplying.trimmed)")
            parts.append("Tone: \(selectedTone.lowercased())")
            if !experienceSummary.trimmed.isEmpty {
                parts.append("Relevant experience: \(experienceSummary.trimmed)")
            }
        } else {
            parts.append("Write a modern CV/Resume for the role: \(position.trimmed)")
            if !education.trimmed.isEmpty {
                parts.append("Education: \(education.trimmed)")
            }
            if !experience.trimmed.isEmpty {
                parts.append("Work experience: \(experience.trimmed)")
            }
            if !skills.trimmed.isEmpty {
                parts.append("Skills: \(skills.trimmed)")
            }
        }

        if let job = seed.job {
            parts.append("Match the document to this job posting when useful.")
            parts.append("Job title: \(job.title)")
            if !job.company.isEmpty {
                parts.append("Job company: \(job.company)")
            }
            if !job.location.isEmpty {
                parts.append("Job location: \(job.location)")
            }
            if !job.excerpt.isEmpty {
                parts.append("Job summary: \(job.excerpt)")
            }
        }

        if !notes.trimmed.isEmpty {
            parts.append("Extra notes: \(notes.trimmed)")
        }

        parts.append("Return only the final plain document text.")
        return parts.joined(separator: "\n")
    }

    private func outputBaseName() -> String {
        let subject = seed.job?.title.isEmpty == false ? seed.job?.title : name
        return "\(selectedType.filePrefix)_\(subject ?? selectedType.filePrefix)"
    }
}

private struct DocumentLibraryView: View {
    @ObservedObject var documentStore: GeneratedDocumentsStore
    let onSearch: () -> Void
    @State private var shareItem: ShareItem?

    var body: some View {
        List {
            if documentStore.documents.isEmpty {
                Text("No generated documents yet.")
                    .foregroundStyle(.secondary)
                NativeAdCardView(slot: 320)
            } else {
                ForEach(Array(documentStore.documents.enumerated()), id: \.element.id) { index, document in
                    VStack(alignment: .leading, spacing: 8) {
                        Text(document.title)
                            .font(.headline)
                        Text("\(document.format.title) - \(document.createdAt.formatted(date: .abbreviated, time: .shortened))")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        HStack {
                            Button {
                                if let url = documentStore.fileURL(for: document) {
                                    shareItem = ShareItem(items: [url])
                                }
                            } label: {
                                Label("Share", systemImage: "square.and.arrow.up")
                            }
                            .buttonStyle(.bordered)

                            Button(role: .destructive) {
                                documentStore.delete(document)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                    .padding(.vertical, 4)

                    if (index + 1).isMultiple(of: nativeAdInterval) {
                        NativeAdCardView(slot: 330 + index)
                            .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                    }
                }
            }
        }
        .navigationTitle("Documents")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: onSearch) {
                    Image(systemName: "magnifyingglass")
                }
                .accessibilityLabel("Search jobs")
            }
        }
        .sheet(item: $shareItem) { item in
            ShareSheet(items: item.items)
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            BottomBannerAdView()
        }
    }
}

private struct GlobalJobSearchView: View {
    @ObservedObject var jobsStore: JobsStore
    @Binding var query: String
    let openJob: (JobSummary) -> Void
    @Environment(\.dismiss) private var dismiss

    private var filteredJobs: [JobSummary] {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return jobsStore.jobs
        }
        return jobsStore.jobs.filter { job in
            job.title.localizedCaseInsensitiveContains(trimmed) ||
                job.company.localizedCaseInsensitiveContains(trimmed) ||
                job.location.localizedCaseInsensitiveContains(trimmed) ||
                job.excerpt.localizedCaseInsensitiveContains(trimmed) ||
                job.content.localizedCaseInsensitiveContains(trimmed)
        }
    }

    var body: some View {
        NavigationView {
            List {
                if jobsStore.isLoading && jobsStore.jobs.isEmpty {
                    ProgressView("Loading jobs")
                        .frame(maxWidth: .infinity, alignment: .center)
                } else if filteredJobs.isEmpty {
                    Text("No jobs found.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(Array(filteredJobs.enumerated()), id: \.element.id) { index, job in
                        Button {
                            dismiss()
                            openJob(job)
                        } label: {
                            JobRow(job: job)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        .buttonStyle(.plain)

                        if (index + 1).isMultiple(of: nativeAdInterval) {
                            NativeAdCardView(slot: 720 + index)
                                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                        }
                    }
                }

                if jobsStore.hasMore {
                    Button {
                        Task {
                            await jobsStore.loadMore()
                        }
                    } label: {
                        HStack {
                            Spacer()
                            if jobsStore.isLoadingMore {
                                ProgressView()
                            } else {
                                Label("Load More Jobs", systemImage: "arrow.down.circle")
                            }
                            Spacer()
                        }
                    }
                    .disabled(jobsStore.isLoadingMore)
                }

                BottomBannerAdView()
                    .listRowInsets(EdgeInsets(top: 8, leading: 0, bottom: 8, trailing: 0))
            }
            .navigationTitle("Search Jobs")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $query, prompt: "Search jobs")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                }
            }
            .task {
                await jobsStore.loadIfNeeded()
            }
        }
        .navigationViewStyle(.stack)
    }
}

@MainActor
private final class GeneratedDocumentsStore: ObservableObject {
    @Published private(set) var documents: [GeneratedDocumentRecord] = []

    private let defaultsKey = "generated_documents"

    init() {
        load()
    }

    func save(_ document: GeneratedDocumentRecord) {
        documents.insert(document, at: 0)
        persist()
    }

    func delete(_ document: GeneratedDocumentRecord) {
        if let url = fileURL(for: document) {
            try? FileManager.default.removeItem(at: url)
        }
        documents.removeAll { $0.id == document.id }
        persist()
    }

    func fileURL(for document: GeneratedDocumentRecord) -> URL? {
        let url = URL(fileURLWithPath: document.filePath)
        return FileManager.default.fileExists(atPath: url.path) ? url : nil
    }

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: defaultsKey),
              let decoded = try? JSONDecoder().decode([GeneratedDocumentRecord].self, from: data) else {
            documents = []
            return
        }
        documents = decoded
    }

    private func persist() {
        guard let data = try? JSONEncoder().encode(documents) else { return }
        UserDefaults.standard.set(data, forKey: defaultsKey)
    }
}

private enum TextDocumentGenerator {
    static func generate(prompt: String) async throws -> String {
        let models = TextGenerationConfig.models.isEmpty ? ["openai"] : TextGenerationConfig.models

        for model in models {
            do {
                return try await request(prompt: prompt, model: model)
            } catch {
                continue
            }
        }

        return LocalDocumentTemplateGenerator.generate(prompt: prompt)
    }

    private static func request(prompt: String, model: String) async throws -> String {
        var allowed = CharacterSet.urlPathAllowed
        allowed.remove(charactersIn: "/?#[]@!$&'()*+,;=")
        guard let encodedPrompt = prompt.addingPercentEncoding(withAllowedCharacters: allowed) else {
            throw URLError(.badURL)
        }

        let base = TextGenerationConfig.endpoint.hasSuffix("/")
            ? TextGenerationConfig.endpoint
            : "\(TextGenerationConfig.endpoint)/"
        guard let url = URL(string: "\(base)\(model)/\(encodedPrompt)") else {
            throw URLError(.badURL)
        }

        let (data, response) = try await URLSession.shared.data(from: url)
        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode),
              let text = String(data: data, encoding: .utf8)?.trimmed,
              !text.isEmpty,
              !text.lowercased().hasPrefix("<!doctype html"),
              !text.lowercased().hasPrefix("<html"),
              !text.contains("\"error\"") else {
            throw URLError(.badServerResponse)
        }

        return text
    }
}

private enum LocalDocumentTemplateGenerator {
    static func generate(prompt: String) -> String {
        let fields = parseFields(prompt: prompt)
        if prompt.localizedCaseInsensitiveContains("cover letter") {
            return buildCoverLetter(fields: fields)
        }
        return buildCV(fields: fields)
    }

    private static func parseFields(prompt: String) -> [String: String] {
        var fields: [String: String] = [:]
        for line in prompt.components(separatedBy: .newlines) {
            guard let separator = line.firstIndex(of: ":"),
                  separator < line.index(before: line.endIndex) else {
                continue
            }
            let key = String(line.prefix(upTo: separator)).trimmed.lowercased()
            let value = clean(String(line.suffix(from: line.index(after: separator))))
            if !key.isEmpty, !value.isEmpty {
                fields[key] = value
            }
        }
        return fields
    }

    private static func buildCV(fields: [String: String]) -> String {
        let name = field(fields, "applicant name").nonEmpty ?? "Applicant"
        let email = field(fields, "email")
        let phone = field(fields, "phone")
        let role = field(fields, "write a modern cv/resume for the role", "job title", "role")
            .nonEmpty ?? "the target role"
        let education = field(fields, "education")
        let experience = field(fields, "work experience", "relevant experience")
        let skills = splitList(field(fields, "skills"))
        let notes = field(fields, "extra notes")
        let jobSummary = field(fields, "job summary")
        let jobLocation = field(fields, "job location")
        let contactLine = [email, phone].filter { !$0.isEmpty }.joined(separator: " | ")

        var lines: [String] = [name]
        if !contactLine.isEmpty {
            lines.append(contactLine)
        }
        lines.append(contentsOf: [
            "",
            "Professional Summary",
            "Interview-ready candidate targeting \(role) with a practical background aligned to the needs of the position."
        ])

        if !experience.isEmpty {
            lines.append("Experience focus: \(experience)")
        } else if !jobSummary.isEmpty {
            lines.append("Role focus: \(jobSummary)")
        }
        if !jobLocation.isEmpty {
            lines.append("Preferred location: \(jobLocation)")
        }

        lines.append(contentsOf: ["", "Key Skills"])
        if skills.isEmpty {
            lines.append(contentsOf: [
                "- Clear communication and professional conduct",
                "- Problem solving and reliable task execution",
                "- Team collaboration and customer-focused service"
            ])
        } else {
            lines.append(contentsOf: skills.map { "- \($0)" })
        }

        lines.append(contentsOf: [
            "",
            "Work Experience",
            experience.isEmpty
                ? "Relevant experience can be tailored further with previous employer names, dates, and measurable achievements."
                : experience,
            "",
            "Education",
            education.isEmpty ? "Education details available on request." : education
        ])

        if !notes.isEmpty {
            lines.append(contentsOf: ["", "Additional Information", notes])
        }

        lines.append(contentsOf: ["", "References", "Available on request."])
        return lines.joined(separator: "\n").trimmed
    }

    private static func buildCoverLetter(fields: [String: String]) -> String {
        let name = field(fields, "applicant name").nonEmpty ?? "Applicant"
        let email = field(fields, "email")
        let phone = field(fields, "phone")
        let company = field(fields, "company", "job company").nonEmpty ?? "your organisation"
        let role = field(fields, "role", "job title", "write a modern cv/resume for the role")
            .nonEmpty ?? "the advertised position"
        let experience = field(fields, "relevant experience", "work experience")
        let notes = field(fields, "extra notes")
        let jobSummary = field(fields, "job summary")
        let tone = field(fields, "tone").replacingOccurrences(of: "_", with: " ").nonEmpty ?? "professional"
        let contactLine = [email, phone].filter { !$0.isEmpty }.joined(separator: " | ")

        var lines: [String] = [name]
        if !contactLine.isEmpty {
            lines.append(contactLine)
        }
        lines.append(contentsOf: [
            "",
            "Dear Hiring Manager,",
            "",
            "I am writing to apply for the \(role) position at \(company). I am interested in this opportunity because it matches my skills, work ethic, and commitment to delivering dependable results.",
            ""
        ])

        if !experience.isEmpty {
            lines.append("My relevant experience includes \(experience). This background has prepared me to contribute quickly, communicate clearly, and handle responsibilities with care.")
        } else {
            lines.append("I bring a strong willingness to learn, a professional approach to work, and the discipline needed to contribute effectively in this role.")
        }
        if !jobSummary.isEmpty {
            lines.append(contentsOf: [
                "",
                "Based on the job details, I understand that the role requires focus in areas such as \(jobSummary)."
            ])
        }
        if !notes.isEmpty {
            lines.append(contentsOf: ["", notes])
        }

        lines.append(contentsOf: [
            "",
            "I would welcome the opportunity to discuss how my background and \(tone) approach can support \(company). Thank you for considering my application.",
            "",
            "Sincerely,",
            name
        ])
        return lines.joined(separator: "\n").trimmed
    }

    private static func field(_ fields: [String: String], _ keys: String...) -> String {
        for key in keys {
            if let value = fields[key], !value.isEmpty {
                return clean(value)
            }
        }
        return ""
    }

    private static func splitList(_ raw: String) -> [String] {
        raw.components(separatedBy: CharacterSet(charactersIn: ",;\n"))
            .map(clean)
            .filter { !$0.isEmpty }
    }

    private static func clean(_ value: String) -> String {
        value
            .replacingOccurrences(of: "<[^>]*>", with: " ", options: .regularExpression)
            .replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmed
    }
}

private enum GeneratedDocumentFileWriter {
    static func write(text: String, format: GeneratedDocumentFormat, baseName: String) throws -> URL {
        let documentsDirectory = try FileManager.default.url(
            for: .documentDirectory,
            in: .userDomainMask,
            appropriateFor: nil,
            create: true
        )
        let safeBaseName = sanitized(baseName)
        let timestamp = Int(Date().timeIntervalSince1970)
        let fileURL = documentsDirectory
            .appendingPathComponent("\(safeBaseName)_\(timestamp)")
            .appendingPathExtension(format.fileExtension)

        switch format {
        case .text:
            try text.write(to: fileURL, atomically: true, encoding: .utf8)
        case .pdf:
            try writePDF(text: text, to: fileURL)
        }

        return fileURL
    }

    private static func sanitized(_ value: String) -> String {
        let allowed = CharacterSet.alphanumerics.union(CharacterSet(charactersIn: "_-"))
        let scalars = value.replacingOccurrences(of: " ", with: "_").unicodeScalars.map { scalar in
            allowed.contains(scalar) ? Character(scalar) : "_"
        }
        let result = String(scalars).trimmingCharacters(in: CharacterSet(charactersIn: "_"))
        return result.isEmpty ? "Generated_Document" : result
    }

    private static func writePDF(text: String, to url: URL) throws {
        let pageBounds = CGRect(x: 0, y: 0, width: 612, height: 792)
        let margin: CGFloat = 48
        let renderer = UIGraphicsPDFRenderer(bounds: pageBounds)
        let attributes: [NSAttributedString.Key: Any] = [
            .font: UIFont.systemFont(ofSize: 12),
            .foregroundColor: UIColor.label
        ]
        let availableWidth = pageBounds.width - (margin * 2)
        let availableHeight = pageBounds.height - margin

        try renderer.writePDF(to: url) { context in
            context.beginPage()
            var y = margin

            for paragraph in text.components(separatedBy: .newlines) {
                let line = paragraph.isEmpty ? " " : paragraph
                let attributed = NSAttributedString(string: "\(line)\n", attributes: attributes)
                let height = ceil(
                    attributed.boundingRect(
                        with: CGSize(width: availableWidth, height: .greatestFiniteMagnitude),
                        options: [.usesLineFragmentOrigin, .usesFontLeading],
                        context: nil
                    ).height
                )

                if y + height > availableHeight {
                    context.beginPage()
                    y = margin
                }

                attributed.draw(
                    with: CGRect(x: margin, y: y, width: availableWidth, height: height),
                    options: [.usesLineFragmentOrigin, .usesFontLeading],
                    context: nil
                )
                y += height + 4
            }
        }
    }
}

private struct ShareItem: Identifiable {
    let id = UUID()
    let items: [Any]
}

private struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}

private extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }

    var nonEmpty: String? {
        let value = trimmed
        return value.isEmpty ? nil : value
    }
}
