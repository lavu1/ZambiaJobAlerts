package com.solutions.alphil.zambiajobalerts.ui.aigenerate;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.nativead.NativeAd;
import com.solutions.alphil.zambiajobalerts.R;
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocument;
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocumentStore;
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocumentWriter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocumentLibraryFragment extends Fragment {

    private androidx.recyclerview.widget.RecyclerView rvDocuments;
    private android.widget.TextView tvEmpty;
    private android.widget.Button btnClearAll;

    private GeneratedDocumentStore store;
    private GeneratedDocumentWriter writer;
    private GeneratedDocumentAdapter adapter;
    private NativeAd documentNativeAd;
    private AdLoader adLoader;
    private static final String NATIVE_AD_UNIT_ID = "ca-app-pub-2168080105757285/5207161115";
    private final GeneratedDocumentAdapter.BannerItem bannerItem = new GeneratedDocumentAdapter.BannerItem();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_document_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        store = new GeneratedDocumentStore(requireContext());
        writer = new GeneratedDocumentWriter(requireContext());

        rvDocuments = view.findViewById(R.id.rvGeneratedDocuments);
        tvEmpty = view.findViewById(R.id.tvEmptyState);
        btnClearAll = view.findViewById(R.id.btnClearAll);

        rvDocuments.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new GeneratedDocumentAdapter(new GeneratedDocumentAdapter.OnDocumentActionListener() {
            @Override
            public void onOpen(GeneratedDocument document) {
                openGeneratedDocument(document);
            }

            @Override
            public void onDelete(GeneratedDocument document) {
                deleteGeneratedDocument(document);
            }
        });
        rvDocuments.setAdapter(adapter);

        btnClearAll.setOnClickListener(v -> {
            store.clear();
            loadDocuments();
            Toast.makeText(requireContext(), "Library cleared", Toast.LENGTH_SHORT).show();
        });

        initializeNativeAd();
        loadDocuments();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (documentNativeAd != null) {
            documentNativeAd.destroy();
            documentNativeAd = null;
        }
        adLoader = null;
    }

    private void loadDocuments() {
        List<GeneratedDocument> documents = store.getAll();
        List<Object> items = new ArrayList<>();
        if (documentNativeAd != null) {
            items.add(documentNativeAd);
        }
        items.add(bannerItem);
        if (documents != null && !documents.isEmpty()) {
            items.addAll(documents);
        }

        adapter.submitList(items);

        if (documents == null || documents.isEmpty()) {
            rvDocuments.setVisibility(View.GONE);
            tvEmpty.setVisibility(View.VISIBLE);
            btnClearAll.setEnabled(false);
        } else {
            rvDocuments.setVisibility(View.VISIBLE);
            tvEmpty.setVisibility(View.GONE);
            btnClearAll.setEnabled(true);
        }
    }

    private void initializeNativeAd() {
        adLoader = new AdLoader.Builder(requireContext(), NATIVE_AD_UNIT_ID)
                .forNativeAd(nativeAd -> {
                    if (!isAdded() || getView() == null) {
                        nativeAd.destroy();
                        return;
                    }
                    if (documentNativeAd != null) {
                        documentNativeAd.destroy();
                    }
                    documentNativeAd = nativeAd;
                    loadDocuments();
                })
                .withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        // Keep banner-only layout if native ad fails to load.
                    }
                })
                .build();
        adLoader.loadAds(new AdRequest.Builder().build(), 1);
    }

    private void openGeneratedDocument(GeneratedDocument document) {
        File file = new File(document.getFilePath());
        if (!file.exists()) {
            Toast.makeText(requireContext(), "File not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = writer.getUriForFile(file);
        String format = document.getFormat() == null ? "txt" : document.getFormat();
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, writer.getMimeType(format.toLowerCase()));
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "No app found to open this document", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteGeneratedDocument(GeneratedDocument document) {
        if (document == null || document.getFilePath() == null) {
            return;
        }

        File file = new File(document.getFilePath());
        if (file.exists()) {
            file.delete();
        }

        store.remove(document.getId());
        loadDocuments();
        Toast.makeText(requireContext(), "Document deleted", Toast.LENGTH_SHORT).show();
    }
}
