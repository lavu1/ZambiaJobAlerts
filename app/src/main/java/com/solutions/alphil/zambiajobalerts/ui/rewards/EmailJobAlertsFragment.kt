package com.solutions.alphil.zambiajobalerts.ui.rewards

import androidx.compose.ui.text.input.KeyboardType
import okhttp3.FormBody

class EmailJobAlertsFragment : RewardRequestFragment() {
    override val screenTitle: String = "Email Job Alerts"
    override val description: String = "Receive job alerts by email for your selected job category."
    override val successToast: String = "Email Job Alerts Activated!"
    override val fields: List<RequestField> = listOf(
        RequestField("email", "Email", required = true, keyboardType = KeyboardType.Email),
        RequestField("job_category", "Job Category"),
    )

    override fun buildFormBody(values: Map<String, String>): FormBody =
        backendForm(
            type = "Share me Jobs",
            days = "1",
            email = values["email"].orEmpty(),
            notes = values["job_category"].orEmpty(),
        )
}
