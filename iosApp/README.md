# Zambia Job Alerts iOS

This is the iOS host app for the Kotlin Multiplatform `:shared` module.

Before running on a physical device:

1. Confirm `GoogleService-Info.plist` in `iosApp/ZambiaJobAlertsIOS/Resources` matches the production Firebase iOS app.
2. Confirm the production iOS AdMob IDs in `SharedAdConfig` and `Info.plist` match the App Store bundle in AdMob.
3. Run `pod install --repo-update` from `iosApp`.
4. Open `ZambiaJobAlerts.xcworkspace`.
   Do not open `ZambiaJobAlerts.xcodeproj` directly; CocoaPods frameworks such as `FBLPromises`, Firebase, and Google Mobile Ads are linked through the workspace.
5. Set the Apple development team and enable Push Notifications plus Associated Domains.

The Xcode target runs `./gradlew :shared:embedAndSignAppleFrameworkForXcode` before compiling Swift sources.
