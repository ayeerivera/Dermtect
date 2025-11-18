package com.example.dermtect.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dermtect.model.NewsItem
import androidx.compose.ui.text.SpanStyle
import kotlinx.coroutines.launch

@Composable
fun ArticleTemplate(
    newsItem: NewsItem,
    onBackClick: () -> Unit
) {
    BubblesBackground {
        Column(modifier = Modifier.fillMaxSize()) {

            // 🔹 Header (unchanged)
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(top = 50.dp, start = 20.dp, end = 20.dp)
            ) {
                BackButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Text(
                    text = newsItem.title,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                if (!newsItem.imageUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = newsItem.imageUrl,
                        contentDescription = "Article Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (newsItem.imageResId != null) {
                    Image(
                        painter = painterResource(id = newsItem.imageResId),
                        contentDescription = "Article Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFECEFF1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No image", color = Color.Gray)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "✎ ${newsItem.source}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    )
                    Text(
                        text = newsItem.date,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFA726)
                        )
                    )
                }

                Divider(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // --- SCROLLABLE BODY + FLOATING ARROW ---
            val scrollState = rememberScrollState()
            val scope = rememberCoroutineScope()

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Scrollable text
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    val body = newsItem.body

                    Text(
                        buildAnnotatedString {
                            val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
                            var lastIndex = 0

                            for (match in boldRegex.findAll(body)) {
                                append(body.substring(lastIndex, match.range.first))
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append(match.groupValues[1])
                                }
                                lastIndex = match.range.last + 1
                            }
                            append(body.substring(lastIndex))
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 27.sp,
                            color = Color(0xFF1D1D1D)
                        ),
                        textAlign = TextAlign.Justify
                    )
                }

                // These must be AFTER the Column so maxValue is known after layout
                val canScroll = scrollState.maxValue > 0
                val atBottom = scrollState.value >= scrollState.maxValue

                // ✅ Use fully-qualified AnimatedVisibility to avoid the ColumnScope extension
                androidx.compose.animation.AnimatedVisibility(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    visible = canScroll && !atBottom,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f))
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
                            .clickable {
                                scope.launch {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDownward,
                            contentDescription = "Scroll down",
                            tint = Color(0xFF0FB2B2),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}