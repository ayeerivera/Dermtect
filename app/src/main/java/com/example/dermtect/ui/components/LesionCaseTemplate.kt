package com.example.dermtect.ui.screens

import android.graphics.Bitmap
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dermtect.R
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.components.BubblesBackground
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import com.example.dermtect.ui.components.DialogTemplate
import com.example.dermtect.ui.components.PrimaryButton
import com.example.dermtect.ui.components.SecondaryButton
import kotlin.math.min
import androidx.compose.ui.platform.LocalConfiguration
@OptIn(ExperimentalFoundationApi::class)
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
    isDerma: Boolean = false,
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
    isSaving: Boolean = false,
    compact: Boolean = false,

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
    val cfg = LocalConfiguration.current
    val screenW = cfg.screenWidthDp.dp
    val screenH = cfg.screenHeightDp.dp
    val imageHPad = 40.dp

    val desiredSide =
        if (showPrimaryButtons) screenH * unsavedHeightFraction
        else screenH * savedHeightFraction

    val targetSide by animateDpAsState(
        // keep it square but never wider than the available width
        targetValue = desiredSide.coerceAtMost(screenW - imageHPad),
        label = "imageSideAnim"
    )
    var showCondImageResId by remember { mutableStateOf<Int?>(null) }


    BubblesBackground {
        val scrollState = rememberScrollState()
        val scope = rememberCoroutineScope()
        val atBottom = remember { derivedStateOf { scrollState.value >= (scrollState.maxValue - 8) } }.value
        val canScroll = remember { derivedStateOf { scrollState.maxValue > 0 } }.value

        // OPTIONAL: account for the navigation bar so the chip isn’t too low
        val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        val arrowRotation by animateFloatAsState(
            targetValue = if (atBottom) 180f else 0f,
            label = "arrowRotation"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black, fontWeight = FontWeight.Normal),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(5.dp))


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(5.dp))

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
                                .size(targetSide)
                                .clickable {
                                    fullImagePage = pagerState.currentPage

                                }) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.matchParentSize()
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
                                            .matchParentSize()       // square frame
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
                        // 🧠 Derma-only prediction badge
                        if (isDerma && prediction.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            PillBadge(
                                text = "Prediction: $prediction",
                                fg = if (prediction.equals("Benign", true)) Color(0xFF126E3A) else Color(0xFF8A1C1C),
                                bg = if (prediction.equals("Benign", true)) Color(0xFFDFFCEB) else Color(0xFFFFE4E4)
                            )
                            Spacer(Modifier.height(8.dp))
                        }

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
                    text = "Possible Skin Condition",
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
                     PrimaryButton(
                    text = "Save",
                    onClick = { onSaveClick?.invoke() },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    height = 56.dp,
                    cornerRadius = 10.dp
                )

                SecondaryButton(
                    text = "Retake",
                    onClick = { onRetakeClick?.invoke() },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving,
                    height = 56.dp,
                    cornerRadius = 10.dp
                )
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
                Spacer(Modifier.height(20.dp))
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
        // ▼▼ fixed bottom-center overlay (sibling of Column)
        AnimatedVisibility(
            modifier = Modifier
                .align(Alignment.BottomCenter)   // keep it pinned to bottom
                .padding(bottom = 20.dp),        // lift above buttons a bit
            visible = canScroll && !atBottom,    // hide when already at bottom
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFFBFFDFD),
                                Color(0xFF88E7E7),
                                Color(0xFF55BFBF)
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable {
                        scope.launch {
                            // Always scroll down; no more "scroll up"
                            val target = scrollState.maxValue
                            scrollState.animateScrollTo(target)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = "Scroll down",
                    tint = Color(0xFF0FB2B2),
                    modifier = Modifier.size(28.dp)
                )
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

        DialogTemplate(
            show = true,
            title = name,
            description = info?.what ?: "No description available.",
            // 👇 provide a clickable image if we have one
            imageContent = {
                val resId = info?.imageResId
                if (resId != null) {
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "$name example",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCondImageResId = resId }, // << open full screen
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tap image to view full screen",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            },
            primaryText = "Close",
            onPrimary = { showInfoFor = null },
            onDismiss = { showInfoFor = null }
        )
    }

    if (showCondImageResId != null) {
        Dialog(
            onDismissRequest = { showCondImageResId = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,   // <- no default margins
                decorFitsSystemWindows = false     // <- draw under system bars
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)       // full bleed
                    .clickable { showCondImageResId = null },  // tap anywhere to close
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = showCondImageResId!!),
                    contentDescription = "Condition example",
                    modifier = Modifier
                        .fillMaxSize(),            // <- take all space (no padding!)
                    contentScale = ContentScale.Fit // scale up/down without cropping
                )
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
        "Melanoma in situ (general)",
        "Superficial basal cell carcinoma",
        "Nodular Basal Cell Carcinoma",
        "Indeterminate melanocytic neoplasm"
    )

    val lt60Ids = listOf(
        "Melanoma in situ, lentigo maligna",
        "Melanoma in situ, with nevus",
        "Melanoma invasive, superficial spreading",
        "Basal cell carcinoma (general malignant group)",
        "Melanoma invasive (general)",
        "Atypical intraepithelial melanocytic proliferation"
    )

    val lt80Ids = listOf(
        "Squamous cell carcinoma, invasive",
        "Melanoma, NOS (not otherwise specified)",
        "Basal cell carcinoma (general malignant group)"
    )

    val gte80Ids = listOf(
        "Nodular basal cell carcinoma",
        "Superficial basal cell carcinoma",
        "Melanoma in situ (general)"
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
                "You can continue your normal skincare routine. Just keep being mindful of your skin and how it changes over time."+
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
    @DrawableRes val imageResId: Int? = null
)

