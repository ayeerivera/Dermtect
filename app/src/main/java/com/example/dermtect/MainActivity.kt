package com.example.dermtect

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dermtect.ui.components.DialogTemplate

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.dermtect.ui.screens.CameraPermissionGate
import com.example.dermtect.ui.screens.TakePhotoScreen
import com.example.dermtect.model.Clinic
import com.example.dermtect.model.NewsItem
import com.example.dermtect.ui.components.DermaAssessmentReportScreen
import com.example.dermtect.ui.components.NearbyClinicsScreen
import com.example.dermtect.ui.screens.AboutScreen
import com.example.dermtect.ui.screens.ArticleDetailScreen
import com.example.dermtect.ui.screens.DermaHistoryScreen
import com.example.dermtect.ui.screens.DermaAssessmentScreen
import com.example.dermtect.ui.screens.Register
import com.example.dermtect.ui.screens.Login
import com.example.dermtect.ui.screens.DermaHomeScreen
import com.example.dermtect.ui.screens.ForgotPass1
import com.example.dermtect.ui.screens.ForgotPass2
import com.example.dermtect.ui.screens.ForgotPass3
import com.example.dermtect.ui.screens.ForgotPass4
import com.example.dermtect.ui.screens.HighlightArticle
import com.example.dermtect.ui.screens.HistoryScreen
import com.example.dermtect.ui.screens.UserHomeScreen
import com.example.dermtect.ui.screens.QuestionnaireScreen
import com.example.dermtect.ui.screens.NotificationScreen
import com.example.dermtect.ui.screens.OnboardingScreen1
import com.example.dermtect.ui.screens.OnboardingScreen2
import com.example.dermtect.ui.screens.OnboardingScreen3
import com.example.dermtect.ui.components.SettingsScreenTemplate
import com.example.dermtect.ui.screens.SplashScreen
import com.example.dermtect.ui.screens.TutorialScreen0
import com.example.dermtect.ui.screens.TutorialScreen1
import com.example.dermtect.ui.screens.TutorialScreen2
import com.example.dermtect.ui.screens.TutorialScreen3
import com.example.dermtect.ui.screens.TutorialScreen4
import com.example.dermtect.ui.screens.TutorialScreen5
import com.example.dermtect.ui.screens.PendingCasesScreen
import com.example.dermtect.ui.components.ProfileScreenTemplate
import com.example.dermtect.ui.screens.ClinicTemplateScreen
import com.example.dermtect.ui.screens.LesionCaseScreen
import com.example.dermtect.ui.screens.TermsPrivacyScreen
import com.example.dermtect.ui.theme.DermtectTheme
import com.example.dermtect.ui.viewmodel.DermaHomeViewModel
import com.example.dermtect.ui.viewmodel.SharedProfileViewModel
import com.example.dermtect.ui.viewmodel.UserHomeViewModel
import com.google.firebase.FirebaseApp
import com.google.gson.Gson
import kotlin.jvm.java
import com.example.dermtect.data.repository.AuthRepositoryImpl
import com.example.dermtect.domain.usecase.AuthUseCase
import com.example.dermtect.ui.viewmodel.AuthViewModel
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay
import com.example.dermtect.data.OnboardingPrefs
import com.google.firebase.auth.FirebaseAuth
import com.example.dermtect.ui.tutorial.TutorialManager
import kotlinx.coroutines.launch
import android.graphics.Color as AColor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.FontWeight
import com.example.dermtect.pdf.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        val app = FirebaseApp.getInstance()
        Log.d("FirebaseCheck", "Firebase project: ${app.options.projectId}")

        setContent {

            DermtectTheme {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.White // opaque root
                ) {
                val navController = rememberNavController()
                val tutorialManager = remember { TutorialManager() } // ✅ create once here


                val sharedProfileViewModel: SharedProfileViewModel = viewModel()
                val userHomeViewModel: UserHomeViewModel = viewModel()

                val authUseCase = AuthUseCase(repository = AuthRepositoryImpl())
                val authVm: AuthViewModel = viewModel(factory = AuthViewModelFactory(authUseCase))
                val authState by authVm.authState.collectAsState()



                NavHost(navController = navController, startDestination = "splash") {
                    composable("demo") { DemoPdfScreen() }

                    composable("demo") {
                        DemoPdfScreen()

                    }

                    composable("splash") {
                        val context = LocalContext.current
                        SplashScreen(navController)

                        var didRoute by rememberSaveable { mutableStateOf(false) }

                        // 🔹 Fast-path: if Firebase already has a user, skip onboarding/login immediately
                        LaunchedEffect(Unit) {
                            if (didRoute) return@LaunchedEffect
                            val existingUser = FirebaseAuth.getInstance().currentUser
                            if (existingUser != null) {
                                // tiny delay so splash paints at least a frame
                                delay(250)
                                didRoute = true
                                navController.navigate("user_home") {
                                    popUpTo("splash") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }

                        // 🔹 Otherwise, react to your AuthViewModel state
                        LaunchedEffect(authState) {
                            if (didRoute) return@LaunchedEffect

                            when (authState) {
                                AuthViewModel.AuthUiState.Loading -> {
                                    // show Splash while Firebase restores session
                                }

                                is AuthViewModel.AuthUiState.SignedIn -> {
                                    delay(250)
                                    didRoute = true
                                    navController.navigate("user_home") {
                                        popUpTo("splash") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }

                                AuthViewModel.AuthUiState.SignedOut,
                                is AuthViewModel.AuthUiState.EmailUnverified -> {
                                    val seen = OnboardingPrefs.hasSeen(context)
                                    delay(250)
                                    didRoute = true
                                    if (!seen) {
                                        navController.navigate("onboarding_screen1") {
                                            popUpTo("splash") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        navController.navigate("login") {
                                            popUpTo("splash") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        }
                    }

                    composable("onboarding_screen1") { OnboardingScreen1(navController) }
                    composable("onboarding_screen2") { OnboardingScreen2(navController) }
                    composable("onboarding_screen3") { OnboardingScreen3(navController) }
                    composable("login") { Login(navController = navController) }
                    composable("register") { Register(navController = navController) }
                    composable("forgot_pass1") { ForgotPass1(navController) }
                    composable("forgot_pass2?email={email}") { backStackEntry ->
                        val email = backStackEntry.arguments?.getString("email") ?: ""
                        ForgotPass2(navController, email)
                    }

                    composable("forgot_pass3") { ForgotPass3(navController) }
                    composable("forgot_pass4") { ForgotPass4(navController) }
                    composable("terms_privacy") { TermsPrivacyScreen(navController) }
                    composable("user_home") {
                        UserHomeScreen(
                            navController = navController,
                            tutorialManager = tutorialManager
                        )
                    }
                    composable("notifications") { NotificationScreen(navController = navController) }
                    composable("questionnaire") { QuestionnaireScreen(navController = navController) }
                    composable("camera") {
                        CameraPermissionGate(
                            onGranted = {
                                TakePhotoScreen(
                                    onBackClick = {
                                        navController.navigate("user_home") {
                                            popUpTo("user_home") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    onFindClinicClick = { navController.navigate("nearby_clinics") },
                                    onNavigateToAssessment = { navController.navigate("questionnaire") } // NEW

                                )
                            }
                        )
                    }
                    composable(
                        route = "highlightarticle?newsJson={newsJson}",
                        arguments = listOf(
                            navArgument("newsJson") { defaultValue = ""; nullable = true }
                        )
                    ) { backStackEntry ->
                        val json = backStackEntry.arguments?.getString("newsJson") ?: ""
                        val newsItem = Gson().fromJson(Uri.decode(json), NewsItem::class.java)
                        HighlightArticle(
                            newsItem = newsItem,
                            onBackClick = { navController.popBackStack() })
                    }


                    composable("history") { HistoryScreen(navController = navController) }
                    composable("case_detail/{caseId}") { backStackEntry ->
                        val caseId = backStackEntry.arguments?.getString("caseId")!!
                        LesionCaseScreen(
                            navController = navController,
                            caseId = caseId,
                            onBackClick = { navController.popBackStack() },
                            onFindClinicClick = { navController.navigate("nearby_clinics") },
                            onNavigateToAssessment = { navController.navigate("questionnaire") }   // ✅

                        )
                    }

                    composable("article_detail_screen/{newsJson}") { backStackEntry ->
                        val json = backStackEntry.arguments?.getString("newsJson") ?: ""
                        val newsItem = Gson().fromJson(json, NewsItem::class.java)
                        ArticleDetailScreen(
                            newsItem = newsItem,
                            onBackClick = { navController.popBackStack() })
                    }
                    composable("user_settings") {
                        val context = LocalContext.current
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken("50445058822-fn9cea4e0bduos6t0g7ofb2g9ujri5s2.apps.googleusercontent.com")
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)

                        SettingsScreenTemplate(
                            navController = navController,
                            userRole = "user",
                            sharedProfileViewModel = sharedProfileViewModel,
                            onLogout = {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    authVm.logout {
                                        navController.navigate("login") {
                                            popUpTo(0) {
                                                inclusive = true
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable("tutorial_screen0") { TutorialScreen0(navController = navController) }
                    composable("tutorial_screen1") { TutorialScreen1(navController) }
                    composable("tutorial_screen2") { TutorialScreen2(navController) }
                    composable("tutorial_screen3") { TutorialScreen3(navController) }
                    composable("tutorial_screen4") { TutorialScreen4(navController) }
                    composable("tutorial_screen5") { TutorialScreen5(navController) }
                    composable(
                        route = "profile/{firstName}/{lastName}/{email}/{isGoogleAccount}/{userRole}"
                    ) { backStackEntry ->
                        val firstName = backStackEntry.arguments?.getString("firstName") ?: ""
                        val lastName = backStackEntry.arguments?.getString("lastName") ?: ""
                        val email = backStackEntry.arguments?.getString("email") ?: ""
                        val isGoogleAccount = backStackEntry.arguments?.getString("isGoogleAccount")
                            ?.toBooleanStrictOrNull() ?: false
                        val userRole = backStackEntry.arguments?.getString("userRole") ?: "user"
                        ProfileScreenTemplate(
                            navController = navController,
                            firstName = firstName,
                            lastName = lastName,
                            email = email,
                            isGoogleAccount = isGoogleAccount,
                            userRole = userRole,
                            sharedProfileViewModel = sharedProfileViewModel
                        )
                    }

                    composable("about") { AboutScreen(navController) }
                    composable("clinic_detail/{clinicJson}") { backStackEntry ->
                        val clinicJson = backStackEntry.arguments?.getString("clinicJson")
                        val clinic = try {
                            Gson().fromJson(Uri.decode(clinicJson), Clinic::class.java)
                        } catch (_: Exception) {
                            null
                        }

                        if (clinic != null) {
                            ClinicTemplateScreen(
                                name = clinic.name,
                                clinic = clinic,
                                onBackClick = { navController.popBackStack() },
                                onToggleSave = { userHomeViewModel.toggleClinicSave(clinic.id) },
                                viewModel = userHomeViewModel
                            )
                        } else {
                            Text("Invalid clinic data.")
                        }
                    }
                    composable("nearby_clinics") {
                        NearbyClinicsScreen(
                            navController = navController,
                            onBackClick = { navController.popBackStack() },
                            viewModel = userHomeViewModel
                        )
                    }

                    composable("derma_home") {
                        val viewModel: DermaHomeViewModel = viewModel()
                        val firstName by viewModel.firstName

                        DermaHomeScreen(
                            navController = navController,
                            onPendingCasesClick = { navController.navigate("pending_cases") },
                            onTotalCasesClick = { navController.navigate("case_history") },
                            onNotifClick = { navController.navigate("notifications") },
                            onSettingsClick = { navController.navigate("derma_settings") },
                            firstName = firstName
                        )
                    }
                    composable(
                        "derma_assessment_screen/{caseJson}",
                        arguments = listOf(navArgument("caseJson") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val caseJson = backStackEntry.arguments?.getString("caseJson")
                        val case = Gson().fromJson(
                            caseJson,
                            com.example.dermtect.ui.components.CaseData::class.java
                        )

                        DermaAssessmentScreen(
                            lesionImage = case.imageRes
                                ?: R.drawable.sample_skin, // fallback to a non-null drawable
                            scanTitle = case.label,                                 // use label instead of title
                            onBackClick = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() },
                            onSubmit = { diagnosis, notes ->
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("pending_cases") { PendingCasesScreen(navController) }
                    composable("case_history") { DermaHistoryScreen(navController) }
                    composable("assessment_report") {
                        DermaAssessmentReportScreen(
                            lesionImage = painterResource(id = R.drawable.sample_skin),
                            onBackClick = { navController.popBackStack() },
                            onSendReport = {},
                            onCancel = {}
                        )
                    }

                    composable("derma_settings") {
                        val context = LocalContext.current
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken("50445058822-fn9cea4e0bduos6t0g7ofb2g9ujri5s2.apps.googleusercontent.com")
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)

                        SettingsScreenTemplate(
                            navController = navController,
                            sharedProfileViewModel = sharedProfileViewModel,
                            userRole = "derma",
                            onLogout = {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    authVm.logout {
                                        navController.navigate("login") {
                                            popUpTo(0) {
                                                inclusive = true
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                }

            }
        }
    }
}
@Composable
fun DemoLesionCaseScreen(navController: androidx.navigation.NavController) {
    // quick timestamp
    val ts = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date())

    com.example.dermtect.ui.screens.LesionCaseTemplate(
        imageResId = com.example.dermtect.R.drawable.sample_skin, // use any placeholder you have
        imageBitmap = null,                     // we’ll let the template show from imageResId
        camBitmap = null,                       // will auto-fallback to a dummy “heatmap”-like tile
        title = "Result (Demo)",
        timestamp = ts,
        riskTitle = "Risk Assessment",
        riskDescription = "",                   // template now generates copy itself
        prediction = "Benign",
        probability = 0.07f,                    // 7% → will trigger “low concern” flow (≥ 1.12% tau)
        onBackClick = { navController.popBackStack() },
        onDownloadClick = { /* no-op in demo */ },
        onFindClinicClick = { /* no-op in demo */ },
        showPrimaryButtons = false,             // show the post-save actions
        showSecondaryActions = true
    )
}

@Composable
fun SavePrivacyDialogDemo() {
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var consentToSave by remember { mutableStateOf(false) }
    val fakeProbability = 0.72f
    val needsDermaReview = fakeProbability * 100f >= 60f
    var isSaving by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = { showPrivacyDialog = true }) {
            Text("Show Privacy Dialog")
        }

        DialogTemplate(
            show = showPrivacyDialog,
            title = "Save & Privacy",
            description = "We’ll store this scan (photo and optional heatmap) securely in your account. " +
                    "Some scans may require a dermatologist’s review based on the analysis. " +
                    "If that happens, we’ll ask to securely send it for review.",
            primaryText = if (isSaving) "Saving…" else "Save",
            onPrimary = {
                isSaving = true
                // Simulate saving process
                kotlinx.coroutines.GlobalScope.launch {
                    kotlinx.coroutines.delay(2000)
                    isSaving = false
                    showPrivacyDialog = false
                }
            },
            secondaryText = "Cancel",
            onSecondary = {
                showPrivacyDialog = false
                consentToSave = false
            },
            onDismiss = {
                showPrivacyDialog = false
                consentToSave = false
            },
            primaryEnabled = consentToSave && !isSaving,
            secondaryEnabled = !isSaving,
            extraContent = {
                Text(
                    text = if (needsDermaReview)
                        "This scan may be sent for dermatologist review."
                    else
                        "This scan will only be saved to your account (not sent).",
                    color = if (needsDermaReview) Color(0xFFB00020) else Color.Gray,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = consentToSave, onCheckedChange = { consentToSave = it })
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "I agree to save this scan and, if needed, send it for review.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }
}

@Composable
private fun DemoPdfScreen() {

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var isWorking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "DermTect PDF Demo",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(8.dp))
                Text("Tap the button to generate a sample PDF and view it.")

                Spacer(Modifier.height(24.dp))

                Button(
                    enabled = !isWorking,
                    onClick = {
                        scope.launch {
                            try {
                                isWorking = true
                                status = "Generating…"

                                // 1) Make demo bitmaps (photo + heatmap)
                                val photo = makeDemoBitmap(640, 640, AColor.parseColor("#EEEFF7"))
                                val heatmap = makeDemoHeatmap(640, 640)

                                // 2) Demo “Possible Conditions” (1–6 shown in PDF)
                                val possible = listOf(
                                    "Common benign nevus",
                                    "Atypical/Dysplastic nevus",
                                    "Seborrheic keratosis",
                                    "Solar lentigo",
                                    "Lichen planus–like keratosis",
                                    "Dermatofibroma"
                                )

                                // 3) Demo questionnaire answers
                                val answers = listOf(
                                    "Do you usually get sunburned easily after ~15–20 mins without protection?" to "No",
                                    "Is your natural skin color fair or very fair?" to "No",
                                    "Have you ever had a severe sunburn?" to "Yes",
                                    "Do you have many moles or freckles?" to "No",
                                    "Family member diagnosed with skin cancer?" to "No",
                                    "Ever diagnosed/treated for skin cancer or precancer?" to "No",
                                    "Often >1 hour outdoors at 10am–4pm unprotected?" to "Sometimes",
                                    "Rarely or never use sunscreen?" to "No",
                                    "Seldom check your skin/moles?" to "Sometimes",
                                    "New or changing mole/spot in last 6 months?" to "No"
                                )

                                // 4) Build data → use a simple demo Report ID
                                val reportId = "DTX-DEMO-" +
                                        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())

                                val data = PdfExporter.CasePdfData(
                                    userFullName = "Jane Doe",
                                    birthday = "01/15/1996", // MM/dd/yyyy
                                    reportId = reportId,
                                    title = "DERMTECT CLINICAL ANALYSIS REPORT",
                                    timestamp = SimpleDateFormat(
                                        "MMM dd, yyyy HH:mm",
                                        Locale.getDefault()
                                    ).format(Date()),
                                    photo = photo,
                                    heatmap = heatmap,
                                    shortMessage = "This scan looks reassuring, with a very low likelihood of a serious issue. " +
                                            "You can continue your normal skincare routine. Just keep being mindful of your skin and how it changes over time.",
                                    possibleConditions = possible,   // << ensures the numbered list appears (1–6)
                                    answers = answers
                                )

                                // 5) Create and open PDF
                                val uri = withContext(Dispatchers.IO) {
                                    PdfExporter.createCasePdf(ctx, data)
                                }
                                status = "Opening…"
                                PdfExporter.openPdf(ctx, uri)
                                status = "Done"
                            } catch (t: Throwable) {
                                status = "Failed: ${t.message}"
                            } finally {
                                isWorking = false
                            }
                        }
                    }
                ) {
                    Text(if (isWorking) "Working…" else "Generate & View PDF")
                }

                Spacer(Modifier.height(12.dp))
                status?.let { Text(it) }
            }
        }
    }
}

/** Simple square demo bitmap with a gradient + label */
private fun makeDemoBitmap(w: Int, h: Int, bgColor: Int): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(bgColor)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = AColor.parseColor("#5A6ACF")
        strokeWidth = 10f
    }
    // frame
    c.drawRect(20f, 20f, w - 20f, h - 20f, paint)

    // diagonal accent
    paint.color = AColor.parseColor("#99A0FF")
    c.drawLine(20f, 20f, w - 20f, h - 20f, paint)
    c.drawLine(20f, h - 20f, w - 20f, 20f, paint)

    // label
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.DKGRAY
        textSize = 36f
        style = Paint.Style.FILL
    }
    c.drawText("Original Image (demo)", 28f, h - 40f, textPaint)
    return bmp
}


/** Fake “heatmap” overlay-ish look just for demo */
private fun makeDemoHeatmap(w: Int, h: Int): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(AColor.WHITE)

    // base rectangle
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.parseColor("#FFCDD2") // soft red
        style = Paint.Style.FILL
    }
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

    // hotspot circles
    p.color = AColor.parseColor("#E57373")
    c.drawCircle(w * 0.35f, h * 0.35f, w * 0.18f, p)

    p.color = AColor.parseColor("#EF5350")
    c.drawCircle(w * 0.6f, h * 0.55f, w * 0.22f, p)

    // label
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AColor.DKGRAY
        textSize = 36f
        style = Paint.Style.FILL
    }
    c.drawText("Processed / Analyzed (demo)", 28f, h - 40f, text)
    return bmp
}
