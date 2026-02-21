package com.avikshit.PestAI.ui

/**
 * Hardcoded remedy data for the 4 supported pests. Used by RemediesFragment and RemedyDetailFragment.
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
            pestKey = "fall_armyworm",
            pestName = "Fall Armyworm",
            badgeCritical = true,
            remedyName = "Neem Oil Spray",
            effectiveness = "85%",
            cost = "Low",
            ingredients = "Neem oil, Water, Liquid soap",
            steps = "Mix 2 tbsp neem oil with 1L water, add soap, spray on whorls."
        ),
        RemedyItem(
            pestKey = "aphids",
            pestName = "Aphids",
            badgeCritical = false,
            remedyName = "Garlic-Chili Spray",
            effectiveness = "78%",
            cost = "Very Low",
            ingredients = "Garlic, Hot chili, Water",
            steps = "Blend garlic and chili with water, strain, spray on leaves."
        ),
        RemedyItem(
            pestKey = "leafhoppers",
            pestName = "Leafhoppers",
            badgeCritical = false,
            remedyName = "Wood Ash",
            effectiveness = "70%",
            cost = "Free",
            ingredients = "Fine wood ash",
            steps = "Dust fine wood ash evenly over the plant leaves early in the morning."
        ),
        RemedyItem(
            pestKey = "stem_borer",
            pestName = "Stem Borer",
            badgeCritical = true,
            remedyName = "Bacillus thuringiensis (Bt)",
            effectiveness = "90%",
            cost = "Medium",
            ingredients = "Bt powder, Water",
            steps = "Mix according to package, spray at the base and stems."
        )
    )

    fun all(): List<RemedyItem> = remedies

    fun findByKey(pestKey: String): RemedyItem? =
        remedies.find { it.pestKey.equals(pestKey, ignoreCase = true) }

    /** Maps a scan/history pest name to a known pestKey, or returns raw name for generic handling. */
    fun normalizePestKey(pestName: String): String {
        val lower = pestName.lowercase().trim()
        return when {
            lower.contains("armyworm") || lower.contains("fall armyworm") -> "fall_armyworm"
            lower.contains("aphid") -> "aphids"
            lower.contains("leafhopper") -> "leafhoppers"
            lower.contains("stem borer") || (lower.contains("borer") && !lower.contains("army")) -> "stem_borer"
            else -> pestName
        }
    }
}
