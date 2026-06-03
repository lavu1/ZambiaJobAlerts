package com.solutions.alphil.zambiajobalerts.ui.rewards

import androidx.compose.ui.text.input.KeyboardType
import okhttp3.FormBody

class CVWriteFragment : RewardRequestFragment() {
    override val screenTitle: String = "CV Write"
    override val description: String = "Request a CV written from scratch."
    override val successToast: String = "CV Write Request Submitted!"
    override val fields: List<RequestField> = listOf(
        RequestField("name", "Name", required = true),
        RequestField("email", "Email", required = true, keyboardType = KeyboardType.Email),
        RequestField("phone", "Phone", required = true, keyboardType = KeyboardType.Phone),
        RequestField("education", "Education Background", multiline = true),
        RequestField("work", "Work Experience", multiline = true),
        RequestField("skills", "Skills", multiline = true),
        RequestField("notes", "Additional Notes", multiline = true),
    )

    override fun buildFormBody(values: Map<String, String>): FormBody =
        backendForm(
            type = "Write CV",
            days = "",
            name = values["name"].orEmpty(),
            email = values["email"].orEmpty(),
            phone = values["phone"].orEmpty(),
            education = values["education"].orEmpty(),
            work = values["work"].orEmpty(),
            skills = values["skills"].orEmpty(),
            notes = values["notes"].orEmpty(),
        )
}
