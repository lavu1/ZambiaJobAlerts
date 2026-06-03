package com.solutions.alphil.zambiajobalerts.ui.rewards

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.solutions.alphil.zambiajobalerts.classes.ApiConfig
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response

class CVReviewFragment : Fragment() {
    private var email by mutableStateOf("")
    private var phone by mutableStateOf("")
    private var changes by mutableStateOf("")
    private var selectedFileNameState by mutableStateOf("")

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null
    private var tempUploadFile: File? = null
    private var loadingDialog: AlertDialog? = null
    private var rewardedAd: RewardedAd? = null

    private var pendingEmail: String? = null
    private var pendingPhone: String? = null
    private var pendingChanges: String? = null

    private val client = OkHttpClient()

    private val filePickerLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                handleSelectedFile(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    CVReviewScreen(
                        email = email,
                        phone = phone,
                        changes = changes,
                        fileName = selectedFileNameState,
                        onEmailChange = { email = it },
                        onPhoneChange = { phone = it },
                        onChangesChange = { changes = it },
                        onPickFile = { openFilePicker() },
                        onSubmit = { submitForm() },
                    )
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        tempUploadFile?.takeIf { it.exists() }?.delete()
        dismissLoadingDialog()
    }

    private fun submitForm() {
        if (email.isBlank() || phone.isBlank() || selectedFileUri == null) {
            Toast.makeText(context, "Please fill all fields and select a file!", Toast.LENGTH_SHORT).show()
            return
        }

        pendingEmail = email.trim()
        pendingPhone = phone.trim()
        pendingChanges = changes.trim()
        showLoadingDialog("Loading ad, please wait...")
        loadRewardedAd()
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                ),
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        filePickerLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            tempUploadFile?.takeIf { it.exists() }?.delete()

            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )

            selectedFileUri = uri
            selectedFileName = getFileName(uri)
            selectedFileNameState = selectedFileName ?: "File selected"
            Toast.makeText(context, "File selected: $selectedFileNameState", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Log.e(TAG, "File selection failed: ${error.message}")
            Toast.makeText(context, "Error accessing file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null

        try {
            if ("content" == uri.scheme) {
                val documentFile = DocumentFile.fromSingleUri(requireContext(), uri)
                if (documentFile?.name != null) {
                    fileName = documentFile.name
                } else {
                    requireContext().contentResolver.query(uri, null, null, null, null).use { cursor ->
                        if (cursor != null && cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex("_display_name")
                            if (nameIndex != -1) {
                                fileName = cursor.getString(nameIndex)
                            }
                        }
                    }
                }
            }

            if (fileName == null) {
                val path = uri.path
                if (path != null) {
                    val cut = path.lastIndexOf('/')
                    if (cut != -1) {
                        fileName = path.substring(cut + 1)
                    }
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Unable to resolve file name: ${error.message}")
        }

        return fileName
    }

    private fun uploadSelectedFile() {
        val uri = selectedFileUri
        if (uri == null) {
            Toast.makeText(context, "No file selected!", Toast.LENGTH_SHORT).show()
            return
        }

        showLoadingDialog("Uploading CV for Review...")

        Thread {
            try {
                tempUploadFile = createTempFileFromUri(uri)
                val uploadFile = tempUploadFile
                if (uploadFile != null && uploadFile.exists() && uploadFile.length() > 0) {
                    uploadFileWithOkHttp(uploadFile)
                } else {
                    requireActivity().runOnUiThread {
                        dismissLoadingDialog()
                        Toast.makeText(context, "File is empty or inaccessible", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (error: IOException) {
                Log.e(TAG, "Upload error: ${error.message}")
                requireActivity().runOnUiThread {
                    dismissLoadingDialog()
                    Toast.makeText(context, "Upload error: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    @Throws(IOException::class)
    private fun createTempFileFromUri(uri: Uri): File {
        requireContext().contentResolver.openInputStream(uri).use { inputStream ->
            if (inputStream == null) {
                throw IOException("Cannot open input stream from URI")
            }

            val extension = getFileExtension(selectedFileName)
            val suffix = if (extension != null) ".$extension" else ".tmp"
            val tempFile = File.createTempFile("cv_upload", suffix, requireContext().cacheDir)

            FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(4096)
                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break
                    outputStream.write(buffer, 0, bytesRead)
                }
                outputStream.flush()
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                return tempFile
            }
            throw IOException("Failed to create temporary file")
        }
    }

    private fun getFileExtension(fileName: String?): String? {
        if (fileName == null || !fileName.contains(".")) return null
        return fileName.substring(fileName.lastIndexOf(".") + 1)
    }

    private fun uploadFileWithOkHttp(file: File) {
        try {
            var mimeType = "application/pdf"
            val fileName = selectedFileName
            if (fileName != null) {
                if (fileName.endsWith(".doc")) {
                    mimeType = "application/msword"
                } else if (fileName.endsWith(".docx")) {
                    mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                }
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("type", "cv_review")
                .addFormDataPart("email", pendingEmail.orEmpty())
                .addFormDataPart("phone", pendingPhone.orEmpty())
                .addFormDataPart("additional_notes", pendingChanges.orEmpty())
                .addFormDataPart("cv_file_path", selectedFileName, file.asRequestBody(mimeType.toMediaTypeOrNull()))
                .build()

            val request = Request.Builder()
                .url(ApiConfig.LEGACY_SERVICES_URL)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Upload failed: ${e.message}")
                        requireActivity().runOnUiThread {
                            dismissLoadingDialog()
                            Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                        cleanupTempFile(file)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            val responseBody = it.body.string()
                            val successful = it.isSuccessful
                            val code = it.code
                            Log.d(TAG, "Server response: $responseBody")

                            requireActivity().runOnUiThread {
                                dismissLoadingDialog()
                                if (successful) {
                                    if (responseBody.contains("id") || responseBody.contains("updated_at")) {
                                        Toast.makeText(context, "CV submitted successfully!", Toast.LENGTH_LONG).show()
                                        resetForm()
                                    } else {
                                        Toast.makeText(context, "Upload response: $responseBody", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Upload failed: $code - $responseBody", Toast.LENGTH_LONG).show()
                                }
                            }

                            cleanupTempFile(file)
                        }
                    }
                },
            )
        } catch (error: Exception) {
            Log.e(TAG, "Upload error: ${error.message}")
            requireActivity().runOnUiThread {
                dismissLoadingDialog()
                Toast.makeText(context, "Upload error: ${error.message}", Toast.LENGTH_LONG).show()
            }
            cleanupTempFile(file)
        }
    }

    private fun cleanupTempFile(file: File) {
        if (file.exists()) {
            file.delete()
        }
        tempUploadFile = null
    }

    private fun resetForm() {
        email = ""
        phone = ""
        changes = ""
        selectedFileNameState = ""
        selectedFileUri = null
        selectedFileName = null
        tempUploadFile?.takeIf { it.exists() }?.delete()
        tempUploadFile = null
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
            uploadSelectedFile()
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
                        uploadSelectedFile()
                        return
                    }
                    rewardedAd = ad
                    showRewardedAd()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    dismissLoadingDialog()
                    Toast.makeText(context, "Ad failed to load. Uploading your file...", Toast.LENGTH_SHORT).show()
                    uploadSelectedFile()
                }
            },
        )
    }

    private fun showRewardedAd() {
        val ad = rewardedAd
        val activity = activity
        if (ad != null && activity != null) {
            ad.show(activity) {
                uploadSelectedFile()
            }
        } else {
            Toast.makeText(context, "Ad not available. Uploading your file...", Toast.LENGTH_SHORT).show()
            uploadSelectedFile()
        }
    }

    companion object {
        private const val TAG = "CVReviewFragment"
    }
}

@Composable
private fun CVReviewScreen(
    email: String,
    phone: String,
    changes: String,
    fileName: String,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onChangesChange: (String) -> Unit,
    onPickFile: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        Text("CV Review", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        OutlinedButton(onClick = onPickFile, modifier = Modifier.fillMaxWidth()) {
            Text(fileName.ifBlank { "Select CV File" })
        }
        OutlinedTextField(
            value = changes,
            onValueChange = onChangesChange,
            label = { Text("Requested Changes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
        )
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) {
            Text("Submit")
        }
    }
}
