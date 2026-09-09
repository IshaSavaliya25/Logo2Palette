package com.example.logo2palette

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.logo2palette.model.ColorPalette
import com.example.logo2palette.utils.ColorUtils

class PreviewActivity : AppCompatActivity() {

    private var palette: ColorPalette? = null
    private var isDarkMode = false
    private var isMobileView = false

    private lateinit var mainContainer: LinearLayout
    private lateinit var contentScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        palette = (intent.getSerializableExtra("palette") as? ColorPalette) ?: ColorPalette(
            primary = intent.getStringExtra("primary") ?: "#6750A4",
            secondary = intent.getStringExtra("secondary") ?: "#9C7BCE",
            accent = intent.getStringExtra("accent") ?: "#FFB74D",
            background = intent.getStringExtra("background") ?: "#F8F7FC",
            surface = intent.getStringExtra("surface") ?: "#FFFFFF",
            textPrimary = intent.getStringExtra("textPrimary") ?: "#1D1B20",
            textSecondary = intent.getStringExtra("textSecondary") ?: "#666666"
        )

        buildRootView()
    }

    private fun buildRootView() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Top Control Panel (Toggles for Web/Mobile UI and Light/Dark Theme)
        val controlPanel = createControlPanel()
        root.addView(controlPanel)

        // Scrollable Content View
        contentScrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFillViewport = true
        }

        mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        contentScrollView.addView(mainContainer)
        root.addView(contentScrollView)

        renderCurrentPreview()
        setContentView(root)
    }

    private fun createControlPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.parseColor("#1E1E24"))

            // Mode Toggle Button (Web vs Mobile UI)
            val modeBtn = Button(this@PreviewActivity).apply {
                text = if (isMobileView) "📱 Mobile App UI" else "🌐 Website UI"
                textSize = 13f
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#333340"))
                setOnClickListener {
                    isMobileView = !isMobileView
                    text = if (isMobileView) "📱 Mobile App UI" else "🌐 Website UI"
                    renderCurrentPreview()
                }
            }

            // Theme Toggle Button (Light vs Dark Mode)
            val themeBtn = Button(this@PreviewActivity).apply {
                text = if (isDarkMode) "🌙 Dark Mode" else "☀️ Light Mode"
                textSize = 13f
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#333340"))
                setOnClickListener {
                    isDarkMode = !isDarkMode
                    text = if (isDarkMode) "🌙 Dark Mode" else "☀️ Light Mode"
                    renderCurrentPreview()
                }
            }

            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            addView(modeBtn, params)
            addView(themeBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
    }

    private fun renderCurrentPreview() {
        mainContainer.removeAllViews()
        val pal = palette ?: return

        val bgHex = if (isDarkMode) pal.darkBackground else pal.background
        val surfaceHex = if (isDarkMode) pal.darkSurface else pal.surface
        val primaryHex = if (isDarkMode) pal.darkPrimary else pal.primary
        val textPrimaryHex = if (isDarkMode) pal.darkTextPrimary else pal.textPrimary
        val textSecondaryHex = if (isDarkMode) pal.darkTextSecondary else pal.textSecondary

        val bgColor = Color.parseColor(bgHex)
        val surfaceColor = Color.parseColor(surfaceHex)
        val primaryColor = Color.parseColor(primaryHex)
        val secondaryColor = Color.parseColor(pal.secondary)
        val accentColor = Color.parseColor(pal.accent)
        val textPrimaryColor = Color.parseColor(textPrimaryHex)
        val textSecondaryColor = Color.parseColor(textSecondaryHex)

        contentScrollView.setBackgroundColor(bgColor)

        if (isMobileView) {
            renderMobileAppPreview(bgColor, surfaceColor, primaryColor, secondaryColor, accentColor, textPrimaryColor, textSecondaryColor)
        } else {
            renderWebsitePreview(bgColor, surfaceColor, primaryColor, secondaryColor, accentColor, textPrimaryColor, textSecondaryColor)
        }
    }

    // =========================================================
    // WEBSITE UI PREVIEW
    // =========================================================

    private fun renderWebsitePreview(
        bgColor: Int,
        surfaceColor: Int,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        textPrimaryColor: Int,
        textSecondaryColor: Int
    ) {
        val onPrimary = ColorUtils.getContrastTextColor(primaryColor)
        val onAccent = ColorUtils.getContrastTextColor(accentColor)
        val guaranteedTitle = ColorUtils.ensureContrast(textPrimaryColor, bgColor, 7.0)
        val guaranteedSubtitle = ColorUtils.ensureContrast(textSecondaryColor, bgColor, 4.5)

        // 1. Navigation Bar
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(primaryColor)

            val brand = TextView(this@PreviewActivity).apply {
                text = "Logo2Palette"
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setTextColor(onPrimary)
            }
            addView(brand, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val links = TextView(this@PreviewActivity).apply {
                text = "Home   Products   About   Contact"
                textSize = 13f
                setTextColor(onPrimary)
            }
            addView(links)
        }
        mainContainer.addView(nav)

        // 2. Hero Section
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24, 48, 24, 36)

            // Accent Pill Badge
            val pill = TextView(this@PreviewActivity).apply {
                text = " BRAND COLOR SYSTEM "
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(onAccent)
                setPadding(16, 8, 16, 8)
                background = GradientDrawable().apply {
                    setColor(accentColor)
                    cornerRadius = 20f
                }
            }
            addView(pill)

            val title = TextView(this@PreviewActivity).apply {
                text = "Build something\nbeautiful."
                textSize = 32f
                setTypeface(null, Typeface.BOLD)
                setTextColor(guaranteedTitle)
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 0)
            }
            addView(title)

            val subtitle = TextView(this@PreviewActivity).apply {
                text = "A responsive web application interface designed with your extracted logo colors."
                textSize = 15f
                setTextColor(guaranteedSubtitle)
                gravity = Gravity.CENTER
                setPadding(12, 12, 12, 24)
            }
            addView(subtitle)

            val ctaButton = Button(this@PreviewActivity).apply {
                text = "Get Started  →"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(onPrimary)
                backgroundTintList = android.content.res.ColorStateList.valueOf(primaryColor)
            }
            addView(ctaButton)
        }
        mainContainer.addView(hero)

        // 3. Feature Cards
        val sectionTitle = TextView(this).apply {
            text = "Design System Features"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(guaranteedTitle)
            setPadding(24, 16, 24, 12)
        }
        mainContainer.addView(sectionTitle)

        val card1AccentText = ColorUtils.ensureContrast(primaryColor, surfaceColor, 4.5)
        val card2AccentText = ColorUtils.ensureContrast(secondaryColor, surfaceColor, 4.5)
        val card3AccentText = ColorUtils.ensureContrast(accentColor, surfaceColor, 4.5)

        mainContainer.addView(createWebCard("01", "Accessible Text Visibility", "All text and element colors are calculated using WCAG 2.1 contrast ratios.", card1AccentText, surfaceColor, textPrimaryColor, textSecondaryColor))
        mainContainer.addView(createWebCard("02", "Harmonious Palette System", "Colors are derived directly from your brand logo for perfect visual alignment.", card2AccentText, surfaceColor, textPrimaryColor, textSecondaryColor))
        mainContainer.addView(createWebCard("03", "Web & Mobile Ready", "Export code tokens directly for CSS, Tailwind, Android XML, Compose, Flutter, and JSON.", card3AccentText, surfaceColor, textPrimaryColor, textSecondaryColor))

        // 4. CTA Banner Card
        val ctaCard = CardView(this).apply {
            radius = 24f
            cardElevation = 4f
            setCardBackgroundColor(primaryColor)

            val ctaContent = LinearLayout(this@PreviewActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(24, 28, 24, 28)

                val ctaHead = TextView(this@PreviewActivity).apply {
                    text = "Ready to launch your project?"
                    textSize = 22f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(onPrimary)
                    gravity = Gravity.CENTER
                }
                addView(ctaHead)

                val ctaDesc = TextView(this@PreviewActivity).apply {
                    text = "Use your brand palette tokens in production with 100% confidence."
                    textSize = 14f
                    setTextColor(onPrimary)
                    gravity = Gravity.CENTER
                    setPadding(0, 8, 0, 16)
                }
                addView(ctaDesc)

                val ctaBtn = Button(this@PreviewActivity).apply {
                    text = "Export Theme Tokens"
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(onAccent)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(accentColor)
                }
                addView(ctaBtn)
            }
            addView(ctaContent)
        }

        val ctaParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(20, 20, 20, 28) }
        mainContainer.addView(ctaCard, ctaParams)
    }

    private fun createWebCard(
        step: String,
        title: String,
        desc: String,
        brandAccent: Int,
        surfaceColor: Int,
        textPrimary: Int,
        textSecondary: Int
    ): View {
        val card = CardView(this).apply {
            radius = 20f
            cardElevation = 3f
            setCardBackgroundColor(surfaceColor)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)

            val stepText = TextView(this@PreviewActivity).apply {
                text = step
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(brandAccent)
            }
            addView(stepText)

            val titleText = TextView(this@PreviewActivity).apply {
                text = title
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(ColorUtils.ensureContrast(textPrimary, surfaceColor, 7.0))
                setPadding(0, 6, 0, 4)
            }
            addView(titleText)

            val descText = TextView(this@PreviewActivity).apply {
                text = desc
                textSize = 14f
                setTextColor(ColorUtils.ensureContrast(textSecondary, surfaceColor, 4.5))
            }
            addView(descText)

            val line = View(this@PreviewActivity).apply {
                setBackgroundColor(brandAccent)
            }
            addView(line, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4).apply { topMargin = 14 })
        }

        card.addView(content)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(20, 0, 20, 16) }
        card.layoutParams = params
        return card
    }

    // =========================================================
    // MOBILE APP UI PREVIEW
    // =========================================================

    private fun renderMobileAppPreview(
        bgColor: Int,
        surfaceColor: Int,
        primaryColor: Int,
        secondaryColor: Int,
        accentColor: Int,
        textPrimaryColor: Int,
        textSecondaryColor: Int
    ) {
        val onPrimary = ColorUtils.getContrastTextColor(primaryColor)
        val onAccent = ColorUtils.getContrastTextColor(accentColor)
        val guaranteedTitle = ColorUtils.ensureContrast(textPrimaryColor, bgColor, 7.0)

        // 1. Mobile App Top Bar
        val appBar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(primaryColor)
            setPadding(20, 16, 20, 18)

            val statusText = TextView(this@PreviewActivity).apply {
                text = "9:41 AM  •  5G ⚡"
                textSize = 11f
                setTextColor(onPrimary)
            }
            addView(statusText)

            val titleRow = LinearLayout(this@PreviewActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 12, 0, 0)

                val appTitle = TextView(this@PreviewActivity).apply {
                    text = "Mobile Dashboard"
                    textSize = 20f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(onPrimary)
                }
                addView(appTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                val actionIcon = TextView(this@PreviewActivity).apply {
                    text = "🔔  ⚙️"
                    textSize = 16f
                    setTextColor(onPrimary)
                }
                addView(actionIcon)
            }
            addView(titleRow)
        }
        mainContainer.addView(appBar)

        // 2. Filter Pills / Categories
        val pillScroll = ScrollView(this).apply {
            isFillViewport = true
        }
        val pillRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 8)

            val pills = listOf("Overview", "Analytics", "Reports", "Settings")
            pills.forEachIndexed { index, pText ->
                val pillBtn = TextView(this@PreviewActivity).apply {
                    text = pText
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(20, 10, 20, 10)
                    if (index == 0) {
                        setTextColor(onAccent)
                        background = GradientDrawable().apply {
                            setColor(accentColor)
                            cornerRadius = 24f
                        }
                    } else {
                        setTextColor(ColorUtils.ensureContrast(textPrimaryColor, surfaceColor, 4.5))
                        background = GradientDrawable().apply {
                            setColor(surfaceColor)
                            cornerRadius = 24f
                            setStroke(2, Color.parseColor("#CCCCCC"))
                        }
                    }
                }
                val pParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 10 }
                addView(pillBtn, pParams)
            }
        }
        pillScroll.addView(pillRow)
        mainContainer.addView(pillScroll)

        // 3. Mobile Card Widgets
        val sectionText = TextView(this).apply {
            text = "Active Application Stats"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(guaranteedTitle)
            setPadding(20, 12, 20, 8)
        }
        mainContainer.addView(sectionText)

        // Dashboard Overview Card
        val overviewCard = CardView(this).apply {
            radius = 20f
            cardElevation = 3f
            setCardBackgroundColor(surfaceColor)

            val cardContent = LinearLayout(this@PreviewActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 20, 20, 20)

                val head = TextView(this@PreviewActivity).apply {
                    text = "Weekly Brand Impressions"
                    textSize = 14f
                    setTextColor(ColorUtils.ensureContrast(textSecondaryColor, surfaceColor, 4.5))
                }
                addView(head)

                val stat = TextView(this@PreviewActivity).apply {
                    text = "128,450"
                    textSize = 30f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ColorUtils.ensureContrast(primaryColor, surfaceColor, 4.5))
                    setPadding(0, 4, 0, 4)
                }
                addView(stat)

                val sub = TextView(this@PreviewActivity).apply {
                    text = "↑ +14.2% from last week"
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ColorUtils.ensureContrast(secondaryColor, surfaceColor, 4.5))
                }
                addView(sub)
            }
            addView(cardContent)
        }
        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(20, 8, 20, 14) }
        mainContainer.addView(overviewCard, cardParams)

        // List Item 1
        mainContainer.addView(createMobileListItem("🎨 Color Contrast Validation", "Passed WCAG AA compliance check", primaryColor, surfaceColor, textPrimaryColor, textSecondaryColor))
        mainContainer.addView(createMobileListItem("📄 Export Palette PDF", "Download generated brand identity guidelines", secondaryColor, surfaceColor, textPrimaryColor, textSecondaryColor))
        mainContainer.addView(createMobileListItem("💻 Code Token Generator", "Copy CSS, Tailwind, Android XML, Flutter", accentColor, surfaceColor, textPrimaryColor, textSecondaryColor))

        // Bottom Nav Bar Simulation
        val bottomNav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 20)
            setBackgroundColor(surfaceColor)

            val tabs = listOf("🏠 Home", "📊 Charts", "💬 Chat", "👤 Profile")
            tabs.forEachIndexed { idx, tText ->
                val tabTv = TextView(this@PreviewActivity).apply {
                    text = tText
                    textSize = 13f
                    gravity = Gravity.CENTER
                    setTypeface(null, if (idx == 0) Typeface.BOLD else Typeface.NORMAL)
                    val activeColor = ColorUtils.ensureContrast(primaryColor, surfaceColor, 4.5)
                    val inactiveColor = ColorUtils.ensureContrast(textSecondaryColor, surfaceColor, 4.5)
                    setTextColor(if (idx == 0) activeColor else inactiveColor)
                }
                addView(tabTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
        }
        val bNavParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 20 }
        mainContainer.addView(bottomNav, bNavParams)
    }

    private fun createMobileListItem(
        title: String,
        desc: String,
        accentColor: Int,
        surfaceColor: Int,
        textPrimary: Int,
        textSecondary: Int
    ): View {
        val card = CardView(this).apply {
            radius = 16f
            cardElevation = 2f
            setCardBackgroundColor(surfaceColor)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 16, 16, 16)

            val iconDot = View(this@PreviewActivity).apply {
                background = GradientDrawable().apply {
                    setColor(accentColor)
                    shape = GradientDrawable.OVAL
                }
            }
            addView(iconDot, LinearLayout.LayoutParams(24, 24).apply { marginEnd = 14 })

            val textCol = LinearLayout(this@PreviewActivity).apply {
                orientation = LinearLayout.VERTICAL

                val headTv = TextView(this@PreviewActivity).apply {
                    text = title
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ColorUtils.ensureContrast(textPrimary, surfaceColor, 7.0))
                }
                addView(headTv)

                val descTv = TextView(this@PreviewActivity).apply {
                    text = desc
                    textSize = 12f
                    setTextColor(ColorUtils.ensureContrast(textSecondary, surfaceColor, 4.5))
                    setPadding(0, 2, 0, 0)
                }
                addView(descTv)
            }
            addView(textCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        card.addView(content)
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(20, 0, 20, 10) }
        card.layoutParams = params
        return card
    }
}