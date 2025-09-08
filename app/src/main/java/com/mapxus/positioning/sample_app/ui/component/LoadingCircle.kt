package com.mapxus.positioning.sample_app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LoadingCircle(isShow: Boolean = true, onDismissRequest: () -> Unit = {}) {
    if (isShow) {
        Dialog(
            properties = DialogProperties(),
            onDismissRequest = onDismissRequest
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.requiredHeight(10.dp))
                Text(text = "Processing")
            }

        }
    }
}