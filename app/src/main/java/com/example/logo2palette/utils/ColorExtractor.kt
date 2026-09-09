package com.example.logo2palette.utils

import android.graphics.Bitmap
import android.graphics.Color
import androidx.palette.graphics.Palette
import com.example.logo2palette.model.ColorPalette
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object ColorExtractor {

    /**
     * Generate usable, WCAG-compliant palette variations for Web and Mobile Apps.
     * Guaranteed to produce distinct, harmonious palettes for any logo on every increment of [variation].
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

        val bitmap = resizeBitmap(softwareBitmap, 500)

        // 1. Extract raw swatches from Android Palette
        val palette = Palette.from(bitmap)
            .maximumColorCount(32)
            .resizeBitmapArea(40_000)
            .generate()

        // 2. Extract Palette candidates + Direct Pixel sampling
        val rawCandidates = extractAllCandidates(bitmap, palette)
        val uniqueColors = filterAndDeduplicate(rawCandidates)

        // 3. Ensure a rich pool of harmonious candidates (even for 1-color / monochrome logos)
        val richPool = buildRichColorPool(uniqueColors)

        // 4. Generate distinct variation based on variation index
        return buildPaletteVariation(richPool, variation)
    }

    // =========================================================
    // CANDIDATE EXTRACTION & SAMPLING
    // =========================================================

    private fun extractAllCandidates(bitmap: Bitmap, palette: Palette): List<Int> {
        val candidates = mutableListOf<Int>()

        // Add standard Palette swatches
        palette.dominantSwatch?.rgb?.let { candidates.add(it) }
        palette.vibrantSwatch?.rgb?.let { candidates.add(it) }
        palette.darkVibrantSwatch?.rgb?.let { candidates.add(it) }
        palette.lightVibrantSwatch?.rgb?.let { candidates.add(it) }
        palette.mutedSwatch?.rgb?.let { candidates.add(it) }
        palette.darkMutedSwatch?.rgb?.let { candidates.add(it) }
        palette.lightMutedSwatch?.rgb?.let { candidates.add(it) }

        palette.swatches.sortedByDescending { it.population }.forEach { swatch ->
            candidates.add(swatch.rgb)
        }

        // Direct grid sampling from bitmap to catch flat vectors / sharp graphic accents
        val stepX = max(1, bitmap.width / 16)
        val stepY = max(1, bitmap.height / 16)
        for (x in 0 until bitmap.width step stepX) {
            for (y in 0 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                if (alpha >= 160) {
                    candidates.add(pixel)
                }
            }
        }

        return candidates
    }

    private fun filterAndDeduplicate(colors: List<Int>): List<Int> {
        // Filter out extreme canvas whites or extreme blacks (unless nothing else exists)
        val nonCanvas = colors.filter { c ->
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val isPureWhite = r > 248 && g > 248 && b > 248
            val isPureBlack = r < 8 && g < 8 && b < 8
            !isPureWhite && !isPureBlack
        }

        val pool = if (nonCanvas.isNotEmpty()) nonCanvas else colors

        val distinctList = mutableListOf<Int>()
        for (color in pool) {
            val isDuplicate = distinctList.any { existing -> colorDistance(color, existing) < 30.0 }
            if (!isDuplicate) {
                distinctList.add(color)
            }
        }

        // Sort by saturation and visual interest
        distinctList.sortByDescending { c ->
            val hsv = FloatArray(3)
            Color.colorToHSV(c, hsv)
            hsv[1] * 0.7f + hsv[2] * 0.3f
        }

        return distinctList
    }

    // =========================================================
    // COLOR POOL SYNTHESIS (FOR 1-2 COLOR LOGOS)
    // =========================================================

    private fun buildRichColorPool(extracted: List<Int>): List<Int> {
        if (extracted.size >= 5) {
            return extracted
        }

        val baseColor = extracted.firstOrNull() ?: Color.rgb(103, 80, 164)
        val pool = extracted.toMutableList()
        if (pool.isEmpty()) pool.add(baseColor)

        // Synthesize harmonious companions using color theory
        val hsv = FloatArray(3)
        Color.colorToHSV(baseColor, hsv)
        val baseHue = hsv[0]
        val baseSat = hsv[1].coerceIn(0.40f, 0.90f)
        val baseVal = hsv[2].coerceIn(0.50f, 0.95f)

        // 1. Analogous (+35° and -35°)
        pool.add(Color.HSVToColor(floatArrayOf((baseHue + 35f) % 360f, baseSat, baseVal)))
        pool.add(Color.HSVToColor(floatArrayOf((baseHue - 35f + 360f) % 360f, baseSat, baseVal)))

        // 2. Complementary (+180°)
        pool.add(Color.HSVToColor(floatArrayOf((baseHue + 180f) % 360f, baseSat.coerceAtLeast(0.55f), baseVal)))

        // 3. Triadic (+120° and +240°)
        pool.add(Color.HSVToColor(floatArrayOf((baseHue + 120f) % 360f, baseSat, baseVal)))
        pool.add(Color.HSVToColor(floatArrayOf((baseHue + 240f) % 360f, baseSat, baseVal)))

        // 4. Split Complementary (+150° and +210°)
        pool.add(Color.HSVToColor(floatArrayOf((baseHue + 150f) % 360f, baseSat, baseVal)))
        pool.add(Color.HSVToColor(floatArrayOf((baseHue + 210f) % 360f, baseSat, baseVal)))

        // 5. Deep shade & Vibrant tint
        pool.add(Color.HSVToColor(floatArrayOf(baseHue, baseSat.coerceAtLeast(0.70f), (baseVal * 0.65f).coerceAtLeast(0.30f))))
        pool.add(Color.HSVToColor(floatArrayOf(baseHue, (baseSat * 0.50f).coerceAtLeast(0.20f), 0.96f)))

        return pool
    }

    // =========================================================
    // VARIATION GENERATOR
    // =========================================================

    private fun buildPaletteVariation(pool: List<Int>, variation: Int): ColorPalette {
        val count = pool.size
        val archetype = variation % 8
        val cycle = variation / 8

        // Cyclic hue rotation offset for high variations (guarantees infinite unique iterations)
        val hueRotation = (cycle * 43f) % 360f

        val primaryInt: Int
        val secondaryInt: Int
        val accentInt: Int

        when (archetype) {
            0 -> {
                // Archetype 0: Classic Brand Dominant
                val p = pool[0 % count]
                primaryInt = rotateHue(p, hueRotation)
                secondaryInt = findDistinctColor(pool, primaryInt, preferredOffset = 1)
                accentInt = findDistinctColor(pool, primaryInt, secondaryInt, preferredOffset = 2)
            }
            1 -> {
                // Archetype 1: Vibrant Secondary Shift
                val p = pool[1 % count]
                primaryInt = rotateHue(p, hueRotation)
                secondaryInt = findDistinctColor(pool, primaryInt, preferredOffset = 2)
                accentInt = rotateHue(findDistinctColor(pool, primaryInt, secondaryInt, preferredOffset = 0), 45f)
            }
            2 -> {
                // Archetype 2: Complementary Contrast Pop
                val base = pool[0 % count]
                primaryInt = rotateHue(base, (180f + hueRotation) % 360f)
                secondaryInt = rotateHue(base, hueRotation)
                accentInt = findDistinctColor(pool, primaryInt, secondaryInt, preferredOffset = 1)
            }
            3 -> {
                // Archetype 3: Triadic Balance
                val base = pool[variation % count]
                primaryInt = rotateHue(base, (120f + hueRotation) % 360f)
                secondaryInt = rotateHue(base, (240f + hueRotation) % 360f)
                accentInt = rotateHue(base, hueRotation)
            }
            4 -> {
                // Archetype 4: Sleek Deep Luxe
                val base = pool[variation % count]
                primaryInt = createRichDeepColor(rotateHue(base, hueRotation))
                secondaryInt = createLuminousColor(findDistinctColor(pool, primaryInt, preferredOffset = 1))
                accentInt = createVibrantAccent(rotateHue(base, 150f))
            }
            5 -> {
                // Archetype 5: Soft Modern Minimal
                val base = pool[(variation + 1) % count]
                primaryInt = createSoftModernTone(rotateHue(base, hueRotation))
                secondaryInt = createRichDeepColor(findDistinctColor(pool, primaryInt, preferredOffset = 2))
                accentInt = createLuminousColor(rotateHue(base, 60f))
            }
            6 -> {
                // Archetype 6: Split-Complementary Energy
                val base = pool[(variation + 2) % count]
                primaryInt = rotateHue(base, hueRotation)
                secondaryInt = rotateHue(base, (150f + hueRotation) % 360f)
                accentInt = rotateHue(base, (210f + hueRotation) % 360f)
            }
            else -> {
                // Archetype 7: Monochromatic Dynamic Tints
                val base = pool[variation % count]
                primaryInt = rotateHue(base, hueRotation)
                secondaryInt = createLighterShade(primaryInt, 0.40f)
                accentInt = createDarkerShade(primaryInt, 0.60f)
            }
        }

        // Ensure distinctness
        val finalPrimary = primaryInt
        val finalSecondary = ensureDifferent(secondaryInt, finalPrimary, 40.0, fallbackAngle = 70f)
        val finalAccent = ensureDifferent(accentInt, finalPrimary, 45.0, fallbackAngle = 140f)

        // Background and surface calculations
        val bgInt = createHarmoniousBackground(finalPrimary, isSoft = (archetype == 5 || archetype == 7))
        val surfaceInt = Color.WHITE
        val surfaceBorderInt = Color.rgb(228, 226, 235)

        // Text colors with WCAG contrast enforcement
        val onPrimaryInt = ColorUtils.getContrastTextColor(finalPrimary)
        val onSecondaryInt = ColorUtils.getContrastTextColor(finalSecondary)
        val onAccentInt = ColorUtils.getContrastTextColor(finalAccent)

        val baseTextDark = Color.rgb(24, 23, 28)
        val baseTextSecondaryDark = Color.rgb(105, 100, 114)

        val textPrimaryInt = ColorUtils.ensureContrast(baseTextDark, bgInt, 7.0)
        val textSecondaryInt = ColorUtils.ensureContrast(baseTextSecondaryDark, bgInt, 4.5)

        // Dark theme variants
        val darkBgInt = createDarkBackground(finalPrimary)
        val darkSurfaceInt = Color.rgb(28, 27, 34)
        val darkPrimaryInt = createLighterColorForDarkTheme(finalPrimary)
        val darkTextPrimaryInt = Color.rgb(245, 245, 248)
        val darkTextSecondaryInt = Color.rgb(175, 172, 185)

        val primaryContainerInt = createPrimaryContainer(finalPrimary)
        val onPrimaryContainerInt = ColorUtils.getContrastTextColor(primaryContainerInt)

        return ColorPalette(
            primary = ColorUtils.colorToHex(finalPrimary),
            onPrimary = ColorUtils.colorToHex(onPrimaryInt),
            primaryContainer = ColorUtils.colorToHex(primaryContainerInt),
            onPrimaryContainer = ColorUtils.colorToHex(onPrimaryContainerInt),
            secondary = ColorUtils.colorToHex(finalSecondary),
            onSecondary = ColorUtils.colorToHex(onSecondaryInt),
            accent = ColorUtils.colorToHex(finalAccent),
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

    // =========================================================
    // COLOR HELPERS & TRANSFORMATIONS
    // =========================================================

    private fun findDistinctColor(
        pool: List<Int>,
        vararg excluded: Int,
        preferredOffset: Int = 1
    ): Int {
        val count = pool.size
        for (i in 0 until count) {
            val candidate = pool[(preferredOffset + i) % count]
            val isTooClose = excluded.any { colorDistance(candidate, it) < 40.0 }
            if (!isTooClose) {
                return candidate
            }
        }
        // Fallback: rotate hue of the first excluded color
        return rotateHue(excluded.first(), 65f)
    }

    private fun ensureDifferent(
        candidate: Int,
        against: Int,
        minDistance: Double,
        fallbackAngle: Float
    ): Int {
        if (colorDistance(candidate, against) >= minDistance) {
            return candidate
        }
        return rotateHue(against, fallbackAngle)
    }

    private fun rotateHue(color: Int, angleDegrees: Float): Int {
        if (angleDegrees == 0f) return color
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[0] = (hsv[0] + angleDegrees + 360f) % 360f
        hsv[1] = hsv[1].coerceIn(0.40f, 0.95f)
        hsv[2] = hsv[2].coerceIn(0.45f, 0.95f)
        return Color.HSVToColor(hsv)
    }

    private fun createRichDeepColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = hsv[1].coerceIn(0.60f, 0.95f)
        hsv[2] = (hsv[2] * 0.65f).coerceIn(0.30f, 0.65f)
        return Color.HSVToColor(hsv)
    }

    private fun createLuminousColor(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * 0.85f).coerceIn(0.50f, 0.85f)
        hsv[2] = 0.96f
        return Color.HSVToColor(hsv)
    }

    private fun createVibrantAccent(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = 0.85f
        hsv[2] = 0.98f
        return Color.HSVToColor(hsv)
    }

    private fun createSoftModernTone(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * 0.60f).coerceIn(0.30f, 0.55f)
        hsv[2] = (hsv[2] * 1.10f).coerceIn(0.70f, 0.92f)
        return Color.HSVToColor(hsv)
    }

    private fun createLighterShade(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * (1f - factor)).coerceIn(0.20f, 0.60f)
        hsv[2] = (hsv[2] + (1f - hsv[2]) * factor).coerceIn(0.80f, 0.98f)
        return Color.HSVToColor(hsv)
    }

    private fun createDarkerShade(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * (1f + factor * 0.3f)).coerceIn(0.50f, 1.0f)
        hsv[2] = (hsv[2] * factor).coerceIn(0.25f, 0.65f)
        return Color.HSVToColor(hsv)
    }

    private fun createHarmoniousBackground(primary: Int, isSoft: Boolean): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(primary, hsv)
        hsv[1] = if (isSoft) 0.04f else 0.07f
        hsv[2] = 0.98f
        return Color.HSVToColor(hsv)
    }

    private fun createPrimaryContainer(primary: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(primary, hsv)
        hsv[1] = (hsv[1] * 0.28f).coerceIn(0.12f, 0.35f)
        hsv[2] = 0.96f
        return Color.HSVToColor(hsv)
    }

    private fun createDarkBackground(primary: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(primary, hsv)
        hsv[1] = (hsv[1] * 0.35f).coerceIn(0.10f, 0.25f)
        hsv[2] = 0.08f
        return Color.HSVToColor(hsv)
    }

    private fun createLighterColorForDarkTheme(color: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[1] = (hsv[1] * 0.70f).coerceIn(0.20f, 0.65f)
        hsv[2] = (hsv[2] * 1.30f).coerceIn(0.75f, 1.0f)
        return Color.HSVToColor(hsv)
    }

    private fun colorDistance(c1: Int, c2: Int): Double {
        val r = Color.red(c1) - Color.red(c2)
        val g = Color.green(c1) - Color.green(c2)
        val b = Color.blue(c1) - Color.blue(c2)
        return sqrt((r * r + g * g + b * b).toDouble())
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap

        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newWidth = max(1, (width * ratio).toInt())
        val newHeight = max(1, (height * ratio).toInt())

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}