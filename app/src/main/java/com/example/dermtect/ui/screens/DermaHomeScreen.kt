package com.example.dermtect.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dermtect.R
import com.example.dermtect.ui.components.TopRightNotificationIcon
import com.example.dermtect.ui.components.CaseData
import com.example.dermtect.ui.components.CaseListItem

@Composable
fun DermaHomeScreen(
    navController: NavController,
    onPendingCasesClick: () -> Unit,
    onTotalCasesClick: () -> Unit,
    onNotifClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCameraClick: () -> Unit,
    firstName: String
) {
    fun indicatorColorOf(result: String?, status: String?): Color = when {
        status?.equals("pending", true) == true -> Color(0xFFFFA500)  // orange
        result?.equals("benign", true) == true -> Color(0xFF4CAF50)   // green
        result?.equals("malignant", true) == true -> Color(0xFFF44336) // red
        else -> Color.Gray
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Notification Button
        TopRightNotificationIcon(
            onNotifClick = onNotifClick,
            modifier = Modifier
                .padding(top = 50.dp, end = 25.dp)
                .align(Alignment.End)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Text(
                text = "Hello,",
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal)
            )
            Text(
                text = "Dr. $firstName!",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Early Detection Saves Lives.",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)
            )

            Spacer(modifier = Modifier.height(30.dp))

            StatCardRow(
                onPendingCasesClick = onPendingCasesClick,
                onTotalCasesClick = onTotalCasesClick
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Pending Cases Section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pending Cases",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    painter = painterResource(id = R.drawable.expand),
                    contentDescription = "Expand",
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { navController.navigate("pending_cases") }
                )
            }

            Spacer(modifier = Modifier.height(15.dp))

            // ===== Empty state (no items) =====
            val pendingCases = emptyList<CaseData>()

            if (pendingCases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pending cases right now",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF7A7A7A)
                    )
                }
            } else {
                pendingCases.forEach { case ->
                    CaseListItem(
                        title = case.label,
                        result = case.result,
                        date = case.date,
                        status = case.status,
                        indicatorColor = indicatorColorOf(case.result, case.status),
                        statusLabel = case.status,
                        statusColor = null,
                        imageUrl = case.imageUrl,
                        imageRes = case.imageRes,
                        onClick = {
                            navController.navigate("pending_cases")
                        }
                    )
                    Divider(modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            Spacer(modifier = Modifier.height(60.dp)) // bottom nav spacing
        }

        BottomNavBar(
            onSettingsClick = onSettingsClick,
            onCameraClick = onCameraClick)
    }
}

@Composable
fun StatCardRow(
    onPendingCasesClick: () -> Unit,
    onTotalCasesClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = "Pending Cases",
                value = "0",
                imageRes = R.drawable.pending_cases,
                imageCardColor = Color(0xFFD7F2D6),
                modifier = Modifier.weight(1f),
                onClick = onPendingCasesClick
            )
            StatCard(
                label = "Total Cases",
                value = "0", // ← all cases empty
                imageRes = R.drawable.total_cases,
                imageCardColor = Color(0xFFDCD2DE),
                modifier = Modifier.weight(1f),
                onClick = onTotalCasesClick
            )
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    imageRes: Int,
    modifier: Modifier = Modifier,
    imageCardColor: Color = Color.White,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFCDFFFF))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                ),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 30.sp
                ),
                modifier = Modifier
                    .padding(start = 15.dp)
                    .fillMaxWidth(0.5f)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = imageCardColor),
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.End)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .offset(x = (-5).dp, y = (-5).dp)
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = label,
                    modifier = Modifier
                        .size(60.dp)
                        .padding(10.dp)
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(
    onSettingsClick: () -> Unit = {},
    onCameraClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // The bar itself (gradient like user’s)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    clip = false
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFBFFDFD),
                            Color(0xFF88E7E7),
                            Color(0xFF55BFBF)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ),
            color = Color.Transparent,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .padding(horizontal = 65.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // left & right icons (leave center empty for the floating camera)
                Image(
                    painter = painterResource(id = R.drawable.home_vector),
                    contentDescription = "Home",
                    modifier = Modifier.size(26.dp)
                )
                Image(
                    painter = painterResource(id = R.drawable.user_vector),
                    contentDescription = "Settings",
                    modifier = Modifier
                        .size(26.dp)
                        .clickable { onSettingsClick() }
                )
            }
        }

        // Floating camera button (centered, overlapping the bar)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp)
                .size(70.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    clip = false
                )
                .background(
                    color = Color(0xFFCDFFFF),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFBFFDFD),
                            Color(0xFF88E7E7),
                            Color(0xFF41A6A6)
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFBFFDFD),
                            Color(0xFF88E7E7),
                            Color(0xFF55BFBF)
                        )
                    ),
                    shape = CircleShape
                )
                .padding(5.dp)
                .clickable { onCameraClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.camera_fill), // or camera_vector
                contentDescription = "Camera",
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

