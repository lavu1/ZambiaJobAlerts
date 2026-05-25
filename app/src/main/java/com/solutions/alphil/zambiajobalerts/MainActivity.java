package com.solutions.alphil.zambiajobalerts;

import android.Manifest;
import android.app.Activity;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
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
import androidx.fragment.app.DialogFragment;
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
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
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

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
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
    private String pendingJobIdentifier;
    private boolean pendingJobOpenedFromDeepLink;
    private boolean pendingOpenHome;
    private AppUpdateManager appUpdateManager;
    private boolean updateFlowStarted;
    private final ActivityResultLauncher<IntentSenderRequest> appUpdateLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        updateFlowStarted = false;
                        if (result.getResultCode() != Activity.RESULT_OK) {
                            Log.i(TAG, "Immediate app update flow ended with result code: " + result.getResultCode());
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "You are not connected to the internet. Some content may be unavailable.", Toast.LENGTH_SHORT).show();
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        appUpdateManager = AppUpdateManagerFactory.create(this);

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

        boolean handledLaunch = savedInstanceState == null && queueLaunch(getIntent());
        if (!handledLaunch) {
            checkNotificationPermission();
        }
        scheduleSavedJobsReminder();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        queueLaunch(intent);
        processPendingLaunch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForImmediateAppUpdate();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        processPendingLaunch();
    }

    private void checkForImmediateAppUpdate() {
        if (appUpdateManager == null || updateFlowStarted || isFinishing() || isDestroyed()) {
            return;
        }

        appUpdateManager.getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.updateAvailability()
                            == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        startImmediateUpdate(appUpdateInfo);
                    } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                            && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        startImmediateUpdate(appUpdateInfo);
                    }
                })
                .addOnFailureListener(error -> Log.d(TAG, "Unable to check for app update", error));
    }

    private void startImmediateUpdate(AppUpdateInfo appUpdateInfo) {
        if (updateFlowStarted || isFinishing() || isDestroyed()) {
            return;
        }

        updateFlowStarted = appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                appUpdateLauncher,
                AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        );

        if (!updateFlowStarted) {
            Log.d(TAG, "Immediate app update flow was not started");
        }
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

        processPendingLaunch();
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

    private boolean queueLaunch(Intent intent) {
        LaunchRequest request = parseLaunchRequest(intent);
        if (request == null) {
            return false;
        }

        pendingOpenHome = request.openHome;
        pendingJobIdentifier = request.identifier;
        pendingJobOpenedFromDeepLink = request.openedFromDeepLink;
        clearJobLaunchMarkers(intent);
        return true;
    }

    private void processPendingLaunch() {
        if ((pendingJobIdentifier == null && !pendingOpenHome) || isFinishing() || isDestroyed()) {
            return;
        }

        if (getSupportFragmentManager().isStateSaved()) {
            binding.getRoot().post(this::processPendingLaunch);
            return;
        }

        if (pendingOpenHome) {
            pendingOpenHome = false;
            pendingJobIdentifier = null;
            pendingJobOpenedFromDeepLink = false;
            navigateHome();
            return;
        }

        String identifier = pendingJobIdentifier;
        boolean openedFromDeepLink = pendingJobOpenedFromDeepLink;
        pendingJobIdentifier = null;
        pendingJobOpenedFromDeepLink = false;

        navigateToJobDetails(identifier, openedFromDeepLink);
    }

    private LaunchRequest parseLaunchRequest(Intent intent) {
        if (intent == null) {
            return null;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            LaunchRequest request = parseUriLaunch(intent.getData(), true);
            if (request != null) {
                return request;
            }
        }

        String jobSlug = intent.getStringExtra("job_slug");
        if (jobSlug != null && !jobSlug.isEmpty()) {
            return LaunchRequest.forJob(jobSlug, false);
        }

        String jobIdentifier = readJobIdentifierExtra(intent);
        if (jobIdentifier != null) {
            return LaunchRequest.forJob(jobIdentifier, false);
        }

        String launchUrl = firstNonEmptyExtra(intent, "deep_link", "deeplink", "link", "url");
        if (launchUrl != null) {
            LaunchRequest request = parseUriLaunch(Uri.parse(normalizeLaunchUrl(launchUrl)), true);
            if (request != null) {
                return request;
            }
        }

        if (intent.getBooleanExtra("open_home", false)) {
            return LaunchRequest.forHome();
        }

        return null;
    }

    private LaunchRequest parseUriLaunch(Uri data, boolean openedFromDeepLink) {
        if (data == null) {
            return null;
        }

        if (JobLaunchParser.isHomeUri(data.getScheme(), data.getHost(), data.getPath())) {
            return LaunchRequest.forHome();
        }

        String identifier = JobLaunchParser.extractIdentifier(
                data.getScheme(),
                data.getHost(),
                data.getPath()
        );
        if (identifier != null && !identifier.isEmpty()) {
            return LaunchRequest.forJob(identifier, openedFromDeepLink);
        }

        return null;
    }

    private void navigateHome() {
        Fragment existingJobDetails = getSupportFragmentManager().findFragmentByTag("JobDetails");
        if (existingJobDetails instanceof DialogFragment) {
            ((DialogFragment) existingJobDetails).dismissAllowingStateLoss();
        }

        if (navController != null) {
            if (!navController.popBackStack(R.id.nav_home, false)) {
                navController.navigate(R.id.nav_home);
            }
        } else {
            loadFragment(new HomeFragment(), "Home");
        }
    }

    private String readJobIdentifierExtra(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }

        String identifier = readNonEmptyExtra(extras, "open_job_id");
        if (identifier != null && !"-1".equals(identifier)) {
            return identifier;
        }

        identifier = readNonEmptyExtra(extras, "job_id");
        if (identifier != null && !"-1".equals(identifier)) {
            return identifier;
        }

        return null;
    }

    private String firstNonEmptyExtra(Intent intent, String... keys) {
        Bundle extras = intent.getExtras();
        if (extras == null) {
            return null;
        }

        for (String key : keys) {
            String value = readNonEmptyExtra(extras, key);
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private String readNonEmptyExtra(Bundle extras, String key) {
        Object value = extras.get(key);
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String normalizeLaunchUrl(String value) {
        String trimmedValue = value.trim();
        if (trimmedValue.startsWith("zambiajobalerts.com")
                || trimmedValue.startsWith("www.zambiajobalerts.com")) {
            return "https://" + trimmedValue;
        }
        return trimmedValue;
    }

    private void clearJobLaunchMarkers(Intent intent) {
        if (intent == null) {
            return;
        }

        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            intent.setAction(Intent.ACTION_MAIN);
            intent.setData(null);
        }

        intent.removeExtra("job_slug");
        intent.removeExtra("open_job_id");
        intent.removeExtra("job_id");
        intent.removeExtra("deep_link");
        intent.removeExtra("deeplink");
        intent.removeExtra("link");
        intent.removeExtra("url");
        intent.removeExtra("open_home");
    }

    private static final class LaunchRequest {
        private final String identifier;
        private final boolean openedFromDeepLink;
        private final boolean openHome;

        private LaunchRequest(String identifier, boolean openedFromDeepLink, boolean openHome) {
            this.identifier = identifier;
            this.openedFromDeepLink = openedFromDeepLink;
            this.openHome = openHome;
        }

        private static LaunchRequest forJob(String identifier, boolean openedFromDeepLink) {
            return new LaunchRequest(identifier, openedFromDeepLink, false);
        }

        private static LaunchRequest forHome() {
            return new LaunchRequest(null, false, true);
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
