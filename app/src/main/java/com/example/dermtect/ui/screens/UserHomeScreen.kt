package com.example.dermtect.ui.screens


import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dermtect.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dermtect.ui.viewmodel.UserHomeViewModel
import com.example.dermtect.model.NewsItem
import com.google.gson.Gson
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.dermtect.ui.tutorial.TutorialManager
import com.example.dermtect.ui.tutorial.TutorialOverlay
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import com.example.dermtect.ui.viewmodel.SharedProfileViewModel
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Security
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.Dp
import com.example.dermtect.ui.components.DialogTemplate
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.flowOf

@Composable
fun ProfileDropdownMenu(
    name: String,
    photoUri: Uri?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    assessmentLabel: String,                 // "My Initial Assessment" or "Start Assessment"
    onEditProfile: () -> Unit,
    onAssessmentClick: () -> Unit,
    onViewNotifications: () -> Unit,
    onAboutClick: () -> Unit,
    onDataPrivacyClick: () -> Unit,
    onLogoutClick: () -> Unit,
    unreadCount: Int            // 👈 NEW
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
            Box(
                modifier = Modifier.size(35.dp)
            ) {
                if (photoUri != null) {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.profilepicture),
                        contentDescription = "Profile",
                        modifier = Modifier
                            .matchParentSize()
                            .clip(CircleShape)
                    )
                }

                // 🔴 small dot if there are unread notifications
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(15.dp)
                            .background(Color.Red, CircleShape)
                    )
                }
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
                        ProfileDropdownHeader(
                            photoUri = photoUri,
                            name = name
                        ) {
                            onExpandedChange(false)
                            onEditProfile()
                        }

                        Divider(color = divider)

                        MenuRow(
                            label = "View Notifications",
                            rowHeight = rowH,
                            onClick = {
                                onExpandedChange(false); onViewNotifications()
                            }
                        ) {
                            IconBadge {
                                Box {
                                    Icon(
                                        Icons.Outlined.NotificationsNone,
                                        contentDescription = null,
                                        tint = Color(0xFF2B6E6E)
                                    )
                                    if (unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(8.dp)
                                                .background(Color.Red, CircleShape)
                                        )
                                    }
                                }
                            }
                        }


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
                            label = assessmentLabel,
                            rowHeight = rowH,
                            onClick = {
                                onExpandedChange(false); onAssessmentClick()
                            }
                        ) {
                            IconBadge { Icon(Icons.Outlined.Assignment, null, tint = Color(0xFF2B6E6E)) }
                        }



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
private fun ProfileDropdownHeader(
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


@Composable
fun UserHomeScreen(
    navController: NavController,
    tutorialManager: TutorialManager
) {
    val viewModel: UserHomeViewModel = viewModel()
    val sharedProfileViewModel: SharedProfileViewModel = viewModel()   // 👈 NEW

    var showConsentDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }              // 👈 keep only ONE
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val email by viewModel.email.collectAsState()
    val isGoogleAccount by viewModel.isGoogleAccount.collectAsState()
    val userRole = "user"
// 🔔 Unread notifications badge
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid

    val diagnosisFlow = remember(currentUserId) {
        if (currentUserId.isNullOrBlank()) {
            flowOf(emptyList<NotificationItem>())
        } else {
            buildDiagnosisNotificationFlow(currentUserId)
        }
    }

// Collect as state
    val diagnosisNotifications by diagnosisFlow.collectAsState(initial = emptyList())
    val unreadCount = diagnosisNotifications.count { !it.isRead }

// Load profile photo once
    LaunchedEffect(Unit) { sharedProfileViewModel.loadPhoto("users") }  // 👈 NEW
    val profileUri by sharedProfileViewModel.selectedImageUri.collectAsState()  // 👈 NEW

    val hasConsented by viewModel.hasConsented.collectAsState()
    val consentChecked by viewModel.consentChecked.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    // Make sure we only auto-show the consent dialog once per session
    var initialConsentDialogShown by rememberSaveable { mutableStateOf(false) }
    var pendingCameraAction by remember { mutableStateOf(false) }
    val newsItems by viewModel.newsItems.collectAsState()
    val isLoadingNews by viewModel.isLoadingNews.collectAsState()
    val highlightItem by viewModel.highlightItem.collectAsState()
    val gson = remember { Gson() }

    var hasInitialAssessment by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        try {
            hasInitialAssessment = viewModel.hasAnsweredQuestionnaire()
        } catch (_: Exception) {
            hasInitialAssessment = false
        }
    }


    val assessmentLabel = if (hasInitialAssessment == true) "My Initial Assessment" else "Start Assessment"

    LaunchedEffect(Unit) {
        viewModel.fetchUserInfo()
        viewModel.checkConsentStatus()
        viewModel.fetchNews()
    }
    LaunchedEffect(consentChecked, hasConsented) {
       if (consentChecked && !hasConsented && !initialConsentDialogShown) {
            showConsentDialog = true
            initialConsentDialogShown = true
        }
    }


    val tutorialSeen by viewModel.tutorialSeen.collectAsState()
