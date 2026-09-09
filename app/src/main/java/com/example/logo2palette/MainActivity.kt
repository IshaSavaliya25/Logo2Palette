package com.example.logo2palette

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.example.logo2palette.model.ColorPalette
import com.example.logo2palette.utils.ColorExtractor
import com.example.logo2palette.utils.ColorUtils
import com.example.logo2palette.utils.PaletteHistoryManager
import com.example.logo2palette.utils.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var logoImage: ImageView
    private lateinit var uploadButton: Button
    private lateinit var generateButton: Button
    private lateinit var anotherPaletteButton: Button
    private lateinit var previewButton: Button
    private lateinit var pdfButton: Button
    private lateinit var exportCodeButton: Button
    private lateinit var savePaletteButton: Button
    private lateinit var profileButton: Button

    private lateinit var resultContainer: LinearLayout
    private lateinit var paletteNumber: TextView

    // Swatch 1: Primary
    private lateinit var cardPrimary: CardView
    private lateinit var swatchPrimary: View
    private lateinit var primaryHex: TextView
    private lateinit var primaryBadge: TextView

    // Swatch 2: Secondary
    private lateinit var cardSecondary: CardView
    private lateinit var swatchSecondary: View
    private lateinit var secondaryHex: TextView
    private lateinit var secondaryBadge: TextView

    // Swatch 3: Accent
    private lateinit var cardAccent: CardView
    private lateinit var swatchAccent: View
    private lateinit var accentHex: TextView
    private lateinit var accentBadge: TextView

    // Swatch 4: Background
    private lateinit var cardBackground: CardView
    private lateinit var swatchBackground: View
    private lateinit var backgroundHex: TextView
    private lateinit var backgroundBadge: TextView

    // Swatch 5: Surface
    private lateinit var cardSurface: CardView
    private lateinit var swatchSurface: View
    private lateinit var surfaceHex: TextView
    private lateinit var surfaceBadge: TextView

    // Swatch 6: Text Primary
    private lateinit var cardText: CardView
    private lateinit var swatchText: View
    private lateinit var textHex: TextView
    private lateinit var textBadge: TextView

    private var selectedBitmap: Bitmap? = null
    private var generatedPalette: ColorPalette? = null
    private var paletteVersion = 0

    // =========================================================
    // IMAGE PICKER
    // =========================================================

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult

            try {
                val bitmap = getBitmapFromUri(uri)
                if (bitmap != null) {
                    selectedBitmap = bitmap
                    logoImage.setImageBitmap(bitmap)

                    generateButton.isEnabled = true
                    anotherPaletteButton.isEnabled = false
                    previewButton.isEnabled = false
                    pdfButton.isEnabled = false
                    exportCodeButton.isEnabled = false
                    savePaletteButton.isEnabled = false

                    generatedPalette = null
                    paletteVersion = 0
                    resultContainer.visibility = View.GONE

                    Toast.makeText(this, "Logo selected successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Unable to read image", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error loading image", Toast.LENGTH_LONG).show()
            }
        }

    // =========================================================
    // CREATE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        // Enforce consistent light mode for brand generator workspace
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)

        // Strict Login Gate
        if (!UserSessionManager.isLoggedIn(this)) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        initializeViews()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        if (!UserSessionManager.isLoggedIn(this)) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        updateProfileButtonBadge()
    }

    private fun updateProfileButtonBadge() {
        val user = UserSessionManager.getCurrentUser(this)
        if (user != null) {
            val shortName = if (user.name.length > 10) "${user.name.take(8)}.." else user.name
            profileButton.text = "👤 $shortName"
        }
    }

    // =========================================================
    // INITIALIZE
    // =========================================================

    private fun initializeViews() {
        logoImage = findViewById(R.id.logoImage)
        uploadButton = findViewById(R.id.uploadButton)
        generateButton = findViewById(R.id.generateButton)
        anotherPaletteButton = findViewById(R.id.anotherPaletteButton)
        previewButton = findViewById(R.id.previewButton)
        pdfButton = findViewById(R.id.pdfButton)
        exportCodeButton = findViewById(R.id.exportCodeButton)
        savePaletteButton = findViewById(R.id.savePaletteButton)
        profileButton = findViewById(R.id.profileButton)

        resultContainer = findViewById(R.id.resultContainer)
        paletteNumber = findViewById(R.id.paletteNumber)

        cardPrimary = findViewById(R.id.cardPrimary)
        swatchPrimary = findViewById(R.id.swatchPrimary)
        primaryHex = findViewById(R.id.primaryHex)
        primaryBadge = findViewById(R.id.primaryBadge)

        cardSecondary = findViewById(R.id.cardSecondary)
        swatchSecondary = findViewById(R.id.swatchSecondary)
        secondaryHex = findViewById(R.id.secondaryHex)
        secondaryBadge = findViewById(R.id.secondaryBadge)

        cardAccent = findViewById(R.id.cardAccent)
        swatchAccent = findViewById(R.id.swatchAccent)
        accentHex = findViewById(R.id.accentHex)
        accentBadge = findViewById(R.id.accentBadge)

        cardBackground = findViewById(R.id.cardBackground)
        swatchBackground = findViewById(R.id.swatchBackground)
        backgroundHex = findViewById(R.id.backgroundHex)
        backgroundBadge = findViewById(R.id.backgroundBadge)

        cardSurface = findViewById(R.id.cardSurface)
        swatchSurface = findViewById(R.id.swatchSurface)
        surfaceHex = findViewById(R.id.surfaceHex)
        surfaceBadge = findViewById(R.id.surfaceBadge)

        cardText = findViewById(R.id.cardText)
        swatchText = findViewById(R.id.swatchText)
        textHex = findViewById(R.id.textHex)
        textBadge = findViewById(R.id.textBadge)
    }

    // =========================================================
    // LISTENERS
    // =========================================================

    private fun setupListeners() {
        profileButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        uploadButton.setOnClickListener {
            imagePicker.launch("image/*")
        }

        generateButton.setOnClickListener {
            paletteVersion = 0
            generatePalette()
        }

        anotherPaletteButton.setOnClickListener {
            paletteVersion++
            generatePalette()
        }

        savePaletteButton.setOnClickListener {
            saveCurrentPalette()
        }

        previewButton.setOnClickListener {
            openWebsitePreview()
        }

        pdfButton.setOnClickListener {
            createPdfReport()
        }

        exportCodeButton.setOnClickListener {
            showCodeExportDialog()
        }
    }

    // =========================================================
    // GENERATE PALETTE
    // =========================================================

    private fun generatePalette() {
        val bitmap = selectedBitmap ?: run {
            Toast.makeText(this, "Please upload a logo first", Toast.LENGTH_SHORT).show()
            return
        }

        generateButton.isEnabled = false
        anotherPaletteButton.isEnabled = false
        previewButton.isEnabled = false
        pdfButton.isEnabled = false
        exportCodeButton.isEnabled = false
        savePaletteButton.isEnabled = false

        Toast.makeText(this, "Analyzing logo...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            try {
                val palette = withContext(Dispatchers.Default) {
                    ColorExtractor.generatePalette(bitmap, paletteVersion)
                }

                generatedPalette = palette
                displayPalette(palette)

                generateButton.isEnabled = true
                anotherPaletteButton.isEnabled = true
                previewButton.isEnabled = true
                pdfButton.isEnabled = true
                exportCodeButton.isEnabled = true
                savePaletteButton.isEnabled = true

                paletteNumber.text = "Palette ${paletteVersion + 1}"
                Toast.makeText(this@MainActivity, "Palette ${paletteVersion + 1} generated 🎨", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                e.printStackTrace()
                generateButton.isEnabled = true
                anotherPaletteButton.isEnabled = true

                Toast.makeText(this@MainActivity, "Could not generate palette: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // =========================================================
    // SAVE PALETTE TO USER PROFILE
    // =========================================================

    private fun saveCurrentPalette() {
        val palette = generatedPalette ?: return
        val user = UserSessionManager.getCurrentUser(this) ?: return

        PaletteHistoryManager.savePalette(this, user.id, "Brand Palette #${paletteVersion + 1}", palette)
        Toast.makeText(this, "⭐ Saved to your profile!", Toast.LENGTH_SHORT).show()
    }

    // =========================================================
    // IMAGE LOADING
    // =========================================================

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = false
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // =========================================================
    // DISPLAY PALETTE
    // =========================================================

    private fun displayPalette(palette: ColorPalette) {
        setupSwatchCard(cardPrimary, swatchPrimary, primaryHex, primaryBadge, palette.primary, palette.onPrimary)
        setupSwatchCard(cardSecondary, swatchSecondary, secondaryHex, secondaryBadge, palette.secondary, palette.onSecondary)
        setupSwatchCard(cardAccent, swatchAccent, accentHex, accentBadge, palette.accent, palette.onAccent)
        setupSwatchCard(cardBackground, swatchBackground, backgroundHex, backgroundBadge, palette.background, palette.onBackground)
        setupSwatchCard(cardSurface, swatchSurface, surfaceHex, surfaceBadge, palette.surface, palette.onSurface)
        setupSwatchCard(cardText, swatchText, textHex, textBadge, palette.textPrimary, palette.background)

        resultContainer.visibility = View.VISIBLE
    }

    private fun setupSwatchCard(
        cardView: CardView,
        swatchView: View,
        hexTextView: TextView,
        badgeTextView: TextView,
        hexColorStr: String,
        contrastTargetStr: String
    ) {
        try {
            val color = Color.parseColor(hexColorStr)
            val textColorInt = ColorUtils.getContrastTextColor(color)

            val contrastRatio = ColorUtils.calculateContrastRatio(textColorInt, color)
            val badgeLabel = ColorUtils.getWcagBadge(contrastRatio)

            // Color circle swatch
            swatchView.background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#E0E0E0"))
            }

            hexTextView.text = "$hexColorStr  •  $badgeLabel"

            // Sample badge pill with guaranteed contrast text
            badgeTextView.text = " Sample Text "
            badgeTextView.setTextColor(textColorInt)
            badgeTextView.background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = 16f
                setStroke(1, Color.parseColor("#CCCCCC"))
            }

            cardView.setOnClickListener {
                copyColor(hexColorStr)
            }

        } catch (e: Exception) {
            hexTextView.text = hexColorStr
        }
    }

    // =========================================================
    // COPY & CODE EXPORT
    // =========================================================

    private fun copyColor(hex: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Color", hex)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$hex copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun copyCode(label: String, code: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(label, code)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "$label copied to clipboard! 🚀", Toast.LENGTH_SHORT).show()
    }

    private fun showCodeExportDialog() {
        val palette = generatedPalette ?: return

        val formats = arrayOf(
            "CSS Variables (:root)",
            "Tailwind CSS Config",
            "Android XML (colors.xml)",
            "Jetpack Compose (Color.kt)",
            "Flutter (AppColors.dart)",
            "JSON Format"
        )

        AlertDialog.Builder(this)
            .setTitle("💻 Export Code Tokens")
            .setItems(formats) { _, which ->
                val (label, code) = when (which) {
                    0 -> "CSS Variables" to ColorUtils.toCssVariables(palette)
                    1 -> "Tailwind Config" to ColorUtils.toTailwindConfig(palette)
                    2 -> "Android XML" to ColorUtils.toAndroidXml(palette)
                    3 -> "Jetpack Compose" to ColorUtils.toJetpackCompose(palette)
                    4 -> "Flutter" to ColorUtils.toFlutter(palette)
                    else -> "JSON" to ColorUtils.toJson(palette)
                }

                AlertDialog.Builder(this)
                    .setTitle(label)
                    .setMessage(code)
                    .setPositiveButton("Copy Code") { _, _ ->
                        copyCode(label, code)
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // =========================================================
    // WEBSITE PREVIEW
    // =========================================================

    private fun openWebsitePreview() {
        val palette = generatedPalette ?: run {
            Toast.makeText(this, "Generate a palette first", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, PreviewActivity::class.java).apply {
            putExtra("palette", palette)
            putExtra("primary", palette.primary)
            putExtra("secondary", palette.secondary)
            putExtra("accent", palette.accent)
            putExtra("background", palette.background)
            putExtra("surface", palette.surface)
            putExtra("textPrimary", palette.textPrimary)
            putExtra("textSecondary", palette.textSecondary)
        }

        startActivity(intent)
    }

    // =========================================================
    // PDF
    // =========================================================

    private fun createPdfReport() {
        val palette = generatedPalette
        val bitmap = selectedBitmap

        if (palette == null || bitmap == null) {
            Toast.makeText(this, "Generate a palette first", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val pdfFile = withContext(Dispatchers.Default) {
                    PdfReportGenerator.createReport(
                        this@MainActivity,
                        bitmap,
                        palette,
                        paletteVersion + 1
                    )
                }

                PdfReportGenerator.sharePdf(this@MainActivity, pdfFile)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "PDF creation failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}