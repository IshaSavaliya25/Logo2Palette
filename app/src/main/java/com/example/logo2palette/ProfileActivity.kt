package com.example.logo2palette

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.logo2palette.model.ColorPalette
import com.example.logo2palette.model.SavedPalette
import com.example.logo2palette.model.User
import com.example.logo2palette.utils.ColorUtils
import com.example.logo2palette.utils.PaletteHistoryManager
import com.example.logo2palette.utils.UserSessionManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileActivity : AppCompatActivity() {

    private lateinit var avatarBadge: TextView
    private lateinit var userNameText: TextView
    private lateinit var userEmailText: TextView
    private lateinit var userCompanyText: TextView
    private lateinit var savedCountText: TextView
    private lateinit var logoutButton: Button
    private lateinit var palettesContainer: LinearLayout
    private lateinit var emptyStateText: TextView

    private var currentUser: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        avatarBadge = findViewById(R.id.avatarBadge)
        userNameText = findViewById(R.id.userNameText)
        userEmailText = findViewById(R.id.userEmailText)
        userCompanyText = findViewById(R.id.userCompanyText)
        savedCountText = findViewById(R.id.savedCountText)
        logoutButton = findViewById(R.id.logoutButton)
        palettesContainer = findViewById(R.id.palettesContainer)
        emptyStateText = findViewById(R.id.emptyStateText)

        logoutButton.setOnClickListener {
            UserSessionManager.logout(this)
            Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val user = UserSessionManager.getCurrentUser(this)
        if (user == null) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        currentUser = user

        avatarBadge.text = user.name.take(1).uppercase()
        userNameText.text = user.name
        userEmailText.text = user.email
        userCompanyText.text = user.companyName

        PaletteHistoryManager.getUserSavedPalettesAsync(this, user.id) { savedList ->
            savedCountText.text = "${savedList.size} Saved"
            renderSavedPalettes(savedList)
        }
    }

    private fun renderSavedPalettes(list: List<SavedPalette>) {
        palettesContainer.removeAllViews()

        if (list.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            return
        }

        emptyStateText.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        val dateFormat = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())

        list.forEach { item ->
            val card = inflater.inflate(R.layout.item_saved_palette, palettesContainer, false)

            val titleTv = card.findViewById<TextView>(R.id.paletteTitle)
            val dateTv = card.findViewById<TextView>(R.id.paletteDate)

            val s1 = card.findViewById<View>(R.id.swatch1)
            val s2 = card.findViewById<View>(R.id.swatch2)
            val s3 = card.findViewById<View>(R.id.swatch3)
            val s4 = card.findViewById<View>(R.id.swatch4)
            val s5 = card.findViewById<View>(R.id.swatch5)
            val s6 = card.findViewById<View>(R.id.swatch6)

            val btnPreview = card.findViewById<Button>(R.id.btnPreview)
            val btnCode = card.findViewById<Button>(R.id.btnCode)
            val btnDelete = card.findViewById<Button>(R.id.btnDelete)

            titleTv.text = item.title
            dateTv.text = dateFormat.format(Date(item.createdAt))

            setSwatchBg(s1, item.palette.primary)
            setSwatchBg(s2, item.palette.secondary)
            setSwatchBg(s3, item.palette.accent)
            setSwatchBg(s4, item.palette.background)
            setSwatchBg(s5, item.palette.surface)
            setSwatchBg(s6, item.palette.textPrimary)

            btnPreview.setOnClickListener {
                val intent = Intent(this, PreviewActivity::class.java).apply {
                    putExtra("palette", item.palette)
                }
                startActivity(intent)
            }

            btnCode.setOnClickListener {
                showCodeExportDialog(item.palette)
            }

            btnDelete.setOnClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete Palette")
                    .setMessage("Are you sure you want to delete '${item.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        PaletteHistoryManager.deletePalette(this, item.id)
                        Toast.makeText(this, "Palette deleted", Toast.LENGTH_SHORT).show()
                        loadUserData()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            palettesContainer.addView(card)
        }
    }

    private fun setSwatchBg(view: View, hex: String) {
        val color = try { Color.parseColor(hex) } catch (e: Exception) { Color.GRAY }
        view.background = GradientDrawable().apply {
            setColor(color)
            cornerRadius = 12f
            setStroke(1, Color.parseColor("#CCCCCC"))
        }
    }

    private fun showCodeExportDialog(palette: ColorPalette) {
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
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText(label, code)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "$label copied!", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
