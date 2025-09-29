package com.example.dermtect.data.repository
import com.example.dermtect.data.model.ClinicPin

object ClinicPinsRepository {
    // Clinics
    val pins = listOf(
        ClinicPin("Derma A", "123 Sample St, QC", 14.6440, 121.0320),
        ClinicPin("Derma B", "456 Test Ave, Makati", 14.5534, 121.0330),
        ClinicPin("Derma C", "789 Skin Rd, Manila", 14.5995, 120.9842)
    )
}