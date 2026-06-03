package com.solutions.alphil.zambiajobalerts.shared

enum class SharedServiceType(
    val title: String,
    val creditCost: Int,
    val requestType: String,
    val dayValue: String,
) {
    EMAIL_ALERTS("Email Alerts", 1, "Share me Jobs", "1"),
    PHONE_ALERTS("Phone Alerts", 1, "Share me Jobs", "2"),
    PRIORITY_APPLICATION("Priority Application", 3, "Priority Job Application", "0"),
    CV_REVIEW("CV Review", 5, "cv_review", ""),
    CV_WRITE("CV Writing", 5, "Write CV", ""),
    CAREER_COACHING("Career Coaching", 7, "Career Coaching", "0"),
}
