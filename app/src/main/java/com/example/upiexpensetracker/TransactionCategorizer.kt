package com.example.upiexpensetracker

data class CategorizedTransaction(
    val transaction: ParsedTransaction,
    val category: String,
    val confidence: Double
)

class TransactionCategorizer(
    private val vectorizer: TfidfVectorizer,
    private val classifier: LogisticRegressionClassifier
) {
    fun categorize(
        transaction: ParsedTransaction
    ): CategorizedTransaction {
        val vector = vectorizer.transform(transaction.rawText)
        val prediction = classifier.predict(vector)

        return CategorizedTransaction(
            transaction = transaction,
            category = prediction.category,
            confidence = prediction.confidence
        )
    }
}