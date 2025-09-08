package com.mapxus.positioning.sample_app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Preview(showBackground = true)
@Composable
fun CurrentStepLength(stepLength: String = "0.99") {
    Text(
        text = "Current Step Length $stepLength m",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .padding(10.dp)
    )
}

@Preview
@Composable
fun InputStepLength(
    stepLengthTextFieldValue: String = "0.99",
    setStepLengthResult: Boolean? = null,
    onValueChange: (String) -> Unit = {},
    onDone: () -> Unit = {}
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(percent = 40),
        border = BorderStroke(1.dp, Color(0xff90A4AE)),
    ) {
        TextField(
            shape = RoundedCornerShape(percent = 40),
            value = stepLengthTextFieldValue,
            textStyle = TextStyle(fontSize = 20.sp),
            label = { Text("Please Input Step Length between 0.3m~1.2m") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                setStepLengthResult?.let {
                    if (setStepLengthResult) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            tint = Color.Green,
                            contentDescription = null
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Close,
                            tint = Color.Red,
                            contentDescription = null
                        )
                    }
                }
            },
            onValueChange = { value ->
                onValueChange(value)
            },
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    onDone()
                }),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            singleLine = true,
        )
    }
}