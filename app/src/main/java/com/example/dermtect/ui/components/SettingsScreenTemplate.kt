package com.example.dermtect.ui.components

import com.example.dermtect.ui.viewmodel.AuthViewModel
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
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory
import com.example.dermtect.ui.viewmodel.UserHomeViewModel
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.ColorFilter


@Composable
fun SettingsScreenTemplate(
    navController: NavController,
    userRole: String = "user",
    onLogout: () -> Unit = {}
) {
    var showPhoto by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())
    val userHomeViewModel: UserHomeViewModel = viewModel()
    val firstName by userHomeViewModel.firstName.collectAsState()
    val lastName by userHomeViewModel.lastName.collectAsState()
    val email by userHomeViewModel.email.collectAsState()
    val isGoogleAccount by userHomeViewModel.isGoogleAccount.collectAsState()

    LaunchedEffect(Unit) {
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

                Image(
                    painter = painterResource(id = R.drawable.profile),
                    contentDescription = "Profile",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .clickable { showPhoto = true },
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(15.dp))

                Text(
                    text = "$firstName $lastName",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
            Card(
                modifier = Modifier
                    .offset(x = 25.dp, y = 344.dp)
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
                            val encodedEmail = Uri.encode(email)
                            navController.navigate("profile/${firstName}/${lastName}/${encodedEmail}/${isGoogleAccount}/$userRole")
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
                                imageVector = 	Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFF0FB2B2),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = "Notification",
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it },
                        onClick = {
                            navController.navigate("notifications")
                        }
                    )


                    Spacer(modifier = Modifier.height(15.dp))

                    LogoutRow {
                        showLogoutDialog = true
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
                        Image(
                            painter = painterResource(id = R.drawable.profile),
                            contentDescription = "Full Photo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
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
                            painter = painterResource(id = R.drawable.x_icon), // replace with your actual 'X' icon
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

@Composable
fun LogoutRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Icon(
            painter = painterResource(id = R.drawable.logout_icon),
            contentDescription = "Logout",
            tint = Color(0xFF00B2B2),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Logout",
            color = Color(0xFF484848),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal)
        )
    }
}