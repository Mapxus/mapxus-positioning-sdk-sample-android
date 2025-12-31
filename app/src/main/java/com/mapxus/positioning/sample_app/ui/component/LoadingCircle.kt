package com.mapxus.positioning.sample_app.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mapxus.positioning.sample_app.utils.bytesToMb
import com.mapxus.positioning.sample_app.utils.keepTwo

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

@Composable
fun LoadingCircleWithProgressBar(
    isShow: Boolean = true,
    progressAnimated: Float,
    writtenSize: Long,
    totalSize: Long,
    onDismissRequest: () -> Unit = {}
) {
    if (isShow) {
        val animatedProgress by animateFloatAsState(
            targetValue = progressAnimated,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )
        Dialog(
            properties = DialogProperties(),
            onDismissRequest = onDismissRequest
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = animatedProgress,
                    strokeWidth = 15.dp,
                    modifier = Modifier.size(180.dp)
                )
                Text(
                    text = "${writtenSize.bytesToMb().keepTwo()}MB/${
                        totalSize.bytesToMb().keepTwo()
                    }MB",
                    fontSize = 15.sp
                )
            }
        }
    }
}