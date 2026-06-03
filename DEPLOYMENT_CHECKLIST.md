# Deployment Checklist

## Android / Play Store

- Provide release signing values through Gradle properties or environment variables:
  - `RELEASE_STORE_FILE`
  - `RELEASE_STORE_PASSWORD`
  - `RELEASE_KEY_ALIAS`
  - `RELEASE_KEY_PASSWORD`
- Build the Play Store artifact:
  - `./gradlew :app:bundleRelease`
- Upload `app/build/outputs/bundle/release/app-release.aab`.
- Confirm Play Console app signing, privacy declarations, ads declaration, notification permission declaration, and data safety answers.

## iOS / App Store

- Add `iosApp/ZambiaJobAlertsIOS/Resources/GoogleService-Info.plist`.
- Confirm the production iOS AdMob IDs match the App Store bundle in:
  - `shared/src/commonMain/kotlin/com/solutions/alphil/zambiajobalerts/shared/SharedAdConfig.kt`
  - `iosApp/ZambiaJobAlertsIOS/Resources/Info.plist`
- Set `DEVELOPMENT_TEAM` in the Xcode target.
- Enable Push Notifications and Associated Domains in the Apple developer portal.
- Configure Firebase Cloud Messaging with the APNs key/certificate.
- Serve `apple-app-site-association` for:
  - `https://zambiajobalerts.com/.well-known/apple-app-site-association`
  - `https://www.zambiajobalerts.com/.well-known/apple-app-site-association`
- Build/archive from `iosApp/ZambiaJobAlerts.xcworkspace`, not the `.xcodeproj`.

## Verified Local Build Commands

- `./gradlew :shared:assemble :shared:allTests :app:assembleDebug :app:assembleRelease`
- `./gradlew :app:bundleRelease`
- `cd iosApp && pod install`
- `xcodebuild -workspace ZambiaJobAlerts.xcworkspace -scheme ZambiaJobAlertsIOS -configuration Debug -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`
- `xcodebuild -workspace ZambiaJobAlerts.xcworkspace -scheme ZambiaJobAlertsIOS -configuration Release -sdk iphonesimulator -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`
- `xcodebuild -workspace ZambiaJobAlerts.xcworkspace -scheme ZambiaJobAlertsIOS -configuration Release -sdk iphoneos -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build`
