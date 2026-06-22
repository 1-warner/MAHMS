package com.doodoobops.mahms.ui.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.doodoobops.mahms.ui.theme.CareTeal400
import com.doodoobops.mahms.ui.theme.MpiloBlue500
import com.doodoobops.mahms.ui.theme.Neutral500

/** Minimal bar chart drawn on a Canvas. */
@Composable
fun BarChart(values: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            if (values.isEmpty()) return@Canvas
            val gap = 10.dp.toPx()
            val barWidth = (size.width - gap * (values.size - 1)) / values.size
            values.forEachIndexed { i, v ->
                val barHeight = (v / max) * (size.height - 8.dp.toPx())
                val x = i * (barWidth + gap)
                drawRoundRect(
                    color = MpiloBlue500,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                )
            }
        }
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEach { l ->
                Text(l, style = MaterialTheme.typography.bodySmall, color = Neutral500, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

/** Minimal line chart drawn on a Canvas. */
@Composable
fun LineChart(values: List<Float>, modifier: Modifier = Modifier) {
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val min = values.minOrNull() ?: 0f
    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        val range = (max - min).coerceAtLeast(1f)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - min) / range) * (size.height - 8.dp.toPx())
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = CareTeal400, style = Stroke(width = 4.dp.toPx()))
    }
}
