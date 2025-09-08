package com.mapxus.positioning.sample_app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapxus.positioning.sample_app.ui.theme.MainGreen

/**
 * Created by Edison on 2024/7/2.
 * Describe:
 */

@Preview
@Composable
fun TitleText(modifier: Modifier = Modifier, text: String = "Logs not found") {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        color = Color(0x8a000000),
        style = MaterialTheme.typography.headlineLarge,
        modifier = modifier.fillMaxWidth()
    )
}

@Preview
@Composable
fun DebugTextRow(
    textTitle: String = "title",
    textValue: String = "value",
    textValueFontColor: Color = MainGreen
) {
    Row(
        modifier = Modifier
            .background(color = Color(0xFFADD8E6))
            .sizeIn(maxWidth = 200.dp)
            .padding(5.dp)
    ) {
        Text(
            text = textTitle,
            fontSize = 6.sp,
            lineHeight = 1.sp,
            softWrap = false,
            overflow = Ellipsis
        )
        Text(
            text = textValue, fontSize = 6.sp,
            color = textValueFontColor,
            lineHeight = 1.sp, softWrap = false, overflow = Ellipsis
        )
    }
    Spacer(modifier = Modifier.height(3.dp))
}

