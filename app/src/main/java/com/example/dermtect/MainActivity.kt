package com.example.dermtect

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.dermtect.ui.components.NearbyClinicsScreen
import com.example.dermtect.ui.screens.AboutScreen
import com.example.dermtect.ui.screens.ArticleDetailScreen
import com.example.dermtect.ui.screens.DermaHistoryScreen
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
import androidx.navigation.navDeepLink
import kotlinx.coroutines.delay
import com.example.dermtect.data.OnboardingPrefs
import com.google.firebase.auth.FirebaseAuth
import com.example.dermtect.ui.tutorial.TutorialManager
import com.example.dermtect.ui.screens.DermaTakePhotoScreen
import com.example.dermtect.ui.components.DermaAssessmentScreenReport
import com.example.dermtect.ui.screens.PendingCasesScreen
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavController
import com.example.dermtect.ui.screens.ChooseAccount
import com.example.dermtect.ui.screens.DermaProfileScreen
import com.example.dermtect.ui.screens.DermaRegister
import com.example.dermtect.ui.screens.DermaTermsPrivacyScreen

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

//                    composable("demo") {
//                        DemoPdfScreen()
//
//                    }

                    composable("splash") {
                        val context = LocalContext.current
                        SplashScreen(navController)

                        var didRoute by rememberSaveable { mutableStateOf(false) }

                        LaunchedEffect(Unit, authState) {
                            if (didRoute) return@LaunchedEffect

                            val existingUser = FirebaseAuth.getInstance().currentUser
                            when {
                                // Session already restored → decide by Firestore "role"
                                existingUser != null -> {
                                    didRoute = true
                                    // tiny paint delay so splash actually shows
                                    delay(250)
                                    routeToProperHome(navController)   // ← 🔑 use your helper here
                                }

                                // Rely on your AuthViewModel while Firebase warms up
                                authState is AuthViewModel.AuthUiState.SignedIn -> {
                                    didRoute = true
                                    delay(250)
                                    routeToProperHome(navController)   // ← 🔑 also here
                                }

                                authState is AuthViewModel.AuthUiState.SignedOut ||
                                        authState is AuthViewModel.AuthUiState.EmailUnverified -> {
                                    didRoute = true
                                    delay(250)
                                    if (!OnboardingPrefs.hasSeen(context)) {
                                        navController.navigate("onboarding_screen1") {
                                            popUpTo("splash") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        navController.navigate("choose_account") {
                                            popUpTo("splash") { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                }

                                // Loading → do nothing; keep showing SplashScreen
                            }
                        }
                    }

                    composable("onboarding_screen1") { OnboardingScreen1(navController) }
                    composable("onboarding_screen2") { OnboardingScreen2(navController) }
                    composable("onboarding_screen3") { OnboardingScreen3(navController) }
                    composable("login?role={role}") { backStackEntry ->
                        val role = backStackEntry.arguments?.getString("role") ?: "patient"
                        Login(navController, role) }
                    composable("choose_account") { ChooseAccount(navController) }
                    composable("register_user") { Register(navController) }
                    composable("derma_register") { DermaRegister(navController) }
                    composable("forgot_pass1") { ForgotPass1(navController) }
                    composable("forgot_pass2?email={email}") { backStackEntry ->
                        val email = backStackEntry.arguments?.getString("email") ?: ""
                        ForgotPass2(navController, email)
                    }

                    composable("forgot_pass3") { ForgotPass3(navController) }
                    composable("forgot_pass4") { ForgotPass4(navController) }
                    composable("terms_privacy") { TermsPrivacyScreen(navController) }
                    composable("derma_terms_privacy") { DermaTermsPrivacyScreen(navController) }

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
                    composable(
                        route = "lesion_case/{caseId}",
                        arguments = listOf(navArgument("caseId"){ type = NavType.StringType }),
                        deepLinks = listOf(navDeepLink { uriPattern = "dermtect://case/{caseId}" })
                    ) { backStackEntry ->
                        val caseId = backStackEntry.arguments?.getString("caseId") ?: return@composable
                        LesionCaseScreen(
                            navController = navController,
                            caseId = caseId,
                            onBackClick = { navController.popBackStack() },
                            onFindClinicClick = { navController.navigate("nearby_clinics") },
                            onNavigateToAssessment = { navController.navigate("questionnaire") }
                        )
                    }





                    composable("article_detail_screen/{newsJson}") { backStackEntry ->
                        val json = backStackEntry.arguments?.getString("newsJson") ?: ""
                        val newsItem = Gson().fromJson(json, NewsItem::class.java)
                        ArticleDetailScreen(
                            newsItem = newsItem,
                            onBackClick = { navController.popBackStack() })
                    }


                    composable("tutorial_screen0") { TutorialScreen0(navController = navController) }
                    composable("tutorial_screen1") { TutorialScreen1(navController) }
                    composable("tutorial_screen2") { TutorialScreen2(navController) }
                    composable("tutorial_screen3") { TutorialScreen3(navController) }
                    composable("tutorial_screen4") { TutorialScreen4(navController) }
                    composable("tutorial_screen5") { TutorialScreen5(navController) }
                    composable(
                        route = "profile/{first}/{last}/{email}/{google}/{role}",
                        arguments = listOf(
                            navArgument("first")  { defaultValue = "" },
                            navArgument("last")   { defaultValue = "" },
                            navArgument("email")  { defaultValue = "" },
                            navArgument("google") { type = NavType.BoolType; defaultValue = false },
                            navArgument("role")   { defaultValue = "user" }
                        )
                    ) { backStackEntry ->
                        val first  = backStackEntry.arguments?.getString("first").orEmpty()
                        val last   = backStackEntry.arguments?.getString("last").orEmpty()
                        val email  = backStackEntry.arguments?.getString("email").orEmpty()
                        val google = backStackEntry.arguments?.getBoolean("google") ?: false
                        val role   = backStackEntry.arguments?.getString("role").orEmpty()

                        ProfileScreenTemplate(
                            navController = navController,
                            firstName = first,
                            lastName = last,
                            email = email,
                            isGoogleAccount = google,
                            userRole = role,                      // ← let the template switch UI by role
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
                            onTotalCasesClick   = { navController.navigate("case_history") },

                        firstName           = firstName,
                            onCameraClick       = {
                                navController.navigate("derma_take_photo") {
                                    launchSingleTop = true
                                    restoreState = false
                                    popUpTo("derma_home") { inclusive = false }
                                }
                            }
                        )
                    }
                    composable(
                        route = "derma_profile/{email}/{isGoogle}"
                    ) { backStackEntry ->
                        val emailArg = Uri.decode(backStackEntry.arguments?.getString("email") ?: "")
                        val isGoogle = backStackEntry.arguments?.getString("isGoogle")?.toBoolean() ?: false

                        DermaProfileScreen(
                            navController = navController,
                            email = emailArg,
                            isGoogleAccount = isGoogle,
                            sharedProfileViewModel = sharedProfileViewModel   // hoisted in MainActivity
                        )
                    }


                    composable("derma_take_photo") {
                        DermaTakePhotoScreen(onBackClick = { navController.popBackStack() })
                    }

                    composable(
                        route = "DermaAssessmentScreenReport/{caseId}?startEdit={startEdit}",
                        arguments = listOf(
                            navArgument("caseId") { type = NavType.StringType },
                            navArgument("startEdit") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val caseId = backStackEntry.arguments?.getString("caseId") ?: ""
                        val startEdit = backStackEntry.arguments?.getBoolean("startEdit") ?: false

                        DermaAssessmentScreenReport(
                            caseId = caseId,
                            startInEditMode = startEdit,
                            onBackClick = {
                                // 🔔 tell the previous screen it should refresh
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("refresh_history", true)

                                navController.popBackStack()
                            }
                        )
                    }


//

                    composable("pending_cases") { PendingCasesScreen(navController) }
                    composable("case_history") { DermaHistoryScreen(navController) }


                }

                }

            }
        }
    }
}

