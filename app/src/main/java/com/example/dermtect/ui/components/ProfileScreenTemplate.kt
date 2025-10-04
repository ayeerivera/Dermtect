package com.example.dermtect.ui.components

import android.net.Uri
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
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

@Composable
fun ProfileScreenTemplate(
    navController: NavController,
    firstName: String,
    lastName: String,
    email: String,
    isGoogleAccount: Boolean,
    userRole: String = "user",
    sharedProfileViewModel: SharedProfileViewModel,
){
    val userHomeViewModel: UserHomeViewModel = viewModel()
    val dermaHomeViewModel: DermaHomeViewModel = viewModel()

    val firstNameState by userHomeViewModel.firstName.collectAsState()
    val lastNameState  by userHomeViewModel.lastName.collectAsState()
    LaunchedEffect(Unit) {
        userHomeViewModel.fetchUserInfo()
    }
    val fullName = "$firstNameState $lastNameState"
    var showPhoto by remember { mutableStateOf(false) }
    var showPhotoOptions by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedFirstName by remember { mutableStateOf(firstName) }
    var editedLastName by remember { mutableStateOf(lastName) }

    var showDiscardDialog by remember { mutableStateOf(false) }
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
    val selectedImageUri = sharedProfileViewModel.selectedImageUri.collectAsState().value
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        sharedProfileViewModel.setImageUri(uri) // must call ViewModel
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
            .verticalScroll(rememberScrollState())
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

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { showPhotoOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Photo",
                            tint = Color(0xFF0FB2B2),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = fullName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
        }
        Card(
            modifier = Modifier
                .offset(x = 25.dp, y = -20.dp) // matched to Settings
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .shadow(8.dp, RoundedCornerShape(36.dp)), // matched to Settings
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .offset(x = 19.dp, y = 18.dp)
                        .size(width = 323.dp, height = 85.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDFFFF))
                ) {
                    AccountInfoRow(email = email, isGoogleAccount = isGoogleAccount)
                }

                Spacer(modifier = Modifier.height(30.dp))

                EditName(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = Color(0xFF0FB2B2),
                            modifier = Modifier.size(28.dp) // 28.dp to match Settings icon size
                        )
                    },
                    label = "Edit Name",
                    onClick = {
                        editedFirstName = firstNameState
                        editedLastName = lastNameState
                        showEditNameDialog = true
                    }
                )

                ChangePasswordRow(
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Change Password",
                            tint = Color(0xFF0FB2B2),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    label = "Change Password",
                    onClick = {
                        navController.navigate("change_pass")
                    }
                )

                if (userRole == "user") {
                    AssessmentRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.AssignmentTurnedIn,
                                contentDescription = "My Assessment Report",
                                tint = Color(0xFF0FB2B2),
                                modifier = Modifier.size(28.dp) // matched Settings icon size
                            )
                        },
                        label = "My Assessment Report",
                        onClick = {
                            navController.navigate("questionnaire")
                        }
                    )
                }

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
        show = showPhotoOptions,
        title = "Edit Profile Photo",
        description = "Would you like to change or remove your profile photo?",
        primaryText = "Change Photo",
        onPrimary = {
            imagePickerLauncher.launch("image/*")
            showPhotoOptions = false
        },
        secondaryText = "Remove Photo",
        onSecondary = {
            showPhotoOptions = false
            if (selectedImageUri != null) {
                showRemoveConfirmDialog = true
            }
        },
        tertiaryText = "Cancel",
        onTertiary = { showPhotoOptions = false },
        onDismiss = { showPhotoOptions = false }
    )

    DialogTemplate(
        show = showRemoveConfirmDialog,
        title = "Remove Profile Photo?",
        description = "This will reset your profile picture to the default image.",
        primaryText = "Yes, Remove",
        onPrimary = {
            sharedProfileViewModel.clearImageUri()
            showRemoveConfirmDialog = false
        },
        secondaryText = "Cancel",
        onSecondary = { showRemoveConfirmDialog = false },
        onDismiss = { showRemoveConfirmDialog = false }
    )

    DialogTemplate(
        show = showDeleteDialog,
        title = "Delete Account?",
        description = "Please enter your password to confirm. This action is irreversible.",
        primaryText = "Yes, Delete my Account",
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
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Enter your password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(top = 8.dp)
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
        title = "Your account has been permanently deleted!",
        imageResId = R.drawable.success,
        autoDismiss = true,
        onDismiss = { showDeletedDialog = false }
    )
    if (showEditNameDialog) {
        DialogTemplate(
            show = true,
            title = "Edit Name",
            primaryText = "Save",
            onPrimary = {
                if (userRole == "user") {
                    userHomeViewModel.updateName(editedFirstName, editedLastName)
                } else {
                    dermaHomeViewModel.updateName(editedFirstName, editedLastName)
                }
                editedFirstName = firstNameState
                editedLastName  = lastNameState
                showEditNameDialog = false
            },
            secondaryText = "Cancel",
            onSecondary = {
                if (editedFirstName != firstNameState || editedLastName != lastNameState) {
                    showDiscardDialog = true
                } else {
                    showEditNameDialog = false
                }
            },
            onDismiss = {
                if (editedFirstName != firstNameState || editedLastName != lastNameState) {
                    showDiscardDialog = true
                } else {
                    showEditNameDialog = false
                }
            },
            extraContent = {
                Column {
                    OutlinedTextField(
                        value = editedFirstName,
                        onValueChange = { editedFirstName = it },
                        label = { Text("First Name") }
                    )
                    OutlinedTextField(
                        value = editedLastName,
                        onValueChange = { editedLastName = it },
                        label = { Text("Last Name") }
                    )
                }
            }
        )
    }

    if (showDiscardDialog) {
        DialogTemplate(
            show = true,
            title = "Discard changes?",
            description = "Your edits will not be saved.",
            primaryText = "Discard",
            onPrimary = {
                showDiscardDialog = false
                showEditNameDialog = false
                editedFirstName = firstNameState
                editedLastName  = lastNameState
            },
            secondaryText = "Keep Editing",
            onSecondary = {
                showDiscardDialog = false
            },
            onDismiss = {
                showDiscardDialog = false
            }
        )
    }
}

// Reusing existing composables below with no changes needed

@Composable
fun ChangePasswordRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
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
fun AssessmentRow(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp
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
            fontSize = fontSize,
            color = Color(0xFF484848),
            fontWeight = FontWeight.Normal,
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
fun DeleteAccountRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
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
