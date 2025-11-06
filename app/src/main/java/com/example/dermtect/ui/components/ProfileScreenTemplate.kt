package com.example.dermtect.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.dermtect.ui.viewmodel.AuthViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dermtect.R
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory
import com.example.dermtect.ui.viewmodel.SharedProfileViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import com.example.dermtect.ui.viewmodel.DermaHomeViewModel
import com.example.dermtect.ui.viewmodel.UserHomeViewModel
import com.example.dermtect.data.repository.AuthRepositoryImpl
import com.example.dermtect.domain.usecase.AuthUseCase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.EmailAuthProvider
import android.widget.Toast
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.Icon

@Composable
fun ProfileScreenTemplate(
    navController: NavController,
    firstName: String,
    lastName: String,
    email: String,
    isGoogleAccount: Boolean,
    userRole: String = "user",
    sharedProfileViewModel: SharedProfileViewModel,
) {
    val userHomeViewModel: UserHomeViewModel = viewModel()
    val dermaHomeViewModel: DermaHomeViewModel = viewModel()

    val firstNameState by userHomeViewModel.firstName.collectAsState()
    val lastNameState by userHomeViewModel.lastName.collectAsState()
    LaunchedEffect(Unit) {
        userHomeViewModel.fetchUserInfo()
    }
    val fullName = "${firstNameState.ifBlank { firstName }} ${lastNameState.ifBlank { lastName }}".trim()
    var showPhoto by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedFirstName by remember { mutableStateOf(firstName) }
    var editedLastName by remember { mutableStateOf(lastName) }
    val selectedImageUri = sharedProfileViewModel.selectedImageUri.collectAsState().value
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeletedDialog by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthUseCase(AuthRepositoryImpl()))
    )
    val coroutineScope = rememberCoroutineScope()
    var passwordInput by remember { mutableStateOf("") }
    var showPasswordError by remember { mutableStateOf(false) }
    var triggerNavigation by remember { mutableStateOf(false) }
    val collection = "users"
    LaunchedEffect(Unit) { sharedProfileViewModel.loadPhoto(collection) }

    // add with the other remember states
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showSavePhotoDialog by remember { mutableStateOf(false) }

