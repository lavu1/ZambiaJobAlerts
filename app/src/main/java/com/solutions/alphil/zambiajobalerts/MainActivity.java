package com.solutions.alphil.zambiajobalerts;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.play.core.review.ReviewException;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.review.model.ReviewErrorCode;
import com.solutions.alphil.zambiajobalerts.classes.JobDetailsBottomSheet;
import com.solutions.alphil.zambiajobalerts.databinding.ActivityMainBinding;
import com.solutions.alphil.zambiajobalerts.ui.aigenerate.CVGeneratorFragment;
import com.solutions.alphil.zambiajobalerts.ui.jobs.JobsListFragment;
import com.solutions.alphil.zambiajobalerts.ui.home.HomeFragment;
import com.solutions.alphil.zambiajobalerts.ui.gallery.GalleryFragment;
import com.solutions.alphil.zambiajobalerts.ui.postjob.PostJobFragment;
import com.solutions.alphil.zambiajobalerts.ui.savedjobs.SavedJobsFragment;
import com.solutions.alphil.zambiajobalerts.ui.savedjobs.SavedJobsReminderWorker;
import com.solutions.alphil.zambiajobalerts.ui.services.ServicesFragment;
import com.solutions.alphil.zambiajobalerts.ui.slideshow.SlideshowFragment;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String CHANNEL_ID = "job_alerts_channel";
    private static final String PREFS_NAME = "app_prefs";
    private static final String NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked";
    private static final String KEY_NOTIFICATION_DENIED = "notification_denied";
    private static final String KEY_NOTIFICATION_GRANTED = "notification_granted";
    private NavController navController;
    private SharedPreferences prefs;
    private InterstitialAd interstitialAd;
    private static final String AD_UNIT_ID = "ca-app-pub-2168080105757285/4046795138";
    private static final String PREFS_NAMEC = "app_prefs";
    private static final String KEY_APP_OPENS = "app_opens";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "You are not connected to the internet", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        ReviewManager manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                ReviewInfo reviewInfo = task.getResult();
            }
        });

        // App Open Ad is now handled by AppOpenManager automatically on foreground.
        // Removed the every 5th cold-start Interstitial to avoid double ads.

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadAndShowInterstitialAd();
                if (navController != null) {
                    navController.navigate(R.id.nav_jobs);
                } else {
                    loadFragment(new JobsListFragment(), "PostJob");
                }
                // loadAndShowInterstitialAd();
            }
        });

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        initializeNavigation(drawer, navigationView);

        createNotificationChannel();
        setupHamburgerMenu();
        checkNotificationPermission();

        handleNotificationIntent(getIntent());
        handleDeepLink(getIntent());
        checkNotificationPermission();
        scheduleSavedJobsReminder();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                prefs.edit()
                        .putBoolean(KEY_NOTIFICATION_GRANTED, true)
                        .putBoolean(KEY_NOTIFICATION_DENIED, false)
                        .apply();

                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();

            } else {
                prefs.edit()
                        .putBoolean(KEY_NOTIFICATION_GRANTED, false)
                        .putBoolean(KEY_NOTIFICATION_DENIED, true)
                        .apply();

                Toast.makeText(this, "Notifications are disabled. You may be asked again next time you open the app.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void scheduleSavedJobsReminder() {
        PeriodicWorkRequest reminderRequest = new PeriodicWorkRequest.Builder(
                SavedJobsReminderWorker.class,
                6, TimeUnit.HOURS, // Remind every 6 hours
                1, TimeUnit.HOURS
        ).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "SavedJobsReminder",
                ExistingPeriodicWorkPolicy.REPLACE, // Ensure interval update
                reminderRequest
        );
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    private void handleDeepLink(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri data = intent.getData();
            String path = data.getPath();

            if (path != null && (path.startsWith("/job/") || path.startsWith("/jobs/"))) {
                String prefix = path.startsWith("/jobs/") ? "/jobs/" : "/job/";
                String identifier = path.substring(prefix.length()).replaceAll("/", "");
                if (!identifier.isEmpty()) {
                    // Open the job first. JobDetailsBottomSheet will show one rewarded ad after the job has loaded.
                    navigateToJobDetails(identifier, true);
                }
            }
        }
    }

    private void navigateToJobDetails(String identifier) {
        navigateToJobDetails(identifier, false);
    }

    private void navigateToJobDetails(String identifier, boolean openedFromDeepLink) {
        try {
            int jobId = Integer.parseInt(identifier);
            JobDetailsBottomSheet.newInstance(jobId, openedFromDeepLink)
                    .show(getSupportFragmentManager(), "JobDetails");
        } catch (NumberFormatException e) {
            JobDetailsBottomSheet.newInstance(identifier, openedFromDeepLink)
                    .show(getSupportFragmentManager(), "JobDetails");
        }
    }

    private void loadAndShowInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        if (!isFinishing() && !isDestroyed()) {
                            interstitialAd.show(MainActivity.this);
                        }
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        interstitialAd = null;
                    }
                });
    }

    private void initializeNavigation(DrawerLayout drawer, NavigationView navigationView) {
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_jobs, R.id.nav_gallery, R.id.nav_post_job, R.id.nav_rewards, R.id.nav_saved_jobs, R.id.nav_slideshow, R.id.nav_ai, R.id.nav_documents, R.id.nav_terms)
                .setOpenableLayout(drawer)
                .build();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
            NavigationUI.setupWithNavController(navigationView, navController);
        }
    }

    private void setupHamburgerMenu() {
        binding.appBarMain.toolbar.setNavigationOnClickListener(v -> binding.drawerLayout.open());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {

                prefs.edit()
                        .putBoolean(KEY_NOTIFICATION_GRANTED, true)
                        .putBoolean(KEY_NOTIFICATION_DENIED, false)
                        .apply();
                return;
            }

            boolean deniedBefore = prefs.getBoolean(KEY_NOTIFICATION_DENIED, false);

            if (deniedBefore && ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.POST_NOTIFICATIONS)) {

                new MaterialAlertDialogBuilder(this)
                        .setTitle("Enable Notifications")
                        .setMessage("Please allow notifications so you can receive the latest job alerts.")
                        .setPositiveButton("Allow", (dialog, which) -> ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                NOTIFICATION_PERMISSION_REQUEST_CODE
                        ))
                        .setNegativeButton("Not Now", null)
                        .show();

            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }
