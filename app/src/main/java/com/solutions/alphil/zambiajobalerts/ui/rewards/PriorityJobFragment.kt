package com.solutions.alphil.zambiajobalerts.ui.rewards

import androidx.compose.ui.text.input.KeyboardType
import okhttp3.FormBody

class PriorityJobFragment : RewardRequestFragment() {
    override val screenTitle: String = "Priority Job Application"
    override val description: String = "Send your priority application request to the team."
    override val successToast: String = "Priority Job Application Submitted!"
    override val fields: List<RequestField> = listOf(
        RequestField("email", "Email", required = true, keyboardType = KeyboardType.Email),
        RequestField("job_category", "Job Category"),
    )

    override fun buildFormBody(values: Map<String, String>): FormBody =
        backendForm(
            type = "Priority Job Application",
            days = "0",
            email = values["email"].orEmpty(),
            notes = values["job_category"].orEmpty(),
        )
}
