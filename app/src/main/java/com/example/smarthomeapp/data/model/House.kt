package com.example.smarthomeapp.data.model

import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties

/** A house at `/houses/{houseId}`. [members] maps a uid to "owner" or "member". */
@IgnoreExtraProperties
data class House(
    @get:Exclude var id: String = "",
    val name: String = "",
    val ownerUid: String = "",
    val members: Map<String, String> = emptyMap(),
) {
    @Exclude
    fun isOwner(uid: String): Boolean = ownerUid == uid
}
