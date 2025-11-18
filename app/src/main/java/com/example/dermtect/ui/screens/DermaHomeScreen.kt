package com.example.dermtect.ui.screens

import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dermtect.R
import com.example.dermtect.ui.components.DermaCaseData
import com.example.dermtect.ui.components.DermaCaseListItem
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import coil.compose.AsyncImage
import com.example.dermtect.ui.components.PrimaryButton
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dermtect.ui.components.DialogTemplate
import com.example.dermtect.ui.tutorial.DermaTutorialManager
import com.example.dermtect.ui.tutorial.TutorialOverlay
import com.example.dermtect.ui.viewmodel.DermaHomeViewModel
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.example.dermtect.ui.components.SecondaryButton
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.getValue
import com.google.firebase.firestore.Query
import androidx.compose.runtime.collectAsState
import com.google.firebase.auth.FirebaseAuth


@Composable
fun DermaHomeScreen(
    navController: NavController,
    onPendingCasesClick: () -> Unit,
    onTotalCasesClick: () -> Unit,
    onCameraClick: () -> Unit,
    firstName: String
) {
    val vm: DermaHomeViewModel = viewModel()
    val lastName by vm.lastName
    val email by vm.email
    val isGoogle by vm.isGoogleAccount
    val profilePhotoUri by vm.photoUri
    // 🔹 NEW: newest pending case state
    var newestPendingCase by remember { mutableStateOf<DermaCaseData?>(null) }
    var newestPendingLoading by remember { mutableStateOf(true) }
    val currentUid = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }

    fun indicatorColorOf(result: String?, status: String?): Color = when {
        status?.equals("derma_pending", true) == true -> Color(0xFFFFA500)  // orange (pending status)
        result?.equals("benign", true) == true -> Color(0xFF4CAF50)   // green
        result?.equals("malignant", true) == true -> Color(0xFFF44336) // red
        else -> Color.Gray
    }

    var showLookup by rememberSaveable { mutableStateOf(false) }

    var menuExpanded by remember { mutableStateOf(false) }
    val profileUri: Uri? = null
    val isGoogleAccount = false
    val userRole = "derma" // or "user" – whatever your app expects
    val dermaHasConsented by vm.dermaHasConsented.collectAsState()
    val dermaConsentChecked by vm.dermaConsentChecked.collectAsState()

    var showDermaConsentDialog by remember { mutableStateOf(false) }
    var initialDermaConsentDialogShown by rememberSaveable { mutableStateOf(false) }

    val assessmentLabel = "Start Assessment" // or "My Initial Assessment"
    val context = LocalContext.current
    val tutorial = remember { DermaTutorialManager() }
    val showTutorial by produceState<Boolean?>(initialValue = null, tutorial, context) {
        tutorial.initialize(context)
        value = tutorial.isFirstRun
    }

    // 🔑 Use the live counts from the ViewModel
    val pendingCount by vm.pendingCount.collectAsState(initial = 0)
    val totalCount by vm.totalCount.collectAsState(initial = 0)

    val scope = rememberCoroutineScope()
    // ... removed SavedStateHandle listeners for history counts ...
    var fixedScanNumber by remember { mutableStateOf<Int?>(null) } // 👈 New State

