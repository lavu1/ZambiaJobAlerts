package com.solutions.alphil.zambiajobalerts.classes

class GeneratedDocument {
    var id: String? = null
    var sourceJobId: Int = 0
    var sourceJobTitle: String? = null
    var sourceCompany: String? = null
    var documentType: String? = null
    var format: String? = null
    var fileName: String? = null
    var filePath: String? = null
    var createdAt: Long = 0

    constructor()

    constructor(
        id: String?,
        sourceJobId: Int,
        sourceJobTitle: String?,
        sourceCompany: String?,
        documentType: String?,
        format: String?,
        fileName: String?,
        filePath: String?,
        createdAt: Long,
    ) {
        this.id = id
        this.sourceJobId = sourceJobId
        this.sourceJobTitle = sourceJobTitle
        this.sourceCompany = sourceCompany
        this.documentType = documentType
        this.format = format
        this.fileName = fileName
        this.filePath = filePath
        this.createdAt = createdAt
    }

    fun getDisplayLabel(): String {
        val typeLabel = if ("Cover Letter".equals(documentType, ignoreCase = true)) {
            "Cover Letter"
        } else {
            "CV/Resume"
        }
        return "$typeLabel (${format.orEmpty().uppercase()})"
    }

    fun getDisplaySource(): String {
        val title = sourceJobTitle
        if (title.isNullOrEmpty()) {
            return "Manual generation"
        }

        val company = sourceCompany
        return if (!company.isNullOrEmpty()) {
            "$title · $company"
        } else {
            title
        }
    }

    override fun toString(): String =
        "GeneratedDocument{" +
            "id='$id'" +
            ", sourceJobId=$sourceJobId" +
            ", sourceJobTitle='$sourceJobTitle'" +
            ", sourceCompany='$sourceCompany'" +
            ", documentType='$documentType'" +
            ", format='$format'" +
            ", fileName='$fileName'" +
            ", filePath='$filePath'" +
            ", createdAt=$createdAt" +
            '}'
}
