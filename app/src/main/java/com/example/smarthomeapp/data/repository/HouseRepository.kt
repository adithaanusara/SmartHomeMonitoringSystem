package com.example.smarthomeapp.data.repository

import com.example.smarthomeapp.data.model.Floor
import com.example.smarthomeapp.data.model.House
import com.example.smarthomeapp.data.model.Room
import com.example.smarthomeapp.data.remote.FirebaseDatabaseService
import com.example.smarthomeapp.data.remote.observeObject
import com.example.smarthomeapp.data.remote.setValueSuspend
import com.example.smarthomeapp.data.remote.valueEvents
import com.example.smarthomeapp.utils.Constants
import com.google.firebase.database.DataSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes `/houses` and `/floors`. */
class HouseRepository(
    private val db: FirebaseDatabaseService = FirebaseDatabaseService(),
) {
    fun observeHouse(houseId: String): Flow<House?> =
        db.house(houseId).observeObject(House::class.java) { house, key -> house.id = key }

    fun observeFloors(houseId: String): Flow<List<Floor>> =
        db.floors(houseId).valueEvents()
            .map { snapshot ->
                snapshot.children
                    .mapNotNull { it.toFloorOrNull() }
                    .sortedBy { it.level }
            }

    fun observeFloor(houseId: String, floorId: String): Flow<Floor?> =
        db.floor(houseId, floorId).valueEvents().map { it.toFloorOrNull() }

    /**
     * Creates a house and indexes it under its owner in one atomic write, so a house can never
     * exist that no user can see.
     */
    suspend fun createHouse(name: String, ownerUid: String): String {
        val houseId = db.houses().push().key
            ?: error("Realtime Database returned no push key for a new house")
        val house = mapOf(
            "name" to name,
            "ownerUid" to ownerUid,
            "members" to mapOf(ownerUid to "owner"),
        )
        db.applyAtomicUpdate(
            mapOf(
                "/${Constants.PATH_HOUSES}/$houseId" to house,
                "/${Constants.PATH_USERS}/$ownerUid/houses/$houseId" to true,
            )
        )
        return houseId
    }

    suspend fun addFloor(houseId: String, floor: Floor): String {
        val ref = db.floors(houseId).push()
        val floorId = ref.key ?: error("Realtime Database returned no push key for a new floor")
        ref.setValueSuspend(floor)
        return floorId
    }

    suspend fun updateFloor(houseId: String, floor: Floor) {
        db.floor(houseId, floor.id).setValueSuspend(floor)
    }

    /**
     * Writes only the `rooms` child rather than the whole floor.
     *
     * Saving the plan must not depend on the client holding a complete, current copy of every
     * other field on the floor — a full `setValue` would silently drop anything this build of the
     * app does not know about, which is exactly how a schema addition by one teammate gets erased
     * by another teammate's screen.
     */
    suspend fun updateFloorRooms(houseId: String, floorId: String, rooms: Map<String, Room>) {
        db.floor(houseId, floorId).child(FLOOR_CHILD_ROOMS).setValueSuspend(rooms)
    }

    /**
     * Removes a floor and every device on it in one write. Devices are keyed by house rather than
     * by floor, so they would otherwise be orphaned onto a plan that no longer exists.
     */
    suspend fun deleteFloor(houseId: String, floorId: String, deviceIdsOnFloor: List<String>) {
        val updates = mutableMapOf<String, Any?>(
            "/${Constants.PATH_FLOORS}/$houseId/$floorId" to null,
        )
        deviceIdsOnFloor.forEach { deviceId ->
            updates["/${Constants.PATH_DEVICES}/$houseId/$deviceId"] = null
        }
        db.applyAtomicUpdate(updates)
    }
}

private const val FLOOR_CHILD_ROOMS = "rooms"

/**
 * Deserialises one floor, treating its `rooms` node as untrusted input.
 *
 * `Floor.rooms` is excluded from the Firebase mapper, so a malformed rooms node can no longer
 * abort the floor itself. Rooms are parsed here instead, one at a time, and anything unusable is
 * dropped — three programs write to this database and two of them are not this build of this app.
 */
private fun DataSnapshot.toFloorOrNull(): Floor? {
    val floor = runCatching { getValue(Floor::class.java) }.getOrNull() ?: return null
    floor.id = key.orEmpty()
    floor.rooms = parseRooms()
    return floor
}

/**
 * The rooms that survive parsing, keyed by id.
 *
 * Two failures are tolerated rather than fatal: a room that will not deserialise at all, and one
 * that deserialises but carries geometry outside 0..1 — which is what an older build's raw pixel
 * coordinates look like. Both are skipped, leaving the rest of the floor intact.
 */
private fun DataSnapshot.parseRooms(): Map<String, Room> =
    child(FLOOR_CHILD_ROOMS).children.mapNotNull { node ->
        val id = node.key ?: return@mapNotNull null
        val room = runCatching { node.getValue(Room::class.java) }.getOrNull()
            ?: return@mapNotNull null
        if (room.hasRenderableGeometry) id to room else null
    }.toMap()
