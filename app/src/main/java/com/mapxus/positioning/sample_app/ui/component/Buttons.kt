package com.mapxus.positioning.sample_app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapxus.positioning.sample_app.ui.theme.Purple40

@Preview
@Composable
fun TextButton(
    modifier: Modifier = Modifier,
    text: String = "Test",
    isEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Button(
        modifier = modifier,
        enabled = isEnabled,
        onClick = onClick
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
        )
    }
}

@Preview
@Composable
fun TextButtonSmall(text: String = "test", enabled: Boolean = true, onClick: () -> Unit = {}) {
    Button(
        onClick = onClick,
        enabled = enabled
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
        )
    }
}

@Preview
@Composable
fun TextButtonBig(
    modifier: Modifier = Modifier,
    textSize: TextUnit = 20.sp,
    text: String = "test",
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    onClick: () -> Unit = {},
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        colors = colors
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = textSize,
        )
    }
}

@Preview
@Composable
fun OutlinedButtonCommon(
    modifier: Modifier = Modifier,
    text: String = "Test",
    textColor: Color = Purple40,
    isEnabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun ButtonGrid(
    columns: Int = 2,
    buttonList: List<Pair<String, () -> Unit>> = listOf(
        "test" to {},
        "test" to {},
        "test" to {},
    ),
    enabledHandler: (Int) -> Boolean = { true }
) {
    LazyVerticalGrid(
        verticalArrangement = Arrangement.Center,
        horizontalArrangement = Arrangement.Center,
        columns = GridCells.Fixed(columns),
        userScrollEnabled = false,
        content = {
            itemsIndexed(buttonList) { index, button ->
                OutlinedButtonCommon(
                    modifier = Modifier
                        .size(65.dp)
                        .padding(8.dp),
                    text = button.first,
                    isEnabled = enabledHandler(index),
                    onClick = button.second
                )
            }
        })
}