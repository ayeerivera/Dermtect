package com.example.dermtect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.example.dermtect.ui.screens.ResultFilter
import com.example.dermtect.ui.screens.StatusFilter

data class CaseData(
    val caseId: String,
    val label: String,                 // "Scan 1"
    val result: String?,               // "Benign"/"Malignant"
    val date: String,                  // formatted time stamp
    val status: String?,               // "completed"
    val imageUrl: String? = null,      // Storage URL
    val imageRes: Int? = null,
    val createdAt: Long = 0,
    val heatmapUrl: String? = null
)

@Composable
fun HistoryScreenTemplate(
    navController: NavController,
    screenTitle: String,
    caseList: List<CaseData>,
    showIndicators: Boolean = true,
    topContent: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    BubblesBackground {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                BackButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                Text(
                    text = screenTitle,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
            }
            topContent()
        }


        Spacer(Modifier.height(20.dp))

        // Scrollable case list...
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 140.dp, start = 20.dp, end = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val sortedList = remember(caseList) {
                // newestFirst? If you have a state/param, swap true/false accordingly.
                val newestFirst = true  // default behavior you had before

                if (newestFirst) {
                    caseList.sortedByDescending { it.createdAt.takeIf { t -> t > 0L } ?: Long.MIN_VALUE }
                } else {
                    caseList.sortedBy { it.createdAt.takeIf { t -> t > 0L } ?: Long.MAX_VALUE }
                }
            }

            if (sortedList.isEmpty()) {
                // ✅ Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {

                        Text(
                            text = "There’s no scan history yet.",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color(0xFF1D1D1D),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Your saved scans will appear here after you take your first photo.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        // Optional: quick action button to start scanning
                        Spacer(Modifier.height(16.dp))
                        Spacer(Modifier.height(16.dp))
                        PrimaryButton(
                            text = "Take your first scan",
                            onClick = { navController.navigate("camera") }, // ✅ redirects to your camera route
                            modifier = Modifier.fillMaxWidth(0.9f)
                        )

                    }
                }
            } else {
                sortedList.forEach { case ->


                    val statusLabel = when (case.status?.lowercase()) {
                        "reviewed"  -> "Reviewed"
                        "completed" -> "Completed"
                        else        -> null
                    }

                    CaseListItem(
                        title = case.label,
                        result = case.result,
                        date = case.date,
                        status = case.status,
                        statusLabel = statusLabel,
                        imageUrl = case.imageUrl
                    ) {
                        navController.navigate("case_detail/${case.caseId}")
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }
    }
}

@Composable
fun CaseListItem(
    title: String,
    result: String?,
    date: String,
    status: String?,
    statusLabel: String? = null,
    imageUrl: String? = null,
    imageRes: Int? = null,
    hideStatusChip: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(15.dp)),
                    placeholder = imageRes?.let { painterResource(it) },
                    error = imageRes?.let { painterResource(it) },
                    contentScale = ContentScale.Crop
                )
            } else if (imageRes != null) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(15.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFE0E0E0))
                )
            }
        }

        Spacer(Modifier.width(15.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(date, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }

        if (!hideStatusChip && statusLabel != null) {
            val (textColor, bgColor) = when (statusLabel.lowercase()) {
                "completed" -> Color(0xFF00B69B) to Color(0xFF00B69B).copy(alpha = 0.15f) // green
                "reviewed"  -> Color(0xFF1565C0) to Color(0xFF1565C0).copy(alpha = 0.15f) // blue
                else        -> Color(0xFF9E9E9E) to Color(0xFF9E9E9E).copy(alpha = 0.15f) // neutral fallback
        }

            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                modifier = Modifier
                    .background(bgColor, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun HistoryFilterButton(
    isDerma: Boolean,
    statusFilter: StatusFilter,
    onStatusChange: (StatusFilter) -> Unit,
    resultFilter: ResultFilter,
    onResultChange: (ResultFilter) -> Unit,
    newestFirst: Boolean,
    onSortChange: (Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(20.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFBFFDFD),
                        Color(0xFF88E7E7),
                        Color(0xFF55BFBF)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.3f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { expanded = true }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = "Filter",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {

            if (isDerma) {
                // Result (Derma-only)
                DropdownMenuItem(text = { Text("All") },       onClick = { onResultChange(ResultFilter.ALL); expanded = false })
                DropdownMenuItem(text = { Text("Malignant") }, onClick = { onResultChange(ResultFilter.MALIGNANT); expanded = false })
                DropdownMenuItem(text = { Text("Benign") },    onClick = { onResultChange(ResultFilter.BENIGN); expanded = false })
            }

            // Sort
            DropdownMenuItem(text = { Text("Newest to Oldest") }, onClick = { onSortChange(true);  expanded = false })
            DropdownMenuItem(text = { Text("Oldest to Newest") }, onClick = { onSortChange(false); expanded = false })
        }
    }
}