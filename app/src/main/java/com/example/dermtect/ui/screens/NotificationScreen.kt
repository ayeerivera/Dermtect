package com.example.dermtect.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dermtect.R
import com.example.dermtect.ui.components.BackButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun buildDiagnosisNotificationFlow(userId: String): Flow<List<NotificationItem>> {
    val db = FirebaseFirestore.getInstance()

    return db.collection("lesion_case")
        .whereEqualTo("user_id", userId)
        .snapshots()
        .map { snapshot ->
            snapshot.documents.mapNotNull { doc ->
                val caseId = doc.id
                val hasNew = doc.getBoolean("has_new_derma_assessment") ?: false
                val diagnosis = doc.getString("diagnosis")
                val notes = doc.getString("notes")
                val assessedAt = doc.getTimestamp("assessed_at")?.toDate()
                val shownTime = assessedAt ?: doc.getTimestamp("timestamp")?.toDate()

                val hasAssessment = !diagnosis.isNullOrBlank() || !notes.isNullOrBlank()
                if (!hasAssessment) return@mapNotNull null

                val label = doc.getString("label") ?: "scan"
                val title = "Assessment Complete"
                val message =
                    "A dermatologist has updated the assessment for your $label. Tap to view the official notes and next steps."

                NotificationItem(
                    id = caseId,
                    title = title,
                    message = message,
                    type = 2, // Diagnosis Notification Type
                    timestamp = shownTime?.let {
                        SimpleDateFormat(
                            "MMM dd, yyyy · HH:mm",
                            Locale.getDefault()
                        ).format(it)
                    } ?: "—",
                    isRead = !hasNew,
                    date = shownTime ?: Date()
                )
            }
        }
}

// Notification data model
data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val type: Int,
    val timestamp: String,
    var isRead: Boolean = true,
    val date: Date = Date()
)

enum class FilterOption { ALL, READ, UNREAD, NEWEST_FIRST, OLDEST_FIRST }

