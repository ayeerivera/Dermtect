package com.example.dermtect.data

data class ManualClinic(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double
)

object ManualClinics {
    // TODO: replace with your real coordinates
    val items = listOf(
        ManualClinic("Dr. Giselle Tioleco-Ver", "Rm. 212, City Medical Plaza, B. Mendoza, San Fernando, 2000 Pampanga", 15.032012858031111, 120.68952882430786),
        ManualClinic("Dr. Ma. Agnes Mercado-Pineda, MD", "2nd Floor, Northwalk Complex, Jose Abad Santos Ave, San Fernando, 2000 Pampanga", 15.04305984193354, 120.68453683779991),
        ManualClinic("Dr. Celina Faye Manalastas, MD, DPDS", "Brgy, Santo Rosario St, Angeles, 2009 Pampanga", 15.12786262627396, 120.59679993780138)
    )
}