// 🔹 NEW: LaunchedEffect to calculate the fixed scan number once the case is loaded
    LaunchedEffect(newestPendingCase) {
        if (newestPendingCase != null) {
            // Run the expensive calculation function on the single case
            fixedScanNumber = vm.getFixedScanNumber(newestPendingCase!!.caseId)
        } else {
            fixedScanNumber = null
        }
    }
    LaunchedEffect(Unit) {
        try { FirebaseFirestore.getInstance().enableNetwork() } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        vm.checkDermaConsentStatus()
    }
    LaunchedEffect(dermaConsentChecked, dermaHasConsented) {
        if (dermaConsentChecked && !dermaHasConsented && !initialDermaConsentDialogShown) {
            showDermaConsentDialog = true
            initialDermaConsentDialogShown = true
        }
    }

    // 🔑 LaunchedEffect to fetch only the newest pending case (limit 1)
    LaunchedEffect(currentUid, pendingCount) {
        if (currentUid == null) {
            newestPendingCase = null
            newestPendingLoading = false
            return@LaunchedEffect
        }

        newestPendingLoading = true
        try {
            val db = FirebaseFirestore.getInstance()
            val snap = db.collection("lesion_case")
                .whereEqualTo("derma_status", "derma_pending")
                .whereEqualTo("assessor_id", currentUid)
                // 🔑 FIX: Order by 'assessed_at' for the newest assignment/update time
                .orderBy("assessed_at", Query.Direction.DESCENDING)
                .limit(1) // ✅ Ensures only the newest case is fetched
                .get()
                .await()


            val doc = snap.documents.firstOrNull()
            if (doc == null) {
                newestPendingCase = null
            } else {
                // 🔑 FIX: Prioritize 'assessed_at' for the displayed date and sort key
                val assessedAt = doc.getTimestamp("assessed_at")?.toDate()
                val originalTs = doc.getTimestamp("timestamp")?.toDate()
                val displayDate = assessedAt ?: originalTs // Use assessment time if available

                val df = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())

                newestPendingCase = DermaCaseData(
                    caseId = doc.id,
                    label = doc.getString("label") ?: "Scan",
                    result = doc.getString("diagnosis"),
                    date = displayDate?.let(df::format) ?: "—",
                    status = doc.getString("derma_status"),
                    imageUrl = doc.getString("scan_url"),
                    heatmapUrl = doc.getString("heatmap_url"),
                    createdAt = displayDate?.time ?: 0L, // Use assessment time for sorting/ordering
                    reportCode = doc.getString("report_code")
                )
            }
        } catch (e: Exception) {
            Log.e("DermaHomeScreen", "Failed to load newest pending case", e)
            newestPendingCase = null
        } finally {
            newestPendingLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .background(Color(0x3DCDFFFF))
                    .padding(start = 20.dp, end = 20.dp, top = 26.dp)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(
                        text = "Hello,",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 32.sp,
                            color = Color(0xFF1D1D1D)
                        )
                    )

                    Text(
                        text = "Dr. $firstName!",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 42.sp,
                            color = Color(0xFF1D1D1D)
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Early Detection Saves Lives.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Normal,
                            fontStyle = FontStyle.Italic,
                            fontSize = 16.sp,
                            color = Color(0xFF1D1D1D)
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp)
                        .onGloballyPositioned { coords ->
                            Log.d("Tutorial", "derma_profile_menu bounds set")

                            if (tutorial.getStepKey() == "derma_profile_menu") {
                                tutorial.currentTargetBounds = coords.boundsInWindow()
                            }
                        }
                ) {
                    DermaDropdownMenu(
                        name = "Dr. $firstName ${lastName}",
                        photoUri = profilePhotoUri, expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        assessmentLabel = "Start Assessment",
                        onEditProfile = {
                            if (!dermaHasConsented) {
                                showDermaConsentDialog = true
                            } else {
                                val e = Uri.encode(email.ifBlank { "-" })
                                val g = isGoogle.toString()
                                navController.navigate("derma_profile/$e/$g")
                            }
                        },

                        onAssessmentClick = {
                            if (!dermaHasConsented) {
                                showDermaConsentDialog = true
                            } else {
                                navController.navigate("questionnaire")

                            }
                        },

                        onAboutClick = { navController.navigate("about") },
                        onDataPrivacyClick = { navController.navigate("derma_terms_privacy") },
                        onLogoutClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("derma_home") {
                                    inclusive = true     // remove home itself too
                                }
                                launchSingleTop = true
                            }
                        }
                    )

                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                StatCardRow(
                    tutorial = tutorial,
                    onPendingCasesClick = {
                        if (!dermaHasConsented) {
                            showDermaConsentDialog = true
                        } else {
                            onPendingCasesClick()
                        }
                    },
                    onTotalCasesClick = {
                        if (!dermaHasConsented) {
                            showDermaConsentDialog = true
                        } else {
                            onTotalCasesClick()
                        }
                    },
                    pendingCount = pendingCount,
                    totalCount = totalCount
                )





                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Look Up Cases",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold
                        )
                        // (optional) add an icon here
                    }

                    Spacer(Modifier.height(12.dp))

                    DermaLookupInline(
                        tutorial = tutorial,
                        navController = navController,
                        hasConsented = dermaHasConsented,
                        onRequireConsent = { showDermaConsentDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))              // ✅ keep some space after lookup
                }
                // ===== Empty state (no items) / Newest Pending Case =====
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    when {
                        newestPendingLoading -> {
                            // ⏳ Loading state
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Checking for your pending cases…", color = Color.Gray)
                            }
                        }

                        newestPendingCase == null -> {
                            // 🚫 No pending cases
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No pending cases yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.DarkGray
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Newest case will appear here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }

                        else -> {
                            // ✅ Show newest pending case

                            val caseIndicatorColor = indicatorColorOf(
                                newestPendingCase!!.result,
                                newestPendingCase!!.status
                            )
                            // Retained light orange background color (alpha 0.20f)
                            val caseBgColor = caseIndicatorColor.copy(alpha = 0.20f)

                            val statusToDisplay = newestPendingCase!!.status
                                .takeIf { it.equals("derma_pending", true) }
                                ?.let { "Pending" } // Standardized label
                                ?: newestPendingCase!!.status


                            // com/example/dermtect/ui/screens/DermaHomeScreen.kt (Inside the `else -> { // ✅ Show newest pending case }` block)

// ... color/status logic ...

                            DermaCaseListItem(
                                // ✅ FIX: Use the calculated fixed number for the title
                                title = if (fixedScanNumber != null) "Scan #$fixedScanNumber" else newestPendingCase!!.label,
                                result = newestPendingCase!!.result,
                                date = "${newestPendingCase!!.date}\nID: ${newestPendingCase!!.reportCode ?: "—"}",
                                status = statusToDisplay,
                                indicatorColor = caseIndicatorColor,
                                statusLabel = "Pending",
                                statusColor = caseBgColor,
                                imageUrl = newestPendingCase!!.imageUrl,
                                imageRes = null,
                                onClick = {
                                    // ✅ FIX: Pass the fixed number through navigation
                                    val numberArg = fixedScanNumber?.toString() ?: ""
                                    navController.navigate("DermaAssessmentScreenReport/${newestPendingCase!!.caseId}?scanNumber=$numberArg&startEdit=false")
                                }
                            )
                        }
                    }
                }
            }

            DermaBottomNavBar(
                onCameraClick = {
                    if (!dermaHasConsented) {
                        showDermaConsentDialog = true
                    } else {
                        onCameraClick()
                    }
                },
                cameraModifier = Modifier.onGloballyPositioned { coords ->
                    if (tutorial.getStepKey() == "camera_scanner") {
                        tutorial.currentTargetBounds = coords.boundsInWindow()
                    }
                }
            )
        }

        // 1) Show consent dialog first (on top of everything)
        if (showDermaConsentDialog && !dermaHasConsented) {
            DermaPrivacyDialog(
                show = showDermaConsentDialog,
                onAgree = {
                    vm.saveDermaConsent()
                    showDermaConsentDialog = false
                },
                onDismiss = {
                    showDermaConsentDialog = false
                },
                onViewTermsClick = {
                    navController.navigate("terms_privacy")
                }
            )
        }

