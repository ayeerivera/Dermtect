package com.example.dermtect.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dermtect.R
import com.example.dermtect.ui.components.BubblesBackground
import com.example.dermtect.ui.components.InputField
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dermtect.ui.viewmodel.AuthViewModel
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.runtime.LaunchedEffect
import android.widget.Toast
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.components.DialogTemplate
import com.example.dermtect.ui.components.GifImage
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import com.example.dermtect.data.repository.AuthRepositoryImpl
import com.example.dermtect.domain.usecase.AuthUseCase
import androidx.activity.compose.BackHandler


@Composable
fun Login(navController: NavController, role: String) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showBackDialog by remember { mutableStateOf(false) }
    val isEmailValid = remember(email) { android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthUseCase(AuthRepositoryImpl()))
    )
    val authSuccess by viewModel.authSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    var showEmailNotVerifiedDialog by remember { mutableStateOf(false) }

    BubblesBackground {

        Scaffold(

            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        focusManager.clearFocus()
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp, start = 24.dp)
                ) {
                    BackButton(
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = {
                            if (email.isNotBlank() || password.isNotBlank()) {
                                showBackDialog = true
                            } else {
                                navController.popBackStack()
                            }
                        }
                    )

                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1D1D1D)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Email input
                    InputField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "Email",
                        iconRes = R.drawable.icon_email,
                        textColor = Color(0xFF1D1D1D)
                    )

                    if (email.isNotEmpty() && !isEmailValid) {
                        Text(
                            text = "Invalid email address",
                            color = Color.Red,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .padding(start = 48.dp, top = 4.dp)
                                .align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))


                    // Password input
                    InputField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "Password",
                        iconRes = R.drawable.icon_pass,
                        isPassword = true,
                        textColor = Color(0xFF1D1D1D)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Forgot Password
                    Box(
                        modifier = Modifier
                            .width(299.dp)
                            .padding(top = 8.dp)
                            .clickable { navController.navigate("forgot_pass1") },
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "Forgot Password?",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1D1D1D),
                            textDecoration = TextDecoration.Underline
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    val buttonEnabled = isEmailValid && password.isNotBlank()

                    Box(
                        modifier = Modifier
                            .width(320.dp)
                            .height(50.dp)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(12.dp),
                                clip = false
                            )
                            .background(
                                brush = if (buttonEnabled) {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF5FEAEA), // top lighter shade
                                            Color(0xFF2A9D9D), // bottom darker shade
                                            Color(0xFF187878)
                                        )
                                    )
                                } else {
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFBDBDBD), // top light grey
                                            Color(0xFF9E9E9E), // middle grey
                                            Color(0xFF757575)  // bottom dark grey
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = buttonEnabled) {
                                viewModel.login(
                                    email = email,
                                    password = password,
                                    onSuccess = { /* handled */ },
                                    onError = { error ->
                                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Login",
                            color = if (buttonEnabled) Color.White else Color.LightGray,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row {
                        Text(
                            text = "Don't have an account? ",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1D1D1D)
                        )
                        Text(
                            text = "Register",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF2FD8D8),
                            modifier = Modifier.clickable {
                                if (role == "patient") {
                                    navController.navigate("register_user")
                                } else {
                                    navController.navigate("derma_register")
                                }
                            }

                        )

                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Back to choose account",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF2FD8D8),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable {
                            navController.navigate("choose_account") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                }


                EmailNotVerifiedDialog(
                    show = showEmailNotVerifiedDialog,
                    viewModel = viewModel,
                    onClose = { showEmailNotVerifiedDialog = false }
                )
                // Back dialog
                if (showBackDialog) {
                    DialogTemplate(
                        show = showBackDialog,
                        title = "Go Back?",
                        description = "You’re about to leave this page. Any information you’ve entered will be lost.",
                        primaryText = "Yes, go back",
                        onPrimary = {
                            showBackDialog = false
                            navController.popBackStack()
                        },
                        secondaryText = "Cancel",
                        onSecondary = { showBackDialog = false },
                        onDismiss = { showBackDialog = false }
                    )
                }

// ✅ SINGLE LaunchedEffect for authSuccess (with popUpTo)
                LaunchedEffect(authSuccess) {
                    if (authSuccess) {
                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                        if (uid != null) {
                            val db = FirebaseFirestore.getInstance()
                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { document ->
                                    val roleFromDb = document.getString("role")
                                    when (roleFromDb) {
                                        "patient" -> navController.navigate("user_home") {
                                            popUpTo("login") { inclusive = true }
                                        }

                                        "derma" -> navController.navigate("derma_home") {
                                            popUpTo("login") { inclusive = true }
                                            launchSingleTop = true

                                        }

                                        else -> navController.navigate("user_home") {
                                            popUpTo("login") { inclusive = true }
                                            launchSingleTop = true

                                        }
                                    }
                                    viewModel.resetAuthSuccess()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        context,
                                        "Failed to fetch role",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                }
                LaunchedEffect(errorMessage) {
                    errorMessage?.let { message ->
                        // If it's the "verify email" case, show dialog instead of only toast
                        if (message.contains("verify your email", ignoreCase = true)) {
                            showEmailNotVerifiedDialog = true
                        } else {
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                        viewModel.clearError()
                    }
                }


// ✅ SINGLE loader dialog
                DialogTemplate(
                    show = isLoading,
                    title = "Logging you in",
                    description = "Please wait ...",
                    imageContent = {
                        GifImage(resId = R.drawable.loader, size = 150)
                    },
                    onDismiss = {},
                    autoDismiss = false
                )
            }
        }
    }
}
// In LoginScreen.kt

@Composable
fun EmailNotVerifiedDialog(
    show: Boolean,
    viewModel: AuthViewModel,
    onClose: () -> Unit // Note: onClose is used to close the dialog
) {
    val context = LocalContext.current
    var isSending by remember { mutableStateOf(false) }

    if (!show) return

    DialogTemplate(
        show = show,
        title = "Email Not Verified",
        description = "Your email is not verified yet. Please check your inbox. " +
                "If you didn’t receive anything, you can resend the verification email. " +
                "If you have already verified your email, click Check Status.", // 👈 Updated description

        primaryText = if (isSending) "Sending..." else "Resend Email",
        onPrimary = {
            if (!isSending) {
                isSending = true
                viewModel.resendVerificationEmail { success, error ->
                    isSending = false
                    if (success) {
                        // ... (Toast message for success)
                    } else {
                        // ... (Toast message for error)
                    }
                }
            }
        },

        // 🚀 CRITICAL CHANGE: Use the secondary action to reload and refresh the state
        secondaryText = "Check Status", // 👈 New button text
        onSecondary = {
            // This calls the function that executes user.reload()
            // and re-evaluates the AuthUiState.
            viewModel.reloadAndRefresh()
            onClose() // Close the dialog
        },

        onDismiss = { onClose() }
    )
}