package com.example.dermtect.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dermtect.R


@Composable
fun DermaAssessmentReportScreen(
    scanTitle: String = "Scan 1",
    lesionImage: Painter,
    assessmentResult: String = "Benign",
    notes: String = "No serious risks found. The lesion looks non-cancerous. Monitor for changes, and consult a dermatologist if needed.",
    onBackClick: () -> Unit,
    onSendReport: () -> Unit,
    onCancel: () -> Unit

) {
    BubblesBackground {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp, bottom = 10.dp)
        ) {
            BackButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 23.dp)
            )

            Text(
                text =  scanTitle,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .align(Alignment.Center) // This ensures the title is centered in the Box
                    .fillMaxWidth()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 110.dp, bottom = 50.dp)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Assessment Report title
            Text(
                text = "Assessment Report",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Lesion Image
            Image(
                painter = lesionImage,
                contentDescription = "Lesion Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(39.dp))

            Column(modifier = Modifier.fillMaxWidth(0.9f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Assessment: ")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Normal
                        )
                    ) {
                        append(assessmentResult)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Notes text
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("Notes: ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append(notes)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Normal,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(60.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp, top = 150.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var isReportSent by remember { mutableStateOf(false) }

                if (!isReportSent) {
                    Button(
                        onClick = { isReportSent = true },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .wrapContentHeight(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0FB2B2))
                    ) {
                        Text(
                            text = "Send Report to User",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    OutlinedButton(
                        onClick = { /* cancel logic */ },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .wrapContentHeight(),
                        border = BorderStroke(0.5.dp, Color(0xFF0FB2B2))
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                            color = Color(0xFF0FB2B2)
                        )
                    }
                } else {
                    // Report has been sent
                    Button(
                        onClick = { /* do nothing or show a message */ },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .wrapContentHeight(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF626262))
                    ) {
                        Text(
                            text = "Assessment Done",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),

                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(25.dp))

                    OutlinedButton(
                        onClick = { /* edit logic */ },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .wrapContentHeight(),
                        border = BorderStroke(0.5.dp, Color(0xFF0FB2B2))
                    ) {
                        Text(
                            text = "Edit Assessment",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
                            color = Color(0xFF0FB2B2)
                        )
                    }
                }
                }
            }
        }
    }
}