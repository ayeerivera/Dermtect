package com.example.dermtect.ui.tutorial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.intl.Locale
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.clip
import androidx.compose.animation.animateContentSize

@Composable
fun TutorialOverlay(
    tutorialManager: TutorialManager,
    onFinish: () -> Unit // Called when tutorial is completed or skipped
) {
    val stepIndex = tutorialManager.currentStep
    val steps = tutorialManager.steps

    if (tutorialManager.isFinished()) {
        onFinish()
        return
    }

    // BackHandler for device back button press (REMAINS THE SAME)
    BackHandler(enabled = true) {
        if (stepIndex == 0) {
            onFinish()
        } else {
            tutorialManager.previousStep()
        }
    }

    var boundsForThisStep by remember(stepIndex) { mutableStateOf<Rect?>(null) }
    val currentBounds = tutorialManager.currentTargetBounds

    if (currentBounds != null) {
        boundsForThisStep = currentBounds
    }

    val targetBounds = boundsForThisStep ?: return
    val density = LocalDensity.current

    val cardColor = Color.White
    val cardCornerRadius = 12.dp
    val arrowSize = 12.dp
    val maxCardWidthDp = 300.dp
    val verticalSpacingDp = 16.dp

    val screenHeightThreshold = with(density) { 300.dp.toPx() }
    val isTargetAtTop = targetBounds.top < screenHeightThreshold
    val arrowDirection = if (isTargetAtTop) "up" else "down"

    // ✅ FIX: Declare arrowHorizontalPositionPx as a state variable here
    var arrowHorizontalPositionPx by remember { mutableStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val parentWidth = this.maxWidth
        val parentHeight = this.maxHeight

        // 1. Dim background with transparent hole (REMAINS THE SAME)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = { /* eat taps behind overlay */ }, indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() })
                .graphicsLayer(alpha = 0.99f)
                .drawWithContent {
                    drawRect(color = Color(0x99000000))
                    drawRect(
                        color = Color.Transparent,
                        topLeft = Offset(targetBounds.left, targetBounds.top),
                        size = Size(targetBounds.width, targetBounds.height),
                        blendMode = BlendMode.Clear
                    )
                }
        )

        // Comment Card Positioning Logic (UPDATED SCOPE)
        val step = steps[stepIndex]
        val totalSteps = steps.size
        val currentStepDisplay = stepIndex + 1
        val targetCenterX = targetBounds.left + targetBounds.width / 2

        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .align(if (arrowDirection == "up") Alignment.TopStart else Alignment.BottomStart)
                .offset {
                    val xOffsetApprox = (targetCenterX - (maxCardWidthDp.toPx() / 2)).toInt()

                    val safeXOffset = xOffsetApprox.coerceIn(
                        with(density) { 20.dp.roundToPx() },
                        (parentWidth.toPx() - maxCardWidthDp.toPx() - with(density) { 20.dp.roundToPx() }).toInt()
                    )

                    // ✅ FIX: Update the state variable inside the offset lambda
                    arrowHorizontalPositionPx = (targetCenterX - safeXOffset).toFloat()

                    val yOffsetForUpDirection = targetBounds.bottom.toInt() + verticalSpacingDp.roundToPx()

                    val finalYOffset = if (arrowDirection == "down") {
                        targetBounds.top.toInt() - parentHeight.toPx().toInt() - verticalSpacingDp.roundToPx()
                    } else {
                        yOffsetForUpDirection
                    }

                    IntOffset(safeXOffset, finalYOffset)
                }
                .widthIn(min = 200.dp, max = maxCardWidthDp)
                .wrapContentHeight()
        ) {
            CommentCard(
                title = step.key.replace('_', ' ').capitalize(Locale.current),
                description = step.description,
                stepNumber = currentStepDisplay,
                totalSteps = totalSteps,
                arrowDirection = arrowDirection,
                arrowSize = arrowSize,
                cardCornerRadius = cardCornerRadius,
                cardColor = cardColor,
                onNext = { tutorialManager.nextStep() },
                onBack = { tutorialManager.previousStep() },
                arrowHorizontalPositionPx = arrowHorizontalPositionPx,
                // ✅ RE-INTRODUCE onSkip
                onSkip = {
                    tutorialManager.currentStep = tutorialManager.steps.size
                    tutorialManager.currentTargetBounds = null
                    onFinish()
                }

            )
        }
    }
}

@Composable
private fun CommentCard(
    title: String,
    description: String,
    stepNumber: Int,
    totalSteps: Int,
    arrowDirection: String,
    arrowSize: Dp,
    cardCornerRadius: Dp,
    cardColor: Color,
    onNext: () -> Unit,
    onBack: () -> Unit,
    arrowHorizontalPositionPx: Float,
    // ✅ ADD NEW PARAMETER
    onSkip: () -> Unit
) {
    val density = LocalDensity.current
    val standardPadding = 16.dp

    // Convert Dp to Px that are used in the drawing scope
    val arrowSizePx = with(density) { arrowSize.toPx() }
    val arrowMarginPx = with(density) { 4.dp.toPx() }

    val isFirstStep = stepNumber == 1
    val isLastStep = stepNumber == totalSteps

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(cardCornerRadius))
            .drawWithContent {

                // --- ARROW DRAWING LOGIC (FIXED) ---

                // Define maxArrowX here using size.width, where it's available
                val dynamicMaxArrowX = size.width - arrowSizePx - arrowMarginPx

                // Final calculation for the arrow's center point
                val xCenter = arrowHorizontalPositionPx.coerceIn(
                    arrowSizePx + arrowMarginPx,
                    dynamicMaxArrowX
                )

                // Draw the content (the Column) first
                drawContent()

                // Draw the arrow path on top
                val path = Path().apply {
                    when (arrowDirection) {
                        "up" -> {
                            moveTo(xCenter - arrowSizePx, 0f)
                            lineTo(xCenter + arrowSizePx, 0f)
                            lineTo(xCenter, -arrowSizePx)
                            close()
                        }

                        "down" -> {
                            moveTo(xCenter - arrowSizePx, size.height)
                            lineTo(xCenter + arrowSizePx, size.height)
                            lineTo(xCenter, size.height + arrowSizePx)
                            close()
                        }
                    }
                }
                drawPath(path, color = cardColor)
            }
            .clip(RoundedCornerShape(cardCornerRadius))
    ) {

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // ✅ Apply animateContentSize here for smooth resizing
                .animateContentSize()
                .padding(standardPadding)
        ) {

            // CONCISE HEADER: Title and Minimized Step Counter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                // Minimized Step Counter
                Text(
                    text = "$stepNumber/$totalSteps",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = description,
                fontSize = 14.sp,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(16.dp))

            // NAVIGATION ROW: Back and Next/Finish Buttons
            // ✅ NAVIGATION ROW: Skip, Back and Next/Finish Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                // Change arrangement to SpaceBetween to push Skip/Back to left/center and Next to the right
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✅ 1. SKIP BUTTON (Left side)
                Button(
                    onClick = onSkip,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        // Use a light, secondary color for Skip
                        containerColor = Color.LightGray.copy(alpha = 0.5f),
                        contentColor = Color.Black
                    )
                ) {
                    Text(text = "Skip")
                }

                // Group Back and Next buttons to the right
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 2. Back Button
                    if (!isFirstStep) {
                        Button(
                            onClick = onBack,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color.LightGray.copy(alpha = 0.5f),
                                contentColor = Color.Black
                            )
                        ) {
                            Text(text = "Back")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = onNext,
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(text = if (isLastStep) "Finish" else "Next")
                    }
                }
            }
        }
    }
}