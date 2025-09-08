package com.mapxus.positioning.sample_app.page.readiness

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapxus.positioning.sample_app.ui.component.TextButton

/**
 * Created by Edison on 2024/9/26.
 * Describe:
 */
@Composable
fun CheckReadinessScreen(
    modifier: Modifier = Modifier,
    onCheckPositioningReadinessClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {

        TextButton(text = "Check Positioning Readiness", onClick = {
            onCheckPositioningReadinessClick()
        })
    }
}