// change your launcher to *stage* the photo instead of saving immediately
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            tempPhotoUri = uri            // stage first
            showSavePhotoDialog = true    // ask to confirm
        }
    }

    val context = LocalContext.current
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("50445058822-fn9cea4e0bduos6t0g7ofb2g9ujri5s2.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val googleSignInClient = GoogleSignIn.getClient(context, gso)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(
                    color = Color(0xFFCDFFFF)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 50.dp, start = 23.dp)
            ) {
                BackButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { showPhoto = true },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.profilepicture),
                            contentDescription = "Default Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }


                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PrimaryButton(
                        text = "Change Photo",
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.height(42.dp).defaultMinSize(minWidth = 140.dp)
                    )

                    if (selectedImageUri != null) {
                        SecondaryButton(
                            text = "Remove Photo",
                            onClick = { showRemoveConfirmDialog = true },
                            modifier = Modifier.height(42.dp).defaultMinSize(minWidth = 140.dp)
                        )
                    }
                }
                }
            }

            Card(
                modifier = Modifier
                    .offset(x = 25.dp, y = -10.dp) // matched to Settings
                    .fillMaxWidth(0.9f)
                    .padding (bottom = 10.dp)
                    .wrapContentHeight()
                    .shadow(8.dp, RoundedCornerShape(36.dp)), // matched to Settings
                shape = RoundedCornerShape(36.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column (
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                ){
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 19.dp, vertical = 18.dp)
                            .fillMaxWidth()
                            .height(85.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEDFFFF))
                    ) {
                        AccountIdentityRow(email = email, isGoogleAccount = isGoogleAccount)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

// Inline editable name (no dialog)
                    EditableNameSection(
                        firstNameInitial = firstNameState.ifBlank { firstName },
                        lastNameInitial = lastNameState.ifBlank { lastName }
                    ) { newFirst, newLast ->
                        if (userRole == "user") {
                            userHomeViewModel.updateName(newFirst, newLast)
                        } else {
                            dermaHomeViewModel.updateName(newFirst, newLast)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

// Inline change password (no dialog)
                    ChangePasswordSection()

                    Spacer(modifier = Modifier.height(15.dp))
                    DeleteAccountRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Deactivate Account",
                                tint = Color(0xFF0FB2B2),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = "Deactivate Account",
                        onClick = {
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }

        if (showPhoto) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000))
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = 60.dp, y = 300.dp)
                        .size(width = 296.dp, height = 295.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Full Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.profilepicture),
                            contentDescription = "Default Full Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(x = 56.dp, y = 296.dp)
                        .size(width = 37.dp, height = 35.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { showPhoto = false },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        DialogTemplate(
            show = showSavePhotoDialog,
            title = "Save this photo?",
            description = "Use this as your new profile picture?",
            primaryText = "Save",
            onPrimary = {
                tempPhotoUri?.let { sharedProfileViewModel.setImageUri(it, collection) }
                tempPhotoUri = null
                showSavePhotoDialog = false
            },
            secondaryText = "Cancel",
            onSecondary = {
                tempPhotoUri = null
                showSavePhotoDialog = false
            },
            onDismiss = {
                tempPhotoUri = null
                showSavePhotoDialog = false
            }
        )

        DialogTemplate(
            show = showRemoveConfirmDialog,
            title = "Remove Profile Photo?",
            description = "This will reset your profile picture to the default image.",
            primaryText = "Yes, Remove",
            onPrimary = {
                sharedProfileViewModel.clearImageUri(collection)
                showRemoveConfirmDialog = false
            },
            secondaryText = "Cancel",
            onSecondary = { showRemoveConfirmDialog = false },
            onDismiss = { showRemoveConfirmDialog = false }
        )

        DialogTemplate(
            show = showDeleteDialog,
            title = "Deactivate Account?",
            description = "Please enter your password to confirm. This action is irreversible.",
            primaryText = "Deactivate my Account",
            onPrimary = {
                if (passwordInput.isBlank()) {
                    showPasswordError = true
                    showDeleteDialog = true
                } else {
                    showPasswordError = false
                    coroutineScope.launch {
                        try {
                            authViewModel.reauthenticateAndDelete(
                                password = passwordInput,
                                onSuccess = {
                                    showDeleteDialog = false
                                    showDeletedDialog = true
                                    triggerNavigation = true
                                },
                                onFailure = {
                                    showPasswordError = true
                                    showDeleteDialog = true
                                }
                            )
                        } catch (_: Exception) {
                            showPasswordError = true
                            showDeleteDialog = true
                        }
                    }
                }
            },
            secondaryText = "Cancel",
            onSecondary = {
                showDeleteDialog = false
                passwordInput = ""
                showPasswordError = false
            },
            onDismiss = {
                showDeleteDialog = false
                passwordInput = ""
                showPasswordError = false
            },
            extraContent = {
                Column {
                    InputField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            if (showPasswordError) showPasswordError = false
                        },
                        placeholder = "Enter your password",
                        iconRes = R.drawable.icon_pass,
                        isPassword = true,
                        errorMessage = if (showPasswordError) "Incorrect password. Please try again." else null,
                        modifier = Modifier.fillMaxWidth(),                   // ⬅️ full width so placeholder doesn't truncate
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholderStyle = MaterialTheme.typography.labelMedium
                    )
                    if (showPasswordError) {
                        Text(
                            text = "Incorrect password. Please try again.",
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        )

        if (triggerNavigation) {
            LaunchedEffect(Unit) {
                delay(2000)
                navController.navigate("login") {
                    popUpTo("profile") { inclusive = true }
                }
                triggerNavigation = false
            }
        }

        DialogTemplate(
            show = showLogoutDialog,
            title = "Confirm logout?",
            primaryText = "Yes, Logout",
            onPrimary = {
                // 1) Sign out Google (harmless if email/password user)
                googleSignInClient.signOut().addOnCompleteListener {
                    // 2) Then sign out Firebase
                    authViewModel.logout {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            },
            secondaryText = "Stay logged in",
            onSecondary = { showLogoutDialog = false },
            onDismiss = { showLogoutDialog = false }
        )

        DialogTemplate(
            show = showDeletedDialog,
            title = "Your account has been deactivated!",
            imageResId = R.drawable.success,
            autoDismiss = true,
            onDismiss = { showDeletedDialog = false }
        )

    }



// Reusing existing composables below with no changes needed

    @Composable
    fun ChangePasswordRow(
        icon: @Composable () -> Unit,
        label: String,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 43.92.dp, height = 40.26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDFFFF)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(15.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                color = Color(0xFF484848),
                modifier = Modifier.weight(1f)
            )

            Box(
                modifier = Modifier
                    .size(width = 26.dp, height = 24.dp)
                    .background(Color.White, shape = RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_right),
                    contentDescription = "Navigate",
                    tint = Color(0xFF0FB2B2),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }



                @Composable
                fun DeleteAccountRow(
                    icon: @Composable () -> Unit,
                    label: String,
                    onClick: () -> Unit
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick() }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 43.92.dp, height = 40.26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            icon()
                        }

                        Spacer(modifier = Modifier.width(15.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                            color = Color(0xFF484848),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(width = 26.dp, height = 24.dp)
                                .background(Color.White, shape = RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_right),
                                contentDescription = "Navigate",
                                tint = Color(0xFF0FB2B2),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

@Composable
                fun EditName(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick() }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 43.92.dp, height = 40.26.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDFFFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            icon()
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                            color = Color(0xFF484848),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(width = 26.dp, height = 24.dp)
                                .background(Color.White, shape = RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_right),
                                contentDescription = "Navigate",
                                tint = Color(0xFF0FB2B2),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }




                @Composable
                private fun Label(text: String) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF1D1D1D),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, bottom = 6.dp)
                    )
                }

                @Composable
                fun AccountIdentityRow(email: String, isGoogleAccount: Boolean) {

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isGoogleAccount) {
                            Box(
                                modifier = Modifier
                                    .size(width = 62.dp, height = 57.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFCDFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.google_logo),
                                    contentDescription = "Google Icon",
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(width = 62.dp, height = 57.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFCDFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Email,
                                    contentDescription = "Email",
                                    tint = Color(0xFF0FB2B2),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF484848)
                            )

                            if (isGoogleAccount) {
                                Text(
                                    text = "Google Account",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                                    color = Color.Gray,
                                )
                            } else {
                                Text(
                                    text = "Email",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                                    color = Color.Gray,
                                )
                            }

                        }
                    }
                }

                @Composable
                private fun EditableNameSection(
                    firstNameInitial: String,
                    lastNameInitial: String,
                    onSave: (String, String) -> Unit
                ) {
                    var first by remember(firstNameInitial) { mutableStateOf(firstNameInitial) }
                    var last by remember(lastNameInitial) { mutableStateOf(lastNameInitial) }

                    val changed = first != firstNameInitial || last != lastNameInitial
                    val valid = first.isNotBlank() && last.isNotBlank()

                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Text(
                            "Edit Name",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF1D1D1D)
                        )
                        Spacer(Modifier.height(10.dp))

                        InputField(
                            value = first,
                            onValueChange = { first = it },
                            placeholder = "First name",
                            iconRes = null,                 // or a person icon if you have one
                            isPassword = false,
                            errorMessage = if (first.isBlank()) "First name is required" else null,
                            modifier = Modifier.fillMaxWidth() // your InputField will cap at 90% width internally
                        )

                        Spacer(Modifier.height(10.dp))

                        InputField(
                            value = last,
                            onValueChange = { last = it },
                            placeholder = "Last name",
                            iconRes = null,
                            isPassword = false,
                            errorMessage = if (last.isBlank()) "Last name is required" else null,
                            modifier = Modifier.fillMaxWidth()
                        )


                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            PrimaryButton(
                                text = "Save",
                                onClick = { onSave(first.trim(), last.trim()) },
                                enabled = changed && valid,
                                modifier = Modifier.weight(1f).height(48.dp)
                            )

                            SecondaryButton(
                                text = "Reset",
                                onClick = {
                                    first = firstNameInitial
                                    last = lastNameInitial
                                },
                                enabled = changed,
                                modifier = Modifier.weight(1f).height(48.dp)
                            )
                        }

                    }
                }

                @Composable
                private fun ChangePasswordSection() {
                    val context = LocalContext.current
                    val authViewModel: AuthViewModel = viewModel(
                        factory = AuthViewModelFactory(AuthUseCase(AuthRepositoryImpl()))
                    )

                    var current by remember { mutableStateOf("") }
                    var pass by remember { mutableStateOf("") }
                    var confirm by remember { mutableStateOf("") }
                    var isLoading by remember { mutableStateOf(false) }
                    val passValid = authViewModel.isPasswordStrong(pass)
                    val matches = pass == confirm
                    val canSubmit = current.isNotBlank() && passValid && matches && !isLoading

                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Text(
                            "Change Password",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color(0xFF1D1D1D)
                        )

                        Spacer(Modifier.height(10.dp))

                        InputField(
                            value = current,
                            onValueChange = { current = it },
                            placeholder = "Current password",
                            iconRes = R.drawable.icon_pass,
                            isPassword = true,
                            errorMessage = null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        InputField(
                            value = pass,
                            onValueChange = { pass = it },
                            placeholder = "New password",
                            iconRes = R.drawable.icon_pass,
                            isPassword = true,
                            errorMessage = if (pass.isNotBlank() && !passValid)
                                "Use at least 8 characters including upper, lower, number, special"
                            else null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        InputField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            placeholder = "Confirm new password",
                            iconRes = R.drawable.icon_pass,
                            isPassword = true,
                            errorMessage = if (confirm.isNotBlank() && confirm != pass) "Passwords do not match" else null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        PrimaryButton(
                            text = "Update Password",
                            onClick = {
                                val user = FirebaseAuth.getInstance().currentUser
                                val email = user?.email
                                if (user == null || email == null) {
                                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                                    return@PrimaryButton
                                }

                                val cred = EmailAuthProvider.getCredential(email, current)
                                user.reauthenticate(cred).addOnCompleteListener { authTask ->
                                    if (authTask.isSuccessful) {
                                        user.updatePassword(pass).addOnCompleteListener { upd ->
                                            if (upd.isSuccessful) {
                                                Toast.makeText(context, "Password updated", Toast.LENGTH_SHORT).show()
                                                current = ""; pass = ""; confirm = ""
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    upd.exception?.message ?: "Failed to update password",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            context,
                                            authTask.exception?.message ?: "Re-authentication failed",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            },
                            enabled = canSubmit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        )

                    }
                }

