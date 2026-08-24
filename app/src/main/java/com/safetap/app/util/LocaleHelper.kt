package com.safetap.app.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleHelper {
    private val languageMap = mapOf(
        "English (US)" to "en",
        "Español" to "es",
        "हिन्दी (Hindi)" to "hi",
        "Français" to "fr",
        "Deutsch" to "de",
        "日本語" to "ja"
    )

    fun applyLanguage(languageName: String) {
        val languageTag = languageMap[languageName] ?: "en"
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getSelectedLanguageName(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val currentLocale = if (!appLocales.isEmpty) {
            appLocales.toLanguageTags()
        } else {
            LocaleListCompat.getAdjustedDefault().toLanguageTags()
        }
        val tag = currentLocale.take(2).lowercase()
        return languageMap.entries.find { it.value == tag }?.key ?: "English (US)"
    }
    
    fun getLanguages(): List<String> = languageMap.keys.toList()
}
