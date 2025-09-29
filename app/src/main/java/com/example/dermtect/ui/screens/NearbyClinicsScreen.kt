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
import androidx.compose.runtime.*
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
import com.example.dermtect.ui.viewmodel.OverpassClinicsViewModel
import com.example.dermtect.ui.viewmodel.UserHomeViewModel
import com.google.gson.Gson
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import com.example.dermtect.data.ManualClinics
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.app.ActivityCompat


@Composable
fun NearbyClinicsScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    viewModel: UserHomeViewModel, // keep for your other features if needed
    overpassVm: OverpassClinicsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    // You can remove this if your Firestore clinics aren’t used here anymore:
    LaunchedEffect(Unit) { viewModel.fetchClinics() }

    val isLoadingClinics by viewModel.isLoadingClinics.collectAsState()
    val clinicList by viewModel.clinicList.collectAsState()

    val user by overpassVm.user.collectAsState()
    val osmClinics by overpassVm.clinics.collectAsState()

    var showLocationDialog by remember { mutableStateOf(false) }
    var requestPermissionNow by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

        if (granted) {
            overpassVm.loadUserLocation()
            // optional: fetch nearby after we (soon) have a location
            // a small delay isn’t strictly necessary if your VM updates `user`
            overpassVm.fetchNearbyDermatology(radiusMeters = 5000)
        } else {
            // user denied — do nothing or show a toast/snackbar if you like
        }
    }

    val activity = context as Activity
    var showOpenSettings by remember { mutableStateOf(false) }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun shouldShowRationale(): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

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
                        .clickable { showLocationDialog = true }
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
                        color = Color.Black,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(25.dp))

                // === REPLACE STATIC IMAGE WITH REAL MAP ===
                AndroidView(
                    modifier = Modifier
                        .size(width = 335.dp, height = 248.dp)
                        .clip(RoundedCornerShape(11.dp)),
                    factory = { ctx ->
                        org.osmdroid.views.MapView(ctx).apply {
                            setMultiTouchControls(true)
                            controller.setZoom(14.0)
                            // Default center until we have user location
                            controller.setCenter(GeoPoint(14.5995, 120.9842))
                        }
                    },


                    update = { map ->
                        // 1) Clear all markers first
                        map.overlays.removeAll { it is Marker }

                        // 2) If we already have a user, center and draw a RED pin
                        user?.let { u ->
                            map.controller.setCenter(GeoPoint(u.lat, u.lon))

                            val ctx = map.context
                            val base = ContextCompat.getDrawable(ctx, org.osmdroid.library.R.drawable.marker_default)?.mutate()
                            val redIcon = base?.also { DrawableCompat.setTint(it, android.graphics.Color.RED) }

                            val me = Marker(map).apply {
                                position = GeoPoint(u.lat, u.lon)
                                title = "You are here"
                                icon = redIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            map.overlays.add(me)
                        } ?: run {
                            // No user yet → keep your Manila default
                            map.controller.setCenter(GeoPoint(14.5995, 120.9842))
                        }

                        // 3) Add clinic pins (Overpass first)
                        if (osmClinics.isNotEmpty()) {
                            osmClinics.forEach { c ->
                                val marker = Marker(map).apply {
                                    position = GeoPoint(c.lat, c.lon)
                                    title = c.name
                                    snippet = buildString {
                                        append(String.format("%.2f km away", c.distanceMeters / 1000.0))
                                        c.address?.let { append("\n$it") }
                                    }
                                    setOnMarkerClickListener { m, mv ->
                                        m.showInfoWindow(); mv.controller.animateTo(m.position); true
                                    }
                                }
                                map.overlays.add(marker)
                            }
                        } else {
                            // Manual fallback
                            ManualClinics.items.forEach { c ->
                                val marker = Marker(map).apply {
                                    position = GeoPoint(c.lat, c.lon)
                                    title = c.name
                                    snippet = c.address
                                    setOnMarkerClickListener { m, mv ->
                                        m.showInfoWindow(); mv.controller.animateTo(m.position); true
                                    }
                                }
                                map.overlays.add(marker)
                            }
                        }

                        map.invalidate()

                    }

                )

                Spacer(modifier = Modifier.height(30.dp))

                Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.width(335.dp))

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.width(335.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nearby Clinics",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (showLocationDialog) {
                            showLocationDialog = false

                            val fineGranted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED

                            if (fineGranted || coarseGranted) {
                                overpassVm.loadUserLocation()
                                overpassVm.fetchNearbyDermatology(radiusMeters = 5000)
                            } else {
                                permissionLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            }
                }

                if (showOpenSettings) {
                    AlertDialog(
                        onDismissRequest = { showOpenSettings = false },
                        title = { Text("Enable Location Permission") },
                        text = { Text("Location is disabled and can’t be requested again. Open App Settings to allow it.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showOpenSettings = false
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }) { Text("Open Settings") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOpenSettings = false }) { Text("Cancel") }
                        }
                    )
                }


                // You can keep your existing Firestore list OR switch to osmClinics below:
                val finalClinics = when {
                    osmClinics.isNotEmpty() -> {
                        osmClinics.map {
                            Triple(
                                it.name,
                                it.address ?: "No address",
                                String.format("%.2fkm", it.distanceMeters / 1000.0)
                            )
                        }
                    }
                    ManualClinics.items.isNotEmpty() -> {
                        val u = user
                        val sortedManual = if (u != null) {
                            ManualClinics.items.sortedBy {
                                distanceMeters(u.lat, u.lon, it.lat, it.lon)
                            }
                        } else ManualClinics.items

                        sortedManual.map {
                            val dist = user?.let { u ->
                                distanceMeters(u.lat, u.lon, it.lat, it.lon) / 1000.0
                            }?.let { "%.2fkm".format(it) } ?: "—"
                            Triple(it.name, it.address, dist)
                        }
                    }
                    else -> {
                        clinicList.map { Triple(it.name, it.address, "—") }
                    }
                }

                if (isLoadingClinics && osmClinics.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00BBD3))
                    }
                } else if (finalClinics.isEmpty()) {
                    Text("No clinics found.", color = Color.Gray)
                } else {
                    finalClinics.forEach { (name, address, dist) ->
                        ClinicItem(
                            name = name,
                            address = address,
                            distance = dist,
                            onClick = {
                                // Navigate as you already do
                                // Here you could pass lat/lon too if you map from osmClinics
                                val gson = Gson()
                                val clinicJson = Uri.encode(gson.toJson(mapOf("name" to name, "address" to address)))
                                navController.navigate("clinic_detail/$clinicJson")
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun distanceMeters(
    userLat: Double, userLon: Double,
    targetLat: Double, targetLon: Double
): Double {
    val R = 6371000.0
    val dLat = Math.toRadians(targetLat - userLat)
    val dLon = Math.toRadians(targetLon - userLon)
    val aLat = Math.toRadians(userLat)
    val bLat = Math.toRadians(targetLat)
    val h = kotlin.math.sin(dLat/2) * kotlin.math.sin(dLat/2) +
            kotlin.math.cos(aLat) * kotlin.math.cos(bLat) *
            kotlin.math.sin(dLon/2) * kotlin.math.sin(dLon/2)
    return 2 * R * kotlin.math.asin(kotlin.math.sqrt(h))
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
