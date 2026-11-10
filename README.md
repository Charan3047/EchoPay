<div align="center">

# EchoPay

### Your UPI and bank spending, organized privately on your phone.

Track transaction notifications, understand spending by category, plan monthly budgets, and export your records when you choose.

**Kotlin · Jetpack Compose · Room · Android 7.0+**

[Features](#what-you-get) · [Quickstart](#quickstart) · [Privacy](#privacy-first-by-design) · [Release](#build-a-release) · [Contributing](#contributing)

</div>

---

## Why EchoPay

Payment alerts are easy to lose in a busy notification shade. EchoPay turns supported bank and UPI notifications into a searchable local transaction history, so you can see where your money is going without connecting a bank account or sending your spending data to an EchoPay cloud service.

> EchoPay tracks expenses; it does not make payments, connect to bank accounts, or independently verify settlement with a bank.

## What you get

| Feature | What it does |
| --- | --- |
| **Automatic transaction tracking** | Reads supported bank, UPI, payment, and SMS notifications after Notification access is enabled. |
| **Searchable transaction history** | Browse, search, and filter recorded income and expenses by date and type. |
| **On-device categorization** | Suggests a category using a bundled TF-IDF and logistic-regression model. Low-confidence suggestions can be reviewed. |
| **Manual entries** | Add an expense or income yourself, or preview details parsed from a pasted message before saving. |
| **Category editing** | Change a transaction's category, including **Other**, whenever a suggestion needs correction. |
| **Monthly budgets** | Set optional limits for categories and see the month's spending progress. |
| **Spending insights** | Explore monthly category and merchant totals, including interactive chart details. |
| **Excel export** | Save a spreadsheet of structured transaction data to a location you select. |
| **Personal settings** | Add a display name and switch between light and dark appearance. |

## How it works

1. **Enable Notification access.** Android asks you to grant EchoPay special access in Settings. The current onboarding requires it to continue.
2. **Receive a supported payment alert.** EchoPay checks notifications from supported sources and filters for likely completed transactions.
3. **Review the saved entry.** Parsed fields are categorized on-device and stored in the local Room database. Check uncertain categories and edit or remove entries as needed.
4. **Understand and export.** Use history, budgets, and insights to review your spending, then export a spreadsheet when you want a separate copy.

Notification wording differs across banks and payment apps. Detection is best-effort and may miss or misread an alert; EchoPay is not a bank statement or source of payment confirmation.

## Quickstart

### Requirements

- Android Studio and Android SDK Platform 37 (the project uses `compileSdk 37`).
- JDK 25 for the configured Gradle toolchain.
- Android 7.0 / API 24 or newer to install the app.

### Open the project

```bash
git clone https://github.com/Charan3047/EchoPay.git
cd EchoPay
```

Open the folder in Android Studio, allow Gradle to sync, and run the `app` configuration on an Android device or emulator. Install the required SDK platform if Android Studio prompts you. During first-run setup, enable EchoPay's Notification access in Android Settings to enter the app and use automatic capture.

### Build a debug APK

macOS / Linux:

```bash
./gradlew assembleDebug
```

Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Build a release

In Android Studio, select **Build > Generate Signed Bundle / APK** and follow the signing wizard. Share the signed APK through a GitHub Release or another trusted download location. Keep your signing key and passwords private; never add them to Git. Do not commit generated APK/AAB files to the source tree.

The repository includes a [Google Play listing draft](PLAY_STORE_LISTING_DRAFT.md) and a [privacy policy draft](PRIVACY_POLICY.md). Review and publish accurate store and privacy information before distributing the app.

## Privacy first by design

- Transaction detection and categorization run on the device.
- Notification text is processed temporarily to extract structured fields; the original message is not intended to be stored or logged.
- Transaction records, budgets, and app preferences are kept in app-private local storage. The app does not require an account or EchoPay server.
- Android backup is disabled in the current app configuration.
- Excel exports are saved to the destination you choose and are then outside EchoPay's private storage.
- Notification access is sensitive. The current onboarding requires it; you can revoke it later in Android Settings.

Read [`PRIVACY_POLICY.md`](PRIVACY_POLICY.md) for the full policy draft. Confirm it matches the release build before publishing.

## Built with

- **Kotlin** and **Jetpack Compose** with Material 3 for the Android app.
- **Room** for local transaction storage.
- **NotificationListenerService** for user-authorized transaction alert capture.
- **TF-IDF and logistic regression** for on-device category suggestions.
- **Kotlin coroutines** for background data work and spreadsheet export.
- **Android document picker** for user-selected Excel export destinations.

## Project structure

```text
EchoPay/
├── app/
│   └── src/
│       ├── main/         # Compose UI, notification service, parser, database, model assets
│       ├── test/         # Local unit tests
│       └── androidTest/  # Android instrumentation tests
├── gradle/               # Gradle wrapper and dependency catalog
├── PRIVACY_POLICY.md     # Privacy policy draft
└── PLAY_STORE_LISTING_DRAFT.md
```

## Limitations

- Automatic tracking works only for supported notification sources and recognizable alerts.
- Merchant, payment method, and transaction type may be missing or estimated from the alert wording.
- Suggested categories can be wrong; users can correct them in transaction details.
- Data is local to the installation. Export a spreadsheet if you need a separate backup.
- A successful payment notification does not mean EchoPay has independently confirmed the final bank settlement.

## Contributing

Bug reports and focused improvements are welcome. For transaction parsing issues, use synthetic examples only. Do not post real notification text, account numbers, phone numbers, UPI IDs, or other personal financial information in issues, screenshots, logs, or test fixtures.

## License

No license is currently included. Contact the repository owner before reusing or redistributing this project.
