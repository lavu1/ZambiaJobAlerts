package com.solutions.alphil.zambiajobalerts

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewManagerFactory
import com.solutions.alphil.zambiajobalerts.classes.JobDetailsBottomSheet
import com.solutions.alphil.zambiajobalerts.shared.SharedAdConfig
import com.solutions.alphil.zambiajobalerts.shared.SharedLaunchDestination
import com.solutions.alphil.zambiajobalerts.shared.SharedLaunchRequest
import com.solutions.alphil.zambiajobalerts.shared.SharedLaunchRouter
import com.solutions.alphil.zambiajobalerts.ui.home.HomeFragment
import com.solutions.alphil.zambiajobalerts.ui.jobs.JobsListFragment
import com.solutions.alphil.zambiajobalerts.ui.savedjobs.SavedJobsReminderWorker
import kotlin.math.min
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var rootView: DrawerLayout
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var updateBanner: TextView
    private lateinit var fab: FloatingActionButton
    private lateinit var prefs: SharedPreferences
    private lateinit var appUpdateManager: AppUpdateManager

    private var navController: NavController? = null
    private var interstitialAd: InterstitialAd? = null
    private var pendingJobIdentifier: String? = null
    private var pendingJobOpenedFromDeepLink = false
    private var pendingOpenHome = false
    private var updateFlowStarted = false
    private var availableUpdateInfo: AppUpdateInfo? = null
    private var availableUpdateType: Int = AppUpdateType.IMMEDIATE

    private val appUpdateLauncher: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            updateFlowStarted = false
            if (result.resultCode != Activity.RESULT_OK) {
                Log.i(TAG, "Immediate app update flow ended with result code: ${result.resultCode}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(0, 31, 63)
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(
                this,
                "You are not connected to the internet. Some content may be unavailable.",
                Toast.LENGTH_SHORT,
            ).show()
        }

        rootView = buildMainLayout()
        setContentView(rootView)
        ensureNavHost()

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        appUpdateManager = AppUpdateManagerFactory.create(this)

        ReviewManagerFactory.create(this)
            .requestReviewFlow()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    task.result
                }
            }

        setSupportActionBar(toolbar)
        fab.setOnClickListener {
            loadAndShowInterstitialAd()
            navController?.navigate(R.id.nav_jobs) ?: loadFragment(JobsListFragment(), "PostJob")
        }

        initializeNavigation(drawerLayout, navigationView)
        createNotificationChannel()
        setupHamburgerMenu()

        val handledLaunch = savedInstanceState == null && queueLaunch(intent)
        if (!handledLaunch) {
            checkNotificationPermission()
        }
        scheduleSavedJobsReminder()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        queueLaunch(intent)
        processPendingLaunch()
    }

    private fun buildMainLayout(): DrawerLayout {
        drawerLayout = DrawerLayout(this).apply {
            id = AppViewIds.DRAWER_LAYOUT
            fitsSystemWindows = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
        }

        val coordinatorLayout = CoordinatorLayout(this).apply {
            layoutParams = DrawerLayout.LayoutParams(
                DrawerLayout.LayoutParams.MATCH_PARENT,
                DrawerLayout.LayoutParams.MATCH_PARENT,
            )
        }

        val appBarLayout = AppBarLayout(this).apply {
            layoutParams = CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        toolbar = Toolbar(this).apply {
            id = AppViewIds.TOOLBAR
            setBackgroundColor(Color.rgb(0, 31, 63))
            setTitleTextColor(Color.WHITE)
            setSubtitleTextColor(Color.WHITE)
            minimumHeight = dpToPx(64)
            setContentInsetsRelative(0, 0)
            popupTheme = R.style.Theme_ZambiaJobAlerts_PopupOverlay
            layoutParams = AppBarLayout.LayoutParams(
                AppBarLayout.LayoutParams.MATCH_PARENT,
                maxOf(resolveActionBarSize(), dpToPx(64)),
            )
        }
        appBarLayout.addView(toolbar)

        updateBanner = TextView(this).apply {
            text = "Update available. Tap to install."
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(255, 133, 27))
            typeface = Typeface.DEFAULT_BOLD
            textSize = 14f
            gravity = Gravity.CENTER
            visibility = View.GONE
            minHeight = dpToPx(44)
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            setOnClickListener { startAvailableUpdate() }
            layoutParams = AppBarLayout.LayoutParams(
                AppBarLayout.LayoutParams.MATCH_PARENT,
                AppBarLayout.LayoutParams.WRAP_CONTENT,
            )
        }
        appBarLayout.addView(updateBanner)

        val navHostContainer = FragmentContainerView(this).apply {
            id = AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN
        }
        val navHostParams = CoordinatorLayout.LayoutParams(
            CoordinatorLayout.LayoutParams.MATCH_PARENT,
            CoordinatorLayout.LayoutParams.MATCH_PARENT,
        ).apply {
            behavior = AppBarLayout.ScrollingViewBehavior()
        }

        fab = FloatingActionButton(this).apply {
            id = AppViewIds.FAB
            setImageResource(android.R.drawable.ic_input_add)
            visibility = View.GONE
        }
        val fabMargin = resources.getDimensionPixelSize(R.dimen.fab_margin)
        val fabParams = CoordinatorLayout.LayoutParams(
            CoordinatorLayout.LayoutParams.WRAP_CONTENT,
            CoordinatorLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            marginEnd = fabMargin
            bottomMargin = dpToPx(16)
        }

        coordinatorLayout.addView(appBarLayout)
        coordinatorLayout.addView(navHostContainer, navHostParams)
        coordinatorLayout.addView(fab, fabParams)

        navigationView = NavigationView(this).apply {
            id = AppViewIds.NAV_VIEW
            fitsSystemWindows = true
            itemTextColor = ColorStateList.valueOf(Color.rgb(0, 31, 63))
            itemIconTintList = ColorStateList.valueOf(Color.rgb(0, 31, 63))
            addHeaderView(createNavigationHeader())
            populateDrawerMenu(menu)
        }
        val drawerWidth = min(resources.displayMetrics.widthPixels - dpToPx(56), dpToPx(320))

        drawerLayout.addView(coordinatorLayout)
        drawerLayout.addView(
            navigationView,
            DrawerLayout.LayoutParams(
                drawerWidth,
                DrawerLayout.LayoutParams.MATCH_PARENT,
            ).apply {
                gravity = GravityCompat.START
            },
        )
        applySystemBarInsets(drawerLayout, appBarLayout, navHostContainer, navigationView, fab)

        return drawerLayout
    }

    private fun applySystemBarInsets(
        root: DrawerLayout,
        appBarLayout: AppBarLayout,
        navHostContainer: FragmentContainerView,
        navigationView: NavigationView,
        fab: FloatingActionButton,
    ) {
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val bottomInset = maxOf(systemBars.bottom, navigationBars.bottom, dpToPx(72))
            appBarLayout.setPadding(0, systemBars.top, 0, 0)
            navHostContainer.setPadding(0, 0, 0, bottomInset)
            (navHostContainer.layoutParams as? CoordinatorLayout.LayoutParams)?.let { params ->
                if (params.bottomMargin != bottomInset) {
                    params.bottomMargin = bottomInset
                    navHostContainer.layoutParams = params
                }
            }
            navigationView.setPadding(0, systemBars.top, 0, bottomInset)
            (fab.layoutParams as? CoordinatorLayout.LayoutParams)?.let { params ->
                val desiredBottomMargin = bottomInset + dpToPx(16)
                if (params.bottomMargin != desiredBottomMargin) {
                    params.bottomMargin = desiredBottomMargin
                    fab.layoutParams = params
                }
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun createNavigationHeader(): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(255, 171, 64), Color.rgb(255, 109, 0)),
            )
            setPadding(
                resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                resources.getDimensionPixelSize(R.dimen.activity_vertical_margin),
                resources.getDimensionPixelSize(R.dimen.activity_horizontal_margin),
                resources.getDimensionPixelSize(R.dimen.activity_vertical_margin),
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.nav_header_height),
            )

            addView(
                ImageView(this@MainActivity).apply {
                    contentDescription = getString(R.string.nav_header_desc)
                    setImageResource(R.drawable.img)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    setPadding(0, resources.getDimensionPixelSize(R.dimen.nav_header_vertical_spacing), 0, 0)
                },
                LinearLayout.LayoutParams(dpToPx(113), dpToPx(82)),
            )

            addView(
                TextView(this@MainActivity).apply {
                    text = getString(R.string.nav_header_title)
                    setTextColor(Color.WHITE)
                    typeface = Typeface.DEFAULT_BOLD
                    textSize = 16f
                    setPadding(0, resources.getDimensionPixelSize(R.dimen.nav_header_vertical_spacing), 0, 0)
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )

            addView(
                TextView(this@MainActivity).apply {
                    text = getString(R.string.nav_header_subtitle)
                    setTextColor(Color.WHITE)
                    textSize = 14f
                },
            )
        }

    private fun ensureNavHost() {
        val existingHost = supportFragmentManager
            .findFragmentById(AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN) as? NavHostFragment
        if (existingHost != null) {
            return
        }

        val host = NavHostFragment.create(R.navigation.mobile_navigation)
        supportFragmentManager.beginTransaction()
            .replace(AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN, host)
            .setPrimaryNavigationFragment(host)
            .commitNow()
    }

    private fun resolveActionBarSize(): Int {
        val typedValue = TypedValue()
        return if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        } else {
            dpToPx(56)
        }
    }

    private fun resolveThemeColor(attr: Int, fallback: Int): Int {
        val typedValue = TypedValue()
        return if (theme.resolveAttribute(attr, typedValue, true)) {
            typedValue.data
        } else {
            fallback
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume()
        checkForImmediateAppUpdate()
    }

    override fun onPostResume() {
        super.onPostResume()
        processPendingLaunch()
    }

    private fun checkForImmediateAppUpdate() {
        if (!::appUpdateManager.isInitialized || updateFlowStarted || isFinishing || isDestroyed) {
            return
        }

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                val updateAvailable = appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                val updateInProgress =
                    appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS

                if (updateInProgress) {
                    startAppUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                    return@addOnSuccessListener
                }

                val immediateAllowed = updateAvailable && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                val flexibleAllowed = updateAvailable && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)

                if (immediateAllowed || flexibleAllowed) {
                    availableUpdateInfo = appUpdateInfo
                    availableUpdateType = if (immediateAllowed) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
                    showUpdateBanner(appUpdateInfo)
                    showUpdateNotificationIfNeeded(appUpdateInfo)
                } else {
                    availableUpdateInfo = null
                    hideUpdateBanner()
                }
            }
            .addOnFailureListener { error ->
                Log.d(TAG, "Unable to check for app update", error)
            }
    }

    private fun startAvailableUpdate() {
        val appUpdateInfo = availableUpdateInfo
        if (appUpdateInfo == null) {
            checkForImmediateAppUpdate()
            return
        }
        startAppUpdate(appUpdateInfo, availableUpdateType)
    }

    private fun startAppUpdate(appUpdateInfo: AppUpdateInfo, updateType: Int) {
        if (updateFlowStarted || isFinishing || isDestroyed) {
            return
        }

        updateFlowStarted = appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            appUpdateLauncher,
            AppUpdateOptions.newBuilder(updateType).build(),
        )

        if (!updateFlowStarted) {
            Log.d(TAG, "App update flow was not started")
        }
    }

    private fun showUpdateBanner(appUpdateInfo: AppUpdateInfo) {
        if (!::updateBanner.isInitialized) return

        val version = appUpdateInfo.availableVersionCode()
        updateBanner.text = if (version > 0) {
            "Update available. Tap to install the latest version."
        } else {
            "Update available. Tap to install."
        }
        updateBanner.visibility = View.VISIBLE
    }

    private fun hideUpdateBanner() {
        if (::updateBanner.isInitialized) {
            updateBanner.visibility = View.GONE
        }
    }

    @SuppressLint("MissingPermission")
    private fun showUpdateNotificationIfNeeded(appUpdateInfo: AppUpdateInfo) {
        if (!::prefs.isInitialized || !canPostNotifications()) return
        if (!prefs.getBoolean("notifications_enabled", true)) return

        val versionCode = appUpdateInfo.availableVersionCode()
        val lastNotifiedVersion = prefs.getInt(KEY_NOTIFIED_UPDATE_VERSION, -1)
        if (versionCode > 0 && lastNotifiedVersion == versionCode) return

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_UPDATE, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_UPDATE_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Zambia Job Alerts update available")
            .setContentText("Tap to open the app and install the latest update.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("A new Zambia Job Alerts update is available. Open the app to install it."))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(NOTIFICATION_UPDATE_ID, notification)
        if (versionCode > 0) {
            prefs.edit().putInt(KEY_NOTIFIED_UPDATE_VERSION, versionCode).apply()
        }
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED

            prefs.edit()
                .putBoolean(KEY_NOTIFICATION_GRANTED, granted)
                .putBoolean(KEY_NOTIFICATION_DENIED, !granted)
                .apply()

            Toast.makeText(
                this,
                if (granted) {
                    "Notifications enabled"
                } else {
                    "Notifications are disabled. You may be asked again next time you open the app."
                },
                Toast.LENGTH_SHORT,
            ).show()
        }

        processPendingLaunch()
    }

    private fun scheduleSavedJobsReminder() {
        val reminderRequest = PeriodicWorkRequest.Builder(
            SavedJobsReminderWorker::class.java,
            6,
            TimeUnit.HOURS,
            1,
            TimeUnit.HOURS,
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "SavedJobsReminder",
            ExistingPeriodicWorkPolicy.REPLACE,
            reminderRequest,
        )
    }

    @Suppress("DEPRECATION")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetworkInfo
        return activeNetwork?.isConnected == true
    }

    private fun navigateToJobDetails(identifier: String, openedFromDeepLink: Boolean) {
        val jobId = identifier.toIntOrNull()
        val sheet = if (jobId != null) {
            JobDetailsBottomSheet.newInstance(jobId, openedFromDeepLink)
        } else {
            JobDetailsBottomSheet.newInstance(identifier, openedFromDeepLink)
        }
        sheet.show(supportFragmentManager, "JobDetails")
    }

    private fun loadAndShowInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    if (!isFinishing && !isDestroyed) {
                        ad.show(this@MainActivity)
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                }
            },
        )
    }

    private fun initializeNavigation(drawer: DrawerLayout, navigationView: NavigationView) {
        appBarConfiguration = AppBarConfiguration.Builder(
            R.id.nav_home,
            R.id.nav_jobs,
            R.id.nav_gallery,
            R.id.nav_post_job,
            R.id.nav_rewards,
            R.id.nav_saved_jobs,
            R.id.nav_slideshow,
            R.id.nav_ai,
            R.id.nav_documents,
            R.id.nav_terms,
        )
            .setOpenableLayout(drawer)
            .build()

        val navHostFragment = supportFragmentManager
            .findFragmentById(AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN) as? NavHostFragment

        navHostFragment?.let { host ->
            navController = host.navController
            NavigationUI.setupActionBarWithNavController(this, host.navController, appBarConfiguration)
            NavigationUI.setupWithNavController(navigationView, host.navController)
            setupDrawerNavigation(navigationView)
            host.navController.addOnDestinationChangedListener { _, destination, _ ->
                navigationView.setCheckedItem(destination.id)
                fab.visibility = View.GONE
                setupHamburgerMenu()
            }
        }
    }

    private fun setupDrawerNavigation(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { item ->
            val destinationId = item.itemId
            if (isDrawerDestination(destinationId)) {
                navigateTopLevel(destinationId, null)
                item.isChecked = true
                true
            } else {
                false
            }
        }
    }

    private fun isDrawerDestination(destinationId: Int): Boolean =
        destinationId == R.id.nav_home ||
            destinationId == R.id.nav_jobs ||
            destinationId == R.id.nav_saved_jobs ||
            destinationId == R.id.nav_post_job ||
            destinationId == R.id.nav_gallery ||
            destinationId == R.id.nav_slideshow ||
            destinationId == R.id.nav_ai ||
            destinationId == R.id.nav_rewards ||
            destinationId == R.id.nav_documents ||
            destinationId == R.id.nav_terms

    private fun populateDrawerMenu(menu: Menu) {
        menu.clear()
        menu.setGroupCheckable(DRAWER_MENU_GROUP_ID, true, true)
        addDrawerItem(menu, R.id.nav_home, 0, R.string.menu_home, R.drawable.ic_home)
        addDrawerItem(menu, R.id.nav_jobs, 1, R.string.menu_jobs, R.drawable.jobs)
        addDrawerItem(menu, R.id.nav_saved_jobs, 2, R.string.menu_saved_jobs, R.drawable.ic_menu_slideshow)
        addDrawerItem(menu, R.id.nav_post_job, 3, R.string.menu_post_job, R.drawable.ic_menu_send)
        addDrawerItem(menu, R.id.nav_gallery, 4, R.string.menu_gallery, R.drawable.ic_tips)
        addDrawerItem(menu, R.id.nav_slideshow, 5, R.string.menu_slideshow, R.drawable.ic_news_posts)
        addDrawerItem(menu, R.id.nav_ai, 6, R.string.menu_aiServices, R.drawable.ic_news_posts)
        addDrawerItem(menu, R.id.nav_rewards, 7, R.string.menu_rewards, R.drawable.ic_news_posts)
        addDrawerItem(menu, R.id.nav_documents, 8, R.string.menu_documents, R.drawable.ic_news_posts)
        addDrawerItem(menu, R.id.nav_terms, 9, R.string.menu_terms, R.drawable.ic_menu_slideshow)
    }

    private fun addDrawerItem(menu: Menu, itemId: Int, order: Int, titleRes: Int, iconRes: Int) {
        menu.add(DRAWER_MENU_GROUP_ID, itemId, order, getString(titleRes))
            .setIcon(iconRes)
    }

    private fun setupHamburgerMenu() {
        toolbar.navigationIcon = HamburgerDrawable(
            sizePx = dpToPx(32),
            strokePx = dpToPx(3).toFloat(),
            color = Color.WHITE,
        )
        toolbar.overflowIcon = OverflowMenuDrawable(
            sizePx = dpToPx(32),
            dotRadiusPx = dpToPx(2).toFloat(),
            color = Color.WHITE,
        )
        toolbar.navigationContentDescription = getString(R.string.app_name)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.channel_description)
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            prefs.edit()
                .putBoolean(KEY_NOTIFICATION_GRANTED, true)
                .putBoolean(KEY_NOTIFICATION_DENIED, false)
                .apply()
            return
        }

        val deniedBefore = prefs.getBoolean(KEY_NOTIFICATION_DENIED, false)
        if (deniedBefore && ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        ) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Enable Notifications")
                .setMessage("Please allow notifications so you can receive the latest job alerts.")
                .setPositiveButton("Allow") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE,
                    )
                }
                .setNegativeButton("Not Now", null)
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE,
            )
        }
    }

    private fun queueLaunch(intent: Intent?): Boolean {
        val request = parseLaunchRequest(intent) ?: return false

        pendingOpenHome = request.openHome
        pendingJobIdentifier = request.identifier
        pendingJobOpenedFromDeepLink = request.openedFromDeepLink
        clearJobLaunchMarkers(intent)
        return true
    }

    private fun processPendingLaunch() {
        if ((pendingJobIdentifier == null && !pendingOpenHome) || isFinishing || isDestroyed) {
            return
        }

        if (supportFragmentManager.isStateSaved) {
            rootView.post { processPendingLaunch() }
            return
        }

        if (pendingOpenHome) {
            pendingOpenHome = false
            pendingJobIdentifier = null
            pendingJobOpenedFromDeepLink = false
            navigateHome()
            return
        }

        val identifier = pendingJobIdentifier ?: return
        val openedFromDeepLink = pendingJobOpenedFromDeepLink
        pendingJobIdentifier = null
        pendingJobOpenedFromDeepLink = false

        navigateToJobDetails(identifier, openedFromDeepLink)
    }

    private fun parseLaunchRequest(intent: Intent?): LaunchRequest? {
        intent ?: return null

        if (Intent.ACTION_VIEW == intent.action && intent.data != null) {
            parseUriLaunch(intent.data, true)?.let { return it }
        }

        val jobSlug = intent.getStringExtra("job_slug")
        if (!jobSlug.isNullOrEmpty()) {
            return LaunchRequest.forJob(jobSlug, false)
        }

        val jobIdentifier = readJobIdentifierExtra(intent)
        if (jobIdentifier != null) {
            return LaunchRequest.forJob(jobIdentifier, false)
        }

        val launchUrl = firstNonEmptyExtra(intent, "deep_link", "deeplink", "link", "url")
        if (launchUrl != null) {
            parseUriLaunch(Uri.parse(normalizeLaunchUrl(launchUrl)), true)?.let { return it }
        }

        return if (intent.getBooleanExtra("open_home", false)) {
            LaunchRequest.forHome()
        } else {
            null
        }
    }

    private fun parseUriLaunch(data: Uri?, openedFromDeepLink: Boolean): LaunchRequest? {
        data ?: return null
        val request = SharedLaunchRouter.parseUri(
            data.scheme,
            data.host,
            data.path,
            openedFromDeepLink,
        )
        return fromSharedLaunchRequest(request)
    }

    private fun navigateHome() {
        navigateTopLevel(R.id.nav_home, null)
    }

    private fun navigateTopLevel(destinationId: Int, args: Bundle?) {
        val existingJobDetails = supportFragmentManager.findFragmentByTag("JobDetails")
        if (existingJobDetails is DialogFragment) {
            existingJobDetails.dismissAllowingStateLoss()
        }

        drawerLayout.closeDrawer(GravityCompat.START)

        val controller = navController
        if (controller != null) {
            if (controller.currentDestination?.id == destinationId && args == null) {
                return
            }

            val navOptions = NavOptions.Builder()
                .setPopUpTo(controller.graph.startDestinationId, false)
                .setLaunchSingleTop(true)
                .setRestoreState(false)
                .build()
            controller.navigate(destinationId, args, navOptions)
        } else {
            loadFragment(HomeFragment(), "Home")
        }
    }

    private fun navigateToJobsSearch(query: String?) {
        val safeQuery = query?.trim().orEmpty()
        val currentFragment = currentPrimaryFragment()
        if (currentFragment is JobsListFragment) {
            currentFragment.applySearchQuery(safeQuery)
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        val args = Bundle().apply {
            putString(ARG_SEARCH_QUERY, safeQuery)
        }
        navigateTopLevel(R.id.nav_jobs, args)
    }

    private fun currentPrimaryFragment(): Fragment? {
        val navHostFragment = supportFragmentManager
            .findFragmentById(AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN) as? NavHostFragment
        return navHostFragment?.childFragmentManager?.primaryNavigationFragment
    }

    private fun readJobIdentifierExtra(intent: Intent): String? {
        val extras = intent.extras ?: return null

        readStringOrIntExtra(extras, "open_job_id")?.let { identifier ->
            if (identifier != "-1") return identifier
        }

        readStringOrIntExtra(extras, "job_id")?.let { identifier ->
            if (identifier != "-1") return identifier
        }

        return null
    }

    private fun firstNonEmptyExtra(intent: Intent, vararg keys: String): String? {
        val extras = intent.extras ?: return null
        for (key in keys) {
            val value = extras.getString(key)?.trim()?.takeIf { it.isNotEmpty() }
            if (value != null) {
                return value
            }
        }
        return null
    }

    private fun readStringOrIntExtra(extras: Bundle, key: String): String? {
        if (!extras.containsKey(key)) {
            return null
        }

        val text = extras.getString(key)?.trim()?.takeIf { it.isNotEmpty() }
        if (text != null) {
            return text
        }

        val intValue = extras.getInt(key, Int.MIN_VALUE)
        return if (intValue == Int.MIN_VALUE) null else intValue.toString()
    }

    private fun normalizeLaunchUrl(value: String): String =
        SharedLaunchRouter.normalizeLaunchUrl(value)

    private fun fromSharedLaunchRequest(request: SharedLaunchRequest?): LaunchRequest? {
        request ?: return null

        if (request.destination == SharedLaunchDestination.HOME) {
            return LaunchRequest.forHome()
        }

        val identifier = request.identifier
        if (identifier.isNullOrEmpty()) {
            return null
        }

        return LaunchRequest.forJob(identifier, request.openedFromDeepLink)
    }

    private fun clearJobLaunchMarkers(intent: Intent?) {
        intent ?: return

        if (Intent.ACTION_VIEW == intent.action) {
            intent.action = Intent.ACTION_MAIN
            intent.data = null
        }

        intent.removeExtra("job_slug")
        intent.removeExtra("open_job_id")
        intent.removeExtra("job_id")
        intent.removeExtra("deep_link")
        intent.removeExtra("deeplink")
        intent.removeExtra("link")
        intent.removeExtra("url")
        intent.removeExtra("open_home")
    }

    private fun loadFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(AppViewIds.NAV_HOST_FRAGMENT_CONTENT_MAIN, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        val searchItem = menu.add(Menu.NONE, AppMenuIds.ACTION_SEARCH_JOBS, 50, getString(R.string.search_jobs))
            .setIcon(android.R.drawable.ic_menu_search)
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)

        val searchView = SearchView(this)
        searchItem.actionView = searchView
        searchView.apply {
            queryHint = getString(R.string.search_jobs)
            setSubmitButtonEnabled(true)
            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        navigateToJobsSearch(query)
                        searchItem.collapseActionView()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean = false
                },
            )
        }

        menu.add(Menu.NONE, AppMenuIds.ACTION_NOTIFICATIONS, 100, "Enable Notifications")
            .setIcon(R.drawable.ic_notifications_off)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.add(Menu.NONE, AppMenuIds.ACTION_NOTIFICATION_SETTINGS, 200, "Notification Settings")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, AppMenuIds.ACTION_SETTINGS, 300, "View Jobs")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, AppMenuIds.ACTION_SHARE, 400, "Share App")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.add(Menu.NONE, AppMenuIds.ACTION_RATE, 500, "Rate App")
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val notificationItem = menu.findItem(AppMenuIds.ACTION_NOTIFICATIONS)
        if (notificationItem != null && ::prefs.isInitialized) {
            val enabled = prefs.getBoolean("notifications_enabled", true)
            notificationItem.setIcon(
                if (enabled) R.drawable.ic_notifications_on else R.drawable.ic_notifications_off,
            )
            notificationItem.title = if (enabled) "Disable Notifications" else "Enable Notifications"
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            AppMenuIds.ACTION_NOTIFICATIONS -> {
                val currentState = prefs.getBoolean("notifications_enabled", true)
                prefs.edit().putBoolean("notifications_enabled", !currentState).apply()
                if (!currentState) {
                    checkNotificationPermission()
                }
                invalidateOptionsMenu()
                Toast.makeText(
                    this,
                    if (!currentState) "Notifications Enabled" else "Notifications Muted",
                    Toast.LENGTH_SHORT,
                ).show()
                true
            }

            AppMenuIds.ACTION_SETTINGS -> {
                navigateTopLevel(R.id.nav_jobs, null)
                true
            }

            AppMenuIds.ACTION_NOTIFICATION_SETTINGS -> {
                val intent = Intent().apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    } else {
                        action = "android.settings.APP_NOTIFICATION_SETTINGS"
                        putExtra("app_package", packageName)
                        putExtra("app_uid", applicationInfo.uid)
                    }
                }
                startActivity(intent)
                true
            }

            AppMenuIds.ACTION_SHARE -> {
                shareApp()
                true
            }

            AppMenuIds.ACTION_RATE -> {
                rateApp()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun shareApp() {
        val playStoreUrl = "https://play.google.com/store/apps/details?id=com.solutions.alphil.zambiajobalerts"
        val shareText =
            "Check out Zambia Job Alerts app for the latest job opportunities in Zambia! Download now: $playStoreUrl"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Zambia Job Alerts via"))
    }

    private fun rateApp() {
        val reviewManager = ReviewManagerFactory.create(this)
        reviewManager.requestReviewFlow()
            .addOnCompleteListener { request ->
                if (request.isSuccessful) {
                    reviewManager.launchReviewFlow(this, request.result)
                        .addOnFailureListener { openPlayStoreListing() }
                } else {
                    openPlayStoreListing()
                }
            }
    }

    private fun openPlayStoreListing() {
        val packageName = "com.solutions.alphil.zambiajobalerts"
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        try {
            startActivity(marketIntent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val canNavigateUp = navController?.let { controller ->
            ::appBarConfiguration.isInitialized &&
                NavigationUI.navigateUp(controller, appBarConfiguration)
        } == true
        return canNavigateUp || super.onSupportNavigateUp()
    }

    private data class LaunchRequest(
        val identifier: String?,
        val openedFromDeepLink: Boolean,
        val openHome: Boolean,
    ) {
        companion object {
            fun forJob(identifier: String, openedFromDeepLink: Boolean): LaunchRequest =
                LaunchRequest(identifier, openedFromDeepLink, false)

            fun forHome(): LaunchRequest =
                LaunchRequest(null, false, true)
        }
    }

    private class HamburgerDrawable(
        private val sizePx: Int,
        private val strokePx: Float,
        color: Int,
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        private val rect = RectF()

        override fun draw(canvas: Canvas) {
            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()
            val width = sizePx * 0.78f
            val left = centerX - width / 2f
            val right = centerX + width / 2f
            val spacing = sizePx * 0.22f
            listOf(-spacing, 0f, spacing).forEach { offset ->
                val top = centerY + offset - strokePx / 2f
                rect.set(left, top, right, top + strokePx)
                canvas.drawRoundRect(rect, strokePx, strokePx, paint)
            }
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
            invalidateSelf()
        }

        @Deprecated("Deprecated in Android framework")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun getIntrinsicWidth(): Int = sizePx

        override fun getIntrinsicHeight(): Int = sizePx
    }

    private class OverflowMenuDrawable(
        private val sizePx: Int,
        private val dotRadiusPx: Float,
        color: Int,
    ) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        override fun draw(canvas: Canvas) {
            val centerX = bounds.exactCenterX()
            val centerY = bounds.exactCenterY()
            val spacing = sizePx * 0.24f
            listOf(-spacing, 0f, spacing).forEach { offset ->
                canvas.drawCircle(centerX, centerY + offset, dotRadiusPx, paint)
            }
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
            invalidateSelf()
        }

        @Deprecated("Deprecated in Android framework")
        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun getIntrinsicWidth(): Int = sizePx

        override fun getIntrinsicHeight(): Int = sizePx
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
        private const val CHANNEL_ID = "job_alerts_channel"
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_NOTIFICATION_DENIED = "notification_denied"
        private const val KEY_NOTIFICATION_GRANTED = "notification_granted"
        private const val KEY_NOTIFIED_UPDATE_VERSION = "notified_update_version"
        private const val KEY_APP_OPENS = "app_opens"
        private const val ARG_SEARCH_QUERY = "search_query"
        private const val EXTRA_OPEN_UPDATE = "open_update"
        private const val NOTIFICATION_UPDATE_ID = 2208
        private const val DRAWER_MENU_GROUP_ID = 0x12030001
        private const val AD_UNIT_ID = SharedAdConfig.ANDROID_INTERSTITIAL_AD_UNIT_ID
    }
}
