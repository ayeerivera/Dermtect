package com.example.dermtect

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cameradermtect.TakePhotoScreen
import com.example.dermtect.model.Clinic
import com.example.dermtect.model.NewsItem
import com.example.dermtect.ui.components.*
import com.example.dermtect.ui.screens.*
import com.example.dermtect.ui.theme.DermtectTheme
import com.example.dermtect.ui.viewmodel.*
import com.google.firebase.FirebaseApp
import com.google.gson.Gson
import com.example.dermtect.data.repository.AuthRepositoryImpl
import com.example.dermtect.domain.usecase.AuthUseCase
import com.example.dermtect.ui.viewmodel.AuthViewModel
import com.example.dermtect.ui.viewmodel.AuthViewModelFactory
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.example.cameradermtect.CameraPermissionGate


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            DermtectTheme {
                val navController = rememberNavController()
                val sharedProfileViewModel: SharedProfileViewModel = viewModel()
                val userHomeViewModel: UserHomeViewModel = viewModel()

                val authUseCase = AuthUseCase(repository = AuthRepositoryImpl())
                val authVm: AuthViewModel = viewModel(factory = AuthViewModelFactory(authUseCase))
                val authState by authVm.authState.collectAsState()
                
                NavHost(navController = navController, startDestination = "login") {
                    composable("splash") {
                        // Show your existing splash UI (but remove any internal navigation in it)
                        SplashScreen(navController)

                        LaunchedEffect(authState) {
                            when (authState) {
                                AuthViewModel.AuthUiState.Loading -> {
                                    // do nothing; wait for Firebase to restore the session
                                }

                                AuthViewModel.AuthUiState.SignedOut -> {
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }

                                is AuthViewModel.AuthUiState.EmailUnverified -> {
                                    // Minimal: still send to login (or create a VerifyEmail screen later)
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }

                                is AuthViewModel.AuthUiState.SignedIn -> {
                                    navController.navigate("user_home") {
                                        popUpTo("splash") { inclusive = true }
                                        launchSingleTop = true
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
                    composable("change_pass") { ChangePasswordScreen(navController) }
                    composable("user_home") {UserHomeScreen(navController = navController) }
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
                                    }
                                )
                            },
                            deniedContent = {
                                // Optional: nice UI if permission is denied
                                Column(Modifier.padding(24.dp)) {
                                    Text("We need the camera to scan lesions.")
                                    Spacer(Modifier.height(12.dp))
                                    Text("Please allow the Camera permission to continue.")
                                }
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
                        val case = Gson().fromJson(caseJson, CaseData::class.java)

                        DermaAssessmentScreen(
                            lesionImage = case.imageRes,
                            scanTitle = case.title,
                            onBackClick = { navController.popBackStack() },
                            onCancel = { navController.popBackStack() },
                            onSubmit = { diagnosis, notes ->
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("pending_cases") { PendingCasesScreen(navController) }
                    composable("case_history") { CaseHistoryScreen(navController) }
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
