package com.example.dermtect.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dermtect.R
import com.example.dermtect.ui.viewmodel.UserHomeViewModel
import com.google.gson.Gson
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue


@Composable
fun NearbyClinicsScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    viewModel: UserHomeViewModel // ✅ Add this
) {
    LaunchedEffect(Unit) {
        viewModel.fetchClinics()
    }

    val isLoadingClinics by viewModel.isLoadingClinics.collectAsState()
    val clinicList by viewModel.clinicList.collectAsState()
    var showLocationDialog by remember { mutableStateOf(false) }

    BubblesBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp, bottom = 10.dp)
                ) {
                    BackButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 23.dp)
                    )

                    Text(
                        text = "Find Nearest Clinics",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                    )
                }
            }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 120.dp, start = 20.dp, end = 20.dp, bottom = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "+ Add your Address",
                        fontSize = 14.sp,
                        color = Color(0xFF1D1D1D),
                        modifier = Modifier.align(Alignment.Start).padding(start = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .clickable {
                                showLocationDialog = true
                            }
                            .size(width = 335.dp, height = 44.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                            .border(
                                width = 1.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF33E4DB), Color(0xFF00BBD3))
                                ),
                                shape = RoundedCornerShape(18.dp)
                            ),

                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "Turn On Location or Manually Enter",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Black,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(25.dp))

                    Image(
                        painter = painterResource(id = R.drawable.map1),
                        contentDescription = "Map",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(width = 335.dp, height = 248.dp)
                            .clip(RoundedCornerShape(11.dp))
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Divider(
                        color = Color.LightGray,
                        thickness = 1.dp,
                        modifier = Modifier.width(335.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.width(335.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nearby Clinics",//saved clinics
                            fontSize = 14.sp,
                            color = Color.Black,
                            modifier = Modifier.weight(1f)
                        )
//                        Icon(
//                            painter = painterResource(id = R.drawable.heart_icon),
//                            contentDescription = "Saved",
//                            tint = Color(0xFF0FB2B2),
//                            modifier = Modifier.size(width = 22.dp, height = 22.dp)
//                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (showLocationDialog) {
                        DialogTemplate(
                            show = showLocationDialog,
                            title = "Allow DermTect to access this device’s location?",
                            imageResId = R.drawable.map_dialog,
                            primaryText = "While using the app",
                            onPrimary = {
                                // TODO: Replace this with real location permission logic later
                                showLocationDialog = false
                            },
                            secondaryText = "Only this time",
                            onSecondary = {
                                // TODO: Replace this with real one-time permission logic later
                                showLocationDialog = false
                            },
                            tertiaryText = "Don’t allow",
                            onTertiary = {
                                showLocationDialog = false
                            },
                            onDismiss = {
                                showLocationDialog = false
                            }
                        )
                    }

                    if (isLoadingClinics) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF00BBD3))
                        }
                    } else if (clinicList.isEmpty()) {
                        Text("No clinics found.", color = Color.Gray)
                    } else {
                        clinicList.forEach { clinic ->
                            ClinicItem(
                                name = clinic.name,
                                address = clinic.address,
                                distance = "2.3km",
                                onClick = {
                                    val gson = Gson()
                                    val clinicJson = Uri.encode(gson.toJson(clinic))
                                    navController.navigate("clinic_detail/$clinicJson")
                                }
                            )
                        }
                    }
                }
            }
        }
}

@Composable
fun ClinicItem(
    name: String,
    address: String,
    distance: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .background(Color.White)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = R.drawable.location_icon),
                contentDescription = "Location Icon",
                modifier = Modifier
                    .size(20.dp)
                    .padding(end = 6.dp)
            )

            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(0.5.dp, Color.Black, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = distance,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = address,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF484848),
            modifier = Modifier.fillMaxWidth(0.9f)
        )

    }
}
