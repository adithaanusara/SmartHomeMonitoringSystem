package com.example.smarthomeapp.data.model

import com.google.firebase.database.IgnoreExtraProperties

/**
 * A room the user drew, stored under `/floors/{houseId}/{floorId}/rooms/{roomId}`.
 *
 * Rooms are keyed by id in a map rather than held in a list, matching `Device.channels`: a list is
 * stored as a Firebase array, so deleting anything but the last room leaves a null hole that every
 * reader then has to skip.
 *
 * The room carries no `id` of its own — the map key is the id, the same arrangement
 * [DeviceChannel] uses.
 *
 * **All four geometry values are fractions of the plan area, 0..1** — not pixels. The editor
 * canvas, the dashboard thumbnail and the floor screen are three different sizes, so raw touch
 * coordinates would put a room somewhere different on each, and different again on another phone.
 * Fractions also share a coordinate space with a device's `gridX`/`gridY`: a device in grid cell
 * (x, y) sits inside this room when `x / gridCols` falls between [x] and [x] + [width].
 */
@IgnoreExtraProperties
data class Room(

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
