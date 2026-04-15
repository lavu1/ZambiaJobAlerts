package com.solutions.alphil.zambiajobalerts.ui.terms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.solutions.alphil.zambiajobalerts.databinding.FragmentTermsBinding;

public class TermsFragment extends Fragment {

    private FragmentTermsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTermsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        MobileAds.initialize(requireContext());

        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adViewTermsTop.loadAd(adRequest);
        binding.adViewTermsMiddle.loadAd(adRequest);
        binding.adViewTermsBottom.loadAd(adRequest);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}