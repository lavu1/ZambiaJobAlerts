package com.solutions.alphil.zambiajobalerts.ui.rewards

import androidx.compose.ui.text.input.KeyboardType
import okhttp3.FormBody

class CareerCoachingFragment : RewardRequestFragment() {
    override val screenTitle: String = "Career Coaching"
    override val description: String = "Request a career coaching session with your profile details."
    override val successToast: String = "Career Coaching Request Submitted!"
    override val fields: List<RequestField> = listOf(
        RequestField("name", "Name", required = true),
        RequestField("email", "Email", required = true, keyboardType = KeyboardType.Email),
        RequestField("career_profile", "Career Profile", multiline = true),
    )

    override fun buildFormBody(values: Map<String, String>): FormBody =
        backendForm(
            type = "Career Coaching",
            days = "0",
            name = values["name"].orEmpty(),
            email = values["email"].orEmpty(),
            notes = values["career_profile"].orEmpty(),
        )
}