val conditionInfo: Map<String, ConditionInfo> = mapOf(
    "Solar/actinic keratosis" to ConditionInfo(
        what = "Rough, scaly erythematous or hyperkeratotic patches on chronically sun-exposed areas; common in older fair-skinned people.",
        imageResId = R.drawable.solar_or_actinic_keratosis
    ),
    "Squamous cell carcinoma in situ" to ConditionInfo(
        what = "Red scaly or crusted patch that may be mistaken for eczema or psoriasis; occurs on sun-exposed sites or mucosa.",
        imageResId = R.drawable.squamous_cell_carcinoma_in_situ

    ),
    "Melanoma in situ (general)" to ConditionInfo(
        what = "Pigmented patch or macule exhibiting ABCDE changes (asymmetry, border irregularity, colour variegation, diameter, evolving). Confined to epidermis.",
        imageResId = R.drawable.melanoma_in_situ

    ),
    "Superficial basal cell carcinoma" to ConditionInfo(
        what = "Thin, pink or scaly patch that may resemble eczema or AK; most often on trunk and limbs.",
        imageResId = R.drawable.superficial_bcc
    ),
    "Nodular basal cell carcinoma" to ConditionInfo(
        what = "Pearly, translucent papule or nodule with telangiectasia; may ulcerate (rodent ulcer). Common on head/neck.",
        imageResId = R.drawable.nodular_bcc
    ),
    "Indeterminate melanocytic neoplasm" to ConditionInfo(
        what = "Lesion that cannot be confidently classified clinically or histologically as benign or malignant.",
        imageResId = R.drawable.indeterminate_melanocytic_neoplasm
    ),
    "Melanoma in situ, lentigo maligna" to ConditionInfo(
        what = "Slowly enlarging irregular pigmented patch on chronically sun-damaged skin (face/neck) of older adults.",
        imageResId = R.drawable.melanoma_in_situ_lentigo_maligna
    ),
    "Melanoma in situ, with nevus" to ConditionInfo(
        what = "Melanoma arising adjacent to or within a pre-existing nevus but still confined to epidermis.",
        imageResId = R.drawable.melanoma_in_situ_with_nevus
    ),
    "Melanoma invasive, superficial spreading" to ConditionInfo(
        what = "Most common invasive subtype — irregular, often multicolored plaque that initially grows radially then invades vertically; common on trunk (men) and legs (women).",
        imageResId = R.drawable.melanoma_invasive_superficial_spreading
    ),
    "Basal cell carcinoma (general malignant group)" to ConditionInfo(
        what = "Group term for BCC subtypes (nodular, superficial, pigmented, morpheaform, etc.). Usually slow-growing, locally invasive lesions on sun-exposed skin.",
        imageResId = R.drawable.bcc_general_malignant_group
    ),
    "Melanoma invasive (general)" to ConditionInfo(
        what = "Melanoma that has invaded beyond the epidermis into dermis; variable appearance depending on subtype.",
        imageResId = R.drawable.melanoma_invasive_general
    ),
    "Atypical intraepithelial melanocytic proliferation" to ConditionInfo(
        what = "A descriptive term for an epidermal melanocytic proliferation with atypia insufficient for a definitive melanoma diagnosis. May appear clinically suspicious.",
        imageResId = R.drawable.atypical_intraepithelial_melanocytic_proliferation
    ),
    "Squamous cell carcinoma, invasive" to ConditionInfo(
        what = "Scaly, crusted, or ulcerated nodule or plaque that may bleed or grow rapidly; arises on sun-exposed sites, scars, or immunosuppressed skin.",
        imageResId = R.drawable.squamous_cell_carcinoma_invasive
    ),
    "Melanoma, NOS (not otherwise specified)" to ConditionInfo(
        what = "Melanoma that cannot be classified into a specific histologic subtype from available clinical/pathology data."
    ),
    "Atypical/Dysplastic nevus" to ConditionInfo(
        what = "Moles that appear larger and more irregular than common nevi — irregular border, variable colour, often >5 mm. May clinically resemble melanoma.",
        imageResId = R.drawable.atypical_or_dysplastic_nevus
    ),
    "Common benign nevus" to ConditionInfo(
        what = "Small, round to oval pigmented macules or papules (typically <6 mm) with uniform colour and smooth borders; common in childhood and young adulthood.",
        imageResId = R.drawable.common_benign_nevus
    ),
    "Seborrheic keratosis" to ConditionInfo(
        what = "Very common benign “stuck-on” waxy papules/plaques in older adults; colour ranges from light tan to black. Often on trunk, face.",
        imageResId = R.drawable.seborrheic_keratosis
    ),
    "Solar lentigo" to ConditionInfo(
        what = "Flat, well-circumscribed brown macules on chronically sun-exposed skin (face, hands, forearms).",
        imageResId = R.drawable.solar_lentigo
    ),
    "Lichen planus–like keratosis" to ConditionInfo(
        what = "Usually a solitary pink to brown inflamed patch or plaque that may appear on sun-exposed skin and can mimic atypical pigmented lesions.",
        imageResId = R.drawable.lichen_planus_like_keratosis
    ),
    "Dermatofibroma" to ConditionInfo(
        what = "Small, firm papule or nodule (commonly on lower legs) that often dimples with lateral pressure (“dimple sign”). Usually under 1 cm.",
        imageResId = R.drawable.dermatofibroma
    )
)
