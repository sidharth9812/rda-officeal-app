package com.example.util

import android.content.Context
import android.net.Uri

object CloudinaryUploader {
    private val service = CloudinaryService.instance

    suspend fun uploadImage(
        context: Context,
        imageUri: Uri,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> {
        return service.uploadImageFromUri(context, imageUri, onProgress)
    }
}

