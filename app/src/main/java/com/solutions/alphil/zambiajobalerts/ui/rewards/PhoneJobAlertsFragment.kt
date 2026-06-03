package com.solutions.alphil.zambiajobalerts.ui.rewards

import androidx.compose.ui.text.input.KeyboardType
import okhttp3.FormBody

class PhoneJobAlertsFragment : RewardRequestFragment() {
    override val screenTitle: String = "Phone Job Alerts"
    override val description: String = "Receive job alerts by phone for the next two days."
    override val successToast: String = "Phone Job Alerts Activated for the next two days"
    override val fields: List<RequestField> = listOf(
        RequestField("phone", "Phone", required = true, keyboardType = KeyboardType.Phone),
        RequestField("job_category", "Job Category"),
    )

    override fun buildFormBody(values: Map<String, String>): FormBody =
        backendForm(
            type = "Share me Jobs",
            days = "2",
            phone = values["phone"].orEmpty(),
            notes = values["job_category"].orEmpty(),
        )
}
