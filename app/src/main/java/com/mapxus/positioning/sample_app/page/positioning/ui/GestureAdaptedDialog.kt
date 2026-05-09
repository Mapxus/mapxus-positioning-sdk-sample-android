package com.mapxus.positioning.sample_app.page.positioning.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.request.ImageRequest
import com.mapxus.positioning.api.positioning.DirectionAccuracy
import com.mapxus.positioning.sample_app.ui.component.TextButton
import com.mapxus.positioning.sample_app.ui.theme.MainGreen

/**
 * Created by Edison on 2024/2/21.
 * Describe:
 */
@Preview
@Composable
fun PoorAccuracyNeedCalibratingDialog(
    compassAccuracy: DirectionAccuracy = DirectionAccuracy.LOW,
    onDismiss: () -> Unit = {},
) {
    DialogCard(onDismiss = onDismiss) {
        GestureAdaptedContent()
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Please stand still",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Tilt and move your device",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(buildAnnotatedString {
                append("Compass accuracy: ")
                withStyle(
                    SpanStyle(
                        color = when (compassAccuracy) {
                            DirectionAccuracy.HIGH -> MainGreen
                            DirectionAccuracy.MEDIUM -> Color.Yellow
                            else -> Color.Red
                        }
                    )
                ) {
                    append(compassAccuracy.name)
                }
            })
            if (compassAccuracy == DirectionAccuracy.HIGH) {
                TextButton(
                    modifier = Modifier
                        .padding(horizontal = 30.dp)
                        .fillMaxWidth(),
                    text = "Done",
                ) {
                    onDismiss()
                }
            }
        }
    }
}

@Composable
private fun DialogCard(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        onDismissRequest = onDismiss
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun GestureAdaptedContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                model =
                    ImageRequest.Builder(LocalContext.current)
                        .data("file:///android_asset/gestureAdapted.gif")
                        .decoderFactory(GifDecoder.Factory())
                        .build()
            ),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
            contentDescription = null,
        )
    }
}