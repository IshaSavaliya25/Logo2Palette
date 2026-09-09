package com.example.logo2palette.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette
import com.example.logo2palette.model.ColorPalette
import kotlin.math.sqrt

object ColorExtractor {

    /**
     * Generate usable, WCAG-compliant palette variations for Web and Mobile Apps.
     *
     * variation = 0 -> Brand Classic
     * variation = 1 -> Vibrant Modern
     * variation = 2 -> Sleek Dark Focus
     * variation = 3 -> Soft & Minimal
     * variation = 4 -> High Contrast Corporate
     */
    fun generatePalette(
        originalBitmap: Bitmap,
        variation: Int = 0
    ): ColorPalette {

        val softwareBitmap = if (originalBitmap.config == Bitmap.Config.HARDWARE) {
            originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
        } else {
            originalBitmap.copy(Bitmap.Config.ARGB_8888, false)
        }

        val bitmap = resizeBitmap(softwareBitmap, 600)

        // Generate Android Palette swatches
        val palette = Palette.from(bitmap)
            .maximumColorCount(32)
            .resizeBitmapArea(50_000)
            .generate()

        val candidates = extractCandidates(palette)
        val uniqueColors = removeSimilarColors(candidates)

        val colors = if (uniqueColors.isNotEmpty()) {
            uniqueColors
        } else {
            listOf(
                Color.rgb(103, 80, 164),
                Color.rgb(156, 123, 206),
                Color.rgb(255, 183, 77)
            )
        }

        return createVariation(colors, variation)
    }

    private fun extractCandidates(palette: Palette): List<Int> {
        val colors = mutableListOf<Int>()

        palette.vibrantSwatch?.rgb?.let { colors.add(it) }
        palette.darkVibrantSwatch?.rgb?.let { colors.add(it) }
        palette.lightVibrantSwatch?.rgb?.let { colors.add(it) }
        palette.dominantSwatch?.rgb?.let { colors.add(it) }
        palette.mutedSwatch?.rgb?.let { colors.add(it) }
        palette.darkMutedSwatch?.rgb?.let { colors.add(it) }
        palette.lightMutedSwatch?.rgb?.let { colors.add(it) }

        palette.swatches.forEach { swatch ->
            colors.add(swatch.rgb)
        }

        // Filter out extreme pure whites (#FFFFFF) or pure blacks (#000000) from logo canvas background
        return colors.filter { color ->
            val r = Color.red(color)
            val g = Color.green(color)
            val b = Color.blue(color)
            val isPureWhite = r > 250 && g > 250 && b > 250
            val isPureBlack = r < 5 && g < 5 && b < 5
            !isPureWhite && !isPureBlack
        }.ifEmpty { colors }
    }

    private fun removeSimilarColors(colors: List<Int>): List<Int> {
        val result = mutableListOf<Int>()
        for (color in colors) {
            var tooSimilar = false
            for (existing in result) {
                if (colorDistance(color, existing) < 35) {
                    tooSimilar = true
                    break
                }
            }
            if (!tooSimilar) {
                result.add(color)
            }
        }
        return result
    }

