package com.example.smarthomeapp.data.repository

import com.example.smarthomeapp.data.model.Floor
import com.example.smarthomeapp.data.model.House
import com.example.smarthomeapp.data.remote.FirebaseDatabaseService
import com.example.smarthomeapp.data.remote.observeChildren
import com.example.smarthomeapp.data.remote.observeObject
import com.example.smarthomeapp.data.remote.setValueSuspend
import com.example.smarthomeapp.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes `/houses` and `/floors`. */
class HouseRepository(
    private val db: FirebaseDatabaseService = FirebaseDatabaseService(),
) {
    fun observeHouse(houseId: String): Flow<House?> =
        db.house(houseId).observeObject(House::class.java) { house, key -> house.id = key }

    fun observeFloors(houseId: String): Flow<List<Floor>> =
        db.floors(houseId)
            .observeChildren(Floor::class.java) { floor, key -> floor.id = key }
            .map { floors -> floors.sortedBy { it.level } }

    fun observeFloor(houseId: String, floorId: String): Flow<Floor?> =
        db.floor(houseId, floorId).observeObject(Floor::class.java) { floor, key -> floor.id = key }

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
