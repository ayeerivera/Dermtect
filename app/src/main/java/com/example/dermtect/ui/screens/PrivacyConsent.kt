package com.example.dermtect.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dermtect.ui.components.PrimaryButton
import com.example.dermtect.ui.components.SecondaryButton

@Composable
fun PrivacyConsentDialog(
    show: Boolean,
    onConsent: () -> Unit,
    onDecline: () -> Unit,
    onViewTermsClick: () -> Unit = {}
) {
    if (!show) return

    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, shape = RoundedCornerShape(25.dp))
                .widthIn(max = 500.dp)
                .border(0.5.dp, Color(0xFF0FB2B2), shape = RoundedCornerShape(25.dp))
                .padding(20.dp) // adds even margin from edges
        ) {
            PrivacyConsentContent(
                onConsent = onConsent,
                onDecline = onDecline,
                onViewTermsClick = onViewTermsClick
            )
        }
    }
}

@Composable
fun PrivacyConsentContent(
    onConsent: () -> Unit,
    onDecline: () -> Unit,
    onViewTermsClick: () -> Unit
) {
    var isChecked by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val atBottom by remember {
        derivedStateOf { scrollState.value >= scrollState.maxValue - 6 }
    }

    val fullText = "At DermTect, your privacy is our priority. We only collect the information needed to deliver accurate skin analyses and improve your experience. Your photos, reports, and skin data are stored safely to help track your progress and, when you choose, support dermatologist consultations.\n" +
            "\n" +
            "We never access personal details from your other apps or devices. Your data stays within DermTect and is used solely to enhance accuracy and provide better insights. Every file is protected with strict security standards and will never be shared without your clear permission.\n" +
            "\n" +
            "By continuing, you allow DermTect to securely store and process your information — helping you stay confident, informed, and glowing in your skin-health journey."


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 450.dp, max = 520.dp) // cap height so scrolling can happen
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Text(
            text = "We Care About Your Privacy",
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
                text = "I understand that my skin photos and reports will be securely stored to support analysis, track my progress, and allow dermatologist feedback when needed.",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = Color(0xFF1D1D1D),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(23.dp))

        val activeBrush = Brush.verticalGradient(
            listOf(Color(0xFF5FEAEA), Color(0xFF2A9D9D), Color(0xFF187878))
        )
        val disabledBrush = Brush.verticalGradient(
            listOf(Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575))
        )

        PrimaryButton(
            text = "Consent",
            onClick = onConsent,
            enabled = isChecked,
            cornerRadius = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        SecondaryButton(
            text = "Do Not Consent",
            onClick = onDecline,

            cornerRadius = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        // 🔗 New clickable link
        Text(
            text = "🔗 View Full Terms & Privacy Policy",
            color = Color(0xFF0FB2B2),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier
                .clickable { onViewTermsClick() }
                .padding(vertical = 4.dp)
        )
    }

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