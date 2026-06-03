import Foundation

struct JobSummary: Identifiable, Hashable, Codable {
    let id: Int
    let slug: String
    let title: String
    let excerpt: String
    let content: String
    let date: String
    let link: String
    let featuredImage: String
    let company: String
    let location: String
    let jobType: String
    let applicationLink: String

    var webURL: URL? {
        URL(string: link)
    }

    var applicationURL: URL? {
        URL(string: applicationLink)
    }
}

struct SharedService: Identifiable {
    let id: String
    let title: String
    let creditCost: Int

    static let all: [SharedService] = [
        SharedService(id: "email_alerts", title: "Email Alerts", creditCost: 1),
        SharedService(id: "phone_alerts", title: "Phone Alerts", creditCost: 1),
        SharedService(id: "priority_application", title: "Priority Application", creditCost: 3),
        SharedService(id: "cv_review", title: "CV Review", creditCost: 5),
        SharedService(id: "cv_write", title: "CV Writing", creditCost: 5),
        SharedService(id: "career_coaching", title: "Career Coaching", creditCost: 7)
    ]
}

enum GeneratedDocumentType: String, CaseIterable, Identifiable, Codable {
    case cv
    case coverLetter

    var id: String { rawValue }

    var title: String {
        switch self {
        case .cv:
            return "CV/Resume"
        case .coverLetter:
            return "Cover Letter"
        }
    }

    var filePrefix: String {
        switch self {
        case .cv:
            return "CV"
        case .coverLetter:
            return "Cover_Letter"
        }
    }
}

enum GeneratedDocumentFormat: String, CaseIterable, Identifiable, Codable {
    case text
    case pdf

    var id: String { rawValue }

    var title: String {
        switch self {
        case .text:
            return "Text"
        case .pdf:
            return "PDF"
        }
    }

    var fileExtension: String {
        switch self {
        case .text:
            return "txt"
        case .pdf:
            return "pdf"
        }
    }
}

struct DocumentGeneratorSeed: Identifiable, Hashable {
    let id = UUID()
    let type: GeneratedDocumentType
    let job: JobSummary?

    static let empty = DocumentGeneratorSeed(type: .cv, job: nil)
}

struct GeneratedDocumentRecord: Identifiable, Codable, Hashable {
    let id: UUID
    let jobId: Int?
    let jobTitle: String
    let company: String
    let type: GeneratedDocumentType
    let format: GeneratedDocumentFormat
    let fileName: String
    let filePath: String
    let createdAt: Date

    var title: String {
        let base = jobTitle.isEmpty ? type.title : jobTitle
        return "\(type.title): \(base)"
    }
}