// 2) Show tutorial ONLY when consent dialog is not showing
        if (showTutorial == true && !tutorial.isFinished() && !showDermaConsentDialog) {
            TutorialOverlay(
                tutorialManager = tutorial,
                onFinish = { scope.launch { tutorial.markSeen(context) } }
            )
        }


    }
}


// ... (The rest of the file remains the same, including all helper functions)

@Composable
fun StatCardRow(
    tutorial: DermaTutorialManager,
    onPendingCasesClick: () -> Unit,
    onTotalCasesClick: () -> Unit,
    pendingCount: Int,
    totalCount: Int
){
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatCard(
                label = "Pending Cases",
                value = pendingCount.toString(),
                imageRes = R.drawable.pending_cases,
                imageCardColor = Color(0xFFD7F2D6),
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coords ->
                        if (tutorial.getStepKey() == "pending_cases_tab") {
                            tutorial.currentTargetBounds = coords.boundsInWindow()
                        }
                    },
                onClick = onPendingCasesClick
            )
            StatCard(
                label = "Total Cases",
                value = totalCount.toString(),
                imageRes = R.drawable.total_cases,
                imageCardColor = Color(0xFFDCD2DE),
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coords ->
                        if (tutorial.getStepKey() == "case_history_tab") {
                            tutorial.currentTargetBounds = coords.boundsInWindow()
                        }
                    },
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
fun DermaLookupInline(
    tutorial: DermaTutorialManager,
    navController: NavController,
    hasConsented: Boolean,
    onRequireConsent: () -> Unit,
    modifier: Modifier = Modifier,
    onCameraClick: () -> Unit = {},
) {

    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    var reportCode by remember { mutableStateOf("") }
    var sharedData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    // ADD — dialog state
    var showDialog by remember { mutableStateOf(false) }
    var dialogPayload by remember { mutableStateOf<Map<String, Any>?>(null) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                if (tutorial.getStepKey() == "search_bar") {
                    tutorial.currentTargetBounds = coords.boundsInWindow()
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5FDFD))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = reportCode,
                onValueChange = { reportCode = it },
                label = { Text("Enter Report ID") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            PrimaryButton(
                text = "Search",
                onClick = {
                    if (!hasConsented) {
                        onRequireConsent()
                        return@PrimaryButton
                    }

                    scope.launch {
                        loading = true
                        error = null
                        sharedData = null
                        try {
                            val code = reportCode.trim().uppercase()

                            if (code.length != 4) {
                                error = "Please enter the 4-character Report Code shown on the PDF."
                                return@launch
                            }

                            // 🔎 Look up in lesion_case by a stored field "report_code" (e.g., last 4 of caseId)
                            // Make sure you write this field when creating the case doc.
                            val snap = db.collection("lesion_case")          // ✅ real collection
                                .whereEqualTo("report_code", code)
                                .limit(1)
                                .get()
                                .await()


                            if (snap.isEmpty) {
                                showDialog = false
                                dialogPayload = null
                                error = "No case found for that code."
                                return@launch
                            }


                            val doc = snap.documents.first()
                            val tsMsFromField = doc.getLong("timestamp_ms")                        // Long?
                            val tsMsFromStamp = doc.getTimestamp("timestamp")?.toDate()?.time      // Long?
                            val tsMs = tsMsFromField ?: tsMsFromStamp
                            val data = mutableMapOf<String, Any>(
                                "caseId" to doc.id,
                                "title" to (doc.getString("label") ?: "Scan"),
                                "probability" to (doc.getDouble("probability") ?: 0.0)
                            )
                            doc.getString("scan_url")?.let { data["scan_url"] = it }
                            doc.getString("heatmap_url")?.let { data["heatmap_url"] = it }

                            // ADD — pass report code and timestamp for the dialog
                            doc.getString("report_code")?.let { data["report_code"] = it }
                            (doc.getLong("timestamp_ms")
                                ?: doc.getTimestamp("timestamp")?.toDate()?.time
                                    )?.let { data["timestamp_ms"] = it }

                            // Keep your old state if you still want inline fallback
                            sharedData = data

                            // ADD — open the dialog
                            dialogPayload = data
                            showDialog = true

                        } catch (t: Throwable) {
                            error = when (t) {
                                is FirebaseFirestoreException ->
                                    if (t.code == FirebaseFirestoreException.Code.UNAVAILABLE)
                                        "No internet connection. Please check your network and try again."
                                    else "Error: ${t.code.name}"

                                else -> t.message ?: "Error fetching case."
                            }
                        } finally {
                            loading = false
                        }

                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            when {
                loading -> {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                error != null -> {
                    Text(text = error ?: "", color = Color.Red)
                }
            }

            // ADD — render popup
            if (showDialog && dialogPayload != null) {
                val title   = dialogPayload!!["title"] as? String ?: "Scan"
                val caseId  = dialogPayload!!["caseId"] as String
                val scanUrl = dialogPayload!!["scan_url"] as? String
                val heatUrl = dialogPayload!!["heatmap_url"] as? String
                val report  = dialogPayload!!["report_code"] as? String
                val tsMs    = (dialogPayload!!["timestamp_ms"] as? Number)?.toLong()

                CaseFoundDialog(
                    title = title,
                    caseId = caseId,
                    reportCode = report,
                    timestampMs = tsMs,
                    scanUrl = scanUrl,
                    heatUrl = heatUrl,
                    onOpen = {
                        showDialog = false
                        val searchedCaseId = Uri.encode(caseId)
                        navController.navigate("DermaAssessmentScreenReport/${searchedCaseId}?startEdit=true")
                    },
                    onDismiss = { showDialog = false }
                )
            }
        }
    }
}


@Composable
fun DermaBottomNavBar(
    onCameraClick: () -> Unit = {},
    cameraModifier: Modifier = Modifier

) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        // gradient bar (no icons)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
            )
        }

        // floating camera button (centered)
        Box(
            modifier = cameraModifier
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
                .clickable { onCameraClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.camera_fill),
                contentDescription = "Camera",
                modifier = Modifier
                    .size(30.dp)
            )
        }
    }
}


