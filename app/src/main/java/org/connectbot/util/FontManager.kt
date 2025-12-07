/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2025 Kenny Root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot.util

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import org.connectbot.R
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages downloadable fonts from Google Fonts for terminal display.
 *
 * This class handles:
 * - Defining available monospace programming fonts
 * - Asynchronous font downloading via FontsContractCompat
 * - Caching of loaded typefaces
 * - Fallback to system monospace when fonts fail to load
 */
object FontManager {

    private const val TAG = "FontManager"

    /**
     * Font identifier for system default monospace font.
     */
    const val SYSTEM_DEFAULT = "monospace"

    /**
     * Available monospace fonts for terminal display.
     * Key is the internal identifier, value is the display name.
     */
    val availableFonts: List<Pair<String, String>> = listOf(
        SYSTEM_DEFAULT to "System Default",
        "jetbrains_mono" to "JetBrains Mono",
        "fira_code" to "Fira Code",
        "source_code_pro" to "Source Code Pro",
        "roboto_mono" to "Roboto Mono",
        "ubuntu_mono" to "Ubuntu Mono",
        "hack" to "Hack",
        "inconsolata" to "Inconsolata",
        "anonymous_pro" to "Anonymous Pro",
        "cascadia_code" to "Cascadia Code",
        "ibm_plex_mono" to "IBM Plex Mono",
        "cousine" to "Cousine",
        "pt_mono" to "PT Mono",
        "space_mono" to "Space Mono",
        "overpass_mono" to "Overpass Mono",
        "noto_sans_mono" to "Noto Sans Mono",
        "droid_sans_mono" to "Droid Sans Mono"
    )

    /**
     * Map of font identifiers to Google Fonts query names.
     */
    private val fontQueryMap: Map<String, String> = mapOf(
        "jetbrains_mono" to "JetBrains Mono",
        "fira_code" to "Fira Code",
        "source_code_pro" to "Source Code Pro",
        "roboto_mono" to "Roboto Mono",
        "ubuntu_mono" to "Ubuntu Mono",
        "hack" to "Hack",
        "inconsolata" to "Inconsolata",
        "anonymous_pro" to "Anonymous Pro",
        "cascadia_code" to "Cascadia Code",
        "ibm_plex_mono" to "IBM Plex Mono",
        "cousine" to "Cousine",
        "pt_mono" to "PT Mono",
        "space_mono" to "Space Mono",
        "overpass_mono" to "Overpass Mono",
        "noto_sans_mono" to "Noto Sans Mono",
        "droid_sans_mono" to "Droid Sans Mono"
    )

    /**
     * Cache of loaded typefaces.
     */
    private val typefaceCache = ConcurrentHashMap<String, Typeface>()

    /**
     * Handler thread for font loading operations.
     */
    private val handlerThread = HandlerThread("FontManager").apply { start() }
    private val handler = Handler(handlerThread.looper)

    /**
     * Get the display name for a font identifier.
     *
     * @param fontId The internal font identifier
     * @return The display name, or the identifier if not found
     */
    fun getDisplayName(fontId: String): String {
        return availableFonts.find { it.first == fontId }?.second ?: fontId
    }

    /**
     * Get a cached typeface synchronously, or return default monospace.
     *
     * @param fontId The font identifier
     * @return The cached typeface or system monospace if not loaded
     */
    fun getTypeface(fontId: String?): Typeface {
        if (fontId == null || fontId == SYSTEM_DEFAULT) {
            return Typeface.MONOSPACE
        }
        return typefaceCache[fontId] ?: Typeface.MONOSPACE
    }

    /**
     * Check if a font is cached and ready to use.
     *
     * @param fontId The font identifier
     * @return true if the font is loaded and cached
     */
    fun isFontLoaded(fontId: String?): Boolean {
        if (fontId == null || fontId == SYSTEM_DEFAULT) {
            return true
        }
        return typefaceCache.containsKey(fontId)
    }

    /**
     * Load a font asynchronously from Google Fonts.
     *
     * @param context Android context
     * @param fontId The font identifier to load
     * @param onLoaded Callback invoked when font is loaded (or failed)
     */
    fun loadFont(
        context: Context,
        fontId: String?,
        onLoaded: (Typeface) -> Unit
    ) {
        if (fontId == null || fontId == SYSTEM_DEFAULT) {
            onLoaded(Typeface.MONOSPACE)
            return
        }

        // Return cached typeface if available
        typefaceCache[fontId]?.let {
            onLoaded(it)
            return
        }

        // Get the Google Fonts query name
        val fontQuery = fontQueryMap[fontId]
        if (fontQuery == null) {
            onLoaded(Typeface.MONOSPACE)
            return
        }

        // Create font request for Google Fonts provider
        val request = FontRequest(
            "com.google.android.gms.fonts",
            "com.google.android.gms",
            fontQuery,
            R.array.com_google_android_gms_fonts_certs
        )

        val callback = object : FontsContractCompat.FontRequestCallback() {
            override fun onTypefaceRetrieved(typeface: Typeface) {
                typefaceCache[fontId] = typeface
                onLoaded(typeface)
            }

            override fun onTypefaceRequestFailed(reason: Int) {
                // Fall back to system monospace on failure
                onLoaded(Typeface.MONOSPACE)
            }
        }

        FontsContractCompat.requestFont(context, request, callback, handler)
    }

    /**
     * Preload a font in the background.
     * Useful for loading fonts before they're needed.
     *
     * @param context Android context
     * @param fontId The font identifier to preload
     */
    fun preloadFont(context: Context, fontId: String?) {
        loadFont(context, fontId) { /* ignored */ }
    }

    /**
     * Resolve the effective font to use, considering per-host and global settings.
     *
     * @param hostFont The per-host font setting (null means use global default)
     * @param globalFont The global font setting
     * @return The effective font identifier to use
     */
    fun resolveEffectiveFont(hostFont: String?, globalFont: String): String {
        return hostFont ?: globalFont
    }

    /**
     * Clear the typeface cache.
     * Useful for testing or when memory pressure is high.
     */
    fun clearCache() {
        typefaceCache.clear()
    }
}
