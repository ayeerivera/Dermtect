package com.example.dermtect.ui.viewmodel

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Simple LatLng to avoid importing maps libs just for a pair */
data class LatLng(val lat: Double, val lon: Double)

data class ClinicUi(
    val name: String,
    val address: String?,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Double
)

class OverpassClinicsViewModel(app: Application) : AndroidViewModel(app) {

    private val fused = LocationServices.getFusedLocationProviderClient(app)
    private val http = OkHttpClient()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val _user = MutableStateFlow<LatLng?>(null)
    val user: StateFlow<LatLng?> = _user

    private val _clinics = MutableStateFlow<List<ClinicUi>>(emptyList())
    val clinics: StateFlow<List<ClinicUi>> = _clinics

    // ✅ Only update user location if it actually changed (reduces UI recentering)
    private fun updateUserIfChanged(new: LatLng) {
        val cur = _user.value
        val thresholdMeters = 15.0
        if (cur == null || haversine(cur.lat, cur.lon, new.lat, new.lon) >= thresholdMeters) {
            _user.value = new
        }
    }

    @SuppressLint("MissingPermission") // we gate this with runtime checks below
    fun loadUserLocation() {
        val ctx = getApplication<Application>()
        val fine = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return

        fused.lastLocation.addOnSuccessListener { loc ->
            loc?.let { updateUserIfChanged(LatLng(it.latitude, it.longitude)) }
        }
    }

    /**
     * Fetch nearby dermatology clinics via Overpass (OpenStreetMap).
     * radiusMeters: e.g., 5000 = 5km
     */
    fun fetchNearbyDermatology(radiusMeters: Int = 5000) {
        val center = _user.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val query = """
                    [out:json][timeout:25];
                    (
                      node["healthcare"="dermatology"](around:$radiusMeters,${center.lat},${center.lon});
                      way ["healthcare"="dermatology"](around:$radiusMeters,${center.lat},${center.lon});
                      node["amenity"="clinic"]["healthcare:speciality"~"dermatology|skin|cosmetic|aesthetic", i](around:$radiusMeters,${center.lat},${center.lon});
                      way ["amenity"="clinic"]["healthcare:speciality"~"dermatology|skin|cosmetic|aesthetic", i](around:$radiusMeters,${center.lat},${center.lon});
                    );
                    out center tags 80;
                """.trimIndent()

                val req = Request.Builder()
                    .url("https://overpass-api.de/api/interpreter")
                    .post(query.toRequestBody("text/plain; charset=utf-8".toMediaType()))
                    .header("User-Agent", "DermTect/${getApplication<Application>().packageName}")
                    .build()

                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val body = resp.body?.string() ?: return@use

                    // ✅ Tell the deserializer the target type explicitly
                    val parsed: OverpassResponse = json.decodeFromString(body)

                    val result: List<ClinicUi> = parsed.elements.mapNotNull { el: OverpassElement ->
                        val lat = el.lat ?: el.center?.lat
                        val lon = el.lon ?: el.center?.lon
                        if (lat == null || lon == null) return@mapNotNull null

                        val name = el.tags["name"]
                            ?: el.tags["official_name"]
                            ?: "Dermatology Clinic"

                        val address = el.tags["addr:full"]
                            ?: listOfNotNull(
                                el.tags["addr:housenumber"],
                                el.tags["addr:street"],
                                el.tags["addr:city"]
                            ).joinToString(" ").ifBlank { null }

                        val dist = haversine(center.lat, center.lon, lat, lon)
                        ClinicUi(name, address, lat, lon, dist)
                    }.sortedBy { it.distanceMeters }

                    _clinics.value = result
                }
            } catch (_: Exception) {
                // TODO: emit an error state if needed
            }
        }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}

/** ===== Overpass DTOs ===== */
@kotlinx.serialization.Serializable
data class OverpassResponse(
    val elements: List<OverpassElement> = emptyList()
)

@kotlinx.serialization.Serializable
data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: Center? = null,
    val tags: Map<String, String> = emptyMap()
)

@kotlinx.serialization.Serializable
data class Center(
    val lat: Double? = null,
    val lon: Double? = null
)