@Composable
fun DermaDropdownMenu(
    name: String,
    photoUri: Uri?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    assessmentLabel: String,                 // "My Initial Assessment" or "Start Assessment"
    onEditProfile: () -> Unit,
    onAssessmentClick: () -> Unit,
    onAboutClick: () -> Unit,
    onDataPrivacyClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    // Brand-ish colors
    val edge = listOf(Color(0xFFBFFDFD), Color(0xFF88E7E7), Color(0xFF55BFBF))
    val radius = 16.dp
    val rowH = 48.dp
    val divider = Color(0xFFECECEC)
    val labelColor = Color(0xFF1D1D1D)
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box {
        // Trigger (avatar + chevron)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(80))
                .clickable { onExpandedChange(true) }
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            if (photoUri != null) {
                AsyncImage(
                    model = photoUri,
                    contentDescription = "Profile",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(35.dp).clip(CircleShape)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.profilepicture),
                    contentDescription = "Profile",
                    modifier = Modifier.size(35.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Menu",
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF1D1D1D)
            )
        }

        // Dropdown panel (custom card inside)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.padding(top = 4.dp), // tiny breathing space
            offset = DpOffset(0.dp, 8.dp),
            containerColor = Color.Transparent
        ) {
            // Gradient edge wrapper
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .shadow(12.dp, RoundedCornerShape(radius))
                    .background(Brush.verticalGradient(edge), RoundedCornerShape(radius))
                    .padding(1.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(radius - 1.dp),
                    color = Color.White
                ) {
                    Column(Modifier.padding(vertical = 6.dp)) {
                        // Header — photo + name (tap → Edit Profile)
                        DermaDropdownHeader(
                            photoUri = photoUri,
                            name = name
                        ) {
                            onExpandedChange(false)
                            onEditProfile()
                        }

                        Divider(color = divider)


                        MenuRow(
                            label = "Edit Profile",
                            rowHeight = rowH,
                            onClick = {
                                onExpandedChange(false); onEditProfile()
                            }
                        ) {
                            IconBadge { Icon(Icons.Outlined.Edit, null, tint = Color(0xFF2B6E6E)) }
                        }


                        Divider(color = divider)

                        MenuRow(
                            label = "About",
                            rowHeight = rowH,
                            onClick = {
                                onExpandedChange(false); onAboutClick()
                            }
                        ) {
                            IconBadge { Icon(Icons.Outlined.Info, null, tint = Color(0xFF2B6E6E)) }
                        }

                        MenuRow(
                            label = "Data & Privacy",
                            rowHeight = rowH,
                            onClick = {
                                onExpandedChange(false); onDataPrivacyClick()
                            }
                        ) {
                            IconBadge { Icon(Icons.Outlined.Security, null, tint = Color(0xFF2B6E6E)) }
                        }

                        Divider(color = divider)

                        MenuRow(
                            label = "Logout",
                            rowHeight = rowH,
                            labelColor = Color(0xFFB00020),
                            onClick = {
                                showLogoutDialog = true
                            }
                        ) {
                            IconBadge(bg = Color(0xFFFFF1F1)) { Icon(Icons.Outlined.Logout, null, tint = Color(0xFFB00020)) }
                        }
                    }
                }
            }
        }
    }
    DialogTemplate(
        show = showLogoutDialog,
        title = "Confirm logout?",
        primaryText = "Yes, Logout",
        onPrimary = {
            showLogoutDialog = false
            onExpandedChange(false)
            onLogoutClick()  // <-- delegate to the screen
        },
        secondaryText = "Stay logged in",
        onSecondary = { showLogoutDialog = false },
        onDismiss = { showLogoutDialog = false }
    )
}

