package com.example.smarthomeapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smarthomeapp.viewmodel.AuthFormState
import com.example.smarthomeapp.viewmodel.AuthMode
import com.example.smarthomeapp.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    val form by viewModel.form.collectAsStateWithLifecycle()

    LoginContent(
        form = form,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onToggleMode = viewModel::toggleMode,
        onSubmit = viewModel::submit,
        onForgotPassword = viewModel::sendPasswordReset,
        modifier = modifier,
    )
}

/** Stateless so it can be previewed and tested without a Firebase-backed ViewModel. */
@Composable
private fun LoginContent(
    form: AuthFormState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val isSignUp = form.mode == AuthMode.SIGN_UP

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Smart Home",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (isSignUp) "Create your account" else "Monitoring & Control",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp),
            )

            Spacer(Modifier.height(32.dp))

            Column(
                modifier = Modifier.widthIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isSignUp) {
                    OutlinedTextField(
                        value = form.displayName,
                        onValueChange = onDisplayNameChange,
                        label = { Text("Name") },
                        singleLine = true,
                        isError = form.displayNameError != null,
                        supportingText = form.displayNameError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        enabled = !form.isSubmitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = form.email,
                    onValueChange = onEmailChange,
                    label = { Text("Email") },
                    singleLine = true,
                    isError = form.emailError != null,
                    supportingText = form.emailError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    enabled = !form.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = form.password,
                    onValueChange = onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    isError = form.passwordError != null,
                    supportingText = form.passwordError?.let { { Text(it) } },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        PasswordVisibilityToggle(
                            visible = passwordVisible,
                            onToggle = { passwordVisible = !passwordVisible },
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            onSubmit()
                        },
                    ),
                    enabled = !form.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )

                form.errorMessage?.let { MessageCard(it, isError = true) }
                form.infoMessage?.let { MessageCard(it, isError = false) }

                Button(
                    onClick = {
                        keyboard?.hide()
                        onSubmit()
                    },
                    enabled = !form.isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (form.isSubmitting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (isSignUp) "Create account" else "Sign in")
                    }
                }

                if (!isSignUp) {
                    TextButton(
                        onClick = onForgotPassword,
                        enabled = !form.isSubmitting,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text("Forgot password?")
                    }
                }

                TextButton(
                    onClick = onToggleMode,
                    enabled = !form.isSubmitting,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(
                        if (isSignUp) {
                            "Already have an account? Sign in"
                        } else {
                            "New here? Create an account"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    val icon: ImageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = icon,
            contentDescription = if (visible) "Hide password" else "Show password",
        )
    }
}

@Composable
private fun MessageCard(message: String, isError: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            },
            modifier = Modifier.padding(12.dp),
        )
    }
}
