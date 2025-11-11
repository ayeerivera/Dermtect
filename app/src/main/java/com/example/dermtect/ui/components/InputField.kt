package com.example.dermtect.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import com.example.dermtect.R
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    iconRes: Int? = null,
    iconVector: ImageVector? = null,     // NEW
    isPassword: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    placeholderStyle: TextStyle = MaterialTheme.typography.labelMedium,
    textColor: Color = Color.Black,
    readOnly: Boolean = false,           // NEW
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onClick: (() -> Unit)? = null,
    valueTFV: TextFieldValue? = null,
    onValueChangeTFV: ((TextFieldValue) -> Unit)? = null

) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (valueTFV != null && onValueChangeTFV != null) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            readOnly = readOnly, // ✅ prevents keyboard for birthday
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = if (isPassword && !isPasswordVisible)
                PasswordVisualTransformation() else VisualTransformation.None,
            leadingIcon = {
                when {
                    iconVector != null -> Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                    iconRes != null -> Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                }
            },
            trailingIcon = {
                if (isPassword) {
                    val icon = if (isPasswordVisible) R.drawable.on else R.drawable.off
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            painter = painterResource(id = icon),
                            contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            textStyle = textStyle.copy(fontWeight = FontWeight.Bold),
            placeholder = {
                Text(
                    text = placeholder,
                    style = placeholderStyle.copy(fontWeight = FontWeight.Normal),
                    color = Color.DarkGray,
                    maxLines = 1,
                    softWrap = false
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFFFFF),
                unfocusedContainerColor = Color(0xFFF7F7F7),
                disabledContainerColor = Color(0xFFF0F0F0),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                cursorColor = textColor
            ),
            // NEW ✅ (use the passed-in modifier)
            modifier = modifier
                .fillMaxWidth(0.9f)
                .height(56.dp)
                .let { if (onClick != null) it.clickable { onClick() } else it }

        )} else {
            // Original String overload
            TextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                readOnly = readOnly,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = if (isPassword && !isPasswordVisible)
                    PasswordVisualTransformation() else VisualTransformation.None,
                leadingIcon = {
                    when {
                        iconVector != null -> Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Gray
                        )
                        iconRes != null -> Icon(
                            painter = painterResource(id = iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.Gray
                        )
                    }
                },
                trailingIcon = {
                    if (isPassword) {
                        val icon = if (isPasswordVisible) R.drawable.on else R.drawable.off
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                painter = painterResource(id = icon),
                                contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                textStyle = textStyle.copy(fontWeight = FontWeight.Bold),
                placeholder = {
                    Text(
                        text = placeholder,
                        style = placeholderStyle.copy(fontWeight = FontWeight.Normal),
                        color = Color.DarkGray,
                        maxLines = 1,
                        softWrap = false
                    )
                },
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF7F7F7),
                    unfocusedContainerColor = Color(0xFFF7F7F7),
                    disabledContainerColor = Color(0xFFF0F0F0),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor,
                    cursorColor = textColor
                ),
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(56.dp)
                    .let { if (onClick != null) it.clickable { onClick() } else it }
            )
        }


        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}
