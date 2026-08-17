package com.example.smarthomeapp

import com.example.smarthomeapp.data.model.Floor
import com.example.smarthomeapp.data.model.Room
import com.google.firebase.database.core.utilities.encoding.CustomClassMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the failure that took two floors off the dashboard once already.
 *
 * `HouseRepository.observeFloors` skips any child that fails to deserialise, so one bad field
 * inside a floor used to hide the floor *and its devices* with no error anywhere. Rooms were
 * briefly stored as an array of pixel coordinates by an earlier build; the current model wants a
 * map of 0..1 fractions.
 *
 * `convertToCustomClass` is what `DataSnapshot.getValue(Class)` calls internally, so these
 * assertions exercise the real Firebase mapper rather than a stand-in.
 */
class FloorRoomsDeserialisationTest {

    private fun floorSnapshot(rooms: Any?): Map<String, Any?> = buildMap {
        put("name", "Ground Floor")
        put("level", 0L)
        put("planImageAsset", "plan_ground_floor")
        put("gridCols", 8L)
        put("gridRows", 6L)
        if (rooms != null) put("rooms", rooms)
    }

    private fun convert(snapshot: Map<String, Any?>): Floor? =
        runCatching { CustomClassMapper.convertToCustomClass(snapshot, Floor::class.java) }
            .getOrNull()

    /** The shape that used to be fatal: rooms as an array, written by an older build. */
    @Test
    fun `legacy array rooms no longer abort the floor`() {
        val legacy = listOf(
            mapOf(
                "id" to "1786625157077",
                "name" to "1",
                "x" to 109.2470703125,
                "y" to 63.2744140625,
                "width" to 471.7109375,
                "height" to 415.6787109375,
            ),
        )

        val floor = convert(floorSnapshot(legacy))

        assertNotNull("A legacy rooms array must not take the whole floor down", floor)
        assertEquals("Ground Floor", floor!!.name)
        assertEquals(8, floor.gridCols)
    }

    @Test
    fun `a floor with no rooms node parses`() {
        val floor = convert(floorSnapshot(rooms = null))

        assertNotNull(floor)
        assertTrue(floor!!.rooms.isEmpty())
    }

    /**
     * `rooms` is excluded from the mapper and populated by the repository, so the mapper leaves it
     * empty even when the snapshot carries a well-formed map. The repository's own parsing is what
     * fills it in; this pins the exclusion so nobody re-adds it to the mapper by accident.
     */
    @Test
    fun `mapper leaves rooms to the repository`() {
        val wellFormed = mapOf(
            "room-1" to mapOf(
                "name" to "Kitchen",
                "x" to 0.11,
                "y" to 0.057,
                "width" to 0.474,
                "height" to 0.377,
            ),
        )

        val floor = convert(floorSnapshot(wellFormed))

        assertNotNull(floor)
        assertTrue(floor!!.rooms.isEmpty())
    }

    @Test
    fun `pixel geometry is not renderable`() {
        val legacy = Room(name = "1", x = 109.2f, y = 63.2f, width = 471.7f, height = 415.6f)

        assertFalse(legacy.hasRenderableGeometry)
    }

    @Test
    fun `fractional geometry is renderable`() {
        val current = Room(name = "Kitchen", x = 0.11f, y = 0.057f, width = 0.474f, height = 0.377f)

        assertTrue(current.hasRenderableGeometry)
    }

    @Test
    fun `a room running past the right edge is not renderable`() {
        val overflowing = Room(name = "Wide", x = 0.8f, y = 0.1f, width = 0.5f, height = 0.2f)

        assertFalse(overflowing.hasRenderableGeometry)
    }

    @Test
    fun `a zero-area room is not renderable`() {
        val degenerate = Room(name = "Nothing", x = 0.2f, y = 0.2f, width = 0f, height = 0f)

        assertFalse(degenerate.hasRenderableGeometry)
    }

    /** A room filling the whole plan is legitimate, and float division must not exclude it. */
    @Test
    fun `a full-plan room is renderable`() {
        val full = Room(name = "Studio", x = 0f, y = 0f, width = 1f, height = 1f)

        assertTrue(full.hasRenderableGeometry)
    }
}
