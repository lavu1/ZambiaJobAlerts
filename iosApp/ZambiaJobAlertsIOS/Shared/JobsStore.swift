import Foundation

private let jobsConnectionMessage = "Unable to load jobs right now. Please refresh, check your internet connection, or switch networks and try again."

@MainActor
final class JobsStore: ObservableObject {
    @Published private(set) var jobs: [JobSummary] = []
    @Published private(set) var isLoading = false
    @Published private(set) var isLoadingMore = false
    @Published private(set) var hasMore = true
    @Published private(set) var error: String?

    private let endpoint = URL(string: "https://zambiajobalerts.com/wp-json/wp/v2/job-listings")!
    private let pageSize = 30
    private var currentPage = 0

    func loadIfNeeded() async {
        guard jobs.isEmpty else { return }
        await reload()
    }

    func reload() async {
        isLoading = true
        error = nil
        defer { isLoading = false }

        do {
            let result = try await fetchJobs(page: 1)
            jobs = result.jobs
            currentPage = 1
            hasMore = result.hasMore
        } catch {
            self.error = jobsConnectionMessage
        }
    }

    @discardableResult
    func loadMore() async -> JobSummary? {
        guard !isLoading, !isLoadingMore, hasMore else { return nil }

        isLoadingMore = true
        error = nil
        defer { isLoadingMore = false }

        var nextPage = max(currentPage + 1, 1)
        var attempts = 0
        do {
            while attempts < 4, hasMore {
                attempts += 1
                let result = try await fetchJobs(page: nextPage)
                let firstNewJob = appendUnique(result.jobs)
                currentPage = nextPage
                hasMore = result.hasMore

                if let firstNewJob {
                    return firstNewJob
                }
                if result.jobs.isEmpty {
                    return nil
                }
                nextPage += 1
            }
            return nil
        } catch {
            self.error = jobsConnectionMessage
            return nil
        }
    }

    func job(identifier: String) async -> JobSummary? {
        if let id = Int(identifier),
           let existing = jobs.first(where: { $0.id == id }) {
            return existing
        }
        if let existing = jobs.first(where: { $0.slug == identifier }) {
            return existing
        }

        do {
            if Int(identifier) != nil {
                return try await fetchJobById(identifier)
            }
            return try await fetchJobBySlug(identifier)
        } catch {
            self.error = jobsConnectionMessage
            return nil
        }
    }

    private func fetchJobs(page: Int) async throws -> JobsPageResult {
        var components = URLComponents(url: endpoint, resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "per_page", value: "\(pageSize)"),
            URLQueryItem(name: "page", value: "\(page)"),
            URLQueryItem(name: "_embed", value: "1")
        ]
        let (data, response) = try await loadData(url: components.url!)
        guard let array = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return JobsPageResult(jobs: [], hasMore: false)
        }
        let jobs = array.compactMap(JobSummary.init(json:))
        let totalPages = Int(response.value(forHTTPHeaderField: "X-WP-TotalPages") ?? "")
        let hasMore = totalPages.map { page < $0 } ?? (jobs.count >= pageSize)
        return JobsPageResult(jobs: jobs, hasMore: hasMore)
    }

    private func fetchJobById(_ id: String) async throws -> JobSummary? {
        let url = endpoint.appendingPathComponent(id)
        var components = URLComponents(url: url, resolvingAgainstBaseURL: false)!
        components.queryItems = [URLQueryItem(name: "_embed", value: "1")]
        let (data, _) = try await loadData(url: components.url!)
        guard let object = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        return JobSummary(json: object)
    }

    private func fetchJobBySlug(_ slug: String) async throws -> JobSummary? {
        var components = URLComponents(url: endpoint, resolvingAgainstBaseURL: false)!
        components.queryItems = [
            URLQueryItem(name: "slug", value: slug),
            URLQueryItem(name: "_embed", value: "1")
        ]
        let (data, _) = try await loadData(url: components.url!)
        guard let array = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return nil
        }
        return array.compactMap(JobSummary.init(json:)).first
    }

    private func loadData(url: URL) async throws -> (Data, HTTPURLResponse) {
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("ZambiaJobAlerts/2.8 iOS", forHTTPHeaderField: "User-Agent")

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        if http.statusCode == 403 {
            throw JobsStoreError.serverProtection
        }
        guard (200...299).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }
        if looksLikeHtmlChallenge(data) {
            throw JobsStoreError.serverProtection
        }
        return (data, http)
    }

    private func appendUnique(_ newJobs: [JobSummary]) -> JobSummary? {
        var seen = Set(jobs.map(\.id))
        var firstNewJob: JobSummary?
        for job in newJobs where !seen.contains(job.id) {
            if firstNewJob == nil {
                firstNewJob = job
            }
            jobs.append(job)
            seen.insert(job.id)
        }
        return firstNewJob
    }

    private func looksLikeHtmlChallenge(_ data: Data) -> Bool {
        guard let value = String(data: data.prefix(512), encoding: .utf8) else {
            return false
        }
        let lowered = value.lowercased()
        return lowered.hasPrefix("<!doctype html") ||
            lowered.hasPrefix("<html") ||
            lowered.contains("checking your browser before accessing") ||
            lowered.contains("/hcdn-cgi/jschallenge")
    }

}

