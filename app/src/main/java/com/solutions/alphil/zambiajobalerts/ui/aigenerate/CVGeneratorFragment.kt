package com.solutions.alphil.zambiajobalerts.ui.aigenerate

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.solutions.alphil.zambiajobalerts.AppViewIds
import com.solutions.alphil.zambiajobalerts.R
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocument
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocumentStore
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocumentWriter
import com.solutions.alphil.zambiajobalerts.classes.ModelFallbackGenerator
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID

class CVGeneratorFragment : Fragment() {
    private val documentTypes = arrayOf("CV/Resume", "Cover Letter")
    private val documentFormats = arrayOf("PDF Document", "Word Document", "Text Only")
    private val letterTones = arrayOf("Formal", "Professional", "Confident", "Friendly", "Enthusiastic")

    private var selectedType by mutableStateOf(ARG_CV_TYPE)
    private var selectedFormat by mutableStateOf("pdf")
    private var selectedTone by mutableStateOf("formal")

    private var name by mutableStateOf("")
    private var email by mutableStateOf("")
    private var phone by mutableStateOf("")
    private var position by mutableStateOf("")
    private var education by mutableStateOf("")
    private var experience by mutableStateOf("")
    private var skills by mutableStateOf("")
    private var company by mutableStateOf("")
    private var positionApplying by mutableStateOf("")
    private var experienceSummary by mutableStateOf("")
    private var addNotes by mutableStateOf("")

    private var isGenerating by mutableStateOf(false)
    private var resultText by mutableStateOf("")
    private var hasResult by mutableStateOf(false)

    private var loadingDialog: AlertDialog? = null
    private var rewardedAd: RewardedAd? = null
    private var pendingGenerateAfterAd = false

    private var sourceJobId = -1
    private var sourceJobTitle = ""
    private var sourceCompany = ""

    private var currentFilePath = ""
    private var currentTextResult = ""

