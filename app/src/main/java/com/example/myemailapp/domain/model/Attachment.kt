package com.example.myemailapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Attachment(
    val id: String,
    val dataBase64: String,
    val type: String,
    val name: String
) : Parcelable
