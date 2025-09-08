package com.mapxus.positioning.sample_app.page.positioning.ui

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Created by Edison on 2024/2/21.
 * Describe:
 */
@Preview
@Composable
fun TopContainer(
    modifier: Modifier = Modifier,
    data: List<String> = listOf("test", "test", "test")
) {
    LazyColumn(modifier = modifier) {
        items(data) {
            ListItem(
                modifier = Modifier
                    .padding(5.dp)
                    .padding(horizontal = 20.dp)
                    .height(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colors.surface.copy(alpha = 0.5f),
                ),
                headlineContent = {
                    Text(text = it, fontSize = 10.sp)
                },
            )
        }
    }
}