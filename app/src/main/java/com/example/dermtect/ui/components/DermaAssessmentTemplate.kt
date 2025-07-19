package com.example.dermtect.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.dermtect.R
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.components.BubblesBackground

@Composable
fun DermaAssessmentScreen(
    scanTitle: String = "Scan 1",
    lesionImage: Int,
    onBackClick: () -> Unit,
    onSubmit: (diagnosis: String, notes: String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedDiagnosis by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

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
                text = scanTitle,
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
                .padding(top = 110.dp, bottom = 20.dp)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(18.dp))

            Image(
                painter = painterResource(id = lesionImage),
                contentDescription = "Lesion Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
            )

            Spacer(modifier = Modifier.height(27.dp))

            Column(modifier = Modifier.fillMaxWidth(0.9f)
            ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFD3FFFD), RoundedCornerShape(10.dp))
            ) {
                Column (modifier = Modifier.padding(10.dp)){
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.context_icon),
                            contentDescription = "Context Icon",
                            modifier = Modifier.size(16.67.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Additional Context",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "View Risk Assessment & Report",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Black
                    )
                }
            }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Assessment",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,

                    )

                Spacer(modifier = Modifier.height(19.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(21.dp)) {
                    Button(
                        onClick = { selectedDiagnosis = "Benign" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDiagnosis == "Benign") Color(0xFF0FB2B2) else Color(
                                0xFFC2C2C2
                            ),
                            contentColor = if (selectedDiagnosis == "Benign") Color.White else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Benign",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }

                    Button(
                        onClick = { selectedDiagnosis = "Malignant" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedDiagnosis == "Malignant") Color(0xFF0FB2B2) else Color(
                                0xFFC2C2C2
                            ),
                            contentColor = if (selectedDiagnosis == "Malignant") Color.White else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Malignant",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(19.dp))

                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(19.dp))

                TextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = {
                        Text(
                            "Write your notes here",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Normal,
                                color = Color.Gray
                            )
                        )
                    },
                    modifier = Modifier
                        .size(width = 311.dp, height = 92.dp)
                        .background(Color(0xFFF0F0F0), RoundedCornerShape(10.dp)),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        disabledContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(34.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp, top = 50.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { onSubmit(selectedDiagnosis, notes) },
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .wrapContentHeight()
                            .align(Alignment.CenterHorizontally),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0FB2B2))
                    ) {
                        Text(
                            text = "Submit Assessment",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .wrapContentHeight()
                            .align(Alignment.CenterHorizontally),
                        border = BorderStroke(0.5.dp, Color(0xFF0FB2B2))
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                            color = Color(0xFF0FB2B2)
                        )
                    }
                }
            }
        }
    }
}