package com.example.dermtect.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.google.gson.Gson


data class CaseData(
    val imageRes: Int,
    val title: String,
    val result: String?,
    val date: String,
    val status: String?
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
            Spacer(modifier = Modifier.height(40.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 140.dp, start = 20.dp, end = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            caseList.forEach { case ->
                val indicatorColor = when (case.result?.lowercase()) {
                    "pending" -> Color(0xFFFFC107)   // Yellow
                    "benign" -> Color(0xFF4CAF50)    // Green
                    "malignant" -> Color(0xFFF44336) // Red
                    else -> Color(0xFFFFC107)
                }

                val (statusLabel, statusColor) = when (case.status?.lowercase()) {
                    "pending" -> "Pending" to Color(0xFFFFD46D).copy(alpha = 0.2f)
                    "completed" -> "Completed" to Color(0xFF00B69B).copy(alpha = 0.2f)
                    else -> null to null
                }

                CaseListItem(
                    imageRes = case.imageRes,
                    title = case.title,
                    result = case.result,
                    date = case.date,
                    status = case.status,
                    indicatorColor = indicatorColor,
                    statusLabel = statusLabel,
                    statusColor = statusColor,
                    onClick = {
                        val gson = Gson()
                        val caseJson = Uri.encode(gson.toJson(case))
                        navController.navigate("derma_assessment_screen/$caseJson")
                    }
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

                }
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

@Composable
fun CaseListItem(
    imageRes: Int,
    title: String,
    result: String?,
    date: String,
    status: String?,
    indicatorColor: Color,
    statusLabel: String? = null, // Show "Pending" or "Completed"
    statusColor: Color? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box {
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = title,
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(15.dp))
            )
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.TopStart)
                    .background(indicatorColor, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            result?.let {
                Text("Result: $it", style = MaterialTheme.typography.bodyMedium)
            }
            Text(date, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        }
        statusLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = if (it.equals("Pending", ignoreCase = true)) Color(0xFFC48833)
                else if (it.equals("Completed", ignoreCase = true)) Color(0xFF00B69B)
                else Color(0xFFFFC107),
                modifier = Modifier
                    .background(
                        statusColor ?: Color.Gray.copy(alpha = 0.1f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } ?: Spacer(modifier = Modifier.width(0.dp))


    }
}
