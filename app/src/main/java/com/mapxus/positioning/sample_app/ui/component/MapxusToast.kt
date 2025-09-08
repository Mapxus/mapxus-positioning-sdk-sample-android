package com.mapxus.positioning.sample_app.ui.component

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapxus.positioning.sample_app.ui.icons.Error

val InfoBackground = Color(0xff0340DA)
val WarningBackground = Color(0xffFBC02D)
val ErrorBackground = Color(0xffD50000)
val SuccessBackground = Color(0xff17B978)

data class MapxusToastData(
    val title: String,
    val message: String,
    val color: Color,
    val iconRes: ImageVector
)

val fakeData = listOf(
    MapxusToastData("test", "test", InfoBackground, Icons.Filled.Info),
    MapxusToastData("test", "test", WarningBackground, Icons.Filled.Error),
    MapxusToastData("test", "test", ErrorBackground, Icons.Filled.Error),
    MapxusToastData("test", "test", SuccessBackground, Icons.Filled.CheckCircle),
)

private const val alpha = 0.8f

@Preview
@Composable
fun MapxusToastContainer(
    modifier: Modifier = Modifier,
    data: List<MapxusToastData> = fakeData
) {
    LazyColumn {
        items(data) {
            MapxusToast(
                data = it,
            )
        }
    }
}

@Preview
@Composable
fun MapxusToast(
    modifier: Modifier = Modifier,
    data: MapxusToastData = MapxusToastData("test", "test", InfoBackground, Icons.Filled.Info),
) {
    Log.i("TAG", "MapxusToastContainer: $data")
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 20.dp),
        shape = RoundedCornerShape(25.dp),
        color = data.color.copy(alpha = alpha),
        contentColor = Color.White,
    ) {
        MapxusToastContent(
            title = data.title,
            message = data.message,
            color = data.color,
            iconRes = data.iconRes
        )
    }
}

@Preview
@Composable
private fun MapxusToastContent(
    title: String = "Test",
    message: String = "Test",
    color: Color = InfoBackground,
    iconRes: ImageVector = Icons.Filled.Info
) {
    Row(modifier = Modifier.padding(all = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(color = Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .size(50.dp),
                tint = color.copy(alpha = alpha),
                imageVector = iconRes,
                contentDescription = null
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}