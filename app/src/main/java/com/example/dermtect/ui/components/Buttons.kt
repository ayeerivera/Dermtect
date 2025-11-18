package com.example.dermtect.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Dialog-style gradient brushes (same as your modal buttons)
private val PrimaryBrushEnabled = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF5FEAEA), // top lighter teal
        Color(0xFF2A9D9D), // middle teal
        Color(0xFF187878)  // bottom darker teal
    )
)

private val PrimaryBrushDisabled = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFBDBDBD),
        Color(0xFF9E9E9E),
        Color(0xFF757575)
    )
)

@Composable
fun BackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(50.dp)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .background(Color.White, shape = CircleShape)
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
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.back),
            contentDescription = "Back",
            tint = Color.Black,
            modifier = Modifier.size(28.dp)
        )
    }
}


@Composable
fun BackButtonWithUnsavedCheck(
    navController: NavController,
    hasUnsavedChanges: () -> Boolean,
    onShowConfirmDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackButton(
        onClick = {
            if (hasUnsavedChanges()) {
                onShowConfirmDialog()
            } else {
                navController.popBackStack()
            }
        },
        modifier = modifier
    )
}

@Composable
fun HandleBackNavigation(
    navController: NavController,
    hasUnsavedChanges: () -> Boolean,
    onShowConfirmDialog: () -> Unit
) {
    BackHandler {
        if (hasUnsavedChanges()) {
            onShowConfirmDialog()
        } else {
            navController.popBackStack()
        }
    }
}


@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false
            )
            .background(
                brush = if (enabled) PrimaryBrushEnabled else PrimaryBrushDisabled,
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable(enabled = enabled) { onClick() }
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}


@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    cornerRadius: Dp = 12.dp
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .shadow(
                elevation = 6.dp, // soft embossed feel
                shape = shape,
                clip = false
            )
            .background(color = Color.White, shape = shape)
            .border(
                width = 1.dp,
                color = Color(0xFFDDDDDD), // light gray accent border
                shape = shape
            )
            .clickable(enabled = enabled) { onClick() }
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) Color(0xFF4F4F4F) else Color(0xFFBDBDBD),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}











