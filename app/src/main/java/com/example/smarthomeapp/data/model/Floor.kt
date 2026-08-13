package com.example.smarthomeapp.data.model

import com.example.smarthomeapp.utils.Constants
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties


/**
 * Floor created by user.
 * Rooms are drawn manually using FloorPlanEditor.
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


    /**
     * Empty grid where user draws rooms manually
     */
    val gridCols: Int = Constants.DEFAULT_GRID_COLS,

    val gridRows: Int = Constants.DEFAULT_GRID_ROWS,


    /**
     * Rooms created by user
     */
    val rooms: List<Room> = emptyList()

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