package com.example.smarthomeapp.ui.screens

import androidx.annotation.DrawableRes
import com.example.smarthomeapp.R

/**
 * Maps the `planImageAsset` string stored in the database to a bundled drawable.
 *
 * An explicit map rather than `Resources.getIdentifier`: reflection by name is slow, invisible to
 * R8 (so the drawables would need keep rules to survive shrinking), and fails at runtime rather
 * than at compile time when a plan is renamed.
 *
 * To add a plan: drop the vector into res/drawable and add one line here.
 */
private val FLOOR_PLANS: Map<String, Int> = mapOf(
    "plan_ground_floor" to R.drawable.plan_ground_floor,
    "plan_first_floor" to R.drawable.plan_first_floor,
)

/**
 * The drawable for a plan name, or null when the floor has no background image.
 *
 * Null rather than a fallback plan: since floors can be drawn room by room in the editor, "no
 * plan image" is a legitimate state, and substituting some other floor's plan behind a
 * hand-drawn layout would be actively misleading.
 */
@DrawableRes
fun floorPlanResource(assetName: String?): Int? =
    assetName?.takeIf { it.isNotBlank() }?.let { FLOOR_PLANS[it] }

/** Names offered when adding a floor. */
val AVAILABLE_FLOOR_PLANS: List<String> = FLOOR_PLANS.keys.toList()

/** "plan_ground_floor" -> "ground floor", for chips and labels. */
fun floorPlanLabel(assetName: String): String =
    assetName.removePrefix("plan_").replace('_', ' ')
