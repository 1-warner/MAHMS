package com.doodoobops.mahms.ui.patient

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.doodoobops.mahms.ui.theme.CareTeal400
import com.doodoobops.mahms.ui.theme.DataTextStyle
import com.doodoobops.mahms.ui.theme.Neutral100
import com.doodoobops.mahms.ui.theme.Neutral900

/** Circular medication-adherence gauge (design-kit "adherence ring"). */
@Composable
fun AdherenceRing(percent: Int, modifier: Modifier = Modifier, size: Int = 84) {
    Box(modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = Neutral100,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke),
            )
            drawArc(
                color = CareTeal400,
                startAngle = -90f,
                sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text("$percent%", style = DataTextStyle.copy(color = Neutral900))
    }
}
