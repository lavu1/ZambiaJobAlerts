package com.solutions.alphil.zambiajobalerts.classes;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.solutions.alphil.zambiajobalerts.R;
import com.solutions.alphil.zambiajobalerts.databinding.BottomSheetJobDetailsBinding;
import com.solutions.alphil.zambiajobalerts.ui.jobs.JobsViewModel;
import com.solutions.alphil.zambiajobalerts.ui.savedjobs.SavedJobsViewModel;

public class JobDetailsBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetJobDetailsBinding binding;
    private JobsViewModel viewModel;
    private SavedJobsViewModel savedJobsViewModel;
    private JobRepository repository;
    private int jobId = -1;
    private String jobSlug;
    private Job currentJob;

    public static JobDetailsBottomSheet newInstance(int jobId) {
        JobDetailsBottomSheet fragment = new JobDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putInt("job_id", jobId);
        fragment.setArguments(args);
        return fragment;
    }

    public static JobDetailsBottomSheet newInstance(String slug) {
        JobDetailsBottomSheet fragment = new JobDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString("job_slug", slug);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            jobId = getArguments().getInt("job_id", -1);
            jobSlug = getArguments().getString("job_slug");
        }
        repository = new JobRepository(requireActivity().getApplication());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BottomSheetJobDetailsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(JobsViewModel.class);
        savedJobsViewModel = new ViewModelProvider(requireActivity()).get(SavedJobsViewModel.class);

        loadJobDetails();
        setupClickListeners();
        
        // MAX REVENUE: Show pre-cached interstitial ad immediately on opening details
        if (AdManager.getInstance().isInterstitialAdLoaded()) {
            InterstitialAd interstitialAd = AdManager.getInstance().getInterstitialAd();
            interstitialAd.show(requireActivity());
            AdManager.getInstance().clearInterstitialAd();
            // Pre-cache the next one immediately for the next view
            AdManager.getInstance().loadInterstitialAd(requireContext());
        }

        // Load banner ads
        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adViewDescription.loadAd(adRequest);
        binding.adViewDetails.loadAd(adRequest);
    }

    private void loadJobDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        
        if (jobId != -1) {
            repository.fetchJobDetails(jobId, new JobRepository.ResponseListener<JobEntity>() {
                @Override
                public void onResponse(JobEntity result) {
                    onJobLoaded(result);
                }

                @Override
                public void onError(String error) {
                    onLoadError(error);
                }
            });
        } else if (jobSlug != null) {
            viewModel.fetchJobDetailsBySlug(jobSlug);
            viewModel.getJobDetails().observe(getViewLifecycleOwner(), job -> {
                if (job != null) {
                    currentJob = job;
                    displayJobDetails(job);
                    updateSaveButtonState(job.getId());
                    binding.progressBar.setVisibility(View.GONE);
                    binding.contentLayout.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void onJobLoaded(JobEntity result) {
        currentJob = Job.fromEntity(result);
        requireActivity().runOnUiThread(() -> {
            displayJobDetails(currentJob);
            updateSaveButtonState(currentJob.getId());
            binding.progressBar.setVisibility(View.GONE);
            binding.contentLayout.setVisibility(View.VISIBLE);
        });
    }

    private void onLoadError(String error) {
        requireActivity().runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Offline: " + error, Toast.LENGTH_SHORT).show();
        });
    }

    private void displayJobDetails(Job job) {
        binding.tvTitle.setText(job.getTitle());
        binding.tvCompany.setText(job.getCompany());
        binding.tvLocation.setText(job.getLocation());
        binding.tvDate.setText(job.getFormattedDate());
        binding.tvJobType.setText(job.getJobType());

        if (job.getFeaturedImage() != null && !job.getFeaturedImage().isEmpty()) {
            Glide.with(this).load(job.getFeaturedImage()).into(binding.ivLogo);
        }

        String fullDescription = job.getContent();
        if (fullDescription != null && !fullDescription.isEmpty()) {
            // MAX REVENUE: Split content to show ad between content if it's long enough
            if (fullDescription.length() > 600) {
                int midIndex = fullDescription.indexOf(" ", fullDescription.length() / 2);
                if (midIndex != -1) {
                    String firstHalf = fullDescription.substring(0, midIndex);
                    String secondHalf = fullDescription.substring(midIndex);
                    
                    binding.tvDescription.setText(Html.fromHtml(firstHalf, Html.FROM_HTML_MODE_COMPACT));
                    binding.tvDescriptionBottom.setText(Html.fromHtml(secondHalf, Html.FROM_HTML_MODE_COMPACT));
                    binding.tvDescriptionBottom.setVisibility(View.VISIBLE);
                    binding.adViewDescription.setVisibility(View.VISIBLE);
                } else {
                    binding.tvDescription.setText(Html.fromHtml(fullDescription, Html.FROM_HTML_MODE_COMPACT));
                    binding.adViewDescription.setVisibility(View.VISIBLE);
                }
            } else {
                binding.tvDescription.setText(Html.fromHtml(fullDescription, Html.FROM_HTML_MODE_COMPACT));
                binding.adViewDescription.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setupClickListeners() {
        binding.btnApply.setOnClickListener(v -> {
            if (currentJob != null) {
                applyForJob(currentJob);
            }
        });

        binding.btnShare.setOnClickListener(v -> {
            if (currentJob != null) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out this job: " + currentJob.getTitle() + "\n" + currentJob.getLink());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share Job"));
            }
        });

        binding.btnSave.setOnClickListener(v -> {
            if (currentJob != null) {
                if (savedJobsViewModel.isJobSaved(currentJob.getId())) {
                    savedJobsViewModel.removeJob(currentJob.getId());
                } else {
                    savedJobsViewModel.saveJob(currentJob);
                }
                updateSaveButtonState(currentJob.getId());
            }
        });
    }

    private void applyForJob(Job job) {
        String application = job.getApplication();
        if (application != null && !application.isEmpty()) {
            String cleanApp = application.trim();

            if (cleanApp.contains("@") && cleanApp.contains(".")) {
                // Treat as email
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + cleanApp));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Job Application: " + job.getTitle());
                intent.putExtra(Intent.EXTRA_TEXT, createEmailBody(job));

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    offerEmailCopy(cleanApp);
                }
            } else if (cleanApp.startsWith("http://") || cleanApp.startsWith("https://")) {
                // Treat as URL
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(cleanApp));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    offerEmailCopy(cleanApp);
                }
            } else {
                offerEmailCopy(cleanApp);
            }
        } else {
            Toast.makeText(getContext(), "Application link not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void offerEmailCopy(String email) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Application Method")
                .setMessage("Would you like to copy the application details to your clipboard?")
                .setPositiveButton("Copy", (dialog, which) -> {
                    ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Job Application", email);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), "Copied: " + email, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String createEmailBody(Job job) {
        return "Dear Hiring Manager,\n\n" +
                "I am writing to apply for the position of " + job.getTitle() +
                " that I found through Zambia Job Alerts.\n\n" +
                "Please find my application materials attached.\n\n" +
                "Thank you for your consideration.\n\n" +
                "Sincerely,\n" +
                "[Your Name]\n" +
                "[Your Phone Number]\n" +
                "[Your Email Address]";
    }

    private void updateSaveButtonState(int jobId) {
        if (savedJobsViewModel.isJobSaved(jobId)) {
            binding.btnSave.setImageResource(R.drawable.ic_bookmark_filled);
        } else {
            binding.btnSave.setImageResource(R.drawable.ic_bookmark_border);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
