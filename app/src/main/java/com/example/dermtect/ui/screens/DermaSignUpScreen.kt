package com.example.dermtect.ui.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dermtect.R
import com.example.dermtect.data.repository.AuthRepositoryImpl
import com.example.dermtect.domain.usecase.AuthUseCase
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.components.BubblesBackground
import com.example.dermtect.ui.components.DialogTemplate
import com.example.dermtect.ui.components.InputField
import com.example.dermtect.ui.viewmodel.AuthViewModel
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun DermaRegister(
    navController: NavController
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthUseCase(AuthRepositoryImpl()))
    )

    // --- States ---
    var fnameTouched by remember { mutableStateOf(false) }
    var lnameTouched by remember { mutableStateOf(false) }
    var clinicTouched by remember { mutableStateOf(false) }
    var contactTouched by remember { mutableStateOf(false) }
    var credentialsTouched by remember { mutableStateOf(false) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var contactNumber by rememberSaveable { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var clinicName by rememberSaveable { mutableStateOf("") }
    var credentials by remember { mutableStateOf("") }

    var showBackDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // ViewModel states
    val loading by viewModel.loading.collectAsState()
    val authSuccess by viewModel.authSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // --- Validation ---
    val isEmailValid = email.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            !email.contains(" ")

    val isPasswordValid = remember(password, confirmPassword) {
        password == confirmPassword && viewModel.isPasswordStrong(password)
    }

    val digitsOnly = contactNumber.filter { it.isDigit() }
    val isContactValid = contactNumber.length == 10

    val isFormValid =
        firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                credentials.isNotBlank() &&   // ✅ now required
                clinicName.isNotBlank() &&
                contactNumber.isNotBlank() &&
                isContactValid &&
                isEmailValid &&
                isPasswordValid

    val canScroll by remember { derivedStateOf { scrollState.maxValue > 0 } }
    val atBottom by remember { derivedStateOf { scrollState.value >= scrollState.maxValue } }

    // UI
    BubblesBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { focusManager.clearFocus() }
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, start = 24.dp)
            ) {
                BackButton(
                    onClick = {
                        if (
                            firstName.isNotBlank() ||
                            lastName.isNotBlank() ||
                            clinicName.isNotBlank() ||
                            email.isNotBlank() ||
                            password.isNotBlank() ||
                            confirmPassword.isNotBlank() ||
                            contactNumber.isNotBlank()  ||
                            credentials.isNotBlank()          // ✅ add this

                        ) {
                            showBackDialog = true
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            // Main Content Scroll
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Sign Up",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1D1D1D)
                )

                Spacer(modifier = Modifier.height(10.dp))

// 🔹 Dermatologist badge
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFE0FBFB),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.MedicalServices,
                            contentDescription = null,
                            tint = Color(0xFF0FB2B2),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Dermatologist Account",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0FB2B2)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // First name
                InputField(
                    value = firstName,
                    onValueChange = {
                        firstName = it.take(30)
                        if (!fnameTouched) fnameTouched = true
                    },
                    placeholder = "First Name",
                    iconVector = Icons.Outlined.Person,
                    textColor = Color.Black,
                    errorMessage = if (fnameTouched && firstName.isBlank()) "First name is required" else null
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Last name
                InputField(
                    value = lastName,
                    onValueChange = {
                        lastName = it.take(30)
                        if (!lnameTouched) lnameTouched = true
                    },
                    placeholder = "Last Name",
                    iconVector = Icons.Outlined.Person,
                    textColor = Color.Black,
                    errorMessage = if (lnameTouched && lastName.isBlank()) "Last name is required" else null
                )


                Spacer(modifier = Modifier.height(5.dp))
                InputField(
                    value = credentials,
                    onValueChange = {
                        credentials = it.take(50)
                        if (!credentialsTouched) credentialsTouched = true
                    },
                    placeholder = "Credentials (e.g. MD)",
                    iconVector = Icons.Outlined.LocalHospital,
                    textColor = Color.Black,
                    errorMessage = if (credentialsTouched && credentials.isBlank())
                        "Credentials are required"
                    else null
                )

                Spacer(modifier = Modifier.height(5.dp))
                // Clinic name
                InputField(
                    value = clinicName,
                    onValueChange = {
                        clinicName = it.take(50)
                        if (!clinicTouched) clinicTouched = true
                    },
                    placeholder = "Clinic / Institution",
                    iconVector = Icons.Outlined.LocalHospital,
                    textColor = Color.Black,
                    errorMessage = if (clinicTouched && clinicName.isBlank())
                        "Clinic name is required"
                    else null
                )

                Spacer(modifier = Modifier.height(5.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TextField(
                        value = contactNumber,
                        onValueChange = {
                            contactNumber = it.filter { ch -> ch.isDigit() }.take(10) // only digits after +63
                            if (!contactTouched) contactTouched = true
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        leadingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp)  // 👈 MATCHES OTHER FIELDS
                            ) {
                                // Phone Icon
                                Icon(
                                    imageVector = Icons.Outlined.Phone,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                // +63 fixed prefix
                                Text(
                                    text = "+63",
                                    color = Color.Black,
                                   style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Divider bar
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(24.dp)
                                        .background(Color(0xFFDDDDDD))
                                )
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Clinic Mobile Number",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.DarkGray
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF7F7F7),
                            unfocusedContainerColor = Color(0xFFF7F7F7),
                            disabledContainerColor = Color(0xFFF0F0F0),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            cursorColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(56.dp)
                    )

                    // Error text under field
                    if (contactTouched) {
                        val contactError = when {
                            contactNumber.isBlank() ->
                                "Mobile number is required"
                            contactNumber.length != 10 ->
                                "Enter a valid 10-digit mobile number"
                            else -> null
                        }

                        if (contactError != null) {
                            Text(
                                text = contactError,
                                color = Color.Red,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(5.dp))

                // Email
                InputField(
                    value = email,
                    onValueChange = { email = it.take(64) },
                    placeholder = "Email",
                    iconVector = Icons.Outlined.Email,
                    textColor = Color.Black,
                    errorMessage = when {
                        email.contains(" ") -> "Email must not contain spaces"
                        email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                            "Enter a valid email"
                        else -> null
                    }
                )


                Spacer(modifier = Modifier.height(5.dp))

                // Password
                InputField(
                    value = password,
                    onValueChange = { password = it.take(32) },
                    placeholder = "Password",
                    iconVector = Icons.Outlined.Lock,
                    isPassword = true,
                    textColor = Color.Black,
                    errorMessage = when {
                        password.isNotBlank() && !viewModel.isPasswordStrong(password) ->
                            "Use at least 8 characters with uppercase, lowercase, number & special character"
                        else -> null
                    }
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Confirm Password
                InputField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it.take(32) },
                    placeholder = "Confirm Password",
                    iconVector = Icons.Outlined.Lock,
                    isPassword = true,
                    textColor = Color.Black,
                    errorMessage = if (confirmPassword.isNotBlank() && password != confirmPassword)
                        "Passwords do not match"
                    else null
                )


                Spacer(modifier = Modifier.height(30.dp))

                // Register Button
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .height(50.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            brush = if (isFormValid && !loading) {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF5FEAEA),
                                        Color(0xFF2A9D9D),
                                        Color(0xFF187878)
                                    )
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFBDBDBD),
                                        Color(0xFF9E9E9E),
                                        Color(0xFF757575)
                                    )
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = isFormValid && !loading) {
                            viewModel.register(
                                email = email,
                                password = password,
                                firstName = firstName,
                                lastName = lastName,
                                birthday = "",
                                role = "derma",
                                clinicName = clinicName,
                                contactNumber = "+63$contactNumber",
                                clinicAddress = "",
                                credentials = credentials
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (loading) "Registering..." else "Register",
                        color = if (isFormValid && !loading) Color.White else Color.LightGray,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Text(
                        "Already have an account? ",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF1D1D1D)
                    )
                     Text(
                        text = "Login",
                        color = Color(0xFF2FD8D8),
                        modifier = Modifier.clickable {
                            navController.navigate("login?role=derma")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            // Scroll arrow
            AnimatedVisibility(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
                visible = canScroll && !atBottom,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.5f))
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    Color(0xFFBFFDFD),
                                    Color(0xFF88E7E7),
                                    Color(0xFF55BFBF)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            scope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        tint = Color(0xFF0FB2B2),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Back confirmation dialog
            if (showBackDialog) {
                DialogTemplate(
                    show = showBackDialog,
                    title = "Go Back?",
                    description = "Your details won’t be saved.",
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

            // Success dialog
            if (showSuccessDialog) {
                DialogTemplate(
                    show = showSuccessDialog,
                    title = "Registration Submitted",
                    description = "Please verify your email to proceed. Once verified, our team will review your dermatologist credentials.\n\nYou will receive an email once your account has been approved.",
                    imageResId = R.drawable.success,
                    primaryText = "Go to Login",
                    onPrimary = {
                        showSuccessDialog = false
                        navController.navigate("login?role=derma") {
                            popUpTo("derma_register") { inclusive = true }
                        }
                    },
                    onDismiss = { showSuccessDialog = false }
                )
            }

            // VM events
            LaunchedEffect(authSuccess) {
                if (authSuccess) {
                    showSuccessDialog = true
                    viewModel.resetAuthSuccess()
                }
            }

            LaunchedEffect(errorMessage) {
                errorMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }
            }
        }
    }
}
