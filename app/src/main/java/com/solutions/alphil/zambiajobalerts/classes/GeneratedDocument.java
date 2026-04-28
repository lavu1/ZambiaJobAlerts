package com.solutions.alphil.zambiajobalerts.classes;

import androidx.annotation.NonNull;

public class GeneratedDocument {

    private String id;
    private int sourceJobId;
    private String sourceJobTitle;
    private String sourceCompany;
    private String documentType;
    private String format;
    private String fileName;
    private String filePath;
    private long createdAt;

    public GeneratedDocument() {
    }

    public GeneratedDocument(String id, int sourceJobId, String sourceJobTitle, String sourceCompany,
                            String documentType, String format, String fileName, String filePath, long createdAt) {
        this.id = id;
        this.sourceJobId = sourceJobId;
        this.sourceJobTitle = sourceJobTitle;
        this.sourceCompany = sourceCompany;
        this.documentType = documentType;
        this.format = format;
        this.fileName = fileName;
        this.filePath = filePath;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getSourceJobId() {
        return sourceJobId;
    }

    public void setSourceJobId(int sourceJobId) {
        this.sourceJobId = sourceJobId;
    }

    public String getSourceJobTitle() {
        return sourceJobTitle;
    }

    public void setSourceJobTitle(String sourceJobTitle) {
        this.sourceJobTitle = sourceJobTitle;
    }

    public String getSourceCompany() {
        return sourceCompany;
    }

    public void setSourceCompany(String sourceCompany) {
        this.sourceCompany = sourceCompany;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getDisplayLabel() {
        String typeLabel = "Cover Letter".equalsIgnoreCase(documentType) ? "Cover Letter" : "CV/Resume";
        return typeLabel + " (" + format.toUpperCase() + ")";
    }

    public String getDisplaySource() {
        if (sourceJobTitle == null || sourceJobTitle.isEmpty()) {
            return "Manual generation";
        }

        if (sourceCompany != null && !sourceCompany.isEmpty()) {
            return sourceJobTitle + " · " + sourceCompany;
        }

        return sourceJobTitle;
    }

    @NonNull
    @Override
    public String toString() {
        return "GeneratedDocument{" +
                "id='" + id + '\'' +
                ", sourceJobId=" + sourceJobId +
                ", sourceJobTitle='" + sourceJobTitle + '\'' +
                ", sourceCompany='" + sourceCompany + '\'' +
                ", documentType='" + documentType + '\'' +
                ", format='" + format + '\'' +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
