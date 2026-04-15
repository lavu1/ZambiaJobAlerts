package com.solutions.alphil.zambiajobalerts.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.solutions.alphil.zambiajobalerts.R;
import com.solutions.alphil.zambiajobalerts.classes.Job;
import com.solutions.alphil.zambiajobalerts.classes.JobDetailsBottomSheet;
import com.solutions.alphil.zambiajobalerts.classes.JobsAdapter;
import com.solutions.alphil.zambiajobalerts.databinding.FragmentHomeBinding;
import com.solutions.alphil.zambiajobalerts.ui.jobs.JobsViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private JobsAdapter adapter;
    private JobsViewModel viewModel;
    private Button btnViewAllJobs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        initViews();
        setupViewModel();
        setupRecyclerView();
        setupObservers();
        MobileAds.initialize(requireContext());

        // Top Banner - Standard
        AdView adViewTop = binding.adViewhometop;
        AdRequest adRequests = new AdRequest.Builder().build();
        adViewTop.loadAd(adRequests);

        // Bottom Banner - COLLAPSIBLE
        AdView adView = binding.adViewhome;
        Bundle extras = new Bundle();
        extras.putString("collapsible", "bottom"); // Makes the ad expand from the bottom
        AdRequest adRequestCollapsible = new AdRequest.Builder()
                .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                .build();
        adView.loadAd(adRequestCollapsible);

        return root;
    }

    private void initViews() {
        binding.tvWelcome.setText(
                "Welcome to Zambia Job Alerts – Your Gateway to Employment!\n\n" +
                        "Finding a job shouldn't be stressful. Whether you're a fresh graduate, a skilled professional, or looking for your next big career move, we connect you with real opportunities in Zambia.\n\n" +
                        "✅ Daily job updates across various industries\n" +
                        "✅ CV & application letter writing services\n" +
                        "✅ Career tips to help you succeed\n" +
                        "✅ Easy job posting for employers\n\n" +
                        "Your dream job is closer than you think. Start exploring now!"
        );

        btnViewAllJobs = binding.btnViewAllJobs;
        binding.pbLoadings.setVisibility(View.VISIBLE);

        btnViewAllJobs.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_jobs);
        });

        binding.btnRefresh.setOnClickListener(v -> {
            refreshJobs();
        });
    }

    private void setupViewModel() {
        viewModel = new ViewModelProvider(
                this,
                ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().getApplication())
        ).get(JobsViewModel.class);
        refreshJobs();
    }

    private void setupRecyclerView() {
        adapter = new JobsAdapter(new ArrayList<>(), job -> {
            JobDetailsBottomSheet detailsSheet = JobDetailsBottomSheet.newInstance(job.getId());
            detailsSheet.show(getParentFragmentManager(), "JobDetails");
        });
        binding.rvFeaturedJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvFeaturedJobs.setAdapter(adapter);
    }

    private void setupObservers() {
        viewModel.getJobs().observe(getViewLifecycleOwner(), jobs -> {
            binding.pbLoadings.setVisibility(View.GONE);

            if (jobs != null && !jobs.isEmpty()) {
                List<Job> featuredJobs = jobs.subList(0, Math.min(3, jobs.size()));
                adapter.updateDisplayList(new ArrayList<>(featuredJobs));
                binding.rvFeaturedJobs.setVisibility(View.VISIBLE);
                binding.tvNoJobs.setVisibility(View.GONE);
                binding.btnRefresh.setVisibility(View.GONE);
                binding.layoutNoJobs.setVisibility(View.GONE);
            } else {
                binding.rvFeaturedJobs.setVisibility(View.GONE);
                binding.tvNoJobs.setVisibility(View.VISIBLE);
                binding.layoutNoJobs.setVisibility(View.VISIBLE);
                binding.btnRefresh.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                binding.pbLoadings.setVisibility(View.VISIBLE);
                binding.btnRefresh.setVisibility(View.GONE);
                binding.layoutNoJobs.setVisibility(View.GONE);
            } else {
                binding.pbLoadings.setVisibility(View.GONE);
            }
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            binding.pbLoadings.setVisibility(View.GONE);
            if (error != null) {
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
                binding.layoutNoJobs.setVisibility(View.VISIBLE);
                binding.btnRefresh.setVisibility(View.VISIBLE);
            }
        });
    }

    private void refreshJobs() {
        binding.pbLoadings.setVisibility(View.VISIBLE);
        binding.layoutNoJobs.setVisibility(View.GONE);
        binding.btnRefresh.setVisibility(View.GONE);
        viewModel.loadLimitedJobs(3);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}