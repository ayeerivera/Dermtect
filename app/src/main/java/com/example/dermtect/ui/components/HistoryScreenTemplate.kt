package com.example.dermtect.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.dermtect.R

data class CaseData(
    val caseId: String,
    val label: String,                 // "Scan 1"
    val result: String?,               // "Benign"/"Malignant"/"Pending"
    val date: String,                  // formatted timestamp
    val status: String?,               // "completed"/"pending"
    val imageUrl: String? = null,      // Storage URL
    val imageRes: Int? = null
)

@Composable
fun HistoryScreenTemplate(
    navController: NavController,
    screenTitle: String,
    caseList: List<CaseData>
) {
    BubblesBackground {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, bottom = 10.dp)
        ) {
            BackButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 23.dp)
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

        Spacer(Modifier.height(40.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 140.dp, start = 20.dp, end = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            caseList.forEach { case ->
                val indicatorColor = when {
                    case.status?.equals("pending", true) == true -> Color(0xFFFFC107) // pending -> yellow

                    // New strings you save now:
                    case.result?.equals("Alert", true) == true   -> Color(0xFFF44336) // red
                    case.result?.equals("NoAlert", true) == true -> Color(0xFF4CAF50) // green

                    // Backward-compat with previous strings (if they ever exist):
                    case.result?.equals("malignant", true) == true -> Color(0xFFF44336) // red
                    case.result?.equals("benign", true) == true    -> Color(0xFF4CAF50) // green

                    else -> Color(0xFFBDBDBD) // neutral gray instead of always yellow
                }


                val (statusLabel, statusColor) = when (case.status?.lowercase()) {
                    "pending" -> "Pending" to Color(0xFFFFD46D).copy(alpha = 0.2f)
                    "completed" -> "Completed" to Color(0xFF00B69B).copy(alpha = 0.2f)
                    else -> null to null
                }

                CaseListItem(
                    title = case.label,
                    result = case.result,
                    date = case.date,
                    status = case.status,
                    indicatorColor = indicatorColor,
                    statusLabel = statusLabel,
                    statusColor = statusColor,
                    imageUrl = case.imageUrl
                ) {
                    navController.navigate("case_detail/${case.caseId}")

                }

                androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 12.dp))
            }
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
fun CaseListItem(
    title: String,
    result: String?,
    date: String,
    status: String?,
    indicatorColor: Color,
    statusLabel: String? = null,
    statusColor: Color? = null,
    imageUrl: String? = null,
    imageRes: Int? = null,
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

            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.TopStart)
                    .background(indicatorColor, CircleShape)
            )
        }

        Spacer(Modifier.width(15.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            result?.let { Text("Result: $it", style = MaterialTheme.typography.bodyMedium) }
            Text(date, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }

        statusLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    it.equals("Pending", true) -> Color(0xFFC48833)
                    it.equals("Completed", true) -> Color(0xFF00B69B)
                    else -> Color(0xFFFFC107)
                },
                modifier = Modifier
                    .background(statusColor ?: Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } ?: Spacer(Modifier.width(0.dp))
    }
}