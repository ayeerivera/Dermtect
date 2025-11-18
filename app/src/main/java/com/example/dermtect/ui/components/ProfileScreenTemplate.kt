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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dermtect.R
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory
import com.example.dermtect.ui.viewmodel.SharedProfileViewModel
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.ArrowDownward
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.example.dermtect.ui.screens.BirthdayMaskedField
import kotlinx.coroutines.launch

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
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthUseCase(AuthRepositoryImpl()))
    )

    val userHomeViewModel: UserHomeViewModel = viewModel()
    val dermaHomeViewModel: DermaHomeViewModel = viewModel()

    val firstNameFromVm by userHomeViewModel.firstName.collectAsState()
    val lastNameFromVm  by userHomeViewModel.lastName.collectAsState()
    val birthdayFromVm  by userHomeViewModel.birthday.collectAsState()         // String? in your VM

    LaunchedEffect(Unit) {
        userHomeViewModel.fetchUserInfo()
    }

    var showPhoto by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }
    val selectedImageUri = sharedProfileViewModel.selectedImageUri.collectAsState().value
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeletedDialog by remember { mutableStateOf(false) }

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
    // Put this near your other launchers (e.g., right after imagePickerLauncher)
    val googleReauthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val user = FirebaseAuth.getInstance().currentUser

            user?.reauthenticate(credential)?.addOnCompleteListener { reauth ->
                if (reauth.isSuccessful) {
                    user.delete().addOnCompleteListener { del ->
                        if (del.isSuccessful) {
                            showDeleteDialog = false
                            showDeletedDialog = true
                            triggerNavigation = true
                        } else {
                            showPasswordError = true
                        }
                    }
                } else {
                    showPasswordError = true
                }
            }
        } catch (e: Exception) {
            showPasswordError = true
        }
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val atBottom = remember { derivedStateOf { scrollState.value >= (scrollState.maxValue - 8) } }.value
    val canScroll = remember { derivedStateOf { scrollState.maxValue > 0 } }.value


    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("YOUR_WEB_CLIENT_ID")
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
                .padding(bottom = 10.dp)
                .wrapContentHeight()
                .shadow(8.dp, RoundedCornerShape(36.dp)), // matched to Settings
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {
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
                // replace the two sections with:
                CombinedProfileSection(
                    initialFirstName = firstNameFromVm.ifBlank { "" },
                    initialLastName = lastNameFromVm.ifBlank { "" },
                    initialBirthday = (birthdayFromVm ?: ""),
                    onSaveAll = { newFirst, newLast, newBirthday->
                        userHomeViewModel.updateProfile(
                            firstName = newFirst,
                            lastName = newLast,
                            birthday = newBirthday,
                            onSuccess = {
                                userHomeViewModel.fetchUserInfo()

                                Toast.makeText(
                                    context,
                                    "Profile saved successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { errorMsg ->
                                Toast.makeText(context, "Save failed: $errorMsg", Toast.LENGTH_LONG)
                                    .show()
                            }
                        )
                    }
                )

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


    AnimatedVisibility(
        modifier = Modifier
            .align(Alignment.CenterHorizontally) // <-- 'align' is available because the parent is a Box
            .padding(bottom = 20.dp),
        visible = canScroll && !atBottom,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
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
                        val target = scrollState.maxValue
                        scrollState.animateScrollTo(target)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowDownward,
                contentDescription = "Scroll down",
                tint = Color(0xFF0FB2B2),
                modifier = Modifier.size(28.dp)
            )
        }
    } }
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

    var isWorking by remember { mutableStateOf(false) }

// derive whether this account needs a password
    val requiresPassword = !isGoogleAccount

    // 🔒 Disable primary when:
// - working, or
// - (email/password user AND password is blank)
    val canSubmitPrimary =
        if (requiresPassword) passwordInput.isNotBlank() && !isWorking
        else !isWorking

    DialogTemplate(
        show = showDeleteDialog,
        title = "Deactivate Account?",
        description = if (requiresPassword)
            "Please enter your password to confirm. This action is irreversible."
        else
            "Confirm to deactivate your account. This action is irreversible.",
        primaryText = if (isWorking) "Working..." else "Deactivate my Account",
        onPrimary = {
            if (isWorking) return@DialogTemplate

            val user = FirebaseAuth.getInstance().currentUser ?: run {
                showPasswordError = true
                return@DialogTemplate
            }

            // GOOGLE USERS → reauth via Google (no password needed)
            if (isGoogleAccount) {
                isWorking = true
                googleSignInClient.signOut().addOnCompleteListener {
                    val intent = googleSignInClient.signInIntent
                    googleReauthLauncher.launch(intent)
                    isWorking = false // launcher callback will finish the flow
                }
                return@DialogTemplate
            }

            // EMAIL/PASSWORD USERS → must have a non-blank password
            if (passwordInput.isBlank()) {
                showPasswordError = true
                return@DialogTemplate
            }

            val email = user.email ?: run {
                showPasswordError = true
                return@DialogTemplate
            }

            val cred = EmailAuthProvider.getCredential(email, passwordInput)

            isWorking = true
            showPasswordError = false

            user.reauthenticate(cred).addOnCompleteListener { reauth ->
                if (reauth.isSuccessful) {
                    user.delete().addOnCompleteListener { del ->
                        isWorking = false
                        if (del.isSuccessful) {
                            showDeleteDialog = false
                            showDeletedDialog = true
                            triggerNavigation = true
                        } else {
                            showPasswordError = true
                        }
                    }
                } else {
                    isWorking = false
                    showPasswordError = true
                }
            }
        },
        // ✅ Disable primary button until valid so clicking it can't close the dialog
        primaryEnabled = canSubmitPrimary,
        secondaryText = if (isWorking) "Please wait…" else "Cancel",
        onSecondary = {
            if (isWorking) return@DialogTemplate
            showDeleteDialog = false
            passwordInput = ""
            showPasswordError = false
        },
        onDismiss = {
            if (isWorking) return@DialogTemplate
            showDeleteDialog = false
            passwordInput = ""
            showPasswordError = false
        },
        extraContent = {
            // Show password field ONLY for email/password users
            if (requiresPassword) {
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
                        errorMessage = if (showPasswordError) "Incorrect or missing password." else null,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        placeholderStyle = MaterialTheme.typography.labelMedium
                    )
                    if (isWorking) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                // Optional helper text for Google users
                if (isWorking) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
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
            show = showDeletedDialog,
            title = "Your account has been deactivated!",
            imageResId = R.drawable.success,
            autoDismiss = true,
            onDismiss = { showDeletedDialog = false }
        )
}





