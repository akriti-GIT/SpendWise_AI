package com.example.spendwise.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.example.spendwise.model.ParsedTransaction
import com.example.spendwise.model.PaymentMethod
import com.example.spendwise.model.TransactionType
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max

object GeminiVisionManager {

    private const val TAG = "GeminiVisionManager"
    private const val MAX_DIMENSION = 1600
    private val gson = Gson()

    private const val PROMPT = """
Analyze this receipt or payment screenshot and extract financial transactions.
The image could be a UPI payment (Google Pay, PhonePe, Paytm, CRED, BHIM), food order (Swiggy, Zomato), grocery/e-commerce (Blinkit, Zepto, Amazon, Flipkart), bus/metro/train/flight ticket (BMTC, DTC, BEST, Chalo, IRCTC), cab/auto (Uber, Ola, Rapido), paper invoice, or bank debit alert.

CRITICAL RULES:
1. "amount": The actual final transaction or fare amount paid (positive number, e.g. 6.0, 100.0). NEVER confuse transit route numbers (e.g. Route 26), timestamps (e.g. 14:26), dates, or reference IDs (e.g. 26j29E1117866mdR) with the amount.
2. "description": Merchant, payee, or store name (e.g. "BMTC", "Swiggy", "PRERANA UTTAM CHAVAN", "Campus Cafeteria").
3. "category": Must be one of ["Food", "Transport", "Education", "Shopping", "Entertainment", "Health", "Bills", "Other"].
4. "type": "EXPENSE" (money sent, spent, paid) or "INCOME" (money received, cashback).
5. "paymentMethod": One of ["UPI", "CASH", "CARD", "NET_BANKING"].
6. "referenceNumber": UPI RRN, Transaction ID, or Order ID if present, else null.
7. "date": Epoch timestamp in milliseconds if date is shown, else null.

Return a JSON array of objects strictly matching this structure:
[
  {
    "amount": 6.0,
    "description": "BMTC",
    "category": "Transport",
    "type": "EXPENSE",
    "paymentMethod": "UPI",
    "referenceNumber": "26j29E1117866mdR",
    "date": 1729000000000
  }
]
Return JSON only with no markdown or formatting wrapper.
"""

    suspend fun analyzeReceipt(
        context: Context,
        uri: Uri
    ): List<ParsedTransaction> = withContext(Dispatchers.IO) {
        val isCloudEnabled = AiConfigManager.isCloudAiEnabled(context)
        val apiKey = AiConfigManager.getGeminiApiKey(context)

        // 1. Try Cloud Vision AI if enabled and key is available
        if (isCloudEnabled && apiKey.isNotBlank()) {
            try {
                val cloudResults = callGeminiVision(context, uri, apiKey)
                if (cloudResults.isNotEmpty()) {
                    Log.d(TAG, "Gemini Cloud Vision successfully detected ${cloudResults.size} transaction(s)")
                    return@withContext cloudResults
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini Cloud Vision error: ${e.localizedMessage}. Falling back to on-device OCR.", e)
            }
        }

        // 2. Offline Fallback to On-Device ML Kit OCR
        if (AiConfigManager.isOfflineFallbackEnabled(context)) {
            try {
                Log.d(TAG, "Running On-Device ML Kit OCR fallback")
                val rawText = OcrManager.recognizeTextFromUri(context, uri)
                if (rawText.isNotBlank()) {
                    return@withContext TransactionParserEngine.parseOcrText(rawText)
                }
            } catch (e: Exception) {
                Log.e(TAG, "On-device OCR fallback failed: ${e.localizedMessage}", e)
            }
        }

        emptyList()
    }

    private suspend fun callGeminiVision(
        context: Context,
        uri: Uri,
        apiKey: String
    ): List<ParsedTransaction> {
        val bitmap = decodeSampledBitmap(context, uri) ?: return emptyList()

        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    temperature = 0.1f
                }
            )

            val inputContent = content {
                image(bitmap)
                text(PROMPT)
            }

            val response = generativeModel.generateContent(inputContent)
            val jsonText = response.text?.trim() ?: return emptyList()

            // Strip markdown block if model wrapped it
            val cleanJson = jsonText
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val type = object : TypeToken<List<GeminiTransactionItem>>() {}.type
            val items: List<GeminiTransactionItem> = gson.fromJson(cleanJson, type) ?: emptyList()

            return items.filter { it.amount != null && it.amount > 0.0 }.map { item ->
                ParsedTransaction(
                    amount = item.amount ?: 0.0,
                    description = item.description?.trim().orEmpty().ifBlank { item.category ?: "Scanned Expense" },
                    category = item.category ?: "Other",
                    date = item.date ?: System.currentTimeMillis(),
                    type = when (item.type?.uppercase()) {
                        "INCOME" -> TransactionType.INCOME
                        else -> TransactionType.EXPENSE
                    },
                    paymentMethod = when (item.paymentMethod?.uppercase()) {
                        "CASH" -> PaymentMethod.CASH
                        "CARD" -> PaymentMethod.CARD
                        "UPI" -> PaymentMethod.UPI
                        else -> PaymentMethod.OTHER
                    },
                    referenceNumber = item.referenceNumber?.trim(),
                    isSelected = true
                )
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        var input: InputStream? = resolver.openInputStream(uri)
        BitmapFactory.decodeStream(input, null, options)
        input?.close()

        val w = options.outWidth
        val h = options.outHeight
        if (w <= 0 || h <= 0) return null

        var inSampleSize = 1
        val maxDim = max(w, h)
        if (maxDim > MAX_DIMENSION) {
            inSampleSize = maxDim / MAX_DIMENSION
        }

        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        input = resolver.openInputStream(uri)
        val decoded = BitmapFactory.decodeStream(input, null, options)
        input?.close()

        if (decoded == null) return null

        return try {
            val exifStream = resolver.openInputStream(uri)
            val orientation = exifStream?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return decoded
            }
            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            if (rotated != decoded) decoded.recycle()
            rotated
        } catch (_: Exception) {
            decoded
        }
    }

    private data class GeminiTransactionItem(
        val amount: Double?,
        val description: String?,
        val category: String?,
        val type: String?,
        val paymentMethod: String?,
        val referenceNumber: String?,
        val date: Long?
    )
}
