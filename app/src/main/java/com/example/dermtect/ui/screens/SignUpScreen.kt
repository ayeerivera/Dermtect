package com.example.dermtect.ui.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.dermtect.R
import com.example.dermtect.ui.components.BubblesBackground
import com.example.dermtect.ui.components.InputField
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dermtect.ui.components.DialogTemplate
import com.example.dermtect.ui.viewmodel.AuthViewModel
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.dermtect.ui.components.BackButton
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import com.example.dermtect.data.repository.AuthRepositoryImpl
import com.example.dermtect.domain.usecase.AuthUseCase
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.material3.TextFieldDefaults



@Composable
fun Register(
    navController: NavController
) {
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthUseCase(AuthRepositoryImpl()))
    )
    var fnameTouched by remember { mutableStateOf(false) }
    var lnameTouched by remember { mutableStateOf(false) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }

    val loading by viewModel.loading.collectAsState()
    val authSuccess by viewModel.authSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }
    var birthday by rememberSaveable { mutableStateOf("") }

    var birthdayValid by rememberSaveable { mutableStateOf(false) }

    val isEmailValid = email.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            !email.contains(" ")

    val isPasswordValid = remember(password, confirmPassword) {
        password == confirmPassword && viewModel.isPasswordStrong(password)
    }

    val isFormValid = remember(
        firstName, lastName, email, password, confirmPassword,
        isEmailValid, isPasswordValid, birthdayValid
    ) {
        firstName.isNotBlank() &&
                lastName.isNotBlank() &&
                isEmailValid &&
                isPasswordValid &&
                birthdayValid
    }

    BubblesBackground {

        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    .padding(top = 50.dp, start = 24.dp)
            ) {
                BackButton(
                    modifier = Modifier.align(Alignment.CenterStart),
                    onClick = {
                        if (
                            firstName.isNotBlank() ||
                            lastName.isNotBlank() ||
                            email.isNotBlank() ||
                            password.isNotBlank() ||
                            confirmPassword.isNotBlank()
                        ) {
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
                Text(
                    "Sign Up",
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1D1D1D)
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFE0FBFB),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Spacer(modifier = Modifier.width(10.dp))

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
                                text = "Regular User Account",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0FB2B2)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // First name (LIMIT 30)
                InputField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                            .take(30) // ✅ same as derma
                            .replaceFirstChar { char -> char.uppercaseChar().toString() }

                        if (!fnameTouched) fnameTouched = true
                    },
                    placeholder = "First Name",
                    iconRes = R.drawable.user_vector,
                    textColor = Color.Black,
                    isPassword = false,
                    errorMessage = if (fnameTouched && firstName.isBlank()) "First name is required" else null
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Last name (LIMIT 30)
                InputField(
                    value = lastName,
                    onValueChange = {
                        lastName = it
                            .take(30) // ✅ same as derma
                            .replaceFirstChar { char -> char.uppercaseChar().toString() }

                        if (!lnameTouched) lnameTouched = true
                    },
                    placeholder = "Last Name",
                    iconRes = R.drawable.user_vector,
                    textColor = Color.Black,
                    isPassword = false,
                    errorMessage = if (lnameTouched && lastName.isBlank()) "Last name is required" else null
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Birthday (already validated)
                BirthdayMaskedField(
                    birthday = birthday,
                    onValueChange = { birthday = it },
                    onValidDate = { birthday = it },
                    onValidationChange = { isValid -> birthdayValid = isValid },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Email (LIMIT 64)
                InputField(
                    value = email,
                    onValueChange = { email = it.take(64) },  // ✅ same limit as derma
                    placeholder = "Email",
                    iconRes = R.drawable.icon_email,
                    textColor = Color.Black,
                    isPassword = false,
                    errorMessage = when {
                        email.contains(" ") -> "Email must not contain spaces"
                        email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                            "Enter a valid email"
                        else -> null
                    }
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Password (LIMIT 32)
                InputField(
                    value = password,
                    onValueChange = { password = it.take(32) },  // ✅ same limit as derma
                    placeholder = "Password",
                    iconRes = R.drawable.icon_pass,
                    textColor = Color.Black,
                    isPassword = true,
                    errorMessage = when {
                        password.isNotBlank() && !viewModel.isPasswordStrong(password) ->
                            "Use at least 8 characters with uppercase, lowercase, number and special character (e.g. DermTect@2024)"
                        else -> null
                    }
                )

                Spacer(modifier = Modifier.height(5.dp))

                // Confirm password (LIMIT 32)
                InputField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it.take(32) }, // ✅ same limit as derma
                    placeholder = "Confirm Password",
                    iconRes = R.drawable.icon_pass,
                    textColor = Color.Black,
                    isPassword = true,
                    errorMessage = if (confirmPassword.isNotBlank() && password != confirmPassword)
                        "Passwords do not match" else null
                )

                Spacer(modifier = Modifier.height(30.dp))

                // Button (unchanged)
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
                                birthday = birthday,
                                role = "patient",
                                clinicName = null,
                                contactNumber = null,
                                clinicAddress = null
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

                Spacer(modifier = Modifier.height(12.dp))
                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    Text(
                        "Already have an account? ",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF1D1D1D)
                    )
                    Text(
                        text = "Login",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF2FD8D8),
                        modifier = Modifier.clickable {
                            navController.navigate("login?role=patient")
                        }
                    )
                }
            }

            // In Register.kt, inside the main Box scope

