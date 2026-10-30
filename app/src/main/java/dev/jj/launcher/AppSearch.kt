package dev.jj.launcher

import java.text.Normalizer
import java.util.Locale

/** Accent- and case-insensitive search, independent of Android APIs. */
fun normalizeForSearch(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase(Locale.ROOT)
        .trim()

fun appMatchesSearch(label: String, query: String): Boolean =
    normalizeForSearch(label).contains(normalizeForSearch(query))
