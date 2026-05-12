package com.example.myemailapp.domain.model.db

import android.os.Parcel
import android.os.Parcelable
import com.example.myemailapp.domain.model.Attachment
import com.example.myemailapp.domain.model.Tag
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import java.time.Instant

object InstantParceler : Parceler<Instant> {
    override fun create(parcel: Parcel): Instant {
        return Instant.ofEpochMilli(parcel.readLong())
    }
    override fun Instant.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(toEpochMilli())
    }
}

@Parcelize
@TypeParceler<Instant, InstantParceler>
data class Email(
    val id: String,
    val from: String,
    val to: String,
    val cc: String,
    val bcc: String,
    val subject: String,
    val content: String,
    val attachments: List<Attachment>,
    val dateTime: Instant,
    val status: String,
    val folderId: String? = null,
    val tags: List<Tag> = emptyList(),
    val isRead: Boolean = false,
    val isStarred: Boolean = false
) : Parcelable {
    companion object {
        fun toEmail(
            to: String = "",
            cc: String = "",
            bcc: String = "",
            subject: String = "",
            content: String = "",
            attachments: List<Attachment> = emptyList(),
            folderId: String? = null
        ): Email = Email(
            id = "",
            from = "",
            to = to,
            cc = cc,
            bcc = bcc,
            subject = subject,
            content = content,
            attachments = attachments,
            dateTime = Instant.now(),
            status = "",
            folderId = folderId,
            tags = emptyList(),
            isRead = false,
            isStarred = false
        )
    }
}
