package com.example.upiexpensetracker

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionNotificationListener : NotificationListenerService() {

    private val detector = TransactionDetector()
    private val parser = TransactionParser()

    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var database: AppDatabase

    private lateinit var vectorizer: TfidfVectorizer
    private lateinit var classifier: LogisticRegressionClassifier
    private lateinit var categorizer: TransactionCategorizer

    private val reviewThreshold = 0.70

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getDatabase(applicationContext)

        try {
            vectorizer = TfidfVectorizer.fromAssets(this)
            classifier = LogisticRegressionClassifier.fromAssets(this)
            categorizer = TransactionCategorizer(vectorizer, classifier)

            Log.d("UPI_TRACKER", "On-device categorizer initialized")
        } catch (exception: Exception) {
            // Avoid exception messages or notification values in logs.
            Log.e("UPI_TRACKER", "On-device categorizer initialization failed (${exception.javaClass.simpleName})")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        if (sbn == null) return
        // Only inspect known payment, bank, and SMS notification sources. This prevents
        // unrelated shopping, calendar, and messaging alerts from becoming transactions.
        if (sbn.packageName !in allowedSourcePackages) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title")
        val notificationText = buildList {
            extras.getCharSequence("android.text")?.toString()?.let(::add)
            extras.getCharSequence("android.bigText")?.toString()?.let(::add)
            extras.getCharSequence("android.subText")?.toString()?.let(::add)
            extras.getCharSequenceArray("android.textLines")?.forEach { add(it.toString()) }
        }.distinct().joinToString(" ").takeIf(String::isNotBlank)

        // Inspect once on the callback thread; notification text stays in memory only.
        if (!detector.isTransaction(title, notificationText)) return

        serviceScope.launch {
            try {
                if (!::categorizer.isInitialized) {
                    Log.e("UPI_TRACKER", "ML categorizer is not initialized")
                    return@launch
                }

                val transaction = parser.parse(title, notificationText)
                val amount = transaction.amount
                val transactionType = transaction.transactionType
                if (amount == null || transactionType == null) {
                    Log.w("UPI_TRACKER", "Detected transaction could not be parsed; skipped")
                    return@launch
                }

                val result = categorizer.categorize(transaction)

                val entity = TransactionEntity(
                    amount = amount,
                    merchant = transaction.merchant,
                    transactionType = transactionType,
                    paymentMethod = transaction.paymentMethod,
                    category = result.category,
                    confidence = result.confidence,
                    needsReview = result.confidence < reviewThreshold,
                    timestamp = sbn.postTime.takeIf { it > 0L } ?: System.currentTimeMillis()
                )

                val inserted = database.transactionDao().insertIfNotDuplicate(entity)

                if (inserted) {
                    Log.d(
                        "UPI_TRACKER",
                        "Transaction saved. Category=${result.category}, " +
                                "confidence=${result.confidence}, " +
                                "needsReview=${entity.needsReview}"
                    )
                } else {
                    Log.d("UPI_TRACKER", "Repeated transaction notification skipped")
                }
            } catch (exception: Exception) {
                // Exception messages and stack traces can contain untrusted input.
                Log.e("UPI_TRACKER", "Transaction processing failed (${exception.javaClass.simpleName})")
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private companion object {
        val allowedSourcePackages = setOf(
            // UPI and payment apps
            "com.google.android.apps.nbu.paisa.user", "com.phonepe.app", "net.one97.paytm",
            "in.org.npci.upiapp", "com.dreamplug.androidapp", "in.amazon.mShop.android.shopping",
            "com.mobikwik_new", "com.freecharge.android", "com.myairtelapp",
            // Common Indian bank apps
            "com.snapwork.hdfc", "com.csam.icici.bank.imobile", "com.sbi.SBIFreedomPlus",
            "com.axis.mobile", "com.msf.kbank.mobile", "com.infrasofttech.centralbankofindia",
            "com.bankofbaroda.mpassbook", "com.fss.pnbpsp", "com.kotak.bank.mobile",
            "com.idbi.mpassbook", "com.indusind.mobilebanking", "com.yesbank.mobile",
            // SMS apps, where bank transaction alerts are commonly delivered
            "com.google.android.apps.messaging", "com.samsung.android.messaging",
            "com.android.mms", "com.truecaller"
        )
    }
}