    private fun createVariation(
        colors: List<Int>,
        variation: Int
    ): ColorPalette {

        val sortedBySaturation = colors.sortedByDescending {
            val hsv = FloatArray(3)
            Color.colorToHSV(it, hsv)
            hsv[1]
        }

        val sortedByBrightness = colors.sortedByDescending {
            val hsv = FloatArray(3)
            Color.colorToHSV(it, hsv)
            hsv[2]
        }

        val primaryInt: Int
        val secondaryInt: Int
        val accentInt: Int

        when (variation % 5) {
            0 -> { // Brand Classic
                primaryInt = sortedBySaturation.first()
                secondaryInt = findDifferentColor(sortedBySaturation, primaryInt)
                accentInt = findDifferentColor(sortedByBrightness, primaryInt, secondaryInt)
            }
            1 -> { // Vibrant Modern
                primaryInt = sortedBySaturation.first()
                secondaryInt = sortedBySaturation.getOrElse(1) { createShade(primaryInt, 0.80f) }
                accentInt = sortedByBrightness.firstOrNull { it != primaryInt && it != secondaryInt }
                    ?: createShade(primaryInt, 0.60f)
            }
            2 -> { // Sleek Dark Focus
                primaryInt = createDarkerColor(sortedBySaturation.first())
                secondaryInt = createDarkerColor(findDifferentColor(sortedBySaturation, sortedBySaturation.first()))
                accentInt = createDarkerColor(findDifferentColor(sortedByBrightness, sortedBySaturation.first()))
            }
            3 -> { // Soft & Minimal
                primaryInt = createSoftColor(sortedBySaturation.first())
                secondaryInt = createSoftColor(findDifferentColor(sortedBySaturation, sortedBySaturation.first()))
                accentInt = createSoftColor(findDifferentColor(sortedByBrightness, sortedBySaturation.first()))
            }
            else -> { // High Contrast Corporate
                primaryInt = sortedByBrightness.first()
                secondaryInt = findDifferentColor(sortedBySaturation, primaryInt)
                accentInt = findDifferentColor(sortedByBrightness, primaryInt, secondaryInt)
            }
        }

        val bgInt = createBackground(primaryInt)
        val surfaceInt = Color.WHITE
        val surfaceBorderInt = Color.rgb(224, 224, 224)

        // Compute WCAG guaranteed readable text & on-colors
        val onPrimaryInt = ColorUtils.getContrastTextColor(primaryInt)
        val onSecondaryInt = ColorUtils.getContrastTextColor(secondaryInt)
        val onAccentInt = ColorUtils.getContrastTextColor(accentInt)

        val baseTextDark = Color.rgb(28, 27, 31)
        val baseTextSecondaryDark = Color.rgb(111, 107, 118)

        val textPrimaryInt = ColorUtils.ensureContrast(baseTextDark, bgInt, 7.0)
        val textSecondaryInt = ColorUtils.ensureContrast(baseTextSecondaryDark, bgInt, 4.5)

        // Generate derived Dark Mode colors
        val darkBgInt = createDarkBackground(primaryInt)
        val darkSurfaceInt = Color.rgb(30, 30, 36)
        val darkPrimaryInt = createLighterColorForDarkTheme(primaryInt)
        val darkTextPrimaryInt = Color.rgb(245, 245, 245)
        val darkTextSecondaryInt = Color.rgb(176, 176, 176)

        return ColorPalette(
            primary = ColorUtils.colorToHex(primaryInt),
            onPrimary = ColorUtils.colorToHex(onPrimaryInt),
            primaryContainer = ColorUtils.colorToHex(createSoftColor(primaryInt)),
            onPrimaryContainer = ColorUtils.colorToHex(ColorUtils.getContrastTextColor(createSoftColor(primaryInt))),
            secondary = ColorUtils.colorToHex(secondaryInt),
            onSecondary = ColorUtils.colorToHex(onSecondaryInt),
            accent = ColorUtils.colorToHex(accentInt),
            onAccent = ColorUtils.colorToHex(onAccentInt),
            background = ColorUtils.colorToHex(bgInt),
            onBackground = ColorUtils.colorToHex(textPrimaryInt),
            surface = ColorUtils.colorToHex(surfaceInt),
            onSurface = ColorUtils.colorToHex(textPrimaryInt),
            surfaceBorder = ColorUtils.colorToHex(surfaceBorderInt),
            textPrimary = ColorUtils.colorToHex(textPrimaryInt),
            textSecondary = ColorUtils.colorToHex(textSecondaryInt),
            darkPrimary = ColorUtils.colorToHex(darkPrimaryInt),
            darkBackground = ColorUtils.colorToHex(darkBgInt),
            darkSurface = ColorUtils.colorToHex(darkSurfaceInt),
            darkTextPrimary = ColorUtils.colorToHex(darkTextPrimaryInt),
            darkTextSecondary = ColorUtils.colorToHex(darkTextSecondaryInt)
        )
    }

    private fun findDifferentColor(
        colors: List<Int>,
        vararg excluded: Int
    ): Int {
        for (candidate in colors) {
            var valid = true
            for (existing in excluded) {
                if (colorDistance(candidate, existing) < 45) {
                    valid = false
                    break
                }
            }
            if (valid) return candidate
        }
        return createShade(excluded.first(), 0.75f)
    }

    private fun colorDistance(color1: Int, color2: Int): Double {
        val redDiff = Color.red(color1) - Color.red(color2)
        val greenDiff = Color.green(color1) - Color.green(color2)
        val blueDiff = Color.blue(color1) - Color.blue(color2)
        return sqrt((redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff).toDouble())
    }

    private fun createDarkerColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = hsv[1].coerceAtLeast(0.55f)
        hsv[2] = (hsv[2] * 0.65f).coerceIn(0.25f, 0.85f)
        return Color.HSVToColor(hsv)
    }

    private fun createLighterColorForDarkTheme(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * 0.70f).coerceIn(0.20f, 0.65f)
        hsv[2] = (hsv[2] * 1.30f).coerceIn(0.75f, 1.0f)
        return Color.HSVToColor(hsv)
    }

    private fun createSoftColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * 0.45f).coerceIn(0.15f, 0.50f)
        hsv[2] = 0.94f
        return Color.HSVToColor(hsv)
    }

    private fun createShade(color: Int, brightness: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = hsv[1].coerceAtLeast(0.50f)
        hsv[2] = brightness
        return Color.HSVToColor(hsv)
    }

    private fun createBackground(primary: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(primary, hsv)
        hsv[1] = 0.06f
        hsv[2] = 0.98f
        return Color.HSVToColor(hsv)
    }

    private fun createDarkBackground(primary: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(primary, hsv)
        hsv[1] = 0.20f
        hsv[2] = 0.08f
        return Color.HSVToColor(hsv)
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}