package com.example.dermtect.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dermtect.ui.components.EmbossedButton

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
                .fillMaxWidth(0.9f)
                .background(Color.White, shape = RoundedCornerShape(25.dp))
                .border(0.5.dp, Color(0xFF0FB2B2), shape = RoundedCornerShape(25.dp))
                .padding(24.dp)
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

    val fullText = "DermTect is committed to protecting your information. We only collect the details necessary to provide accurate results and helpful features. Your skin photos, reports, and related information are stored securely to support analysis, track your progress, and enable dermatologist feedback when needed.\n" +
            "\n" +
            "We do not collect personal information from your other apps, websites, or outside sources. Your data is used only within DermTect to improve your experience and ensure accurate assessments. All information is handled with strict security and privacy measures, and will never be shared with third parties without your consent.\n" +
            "\n" +
            "By continuing, you acknowledge that DermTect may store and process your information responsibly to support your health journey."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 450.dp)
            .verticalScroll(rememberScrollState()),
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
                text = "I understand that my skin photos, reports, and details may be securely stored to support analysis, track results, and enable dermatologist feedback.",
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

        EmbossedButton(
            text = "Consent",
            onClick = onConsent,
            enabled = isChecked,
            backgroundBrush = if (isChecked) activeBrush else disabledBrush,
            textColor = Color.White,
            cornerRadius = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        EmbossedButton(
            text = "Do Not Consent",
            onClick = onDecline,
            backgroundBrush = Brush.linearGradient(
                listOf(Color(0xFFF0F0F0), Color(0xFFF0F0F0))
            ),
            textColor = Color(0xFF0FB2B2),
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
}