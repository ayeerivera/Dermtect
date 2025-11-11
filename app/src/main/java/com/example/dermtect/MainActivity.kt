package com.example.dermtect

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
import com.example.dermtect.ui.components.CaseData
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
import com.example.dermtect.ui.screens.LesionCaseTemplate
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
import com.example.dermtect.ui.screens.CameraPermissionGate
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import com.example.dermtect.data.OnboardingPrefs
import com.google.firebase.auth.FirebaseAuth
import com.example.dermtect.ui.tutorial.TutorialManager
import com.example.dermtect.ui.tutorial.TutorialOverlay


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        val app = FirebaseApp.getInstance()
        Log.d("FirebaseCheck", "Firebase project: ${app.options.projectId}")

        setContent {
            DermtectTheme {
                val navController = rememberNavController()
                val tutorialManager = remember { TutorialManager() } // ✅ create once here
                UserHomeScreen(
                    navController = navController,
                    tutorialManager = tutorialManager // ✅ Passed to screen
                )

                val sharedProfileViewModel: SharedProfileViewModel = viewModel()
                val userHomeViewModel: UserHomeViewModel = viewModel()

                val authUseCase = AuthUseCase(repository = AuthRepositoryImpl())
                val authVm: AuthViewModel = viewModel(factory = AuthViewModelFactory(authUseCase))
                val authState by authVm.authState.collectAsState()



                NavHost(navController = navController, startDestination = "splash") {

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
                    composable("user_home") {UserHomeScreen(
                        navController = navController,
                        tutorialManager = tutorialManager
                        )
                    }
                    composable("notifications") {NotificationScreen(navController = navController) }
                    composable("questionnaire") { QuestionnaireScreen(navController = navController)}
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
                                    onFindClinicClick = { navController.navigate("nearby_clinics") }
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
                        HighlightArticle(newsItem = newsItem, onBackClick = { navController.popBackStack() })
                    }


                    composable("history") { HistoryScreen(navController = navController)}
                    composable("case_detail/{caseId}") { backStackEntry ->
                        val caseId = backStackEntry.arguments?.getString("caseId")!!
                        LesionCaseScreen(
                            navController = navController,
                            caseId = caseId,
                            onBackClick = { navController.popBackStack()},
                            onFindClinicClick = { navController.navigate("nearby_clinics")},
                            onNavigateToAssessment = { navController.navigate("questionnaire") }   // ✅

                        )
                    }

                    composable("article_detail_screen/{newsJson}") { backStackEntry ->
                        val json = backStackEntry.arguments?.getString("newsJson") ?: ""
                        val newsItem = Gson().fromJson(json, NewsItem::class.java)
                        ArticleDetailScreen(newsItem = newsItem,
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

                    composable("tutorial_screen0") {TutorialScreen0(navController = navController) }
                    composable("tutorial_screen1") {TutorialScreen1(navController) }
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
                        val isGoogleAccount = backStackEntry.arguments?.getString("isGoogleAccount")?.toBooleanStrictOrNull() ?: false
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
                        val case = Gson().fromJson(caseJson, com.example.dermtect.ui.components.CaseData::class.java)

                        DermaAssessmentScreen(
                            lesionImage = case.imageRes ?: R.drawable.sample_skin, // fallback to a non-null drawable
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
                    composable("assessment_report") { DermaAssessmentReportScreen(
                        lesionImage = painterResource(id = R.drawable.sample_skin),
                        onBackClick = { navController.popBackStack() },
                        onSendReport = {},
                        onCancel = {}
                    ) }

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