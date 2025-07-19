package com.example.dermtect.ui.screens

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.gson.Gson

@Composable
fun DermaHomeScreen(
    navController: NavController,
    onPendingCasesClick: () -> Unit,
    onTotalCasesClick: () -> Unit,
    onNotifClick: () -> Unit,
    onSettingsClick: () -> Unit,
    firstName: String
) {
    fun getIndicatorColor(result: String?): Color = when (result?.lowercase()) {
        "pending" -> Color(0xFFFFA500)
        "benign" -> Color(0xFF4CAF50)
        "malignant" -> Color(0xFFF44336)
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

            // Header Section

                Text(
                    text = "Hello,",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Normal)
                )
            Text(
                text = "Dr. ${firstName}!",
                style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold)
            )

            Text(
                    text = "Early Detection Saves Lives.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal)
                )

            Spacer(modifier = Modifier.height(30.dp))
            StatCardRow (
                onPendingCasesClick = onPendingCasesClick,
                onTotalCasesClick = onTotalCasesClick
            )

                    Spacer(modifier = Modifier.height(20.dp))

            // Pending Cases Section
            Row(
                modifier = Modifier.fillMaxWidth()

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

            val pendingCases = listOf(
                CaseData(R.drawable.sample_skin_1, "Scan 1", "Pending", "May 8, 2025", null),
                CaseData(R.drawable.sample_skin_1, "Scan 2", "Pending", "May 9, 2025",null ),
                CaseData(R.drawable.sample_skin_1, "Scan 3", "Pending", "May 10, 2025",null )
            )

            pendingCases.forEach { case ->
                CaseListItem(
                    imageRes = case.imageRes,
                    title = case.title,
                    result = case.result,
                    date = case.date,
                    status = case.status,
                    indicatorColor = getIndicatorColor(case.result),
                    statusLabel = case.status,
                    statusColor = null,
                    onClick = {
                        val gson = Gson()
                        val caseJson = Uri.encode(gson.toJson(case))
                        navController.navigate("derma_assessment_screen/$caseJson")
                    }
                )
                Divider(modifier = Modifier.padding(vertical = 12.dp))
            }

            Spacer(modifier = Modifier.height(60.dp)) // Space for bottom nav


        }
        BottomNavBar(onSettingsClick = onSettingsClick)
    }
}

@Composable
fun StatCardRow ( onPendingCasesClick: () -> Unit,
                  onTotalCasesClick: () -> Unit ){
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            StatCard(
                label = "Pending Cases",
                value = "20",
                imageRes = R.drawable.pending_cases,
                imageCardColor = Color(0xFFD7F2D6),
                modifier = Modifier.weight(1f),
                onClick = onPendingCasesClick
            )
            StatCard(
                label = "Total Cases",
                value = "20",
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
        Column(
            modifier = Modifier.fillMaxSize()
        )   {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = Color.DarkGray),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 20.dp)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Medium, fontSize = 30.sp),
                modifier = Modifier
                    .padding(start = 15.dp)
                    .fillMaxWidth(0.5f)
            )
            Card (
                colors = CardDefaults.cardColors(containerColor = imageCardColor),
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.End)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .offset(x = -5.dp, y = -5.dp)
            ){
            Image(
                painter = painterResource(id = imageRes),
                contentDescription = label,
                modifier = Modifier
                    .size(60.dp)
                    .padding(10.dp)

            ) }
        }
    }
}


@Composable
fun BottomNavBar(
    onSettingsClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.White)
    ) {
        Surface(
            color = Color(0xFFCDFFFF),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(200.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

    }
}
