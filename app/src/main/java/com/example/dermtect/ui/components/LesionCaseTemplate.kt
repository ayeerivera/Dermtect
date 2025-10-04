package com.example.dermtect.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.dermtect.R
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.components.BubblesBackground
import java.util.Locale

@Composable
fun LesionCaseTemplate(
    imageResId: Int? = null,
    imageBitmap: Bitmap? = null,
    camBitmap: Bitmap? = null,   // second photo (heatmap)
    title: String,
    timestamp: String,
    riskTitle: String,
    riskDescription: String,
    prediction: String,
    probability: Float,
    onBackClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onFindClinicClick: () -> Unit,
    // NEW:
    showPrimaryButtons: Boolean = false,   // show Save + Retake (pre-save)
    showSecondaryActions: Boolean = true,  // show the “You can also …” cards (post-save)
    onSaveClick: (() -> Unit)? = null,
    onRetakeClick: (() -> Unit)? = null,
    onImageClick: (page: Int) -> Unit = {},
    unsavedHeightFraction: Float = 0.55f,   // ~bigger before saving
    savedHeightFraction: Float = 0.35f      // smaller after saving
) {
    val riskMessage = generateTherapeuticMessage(probability)

    val frameShape = RoundedCornerShape(12.dp)
    val borderColor = Color(0xFFB7FFFF)

    // compute an animated height based on save state
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val targetHeight by animateDpAsState(
        targetValue = if (showPrimaryButtons)
            screenHeight * unsavedHeightFraction
        else
            screenHeight * savedHeightFraction,
        label = "imageHeightAnim"
    )

    BubblesBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 50.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Box(Modifier.fillMaxWidth()) {
                BackButton(onClick = onBackClick, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(18.dp))

                // --- HORIZONTAL PAGER (page 1 centered; page 2 on swipe) ---
                if (imageBitmap != null || imageResId != null) {
                    val pages = 2 // always two dots; page 2 uses dummy if no camBitmap
                    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages })

                    // Prepare a dummy heatmap bitmap if camBitmap is null
                    val secondBmp = remember(camBitmap) {
                        camBitmap ?: Bitmap.createBitmap(224, 224, Bitmap.Config.ARGB_8888).apply {
                            eraseColor(android.graphics.Color.parseColor("#5548A8")) // purple-ish dummy
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(targetHeight)                 // << animated height
                                .clickable { onImageClick(pagerState.currentPage) }                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                val bmpToShow: Bitmap? = when (page) {
                                    0 -> imageBitmap
                                    else -> secondBmp
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)           // square frame
                                            .clip(frameShape)
                                            .border(4.dp, borderColor, frameShape)
                                    ) {
                                        if (bmpToShow != null) {
                                            Image(
                                                bitmap = bmpToShow.asImageBitmap(),
                                                contentDescription = if (page == 0) "Lesion" else "Heatmap",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else if (page == 0 && imageResId != null) {
                                            Image(
                                                painter = painterResource(id = imageResId),
                                                contentDescription = "Lesion",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(pages) { i ->
                                Box(
                                    Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (i == pagerState.currentPage) Color(0xFF90A4AE)
                                            else Color(0xFFE0E0E0)
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Risk description
                Text(
                    buildAnnotatedString {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(riskTitle)
                            if (riskDescription.isNotBlank()) append(" ")
                        }
                        append(riskDescription)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                // Pre-save actions: Save / Retake (big photo)
                if (showPrimaryButtons) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onSaveClick?.invoke() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save")
                        }
                        OutlinedButton(
                            onClick = { onRetakeClick?.invoke() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Retake")
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Post-save actions (small photo)
                if (showSecondaryActions) {
                    Text(
                        text = "You can also",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))

                    ResultActionCard(
                        text = "Download Full PDF Report\n& Risk Assessment Questionnaires",
                        backgroundColor = Color(0xFFBAFFFF),
                        imageResId = R.drawable.risk_image,
                        onClick = onDownloadClick
                    )

                    Spacer(Modifier.height(16.dp))

                    ResultActionCard(
                        text = "Find Nearby Derma Clinics \nNear You",
                        backgroundColor = Color(0xFFBAFFD7),
                        imageResId = R.drawable.nearby_clinics,
                        onClick = onFindClinicClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultActionCard(
    text: String,
    backgroundColor: Color,
    imageResId: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.9f),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.6f),
                    Color.Black.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = text,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = "Skin Check Icon",
                modifier = Modifier.size(70.dp)
            )
        }
    }
}

fun generateTherapeuticMessage(
    probability: Float,
    tau: Float = 0.0112f
): String {
    val pPct = probability * 100f
    val alerted = probability >= tau
    fun fmt(x: Float) = String.format(Locale.getDefault(), "%.1f", x)

    return if (alerted) {
        when {
            pPct < 10f -> "Precautionary alert. Still very low, just monitor changes."
            pPct < 30f -> "Alert with a low chance. Non-urgent check is reasonable."
            pPct < 60f -> "Some concern. Clinical evaluation advised."
            pPct < 80f -> "Elevated chance. Please arrange a dermatology visit."
            else       -> "Higher chance. Prompt dermatologist review recommended."
        }
    } else {
        when {
            pPct < 10f -> "Very reassuring. Keep up normal skin care."
            pPct < 30f -> "Low chance. Monitor casually and share if changes appear."
            pPct < 60f -> "Unclear. Consider professional opinion."
            pPct < 80f -> "Moderate. Dermatology check would be sensible."
            else       -> "Higher. Timely dermatologist review is recommended."
        }
    }
}
