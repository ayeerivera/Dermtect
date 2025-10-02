package com.example.dermtect.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp

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


@Composable
fun EmbossedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,                // 👈 NEW
    cornerRadius: Dp = 15.dp,
    backgroundBrush: Brush? = Brush.linearGradient(
        colors = listOf(
            Color(0xFF5FEAEA),
            Color(0xFF2A9D9D),
            Color(0xFF187878)
        ),
        start = Offset(0f, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY)
    ),
    disabledBrush: Brush? = Brush.linearGradient(
        colors = listOf(
            Color(0xFFC0C0C0),
            Color(0xFF9E9E9E)
        )
    ),
    textColor: Color = Color.White
) {
    val borderColor =
        if (!enabled) Color(0x33000000)
        else if (selected) Color(0xFF0FB2B2)   // thicker teal border when selected
        else Color(0x33000000)

    val borderWidth = if (selected) 2.dp else 1.dp
    val displayText = if (selected) "$text ✓" else text

    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(
                elevation = if (enabled) 0.dp else 2.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                brush = if (enabled) backgroundBrush!! else disabledBrush!!,
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
            .clickable(
                enabled = enabled,
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        // Shine overlay + content
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContent()
                    if (enabled) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            ),
                            cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx()),
                            blendMode = BlendMode.Lighten
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else textColor,   // white text when selected
                    shadow = if (enabled) Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(1f, 2f),
                        blurRadius = 4f
                    ) else null
                )
            )
        }
    }
}










