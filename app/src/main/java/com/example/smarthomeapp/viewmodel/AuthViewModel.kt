package com.example.smarthomeapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthomeapp.data.remote.FirebaseAuthService
import com.example.smarthomeapp.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode { SIGN_IN, SIGN_UP }

/** Whether the app should show login, the dashboard, or neither while auth is resolving. */
sealed interface AuthStatus {
    /** Firebase has not yet reported a state. Avoids flashing login at an already-signed-in user. */
    data object Loading : AuthStatus

    data object SignedOut : AuthStatus

    data class SignedIn(val uid: String, val displayName: String) : AuthStatus
}

data class AuthFormState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val mode: AuthMode = AuthMode.SIGN_IN,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    /** Field errors stay hidden until the first submit, so the form is not red on arrival. */
    val showValidation: Boolean = false,
) {
    val emailError: String?
        get() = when {
            !showValidation -> null
            email.isBlank() -> "Email is required"
            !EMAIL_PATTERN.matches(email.trim()) -> "Enter a valid email address"
            else -> null
        }

    val passwordError: String?
        get() = when {
            !showValidation -> null
            password.isEmpty() -> "Password is required"
            password.length < MIN_PASSWORD_LENGTH -> "At least $MIN_PASSWORD_LENGTH characters"
            else -> null
        }

    val displayNameError: String?
        get() = when {
            !showValidation || mode != AuthMode.SIGN_UP -> null
            displayName.isBlank() -> "Name is required"
            else -> null
        }

    val isValid: Boolean
        get() = EMAIL_PATTERN.matches(email.trim()) &&
            password.length >= MIN_PASSWORD_LENGTH &&
            (mode == AuthMode.SIGN_IN || displayName.isNotBlank())

    private companion object {
        /** Deliberately not android.util.Patterns, which would make this state untestable on the JVM. */
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$")
        const val MIN_PASSWORD_LENGTH = 6
    }
}

class AuthViewModel(
    private val authService: FirebaseAuthService = FirebaseAuthService(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _form = MutableStateFlow(AuthFormState())
    val form: StateFlow<AuthFormState> = _form.asStateFlow()

    /**
     * Derived from Firebase's own auth-state listener rather than a one-time `currentUser` read, so
     * a token expiry or a sign-out triggered elsewhere routes back to login on its own.
     */
    val authStatus: StateFlow<AuthStatus> = authService.authState()
        .map { user ->
            if (user == null) {
                AuthStatus.SignedOut
            } else {
                AuthStatus.SignedIn(
                    uid = user.uid,
                    displayName = user.displayName?.takeIf { it.isNotBlank() }
                        ?: user.email.orEmpty(),
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = AuthStatus.Loading,
        )

    fun onEmailChange(value: String) = _form.update { it.copy(email = value, errorMessage = null) }

    fun onPasswordChange(value: String) =
        _form.update { it.copy(password = value, errorMessage = null) }

    fun onDisplayNameChange(value: String) =
        _form.update { it.copy(displayName = value, errorMessage = null) }

    fun toggleMode() = _form.update {
        it.copy(
            mode = if (it.mode == AuthMode.SIGN_IN) AuthMode.SIGN_UP else AuthMode.SIGN_IN,
            errorMessage = null,
            infoMessage = null,
            showValidation = false,
        )
    }

    fun dismissMessages() = _form.update { it.copy(errorMessage = null, infoMessage = null) }

    fun submit() {
        val current = _form.value
        if (current.isSubmitting) return
        if (!current.isValid) {
            _form.update { it.copy(showValidation = true) }
            return
        }

        _form.update { it.copy(isSubmitting = true, errorMessage = null, infoMessage = null) }

        viewModelScope.launch {
            val result = when (current.mode) {
                AuthMode.SIGN_IN -> authService.signIn(current.email, current.password)
                AuthMode.SIGN_UP -> authService.signUp(
                    email = current.email,
                    password = current.password,
                    displayName = current.displayName,
                )
            }

            result
                .onSuccess { user ->
                    syncProfile(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        displayName = user.displayName ?: current.displayName,
                    )
                    // authStatus drives navigation, so there is nothing to do here but stop the spinner.
                    _form.update { it.copy(isSubmitting = false) }
                }
                .onFailure { error ->
                    _form.update {
                        it.copy(isSubmitting = false, errorMessage = error.toFriendlyMessage())
                    }
                }
        }
    }

    fun sendPasswordReset() {
        val email = _form.value.email
        if (email.isBlank()) {
            _form.update { it.copy(showValidation = true, errorMessage = "Enter your email first") }
            return
        }
        viewModelScope.launch {
            authService.sendPasswordReset(email)
                .onSuccess {
                    _form.update { it.copy(infoMessage = "Password reset email sent to $email") }
                }
                .onFailure { error ->
                    _form.update { it.copy(errorMessage = error.toFriendlyMessage()) }
                }
        }
    }

    fun signOut() {
        authService.signOut()
        _form.value = AuthFormState()
    }

    /**
     * Mirrors the account into `/users/{uid}`.
     *
     * A failure here is logged rather than surfaced: the user is already authenticated, and
     * blocking them at the login screen over a profile write they cannot fix would be worse than
     * a profile that syncs on the next sign-in. This is also the path that fails while the
     * Realtime Database has not been created yet.
     */
    private suspend fun syncProfile(uid: String, email: String, displayName: String) {
        runCatching { userRepository.upsertProfile(uid, email, displayName) }
            .onFailure { Log.w(TAG, "Could not write /users/$uid profile", it) }
    }

    private companion object {
        const val TAG = "AuthViewModel"
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Firebase's raw messages are developer-facing; these are the ones a user can act on. */
private fun Throwable.toFriendlyMessage(): String = when {
    this is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
    this is FirebaseAuthUserCollisionException -> "An account with this email already exists."
    this is FirebaseAuthInvalidUserException -> "No account found for this email."
    this is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
    message?.contains("CONFIGURATION_NOT_FOUND") == true ->
        "Email/password sign-in is not enabled for this Firebase project yet."
    message?.contains("network", ignoreCase = true) == true ->
        "No connection. Check your network and try again."
    else -> message ?: "Something went wrong. Please try again."
}
