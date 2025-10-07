package com.example.dermtect.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.dermtect.R
import com.example.dermtect.ui.viewmodel.UserHomeViewModel
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Close
import com.example.dermtect.ui.viewmodel.SharedProfileViewModel
import androidx.compose.material.icons.filled.Logout
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun SettingsScreenTemplate(
    navController: NavController,
    userRole: String = "user",
    sharedProfileViewModel: SharedProfileViewModel,
    onLogout: () -> Unit = {}
) {
    var showPhoto by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showNotifTurnOffDialog by remember { mutableStateOf(false) }

    val userHomeViewModel: UserHomeViewModel = viewModel()
    val firstName by userHomeViewModel.firstName.collectAsState()
    val lastName by userHomeViewModel.lastName.collectAsState()
    val email by userHomeViewModel.email.collectAsState()
    val isGoogleAccount by userHomeViewModel.isGoogleAccount.collectAsState()
    val selectedImageUri by sharedProfileViewModel.selectedImageUri.collectAsState()
    val role by userHomeViewModel.role.collectAsState(initial = null)
    val collection = if (role == "derma") "dermas" else "users"
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

// ✅ initialize toggle based on actual permission
    var notificationsEnabled by remember(context) {
        mutableStateOf(isNotificationPermissionGranted(context))
    }

// ✅ request permission; when result returns, update the toggle
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = granted
    }

// ✅ when we return from system dialog / app settings, re-check permission
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = isNotificationPermissionGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        sharedProfileViewModel.loadPhoto("users")
        userHomeViewModel.fetchUserInfo()
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF5F5F5))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp) // adjust as needed
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
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clickable { showPhoto = true },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Only the image is clipped, not the whole box
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

                Text(
                    text = "$firstName $lastName",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }

        }
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .offset(y = (-20).dp)
                    .fillMaxWidth(0.9f)
                    .wrapContentHeight()
                    .shadow(8.dp, RoundedCornerShape(36.dp)),
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


                    SettingsRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "User",
                                tint = Color(0xFF0FB2B2),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = "Profile",
                        onClick = {
                            val encodedFirstName = Uri.encode(firstName)
                            val encodedLastName = Uri.encode(lastName)
                            val encodedEmail = Uri.encode(email)

                            navController.navigate("profile/$encodedFirstName/$encodedLastName/$encodedEmail/$isGoogleAccount/$userRole")
                        }
                    )

                    if (userRole == "user") {
                        SettingsRow(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "User",
                                    tint = Color(0xFF0FB2B2),
                                    modifier = Modifier.size(28.dp)
                                )
                            },
                            label = "About",
                            onClick = {
                                navController.navigate("about")
                            }
                        )
                    }


                    NotificationRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF0FB2B2),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = "Notification",
                        checked = notificationsEnabled,
                        onCheckedChange = { newValue ->
                            if (newValue) {
                                // Turn ON
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notificationsEnabled = true
                                }
                            } else {
                                // Ask first before jumping to Settings
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    showNotifTurnOffDialog = true
                                } else {
                                    notificationsEnabled = false
                                }
                            }
                        },
                                onClick = {
                            navController.navigate("notifications")
                        }
                    )


                    SettingsRow(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.Logout,
                                contentDescription = "Logout",
                                tint = Color(0xFF0FB2B2),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = "Logout",
                        onClick = {
                            showLogoutDialog = true}
                    )
                }
            }
        }
    }

    if (showPhoto) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000)) // semi-transparent backdrop
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
        show = showLogoutDialog,
        title = "Confirm logout?",
        primaryText = "Yes, Logout",
        onPrimary = onLogout,
        secondaryText = "Stay logged in",
        onSecondary = { showLogoutDialog = false },
        onDismiss = { showLogoutDialog = false }
    )
    DialogTemplate(
        show = showNotifTurnOffDialog,
        title = "Turn off notifications?",
        description = "You’ll stop receiving alerts from Dermtect.",
        primaryText = "Confirm",
        onPrimary = {
            // Reflect OFF locally
            notificationsEnabled = false
            // Optional: take them to app settings if they want to fully disable it
            openAppNotificationSettings(context)
            showNotifTurnOffDialog = false
        },
        secondaryText = "Cancel",
        onSecondary = {
            // Keep it ON
            notificationsEnabled = true
            showNotifTurnOffDialog = false
        },
        onDismiss = {
            // Keep it ON if dismissed
            notificationsEnabled = true
            showNotifTurnOffDialog = false
        }
    )
}
@Composable
fun AccountInfoRow(email: String, isGoogleAccount: Boolean) {
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
fun SettingsRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
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
fun NotificationRow(
    icon: @Composable () -> Unit,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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

        Image(
            painter = painterResource(
                id = if (checked) R.drawable.toggle_on else R.drawable.toggle_off
            ),
            contentDescription = if (checked) "Enabled" else "Disabled",
            modifier = Modifier
                .size(width = 40.dp, height = 20.dp)
                .clickable { onCheckedChange(!checked) }
        )
    }
}
private fun isNotificationPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

private fun openAppNotificationSettings(context: Context) {
    val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    context.startActivity(intent)
}
