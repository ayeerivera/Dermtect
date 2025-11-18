package com.example.dermtect.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dermtect.R

@Composable
fun ChooseAccount(navController: NavController) {
    BubblesBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
            ) {
                Text(
                    text = "Type of Account",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1D1D1D)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Choose type of your account",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
                    color = Color(0xFF1D1D1D),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(56.dp))

                AccountTypeButton(
                    title = "Regular User",
                    subtitle = "Scan and check your skin lesions, locate nearby clinics, and download a personal report.",
                    icon = Icons.Outlined.PersonOutline,
                    onClick = { navController.navigate("login?role=patient") }
                )

                Spacer(modifier = Modifier.height(24.dp))

                AccountTypeButton(
                    title = "Dermatologist",
                    subtitle = "Access patient reports, view skin cases, and provide diagnostic insights.",
                    icon = Icons.Outlined.HealthAndSafety
                    ,
                    onClick = { navController.navigate("login?role=derma") },
                    containerColor = Color(0xFFFFE3CD)
                )
            }
        }
    }
}

@Composable
fun AccountTypeButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color = Color(0xFFC5FFFF)
) {
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(182.dp)
            .padding(horizontal = 12.dp)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                clip = false
            )
            .background(
                color = containerColor,
                shape = shape
            )
            .border(
                width = 1.dp,
                color = Color(0xFFDDDDDD),
                shape = shape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1D1D1D)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                    color = Color(0xFF1D1D1D)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(90.dp),
                tint = Color.Black
            )
        }
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ChooseAccountPreview() {
    ChooseAccount(navController = rememberNavController())
}
