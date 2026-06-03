package com.solutions.alphil.zambiajobalerts.ui.aigenerate

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocument
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocumentStore
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocumentWriter
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeBannerAd
import com.solutions.alphil.zambiajobalerts.ui.common.ComposeNativeAd
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DocumentLibraryFragment : Fragment() {
    private lateinit var store: GeneratedDocumentStore
    private lateinit var writer: GeneratedDocumentWriter
    private var documentNativeAd by mutableStateOf<NativeAd?>(null)
    private var documents by mutableStateOf<List<GeneratedDocument>>(emptyList())
    private var adLoader: AdLoader? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        store = GeneratedDocumentStore(requireContext())
        writer = GeneratedDocumentWriter(requireContext())
        initializeNativeAd()
        loadDocuments()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    DocumentLibraryScreen(
                        documents = documents,
                        nativeAd = documentNativeAd,
                        onOpen = { openGeneratedDocument(it) },
                        onDelete = { deleteGeneratedDocument(it) },
                        onClearAll = { clearDocuments() },
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        documentNativeAd?.destroy()
        documentNativeAd = null
        adLoader = null
    }

    private fun loadDocuments() {
        documents = store.getAll()
    }

    private fun initializeNativeAd() {
        adLoader = AdLoader.Builder(requireContext(), SharedAdConfig.ANDROID_NATIVE_AD_UNIT_ID)
            .forNativeAd { nativeAd ->
                if (!isAdded) {
                    nativeAd.destroy()
                    return@forNativeAd
                }
                documentNativeAd?.destroy()
                documentNativeAd = nativeAd
            }
            .withAdListener(
                object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Banner remains visible if native fails.
                    }
                },
            )
            .build()
        adLoader?.loadAds(AdRequest.Builder().build(), 1)
    }

    private fun openGeneratedDocument(document: GeneratedDocument) {
        val file = File(document.filePath.orEmpty())
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = writer.getUriForFile(file)
        val format = document.format ?: "txt"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, writer.getMimeType(format.lowercase(Locale.ROOT)))
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        }

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "No app found to open this document", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteGeneratedDocument(document: GeneratedDocument) {
        val file = File(document.filePath.orEmpty())
        if (file.exists()) {
            file.delete()
        }

        store.remove(document.id)
        loadDocuments()
        Toast.makeText(requireContext(), "Document deleted", Toast.LENGTH_SHORT).show()
    }

    private fun clearDocuments() {
        store.clear()
        loadDocuments()
        Toast.makeText(requireContext(), "Library cleared", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun DocumentLibraryScreen(
    documents: List<GeneratedDocument>,
    nativeAd: NativeAd?,
    onOpen: (GeneratedDocument) -> Unit,
    onDelete: (GeneratedDocument) -> Unit,
    onClearAll: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Saved Documents", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        item {
            ComposeNativeAd(nativeAd)
        }
        item {
            ComposeBannerAd(SharedAdConfig.ANDROID_BANNER_PRIMARY_AD_UNIT_ID)
        }

        if (documents.isEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("No generated documents yet.")
                    Text("Generated CVs and cover letters will appear here.")
                }
            }
        } else {
            item {
                OutlinedButton(onClick = onClearAll, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear Library")
                }
            }
            items(documents, key = { it.id.orEmpty() }) { document ->
                DocumentCard(document = document, onOpen = onOpen, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: GeneratedDocument,
    onOpen: (GeneratedDocument) -> Unit,
    onDelete: (GeneratedDocument) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(document.fileName.orEmpty(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text(document.getDisplayLabel())
            Text(document.getDisplaySource(), color = Color(0xFF555555))
            Text(formatDate(document.createdAt), color = Color(0xFF777777), fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { onOpen(document) }, modifier = Modifier.weight(1f)) {
                    Text("Open")
                }
                OutlinedButton(onClick = { onDelete(document) }, modifier = Modifier.weight(1f)) {
                    Text("Delete")
                }
            }
        }
    }
}

private fun formatDate(timeMillis: Long): String {
    if (timeMillis <= 0) return ""
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timeMillis))
}
