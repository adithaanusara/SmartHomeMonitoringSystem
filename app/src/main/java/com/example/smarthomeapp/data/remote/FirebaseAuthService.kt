package com.example.smarthomeapp.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Email/password authentication.
 *
 * Requires the Email/Password provider to be enabled under Authentication → Sign-in method in the
 * Firebase console; without it every call fails with CONFIGURATION_NOT_FOUND.
 */
class FirebaseAuthService(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    val currentUid: String? get() = auth.currentUser?.uid

    /**
     * Emits the signed-in user, or null when signed out, and re-emits on every change.
     *
     * Navigation observes this rather than checking [currentUser] once at startup, so a token
     * expiry or a sign-out from another screen routes back to login on its own.
     */
    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = runCatching {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        result.user ?: error("Sign-in succeeded but returned no user")
    }

    /**
     * Creates the account and sets the display name before returning, so the caller can write a
     * complete `/users/{uid}` profile in one step.
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<FirebaseUser> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: error("Sign-up succeeded but returned no user")
        if (displayName.isNotBlank()) {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
            ).await()
        }
        user
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    fun signOut() = auth.signOut()
}
