package com.example.myemailapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Tag(
    val id: String,
    val name: String,
    val color: String  // Hex color e.g., "#FF5722"
) : Parcelable
