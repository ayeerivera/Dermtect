package com.example.dermtect.ui.screens

import android.graphics.Bitmap

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ResultScreen(
    imageBitmap: Bitmap,
    prediction: String,
    probability: Float,
    riskMessage: String,
    camBitmap: Bitmap? = null,
    inputPreview: Bitmap? = null
) {
    val frameSize   = 220.dp //change if needed
    val frameShape  = RoundedCornerShape(12.dp)
    val borderColor = Color(0xFFB7FFFF)          // amber like your ref

    val base        = inputPreview ?: imageBitmap // model-input preview if available

    ResultImagePager(
        base = base,           // base = inputPreview ?: imageBitmap
        cam = camBitmap,
        frameSize = frameSize,
        frameShape = frameShape,
        borderColor = borderColor
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title/date
        Text("Scan 1", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("May 10, 2025", fontSize = 14.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .width(frameSize)               // e.g., 224.dp
                .aspectRatio(1f)                // force square
                .align(Alignment.CenterHorizontally)
                .clip(frameShape)
                .border(4.dp, borderColor, frameShape)
        ) {
            Image(
                bitmap = base.asImageBitmap(),  // base = inputPreview ?: imageBitmap
                contentDescription = "Lesion",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Prediction: $prediction",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = if (prediction == "Malignant") Color(0xFFB00020) else Color(0xFF00796B)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Risk assessment: ") }
                append(riskMessage)
            },
            fontSize = 15.sp,
            color = Color.Black
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Probability: ${"%.3f".format(probability)}",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )

        Spacer(Modifier.height(32.dp))

        camBitmap?.let { hm ->
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .width(frameSize)
                    .aspectRatio(1f)                         // square
                    .align(Alignment.CenterHorizontally)
                    .clip(frameShape)
                    .border(4.dp, borderColor, frameShape)
            ) {
                Image(
                    bitmap = base.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Image(
                    bitmap = hm.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        // Buttons (no actions yet, per your request)
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFB2EBF2)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Download Full PDF Report & Risk Assessment Questionnaires",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFC8E6C9)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Find Nearby Derma Clinics Near You",
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}


@Composable
private fun ResultImagePager(
    base: Bitmap,          // <- use Bitmap (imported), not android.graphics.Bitmap
    cam: Bitmap?,
    frameSize: Dp,
    frameShape: Shape,
    borderColor: Color
) {
    val pages = if (cam != null) 2 else 1
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pages })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(frameSize)
                .aspectRatio(1f)
                .clip(frameShape)
                .border(4.dp, borderColor, frameShape)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                if (page == 0) {
                    Image(
                        bitmap = base.asImageBitmap(),
                        contentDescription = "Lesion",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(Modifier.fillMaxSize()) {
                        Image(
                            bitmap = base.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Image(
                            bitmap = cam!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(pages) { i ->
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (i == pagerState.currentPage) Color(0xFF90A4AE) else Color(0xFFE0E0E0))
                )
            }
        }
    }
}
