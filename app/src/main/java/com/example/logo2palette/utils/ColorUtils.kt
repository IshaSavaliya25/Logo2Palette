package com.example.logo2palette.utils

import android.graphics.Color
import com.example.logo2palette.model.ColorPalette
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ColorUtils {

    /**
     * Calculate relative luminance according to WCAG 2.1 specifications.
     */
    fun calculateLuminance(color: Int): Double {
        val red = Color.red(color) / 255.0
        val green = Color.green(color) / 255.0
        val blue = Color.blue(color) / 255.0

        val rLinear = if (red <= 0.03928) red / 12.92 else ((red + 0.055) / 1.055).pow(2.4)
        val gLinear = if (green <= 0.03928) green / 12.92 else ((green + 0.055) / 1.055).pow(2.4)
        val bLinear = if (blue <= 0.03928) blue / 12.92 else ((blue + 0.055) / 1.055).pow(2.4)

        return (0.2126 * rLinear) + (0.7152 * gLinear) + (0.0722 * bLinear)
    }

    /**
     * Calculate WCAG 2.1 contrast ratio between two colors (range 1.0 to 21.0).
     */
    fun calculateContrastRatio(color1: Int, color2: Int): Double {
        val lum1 = calculateLuminance(color1)
        val lum2 = calculateLuminance(color2)
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /**
     * Get guaranteed high-contrast text color (Dark Charcoal or White) for a background.
     */
    fun getContrastTextColor(backgroundColor: Int): Int {
        val darkText = Color.rgb(18, 18, 18)
        val lightText = Color.WHITE

        val darkRatio = calculateContrastRatio(darkText, backgroundColor)
        val lightRatio = calculateContrastRatio(lightText, backgroundColor)

        return if (lightRatio >= darkRatio) lightText else darkText
    }

    /**
     * Ensure a text color has at least [minRatio] contrast against [backgroundColor].
     * Darkens or lightens the text color in HSV space while retaining its hue.
     */
    fun ensureContrast(textColor: Int, backgroundColor: Int, minRatio: Double = 4.5): Int {
        var ratio = calculateContrastRatio(textColor, backgroundColor)
        if (ratio >= minRatio) return textColor

        val bgLum = calculateLuminance(backgroundColor)
        val hsv = FloatArray(3)
        Color.colorToHSV(textColor, hsv)

        // If background is light, darken text; if background is dark, lighten text
        val shouldDarken = bgLum > 0.4

        var currentAdjusted = textColor
        var bestColor = currentAdjusted
        var maxRatioAchieved = ratio

        for (step in 1..20) {
            if (shouldDarken) {
                hsv[2] = (hsv[2] * 0.90f).coerceAtLeast(0.05f)
                hsv[1] = (hsv[1] * 1.05f).coerceAtMost(1.0f) // Keep or slightly boost saturation for readability
            } else {
                hsv[2] = (hsv[2] + (1.0f - hsv[2]) * 0.15f).coerceAtMost(1.0f)
                hsv[1] = (hsv[1] * 0.90f).coerceAtLeast(0.10f) // Desaturate slightly if very bright
            }

            currentAdjusted = Color.HSVToColor(hsv)
            val currentRatio = calculateContrastRatio(currentAdjusted, backgroundColor)

            if (currentRatio > maxRatioAchieved) {
                maxRatioAchieved = currentRatio
                bestColor = currentAdjusted
            }

            if (currentRatio >= minRatio) {
                return currentAdjusted
            }
        }

        // If exact minRatio isn't reached, fallback to absolute contrast text (White / Black)
        return if (maxRatioAchieved < 3.0) getContrastTextColor(backgroundColor) else bestColor
    }

    /**
     * Format contrast ratio into human-readable WCAG badge text.
     */
    fun getWcagBadge(contrastRatio: Double): String {
        val formatted = String.format("%.1f:1", contrastRatio)
        return when {
            contrastRatio >= 7.0 -> "$formatted (AAA)"
            contrastRatio >= 4.5 -> "$formatted (AA)"
            contrastRatio >= 3.0 -> "$formatted (AA Large)"
            else -> "$formatted (Fail)"
        }
    }

    /**
     * Convert Color Int to 6-character uppercase Hex String (#RRGGBB).
     */
    fun colorToHex(color: Int): String {
        return String.format(
            "#%02X%02X%02X",
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    // =========================================================
    // CODE EXPORTERS FOR WEB & APP DEVELOPERS
    // =========================================================

    fun toCssVariables(palette: ColorPalette): String {
        return """
        /* Light Theme */
        :root {
          --color-primary: ${palette.primary};
          --color-on-primary: ${palette.onPrimary};
          --color-primary-container: ${palette.primaryContainer};
          --color-on-primary-container: ${palette.onPrimaryContainer};
          --color-secondary: ${palette.secondary};
          --color-on-secondary: ${palette.onSecondary};
          --color-accent: ${palette.accent};
          --color-on-accent: ${palette.onAccent};
          --color-background: ${palette.background};
          --color-surface: ${palette.surface};
          --color-surface-border: ${palette.surfaceBorder};
          --color-text-primary: ${palette.textPrimary};
          --color-text-secondary: ${palette.textSecondary};
        }

        /* Dark Theme */
        @media (prefers-color-scheme: dark) {
          :root {
            --color-primary: ${palette.darkPrimary};
            --color-background: ${palette.darkBackground};
            --color-surface: ${palette.darkSurface};
            --color-text-primary: ${palette.darkTextPrimary};
            --color-text-secondary: ${palette.darkTextSecondary};
          }
        }
        """.trimIndent()
    }

    fun toTailwindConfig(palette: ColorPalette): String {
        return """
        // tailwind.config.js
        module.exports = {
          theme: {
            extend: {
              colors: {
                brand: {
                  primary: '${palette.primary}',
                  onPrimary: '${palette.onPrimary}',
                  secondary: '${palette.secondary}',
                  onSecondary: '${palette.onSecondary}',
                  accent: '${palette.accent}',
                  onAccent: '${palette.onAccent}',
                  bg: '${palette.background}',
                  surface: '${palette.surface}',
                  border: '${palette.surfaceBorder}',
                  textPrimary: '${palette.textPrimary}',
                  textSecondary: '${palette.textSecondary}',
                }
              }
            }
          }
        }
        """.trimIndent()
    }

    fun toAndroidXml(palette: ColorPalette): String {
        return """
        <!-- res/values/colors.xml -->
        <resources>
            <color name="primary">${palette.primary}</color>
            <color name="on_primary">${palette.onPrimary}</color>
            <color name="secondary">${palette.secondary}</color>
            <color name="on_secondary">${palette.onSecondary}</color>
            <color name="accent">${palette.accent}</color>
            <color name="on_accent">${palette.onAccent}</color>
            <color name="background">${palette.background}</color>
            <color name="surface">${palette.surface}</color>
            <color name="surface_border">${palette.surfaceBorder}</color>
            <color name="text_primary">${palette.textPrimary}</color>
            <color name="text_secondary">${palette.textSecondary}</color>
            
            <!-- Dark Mode -->
            <color name="dark_background">${palette.darkBackground}</color>
            <color name="dark_surface">${palette.darkSurface}</color>
        </resources>
        """.trimIndent()
    }

    fun toJetpackCompose(palette: ColorPalette): String {
        val p = palette.primary.replace("#", "0xFF")
        val onP = palette.onPrimary.replace("#", "0xFF")
        val s = palette.secondary.replace("#", "0xFF")
        val a = palette.accent.replace("#", "0xFF")
        val bg = palette.background.replace("#", "0xFF")
        val surf = palette.surface.replace("#", "0xFF")
        val text1 = palette.textPrimary.replace("#", "0xFF")

        return """
        // Color.kt (Jetpack Compose)
        import androidx.compose.ui.graphics.Color

        val BrandPrimary = Color($p)
        val BrandOnPrimary = Color($onP)
        val BrandSecondary = Color($s)
        val BrandAccent = Color($a)
        val BrandBackground = Color($bg)
        val BrandSurface = Color($surf)
        val BrandTextPrimary = Color($text1)
        """.trimIndent()
    }

    fun toFlutter(palette: ColorPalette): String {
        val p = palette.primary.replace("#", "0xFF")
        val s = palette.secondary.replace("#", "0xFF")
        val a = palette.accent.replace("#", "0xFF")
        val bg = palette.background.replace("#", "0xFF")
        val surf = palette.surface.replace("#", "0xFF")

        return """
        // AppColors.dart (Flutter)
        import 'package:flutter/material.dart';

        class AppColors {
          static const primary = Color($p);
          static const secondary = Color($s);
          static const accent = Color($a);
          static const background = Color($bg);
          static const surface = Color($surf);
        }
        """.trimIndent()
    }

    fun toJson(palette: ColorPalette): String {
        return """
        {
          "primary": "${palette.primary}",
          "onPrimary": "${palette.onPrimary}",
          "secondary": "${palette.secondary}",
          "onSecondary": "${palette.onSecondary}",
          "accent": "${palette.accent}",
          "onAccent": "${palette.onAccent}",
          "background": "${palette.background}",
          "surface": "${palette.surface}",
          "surfaceBorder": "${palette.surfaceBorder}",
          "textPrimary": "${palette.textPrimary}",
          "textSecondary": "${palette.textSecondary}",
          "darkMode": {
            "primary": "${palette.darkPrimary}",
            "background": "${palette.darkBackground}",
            "surface": "${palette.darkSurface}",
            "textPrimary": "${palette.darkTextPrimary}"
          }
        }
        """.trimIndent()
    }
}