/*
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }*/

    private void handleNotificationIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String jobSlug = intent.getStringExtra("job_slug");
        if (jobSlug != null && !jobSlug.isEmpty()) {
            navigateToJobDetails(jobSlug, false);
            return;
        }

        if (intent.hasExtra("open_job_id") || intent.hasExtra("job_id")) {
            int jobId = intent.getIntExtra("open_job_id", intent.getIntExtra("job_id", -1));
            if (jobId != -1) {
                navigateToJobDetails(String.valueOf(jobId), false);
            }
        }
    }

    private void loadFragment(Fragment fragment, String tag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.nav_host_fragment_content_main, fragment, tag);
        transaction.addToBackStack(tag);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem notificationItem = menu.findItem(R.id.action_notifications);
        if (notificationItem != null) {
            boolean enabled = prefs.getBoolean("notifications_enabled", true);
            notificationItem.setIcon(enabled ? R.drawable.ic_notifications_on : R.drawable.ic_notifications_off);
            notificationItem.setTitle(enabled ? "Disable Notifications" : "Enable Notifications");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_notifications) {
            boolean currentState = prefs.getBoolean("notifications_enabled", true);
            prefs.edit().putBoolean("notifications_enabled", !currentState).apply();
            if (!currentState) {
                checkNotificationPermission();
            }
            invalidateOptionsMenu();
            Toast.makeText(this, !currentState ? "Notifications Enabled" : "Notifications Muted", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_settings) {
            if (navController != null) {
                navController.navigate(R.id.nav_jobs);
            }
            return true;
        } else if (id == R.id.action_notification_settings) {
            Intent intent = new Intent();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            } else {
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", getPackageName());
                intent.putExtra("app_uid", getApplicationInfo().uid);
            }
            startActivity(intent);
            return true;
        } else if (id == R.id.action_share) {
            shareApp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareApp() {
        String playStoreUrl = "https://play.google.com/store/apps/details?id=com.solutions.alphil.zambiajobalerts";
        String shareText = "Check out Zambia Job Alerts app for the latest job opportunities in Zambia! Download now: " + playStoreUrl;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Zambia Job Alerts via"));
    }

    @Override
    public boolean onSupportNavigateUp() {
        return (navController != null && NavigationUI.navigateUp(navController, mAppBarConfiguration))
                || super.onSupportNavigateUp();
    }
}