@Composable
private fun DermaDropdownHeader(
    photoUri: Uri?,
    name: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (photoUri != null) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(45.dp).clip(CircleShape)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.profilepicture),
                contentDescription = "Default photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(45.dp).clip(CircleShape)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = name.ifBlank { "User" },
            style = MaterialTheme.typography.headlineSmall.copy(color = Color(0xFF1D1D1D)),
            maxLines = 1
        )
    }
}

/* ---------- Reusable bits ---------- */

@Composable
private fun IconBadge(
    bg: Color = Color(0xFFEFF9F9),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.size(36.dp).background(bg, CircleShape),
        contentAlignment = Alignment.Center,
        content = content
    )
}



@Composable
private fun MenuRow(
    label: String,
    rowHeight: Dp,
    labelColor: Color = Color(0xFF1D1D1D),
    onClick: () -> Unit,
    leading: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading icon badge
        content()

        Spacer(Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = labelColor),
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9AA7A7)
        )
    }
}

private fun riskLevelLabel(probability: Float): String {
    val p = probability * 100f
    return when {
        probability < 0.0112f -> "Very Low"
        p < 10f  -> "Very Low"
        p < 30f  -> "Low"
        p < 60f  -> "Moderate"
        p < 80f  -> "Elevated"
        else     -> "High"
    }
}

