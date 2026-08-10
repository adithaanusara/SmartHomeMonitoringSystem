package com.example.smarthomeapp.data.remote

import com.example.smarthomeapp.utils.Constants
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper over the Realtime Database: path builders plus the listener-to-[Flow] bridge that
 * every repository is built on.
 *
 * This bridge is the whole of the spec's bidirectional-sync requirement. A `ValueEventListener`
 * fires on any change regardless of origin — this app, the web simulator, or the backend worker —
 * so screens collecting these flows update without a manual refresh.
 */
class FirebaseDatabaseService(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(),
) {
    private val root: DatabaseReference get() = db.reference

    fun users(): DatabaseReference = root.child(Constants.PATH_USERS)

    fun user(uid: String): DatabaseReference = users().child(uid)

    fun houses(): DatabaseReference = root.child(Constants.PATH_HOUSES)

    fun house(houseId: String): DatabaseReference = houses().child(houseId)

    fun floors(houseId: String): DatabaseReference =
        root.child(Constants.PATH_FLOORS).child(houseId)

    fun floor(houseId: String, floorId: String): DatabaseReference =
        floors(houseId).child(floorId)

    fun devices(houseId: String): DatabaseReference =
        root.child(Constants.PATH_DEVICES).child(houseId)

    fun device(houseId: String, deviceId: String): DatabaseReference =
        devices(houseId).child(deviceId)

    fun events(houseId: String): DatabaseReference =
        root.child(Constants.PATH_EVENTS).child(houseId)

    fun deviceEvents(houseId: String, deviceId: String): DatabaseReference =
        events(houseId).child(deviceId)

    fun alerts(houseId: String): DatabaseReference =
        root.child(Constants.PATH_ALERTS).child(houseId)

    /** Server timestamp sentinel, so ordering does not depend on correct device clocks. */
    fun serverTimestamp(): Map<String, String> = com.google.firebase.database.ServerValue.TIMESTAMP

    /**
     * Applies a set of absolute, root-relative paths in one atomic write.
     *
     * Used wherever a single user action has to touch several nodes — toggling a hazard device
     * writes its status, clears or sets `safety/onSince`, and appends an event. Doing that as one
     * update means the simulator can never observe a device that is ON with no cutoff armed.
     */
    suspend fun applyAtomicUpdate(updates: Map<String, Any?>) {
        root.updateChildren(updates).await()
    }

    /** Allocates a push key without writing, so the key can go into an atomic multi-path update. */
    fun newEventKey(houseId: String, deviceId: String): String =
        deviceEvents(houseId, deviceId).push().key
            ?: error("Realtime Database returned no push key for $houseId/$deviceId")
}

/** Suspending [DatabaseReference.setValue], so repositories read as straight-line code. */
suspend fun DatabaseReference.setValueSuspend(value: Any?) {
    setValue(value).await()
}

/**
 * Bridges a [DatabaseReference] to a cold [Flow] that re-emits on every remote change and detaches
 * its listener when the collector goes away.
 *
 * [callbackFlow] rather than a plain `flow {}` because the listener is a callback we must be able
 * to unregister; [awaitClose] is what guarantees that, and omitting it leaks the listener for the
 * lifetime of the process.
 */
fun DatabaseReference.valueEvents(): Flow<DataSnapshot> = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            trySend(snapshot)
        }

        override fun onCancelled(error: DatabaseError) {
            // Almost always a security-rules rejection. Surface it rather than hanging silently.
            close(error.toException())
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}

/** Observes a single object, emitting null when the node does not exist. */
fun <T : Any> DatabaseReference.observeObject(
    clazz: Class<T>,
    withId: (T, String) -> Unit,
): Flow<T?> = valueEvents().map { snapshot ->
    snapshot.getValue(clazz)?.also { withId(it, snapshot.key.orEmpty()) }
}

/**
 * Observes a node's children as a list, tagging each child with its own key.
 *
 * Children that fail to deserialise are skipped rather than failing the whole emission: the
 * simulator and worker write to this tree too, and one malformed node should not blank the
 * dashboard.
 */
fun <T : Any> DatabaseReference.observeChildren(
    clazz: Class<T>,
    withId: (T, String) -> Unit,
): Flow<List<T>> = valueEvents().map { snapshot ->
    snapshot.children.mapNotNull { child ->
        runCatching { child.getValue(clazz) }
            .getOrNull()
            ?.also { withId(it, child.key.orEmpty()) }
    }
}
