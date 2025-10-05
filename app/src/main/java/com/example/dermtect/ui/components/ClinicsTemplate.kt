package com.example.dermtect.ui.screens

import android.location.Geocoder
import android.view.MotionEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dermtect.ui.components.BubblesBackground
import com.example.dermtect.model.Clinic
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.viewmodel.UserHomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@Composable
fun ClinicTemplateScreen(
    name: String,
    clinic: Clinic,
    onBackClick: () -> Unit,
    onToggleSave: () -> Unit,
    viewModel: UserHomeViewModel
) {
    val savedIds by viewModel.savedClinicIds.collectAsState()
    val isSaved = clinic.id in savedIds

    val context = LocalContext.current
    val mapResId = context.resources.getIdentifier(clinic.mapImage, "drawable", context.packageName)

    // Geocoded coords from address (used only if no drawable map is provided)
    var geoPoint by remember { mutableStateOf<GeoPoint?>(null) }

    LaunchedEffect(clinic.address) {
        if (mapResId == 0 && clinic.address.isNotBlank()) {
            geoPoint = withContext(Dispatchers.IO) {
                try {
                    val results = Geocoder(context, Locale.getDefault())
                        .getFromLocationName(clinic.address, 1)
                    if (!results.isNullOrEmpty()) {
                        GeoPoint(results[0].latitude, results[0].longitude)
                    } else null
                } catch (_: Throwable) {
                    null
                }
            }
        }
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
                        text = "Nearby Clinics",
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
                    .padding(top = 120.dp, start = 40.dp, end = 40.dp, bottom = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                // Title row — spacing fix by using weight(1f)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = clinic.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.Cyan,
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                viewModel.toggleClinicSave(clinic.id)
                                viewModel.fetchSavedClinics()
                            }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Doctor name (subtitle) under clinic name
                if (clinic.subtitle.isNotBlank()) {
                    Text(
                        text = clinic.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (clinic.description.isNotBlank()) {
                    Text(
                        text = clinic.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Address:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = clinic.address,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(15.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Contact Number:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = clinic.contact,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ---- MAP ----
                when {
                    // 1) If you bundled a static map image, show it
                    mapResId != 0 -> {
                        Image(
                            painter = painterResource(id = mapResId),
                            contentDescription = "Clinic Map",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(279.dp)
                                .clip(RoundedCornerShape(11.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // 2) If we got coordinates via Geocoder, show an interactive OSM map
                    geoPoint != null -> {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(279.dp)
                                .clip(RoundedCornerShape(11.dp)),
                            factory = { ctx ->
                                Configuration.getInstance().userAgentValue = ctx.packageName
                                org.osmdroid.views.MapView(ctx).apply {
                                    setMultiTouchControls(true)
                                    setTilesScaledToDpi(true)
                                    isHorizontalMapRepetitionEnabled = true
                                    controller.setZoom(16.0)
                                    controller.setCenter(geoPoint)

                                    // Make touch smooth inside a scroll view
                                    setOnTouchListener { v, e ->
                                        when (e.actionMasked) {
                                            MotionEvent.ACTION_DOWN,
                                            MotionEvent.ACTION_MOVE,
                                            MotionEvent.ACTION_POINTER_DOWN -> {
                                                v.parent?.requestDisallowInterceptTouchEvent(true)
                                            }
                                            MotionEvent.ACTION_UP,
                                            MotionEvent.ACTION_CANCEL -> {
                                                v.parent?.requestDisallowInterceptTouchEvent(false)
                                            }
                                        }
                                        false
                                    }

                                    overlays.add(
                                        Marker(this).apply {
                                            position = geoPoint
                                            title = clinic.name
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        }
                                    )
                                }
                            },
                            update = { map ->
                                map.overlays.removeAll { it is Marker }
                                geoPoint?.let { gp ->
                                    map.controller.setCenter(gp)
                                    map.overlays.add(
                                        Marker(map).apply {
                                            position = gp
                                            title = clinic.name
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        }
                                    )
                                }
                                map.invalidate()
                            }
                        )
                    }
                    // 3) Fallback
                    else -> {
                        Text(
                            text = "Map image not available.",
                            color = Color.Gray,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
                // ---- END MAP ----
            }
        }
    }
}
