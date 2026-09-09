package com.example.logo2palette.model

import java.io.Serializable

data class ColorPalette(
    val primary: String,
    val onPrimary: String = "#FFFFFF",
    val primaryContainer: String = "#EADDFF",
    val onPrimaryContainer: String = "#21005D",
    val secondary: String,
    val onSecondary: String = "#FFFFFF",
    val accent: String,
    val onAccent: String = "#FFFFFF",
    val background: String,
    val onBackground: String = "#1C1B1F",
    val surface: String,
    val onSurface: String = "#1C1B1F",
    val surfaceBorder: String = "#E0E0E0",
    val textPrimary: String,
    val textSecondary: String,
    
    // Dark mode derived colors
    val darkPrimary: String = primary,
    val darkBackground: String = "#121212",
    val darkSurface: String = "#1E1E1E",
    val darkTextPrimary: String = "#F5F5F5",
    val darkTextSecondary: String = "#B0B0B0"
) : Serializable