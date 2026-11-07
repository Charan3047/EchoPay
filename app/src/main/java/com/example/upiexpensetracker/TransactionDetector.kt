package com.example.upiexpensetracker

/** Identifies completed money movements while ignoring balance, OTP and payment-status alerts. */
class TransactionDetector {

    fun isTransaction(title: String?, text: String?): Boolean {
        val message = normalize("${title.orEmpty()} ${text.orEmpty()}")
        if (message.isBlank() || excludedMessage.containsMatchIn(message)) return false

        val hasAmount = amountPattern.containsMatchIn(message) ||
            contextualAmountPattern.containsMatchIn(message)
        val hasMovement = completedMovementPattern.containsMatchIn(message)
        val explicitlyFailed = failurePattern.containsMatchIn(message)

        return hasAmount && hasMovement && !explicitlyFailed
    }

    internal fun normalize(message: String): String = message
        .lowercase()
        .replace('\u00a0', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()

    companion object {
        // Indian grouping (1,23,456) and western grouping (123,456) are both accepted.
        internal val amountPattern = Regex(
            """(?:₹|rs\.?|inr)\s*[0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?|[0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?\s*/-""",
            RegexOption.IGNORE_CASE
        )
        internal val contextualAmountPattern = Regex(
            """(?:debited|credited|paid|sent|received|transferred|withdrawn|deducted|deposited)\s+(?:by|with|of|for)\s*(?:₹|rs\.?|inr)?\s*[0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?""",
            RegexOption.IGNORE_CASE
        )
        // Avoid broad words such as "payment", "transfer", and "cashback" on their own:
        // offers and reminders often contain those words alongside a rupee amount.
        private val completedMovementPattern = Regex(
            """\b(?:debited|credited|paid|sent|received|transferred|withdrawn|deducted|deposited|spent|refunded|refund|reversed|reversal)\b|\b(?:purchase|transaction)\s+(?:of|for|successful|succeeded|completed)\b""",
            RegexOption.IGNORE_CASE
        )
        private val failurePattern = Regex(
            """\b(?:failed|failure|unsuccessful|declined|cancelled|canceled|pending|processing)\b""",
            RegexOption.IGNORE_CASE
        )
        private val excludedMessage = Regex(
            """\b(?:otp|one[- ]time password|statement|minimum due|available balance|balance is|will expire|expires on)\b""",
            RegexOption.IGNORE_CASE
        )
    }
}
