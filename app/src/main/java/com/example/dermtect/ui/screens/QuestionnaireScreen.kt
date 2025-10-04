package com.example.dermtect.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.dermtect.ui.components.BackButton
import com.example.dermtect.ui.components.BubblesBackground
import com.example.dermtect.ui.components.DialogTemplate
import com.example.dermtect.ui.viewmodel.QuestionnaireViewModel
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawWithContent
import com.example.dermtect.ui.components.EmbossedButton

@Composable
fun QuestionnaireScreen(navController: NavController) {
    val questionnaireViewModel = remember { QuestionnaireViewModel() }

    val loading by questionnaireViewModel.loading.collectAsState()
    val existingAnswers by questionnaireViewModel.existingAnswers.collectAsState()
    val context = LocalContext.current

    // 8 questions to match Firestore structure
    val questions = remember {
        listOf(
            "Have you noticed this skin spot recently appearing or changing in size?",
            "Does the lesion have uneven or irregular borders?",
            "Is the color of the spot unusual (black, blue, red, or a mix of colors)?",
            "Has the lesion been bleeding, itching, or scabbing recently?",
            "Is there a family history of skin cancer or melanoma?",
            "Has the lesion changed in color or texture over the last 3 months?",
            "Is the lesion asymmetrical (one half unlike the other)?",
            "Is the diameter larger than 6mm (about the size of a pencil eraser)?"
        )
    }
    // Modes & navigation
    var isEditMode by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showWarning by remember { mutableStateOf(false) }

    var step by rememberSaveable { mutableStateOf(0) }          // 0..7
    var inReview by rememberSaveable { mutableStateOf(false) }  // review screen
    var showIntro by rememberSaveable { mutableStateOf(false) } // first-time intro screen

// Show progress only while actively answering questions
    val isReadSummary = existingAnswers != null && !isEditMode
    val showEditingProgress = isEditMode && !inReview && !showIntro

    // Editable buffer
    val answers =
        remember { mutableStateListOf<Boolean?>().apply { repeat(questions.size) { add(null) } } }


    // Initial load
    LaunchedEffect(Unit) {
        questionnaireViewModel.loadQuestionnaireAnswers()
        isEditMode = false
        step = 0
        inReview = false
        showIntro = false
    }

    // React to loaded answers
    LaunchedEffect(existingAnswers) {
        when {
            // No doc yet: only go to intro ONCE (don’t keep forcing edit)
            existingAnswers == null && answers.all { it == null } -> {
                isEditMode = true
                inReview = false
                step = 0
                showIntro = true
                answers.clear()
                answers.addAll(List(questions.size) { null })
            }

            // Doc arrived: copy answers and show summary by default
            existingAnswers != null -> {
                answers.clear()
                answers.addAll(existingAnswers!!)
                step = 0
                inReview = false

                // If we were showing the intro due to late data, exit edit mode.
                if (showIntro) {
                    isEditMode = false
                    showIntro = false
                }
            }
        }
    }

    fun goPrev() {
        if (inReview) {
            inReview = false
            step = questions.lastIndex
        } else if (step > 0) {
            step -= 1
        }
    }

    fun goNextOrReview() {
        val currentAnswered = answers[step] != null
        if (!currentAnswered) {
            showWarning = true
            Toast.makeText(context, "Please answer question ${step + 1}.", Toast.LENGTH_SHORT)
                .show()
            return
        }
        showWarning = false
        if (step < questions.lastIndex) {
            step += 1
        } else {
            inReview = true
        }
    }

    fun saveAnswers() {
        val allAnswered = answers.all { it != null }
        showWarning = !allAnswered
        if (!allAnswered) {
            Toast.makeText(context, "Please answer all questions.", Toast.LENGTH_SHORT).show()
            return
        }
        questionnaireViewModel.saveQuestionnaireAnswers(
            answers = answers,
            onSuccess = {
                Toast.makeText(context, "Answers saved successfully!", Toast.LENGTH_SHORT).show()
                isEditMode = false
                inReview = false
                step = 0
                showIntro = false
                questionnaireViewModel.loadQuestionnaireAnswers()
            },
            onError = { msg ->
                Toast.makeText(
                    context,
                    "Failed to save. ${msg ?: "Please check rules & logs."}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    BubblesBackground {
        Scaffold(containerColor = Color.Transparent) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                BackButton(
                    onClick = {
                        if (isEditMode || existingAnswers == null) showBackDialog = true
                        else navController.popBackStack()
                    },
                    modifier = Modifier.offset(x = 25.dp, y = 50.dp)
                )

                BackHandler {
                    if (isEditMode || existingAnswers == null) showBackDialog = true
                    else navController.popBackStack()
                }

                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .padding(15.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(0.85f)
                            .padding(top = 25.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title
                        Text(
                            text = "Skin Check Questionnaire",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(15.dp))

                        if (showEditingProgress) {
                            // Progress bar
                            LinearProgressIndicator(
                                progress = (step + 1).toFloat() / questions.size.toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .height(8.dp),
                                trackColor = Color(0xFFE6F4F4),
                                color = Color(0xFF0FB2B2)
                            )
                            Spacer(Modifier.height(8.dp))

                            // "Question X of Y" | "Skip"
                            Row(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Question ${step + 1} of ${questions.size}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                val rightActionLabel = when {
                                    existingAnswers == null -> "Skip"
                                    isEditMode && !inReview -> "Skip"
                                    else -> null
                                }

                                if (rightActionLabel != null) {
                                    TextButton(
                                        onClick = { showCancelDialog = true },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = rightActionLabel,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF0FB2B2)
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.width(1.dp))
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                        } else {
                            // keep vertical rhythm when the header is hidden
                            Spacer(Modifier.height(16.dp))
                        }


                        Spacer(Modifier.height(12.dp))

                        when {
                            // ============== FIRST-TIME INTRO SCREEN ==============
                            (existingAnswers == null && isEditMode && showIntro) -> {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Before we scan your skin, please answer a few short questions for additional context.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                )
                                Spacer(Modifier.height(24.dp))
                                Button(
                                    onClick = { showIntro = false }, // proceed to step-by-step
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0FB2B2),
                                        contentColor = Color.White
                                    )
                                ) { Text("Next") }
                            }

                            // ============== EDIT / FIRST-TIME: STEP-BY-STEP ==============
                            existingAnswers == null || isEditMode -> {
                                if (!inReview) {
                                    // -------- Single-question centered view --------
                                    // inside: existingAnswers == null || isEditMode -> if (!inReview) { ... }

                                    val arrowHit = 64.dp
                                    val arrowIcon = 36.dp
                                    val sideGutter = arrowHit

// ⬇️ This Box centers the whole question container vertically
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f, fill = true),          // <-- makes it take the available height
                                        contentAlignment = Alignment.Center     // <-- centers inside that space
                                    ) {
                                        // Arrows overlaid on a full-width container, container inset by sideGutter
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.Center)
                                                    .fillMaxWidth()
                                                    .padding(start = sideGutter, end = sideGutter)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .border(1.dp, Color(0x22000000), RoundedCornerShape(16.dp))
                                                    .background(Color(0xFFF7FBFB))
                                                    .padding(horizontal = 16.dp, vertical = 20.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = questions[step],
                                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }

                                            IconButton(
                                                onClick = { if (step > 0) goPrev() },
                                                enabled = step > 0,
                                                modifier = Modifier.align(Alignment.CenterStart).size(arrowHit)
                                            ) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", modifier = Modifier.size(arrowIcon)) }

                                            IconButton(
                                                onClick = { goNextOrReview() },
                                                modifier = Modifier.align(Alignment.CenterEnd).size(arrowHit)
                                            ) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next", modifier = Modifier.size(arrowIcon)) }
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

// Same brushes as the tutorial screens
                                    val nextBrush = Brush.linearGradient(
                                        colors = listOf(Color(0xFF5FEAEA), Color(0xFF2A9D9D), Color(0xFF187878))
                                    )
                                    val skipBrush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFF0F0F0), Color(0xFFF0F0F0))
                                    )
                                    val btnHeight = 56.dp

// selection flags
                                    val yesSelected = answers[step] == true
                                    val noSelected  = answers[step] == false

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = sideGutter, end = sideGutter),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // YES
                                        EmbossedButton(
                                            text = "YES",
                                            onClick = { answers[step] = true },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(btnHeight),
                                            cornerRadius = 15.dp,
                                            backgroundBrush = if (yesSelected)
                                                nextBrush
                                            else
                                                Brush.linearGradient(listOf(Color(0xFFE6F4F4), Color(0xFFD3EBEB))),
                                            textColor = Color(0xFF0FB2B2),
                                            selected = yesSelected // <-- requires updated EmbossedButton
                                        )

                                        Spacer(Modifier.width(10.dp))

                                        // NO
                                        EmbossedButton(
                                            text = "NO",
                                            onClick = { answers[step] = false },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(btnHeight),
                                            cornerRadius = 15.dp,
                                            backgroundBrush = if (noSelected)
                                                Brush.linearGradient(listOf(Color(0xFFCCCCCC), Color(0xFFB5B5B5)))
                                            else
                                                skipBrush,
                                            textColor = Color.Black,
                                            selected = noSelected
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    answers[step]?.let { picked ->
                                        Text(
                                            text = "Selected: " + if (picked) "Yes" else "No",
                                            color = Color(0xFF0FB2B2),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = sideGutter, end = sideGutter)
                                        )
                                    }
                                    // Subtle Cancel/Skip under the answers
                                    Spacer(Modifier.height(8.dp))
                                    // Show warning first (if unanswered)
                                    if (showWarning && answers[step] == null) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = "⚠ Please answer this question to continue.",
                                            color = Color(0xFFa90505),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                    }


                                } else {
                                    // -------- Review screen (scrollable) --------
                                    Text(
                                        text = "Review your answers",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(0.9f),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = PaddingValues(bottom = 10.dp)
                                    ) {
                                        itemsIndexed(questions) { index, q ->
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        inReview = false
                                                        step = index
                                                    }
                                            ) {
                                                Text(
                                                    text = "${index + 1}. $q",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Spacer(Modifier.height(6.dp))
                                                val label = when (answers[index]) {
                                                    true -> "Yes"
                                                    false -> "No"
                                                    else -> "Unanswered"
                                                }
                                                Text(
                                                    text = "Answer: $label",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                )
                                                if (index != questions.lastIndex) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.padding(
                                                            top = 10.dp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    if (showWarning && answers.any { it == null }) {
                                        Text(
                                            text = "⚠ Please complete all questions before saving.",
                                            color = Color(0xFFa90505),
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(0.9f),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                inReview = false; step = questions.lastIndex
                                            },
                                            border = BorderStroke(1.dp, Color(0xFF0FB2B2)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF0FB2B2)
                                            )
                                        ) { Text("Previous") }

                                        Button(
                                            onClick = { saveAnswers() },
                                            enabled = !loading,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF0FB2B2),
                                                contentColor = Color.White
                                            )
                                        ) { Text(if (loading) "Saving..." else "Save") }
                                    }

                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { showCancelDialog = true },
                                        modifier = Modifier.fillMaxWidth(0.9f),
                                        border = BorderStroke(1.dp, Color(0xFF0FB2B2)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color(
                                                0xFF0FB2B2
                                            )
                                        )
                                    ) { Text("Cancel") }
                                }
                            }

                            // ============== READ-ONLY (has existing answers) ==============
                            else -> {
                                Text(
                                    text = "Your previous answers",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(0.9f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(bottom = 10.dp)
                                ) {
                                    itemsIndexed(questions) { index, q ->
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "${index + 1}. $q",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            val ans = answers.getOrNull(index)
                                            Text(
                                                text = "Answer: " + when (ans) {
                                                    true -> "Yes"
                                                    false -> "No"
                                                    else -> "—"
                                                },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                            if (index != questions.lastIndex) {
                                                HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        isEditMode = true
                                        inReview = false
                                        step = 0
                                        showIntro = false
                                        answers.clear()
                                        answers.addAll(existingAnswers ?: List(questions.size) { null })
                                    },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) { Text("Edit") }


                            }
                        }

                        // Back confirm dialog
                        if (showBackDialog) {
                            DialogTemplate(
                                show = showBackDialog,
                                title = "Exit?",
                                description = "Your answers won’t be saved.",
                                primaryText = "Yes, exit",
                                onPrimary = {
                                    showBackDialog = false
                                    navController.popBackStack()
                                },
                                secondaryText = "Cancel",
                                onSecondary = { showBackDialog = false },
                                onDismiss = { showBackDialog = false }
                            )
                        }

                        // Enhanced Cancel dialog (Skip vs Discard)
                        if (showCancelDialog) {
                            val cancelTitle =
                                if (existingAnswers == null) "Skip questionnaire?" else "Discard changes?"
                            val cancelDesc = if (existingAnswers == null)
                                "You can skip for now and answer these later."
                            else
                                "Your answers will revert to your previous submission."
                            val cancelPrimary =
                                if (existingAnswers == null) "Yes, skip" else "Yes, discard"

                            DialogTemplate(
                                show = true,
                                title = cancelTitle,
                                description = cancelDesc,
                                primaryText = cancelPrimary,
                                onPrimary = {
                                    if (existingAnswers == null) {
                                        // First-time: treat cancel as skip and leave the flow
                                        showCancelDialog = false
                                        navController.popBackStack() // or navigate to your home route
                                    } else {
                                        // Edit mode: revert and exit edit mode
                                        isEditMode = false
                                        inReview = false
                                        step = 0
                                        answers.clear()
                                        answers.addAll(
                                            existingAnswers ?: List(questions.size) { null })
                                        showCancelDialog = false
                                    }
                                },
                                secondaryText = "No, keep editing",
                                onSecondary = { showCancelDialog = false },
                                onDismiss = { showCancelDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
