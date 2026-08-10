package com.example.smarthomeapp.data.repository

import com.example.smarthomeapp.data.model.User
import com.example.smarthomeapp.data.remote.FirebaseDatabaseService
import com.example.smarthomeapp.data.remote.observeObject
import com.example.smarthomeapp.data.remote.setValueSuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/** Reads and writes `/users`. */
class UserRepository(
    private val db: FirebaseDatabaseService = FirebaseDatabaseService(),
) {
    fun observeUser(uid: String): Flow<User?> =
        db.user(uid).observeObject(User::class.java) { user, key -> user.id = key }

    /**
     * Writes the profile fields for a newly signed-up user without touching `houses`.
     *
     * Deliberately not a whole-object `setValue`: this runs on every sign-in as well as sign-up,
     * and a full write would wipe the membership index of a returning user.
     */
    suspend fun upsertProfile(uid: String, email: String, displayName: String) {
        db.user(uid).updateChildren(
            mapOf(
                "email" to email,
                "displayName" to displayName,
            )
        ).await()
    }

    suspend fun setDisplayName(uid: String, displayName: String) {
        db.user(uid).child("displayName").setValueSuspend(displayName)
    }
}
