package com.example.dermtect.ui.screens

import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

@Composable
fun Register(navController: NavController) {
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

    val isEmailValid = email.isNotBlank() &&
            Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
            !email.contains(" ")

    val isPasswordValid = remember(password, confirmPassword) {
        password == confirmPassword && viewModel.isPasswordStrong(password)
    }

    val isFormValid = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            isEmailValid &&
            isPasswordValid &&
            birthday.isNotBlank()

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
                        if (firstName.isNotBlank() || lastName.isNotBlank() || email.isNotBlank() || password.isNotBlank() || confirmPassword.isNotBlank()) {
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
                Spacer(modifier = Modifier.height(30.dp))

                InputField(
                    value = firstName,
                    onValueChange = {
                        firstName = it.replaceFirstChar { char ->
                            char.uppercaseChar().toString()
                        }

                        if (!fnameTouched) fnameTouched = true
                    },
                    placeholder = "First Name",
                    iconRes = R.drawable.user_vector,
                    textColor = Color.Black,
                    isPassword = false,
                    errorMessage = if (fnameTouched && firstName.isBlank()) "First name is required" else null
                )

                Spacer(modifier = Modifier.height(5.dp))

                InputField(
                    value = lastName,
                    onValueChange = {
                        lastName = it.replaceFirstChar { char ->
                            char.uppercaseChar().toString()
                        }
                        if (!lnameTouched) lnameTouched = true
                    },
                    placeholder = "Last Name",
                    iconRes = R.drawable.user_vector,
                    textColor = Color.Black,
                    isPassword = false,
                    errorMessage = if (lnameTouched && lastName.isBlank()) "Last name is required" else null
                )

                Spacer(modifier = Modifier.height(5.dp))

                BirthdayMaskedField(
                    birthday = birthday,
                    onValueChange = { birthday = it },
                    onValidDate = { birthday = it }, // keeps the validated value,
                    modifier = Modifier.align(Alignment.CenterHorizontally)

                )



                Spacer(modifier = Modifier.height(5.dp))

                InputField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "Email",
                    iconRes = R.drawable.icon_email,
                    textColor = Color.Black,
                    isPassword = false,
                    errorMessage = when {
                        email.contains(" ") -> "Email must not contain spaces"
                        email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email)
                            .matches() -> "Enter a valid email"

                        else -> null
                    }
                )


                Spacer(modifier = Modifier.height(5.dp))

                InputField(
                    value = password,
                    onValueChange = { password = it },
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

                InputField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Confirm Password",
                    iconRes = R.drawable.icon_pass,
                    textColor = Color.Black,
                    isPassword = true,
                    errorMessage = if (confirmPassword.isNotBlank() && password != confirmPassword) "Passwords do not match" else null
                )

                Spacer(modifier = Modifier.height(30.dp))

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
                                        Color(0xFF5FEAEA), // top lighter shade
                                        Color(0xFF2A9D9D), // middle shade
                                        Color(0xFF187878)  // bottom darker shade
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
                        .clickable(enabled = isFormValid && !loading) {
                            viewModel.register(
                                email = email,
                                password = password,
                                firstName = firstName,
                                lastName = lastName,
                                birthday = birthday
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
                        modifier = Modifier.clickable { navController.navigate("login") }
                    )
                }
            }
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
                    onSecondary = {
                        showBackDialog = false
                    },
                    onDismiss = {
                        showBackDialog = false
                    }
                )
            }
            // Show dialog when registration is successful
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

            // Listen for registration success
            LaunchedEffect(authSuccess) {
                if (authSuccess) {
                    showSuccessDialog = true
                    viewModel.resetAuthSuccess()
                }
            }

            // Listen for error messages
            LaunchedEffect(errorMessage) {
                errorMessage?.let {
                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
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

    // keep external state in sync if it changes (e.g., restored or cleared)
    LaunchedEffect(birthday) {
        if (birthday != tfv.text) {
            tfv = tfv.copy(text = birthday, selection = TextRange(birthday.length))
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {

        TextField(
            value = tfv,
            onValueChange = { new ->
                // mask: keep only digits, insert slashes as MM/DD/YYYY
                val digits = new.text.filter(Char::isDigit).take(8)
                val masked = when {
                    digits.length <= 2 -> digits
                    digits.length <= 4 -> digits.substring(0, 2) + "/" + digits.substring(2)
                    else -> digits.substring(0, 2) + "/" + digits.substring(
                        2,
                        4
                    ) + "/" + digits.substring(4)
                }

                // move cursor to end of masked text
                tfv = TextFieldValue(masked, selection = TextRange(masked.length))
                onValueChange(masked)

                error = null
                if (masked.length == 10) {
                    val ok = runCatching {
                        val sdf =
                            java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault())
                        sdf.isLenient = false
                        val d = sdf.parse(masked)!!
                        val now = java.util.Calendar.getInstance()
                        val min = (now.clone() as java.util.Calendar).apply {
                            add(
                                java.util.Calendar.YEAR,
                                -110
                            )
                        }
                        d.time in min.timeInMillis..now.timeInMillis
                    }.getOrDefault(false)
                    if (ok) onValidDate(masked) else error = "Invalid date"
                }
            },
            singleLine = true,
            readOnly = false,
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
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(56.dp)
        )

        if (!error.isNullOrBlank()) {
            Text(
                text = error!!,
                color = Color.Red,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .padding(start = 16.dp, top = 4.dp)
                    .fillMaxWidth(0.9f)
            )
        }
    }
}

