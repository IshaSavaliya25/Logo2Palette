package com.example.logo2palette.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.logo2palette.model.ColorPalette
import com.example.logo2palette.model.SavedPalette
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PaletteHistoryManager {

    private const val PREF_NAME = "logo2palette_saved_history"
    private const val KEY_SAVED_PALETTES = "saved_palettes_json"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    private fun getFirestore(): FirebaseFirestore? {
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    fun savePalette(
        context: Context,
        userId: String,
        title: String,
        palette: ColorPalette
    ): SavedPalette {
        val list = getAllSavedPalettes(context).toMutableList()
        val item = SavedPalette(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title.ifBlank { "Brand Palette #${list.size + 1}" },
            palette = palette
        )

        list.add(0, item)
        saveList(context, list)

        // Sync to Firebase Firestore asynchronously
        saveToFirestore(userId, item)

        return item
    }

    fun getUserSavedPalettes(context: Context, userId: String): List<SavedPalette> {
        return getAllSavedPalettes(context).filter { it.userId == userId }
    }

    fun getUserSavedPalettesAsync(
        context: Context,
        userId: String,
        onResult: (List<SavedPalette>) -> Unit
    ) {
        val localList = getUserSavedPalettes(context, userId)
        onResult(localList)

        val firestore = getFirestore() ?: return
        if (userId.isBlank() || userId.startsWith("demo")) return

        firestore.collection("users")
            .document(userId)
            .collection("palettes")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val remoteList = mutableListOf<SavedPalette>()
                    for (doc in snapshot.documents) {
                        val palMap = doc.get("palette") as? Map<*, *> ?: continue
                        val palette = ColorPalette(
                            primary = palMap["primary"] as? String ?: "#000000",
                            onPrimary = palMap["onPrimary"] as? String ?: "#FFFFFF",
                            primaryContainer = palMap["primaryContainer"] as? String ?: "#EADDFF",
                            onPrimaryContainer = palMap["onPrimaryContainer"] as? String ?: "#21005D",
                            secondary = palMap["secondary"] as? String ?: "#625B71",
                            onSecondary = palMap["onSecondary"] as? String ?: "#FFFFFF",
                            accent = palMap["accent"] as? String ?: "#7D5260",
                            onAccent = palMap["onAccent"] as? String ?: "#FFFFFF",
                            background = palMap["background"] as? String ?: "#FFFBFE",
                            onBackground = palMap["onBackground"] as? String ?: "#1C1B1F",
                            surface = palMap["surface"] as? String ?: "#FFFBFE",
                            onSurface = palMap["onSurface"] as? String ?: "#1C1B1F",
                            surfaceBorder = palMap["surfaceBorder"] as? String ?: "#E0E0E0",
                            textPrimary = palMap["textPrimary"] as? String ?: "#1C1B1F",
                            textSecondary = palMap["textSecondary"] as? String ?: "#49454F",
                            darkPrimary = palMap["darkPrimary"] as? String ?: "#D0BCFF",
                            darkBackground = palMap["darkBackground"] as? String ?: "#121212",
                            darkSurface = palMap["darkSurface"] as? String ?: "#1E1E1E",
                            darkTextPrimary = palMap["darkTextPrimary"] as? String ?: "#F5F5F5",
                            darkTextSecondary = palMap["darkTextSecondary"] as? String ?: "#B0B0B0"
                        )

                        val savedItem = SavedPalette(
                            id = doc.id,
                            userId = userId,
                            title = doc.getString("title") ?: "Brand Palette",
                            palette = palette,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                        remoteList.add(savedItem)
                    }

                    if (remoteList.isNotEmpty()) {
                        remoteList.sortByDescending { it.createdAt }
                        // Update local cache
                        val allLocal = getAllSavedPalettes(context).toMutableList()
                        allLocal.removeAll { it.userId == userId }
                        allLocal.addAll(0, remoteList)
                        saveList(context, allLocal)

                        onResult(remoteList)
                    }
                }
            }
    }

    fun deletePalette(context: Context, paletteId: String) {
        val list = getAllSavedPalettes(context)
        val targetItem = list.find { it.id == paletteId }
        val updatedList = list.filterNot { it.id == paletteId }
        saveList(context, updatedList)

        // Delete from Firestore if available
        val firestore = getFirestore()
        if (firestore != null && targetItem != null && !targetItem.userId.startsWith("demo")) {
            firestore.collection("users")
                .document(targetItem.userId)
                .collection("palettes")
                .document(paletteId)
                .delete()
        }
    }

    private fun saveToFirestore(userId: String, item: SavedPalette) {
        val firestore = getFirestore() ?: return
        if (userId.isBlank() || userId.startsWith("demo")) return

        val palMap = hashMapOf(
            "primary" to item.palette.primary,
            "onPrimary" to item.palette.onPrimary,
            "primaryContainer" to item.palette.primaryContainer,
            "onPrimaryContainer" to item.palette.onPrimaryContainer,
            "secondary" to item.palette.secondary,
            "onSecondary" to item.palette.onSecondary,
            "accent" to item.palette.accent,
            "onAccent" to item.palette.onAccent,
            "background" to item.palette.background,
            "onBackground" to item.palette.onBackground,
            "surface" to item.palette.surface,
            "onSurface" to item.palette.onSurface,
            "surfaceBorder" to item.palette.surfaceBorder,
            "textPrimary" to item.palette.textPrimary,
            "textSecondary" to item.palette.textSecondary,
            "darkPrimary" to item.palette.darkPrimary,
            "darkBackground" to item.palette.darkBackground,
            "darkSurface" to item.palette.darkSurface,
            "darkTextPrimary" to item.palette.darkTextPrimary,
            "darkTextSecondary" to item.palette.darkTextSecondary
        )

        val docMap = hashMapOf(
            "id" to item.id,
            "userId" to item.userId,
            "title" to item.title,
            "palette" to palMap,
            "createdAt" to item.createdAt
        )

        firestore.collection("users")
            .document(userId)
            .collection("palettes")
            .document(item.id)
            .set(docMap)
    }

    private fun getAllSavedPalettes(context: Context): List<SavedPalette> {
        val jsonStr = getPrefs(context).getString(KEY_SAVED_PALETTES, "[]") ?: "[]"
        val list = mutableListOf<SavedPalette>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val palObj = obj.getJSONObject("palette")

                val palette = ColorPalette(
                    primary = palObj.getString("primary"),
                    onPrimary = palObj.optString("onPrimary", "#FFFFFF"),
                    primaryContainer = palObj.optString("primaryContainer", "#EADDFF"),
                    onPrimaryContainer = palObj.optString("onPrimaryContainer", "#21005D"),
                    secondary = palObj.getString("secondary"),
                    onSecondary = palObj.optString("onSecondary", "#FFFFFF"),
                    accent = palObj.getString("accent"),
                    onAccent = palObj.optString("onAccent", "#FFFFFF"),
                    background = palObj.getString("background"),
                    onBackground = palObj.optString("onBackground", "#1C1B1F"),
                    surface = palObj.getString("surface"),
                    onSurface = palObj.optString("onSurface", "#1C1B1F"),
                    surfaceBorder = palObj.optString("surfaceBorder", "#E0E0E0"),
                    textPrimary = palObj.getString("textPrimary"),
                    textSecondary = palObj.getString("textSecondary"),
                    darkPrimary = palObj.optString("darkPrimary", palObj.getString("primary")),
                    darkBackground = palObj.optString("darkBackground", "#121212"),
                    darkSurface = palObj.optString("darkSurface", "#1E1E1E"),
                    darkTextPrimary = palObj.optString("darkTextPrimary", "#F5F5F5"),
                    darkTextSecondary = palObj.optString("darkTextSecondary", "#B0B0B0")
                )

                list.add(
                    SavedPalette(
                        id = obj.getString("id"),
                        userId = obj.getString("userId"),
                        title = obj.getString("title"),
                        palette = palette,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun saveList(context: Context, list: List<SavedPalette>) {
        val array = JSONArray()
        for (item in list) {
            val palObj = JSONObject().apply {
                put("primary", item.palette.primary)
                put("onPrimary", item.palette.onPrimary)
                put("primaryContainer", item.palette.primaryContainer)
                put("onPrimaryContainer", item.palette.onPrimaryContainer)
                put("secondary", item.palette.secondary)
                put("onSecondary", item.palette.onSecondary)
                put("accent", item.palette.accent)
                put("onAccent", item.palette.onAccent)
                put("background", item.palette.background)
                put("onBackground", item.palette.onBackground)
                put("surface", item.palette.surface)
                put("onSurface", item.palette.onSurface)
                put("surfaceBorder", item.palette.surfaceBorder)
                put("textPrimary", item.palette.textPrimary)
                put("textSecondary", item.palette.textSecondary)
                put("darkPrimary", item.palette.darkPrimary)
                put("darkBackground", item.palette.darkBackground)
                put("darkSurface", item.palette.darkSurface)
                put("darkTextPrimary", item.palette.darkTextPrimary)
                put("darkTextSecondary", item.palette.darkTextSecondary)
            }

            val obj = JSONObject().apply {
                put("id", item.id)
                put("userId", item.userId)
                put("title", item.title)
                put("palette", palObj)
                put("createdAt", item.createdAt)
            }
            array.put(obj)
        }
        getPrefs(context).edit().putString(KEY_SAVED_PALETTES, array.toString()).apply()
    }
}
