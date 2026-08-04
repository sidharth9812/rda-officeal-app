package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * CloudinaryService handles unsigned image uploads to Cloudinary with
 * bitmap resizing and JPEG compression for efficient storage usage.
 */
class CloudinaryService(
    val cloudName: String = "lctewf11",
    val uploadPreset: String = "rda_app_upload",
    val apiKey: String = "459126254263285",
    val apiSecret: String = "JCnI6qB7nJsfH8p7Mh4ROfGyFPQ"
) {
    private val uploadUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun sha1(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-1")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Resizes the given bitmap to fit within [maxDimension] x [maxDimension] while preserving aspect ratio,
     * and compresses it to JPEG format with the specified [quality] percentage.
     */
    fun compressAndResizeBitmap(
        bitmap: Bitmap,
        maxDimension: Int = 1280,
        quality: Int = 85
    ): ByteArray {
        val width = bitmap.width
        val height = bitmap.height

        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val newW = if (ratio > 1) maxDimension else (maxDimension * ratio).toInt()
            val newH = if (ratio > 1) (maxDimension / ratio).toInt() else maxDimension
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Uploads a raw bitmap to Cloudinary after compressing/resizing it.
     * Tries unsigned upload first; if rejected, automatically falls back to signed API upload.
     */
    suspend fun uploadBitmap(
        bitmap: Bitmap,
        filename: String = "rda_image_${System.currentTimeMillis()}.jpg",
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(0.2f)

            val compressedBytes = compressAndResizeBitmap(bitmap, maxDimension = 1280, quality = 85)

            onProgress?.invoke(0.5f)

            // 1. Try Unsigned Upload First
            val unsignedRequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart(
                    "file",
                    filename,
                    compressedBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val unsignedRequest = Request.Builder()
                .url(uploadUrl)
                .post(unsignedRequestBody)
                .build()

            val response = client.newCall(unsignedRequest).execute()
            val responseBodyStr = response.body?.string()

            if (response.isSuccessful && !responseBodyStr.isNullOrBlank()) {
                val json = JSONObject(responseBodyStr)
                val secureUrl = json.optString("secure_url")
                if (secureUrl.isNotBlank()) {
                    onProgress?.invoke(1.0f)
                    Log.d("CloudinaryService", "Unsigned upload successful: $secureUrl")
                    return@withContext Result.success(secureUrl)
                }
            }

            // 2. Fallback to Signed Upload using API Key & Secret
            Log.w("CloudinaryService", "Unsigned upload failed ($responseBodyStr), trying signed upload fallback...")
            onProgress?.invoke(0.7f)

            val timestamp = (System.currentTimeMillis() / 1000).toString()
            val toSign = "timestamp=$timestamp$apiSecret"
            val signature = sha1(toSign)

            val signedRequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_key", apiKey)
                .addFormDataPart("timestamp", timestamp)
                .addFormDataPart("signature", signature)
                .addFormDataPart(
                    "file",
                    filename,
                    compressedBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val signedRequest = Request.Builder()
                .url(uploadUrl)
                .post(signedRequestBody)
                .build()

            val signedResponse = client.newCall(signedRequest).execute()
            onProgress?.invoke(0.9f)

            val signedResponseBodyStr = signedResponse.body?.string()
            if (signedResponse.isSuccessful && !signedResponseBodyStr.isNullOrBlank()) {
                val json = JSONObject(signedResponseBodyStr)
                val secureUrl = json.optString("secure_url")
                if (secureUrl.isNotBlank()) {
                    onProgress?.invoke(1.0f)
                    Log.d("CloudinaryService", "Signed upload successful: $secureUrl")
                    return@withContext Result.success(secureUrl)
                }
            }

            val serverMsg = if (!signedResponseBodyStr.isNullOrBlank()) {
                try {
                    val errObj = JSONObject(signedResponseBodyStr).optJSONObject("error")
                    errObj?.optString("message") ?: signedResponseBodyStr
                } catch (e: Exception) {
                    signedResponseBodyStr
                }
            } else {
                "HTTP ${signedResponse.code}"
            }

            Log.e("CloudinaryService", "Both unsigned & signed upload failed: $serverMsg")
            Result.failure(Exception("Cloudinary HTTP ${signedResponse.code}: $serverMsg"))
        } catch (e: Exception) {
            Log.e("CloudinaryService", "Upload exception", e)
            Result.failure(Exception(e.localizedMessage ?: "Network/Upload error occurred."))
        }
    }

    /**
     * Reads a Uri from [Context], decodes the bitmap, resizes/compresses it, and uploads it to Cloudinary.
     */
    suspend fun uploadImageFromUri(
        context: Context,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            onProgress?.invoke(0.1f)

            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                return@withContext Result.failure(Exception("Could not read image data from selected file."))
            }

            uploadBitmap(bitmap, onProgress = onProgress)
        } catch (e: Exception) {
            Log.e("CloudinaryService", "Uri decoding/upload exception", e)
            Result.failure(Exception(e.localizedMessage ?: "File read or upload error occurred."))
        }
    }

    companion object {
        val instance by lazy { CloudinaryService() }
    }
}