// Reusing existing composables below with no changes needed




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
private fun CombinedProfileSection(
    initialFirstName: String,
    initialLastName: String,
    initialBirthday: String,
    onSaveAll: (String, String, String) -> Unit
) {
    var first by remember(initialFirstName) { mutableStateOf(initialFirstName) }
    var last by remember(initialLastName) { mutableStateOf(initialLastName) }
    var birthday by remember(initialBirthday) { mutableStateOf(initialBirthday) }

    val nameValid = first.isNotBlank() && last.isNotBlank()
    fun isBirthdayValid(s: String): Boolean =
        runCatching {
            if (s.length != 10) return@runCatching false
            val sdf = java.text.SimpleDateFormat("MM/dd/yyyy", java.util.Locale.getDefault()).apply { isLenient = false }
            val d = sdf.parse(s)!!
            val now = java.util.Calendar.getInstance()
            val min = (now.clone() as java.util.Calendar).apply { add(java.util.Calendar.YEAR, -110) }
            d.time in min.timeInMillis..now.timeInMillis
        }.getOrDefault(false)

    var birthdayValid by remember(initialBirthday) { mutableStateOf(isBirthdayValid(initialBirthday)) }

    val changed = first != initialFirstName ||
            last != initialLastName ||
            birthday != initialBirthday
    val canSave = nameValid && birthdayValid && changed

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp) ) {

        // --- Edit Name ---
        Text("Edit Name", style = MaterialTheme.typography.titleLarge, color = Color(0xFF1D1D1D))
        Spacer(Modifier.height(10.dp))

        InputField(
            value = first,
            onValueChange = { first = it },
            placeholder = "First name",
            iconRes = R.drawable.user_vector,
            isPassword = false,
            errorMessage = if (first.isBlank()) "First name is required" else null,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(10.dp))

        InputField(
            value = last,
            onValueChange = { last = it },
            placeholder = "Last name",
            iconRes = R.drawable.user_vector,
            isPassword = false,
            errorMessage = if (last.isBlank()) "Last name is required" else null,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(24.dp))

        // --- Edit Birthday ---
        Text("Edit Birthday", style = MaterialTheme.typography.titleLarge, color = Color(0xFF1D1D1D))
        Spacer(Modifier.height(10.dp))

        // ✅ Pass validity up; field itself shows error via supportingText/isError
        BirthdayMaskedField(
            birthday = birthday,
            onValueChange = { birthday = it },
            onValidDate = { birthday = it },
            onValidationChange = { isValid -> birthdayValid = isValid },
            modifier = Modifier.wrapContentWidth().align(Alignment.CenterHorizontally)
        )


        Spacer(Modifier.height(12.dp))

        // --- One Save/Reset row for all three ---
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = "Save",
                onClick = { onSaveAll(first.trim(), last.trim(), birthday.trim()) },
                enabled = canSave,
                modifier = Modifier.weight(1f).height(48.dp)
            )
            SecondaryButton(
                text = "Reset",
                onClick = {
                    first = initialFirstName
                    last = initialLastName
                    birthday = initialBirthday
                    birthdayValid = isBirthdayValid(initialBirthday)
                },
                enabled = changed,
                modifier = Modifier.weight(1f).height(48.dp)
            )
        }
    }
}

@Composable
                 fun ChangePasswordSection() {
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
                            modifier = Modifier
                                .wrapContentWidth()
                                .align(Alignment.CenterHorizontally)
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
                            modifier = Modifier
                                .wrapContentWidth()
                                .align(Alignment.CenterHorizontally)
                        )

                        Spacer(Modifier.height(10.dp))

                        InputField(
                            value = confirm,
                            onValueChange = { confirm = it },
                            placeholder = "Confirm new password",
                            iconRes = R.drawable.icon_pass,
                            isPassword = true,
                            errorMessage = if (confirm.isNotBlank() && confirm != pass) "Passwords do not match" else null,
                            modifier = Modifier
                                .wrapContentWidth()
                                .align(Alignment.CenterHorizontally)
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