private struct JobsPageResult {
    let jobs: [JobSummary]
    let hasMore: Bool
}

private enum JobsStoreError: LocalizedError {
    case serverProtection

    var errorDescription: String? {
        switch self {
        case .serverProtection:
            return jobsConnectionMessage
        }
    }
}

private extension JobSummary {
    init?(json: [String: Any]) {
        guard let id = json["id"] as? Int else { return nil }

        self.id = id
        slug = (json["slug"] as? String) ?? ""
        title = JobSummary.rendered("title", in: json).cleanHTML
        excerpt = (((json["uagb_excerpt"] as? String) ?? JobSummary.rendered("excerpt", in: json))).cleanHTML
        content = JobSummary.rendered("content", in: json).cleanHTML
        date = (json["date"] as? String) ?? ""
        link = (json["link"] as? String) ?? ""
        featuredImage = JobSummary.featuredImage(in: json)

        let meta = json["meta"] as? [String: Any]
        company = JobSummary.string("_company_name", in: meta)
        location = JobSummary.string("_job_location", in: meta)
        applicationLink = JobSummary.string("_application", in: meta)
        jobType = JobSummary.jobType(in: json)
    }

    static func rendered(_ key: String, in json: [String: Any]) -> String {
        guard let object = json[key] as? [String: Any] else { return "" }
        return (object["rendered"] as? String) ?? ""
    }

    static func string(_ key: String, in dictionary: [String: Any]?) -> String {
        guard let value = dictionary?[key] else { return "" }
        if let string = value as? String {
            return string
        }
        return String(describing: value)
    }

    static func featuredImage(in json: [String: Any]) -> String {
        guard let embedded = json["_embedded"] as? [String: Any],
              let media = embedded["wp:featuredmedia"] as? [[String: Any]],
              let first = media.first else {
            return ""
        }
        return (first["source_url"] as? String) ?? ""
    }

    static func jobType(in json: [String: Any]) -> String {
        guard let embedded = json["_embedded"] as? [String: Any],
              let terms = embedded["wp:term"] as? [[[String: Any]]] else {
            return ""
        }
        for group in terms {
            for term in group {
                if term["taxonomy"] as? String == "job_listing_type" {
                    return (term["name"] as? String) ?? ""
                }
            }
        }
        return ""
    }
}

private extension String {
    var cleanHTML: String {
        guard let data = data(using: .utf8),
              let attributed = try? NSAttributedString(
                data: data,
                options: [
                    .documentType: NSAttributedString.DocumentType.html,
                    .characterEncoding: String.Encoding.utf8.rawValue
                ],
                documentAttributes: nil
              ) else {
            return trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return attributed.string
            .replacingOccurrences(of: "\n\n\n", with: "\n\n")
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
