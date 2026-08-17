package com.example.smarthomeapp.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * A room the user drew on a floor, stored at `/floors/{houseId}/{floorId}/rooms`.
 *
 * **All four geometry values are fractions of the plan area, 0..1** — not pixels. The editor
 * canvas and the floor screen are different sizes on every device, so storing raw touch
 * coordinates would put a room somewhere else on every screen it was opened on. Fractions also
 * share a coordinate space with a device's `gridX`/`gridY`: a device at grid cell (x, y) sits
 * inside this room when `x / gridCols` falls between [x] and [x] + [width].
 */
@IgnoreExtraProperties
data class Room(

    val id: String = "",

    val name: String = "",

    /** Left edge as a fraction of plan width, 0..1. */
    val x: Float = 0f,

    /** Top edge as a fraction of plan height, 0..1. */
    val y: Float = 0f,

    /** Width as a fraction of plan width, 0..1. */
    val width: Float = 0f,

    /** Height as a fraction of plan height, 0..1. */
    val height: Float = 0f,
)
