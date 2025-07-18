package com.example.dermtect

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dermtect.model.NewsItem
import com.example.dermtect.ui.components.DermaAssessmentReportScreen
import com.example.dermtect.ui.components.FindClinic1Screen
import com.example.dermtect.ui.screens.AboutScreen
import com.example.dermtect.ui.screens.ArticleDetailScreen
import com.example.dermtect.ui.screens.CaseHistoryScreen
import com.example.dermtect.ui.screens.Register
import com.example.dermtect.ui.screens.Login
import com.example.dermtect.ui.screens.ChangePasswordScreen
import com.example.dermtect.ui.screens.DermaHomeScreen
import com.example.dermtect.ui.screens.ForgotPass1
import com.example.dermtect.ui.screens.ForgotPass2
import com.example.dermtect.ui.screens.ForgotPass3
import com.example.dermtect.ui.screens.ForgotPass4
import com.example.dermtect.ui.screens.HighlightArticle
import com.example.dermtect.ui.screens.HistoryScreen
import com.example.dermtect.ui.screens.MedicureClinicScreen
import com.example.dermtect.ui.screens.UserHomeScreen
import com.example.dermtect.ui.screens.QuestionnaireScreen
import com.example.dermtect.ui.screens.NotificationScreen
import com.example.dermtect.ui.screens.OnboardingScreen1
import com.example.dermtect.ui.screens.OnboardingScreen2
import com.example.dermtect.ui.screens.OnboardingScreen3
import com.example.dermtect.ui.screens.OrtizClinicScreen
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
import com.example.dermtect.ui.screens.DermaAssessmentScreen
import com.example.dermtect.ui.screens.SkinBenefitClinicScreen
import com.example.dermtect.ui.screens.SkinHealthClinicScreen
import com.example.dermtect.ui.screens.VMClinicScreen
import com.example.dermtect.ui.screens.VitalityClinicScreen
import com.example.dermtect.ui.theme.DermtectTheme
import com.example.dermtect.ui.viewmodel.DermaHomeViewModel
import com.example.dermtect.ui.viewmodel.SharedProfileViewModel
import com.google.firebase.FirebaseApp
import com.google.gson.Gson


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            DermtectTheme {
                val navController = rememberNavController()
                val sharedProfileViewModel: SharedProfileViewModel = viewModel()

                NavHost(navController = navController, startDestination = "user_home") {
                    composable("splash") { SplashScreen(navController) }
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
                    composable("highlightarticle/{newsJson}") { backStackEntry ->
                        val json = backStackEntry.arguments?.getString("newsJson") ?: ""
                        val newsItem = Gson().fromJson(json, NewsItem::class.java)
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
                        SettingsScreenTemplate(
                            navController = navController,
                            userRole = "user",
                            sharedProfileViewModel = sharedProfileViewModel, // ✅ pass the same instance
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
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
                    composable("clinic1") { FindClinic1Screen(navController) }
                    composable("medicure") { MedicureClinicScreen(navController) }
                    composable("vitality") { VitalityClinicScreen(navController) }
                    composable("skin_health") { SkinHealthClinicScreen(navController) }
                    composable("vm") { VMClinicScreen(navController) }
                    composable("ortiz") { OrtizClinicScreen(navController) }
                    composable("skin_benefit") { SkinBenefitClinicScreen(navController) }

                    composable("derma_home") {
                        val viewModel: DermaHomeViewModel = viewModel()
                        val firstName by viewModel.firstName

                        DermaHomeScreen(
                            navController = navController,
                            onPendingCasesClick = { navController.navigate("pending_cases") },
                            onTotalCasesClick = { navController.navigate("case_history") },
                            onNotifClick = { navController.navigate("notifications") },
                            onSettingsClick = { navController.navigate("derma_settings") },
                            firstName = firstName // ✅ Now resolved
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
                    composable("assessment") {  DermaAssessmentScreen(
                        scanTitle = "Scan 1",
                        lesionImage = painterResource(id = R.drawable.sample_skin),
                        onBackClick = { navController.popBackStack() },
                        onSubmit = { _, _ -> },
                        onCancel = {}
                    )}
                    composable("derma_settings") {
                        SettingsScreenTemplate(
                            navController = navController,
                            sharedProfileViewModel = sharedProfileViewModel,
                            userRole = "derma", // hides About section
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }




                }

            }
            }
        }
    }
