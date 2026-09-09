package com.example.logo2palette.model

import java.io.Serializable

data class SavedPalette(
    val id: String,
    val userId: String,
    val title: String,
    val palette: ColorPalette,
    val logoPath: String? = null,
    val logoBase64: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
