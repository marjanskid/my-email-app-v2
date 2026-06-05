package com.example.myemailapp.presentation.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.IOException

fun decodeBase64ToBitmap(base64: String, maxWidth: Int = 100, maxHeight: Int = 100): Bitmap? {
    return try {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)

        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
        options.inJustDecodeBounds = false

        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size, options)
    } catch (_: Exception) {
        null
    }
}

private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun encodeUriToBase64(context: Context, uri: Uri): Result<String> {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return Result.failure(IOException("Could not open image"))
        val bytes = inputStream.readBytes()
        inputStream.close()
        Result.success(Base64.encodeToString(bytes, Base64.DEFAULT))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
