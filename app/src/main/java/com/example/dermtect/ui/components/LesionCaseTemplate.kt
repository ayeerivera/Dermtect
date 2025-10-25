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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dermtect.R
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.components.BubblesBackground
import java.util.Locale
import kotlin.compareTo


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
    savedHeightFraction: Float = 0.35f,     // smaller after saving
    isSaving: Boolean = false

) {
// Which condition to show info for (null = no dialog)
    var showInfoFor by remember { mutableStateOf<String?>(null) }

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
// add near the top of LesionCaseTemplate, after other vals
    var fullImagePage by remember { mutableStateOf<Int?>(null) } // 0 = photo, 1 = heatmap

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
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
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
                                .clickable {
                                    fullImagePage = pagerState.currentPage

                                }) {
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
                // Optional title row
                // --- INLINE RISK LOGIC (fastest, no extra function) ---
                val pPct = probability * 100f
                val alerted = probability >= 0.0112f

                val idsToShow = if (!alerted) {
                    LesionIds.benignIds
                } else {
                    when {
                        pPct < 10f -> LesionIds.benignIds
                        pPct < 30f -> LesionIds.lt30Ids
                        pPct < 60f -> LesionIds.lt60Ids
                        pPct < 80f -> LesionIds.lt80Ids
                        else       -> LesionIds.gte80Ids
                    }
                }

                val summaryMessage = if (!alerted) {
                    "This scan looks reassuring, with a very low likelihood of a serious issue. " +
                            "You can continue your normal skincare routine. Just keep being mindful of your skin and how it changes over time."
                } else {
                    when {
                        pPct < 10f -> "Your result shows a very low chance of concern. This is reassuring, and there’s no need to worry. It may help to simply check your skin from time to time, just to stay aware of any changes."
                        pPct < 30f -> "Your result suggests only a low chance of concern. Everything appears fine. We encourage you to casually observe your skin every now and then, and let a doctor know if you notice something different."
                        pPct < 60f -> "We noticed some minor concern in your skin. This does not mean there is a serious issue, but talking with a doctor could provide peace of mind and helpful guidance."
                        pPct < 80f -> "Your result shows some concern. To better understand this, we recommend scheduling a skin check with a dermatologist. They can give you clearer answers and reassurance."
                        else       -> "Your result shows a higher level of concern. For your safety and peace of mind, we encourage you to visit a dermatologist soon so you can receive proper care and support."
                    }
                }

// 🟢 Risk Assessment Card (no percentage anymore)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8FAF7)
                    ),
                    border = BorderStroke(
                        1.dp,
                        Color.LightGray.copy(alpha = 0.6f)
                    )

                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Risk Assessment",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = summaryMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Black
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Possible Identifications",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
// 📄 Possible Identifications Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8FAF7)
                    ),
                    border = BorderStroke(
                        1.dp,
                        Color.LightGray.copy(alpha = 0.6f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 🏷 Title outside the boxes

// 📦 One box per identification
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            idsToShow.forEach { item ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F7)),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.6f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showInfoFor = item },
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 14.dp,
                                            vertical = 12.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Black,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp),
                                            maxLines = 2, // allow wrapping
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = "More info",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


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
                        onClick = { if (!isSaving) onSaveClick?.invoke() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSaving
                    ) {
                        Text("Save")
                    }

                    OutlinedButton(
                        onClick = { if (!isSaving) onRetakeClick?.invoke() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isSaving
                    ) {
                        Text("Retake")
                    }

                    Spacer(Modifier.height(16.dp))
                }
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
            if (fullImagePage != null) {
                Dialog(
                    onDismissRequest = { fullImagePage = null },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        decorFitsSystemWindows = false
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.10f))
                            .clickable { fullImagePage = null },
                        contentAlignment = Alignment.Center
                    ) {
                        // pick which bitmap to show
                        val bmp = when (fullImagePage) {
                            0 -> imageBitmap
                            1 -> camBitmap ?: imageBitmap
                            else -> null
                        }

                        if (bmp != null) {
                            ZoomableImage(
                                bitmap = bmp,
                                onClose = { fullImagePage = null }
                            )
                        } else {
                            Text("No image", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (isSaving) {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            // ✨ 50% opacity overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Saving your scan…",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
    // Info dialog for a tapped condition
    showInfoFor?.let { name ->
        val info = conditionInfo[name]
        AlertDialog(
            onDismissRequest = { showInfoFor = null },
            confirmButton = {
                TextButton(onClick = { showInfoFor = null }) { Text("Close",                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                ) }
            },
            title = {
                Text(
                    text = name,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = info?.what ?: "No description available.",
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(15.dp))
                    Text(
                        text = "Common signs:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = info?.symptoms ?: "—",
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
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

object LesionIds {
    val benignIds = listOf(
        "Common benign nevus",
        "Atypical/Dysplastic nevus",
        "Seborrheic keratosis",
        "Solar lentigo",
        "Lichen planus–like keratosis",
        "Dermatofibroma"
    )

    val lt30Ids = listOf(
        "Solar/actinic keratosis",
        "Squamous cell carcinoma in situ",
        "Melanoma in situ",
        "Superficial BCC",
        "Nodular BCC",
        "Indeterminate melanocytic neoplasm"
    )

    val lt60Ids = listOf(
        "Melanoma in situ, lentigo maligna",
        "Melanoma in situ, with nevus",
        "Melanoma invasive, superficial spreading",
        "Basal cell carcinoma",
        "Melanoma invasive (general)",
        "Atypical intraepithelial melanocytic proliferation"
    )

    val lt80Ids = listOf(
        "Squamous cell carcinoma, invasive",
        "Melanoma, NOS",
        "Basal cell carcinoma, general malignant group"
    )

    val gte80Ids = listOf(
        "Basal cell carcinoma, nodular",
        "Superficial basal cell carcinoma",
        "Melanoma in situ (general)",
        "Atypical/Dysplastic nevus"
    )
}
fun generateTherapeuticMessage(
    probability: Float,
    tau: Float = 0.0112f
): String {
    val pPct = probability * 100f
    val alerted = probability >= tau

    val idsToShow: List<String> = if (!alerted) {
        LesionIds.benignIds
    } else {
        when {
            pPct < 10f -> LesionIds.benignIds
            pPct < 30f -> LesionIds.lt30Ids
            pPct < 60f -> LesionIds.lt60Ids
            pPct < 80f -> LesionIds.lt80Ids
            else       -> LesionIds.gte80Ids
        }
    }
    val possibleBlock = "\n\nPossible identifications:\n" +
            idsToShow.joinToString("\n") { "• $it" }

    // --- Copy ---
    if (!alerted) {
        return "This scan looks reassuring, with a very low likelihood of a serious issue. " +
                "You can continue your normal skincare routine. Just keep being mindful of your skin and how it changes over time." +
                possibleBlock
    }
    val base = when {
        pPct < 10f -> "Your result shows a very low chance of concern. This is reassuring, and there’s no need to worry. It may help to simply check your skin from time to time, just to stay aware of any changes."
        pPct < 30f -> "Your result suggests only a low chance of concern. Everything appears fine. We encourage you to casually observe your skin every now and then, and let a doctor know if you notice something different."
        pPct < 60f -> "We noticed some minor concern in your skin. This does not mean there is a serious issue, but talking with a doctor could provide peace of mind and helpful guidance."
        pPct < 80f -> "Your result shows some concern. To better understand this, we recommend scheduling a skin check with a dermatologist. They can give you clearer answers and reassurance."
        else       -> "Your result shows a higher level of concern. For your safety and peace of mind, we encourage you to visit a dermatologist soon so you can receive proper care and support."
    }

    return base + possibleBlock
}

data class ConditionInfo(
    val what: String,
    val symptoms: String
)

val conditionInfo: Map<String, ConditionInfo> = mapOf(
    "Solar/actinic keratosis" to ConditionInfo(
        what = "A common sun-related precancerous patch caused by long-term UV exposure.",
        symptoms = "Rough or scaly spot; pink/tan; may feel sandpapery; often on sun-exposed areas."
    ),
    "Squamous cell carcinoma in situ" to ConditionInfo(
        what = "Early (in-situ) form of squamous cell skin cancer limited to the top skin layer.",
        symptoms = "Persistent red/scaly patch; may crust; slow growth; usually painless."
    ),
    "Melanoma in situ" to ConditionInfo(
        what = "Very early melanoma limited to the epidermis (top layer of skin).",
        symptoms = "Irregular borders, color variation; change in size/shape; may be flat."
    ),
    "Superficial BCC" to ConditionInfo(
        what = "A shallow type of basal cell carcinoma, the most common skin cancer.",
        symptoms = "Pink/red thin patch; may be shiny; slow-growing; may bleed easily."
    ),
    "Nodular BCC" to ConditionInfo(
        what = "A dome-shaped basal cell carcinoma that often looks pearly or translucent.",
        symptoms = "Pearly bump; visible small blood vessels; may ulcerate or bleed."
    ),
    "Indeterminate melanocytic neoplasm" to ConditionInfo(
        what = "A melanocytic lesion with uncertain behavior; biopsy may be required.",
        symptoms = "Atypical mole-like appearance; evolving features; often needs evaluation."
    ),
    "Melanoma in situ, lentigo maligna" to ConditionInfo(
        what = "A sun-damaged skin variant of melanoma in situ, often on the face.",
        symptoms = "Slowly enlarging flat brown patch with varied shades; irregular edges."
    ),
    "Melanoma in situ, with nevus" to ConditionInfo(
        what = "Melanoma in situ arising in or adjacent to a mole (nevus).",
        symptoms = "Change in a pre-existing mole: color/border/asymmetry."
    ),
    "Melanoma invasive, superficial spreading" to ConditionInfo(
        what = "The most common invasive melanoma subtype.",
        symptoms = "Asymmetric, irregular borders, multiple colors; enlarging lesion."
    ),
    "Basal cell carcinoma" to ConditionInfo(
        what = "Most common skin cancer; usually slow-growing and highly treatable.",
        symptoms = "Shiny/pearly bump or scaly patch; may bleed; non-healing sore."
    ),
    "Melanoma invasive (general)" to ConditionInfo(
        what = "Melanoma that has grown beyond the top layer of skin.",
        symptoms = "ABCDE changes (Asymmetry, Border, Color, Diameter, Evolving)."
    ),
    "Atypical intraepithelial melanocytic proliferation" to ConditionInfo(
        what = "Atypical melanocytic growth within the epidermis; needs clinicopathologic correlation.",
        symptoms = "Atypical, changing pigmented patch; biopsy is often recommended."
    ),
    "Squamous cell carcinoma, invasive" to ConditionInfo(
        what = "A common skin cancer that can grow deeper and rarely spread.",
        symptoms = "Firm/red nodule or scaly patch that may crust or bleed; sun-exposed sites."
    ),
    "Melanoma, NOS" to ConditionInfo(
        what = "Melanoma (not otherwise specified) when a more specific subtype is not assigned.",
        symptoms = "Irregular pigmented lesion with change over time."
    ),
    "Basal cell carcinoma, general malignant group" to ConditionInfo(
        what = "Basal cell carcinoma grouped without specifying subtype.",
        symptoms = "Pearly bump or scaly patch; easily bleeds; slow growth."
    ),
    "Basal cell carcinoma, nodular" to ConditionInfo(
        what = "See Nodular BCC.",
        symptoms = "Pearly dome-shaped bump; visible blood vessels; may ulcerate."
    ),
    "Superficial basal cell carcinoma" to ConditionInfo(
        what = "See Superficial BCC.",
        symptoms = "Thin red patch; slightly scaly; may look like eczema but doesn’t resolve."
    ),
    "Melanoma in situ (general)" to ConditionInfo(
        what = "Very early melanoma before invasion.",
        symptoms = "Flat irregular patch; color variegation; border changes."
    ),
    "Atypical/Dysplastic nevus" to ConditionInfo(
        what = "A mole with atypical features; usually benign but needs observation.",
        symptoms = "Larger than common nevi; irregular edges/color; change over time."
    ),
    "Common benign nevus" to ConditionInfo(
        what = "A common, harmless mole.",
        symptoms = "Symmetric, uniform color, smooth borders; stable over time."
    ),
    "Seborrheic keratosis" to ConditionInfo(
        what = "Very common, benign “stuck-on” wart-like growth.",
        symptoms = "Waxy or wart-like; brown/tan/black; crumbly surface; not dangerous."
    ),
    "Solar lentigo" to ConditionInfo(
        what = "Sun-spot or age-spot from UV exposure.",
        symptoms = "Flat, well-defined brown spot; stable; sun-exposed areas."
    ),
    "Lichen planus–like keratosis" to ConditionInfo(
        what = "Inflamed regressing sun spot / seborrheic keratosis variant.",
        symptoms = "Pink-to-brown patch; may be itchy; often fades with time."
    ),
    "Dermatofibroma" to ConditionInfo(
        what = "Benign firm bump in the skin (often post-insect-bite/trauma).",
        symptoms = "Firm dimple when pinched; brown/pink; usually harmless."
    )
)
