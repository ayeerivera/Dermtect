package com.example.dermtect.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.dermtect.R
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.components.DialogTemplate
import com.example.dermtect.ui.components.InputField
import com.example.dermtect.ui.components.PrimaryButton
import com.example.dermtect.ui.components.SecondaryButton
import com.example.dermtect.ui.viewmodel.AuthViewModel
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory
import com.example.dermtect.ui.viewmodel.DermaHomeViewModel
import com.example.dermtect.ui.viewmodel.SharedProfileViewModel
import com.example.dermtect.data.repository.AuthRepositoryImpl
import com.example.dermtect.domain.usecase.AuthUseCase
import com.example.dermtect.ui.components.ChangePasswordSection
import kotlinx.coroutines.launch

@Composable
fun DermaProfileScreen(
    navController: NavController,
    email: String,
    isGoogleAccount: Boolean,
    sharedProfileViewModel: SharedProfileViewModel
) {
    val context = LocalContext.current

    // VMs
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(AuthUseCase(AuthRepositoryImpl()))
    )
    val dermaHomeViewModel: DermaHomeViewModel = viewModel()

    // 🔁 Load derma clinic data once
    LaunchedEffect(Unit) {
        dermaHomeViewModel.fetchDermaProfile()   // 👉 implement this in your VM if not yet
    }

    // Firestore-driven state (from DermaHomeViewModel)
    val clinicNameFromVm by dermaHomeViewModel.clinicName.collectAsState(initial = "")
    val contactFromVm    by dermaHomeViewModel.contactNumber.collectAsState(initial = "")
    val addressFromVm    by dermaHomeViewModel.clinicAddress.collectAsState(initial = "")
    val firstFromVm by dermaHomeViewModel.firstName
    val lastFromVm  by dermaHomeViewModel.lastName
    val credsFromVm by dermaHomeViewModel.credentials

    // Profile photo
    val collection = "users"
    LaunchedEffect(Unit) { sharedProfileViewModel.loadPhoto(collection) }
    val selectedImageUri = sharedProfileViewModel.selectedImageUri.collectAsState().value
    var showPhoto by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf(false) }

    // temp photo before saving
    var tempPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var showSavePhotoDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            tempPhotoUri = uri
            showSavePhotoDialog = true
        }
    }

    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // ===== HEADER WITH PHOTO =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .background(Color(0xFFCDFFFF))
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF1D1D1D)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(180.dp)
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
                        modifier = Modifier
                            .height(42.dp)
                            .defaultMinSize(minWidth = 140.dp)
                            .padding(bottom = 10.dp)
                    )

                    if (selectedImageUri != null) {
                        SecondaryButton(
                            text = "Remove Photo",
                            onClick = { showRemoveConfirmDialog = true },
                            modifier = Modifier
                                .height(42.dp)
                                .defaultMinSize(minWidth = 140.dp)
                        )
                    }
                }
            }
        }

        // ===== CARD WITH EMAIL + CLINIC INFO =====
        Card(
            modifier = Modifier
                .offset(x = 25.dp, y = -10.dp)
                .fillMaxWidth(0.9f)
                .padding(bottom = 10.dp)
                .shadow(8.dp, RoundedCornerShape(36.dp)),
            shape = RoundedCornerShape(36.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.verticalScroll(scrollState)
            ) {
                // identity row (email / google)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 19.dp, vertical = 18.dp)
                        .fillMaxWidth()
                        .height(85.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDFFFF))
                ) {
                    DermaAccountIdentityRow(
                        email = email,
                        isGoogleAccount = isGoogleAccount
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CLINIC INFO SECTION
                DermaProfileSection(
                    initialFirstName = firstFromVm,
                    initialLastName = lastFromVm,
                    initialCredentials = credsFromVm,
                    initialClinicName = clinicNameFromVm,
                    initialContactNumber = contactFromVm,
                    initialClinicAddress = addressFromVm,
                    onSaveAll = { newFirst, newLast, newCreds, newClinic, newContact, newAddress ->
                        dermaHomeViewModel.updateDermaProfile(
                            firstName      = newFirst,
                            lastName       = newLast,
                            credentials    = newCreds,
                            clinicName     = newClinic,
                            contactNumber  = newContact,
                            clinicAddress  = newAddress,
                            onSuccess = {
                                Toast.makeText(
                                    context,
                                    "Profile updated!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { errorMsg ->
                                Toast.makeText(
                                    context,
                                    "Save failed: $errorMsg",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Optional: reuse your change password
                ChangePasswordSection()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ===== FULL PHOTO PREVIEW =====
    if (showPhoto) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(260.dp),
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
                    .align(Alignment.Center)
                    .offset(y = 170.dp)
                    .size(40.dp)
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

    // ===== DIALOGS FOR PHOTO SAVE / REMOVE =====
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
}

@Composable
private fun DermaProfileSection(
    initialFirstName: String,
    initialLastName: String,
    initialCredentials: String,
    initialClinicName: String,
    initialContactNumber: String,
    initialClinicAddress: String,
    onSaveAll: (
        String, // firstName
        String, // lastName
        String, // credentials
        String, // clinicName
        String, // contactNumber
        String  // fullAddress
    ) -> Unit
) {
    // Try to parse old address "Street, Barangay, Municipality, Pampanga"
    val parts = remember(initialClinicAddress) {
        initialClinicAddress.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    val initialStreet       = parts.getOrNull(0) ?: ""
    val initialBarangay     = parts.getOrNull(1) ?: ""
    val initialMunicipality = parts.getOrNull(2) ?: ""

    var firstName by remember(initialFirstName) { mutableStateOf(initialFirstName) }
    var lastName by remember(initialLastName) { mutableStateOf(initialLastName) }
    var credentials by remember(initialCredentials) { mutableStateOf(initialCredentials) }

    var clinicName by remember(initialClinicName) { mutableStateOf(initialClinicName) }
    var contactNumber by remember(initialContactNumber) { mutableStateOf(initialContactNumber) }
    var street by remember(initialClinicAddress) { mutableStateOf(initialStreet) }
    var barangay by remember(initialClinicAddress) { mutableStateOf(initialBarangay) }
    var municipality by remember(initialClinicAddress) { mutableStateOf(initialMunicipality) }

    val province = "Pampanga" // fixed

    // Validation
    val firstValid = firstName.isNotBlank()
    val lastValid = lastName.isNotBlank()
    val clinicValid = clinicName.isNotBlank()
    val contactValid = contactNumber.isNotBlank()
    val streetValid = street.isNotBlank()
    val barangayValid = barangay.isNotBlank()
    val municipalityValid = municipality.isNotBlank()

    val changed =
        firstName != initialFirstName ||
                lastName != initialLastName ||
                credentials != initialCredentials ||
                clinicName != initialClinicName ||
                contactNumber != initialContactNumber ||
                street != initialStreet ||
                barangay != initialBarangay ||
                municipality != initialMunicipality

    val allValid =
        firstValid && lastValid && clinicValid && contactValid &&
                streetValid && barangayValid && municipalityValid

    var showErrors by remember { mutableStateOf(false) }

    val canSave = allValid && changed

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // ===== Doctor Info =====
        Text(
            "Doctor Information",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1D1D1D)
        )

        Spacer(Modifier.height(10.dp))

        InputField(
            value = firstName,
            onValueChange = { firstName = it },
            placeholder = "First Name",
            iconVector = Icons.Outlined.LocalHospital,
            isPassword = false,
            errorMessage = if (showErrors && !firstValid) "First name is required" else null,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(10.dp))

        InputField(
            value = lastName,
            onValueChange = { lastName = it },
            placeholder = "Last Name",
            iconVector = Icons.Outlined.LocalHospital,
            isPassword = false,
            errorMessage = if (showErrors && !lastValid) "Last name is required" else null,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(10.dp))

        InputField(
            value = credentials,
            onValueChange = { credentials = it },
            placeholder = "Credentials (e.g. MD, FPDS)",
            iconVector = Icons.Outlined.LocalHospital,
            isPassword = false,
            errorMessage = null, // optional field, no error
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(20.dp))

        // ===== Clinic Info =====
        Text(
            "Clinic Information",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1D1D1D)
        )

        Spacer(Modifier.height(10.dp))

        InputField(
            value = clinicName,
            onValueChange = { clinicName = it },
            placeholder = "Clinic / Institution Name",
            iconVector = Icons.Outlined.LocalHospital,
            isPassword = false,
            errorMessage = if (showErrors && !clinicValid) "Clinic name is required" else null,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(10.dp))

        InputField(
            value = contactNumber,
            onValueChange = {
                contactNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }
            },
            placeholder = "Phone Number",
            iconVector = Icons.Outlined.Phone,
            isPassword = false,
            errorMessage = if (showErrors && !contactValid) "Contact number is required" else null,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "Clinic Address",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1D1D1D)
        )

        Spacer(Modifier.height(8.dp))

        // Street / Bldg
        InputField(
            value = street,
            onValueChange = { street = it },
            placeholder = "Street / Building",
            iconVector = Icons.Outlined.LocalHospital,
            isPassword = false,
            errorMessage = if (showErrors && !streetValid) "Street / Building is required" else null,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(8.dp))

        // Barangay
        InputField(
            value = barangay,
            onValueChange = { barangay = it },
            placeholder = "Barangay",
            iconVector = Icons.Outlined.LocalHospital,
            isPassword = false,
            errorMessage = if (showErrors && !barangayValid) "Barangay is required" else null,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(8.dp))

        // Municipality
        InputField(
            value = municipality,
            onValueChange = { municipality = it },
            placeholder = "Municipality",
            iconVector = Icons.Outlined.LocalHospital,
            isPassword = false,
            errorMessage = if (showErrors && !municipalityValid) "Municipality is required" else null,
            modifier = Modifier
                .wrapContentWidth()
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(8.dp))

        // Province: Pampanga (same box-style UI)
        Text(
            text = "Province",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF5F5F5),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = province,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF484848)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(
                text = "Save Info",
                onClick = {
                    showErrors = true
                    if (!allValid) return@PrimaryButton

                    val fullAddress = listOf(street, barangay, municipality, province)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")

                    onSaveAll(
                        firstName.trim(),
                        lastName.trim(),
                        credentials.trim(),
                        clinicName.trim(),
                        contactNumber.trim(),
                        fullAddress
                    )
                },
                enabled = canSave,   // ✅ only enabled when valid + changed
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )

            SecondaryButton(
                text = "Reset",
                onClick = {
                    firstName = initialFirstName
                    lastName = initialLastName
                    credentials = initialCredentials
                    clinicName = initialClinicName
                    contactNumber = initialContactNumber
                    street = initialStreet
                    barangay = initialBarangay
                    municipality = initialMunicipality
                    showErrors = false
                },
                enabled = changed,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )
        }
    }
}

@Composable
private fun DermaAccountIdentityRow(
    email: String,
    isGoogleAccount: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 62.dp, height = 57.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFCDFFFF)),
            contentAlignment = Alignment.Center
        ) {
            if (isGoogleAccount) {
                Image(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = "Google Icon",
                    modifier = Modifier.size(28.dp)
                )
            } else {
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
            Text(
                text = if (isGoogleAccount) "Google Account" else "Email",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
    }
}
