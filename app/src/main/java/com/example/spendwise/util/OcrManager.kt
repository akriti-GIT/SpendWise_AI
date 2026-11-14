package com.example.spendwise.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max

object OcrManager {

    private const val MAX_DIMENSION = 1920

    /**
     * Reads and processes an image locally using Google ML Kit Text Recognition with Devanagari & Rupee symbol support.
     * Scales down high-resolution images to conserve memory and avoid OOM.
     */
    suspend fun recognizeTextFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeSampledBitmapFromUri(context, uri)
        
        try {
            val recognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
            val inputImage = if (bitmap != null) {
                InputImage.fromBitmap(bitmap, 0)
            } else {
                InputImage.fromFilePath(context, uri)
            }
            suspendCancellableCoroutine { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        continuation.resume(visionText.text)
                    }
                    .addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
            }
        } finally {
            // Immediately recycle bitmap to free memory
            bitmap?.recycle()
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        val contentResolver = context.contentResolver

        // 1. First decode with inJustDecodeBounds=true to check dimensions
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        var inputStream: InputStream? = contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        val height = options.outHeight
        val width = options.outWidth
        if (width <= 0 || height <= 0) return null

        // 2. Calculate inSampleSize
        var inSampleSize = 1
        val maxDim = max(width, height)
        if (maxDim > MAX_DIMENSION) {
            inSampleSize = (maxDim / MAX_DIMENSION)
        }

        // 3. Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        options.inSampleSize = inSampleSize
        inputStream = contentResolver.openInputStream(uri)
        val decodedBitmap = BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        if (decodedBitmap == null) return null

        // 4. Handle EXIF rotation if needed
        return try {
            val exifStream = contentResolver.openInputStream(uri)
            val orientation = exifStream?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            rotateBitmapIfNeeded(decodedBitmap, orientation)
        } catch (_: Exception) {
            decodedBitmap
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> return bitmap
        }

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) {
            bitmap.recycle()
        }
        return rotated
    }
}
