package com.example.dermtect.ui.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.*

data class UserPoint(val lat: Double, val lon: Double)

class LocationViewModel(app: Application) : AndroidViewModel(app) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(app) }

    private val _user = MutableStateFlow<UserPoint?>(null)
    val user: StateFlow<UserPoint?> = _user

    /** Call after permissions are granted */
    @SuppressLint("MissingPermission")
    fun loadUserLocation() = viewModelScope.launch {
        // getCurrentLocation is modern & battery-friendly
        val loc = fused.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            null
        ).await()

        loc?.let { _user.value = UserPoint(it.latitude, it.longitude) }
            ?: run {
                // fallback to lastKnown if current failed
                val last = fused.lastLocation.await()
                last?.let { _user.value = UserPoint(it.latitude, it.longitude) }
            }
    }

    fun distanceMeters(a: UserPoint, bLat: Double, bLon: Double): Double {
        // Haversine
        val R = 6371000.0
        val dLat = Math.toRadians(bLat - a.lat)
        val dLon = Math.toRadians(bLon - a.lon)
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(bLat)
        val h = sin(dLat/2).pow(2.0) + cos(lat1)*cos(lat2)*sin(dLon/2).pow(2.0)
        return 2 * R * asin(min(1.0, sqrt(h)))
    }
}