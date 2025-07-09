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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dermtect.R
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory

@Composable
fun ProfileScreenTemplate(
    navController: NavController,
    firstName: String,
    lastName: String,
    email: String,
    isGoogleAccount: Boolean,
    userRole: String = "user" // new
){

    val fullName = "$firstName $lastName"
    var showPhoto by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDeletedDialog by remember { mutableStateOf(false) }
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory())

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
                    text = "Profile",
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
                    text = fullName,
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
                Column{
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
                            navController.navigate("forgot_pass1")
                        }
                    )

                    if (userRole == "user") {
                        AssessmentRow(
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.AssignmentTurnedIn,
                                    contentDescription = "My Assessment Report",
                                    tint = Color(0xFF0FB2B2),
                                    modifier = Modifier.size(28.dp)
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
                                contentDescription = "Delete Account",
                                tint = Color(0xFF0FB2B2),
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        label = "Delete Account",
                        onClick = {
                            showDeleteDialog = true
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
                        .background(Color(0x80000000))
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
            onPrimary = {
                authViewModel.logout {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            secondaryText = "Stay logged in",
            onSecondary = { showLogoutDialog = false },
            onDismiss = { showLogoutDialog = false }
        )
        DialogTemplate(
            show = showDeleteDialog,
            title = "Delete Account?",
            description = "All your data will be permanently removed and cannot be recovered.",
            primaryText = "Yes, Delete my Account",
            onPrimary = {
                authViewModel.logout {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            secondaryText = "Cancel",
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
        }



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

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            fontSize = 16.sp,
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
                modifier = Modifier.size(width = 8.03.dp, height = 12.dp)
            )
        }
    }
}

@Composable
fun AssessmentRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
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

