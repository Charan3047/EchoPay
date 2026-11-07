
package com.example.upiexpensetracker

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.sqrt

class TfidfVectorizer(
    private val vocabulary: Map<String, Int>,
    private val idf: DoubleArray
) {
    companion object {
        fun fromAssets(context: android.content.Context): TfidfVectorizer {
            val vocabularyText = context.assets
                .open("vocabulary.json")
                .bufferedReader()
                .use { it.readText() }

            val idfText = context.assets
                .open("idf.json")
                .bufferedReader()
                .use { it.readText() }

            val vocabularyJson = JSONObject(vocabularyText)
            val vocabulary = mutableMapOf<String, Int>()

            val keys = vocabularyJson.keys()
            while (keys.hasNext()) {
                val word = keys.next()
                vocabulary[word] = vocabularyJson.getInt(word)
            }

            val idfJson = JSONArray(idfText)
            val idf = DoubleArray(idfJson.length()) { index ->
                idfJson.getDouble(index)
            }

            require(vocabulary.size == idf.size) {
                "Vocabulary and IDF sizes do not match"
            }

            return TfidfVectorizer(vocabulary, idf)
        }
    }

    fun transform(text: String): DoubleArray {
        val normalizedText = text.lowercase()

        // Match scikit-learn's default token pattern: words with 2+ characters.
        val tokens = Regex("""(?u)\b\w\w+\b""")
            .findAll(normalizedText)
            .map { it.value }
            .toList()

        // Generate unigrams and bigrams.
        val terms = mutableListOf<String>()
        terms.addAll(tokens)

        for (i in 0 until tokens.size - 1) {
            terms.add("${tokens[i]} ${tokens[i + 1]}")
        }

        // Count known features.
        val counts = mutableMapOf<Int, Int>()

        for (term in terms) {
            val index = vocabulary[term] ?: continue
            counts[index] = (counts[index] ?: 0) + 1
        }

        // Apply TF-IDF.
        val vector = DoubleArray(idf.size)

        for ((index, count) in counts) {
            vector[index] = count.toDouble() * idf[index]
        }

        // L2 normalization.
        var squaredSum = 0.0
        for (value in vector) {
            squaredSum += value * value
        }

        val norm = sqrt(squaredSum)

        if (norm > 0.0) {
            for (i in vector.indices) {
                vector[i] /= norm
            }
        }

        return vector
    }
}
