package com.example.smarthomeapp.data.model

import com.example.smarthomeapp.utils.Constants
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties


/**
 * A floor plan at `/floors/{houseId}/{floorId}`.
 *
 * A floor is represented two ways, and they layer rather than compete:
 *
 * - [planImageAsset] is a bundled drawable name rather than a Storage URL — the spec permits free
 *   sample plans, and shipping them in the APK removes an upload flow and a second set of security
 *   rules for no marks. Blank means the floor has no background image, which is the normal state
 *   for a floor the user drew themselves.
 * - [rooms] are drawn by the user in the floor plan editor and painted over that background.
 *
 * Either may be empty. A floor with neither is just an empty grid, which is what a newly added
 * floor looks like until someone draws on it.
 */
@IgnoreExtraProperties
data class Floor(

    @get:Exclude
    var id: String = "",

    val name: String = "",

    /**
     * Floor order
     * 0 = Ground floor
     * 1 = First floor
     */
    val level: Int = 0,

    /** Bundled drawable name, e.g. "plan_ground_floor". Blank when the floor has no plan image. */
    val planImageAsset: String = "",

    val gridCols: Int = Constants.DEFAULT_GRID_COLS,

    val gridRows: Int = Constants.DEFAULT_GRID_ROWS,

    /**
     * Rooms drawn by the user, keyed by room id. See [Room] — geometry is stored as fractions of
     * the plan area, so it survives being rendered at three different sizes.
     *
     * Excluded from the Firebase mapper and populated by `HouseRepository` instead, for the same
     * reason [id] is: the mapper aborts the *entire* object when one field does not match its
     * declared type, and `observeChildren` then skips that child. A single malformed room —
     * written by an older build, or by a teammate still running an older APK — would take the
     * whole floor and its devices off the dashboard with no error anywhere. Parsing it separately
     * degrades that to "the floor renders, the bad rooms do not".
     *
     * Because it is excluded, writing a whole [Floor] never persists rooms; use
     * `HouseRepository.updateFloorRooms`.
     */
    @get:Exclude
    var rooms: Map<String, Room> = emptyMap(),

) {


    @get:Exclude
    val cellCount: Int
        get() = gridCols * gridRows



    @Exclude
    fun containsCell(
        x: Int,
        y: Int
    ): Boolean {

        return x in 0 until gridCols &&
                y in 0 until gridRows
    }

}
