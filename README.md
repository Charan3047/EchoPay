# EchoPay — UPI Expense Tracker for Android

**A private, on-device expense tracker for UPI and bank transactions.**

EchoPay turns supported payment notifications into an organized transaction history. Review spending by category, set monthly budgets, correct transaction details, and export your records to an Excel spreadsheet. Transaction data stays on your device.

> EchoPay is an expense tracker. It does not initiate payments, access bank accounts, or guarantee that every bank or payment notification will be recognized.

## Features

- **Automatic transaction capture** from supported UPI, payment, bank, and SMS notifications after Notification access is enabled.
- **On-device transaction parsing and categorization**, with a review indicator when a suggested category has low confidence.
- **Transaction history** with search, date and transaction-type filters, and details for each entry.
- **Manual transaction entry** for expenses or income, including optional parsing of a pasted message before saving.
- **Editable categories**, including an `Other` category.
- **Monthly category budgets** with spending progress.
- **Spending insights** by category and merchant, plus interactive chart details.
- **Excel export** to a location selected by the user.
- **Personalized display name** and light or dark appearance.
- **Local storage** using Room; no EchoPay account or EchoPay cloud service is required.

## Privacy

EchoPay processes supported notifications on the device to identify transaction details. The original notification text is intended to remain temporary: the app stores structured transaction fields rather than the original message and does not send transaction data to an EchoPay server. Database content is excluded from Android backup in the current manifest configuration.

Notification access is a sensitive Android permission. EchoPay needs it for automatic transaction capture, and the current onboarding flow asks the user to enable it before entering the app. Only supported notification sources are considered. Detection depends on the wording used by each source, so review the transaction history and correct or remove entries when needed.

An exported spreadsheet is saved wherever you choose and is then outside EchoPay's private app storage. Protect or delete exported files as you would other personal financial records.

For the full policy, see [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md). Confirm that this policy matches the app before distributing a release.

## Requirements

- Android Studio with Android SDK Platform 37 installed (the project is configured with `compileSdk 37`).
- JDK 25 for the Gradle build, as configured by the Gradle daemon toolchain.
- Android 7.0 (API 24) or later to install and run the app (`minSdk 24`).

## Build and run

1. Clone this repository and open the project directory in Android Studio.
2. Install the required Android SDK platform when Android Studio prompts you.
3. Let Gradle sync, then run the `app` configuration on a device or emulator.
4. During setup, enable EchoPay's Notification access in Android Settings to use the current app flow and automatic capture.

To build a debug APK from a terminal:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`. Build outputs are ignored by Git. For sharing a signed release APK, use Android Studio's **Build > Generate Signed Bundle / APK** flow and distribute the resulting artifact through a GitHub Release or another trusted channel. Never commit a signing key or passwords to this repository.

## Tech stack

- Kotlin and Jetpack Compose with Material 3
- Room for on-device transaction storage
- Android `NotificationListenerService` for user-authorized notification access
- Bundled TF-IDF and logistic-regression categorization model, evaluated on-device
- Kotlin coroutines for asynchronous database and export work
- Android document picker for user-directed spreadsheet export

## Repository layout

```text
app/src/main/       Android application source and resources
app/src/test/       Local unit tests
app/src/androidTest/ Android instrumentation tests
gradle/             Gradle wrapper and dependency catalog
PRIVACY_POLICY.md   Privacy policy draft
PLAY_STORE_LISTING_DRAFT.md  Store listing draft
```

## Limitations

- Automatic detection depends on notifications being enabled and on supported apps producing recognizable transaction alerts.
- Some sources may not show merchant or payment-method details in their notifications.
- Categorization is a suggestion; users can review and change categories.
- EchoPay does not connect to a bank, verify balances, or confirm the final settlement status independently.
- Transaction records are local to the app installation. Keep your own exported backup if you need one outside the device.

## Contributing

Bug reports and focused improvements are welcome. Please avoid including real bank notifications, account identifiers, phone numbers, UPI IDs, or other personal financial information in issues, screenshots, test fixtures, or logs. Use synthetic examples when reporting parsing problems.

## License

No license is currently included. Contact the repository owner before reusing or redistributing this project.