private fun possibleListFor(probability: Float): List<String> {
    val p = probability * 100f
    val alerted = probability >= 0.0112f
    return if (!alerted) {
        // Assuming LesionIds.benignIds is defined elsewhere
        // LesionIds.benignIds
        emptyList()
    } else {
        when {
            p < 10f -> emptyList()//LesionIds.lt30Ids
            p < 30f -> emptyList()//LesionIds.lt30Ids
            p < 60f -> emptyList()//LesionIds.lt60Ids
            p < 80f -> emptyList()//LesionIds.lt80Ids
            else    -> emptyList()//LesionIds.gte80Ids
        }
    }
}

@Composable
private fun CaseFoundDialog(
    title: String,
    caseId: String,
    reportCode: String?,
    timestampMs: Long?,
    scanUrl: String?,
    heatUrl: String?,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateText = remember(timestampMs) {
        timestampMs?.takeIf { it > 0L }?.let {
            java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(it))
        } ?: "—"
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text("CASE FOUND", style = MaterialTheme.typography.titleLarge, color = Color(0xFF0FB2B2))
                Spacer(Modifier.height(6.dp))
                // Meta
                Text("Date: $dateText", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                Text("Report ID: ${reportCode ?: "—"}", style = MaterialTheme.typography.bodyMedium, color = Color.Black)

                Spacer(Modifier.height(16.dp))

                // Images
                if (!scanUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    coil.compose.AsyncImage(
                        model = scanUrl,
                        contentDescription = "Original",
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (!heatUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    coil.compose.AsyncImage(
                        model = heatUrl,
                        contentDescription = "Heatmap",
                        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    PrimaryButton(
                        text = "Open",
                        onClick = onOpen,
                        modifier = Modifier.weight(1f).height(56.dp)
                    )
                    SecondaryButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp)
                    )
                }
            }
        }
    }
}
@Composable
fun DermaPrivacyDialog(
    show: Boolean,
    onAgree: () -> Unit,
    onDismiss: () -> Unit,
    onViewTermsClick: () -> Unit
) {
    if (!show) return

    Dialog(onDismissRequest = { /* block outside dismiss so they decide */ }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(25.dp))
                .widthIn(max = 500.dp)
                .border(0.5.dp, Color(0xFF0FB2B2), shape = RoundedCornerShape(25.dp))
                .padding(20.dp)
        ) {
            DermaPrivacyContent(
                onAgree = onAgree,
                onDismiss = onDismiss,
                onViewTermsClick = onViewTermsClick
            )
        }
    }
}

