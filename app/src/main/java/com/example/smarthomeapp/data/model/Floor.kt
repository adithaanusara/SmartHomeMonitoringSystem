package com.example.smarthomeapp.data.model

import com.example.smarthomeapp.utils.Constants
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * A floor plan at `/floors/{houseId}/{floorId}`.
 *
 * [planImageAsset] is a bundled drawable name rather than a Storage URL — the spec permits free
 * sample plans, and shipping them in the APK removes an upload flow and a second set of security
 * rules for no marks.
 */
@IgnoreExtraProperties
data class Floor(
    @get:Exclude var id: String = "",
    val name: String = "",
    /** Sort order: 0 = ground, 1 = first, and so on. */
    val level: Int = 0,
    val planImageAsset: String = "",
    val gridCols: Int = Constants.DEFAULT_GRID_COLS,
    val gridRows: Int = Constants.DEFAULT_GRID_ROWS,
) {
    @get:Exclude
    val cellCount: Int get() = gridCols * gridRows

    @Exclude
    fun containsCell(x: Int, y: Int): Boolean = x in 0 until gridCols && y in 0 until gridRows
}
