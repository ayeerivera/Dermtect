package com.example.dermtect.ui.screens

import android.util.Log
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dermtect.ui.components.BubblesBackground
import com.example.dermtect.model.Clinic
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.style.TextAlign
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.viewmodel.UserHomeViewModel
import androidx.compose.runtime.getValue


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
    Log.d("FirestoreFetch", "clinic.mapImage = ${clinic.mapImage}")

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = clinic.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,                            contentDescription = "Favorite",
                            tint = Color.Cyan,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    viewModel.toggleClinicSave(clinic.id)
                                    viewModel.fetchSavedClinics()
                                }

                        )

                    }

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        text = clinic.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(text = clinic.description, style = MaterialTheme.typography.bodyMedium,
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)){
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
                    Row (
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)){
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
                    Spacer(modifier = Modifier.height(15.dp))

                    Row( modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Consultation Schedule:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = clinic.schedule,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))


                if (mapResId != 0) {
                    Image(
                        painter = painterResource(id = mapResId),
                        contentDescription = "Clinic Map",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(279.dp)
                            .clip(RoundedCornerShape(11.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "Map image not available.",
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }


                }
            }
        }
    }

@Composable
fun InlineLabelText(label: String, value: String) {
    Row {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Text(
            text = value,
            fontSize = 13.sp
        )
    }
}
