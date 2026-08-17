package com.example.smarthomeapp.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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

/**
 * Opacity for a plan drawable in the current theme.
 *
 * The bundled plans are light-on-white vectors, so on a dark background they render as a glaring
 * white slab that outshines the device markers sitting on top of them — the one thing the screen
 * exists to show. Knocking them back in dark mode turns the plan into the backdrop it is meant to
 * be, while leaving the walls clearly readable. Dimming beats shipping a second night-qualified
 * drawable for each plan, which would double the assets to keep in sync.
 */
@Composable
@ReadOnlyComposable
fun floorPlanAlpha(): Float = if (isSystemInDarkTheme()) 0.45f else 1f

/** "plan_ground_floor" -> "ground floor", for chips and labels. */
fun floorPlanLabel(assetName: String): String =
    assetName.removePrefix("plan_").replace('_', ' ')
