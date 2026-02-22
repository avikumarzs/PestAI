package com.avikshit.PestAI.ui

/**
 * Hardcoded remedy data for exactly 4 supported pests.
 * Only Army_Fallworm, Paddy_Grasshopper, Field_Slug, Grain_Weevil have remedies.
 * All other pests show: "Remedy not yet implemented for this pest."
 */
data class RemedyItem(
    val pestKey: String,
    val pestName: String,
    val badgeCritical: Boolean,
    val remedyName: String,
    val effectiveness: String,
    val cost: String,
    val ingredients: String,
    val steps: String
)

object RemedyData {
    private val remedies = listOf(
        RemedyItem(
            pestKey = "army_fallworm",
            pestName = "Army Fallworm",
            badgeCritical = true,
            remedyName = "Neem Oil Spray",
            effectiveness = "85%",
            cost = "Low",
            ingredients = "Neem oil, Water, Liquid soap",
            steps = "Mix 2 tbsp neem oil with 1L water, add soap, spray on whorls and leaves."
        ),
        RemedyItem(
            pestKey = "paddy_grasshopper",
            pestName = "Paddy Grasshopper",
            badgeCritical = false,
            remedyName = "Garlic-Chili Spray",
            effectiveness = "78%",
            cost = "Very Low",
            ingredients = "Garlic, Hot chili, Water",
            steps = "Blend garlic and chili with water, strain, spray on paddy leaves early morning."
        ),
        RemedyItem(
            pestKey = "field_slug",
            pestName = "Field Slug",
            badgeCritical = false,
            remedyName = "Beer Traps & Diatomaceous Earth",
            effectiveness = "75%",
            cost = "Low",
            ingredients = "Beer, shallow dishes, Diatomaceous earth",
            steps = "Place beer in shallow dishes near plants; dust diatomaceous earth around base. Reapply after rain."
        ),
        RemedyItem(
            pestKey = "grain_weevil",
            pestName = "Grain Weevil",
            badgeCritical = true,
            remedyName = "Hermetic Storage & Neem",
            effectiveness = "88%",
            cost = "Medium",
            ingredients = "Hermetic bags, Neem leaves or neem oil",
            steps = "Store grain in hermetic bags. Add neem leaves or apply neem oil to storage area before filling."
        )
    )

    /** Exactly 4 supported pest keys. */
    val SUPPORTED_PEST_KEYS = setOf("army_fallworm", "paddy_grasshopper", "field_slug", "grain_weevil")

    fun all(): List<RemedyItem> = remedies

    fun findByKey(pestKey: String): RemedyItem? =
        remedies.find { it.pestKey.equals(pestKey, ignoreCase = true) }

    fun isSupportedPest(pestKey: String): Boolean =
        pestKey.lowercase().trim() in SUPPORTED_PEST_KEYS

    /** Maps a scan/history pest name to pestKey. Only the 4 supported pests map to a key; others return as-is for fallback handling. */
    fun normalizePestKey(pestName: String): String {
        val key = pestName.replace(" ", "_").lowercase().trim()
        return if (key in SUPPORTED_PEST_KEYS) key else pestName
    }
}
