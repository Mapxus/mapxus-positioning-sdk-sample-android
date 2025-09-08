package com.mapxus.positioning.sample_app.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Edison on 2024/7/2.
 * Describe:
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun OutlinedTextFieldCommon(
    value: String,
    labelText: String,
    keyboardType: KeyboardType = KeyboardType.Number,
    fillMaxWidth: Float = 0.5f,
    keyboardActionsOnDone: () -> Unit = {},
    onTextChange: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth(fillMaxWidth)
            .padding(8.dp),
        value = value,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType, imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
                keyboardActionsOnDone()
            }),
        label = {
            Text(text = labelText, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
        },
        onValueChange = { newValue ->
            onTextChange(newValue)
        })
}