    private lateinit var documentStore: GeneratedDocumentStore
    private lateinit var documentWriter: GeneratedDocumentWriter
    private lateinit var modelGenerator: ModelFallbackGenerator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        documentStore = GeneratedDocumentStore(requireContext())
        documentWriter = GeneratedDocumentWriter(requireContext())
        modelGenerator = ModelFallbackGenerator()
        applyPrefillArguments()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    CVGeneratorScreen(
                        selectedTypeLabel = selectedTypeLabel(),
                        selectedFormatLabel = selectedFormatLabel(),
                        selectedToneLabel = selectedToneLabel(),
                        selectedType = selectedType,
                        name = name,
                        email = email,
                        phone = phone,
                        position = position,
                        education = education,
                        experience = experience,
                        skills = skills,
                        company = company,
                        positionApplying = positionApplying,
                        experienceSummary = experienceSummary,
                        addNotes = addNotes,
                        isGenerating = isGenerating,
                        resultText = resultText,
                        hasResult = hasResult,
                        onSelectType = { showDocumentTypeDialog() },
                        onSelectFormat = { showFormatDialog() },
                        onSelectTone = { showToneDialog() },
                        onNameChange = { name = it },
                        onEmailChange = { email = it },
                        onPhoneChange = { phone = it },
                        onPositionChange = { position = it },
                        onEducationChange = { education = it },
                        onExperienceChange = { experience = it },
                        onSkillsChange = { skills = it },
                        onCompanyChange = { company = it },
                        onPositionApplyingChange = { positionApplying = it },
                        onExperienceSummaryChange = { experienceSummary = it },
                        onAddNotesChange = { addNotes = it },
                        onGenerate = { validateAndSubmit() },
                        onOpenLibrary = { openLibrary() },
                        onCopy = { copyCurrentTextToClipboard() },
                        onDownload = { downloadCurrentFile() },
                    )
                }
            }
        }
    }

    private fun applyPrefillArguments() {
        val args = arguments ?: return

        sourceJobId = args.getInt(ARG_SOURCE_JOB_ID, -1)
        sourceJobTitle = args.getString(ARG_SOURCE_JOB_TITLE, "")
        sourceCompany = args.getString(ARG_SOURCE_COMPANY, "")
        val prefillType = args.getString(ARG_PREFILL_TYPE, "")

        if (ARG_COVER_TYPE == prefillType) {
            selectedType = ARG_COVER_TYPE
            if (sourceCompany.isNotEmpty()) {
                company = sourceCompany
            }
            if (sourceJobTitle.isNotEmpty()) {
                positionApplying = sourceJobTitle
            }
        } else {
            selectedType = ARG_CV_TYPE
            if (sourceJobTitle.isNotEmpty()) {
                position = sourceJobTitle
            }
        }

        args.getString("name")?.takeIf { it.isNotBlank() }?.let { name = it }
        args.getString("email")?.takeIf { it.isNotBlank() }?.let { email = it }
        args.getString("phone")?.takeIf { it.isNotBlank() }?.let { phone = it }
    }

    private fun showDocumentTypeDialog() {
        val currentIndex = if (ARG_CV_TYPE == selectedType) 0 else 1

        AlertDialog.Builder(requireContext())
            .setTitle("Select Document Type")
            .setSingleChoiceItems(documentTypes, currentIndex) { dialog, which ->
                selectedType = if (which == 0) ARG_CV_TYPE else ARG_COVER_TYPE
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showFormatDialog() {
        val currentIndex = when (selectedFormat) {
            "pdf" -> 0
            "docx" -> 1
            else -> 2
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Select Document Format")
            .setSingleChoiceItems(documentFormats, currentIndex) { dialog, which ->
                selectedFormat = when (which) {
                    0 -> "pdf"
                    1 -> "docx"
                    else -> "txt"
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showToneDialog() {
        val currentIndex = when (selectedTone) {
            "professional" -> 1
            "confident" -> 2
            "friendly" -> 3
            "enthusiastic" -> 4
            else -> 0
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Select Writing Tone")
            .setSingleChoiceItems(letterTones, currentIndex) { dialog, which ->
                selectedTone = letterTones[which].lowercase(Locale.ROOT).replace(" ", "_")
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun validateAndSubmit() {
        if (name.isBlank() || email.isBlank() || phone.isBlank()) {
            Toast.makeText(context, "Please fill all required fields!", Toast.LENGTH_SHORT).show()
            return
        }

        if (ARG_CV_TYPE == selectedType && position.isBlank()) {
            Toast.makeText(context, "Please enter position/job title!", Toast.LENGTH_SHORT).show()
            return
        }

        if (ARG_COVER_TYPE == selectedType && (company.isBlank() || positionApplying.isBlank())) {
            Toast.makeText(context, "Please enter company and position!", Toast.LENGTH_SHORT).show()
            return
        }

        isGenerating = true
        showLoadingDialog("Loading ad, please wait...")
        loadRewardedAd()
    }

    private fun generateDocument() {
        showLoadingDialog("Generating document...")
        val prompt = buildPrompt()
        modelGenerator.generateText(
            prompt,
            object : ModelFallbackGenerator.GenerationListener {
                override fun onSuccess(generatedText: String) {
                    if (!isAdded) return
                    requireActivity().runOnUiThread { saveGeneratedDocument(generatedText) }
                }

                override fun onFailure(message: String) {
                    if (!isAdded) return
                    requireActivity().runOnUiThread {
                        dismissLoadingDialog()
                        isGenerating = false
                        showResult(
                            "GENERATION FAILED\n\n" +
                                "We could not generate the document right now. Please refresh, check your connection, or try again shortly.",
                        )
                        Toast.makeText(context, "Generation failed", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
    }

    private fun buildPrompt(): String {
        val prompt = StringBuilder()

        prompt.append("Write the document as a professional, concise, and interview-ready text.\n\n")
        prompt.append("Applicant Name: ").append(name.trim()).append("\n")
        prompt.append("Email: ").append(email.trim()).append("\n")
        prompt.append("Phone: ").append(phone.trim()).append("\n\n")

        if (ARG_COVER_TYPE == selectedType) {
            prompt.append("Write a cover letter for this job application.\n")
            prompt.append("Company: ").append(company.trim()).append("\n")
            prompt.append("Role: ").append(positionApplying.trim()).append("\n")
            prompt.append("Tone: ").append(selectedTone).append("\n\n")
            if (experienceSummary.isNotBlank()) {
                prompt.append("Relevant experience: ").append(experienceSummary.trim()).append("\n")
            }

            if (sourceJobId != -1 && sourceCompany.isNotEmpty()) {
                prompt.append("Match it to the job details for: ").append(sourceCompany).append(" role")
                if (sourceJobTitle.isNotEmpty()) {
                    prompt.append(" ").append(sourceJobTitle)
                }
                prompt.append(".\n")
            }
        } else {
            prompt.append("Write a modern CV/Resume for the role: ").append(position.trim()).append("\n")
            if (education.isNotBlank()) {
                prompt.append("Education: ").append(education.trim()).append("\n")
            }
            if (experience.isNotBlank()) {
                prompt.append("Work experience: ").append(experience.trim()).append("\n")
            }
            if (skills.isNotBlank()) {
                prompt.append("Skills: ").append(skills.trim()).append("\n")
            }
        }

        if (addNotes.isNotBlank()) {
            prompt.append("Extra notes: ").append(addNotes.trim()).append("\n")
        }

        prompt.append("\nReturn only the plain document text. Keep it ready to download as text.")
        return prompt.toString()
    }

    private fun saveGeneratedDocument(generatedText: String?) {
        if (generatedText.isNullOrBlank()) {
            dismissLoadingDialog()
            isGenerating = false
            showResult(
                "GENERATION FAILED\n\n" +
                    "No document was produced. Please refresh, check your connection, or try again shortly.",
            )
            return
        }

        val outputBaseName = buildOutputBaseName()

        try {
            val written = documentWriter.write(generatedText, selectedFormat, outputBaseName)
            val file = written.file
            currentFilePath = file.absolutePath
            currentTextResult = generatedText

            val docType = if (ARG_CV_TYPE == selectedType) "CV/Resume" else "Cover Letter"
            val item = GeneratedDocument(
                UUID.randomUUID().toString(),
                sourceJobId,
                sourceJobTitle,
                sourceCompany,
                docType,
                selectedFormat,
                file.name,
                file.absolutePath,
                System.currentTimeMillis(),
            )
            documentStore.save(item)

            val result = StringBuilder()
            result.append("GENERATION SUCCESSFUL\n\n")
            result.append("Document: ").append(docType).append(" (").append(selectedFormat.uppercase(Locale.ROOT)).append(")\n")
            result.append("Saved: ").append(file.name).append("\n")
            result.append("Path: ").append(file.absolutePath)
            result.append("\n\n").append(generatedText)

            showResult(result.toString())
            dismissLoadingDialog()
            isGenerating = false
            Toast.makeText(context, "$docType generated successfully", Toast.LENGTH_LONG).show()
        } catch (error: IOException) {
            dismissLoadingDialog()
            isGenerating = false
            showResult("SAVE FAILED\n\nCould not save the generated document. Please check storage access and try again.")
            Toast.makeText(requireContext(), "Failed to save generated file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildOutputBaseName(): String {
        val typePart = if (ARG_CV_TYPE == selectedType) "CV" else "Cover_Letter"
        if (sourceJobId != -1 && sourceJobTitle.isNotEmpty()) {
            return typePart + "_" + sourceJobTitle + if (sourceCompany.isEmpty()) "" else "_$sourceCompany"
        }

        val userName = name.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return if (userName.isEmpty()) typePart else typePart + "_" + userName
    }

    private fun showResult(result: String) {
        resultText = result
        hasResult = true
    }

    private fun copyCurrentTextToClipboard() {
        if (currentTextResult.isNotEmpty()) {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Generated Document", currentTextResult)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No text available to copy", Toast.LENGTH_SHORT).show()
        }
    }

    private fun downloadCurrentFile() {
        if (currentFilePath.isEmpty()) {
            Toast.makeText(context, "No downloaded file available", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(currentFilePath)
        if (!file.exists()) {
            Toast.makeText(context, "Generated file not found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = documentWriter.getUriForFile(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, documentWriter.getMimeType(selectedFormat))
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openLibrary() {
        if (isAdded) {
            Navigation.findNavController(requireActivity(), AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN)
                .navigate(R.id.nav_documents)
        }
    }

    private fun showLoadingDialog(message: String) {
        if (context == null) return
        loadingDialog?.dismiss()
        loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Please Wait")
            .setMessage(message)
            .setCancelable(false)
            .create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun loadRewardedAd() {
        val context = context
        if (context == null) {
            dismissLoadingDialog()
            generateDocument()
            return
        }

        RewardedAd.load(
            context,
            SharedAdConfig.ANDROID_REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    dismissLoadingDialog()
                    if (!isAdded || activity == null) {
                        rewardedAd = null
                        generateDocument()
                        return
                    }
                    rewardedAd = ad
                    pendingGenerateAfterAd = true
                    showRewardedAd()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    dismissLoadingDialog()
                    Toast.makeText(context, "Ad failed to load. Generating document...", Toast.LENGTH_SHORT).show()
                    generateDocument()
                }
            },
        )
    }

    private fun showRewardedAd() {
        val ad = rewardedAd
        val activity = activity
        if (ad != null && activity != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    generateAfterAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    generateAfterAd()
                }
            }
            ad.show(activity) {
                generateAfterAd()
            }
        } else {
            Toast.makeText(context, "Ad not available. Generating document...", Toast.LENGTH_SHORT).show()
            generateDocument()
        }
    }

    private fun generateAfterAd() {
        if (!pendingGenerateAfterAd) return
        pendingGenerateAfterAd = false
        generateDocument()
    }

    private fun selectedTypeLabel(): String =
        if (ARG_CV_TYPE == selectedType) "CV/Resume" else "Cover Letter"

    private fun selectedFormatLabel(): String =
        when (selectedFormat) {
            "pdf" -> "PDF Document"
            "docx" -> "Word Document"
            else -> "Text Only"
        }

    private fun selectedToneLabel(): String =
        selectedTone.split("_").joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }

    override fun onDestroy() {
        super.onDestroy()
        dismissLoadingDialog()
    }

    companion object {
        const val ARG_SOURCE_JOB_ID = "source_job_id"
        const val ARG_SOURCE_JOB_TITLE = "source_job_title"
        const val ARG_SOURCE_COMPANY = "source_company"
        const val ARG_PREFILL_TYPE = "prefill_type"
        const val ARG_CV_TYPE = "cv"
        const val ARG_COVER_TYPE = "cover_letter"

        private const val TAG = "CVGeneratorFragment"
    }
}

@Composable
private fun CVGeneratorScreen(
    selectedTypeLabel: String,
    selectedFormatLabel: String,
    selectedToneLabel: String,
    selectedType: String,
    name: String,
    email: String,
    phone: String,
    position: String,
    education: String,
    experience: String,
    skills: String,
    company: String,
    positionApplying: String,
    experienceSummary: String,
    addNotes: String,
    isGenerating: Boolean,
    resultText: String,
    hasResult: Boolean,
    onSelectType: () -> Unit,
    onSelectFormat: () -> Unit,
    onSelectTone: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onPositionChange: (String) -> Unit,
    onEducationChange: (String) -> Unit,
    onExperienceChange: (String) -> Unit,
    onSkillsChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onPositionApplyingChange: (String) -> Unit,
    onExperienceSummaryChange: (String) -> Unit,
    onAddNotesChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onOpenLibrary: () -> Unit,
    onCopy: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        Text("CV and Cover Letter Generator", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        OutlinedButton(onClick = onSelectType, modifier = Modifier.fillMaxWidth()) {
            Text("Type: $selectedTypeLabel")
        }
        OutlinedButton(onClick = onSelectFormat, modifier = Modifier.fillMaxWidth()) {
            Text("Format: $selectedFormatLabel")
        }
        if (selectedType == CVGeneratorFragment.ARG_COVER_TYPE) {
            OutlinedButton(onClick = onSelectTone, modifier = Modifier.fillMaxWidth()) {
                Text("Tone: $selectedToneLabel")
            }
        }

        GeneratorTextField(name, onNameChange, "Full Name")
        GeneratorTextField(email, onEmailChange, "Email", keyboardType = KeyboardType.Email)
        GeneratorTextField(phone, onPhoneChange, "Phone", keyboardType = KeyboardType.Phone)

        if (selectedType == CVGeneratorFragment.ARG_CV_TYPE) {
            GeneratorTextField(position, onPositionChange, "Position / Job Title")
            GeneratorTextField(education, onEducationChange, "Education", multiline = true)
            GeneratorTextField(experience, onExperienceChange, "Work Experience", multiline = true)
            GeneratorTextField(skills, onSkillsChange, "Skills", multiline = true)
        } else {
            GeneratorTextField(company, onCompanyChange, "Company")
            GeneratorTextField(positionApplying, onPositionApplyingChange, "Position Applying For")
            GeneratorTextField(experienceSummary, onExperienceSummaryChange, "Experience Summary", multiline = true)
        }

        GeneratorTextField(addNotes, onAddNotesChange, "Additional Notes", multiline = true)

        Button(onClick = onGenerate, enabled = !isGenerating, modifier = Modifier.fillMaxWidth()) {
            Text(if (isGenerating) "Generating..." else "Generate")
        }
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
            Text("Open Saved Documents")
        }

        if (isGenerating) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        if (hasResult) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SelectionContainer {
                        Text(resultText)
                    }
                    Button(onClick = onCopy, modifier = Modifier.fillMaxWidth()) {
                        Text("Copy Text")
                    }
                    OutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Downloaded File")
                    }
                }
            }
        }

        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_SECONDARY_AD_UNIT_ID)
    }
}

@Composable
private fun GeneratorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    multiline: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        minLines = if (multiline) 4 else 1,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}
