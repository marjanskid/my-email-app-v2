package com.example.myemailapp.presentation.ui.emails.create

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