private fun routeToProperHome(navController: androidx.navigation.NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
        navController.navigate("choose_account") {
            popUpTo("splash") { inclusive = true }
            launchSingleTop = true
        }
        return
    }

    FirebaseFirestore.getInstance()
        .collection("users")
        .document(uid)
        .get()
        .addOnSuccessListener { doc ->
            val role = doc.getString("role")?.lowercase() ?: "user"
            val dest = if (role == "derma") "derma_home" else "user_home"
            navController.navigate(dest) {
                popUpTo("splash") { inclusive = true }
                launchSingleTop = true
            }
        }
        .addOnFailureListener {
            navController.navigate("user_home") {
                popUpTo("splash") { inclusive = true }
                launchSingleTop = true
            }
        }
}


private fun caseDetailRoute(rawId: String) = "case_detail/$rawId"

fun NavController.navigateCaseDetail(rawId: String) {
    val routeNow = currentBackStackEntry?.destination?.route
    val isAlreadyOnSameDetail =
        routeNow?.startsWith("case_detail/") == true &&
                currentBackStackEntry?.arguments?.getString("caseId") == rawId

    if (isAlreadyOnSameDetail) {
        // If you’re already on the same detail, go back to the list first
        popBackStack()
    }

    navigate(caseDetailRoute(rawId)) {
        launchSingleTop = true
        restoreState = false
    }
}
