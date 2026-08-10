package com.example.smarthomeapp.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/**
 * A user profile at `/users/{uid}`.
 *
 * [houses] is a membership index rather than a list so the security rules can check
 * `root.child('users').child(auth.uid).child('houses').child($houseId).exists()` in constant time.
 */
@IgnoreExtraProperties
data class User(
    @get:Exclude var id: String = "",
    val displayName: String = "",
    val email: String = "",
    val houses: Map<String, Boolean> = emptyMap(),
) {
    @get:Exclude
    val houseIds: List<String> get() = houses.filterValues { it }.keys.toList()
}
