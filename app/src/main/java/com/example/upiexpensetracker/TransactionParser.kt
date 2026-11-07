package com.example.upiexpensetracker

/** Parsed fields are safe to persist. rawText is transient input for on-device categorization only. */
data class ParsedTransaction(
    val amount: Double?,
    val merchant: String?,
    val transactionType: String?,
    val paymentMethod: String?,
    val rawText: String
)

class TransactionParser {

    fun parse(title: String?, text: String?): ParsedTransaction {
        val content = listOfNotNull(title, text)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .joinToString(" ")
        val normalized = content
            .lowercase()
            .replace('\u00a0', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

        return ParsedTransaction(
            amount = extractAmount(normalized),
            merchant = extractMerchant(normalized),
            transactionType = extractTransactionType(normalized),
            paymentMethod = extractPaymentMethod(normalized),
            rawText = content
        )
    }

    private fun extractAmount(text: String): Double? {
        val currencyAmount = Regex(
            """(?:₹|rs\.?|inr)\s*([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)|([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)\s*/-""",
            RegexOption.IGNORE_CASE
        ).find(text)
        val value = currencyAmount?.groupValues?.drop(1)?.firstOrNull(String::isNotEmpty)
            ?: Regex(
                """\b(?:debited|credited|paid|sent|received|transferred|withdrawn|deducted|deposited)\s+(?:by|with|of|for)\s*(?:₹|rs\.?|inr)?\s*([0-9]+(?:,[0-9]{2,3})*(?:\.[0-9]{1,2})?)""",
                RegexOption.IGNORE_CASE
            ).find(text)?.groupValues?.get(1)

        return value?.replace(",", "")?.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    private fun extractTransactionType(text: String): String? {
        // Refund wording must win over "credited" because refunds commonly use both.
        if (Regex("""\b(?:refund(?:ed)?|reversed|reversal|chargeback)\b""")
                .containsMatchIn(text)) return "REFUND"

        val debit = Regex(
            """\b(?:debited|debit|paid|payment|sent|transferred|transfer|withdrawn|deducted|purchase|spent)\b"""
        ).containsMatchIn(text)
        val credit = Regex(
            """\b(?:credited|credit|received|deposited|added|cashback)\b"""
        ).containsMatchIn(text)

        return when {
            debit && !credit -> "DEBIT"
            credit && !debit -> "CREDIT"
            debit && credit -> if (Regex("""\b(?:paid|sent|debited|withdrawn|deducted)\b""")
                    .containsMatchIn(text)) "DEBIT" else "CREDIT"
            else -> null
        }
    }

    private fun extractPaymentMethod(text: String): String? {
        val methods = listOf("upi", "neft", "imps", "rtgs", "vpa", "card", "pos", "nach")
        return methods.firstOrNull { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(text) }
            ?.uppercase()
    }

    private fun extractMerchant(text: String): String? {
        val stopWords = setOf(
            "using", "via", "with", "through", "on", "from", "to", "for", "ref", "upi",
            "vpa", "a/c", "account", "acct", "available", "bal", "balance", "txn", "utr",
            "and", "at", "date", "time", "on"
        )
        val patterns = listOf(
            // Explicit labels are the most reliable source of a payee name.
            Regex("""\b(?:merchant|beneficiary|payee)\s*[:#-]?\s*([a-z0-9@._'-]+(?:\s+[a-z0-9@._'-]+){0,3})"""),
            Regex("""\b(?:paid|payment|sent|transferred|purchase|spent)\s+(?:to|at)\s+([a-z0-9@._'-]+(?:\s+[a-z0-9@._'-]+){0,3})"""),
            Regex("""\b(?:to|at)\s+([a-z0-9@._'-]+(?:\s+[a-z0-9@._'-]+){0,3})"""),
            Regex("""\b(?:refund(?:ed)?|received)\s+from\s+([a-z0-9@._'-]+(?:\s+[a-z0-9@._'-]+){0,3})""")
        )

        for (pattern in patterns) {
            val candidate = pattern.find(text)?.groupValues?.get(1) ?: continue
            val words = candidate.split(Regex("\\s+"))
                .takeWhile { it !in stopWords && !it.matches(Regex("(?:xx|x){2,}\\d*")) }
                .filterNot { it in setOf("your", "the", "our", "bank", "account") }
            val cleaned = words.joinToString(" ").trim(' ', '.', ',', ':', ';', '-')
            if (cleaned.isNotBlank()) return cleaned.uppercase()
        }
        return null
    }
}