@Composable
private fun DermaPrivacyContent(
    onAgree: () -> Unit,
    onDismiss: () -> Unit,
    onViewTermsClick: () -> Unit
) {
    var isChecked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val atBottom by remember {
        derivedStateOf { scrollState.value >= scrollState.maxValue - 6 }
    }

    val fullText = """
As a dermatologist using DermTect, you may view and handle sensitive patient information and images.

By continuing, you confirm that you will:
• Use patient data only for clinical assessment and care
• Keep all case information confidential
• Avoid saving or sharing screenshots or reports outside secure channels
• Follow your clinic’s and local regulations on data privacy and record-keeping

You can review our full Terms & Privacy Policy at any time.
    """.trimIndent()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 450.dp, max = 520.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Clinical Data Privacy",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF0FB2B2)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = fullText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1D1D1D)
            )

            Spacer(modifier = Modifier.height(23.dp))

            // ✅ Checkbox area (same style as user)
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                        .background(
                            if (isChecked) Color(0xFF0FB2B2) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable { isChecked = !isChecked },
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecked) {
                        Text(
                            text = "✓",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "I understand that the cases I view may contain sensitive patient data and I will handle them confidentially according to clinic policy and local regulations.",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = Color(0xFF1D1D1D),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(23.dp))

            // ✅ Primary button – only enabled when checked
            PrimaryButton(
                text = "I Understand",
                onClick = {
                    if (isChecked) onAgree()
                },
                enabled = isChecked,
                cornerRadius = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SecondaryButton(
                text = "Not Now",
                onClick = onDismiss,
                cornerRadius = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🔗 View Full Terms & Privacy Policy",
                color = Color(0xFF0FB2B2),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .clickable { onViewTermsClick() }
                    .padding(vertical = 4.dp)
            )
        }

        // 🔽 Scroll hint like in user dialog
        AnimatedVisibility(
            visible = !atBottom && scrollState.maxValue > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
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
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = "Scroll down",
                    tint = Color(0xFF0FB2B2)
                )
            }
        }
    }
}

