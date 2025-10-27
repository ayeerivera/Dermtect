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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
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


@Composable
fun ProfileDropdownMenu(
    name: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(80))
                .clickable { onExpandedChange(true) }
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.profilepicture),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(35.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Menu",
                modifier = Modifier.size(35.dp),
                tint = Color(0xFF1D1D1D)
            )
        }

        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
        ) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier.width(200.dp)
            ) {
                val itemFontSize = 18.sp
                val itemPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

                DropdownMenuItem(
                    text = { Text("Notifications", fontSize = itemFontSize) },
                    onClick = {
                        onExpandedChange(false)
                        onNotificationsClick()
                    },
                    contentPadding = itemPadding
                )
                DropdownMenuItem(
                    text = { Text("Settings", fontSize = itemFontSize) },
                    onClick = {
                        onExpandedChange(false)
                        onSettingsClick()
                    },
                    contentPadding = itemPadding
                )
                DropdownMenuItem(
                    text = { Text("Profile", fontSize = itemFontSize) },
                    onClick = {
                        onExpandedChange(false)
                        onProfileClick()
                    },
                    contentPadding = itemPadding
                )
                DropdownMenuItem(
                    text = { Text("Logout", fontSize = itemFontSize) },
                    onClick = {
                        onExpandedChange(false)
                        onLogoutClick()
                    },
                    contentPadding = itemPadding
                )
            }
        }
    }
}


@Composable
fun UserHomeScreen(
    navController: NavController,
    tutorialManager: TutorialManager
) {
    val viewModel: UserHomeViewModel = viewModel()
    var showConsentDialog by remember { mutableStateOf(false) }

    val firstName by viewModel.firstName.collectAsState()
    val lastName by viewModel.lastName.collectAsState()
    val email by viewModel.email.collectAsState()
    val isGoogleAccount by viewModel.isGoogleAccount.collectAsState()
    val userRole = "user"

    val hasConsented by viewModel.hasConsented.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val consentChecked by viewModel.consentChecked.collectAsState()
    var pendingCameraAction by remember { mutableStateOf(false) }
    val newsItems by viewModel.newsItems.collectAsState()
    val isLoadingNews by viewModel.isLoadingNews.collectAsState()
    val highlightItem by viewModel.highlightItem.collectAsState()
    val gson = remember { Gson() }

    var showTutorial by remember { mutableStateOf(true) }

    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchUserInfo()
        viewModel.checkConsentStatus()
        viewModel.fetchNews()
    }

    LaunchedEffect(consentChecked, hasConsented) {
        if (consentChecked && !hasConsented) {
            showConsentDialog = true
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
                            if (tutorialManager.getStepKey() == "notification") {
                                tutorialManager.currentTargetBounds = coordinates.boundsInRoot()
                            }
                        }
                ) {
                    ProfileDropdownMenu(
                        name = firstName,
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        onNotificationsClick = {
                            if (!hasConsented) {
                                showConsentDialog = true
                                return@ProfileDropdownMenu
                            }
                            navController.navigate("notifications")
                        },
                        onSettingsClick = {
                            navController.navigate("user_settings")
                        },
                        onProfileClick = {
                            val encodedFirstName = Uri.encode(firstName)
                            val encodedLastName = Uri.encode(lastName)
                            val encodedEmail = Uri.encode(email)
                            navController.navigate("profile/$encodedFirstName/$encodedLastName/$encodedEmail/$isGoogleAccount/$userRole")
                        },
                        onLogoutClick = {
                            navController.navigate("login")
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

    if (showTutorial) {
        TutorialOverlay(
            tutorialManager = tutorialManager,
            onFinish = {
                showTutorial = false
                tutorialManager.currentTargetBounds = null
            }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp)
            .height(150.dp)
            .clickable { onHighlightClick() }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
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
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Black)
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
                Image(
                    painter = painterResource(id = R.drawable.home_icon),
                    contentDescription = "Home",
                    modifier = Modifier
                        .size(30.dp)
                        .onGloballyPositioned { coords ->
                            if (tutorialManager.getStepKey() == "home") {
                                tutorialManager.currentTargetBounds = coords.boundsInRoot()
                            }
                        }
                )
                Image(
                    painter = painterResource(id = R.drawable.user_vector),
                    contentDescription = "Settings",
                    modifier = Modifier
                        .size(26.dp)
                        .onGloballyPositioned { coords ->
                            if (tutorialManager.getStepKey() == "settings") {
                                tutorialManager.currentTargetBounds = coords.boundsInRoot()
                            }
                        }
                        .clickable {
                            if (!hasConsented) {
                                onShowConsentDialog()
                                return@clickable
                            }
                            navController.navigate("user_settings")
                        }
                )
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


@Preview(showBackground = true)
@Composable
fun UserHomeScreenPreview() {
    val tutorialManager = TutorialManager()
    val navController = rememberNavController()

    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                    Text(
                        text = "Hello,",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Normal,
                            fontSize = 32.sp,
                            color = Color(0xFF1D1D1D)
                        )
                    )
                    Text(
                        text = "User!",
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
                ) {
                    ProfileDropdownMenu(
                        name = "User",
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        onNotificationsClick = {},
                        onSettingsClick = {},
                        onProfileClick = {},
                        onLogoutClick = {}
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
                    hasConsented = true,
                    tutorialManager = tutorialManager,
                    onShowConsentDialog = { },
                    onSkinReportClick = { },
                    onNearbyClinicsClick = { }
                )
            }
        }

        if (menuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { menuExpanded = false }
            )
        }
    }
}
