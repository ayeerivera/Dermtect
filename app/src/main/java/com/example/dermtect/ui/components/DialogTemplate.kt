package com.example.dermtect.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.CheckboxDefaults.colors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dermtect.R
import kotlinx.coroutines.delay


@Composable
fun DialogTemplate(
    show: Boolean,
    title: String,
    description: String? = null,
    imageResId: Int? = null,
    imageContent: (@Composable () -> Unit)? = null,
    primaryText: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryText: String? = null,
    onTertiary: (() -> Unit)? = null,
    autoDismiss: Boolean = false,
    dismissDelay: Long = 1000L,
    onDismiss: () -> Unit,
    extraContent: @Composable (() -> Unit)? = null,
    primaryEnabled: Boolean = true,
    secondaryEnabled: Boolean = true,
    tertiaryEnabled: Boolean = true
) {
    if (show) {
        LaunchedEffect(show) {
            if (autoDismiss && primaryText == null && secondaryText == null && tertiaryText == null) {
                delay(dismissDelay)
                onDismiss()
            }
        }

        Dialog(onDismissRequest = { onDismiss() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        textAlign = TextAlign.Center,
                        color = Color(0xFF0FB2B2),
                        modifier = Modifier.fillMaxWidth()
                    )


                    imageContent?.let {
                        Spacer(modifier = Modifier.height(15.dp))
                        it()
                    } ?: imageResId?.let {
                        Spacer(modifier = Modifier.height(15.dp))
                        Image(
                            painter = painterResource(id = it),
                            contentDescription = null,
                            modifier = Modifier.size(150.dp)
                        )
                    }


                    description?.let {
                        Spacer(modifier = Modifier.height(15.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = Color.DarkGray
                        )
                    }

                    extraContent?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        it()
                    }


                    Spacer(modifier = Modifier.height(20.dp))

                    val buttonCount = listOfNotNull(primaryText, secondaryText, tertiaryText).size
                    val allFilled = buttonCount == 3
                    val disabledAlpha = 0.45f

                    Spacer(modifier = Modifier.height(20.dp))

// 🔹 PRIMARY (Teal gradient)
                    primaryText?.let {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(50.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    clip = false
                                )
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF5FEAEA), // top lighter teal
                                            Color(0xFF2A9D9D), // middle
                                            Color(0xFF187878)  // bottom darker teal
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = primaryEnabled) {
                                    onPrimary?.invoke()
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = it,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

// 🔹 SECONDARY (Gray gradient)
                    secondaryText?.let {
                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(50.dp)
                                .shadow(
                                    elevation = 6.dp, // soft shadow for embossed effect
                                    shape = RoundedCornerShape(12.dp),
                                    clip = false
                                )
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFDDDDDD), // light gray accent border
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = secondaryEnabled) {
                                    onSecondary?.invoke()
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = it,
                                color = if (secondaryEnabled) Color(0xFF4F4F4F) else Color(
                                    0xFFBDBDBD
                                ), // darker gray text
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

// 🔹 TERTIARY (Flat Text Button — “Cancel” style)
                    tertiaryText?.let {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(50.dp)
                                .shadow(
                                    elevation = 6.dp, // soft shadow for embossed effect
                                    shape = RoundedCornerShape(12.dp),
                                    clip = false
                                )
                                .background(
                                    color = Color.White,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFFDDDDDD), // light gray accent border
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = secondaryEnabled) {
                                    onSecondary?.invoke()
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = it,
                                color = if (secondaryEnabled) Color(0xFF4F4F4F) else Color(
                                    0xFFBDBDBD
                                ), // darker gray text
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun Dialog1(navController: NavController) {
    var show by remember { mutableStateOf(true) }

    DialogTemplate(
        show = show,
        title = "Saved!",
        imageResId = R.drawable.pdf_download,
        autoDismiss = true,
        onDismiss = { show = false }
    )
}

@Composable
fun Dialog2(navController: NavController) {
    var show by remember { mutableStateOf(true) }

    DialogTemplate(
        show = show,
        title = "Exit?",
        description = "Your answers won’t be saved.",
        primaryText = "Yes, exit",
        onPrimary = { navController.popBackStack() },
        secondaryText = "Cancel",
        onSecondary = { show = false },
        onDismiss = { show = false }
    )
}

@Composable
fun Dialog3(navController: NavController) {
    var show by remember { mutableStateOf(true) }

    DialogTemplate(
        show = show,
        title = "Action required",
        imageResId = R.drawable.pdf_download,
        primaryText = "Download",
        onPrimary = { /* Handle primary */ },
        secondaryText = "Cancel",
        onSecondary = { /* Handle cancel */ },
        tertiaryText = "Learn more",
        onTertiary = { /* Handle info */ },
        onDismiss = { show = false }
    )
}