@Composable
fun NotificationScreen(navController: NavController) {
    var filterOption by remember { mutableStateOf(FilterOption.ALL) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<NotificationItem>>(emptyList()) }
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val userId = auth.currentUser?.uid
    val scope = rememberCoroutineScope()

    val diagnosisNotificationFlow = remember(userId) {
        if (userId.isNullOrBlank()) {
            flowOf(emptyList())
        } else {
            buildDiagnosisNotificationFlow(userId)
        }

        if (userId.isNullOrBlank()) return@remember flowOf(emptyList())

        // 1. Listen to all lesion cases belonging to this user
        db.collection("lesion_case")
            .whereEqualTo("user_id", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                        val caseId = doc.id
                        val hasNew = doc.getBoolean("has_new_derma_assessment") ?: false
                        val diagnosis = doc.getString("diagnosis")
                        val notes = doc.getString("notes")
                        val assessedAt = doc.getTimestamp("assessed_at")?.toDate()
                        // optional: fall back to created timestamp if assessed_at is null
                        val shownTime = assessedAt ?: doc.getTimestamp("timestamp")?.toDate()

                        val hasAssessment = !diagnosis.isNullOrBlank() || !notes.isNullOrBlank()
                        if (!hasAssessment) return@mapNotNull null

                        val label = doc.getString("label") ?: "scan"
                        val title = "Assessment Complete"
                        val message =
                            "A dermatologist has updated the assessment for your $label. Tap to view the official notes and next steps."

                        NotificationItem(
                            id = caseId,
                            title = title,
                            message = message,
                            type = 2, // Diagnosis Notification Type
                            timestamp = shownTime?.let {
                                SimpleDateFormat(
                                    "MMM dd, yyyy · HH:mm",
                                    Locale.getDefault()
                                ).format(it)
                            } ?: "—",
                            isRead = !hasNew,
                            date = shownTime ?: Date()
                        )
                    }
                }
            }

    // 🔑 NEW: Collect the flow and update the 'notifications' state
    LaunchedEffect(diagnosisNotificationFlow) {
        diagnosisNotificationFlow.collect { newNotifications ->
            notifications = newNotifications
        }
    }
    fun applyFilter(list: List<NotificationItem>): List<NotificationItem> {
        // Always start with newest → oldest
        val baseSorted = list.sortedByDescending { it.date }

        return when (filterOption) {
            FilterOption.ALL -> baseSorted

            FilterOption.READ -> baseSorted.filter { it.isRead }

            FilterOption.UNREAD -> baseSorted.filter { !it.isRead }

            FilterOption.NEWEST_FIRST -> baseSorted  // already newest → oldest

            FilterOption.OLDEST_FIRST -> baseSorted.reversed()
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFCDFFFF))
    ) {
        BackButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.offset(x = 25.dp, y = 50.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 20.dp)
            ) {
                Row(
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
                        .clickable { showFilterMenu = !showFilterMenu }
                        .padding(horizontal = 12.dp, vertical = 6.dp),  // compact chip
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.filter), // keep your drawable
                        contentDescription = "Filter",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
                DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }, modifier = Modifier.background(Color.White)) {
                    DropdownMenuItem(text = { Text("All", style = MaterialTheme.typography.labelMedium) }, onClick = {
                        filterOption = FilterOption.ALL
                        showFilterMenu = false
                    })
                    DropdownMenuItem(text = { Text("Read",style = MaterialTheme.typography.labelMedium) }, onClick = {
                        filterOption = FilterOption.READ
                        showFilterMenu = false
                    })
                    DropdownMenuItem(text = { Text("Unread",  style = MaterialTheme.typography.labelMedium) }, onClick = {
                        filterOption = FilterOption.UNREAD
                        showFilterMenu = false
                    })
                    DropdownMenuItem(text = { Text("Newest to Oldest", style = MaterialTheme.typography.labelMedium) }, onClick = {
                        filterOption = FilterOption.NEWEST_FIRST
                        showFilterMenu = false
                    })
                    DropdownMenuItem(text = { Text("Oldest to Newest", style = MaterialTheme.typography.labelMedium) }, onClick = {
                        filterOption = FilterOption.OLDEST_FIRST
                        showFilterMenu = false
                    })
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val filteredList by remember(notifications, filterOption) {
                derivedStateOf { applyFilter(notifications) }
            }
            if (filteredList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.notifications_vector),
                        contentDescription = "No Notifications Icon",
                        tint = Color(0xFFB0BEC5),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No notifications yet",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.Gray
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(40.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredList, key = { it.id }) { notification ->
                            val isUnread = !notification.isRead

                            var menuExpanded by remember { mutableStateOf(false) }
                            var lastClickTime by remember { mutableStateOf(0L) }
                            fun notTooFast(): Boolean {
                                val now = System.currentTimeMillis()
                                return if (now - lastClickTime > 400) { lastClickTime = now; true } else false
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isUnread) Color(0xFFF1FBFB) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isUnread) 1.dp else 0.dp,
                                        color = if (isUnread) Color(0xFFB9EAEA) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        if (!notTooFast()) return@clickable
                                        val id = notification.id
                                        navController.navigateCaseDetail(id)
                                        if (isUnread && userId != null) {
                                            scope.launch {
                                                runCatching {
                                                    db.collection("lesion_case").document(id)
                                                        .update("has_new_derma_assessment", false)
                                                        .await()
                                                }
                                                notifications = notifications.map {
                                                    if (it.id == id) it.copy(isRead = true) else it
                                                }
                                            }
                                        }
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Image(
                                        painter = painterResource(
                                            id = when (notification.type) {
                                                1 -> R.drawable.notif_type1
                                                2 -> R.drawable.notif_type2
                                                else -> R.drawable.notifications_vector // fallback icon
                                            }
                                        ),
                                        contentDescription = "Notification Icon",
                                        modifier = Modifier
                                            .size(27.dp)
                                            .padding(top = 5.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = notification.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Bold
                                        )

// MESSAGE
                                        Text(
                                            text = notification.message,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                        Text(
                                            text = notification.timestamp,
                                            style = MaterialTheme.typography.labelMedium.copy(color = Color.Gray),
                                            modifier = Modifier.padding(top = 4.dp)
                                        )

                                    }
                                    if (isUnread) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    Color(0xFF0EA5A5),
                                                    shape = RoundedCornerShape(50)
                                                )
                                                .align(Alignment.CenterVertically)
                                        )
                                    } else {
                                        Spacer(Modifier.width(10.dp))
                                    }
                                }
                                if (notification != filteredList.last()) {
                                    HorizontalDivider(modifier = Modifier.padding(top = 15.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
private fun caseDetailRoute(rawId: String) = "case_detail/${Uri.encode(rawId)}"

fun NavController.navigateCaseDetail(rawId: String) {
    val routeNow = currentBackStackEntry?.destination?.route
    val argId = currentBackStackEntry?.arguments?.getString("caseId")
    val isAlreadyOnSameDetail =
        routeNow?.startsWith("case_detail/") == true &&
                (argId == rawId || argId == Uri.encode(rawId))

    if (isAlreadyOnSameDetail) {
        popBackStack()
    }

    navigate(caseDetailRoute(rawId)) {
        launchSingleTop = true
        restoreState = false
    }
}

