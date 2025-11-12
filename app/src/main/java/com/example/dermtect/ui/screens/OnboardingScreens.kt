package com.example.dermtect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.request.ImageRequest
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.components.ProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.dermtect.data.OnboardingPrefs
import android.os.Build
import com.example.dermtect.R
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.decode.ImageDecoderDecoder
import androidx.compose.runtime.*

@Composable
private fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(false)
            .diskCache(
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_gif_cache"))
                    .build()
            )
            .memoryCache(
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            )
            .build()
    }
}


@Composable
fun OnboardingScreen(
    imageRes: Int,
    title: String,
    description: String,
    buttonText: String = "Next",
    onNextClick: () -> Unit,
    currentIndex: Int? = null,
    onBackClick: (() -> Unit)? = null,
    nextImageRes: Int? = null,
) {
    val context = LocalContext.current
    val imageLoader = rememberGifImageLoader()

    LaunchedEffect(nextImageRes) {
        nextImageRes?.let { res ->
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(res)
                    .allowHardware(false)
                    .size(280)
                    .build()
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    )
    {
        // ✅ Place BackButton at top start if provided
        if (onBackClick != null) {
            BackButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 50.dp, start = 24.dp)

            )
        }


        Column(
            modifier = Modifier
                .fillMaxSize(0.9f)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .padding(top = 70.dp)
                    .verticalScroll(rememberScrollState()), // Add scroll support
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageRes)
                        .allowHardware(false)
                        .size(280)                 // decode to display size
                        .build(),
                    imageLoader = imageLoader,
                    contentDescription = null,
                    modifier = Modifier.size(280.dp)
                )

                Spacer(modifier = Modifier.height(60.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1D1D1D),
                    textAlign = TextAlign.Center
                )


                Spacer(modifier = Modifier.height(20.dp))


                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                    color = Color(0xFF1D1D1D),
                    textAlign = TextAlign.Center
                )


            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 70.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentIndex != null) {
                    ProgressIndicator(
                        totalDots = 3,
                        selectedIndex = currentIndex
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(56.dp)
                        .shadow(
                            elevation = 10.dp, // outer shadow strength
                            shape = RoundedCornerShape(15.dp),
                            clip = false
                        )
                ) {
                    Button(
                        onClick = onNextClick,
                        modifier = Modifier.matchParentSize(),
                        shape = RoundedCornerShape(15.dp),
                        elevation = ButtonDefaults.buttonElevation(0.dp), // disable default button shadow
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                // Background gradient
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF5FEAEA),
                                            Color(0xFF2A9D9D),
                                            Color(0xFF187878)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, Float.POSITIVE_INFINITY)
                                    ),
                                    shape = RoundedCornerShape(15.dp)
                                )
                                // Emboss border
                                .border(
                                    width = 1.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.6f),
                                            Color.Black.copy(alpha = 0.3f)
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                    ),
                                    shape = RoundedCornerShape(15.dp)
                                )
                                // Inner shine overlay
                                .drawWithContent {
                                    drawContent()
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.15f), // lowered a bit
                                                Color.Transparent
                                            )
                                        ),
                                        cornerRadius = CornerRadius(15.dp.toPx(), 15.dp.toPx()),
                                        blendMode = BlendMode.Lighten
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = buttonText,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.4f),
                                        offset = Offset(1f, 2f),
                                        blurRadius = 4f
                                    )
                                )
                            )
                        }
                    }
                }


            }
        }
    }
}




@Composable
fun OnboardingScreen1(navController: NavController) {
    OnboardingScreen(
        imageRes = com.example.dermtect.R.drawable.onboarding_1,
        title = "Welcome to DermTect!",
        description = "Your AI-powered tool for early skin health checks.",
        onNextClick = { navController.navigate("onboarding_screen2") },
        currentIndex = 0,
        nextImageRes = R.drawable.onboarding_2
    )
}


@Composable
fun OnboardingScreen2(navController: NavController) {
    OnboardingScreen(
        imageRes = com.example.dermtect.R.drawable.onboarding_2,
        title = "Scan. Assess. Find Help",
        description = "Take a photo, answer quick questions, and see instant insights.",
        onNextClick = { navController.navigate("onboarding_screen3") },
        onBackClick = { navController.popBackStack() }, // ✅ back to previous
        currentIndex = 1,
        nextImageRes = R.drawable.skin_health
    )
}


@Composable
fun OnboardingScreen3(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    OnboardingScreen(
        imageRes = com.example.dermtect.R.drawable.skin_health,
        title = "Your Skin Health,\nJust a Tap Away",
        description = "Easy, private, and reliable early detection—anytime, anywhere.",
        onNextClick = {
            scope.launch {
                // ✅ mark onboarding as completed (stored in DataStore)
                OnboardingPrefs.setSeen(context)

                // then go to Login
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        },
        onBackClick = { navController.popBackStack() }, // ✅ back to previous
        currentIndex = 2,
        nextImageRes = null
    )
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreen1Preview() {
    OnboardingScreen1(navController = rememberNavController())
}


@Preview(showBackground = true)
@Composable
fun OnboardingScreen2Preview() {
    OnboardingScreen2(navController = rememberNavController())
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreen3Preview() {
    OnboardingScreen3(navController = rememberNavController())
}