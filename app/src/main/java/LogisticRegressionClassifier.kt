package com.example.upiexpensetracker

import android.content.Context
import org.json.JSONArray
import kotlin.math.exp

data class CategoryPrediction(
    val category: String,
    val confidence: Double
)

class LogisticRegressionClassifier(
    private val coefficients: Array<DoubleArray>,
    private val intercepts: DoubleArray,
    private val classes: List<String>
) {
    companion object {
        fun fromAssets(context: Context): LogisticRegressionClassifier {
            val coefficientsText = context.assets
                .open("coefficients.json")
                .bufferedReader()
                .use { it.readText() }

            val interceptsText = context.assets
                .open("intercepts.json")
                .bufferedReader()
                .use { it.readText() }

            val classesText = context.assets
                .open("classes.json")
                .bufferedReader()
                .use { it.readText() }

            val coefficientsJson = JSONArray(coefficientsText)
            val coefficients = Array(coefficientsJson.length()) { row ->
                val rowJson = coefficientsJson.getJSONArray(row)

                DoubleArray(rowJson.length()) { column ->
                    rowJson.getDouble(column)
                }
            }

            val interceptsJson = JSONArray(interceptsText)
            val intercepts = DoubleArray(interceptsJson.length()) { index ->
                interceptsJson.getDouble(index)
            }

            val classesJson = JSONArray(classesText)
            val classes = List(classesJson.length()) { index ->
                classesJson.getString(index)
            }

            require(coefficients.size == intercepts.size)
            require(coefficients.size == classes.size)

            return LogisticRegressionClassifier(
                coefficients = coefficients,
                intercepts = intercepts,
                classes = classes
            )
        }
    }

    fun predict(vector: DoubleArray): CategoryPrediction {
        require(vector.size == coefficients[0].size) {
            "Vector size does not match model feature count"
        }

        // Calculate one score for each category.
        val scores = DoubleArray(coefficients.size) { categoryIndex ->
            var score = intercepts[categoryIndex]

            for (featureIndex in vector.indices) {
                score += coefficients[categoryIndex][featureIndex] *
                        vector[featureIndex]
            }

            score
        }

        // Convert scores into probabilities using stable softmax.
        val maxScore = scores.maxOrNull() ?: 0.0
        val expScores = DoubleArray(scores.size) { index ->
            exp(scores[index] - maxScore)
        }

        val total = expScores.sum()
        val probabilities = DoubleArray(expScores.size) { index ->
            expScores[index] / total
        }

        val bestIndex = probabilities.indices.maxByOrNull {
            probabilities[it]
        } ?: 0

        return CategoryPrediction(
            category = classes[bestIndex],
            confidence = probabilities[bestIndex]
        )
    }
}