package com.solutions.alphil.zambiajobalerts.ui.aigenerate;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.solutions.alphil.zambiajobalerts.R;
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocument;
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocumentStore;
import com.solutions.alphil.zambiajobalerts.classes.GeneratedDocumentWriter;
import com.solutions.alphil.zambiajobalerts.classes.ModelFallbackGenerator;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class CVGeneratorFragment extends Fragment {

    public static final String ARG_SOURCE_JOB_ID = "source_job_id";
    public static final String ARG_SOURCE_JOB_TITLE = "source_job_title";
    public static final String ARG_SOURCE_COMPANY = "source_company";
    public static final String ARG_PREFILL_TYPE = "prefill_type";

    private static final String TAG = "CVGeneratorFragment";

    public static final String ARG_CV_TYPE = "cv";
    public static final String ARG_COVER_TYPE = "cover_letter";

    private TextView selectedTypeText;
    private TextView selectedFormatText;
    private TextView selectedToneText;
    private Button selectTypeBtn;
    private Button selectFormatBtn;
    private Button selectToneBtn;
    private LinearLayout toneSelectionLayout;
    private LinearLayout cvFields;
    private LinearLayout coverLetterFields;
    private LinearLayout resultsLayout;

    private EditText nameInput;
    private EditText emailInput;
    private EditText phoneInput;
    private EditText positionInput;
    private EditText educationInput;
    private EditText experienceInput;
    private EditText skillsInput;
    private EditText companyInput;
    private EditText positionApplyingInput;
    private EditText experienceSummaryInput;
    private EditText addNotesInput;

    private Button submitBtn;
    private Button viewGeneratedBtn;
    private Button copyTextBtn;
    private Button downloadBtn;

    private TextView resultTextView;

    private AlertDialog loadingDialog;
    private RewardedAd rewardedAd;
    private static final String TEST_AD_UNIT_ID_REWARDED = "ca-app-pub-2168080105757285/1720477714";
    private boolean pendingGenerateAfterAd = false;

    private final String[] documentTypes = {"CV/Resume", "Cover Letter"};
    private final String[] documentFormats = {"PDF Document", "Word Document", "Text Only"};
    private final String[] letterTones = {"Formal", "Professional", "Confident", "Friendly", "Enthusiastic"};

    private String selectedType = ARG_CV_TYPE;
    private String selectedFormat = "pdf";
    private String selectedTone = "formal";

    private int sourceJobId = -1;
    private String sourceJobTitle = "";
    private String sourceCompany = "";

    private String currentFilePath = "";
    private String currentTextResult = "";

    private GeneratedDocumentStore documentStore;
    private GeneratedDocumentWriter documentWriter;
    private ModelFallbackGenerator modelGenerator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_cv_generator, container, false);

        documentStore = new GeneratedDocumentStore(requireContext());
        documentWriter = new GeneratedDocumentWriter(requireContext());
        modelGenerator = new ModelFallbackGenerator();

        initializeUI(root);
        initializeAds(root);
        applyPrefillArguments();

        return root;
    }

    private void initializeUI(View root) {
        selectedTypeText = root.findViewById(R.id.selectedTypeText);
        selectedFormatText = root.findViewById(R.id.selectedFormatText);
        selectedToneText = root.findViewById(R.id.selectedToneText);

        selectTypeBtn = root.findViewById(R.id.selectTypeBtn);
        selectFormatBtn = root.findViewById(R.id.selectFormatBtn);
        selectToneBtn = root.findViewById(R.id.selectToneBtn);

        toneSelectionLayout = root.findViewById(R.id.toneSelectionLayout);
        cvFields = root.findViewById(R.id.cvFields);
        coverLetterFields = root.findViewById(R.id.coverLetterFields);
        resultsLayout = root.findViewById(R.id.resultsLayout);

        nameInput = root.findViewById(R.id.nameInput);
        emailInput = root.findViewById(R.id.emailInput);
        phoneInput = root.findViewById(R.id.phoneInput);
        positionInput = root.findViewById(R.id.positionInput);
        educationInput = root.findViewById(R.id.educationInput);
        experienceInput = root.findViewById(R.id.experienceInput);
        skillsInput = root.findViewById(R.id.skillsInput);
        companyInput = root.findViewById(R.id.companyInput);
        positionApplyingInput = root.findViewById(R.id.positionApplyingInput);
        experienceSummaryInput = root.findViewById(R.id.experienceSummaryInput);
        addNotesInput = root.findViewById(R.id.addNotesInput);

        submitBtn = root.findViewById(R.id.submitButton);
        viewGeneratedBtn = root.findViewById(R.id.viewGeneratedBtn);
        copyTextBtn = root.findViewById(R.id.copyTextBtn);
        downloadBtn = root.findViewById(R.id.downloadBtn);

        resultTextView = root.findViewById(R.id.resultTextView);
        resultTextView.setMovementMethod(LinkMovementMethod.getInstance());
        resultTextView.setAutoLinkMask(Linkify.WEB_URLS);
        resultTextView.setTextIsSelectable(true);

        selectTypeBtn.setOnClickListener(v -> showDocumentTypeDialog());
        selectFormatBtn.setOnClickListener(v -> showFormatDialog());
        selectToneBtn.setOnClickListener(v -> showToneDialog());

        selectedTypeText.setOnClickListener(v -> showDocumentTypeDialog());
        selectedFormatText.setOnClickListener(v -> showFormatDialog());
        selectedToneText.setOnClickListener(v -> showToneDialog());

        submitBtn.setOnClickListener(v -> validateAndSubmit());
        viewGeneratedBtn.setOnClickListener(v -> openLibrary());
        copyTextBtn.setOnClickListener(v -> copyCurrentTextToClipboard());
        downloadBtn.setOnClickListener(v -> downloadCurrentFile());

        selectedTypeText.setText("CV/Resume");
        selectedFormatText.setText("PDF Document");
        selectedToneText.setText("Formal");
        viewGeneratedBtn.setText("Open Saved Documents");
    }

    private void initializeAds(View root) {
        try {
            AdView adViewTop = root.findViewById(R.id.adViewTop);
            AdView adViewBottom = root.findViewById(R.id.adViewBottom);
            AdRequest adRequest = new AdRequest.Builder().build();
            adViewTop.loadAd(adRequest);
            adViewBottom.loadAd(adRequest);
        } catch (Exception e) {
            Log.e(TAG, "Ad initialization failed: " + e.getMessage());
        }
    }

    private void applyPrefillArguments() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }

        sourceJobId = args.getInt(ARG_SOURCE_JOB_ID, -1);
        sourceJobTitle = args.getString(ARG_SOURCE_JOB_TITLE, "");
        sourceCompany = args.getString(ARG_SOURCE_COMPANY, "");
        String prefillType = args.getString(ARG_PREFILL_TYPE, "");

        if (ARG_COVER_TYPE.equals(prefillType)) {
            selectedType = ARG_COVER_TYPE;
            selectedTypeText.setText("Cover Letter");
            if (!sourceCompany.isEmpty()) {
                companyInput.setText(sourceCompany);
            }
            if (!sourceJobTitle.isEmpty()) {
                positionApplyingInput.setText(sourceJobTitle);
            }
        } else {
            selectedType = ARG_CV_TYPE;
            selectedTypeText.setText("CV/Resume");
            if (!sourceJobTitle.isEmpty()) {
                positionInput.setText(sourceJobTitle);
            }
        }

        setTextIfAbsent(nameInput, args.getString("name"));
        setTextIfAbsent(emailInput, args.getString("email"));
        setTextIfAbsent(phoneInput, args.getString("phone"));

        updateFieldsVisibility();
    }

    private void showDocumentTypeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Document Type");

        int currentIndex = ARG_CV_TYPE.equals(selectedType) ? 0 : 1;

        builder.setSingleChoiceItems(documentTypes, currentIndex, (dialog, which) -> {
            selectedType = which == 0 ? ARG_CV_TYPE : ARG_COVER_TYPE;
            selectedTypeText.setText(documentTypes[which]);
            updateFieldsVisibility();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showFormatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Document Format");

        int currentIndex = "pdf".equals(selectedFormat) ? 0 : "docx".equals(selectedFormat) ? 1 : 2;

        builder.setSingleChoiceItems(documentFormats, currentIndex, (dialog, which) -> {
            if (which == 0) {
                selectedFormat = "pdf";
            } else if (which == 1) {
                selectedFormat = "docx";
            } else {
                selectedFormat = "txt";
            }

            selectedFormatText.setText(documentFormats[which]);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showToneDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Writing Tone");

        int currentIndex = 0;
        switch (selectedTone) {
            case "professional":
                currentIndex = 1;
                break;
            case "confident":
                currentIndex = 2;
                break;
            case "friendly":
                currentIndex = 3;
                break;
            case "enthusiastic":
                currentIndex = 4;
                break;
        }

        builder.setSingleChoiceItems(letterTones, currentIndex, (dialog, which) -> {
            selectedTone = letterTones[which].toLowerCase().replace(" ", "_");
            selectedToneText.setText(letterTones[which]);
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateFieldsVisibility() {
        if (ARG_CV_TYPE.equals(selectedType)) {
            cvFields.setVisibility(View.VISIBLE);
            coverLetterFields.setVisibility(View.GONE);
            toneSelectionLayout.setVisibility(View.GONE);
        } else {
            cvFields.setVisibility(View.GONE);
            coverLetterFields.setVisibility(View.VISIBLE);
            toneSelectionLayout.setVisibility(View.VISIBLE);
        }
    }

    private void validateAndSubmit() {
        if (nameInput.getText().toString().trim().isEmpty() ||
                emailInput.getText().toString().trim().isEmpty() ||
                phoneInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Please fill all required fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ARG_CV_TYPE.equals(selectedType) && positionInput.getText().toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Please enter position/job title!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ARG_COVER_TYPE.equals(selectedType) &&
                (companyInput.getText().toString().trim().isEmpty() ||
                        positionApplyingInput.getText().toString().trim().isEmpty())) {
            Toast.makeText(getContext(), "Please enter company and position!", Toast.LENGTH_SHORT).show();
            return;
        }

        submitBtn.setEnabled(false);
        showLoadingDialog("Loading ad, please wait...");
        loadRewardedAdRewarded();
    }

    private void generateDocument() {
        String prompt = buildPrompt();
        modelGenerator.generateText(prompt, new ModelFallbackGenerator.GenerationListener() {
            @Override
            public void onSuccess(String generatedText) {
                if (!isAdded()) {
                    return;
                }
                requireActivity().runOnUiThread(() -> saveGeneratedDocument(generatedText));
            }

            @Override
            public void onFailure(String message) {
                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    dismissLoadingDialog();
                    submitBtn.setEnabled(true);
                    showResult("❌ GENERATION FAILED\n\n" + message);
                    Toast.makeText(getContext(), "Generation failed", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private String buildPrompt() {
        StringBuilder prompt = new StringBuilder();
        String fullName = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String phone = phoneInput.getText().toString().trim();
        String notes = addNotesInput.getText().toString().trim();

        prompt.append("Write the document as a professional, concise, and interview-ready text.\n\n");
        prompt.append("Applicant Name: ").append(fullName).append("\n");
        prompt.append("Email: ").append(email).append("\n");
        prompt.append("Phone: ").append(phone).append("\n\n");

        if (ARG_COVER_TYPE.equals(selectedType)) {
            String company = companyInput.getText().toString().trim();
            String job = positionApplyingInput.getText().toString().trim();
            String experience = experienceSummaryInput.getText().toString().trim();

            prompt.append("Write a cover letter for this job application.\n");
            prompt.append("Company: ").append(company).append("\n");
            prompt.append("Role: ").append(job).append("\n");
            prompt.append("Tone: ").append(selectedTone).append("\n\n");
            if (!experience.isEmpty()) {
                prompt.append("Relevant experience: ").append(experience).append("\n");
            }

            if (sourceJobId != -1 && !sourceCompany.isEmpty()) {
                prompt.append("Match it to the job details for: ").append(sourceCompany).append(" role in");
                if (!sourceJobTitle.isEmpty()) {
                    prompt.append(" ").append(sourceJobTitle);
                }
                prompt.append(".\n");
            }
        } else {
            String position = positionInput.getText().toString().trim();
            String education = educationInput.getText().toString().trim();
            String experience = experienceInput.getText().toString().trim();
            String skills = skillsInput.getText().toString().trim();

            prompt.append("Write a modern CV/Resume for the role: ").append(position).append("\n");
            if (!education.isEmpty()) {
                prompt.append("Education: ").append(education).append("\n");
            }
            if (!experience.isEmpty()) {
                prompt.append("Work experience: ").append(experience).append("\n");
            }
            if (!skills.isEmpty()) {
                prompt.append("Skills: ").append(skills).append("\n");
            }
        }

        if (!notes.isEmpty()) {
            prompt.append("Extra notes: ").append(notes).append("\n");
        }

        prompt.append("\nReturn only the plain document text. Keep it ready to download as text.");

        return prompt.toString();
    }

    private void saveGeneratedDocument(String generatedText) {
        if (generatedText == null || generatedText.trim().isEmpty()) {
            dismissLoadingDialog();
            submitBtn.setEnabled(true);
            showResult("❌ MODEL RETURNED EMPTY RESULT\n\nNo content was produced.");
            return;
        }

        String outputBaseName = buildOutputBaseName();

        try {
            GeneratedDocumentWriter.WrittenDocument written = documentWriter.write(generatedText, selectedFormat, outputBaseName);
            File file = written.getFile();
            currentFilePath = file.getAbsolutePath();
            currentTextResult = generatedText;

            GeneratedDocument item = new GeneratedDocument(
                    UUID.randomUUID().toString(),
                    sourceJobId,
                    sourceJobTitle,
                    sourceCompany,
                    selectedType,
                    selectedFormat,
                    file.getName(),
                    file.getAbsolutePath(),
                    System.currentTimeMillis()
            );
            documentStore.save(item);

            String docType = ARG_CV_TYPE.equals(selectedType) ? "CV/Resume" : "Cover Letter";
            StringBuilder result = new StringBuilder();
            result.append("✅ GENERATION SUCCESSFUL\n\n");
            result.append("Document: ").append(docType).append(" (" ).append(selectedFormat.toUpperCase()).append(")\n");
            result.append("Saved: ").append(file.getName()).append("\n");
            result.append("Path: ").append(file.getAbsolutePath());
            result.append("\n\n").append(generatedText);

            showResult(result.toString());
            downloadBtn.setVisibility(View.VISIBLE);
            dismissLoadingDialog();
            submitBtn.setEnabled(true);
            Toast.makeText(getContext(), docType + " generated successfully", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            dismissLoadingDialog();
            submitBtn.setEnabled(true);
            showResult("❌ FILE ERROR\n\nCould not save generated document: " + e.getMessage());
            Toast.makeText(requireContext(), "Failed to save generated file", Toast.LENGTH_SHORT).show();
        }
    }

    private String buildOutputBaseName() {
        String typePart = ARG_CV_TYPE.equals(selectedType) ? "CV" : "Cover_Letter";
        if (sourceJobId != -1 && !sourceJobTitle.isEmpty()) {
            return typePart + "_" + sourceJobTitle + (sourceCompany.isEmpty() ? "" : "_" + sourceCompany);
        }

        String userName = nameInput.getText().toString().trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        if (userName.isEmpty()) {
            return typePart;
        }

        return typePart + "_" + userName;
    }

    private void showResult(String result) {
        requireActivity().runOnUiThread(() -> {
            resultTextView.setText(result);
            resultsLayout.setVisibility(View.VISIBLE);
            ScrollView scrollView = requireView().findViewById(R.id.mainScrollView);
            if (scrollView != null) {
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }

    private void copyCurrentTextToClipboard() {
        if (!currentTextResult.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Generated Document", currentTextResult);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), "No text available to copy", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadCurrentFile() {
        if (currentFilePath.isEmpty()) {
            Toast.makeText(getContext(), "No downloaded file available", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(currentFilePath);
        if (!file.exists()) {
            Toast.makeText(getContext(), "Generated file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = documentWriter.getUriForFile(file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, documentWriter.getMimeType(selectedFormat));
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "No app found to open this file", Toast.LENGTH_SHORT).show();
        }
    }

    private void openLibrary() {
        if (isAdded()) {
            Navigation.findNavController(requireView()).navigate(R.id.nav_documents);
        }
    }

    private void showLoadingDialog(String message) {
        if (getContext() == null) {
            return;
        }

        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }

        loadingDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Please Wait")
                .setMessage(message)
                .setCancelable(false)
                .create();
        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void loadRewardedAdRewarded() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(requireContext(), TEST_AD_UNIT_ID_REWARDED, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        dismissLoadingDialog();
                        rewardedAd = ad;
                        pendingGenerateAfterAd = true;
                        showRewardedAdRewarded();
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        dismissLoadingDialog();
                        Toast.makeText(getContext(), "Ad failed to load. Generating document...", Toast.LENGTH_SHORT).show();
                        generateDocument();
                    }
                });
    }

    private void showRewardedAdRewarded() {
        if (rewardedAd != null && getActivity() != null) {
            rewardedAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    generateAfterAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                    generateAfterAd();
                }
            });
            rewardedAd.show(getActivity(), rewardItem -> {
                generateAfterAd();
            });
        } else {
            Toast.makeText(getContext(), "Ad not available. Generating document...", Toast.LENGTH_SHORT).show();
            generateDocument();
        }
    }

    private void generateAfterAd() {
        if (!pendingGenerateAfterAd) {
            return;
        }
        pendingGenerateAfterAd = false;
        generateDocument();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    private void setTextIfAbsent(EditText editText, String value) {
        if (editText == null) {
            return;
        }
        if (value != null && !value.trim().isEmpty()) {
            editText.setText(value);
        }
    }
}
