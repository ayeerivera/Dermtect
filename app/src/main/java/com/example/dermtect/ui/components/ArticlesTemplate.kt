package com.example.dermtect.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dermtect.model.NewsItem
import androidx.compose.ui.text.SpanStyle

@Composable
fun ArticleTemplate(
    newsItem: NewsItem,
    onBackClick: () -> Unit
) {
    BubblesBackground {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- FIXED HEADER SECTION ---
            Column(
                modifier = Modifier
                    .wrapContentHeight()
                    .padding(top = 50.dp, start = 20.dp, end = 20.dp)
            ) {
                // Back Button
                BackButton(
                    onClick = onBackClick,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // Title
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

                // Image
                newsItem.imageResId?.let { resId ->
                    Image(
                        painter = painterResource(id = resId),
                        contentDescription = "Article Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                // Source and Date
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

            // --- SCROLLABLE BODY ONLY ---
            Column(
                modifier = Modifier
                    .weight(1f) // takes remaining space
                    .verticalScroll(rememberScrollState())
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
        }
    }
}