// Local guard so it won’t reappear again in the same session after Skip/Finish
    var tutorialDismissedThisSession by rememberSaveable { mutableStateOf(false) }

// Only show when Firestore says not seen yet AND user hasn’t dismissed it in this session
    val shouldShowTutorial = (tutorialSeen == false) && !tutorialDismissedThisSession

    // Load once
    LaunchedEffect(Unit) { viewModel.loadTutorialSeenRemote() }

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
                        text = "$firstName!",
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
                        .onGloballyPositioned { coordinates ->
                            if (tutorialManager.getStepKey() == "profile_menu") {
                                tutorialManager.currentTargetBounds = coordinates.boundsInRoot()
                            }
                        }
                ) {
                    ProfileDropdownMenu(
                        name = "$firstName $lastName",
                        photoUri = profileUri,
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        assessmentLabel = assessmentLabel,
                        onEditProfile = {
                            val encodedFirstName = Uri.encode(firstName)
                            val encodedLastName = Uri.encode(lastName)
                            val encodedEmail = Uri.encode(email)
                            navController.navigate(
                                "profile/$encodedFirstName/$encodedLastName/$encodedEmail/$isGoogleAccount/$userRole"
                            )
                        },
                        onAssessmentClick = {
                            if (!hasConsented) {
                                showConsentDialog = true
                            } else {
                                navController.navigate("questionnaire")
                            }
                        },
                        onViewNotifications = {
                            if (!hasConsented) {
                                showConsentDialog = true
                            } else {
                                navController.navigate("notifications")
                            }
                        },
                        onAboutClick = { navController.navigate("about") },
                        onDataPrivacyClick = { navController.navigate("terms_privacy") },
                        onLogoutClick = {
                            FirebaseAuth.getInstance().signOut()
                            navController.navigate("login") {
                                popUpTo("user_home") {
                                    inclusive = true     // remove home itself too
                                }
                                launchSingleTop = true
                            }
                        },
                        unreadCount = unreadCount     // 👈 NEW
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

                HomeFeatureButtonsRow(
                    hasConsented = hasConsented,
                    tutorialManager = tutorialManager,
                    onShowConsentDialog = { showConsentDialog = true },
                    onSkinReportClick = { navController.navigate("history") },
                    onNearbyClinicsClick = { navController.navigate("nearby_clinics") }
                )

                Spacer(modifier = Modifier.height(15.dp))

                highlightItem?.let { item ->
                    HighlightCard(
                        item = item,
                        onHighlightClick = {
                            val json = Uri.encode(Gson().toJson(item))
                            navController.navigate("highlightarticle?newsJson=$json")
                        },
                        tutorialManager = tutorialManager
                    )
                }



                Spacer(modifier = Modifier.height(15.dp))

                when {
                    isLoadingNews -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    newsItems.isEmpty() -> {
                        Text(
                            text = "No news available at the moment.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    else -> {
                        NewsCarousel(
                            newsItems = newsItems,
                            onItemClick = { item ->
                                val json = Uri.encode(gson.toJson(item))
                                navController.navigate("article_detail_screen/$json")
                            },
                            tutorialManager = tutorialManager
                        )
                    }
                }
            }
        }

        BottomNavBar(
            navController = navController,
            hasConsented = hasConsented,
            onShowConsentDialog = {
                pendingCameraAction = true
                showConsentDialog = true
            },
            setPendingCameraAction = { pendingCameraAction = it },
            coroutineScope = coroutineScope,
            viewModel = viewModel,
            tutorialManager = tutorialManager,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }

    if (menuExpanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { menuExpanded = false }
        )
    }

    if (showConsentDialog && !hasConsented) {
        PrivacyConsentDialog(
            show = showConsentDialog,
            onConsent = {
                viewModel.saveUserConsent()
                showConsentDialog = false
            },
            onDecline = {
                showConsentDialog = false
            },
            onViewTermsClick = { navController.navigate("terms_privacy") }
        )
    }
    if (shouldShowTutorial) {
        TutorialOverlay(
            tutorialManager = tutorialManager,
            onFinish = {
                tutorialManager.currentTargetBounds = null
                tutorialDismissedThisSession = true          // 👈 hide immediately for this session
                viewModel.markTutorialSeenRemote()           // 👈 persist so it won’t show next login
            }
        )
    }

}


