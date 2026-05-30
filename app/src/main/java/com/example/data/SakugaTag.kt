package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SakugaTag(
    val id: Int = 0,
    val name: String = "",
    val count: Int = 0,
    val type: Int = 0, // 0 = General, 1 = Artist, 3 = Copyright, 4 = Character, 5 = Metadata
    val ambiguous: Boolean = false
)

enum class SakugaTagCategory(
    val id: Int, 
    val displayName: String, 
    val darkColorHex: Long, 
    val lightColorHex: Long
) {
    GENERAL(0, "General", 0xFFFF8F6B, 0xFFE07A5F),     // Cool coral/terracotta
    ARTIST(1, "Artist", 0xFFFCD34D, 0xFFDFAF1C),       // Yellow/Gold
    COPYRIGHT(3, "Copyright", 0xFFD6BCFA, 0xFF9F7AEA), // Purple/Lavender
    CHARACTER(4, "Character", 0xFF6EE7B7, 0xFF38A169), // Emerald/Green
    METADATA(5, "Metadata", 0xFFCBD5E0, 0xFF718096),   // Silver/Gray
    UNKNOWN(-1, "Unknown", 0xFFA0AEC0, 0xFF4A5568);

    companion object {
        fun fromId(id: Int): SakugaTagCategory {
            return values().find { it.id == id } ?: UNKNOWN
        }
    }
}
