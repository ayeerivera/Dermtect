package com.example.dermtect.data

data class ManualClinic(
    val clinicName: String,
    val doctorName: String,
    val address: String,
    val contact: String = "",   // ✅ keep this
    val lat: Double,
    val lon: Double
)

object ManualClinics {
    // TODO: fill in real contact numbers if you have them
    val items = listOf(
        ManualClinic(
            clinicName = "SKINscription PH",
            doctorName = "Dr. Giselle Tioleco-Ver",
            address = "Rm. 212, City Medical Plaza, B. Mendoza, San Fernando, 2000 Pampanga",
            contact = "0910 422 8687",
            lat = 15.032012858031111,
            lon = 120.68952882430786
        ),
        ManualClinic(
            clinicName = "Dreamface",
            doctorName = "Dr. Ma. Agnes Mercado-Pineda, MD",
            address = "2nd Floor, Northwalk Complex, Jose Abad Santos Ave, San Fernando, 2000 Pampanga",
            contact = "(045) 963 9741",
            lat = 15.04305984193354,
            lon = 120.68453683779991
        ),
        ManualClinic(
            clinicName = "DERM.I.S.",
            doctorName = "Dr. Celina Faye Manalastas, MD, DPDS",
            address = "Brgy, Santo Rosario St, Angeles, 2009 Pampanga",
            contact = "0961 409 6490",
            lat = 15.12786262627396,
            lon = 120.59679993780138
        )
    )
}