@Composable
fun HomeFeatureButtonsRow(
    hasConsented: Boolean,
    tutorialManager: TutorialManager,
    onShowConsentDialog: () -> Unit,
    onSkinReportClick: () -> Unit,
    onNearbyClinicsClick:  () -> Unit
) {

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeFeatureButton(
                label = "Skin Report",
                imageRes = R.drawable.skin_report,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        if (tutorialManager.getStepKey() == "skin_report") {
                            tutorialManager.currentTargetBounds = coordinates.boundsInRoot()
                        }
                    },
                onClick = {
                    if (!hasConsented) {
                        onShowConsentDialog()
                        return@HomeFeatureButton
                    }
                    onSkinReportClick()
                }

            )
            HomeFeatureButton(
                label = "Nearby Clinics",
                imageRes = R.drawable.nearby_clinics,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { coordinates ->
                        if (tutorialManager.getStepKey() == "nearby_clinics") {
                            tutorialManager.currentTargetBounds = coordinates.boundsInRoot()
                        }
                    },
                onClick = {
                    if (!hasConsented) {
                        onShowConsentDialog()
                        return@HomeFeatureButton
                    }
                    onNearbyClinicsClick()
                }

            )
        }
    }
}

@Composable
fun HomeFeatureButton(
    label: String,
    imageRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val cornerRadius = 15.dp

    Box(
        modifier = modifier
            .height(150.dp)
            .clickable { onClick() }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFBFFDFD),
                            Color(0xFF88E7E7),
                            Color(0xFF55BFBF)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .drawWithContent {
                    drawContent()
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        blendMode = BlendMode.Lighten
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = label,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
            }
        }
    }
}


@Composable
fun HighlightCard(
    onHighlightClick: () -> Unit,
    item: NewsItem,
    tutorialManager: TutorialManager
) {
    val cornerRadius = 15.dp
    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFE5D0),
            Color(0xFFFFD1A3)
        )
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .heightIn(min = 150.dp)       // ✅ allow growth, keep a minimum
            .clickable { onHighlightClick() },
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, RoundedCornerShape(cornerRadius))
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .drawWithContent {
                    drawContent()
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        ),
                        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                        blendMode = BlendMode.Lighten
                    )
                }
                .onGloballyPositioned { coords ->
                    if (tutorialManager.getStepKey() == "highlight_card") {
                        tutorialManager.currentTargetBounds = coords.boundsInRoot()
                    }
                }
                .padding(15.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.labelMedium.copy(color = Color.Black)
                    )
                }
                Image(
                    painter = painterResource(id = R.drawable.risk_image),
                    contentDescription = "Skin Check Icon",
                    modifier = Modifier.size(100.dp)
                )
            }
        }
    }
}



@Composable
fun NewsCarousel(
    newsItems: List<NewsItem>,
    onItemClick: (NewsItem) -> Unit,
    tutorialManager: TutorialManager
) {
    val cornerRadius = 15.dp

    val tutorialModifier = if (tutorialManager.getStepKey() == "news_carousel") {
        Modifier.onGloballyPositioned { coordinates ->
            val bounds = coordinates.boundsInRoot()
            tutorialManager.currentTargetBounds = bounds
        }
    } else {
        Modifier
    }

    LazyRow(
        modifier = tutorialModifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        items(newsItems) { item ->
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .clickable { onItemClick(item) }
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(cornerRadius),
                        clip = false
                    )

            ) {
                Column(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFF8F9FA),
                            shape = RoundedCornerShape(cornerRadius)
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(cornerRadius)
                        )
                ) {
                    when {
                        !item.imageUrl.isNullOrBlank() -> {
                            AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = cornerRadius,
                                            topEnd = cornerRadius
                                        )
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                        item.imageResId != null -> {
                            Image(
                                painter = painterResource(id = item.imageResId),
                                contentDescription = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = cornerRadius,
                                            topEnd = cornerRadius
                                        )
                                    ),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Black
                            ),
                            maxLines = 2
                        )
                    }
                }
            }
        }
    }
}






@Composable
fun BottomNavBar(
    navController: NavController,
    hasConsented: Boolean,
    onShowConsentDialog: () -> Unit,
    setPendingCameraAction: (Boolean) -> Unit,
    coroutineScope: CoroutineScope,
    viewModel: UserHomeViewModel,
    tutorialManager: TutorialManager,
    modifier: Modifier = Modifier
) {

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
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
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.6f),
                            Color.Black.copy(alpha = 0.3f)
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
                // Left & right spacers so the bar keeps its height and looks balanced
                Spacer(modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.size(30.dp))
            }
        }
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
                        .onGloballyPositioned { coordinates ->
                            if (tutorialManager.getStepKey() == "camera") {
                                tutorialManager.currentTargetBounds = coordinates.boundsInRoot()
                            }
                        }
                        .clickable {
                            if (!hasConsented) {
                                setPendingCameraAction(true)
                                onShowConsentDialog()
                                return@clickable
                            }

                            coroutineScope.launch {
                                try {
                                    val answered = viewModel.hasAnsweredQuestionnaire()
                                    navController.navigate(if (answered) "tutorial_screen1" else "tutorial_screen0")
                                } catch (_: Exception) {
                                    navController.navigate("tutorial_screen0")
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {

                    Image(
                        painter = painterResource(id = R.drawable.camera_fill),
                        contentDescription = "Camera",
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
    }

