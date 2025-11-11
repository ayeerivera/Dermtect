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
import androidx.core.app.ActivityCompat
import org.osmdroid.config.Configuration
import android.widget.Toast
import org.osmdroid.util.BoundingBox // 🔹 added

@Composable
fun NearbyClinicsScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    viewModel: UserHomeViewModel,
    overpassVm: OverpassClinicsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    LaunchedEffect(Unit) { viewModel.fetchClinics() }

    val isLoadingClinics by viewModel.isLoadingClinics.collectAsState()
    val clinicList by viewModel.clinicList.collectAsState()

    val user by overpassVm.user.collectAsState()
    val osmClinics by overpassVm.clinics.collectAsState()

    var requestPermissionNow by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

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

    // =======================================
    // 🔹 FIXED permission launcher
    // =======================================
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

        if (granted) {
            overpassVm.loadUserLocation()
            overpassVm.fetchNearbyDermatology(radiusMeters = 5000)
        } else {
            if (shouldShowRationale()) {
                Toast.makeText(
                    context,
                    "Location access is needed to find clinics near you.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // User tapped "Don't ask again"
                showOpenSettings = true
            }
        }
    }

    // 🔹 Auto-load if already granted
    LaunchedEffect(Unit) {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) {
            overpassVm.loadUserLocation()
        }
    }

    LaunchedEffect(user) {
        user?.let { overpassVm.fetchNearbyDermatology(radiusMeters = 5000) }
    }

    // Track last “fit set” so we don’t keep re-fitting unless points changed
    val lastFitSignature = remember { mutableStateOf<String?>(null) }

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
                // =======================================
                // 🔹 Turn On Location button (unchanged)
                // =======================================
                Box(
                    modifier = Modifier
                        .clickable {
                            when {
                                hasLocationPermission() -> {
                                    overpassVm.loadUserLocation()
                                    overpassVm.fetchNearbyDermatology(radiusMeters = 5000)
                                }
                                shouldShowRationale() -> {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )                                }
                                else -> {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
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
                        text = "Turn On Location",
                        fontSize = 14.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(25.dp))

                // === Map view (UI unchanged; internals updated to fit bounds) ===
                AndroidView(
                    modifier = Modifier
                        .size(width = 335.dp, height = 248.dp)
                        .clip(RoundedCornerShape(11.dp)),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        org.osmdroid.views.MapView(ctx).apply {
                            setMultiTouchControls(true)
                            setTilesScaledToDpi(true)
                            isHorizontalMapRepetitionEnabled = true
                            controller.setZoom(14.0)
                            controller.setCenter(GeoPoint(14.5995, 120.9842)) // default (Manila)

                            setOnTouchListener { v, e ->
                                when (e.actionMasked) {
                                    android.view.MotionEvent.ACTION_DOWN,
                                    android.view.MotionEvent.ACTION_MOVE,
                                    android.view.MotionEvent.ACTION_POINTER_DOWN -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(true)
                                    }
                                    android.view.MotionEvent.ACTION_UP,
                                    android.view.MotionEvent.ACTION_CANCEL -> {
                                        v.parent?.requestDisallowInterceptTouchEvent(false)
                                    }
                                }
                                false
                            }
                        }
                    },
                    update = { map ->
                        // Clear old markers
                        map.overlays.removeAll { it is Marker }

                        // Collect all points we want to keep in view
                        val points = mutableListOf<GeoPoint>()

                        // User marker (red)
                        user?.let { u ->
                            val ctx = map.context
                            val redIcon = ContextCompat.getDrawable(ctx, R.drawable.location_icon)
                                ?.mutate()
                                ?.also { DrawableCompat.setTint(it, android.graphics.Color.RED) }

                            val me = Marker(map).apply {
                                position = GeoPoint(u.lat, u.lon)
                                title = "You are here"
                                icon = redIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            map.overlays.add(me)
                            points += me.position
                        }

                        // Clinics (OSM first; else manual; else fallback list)
                        if (osmClinics.isNotEmpty()) {
                            osmClinics.forEach { c ->
                                val marker = Marker(map).apply {
                                    position = GeoPoint(c.lat, c.lon)
                                    title = c.name
                                    snippet = String.format("%.2f km away", c.distanceMeters / 1000.0)
                                    setOnMarkerClickListener { m, mv ->
                                        m.showInfoWindow()
                                        mv.controller.animateTo(m.position)
                                        true
                                    }
                                }
                                map.overlays.add(marker)
                                points += marker.position
                            }
                        } else if (ManualClinics.items.isNotEmpty()) {
                            val u = user
                            ManualClinics.items.forEach { c ->
                                val distKm = u?.let { uu ->
                                    distanceMeters(uu.lat, uu.lon, c.lat, c.lon) / 1000.0
                                }
                                val marker = Marker(map).apply {
                                    position = GeoPoint(c.lat, c.lon)
                                    title = c.clinicName
                                    snippet = buildString {
                                        c.doctorName?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
                                        distKm?.let { append(String.format("%.2f km away", it)) }
                                    }.trim()
                                    setOnMarkerClickListener { m, mv ->
                                        m.showInfoWindow()
                                        mv.controller.animateTo(m.position)
                                        true
                                    }
                                }
                                map.overlays.add(marker)
                                points += marker.position
                            }
                        } else {
                            // Fallback Firestore list (no coordinates => just leave the map as-is)
                            // (Your list below handles the textual list)
                            if (user != null) {
                                // keep user-centered if no clinic coords
                                // (handled by fit logic below)
                            }
                        }

                        // ==============================
                        // 🔹 Auto-fit: center user & show ALL clinics on first render/when set changes
                        // ==============================
                        if (points.isNotEmpty()) {
                            val signature = buildString {
                                points.sortedBy { it.latitude * 1000 + it.longitude }.forEach {
                                    append("%.5f,%.5f|".format(it.latitude, it.longitude))
                                }
                            }
                            if (signature != lastFitSignature.value) {
                                // New/changed set => fit bounds
                                val bbox = BoundingBox.fromGeoPointsSafe(points)
                                // Small padding in degrees via a tiny expansion (about ~1–2%):
                                val padLat = (bbox.latNorth - bbox.latSouth) * 0.10
                                val padLon = (bbox.lonEast - bbox.lonWest) * 0.10

                                val padded = BoundingBox(
                                    bbox.latNorth + padLat,
                                    bbox.lonEast + padLon,
                                    bbox.latSouth - padLat,
                                    bbox.lonWest - padLon
                                )
                                // Animate to show everything
                                map.zoomToBoundingBox(padded, true)
                                lastFitSignature.value = signature
                            }
                        } else {
                            // Nothing to fit yet: center default (or user if present)
                            if (user != null) {
                                map.controller.setZoom(15.0)
                                map.controller.setCenter(GeoPoint(user!!.lat, user!!.lon))
                            } else {
                                map.controller.setZoom(12.0)
                                map.controller.setCenter(GeoPoint(14.5995, 120.9842))
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
                        fontSize = 18.sp,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                data class ListItem(
                    val title: String,
                    val subtitle: String?,
                    val address: String,
                    val distance: String,
                    val contact: String?
                )

                val finalClinics: List<ListItem> = when {
                    osmClinics.isNotEmpty() -> {
                        osmClinics.map {
                            ListItem(
                                it.name,
                                null,
                                it.address ?: "No address",
                                String.format("%.2fkm", it.distanceMeters / 1000.0),
                                null
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
                            val dist = user?.let { u2 ->
                                distanceMeters(u2.lat, u2.lon, it.lat, it.lon) / 1000.0
                            }?.let { "%.2fkm".format(it) } ?: "—"
                            ListItem(it.clinicName, it.doctorName, it.address, dist, it.contact)
                        }
                    }
                    else -> {
                        clinicList.map {
                            ListItem(it.name, null, it.address, "—", null)
                        }
                    }
                }

                if (isLoadingClinics && osmClinics.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00BBD3))
                    }
                } else if (finalClinics.isEmpty()) {
                    Text("No clinics found.", color = Color.Gray)
                } else {
                    finalClinics.forEach { item ->
                        ClinicItem(
                            title = item.title,
                            subtitle = item.subtitle,
                            distance = item.distance,
                            onClick = {
                                val gson = Gson()
                                val clinicJson = Uri.encode(
                                    gson.toJson(
                                        mapOf(
                                            "name" to item.title,
                                            "subtitle" to (item.subtitle ?: ""),
                                            "address" to item.address,
                                            "contact" to (item.contact ?: "")
                                        )
                                    )
                                )
                                navController.navigate("clinic_detail/$clinicJson")
                            }
                        )
                    }
                }
                // =======================================
                // 🔹 Dialog for "Don't ask again" (open Settings)
                // =======================================
                if (showOpenSettings) {
                    AlertDialog(
                        onDismissRequest = { showOpenSettings = false },
                        title = { Text("Enable Location in Settings") },
                        text = { Text("To find nearby clinics, please enable location permission in Settings > Apps > Dermtect > Permissions.") },
                        confirmButton = {
                            TextButton(onClick = {
                                showOpenSettings = false
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null)
                                )
                                context.startActivity(intent)
                            }) { Text("Open Settings") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOpenSettings = false }) {
                                Text("Cancel")
                            }
                        }
                    )
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
    val h = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(aLat) * kotlin.math.cos(bLat) *
            kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
    return 2 * R * kotlin.math.asin(kotlin.math.sqrt(h))
}

@Composable
fun ClinicItem(
    title: String,
    subtitle: String?,
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
                    .size(22.dp)
                    .padding(end = 6.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E2E2E)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .height(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(0.5.dp, Color.Black, shape = RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = distance,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
            }
        }
    }
}