// Back confirmation dialog
            if (showBackDialog) {
                DialogTemplate(
                    show = showBackDialog,
                    title = "Unsaved Changes",
                    description = "Are you sure you want to go back? All entered information will be lost.",
                    primaryText = "Go Back",
                    onPrimary = {
                        showBackDialog = false
                        navController.popBackStack()
                    },
                    secondaryText = "Cancel",
                    onSecondary = { showBackDialog = false },
                    onDismiss = { showBackDialog = false }
                )
            }

// 🚀 Add the Registration Success Dialog
            if (showSuccessDialog) {
                DialogTemplate(
                    show = showSuccessDialog,
                    title = "Check Your Email!",
                    description = buildAnnotatedString {
                        append("A verification email has been sent to\n")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(email)
                        }
                        append("\nPlease verify before logging in.")
                    }.toString(),

                    imageResId = R.drawable.success, // Replace with your image
                    primaryText = "Go to Login",
                    onPrimary = {
                        showSuccessDialog = false
                        navController.navigate("login") {
                            popUpTo("register") { inclusive = true }
                        }
                    },
                    onDismiss = { showSuccessDialog = false }
                )
            }

// ----------------------------------------------------
// 🚀 Add the LaunchedEffect Blocks (CRITICAL FIX)
// ----------------------------------------------------

// B. Success Listener (Triggers the Dialog)
            LaunchedEffect(authSuccess) {
                if (authSuccess) {
                    showSuccessDialog = true
                    viewModel.resetAuthSuccess() // Reset the state so it can be triggered again later
                }
            }

// C. Error Listener (Triggers the Toast)
            LaunchedEffect(errorMessage) {
                errorMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    viewModel.clearError() // Reset the error state
                }
            }
        }
    }
}



@Composable
fun BirthdayMaskedField(
    birthday: String,
    onValueChange: (String) -> Unit,
    onValidDate: (String) -> Unit = {},
    onValidationChange: (Boolean) -> Unit = {}, // report validity upward
    modifier: Modifier = Modifier
) {
    var tfv by remember {
        mutableStateOf(
            TextFieldValue(
                text = birthday,
                selection = TextRange(birthday.length)
            )
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(birthday) {
        if (birthday != tfv.text) {
            tfv = tfv.copy(text = birthday, selection = TextRange(birthday.length))
        }
    }

    TextField(
        value = tfv,
        onValueChange = { new ->
            val digits = new.text.filter(Char::isDigit).take(8)
            val masked = when {
                digits.length <= 2 -> digits
                digits.length <= 4 -> digits.substring(0, 2) + "/" + digits.substring(2)
                else -> digits.substring(0, 2) + "/" + digits.substring(2, 4) + "/" + digits.substring(4)
            }

            tfv = TextFieldValue(masked, selection = TextRange(masked.length))
            onValueChange(masked)

            var valid = false
            error = null

            if (masked.length == 10) {
                valid = runCatching {
                    val sdf = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault()).apply {
                        isLenient = false
                    }
                    val d = sdf.parse(masked)!!
                    val now = java.util.Calendar.getInstance()
                    val min = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.YEAR, -110) }
                    d.time in min.timeInMillis..now.timeInMillis
                }.getOrDefault(false)

                if (valid) onValidDate(masked) else error = "Invalid date"
            }
            onValidationChange(valid)
        },
        singleLine = true,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        },
        placeholder = {
            Text(
                text = "Birthday (MM/DD/YYYY)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Normal,
                    color = Color.DarkGray
                )
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
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
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        ),
        modifier = modifier
            .fillMaxWidth(0.9f),
        isError = !error.isNullOrBlank(),
        supportingText = if (!error.isNullOrBlank()) {
            {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        } else null
    )
}
