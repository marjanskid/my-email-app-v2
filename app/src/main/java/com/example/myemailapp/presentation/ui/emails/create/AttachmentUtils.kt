package com.example.myemailapp.presentation.ui.emails.create

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Locale.ENGLISH

fun decodeBase64ToBitmap(
    base64: String,
    maxWidth: Int = 100,
    maxHeight: Int = 100
): Bitmap? {
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

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight &&
            (halfWidth / inSampleSize) >= reqWidth
        ) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun isImageMimeType(mimeType: String): Boolean {
    return mimeType.startsWith("image/", ignoreCase = true)
}

fun getFileTypeIcon(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("image/") -> Icons.Filled.Image
        mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
        mimeType.startsWith("application/vnd.openxmlformats-officedocument.wordprocessing") ||
                mimeType == "application/msword" -> Icons.Filled.Description
        mimeType.startsWith("application/vnd.openxmlformats-officedocument.spreadsheet") ||
                mimeType == "application/vnd.ms-excel" -> Icons.Filled.TableChart
        mimeType == "application/zip" ||
                mimeType == "application/x-rar-compressed" ||
                mimeType == "application/x-7z-compressed" -> Icons.Filled.FolderZip
        mimeType.startsWith("text/") -> Icons.AutoMirrored.Filled.TextSnippet
        else -> Icons.Filled.AttachFile
    }
}

fun formatFileSize(sizeInBytes: Long): String {
    return when {
        sizeInBytes < 1024 -> "$sizeInBytes B"
        sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
        else -> String.format(locale = ENGLISH, "%.2f MB", sizeInBytes / (1024.0 * 1024.0))
    }
}

fun calculateBase64Size(base64: String): Long {
    return ((base64.length * 3) / 4).toLong()
}

fun getFileTypeLabel(mimeType: String): String {
    return when {
        mimeType.startsWith("image/") -> mimeType.substringAfter("image/").uppercase()
        mimeType == "application/pdf" -> "PDF"
        mimeType.contains("word") -> "Word"
        mimeType.contains("excel") || mimeType.contains("spreadsheet") -> "Excel"
        mimeType == "application/zip" -> "ZIP"
        mimeType.startsWith("text/") -> "Text"
        else -> mimeType.substringAfter("/").take(10).uppercase()
    }
}
