package com.example.dermtect.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dermtect.R
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TopRightNotificationIcon(
    onNotifClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(50.dp)
            .background(
                color = Color(0xFFCDFFFF),
                shape = CircleShape
            )
            .clickable { onNotifClick() },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.notifications_vector),
            contentDescription = "Notifications",
            modifier = Modifier.size(28.dp)
        )
    }
}
@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier

) {
    Box(
        modifier = modifier

            .size(50.dp)
            .shadow(
                elevation = 6.dp,
                shape = CircleShape
            )
            .background(
                color = Color.White,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.back), // Your back icon
            contentDescription = "Back",
            tint = Color.Black,
            modifier = Modifier.size(28.dp)
        )
    }
}


@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.8f)
            .wrapContentHeight(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color(0xFF0FB2B2) else Color(0xFFBDBDBD),
            contentColor = Color.White
        ),
        enabled = enabled
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
        )
    }
}


// Reusable EmbossedButton (same style as Onboarding)
@Composable
fun EmbossedButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Brush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF5FEAEA),
            Color(0xFF2A9D9D),
            Color(0xFF187878)
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY)
    ),
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(15.dp),
                clip = false
            )
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(15.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = textColor
            ),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = backgroundColor,
                        shape = RoundedCornerShape(15.dp)
                    )
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
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
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
                    text = text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = textColor,
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



