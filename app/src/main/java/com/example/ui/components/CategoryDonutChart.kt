package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BudgetLimitEntity
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class ChartSlice(
    val category: String,
    val amount: Double,
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float,
    val percentage: Double
)

@Composable
fun CategoryDonutChart(
    categorySpendMap: Map<String, Double>,
    limits: List<BudgetLimitEntity>,
    modifier: Modifier = Modifier
) {
    val totalSpend = categorySpendMap.values.sum()
    var selectedSliceCategory by remember { mutableStateOf<String?>(null) }

    val limitsMap = remember(limits) { limits.associateBy { it.category } }

    val slices = remember(categorySpendMap, limitsMap) {
        if (totalSpend <= 0.0) emptyList()
        else {
            var currentAngle = -90f
            categorySpendMap.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.value }
                .map { (cat, amount) ->
                    val sweep = ((amount / totalSpend) * 360f).toFloat()
                    val colorHex = limitsMap[cat]?.colorHex ?: "#10B981"
                    val parsedColor = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (_: Exception) {
                        Color(0xFF10B981)
                    }

                    val slice = ChartSlice(
                        category = cat,
                        amount = amount,
                        color = parsedColor,
                        startAngle = currentAngle,
                        sweepAngle = sweep,
                        percentage = (amount / totalSpend) * 100.0
                    )
                    currentAngle += sweep
                    slice
                }
        }
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(categorySpendMap) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    if (totalSpend <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No spending recorded for this period yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val selectedSlice = slices.find { it.category == selectedSliceCategory } ?: slices.firstOrNull()

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .pointerInput(slices) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)
                            val outerRadius = size.width / 2f
                            val innerRadius = outerRadius - 38.dp.toPx()

                            if (dist in innerRadius..outerRadius) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < -90f) angle += 360f
                                if (angle < 0 && angle >= -90f) {
                                    // standard range [-90, 270)
                                } else if (angle >= -90f && angle <= 180f) {
                                    // fine
                                }

                                val normalizedAngle = if (angle < -90f) angle + 360f else angle
                                val hitSlice = slices.find { slice ->
                                    val end = slice.startAngle + slice.sweepAngle
                                    normalizedAngle >= slice.startAngle && normalizedAngle < end
                                }
                                if (hitSlice != null) {
                                    selectedSliceCategory = hitSlice.category
                                }
                            }
                        }
                    }
            ) {
                val strokeWidthBase = 32.dp.toPx()
                val selectedStrokeWidth = 40.dp.toPx()
                val diameter = size.minDimension - selectedStrokeWidth
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)

                slices.forEach { slice ->
                    val isSelected = slice.category == selectedSlice?.category
                    val strokeW = if (isSelected) selectedStrokeWidth else strokeWidthBase

                    drawArc(
                        color = slice.color.copy(alpha = if (isSelected) 1f else 0.85f),
                        startAngle = slice.startAngle,
                        sweepAngle = slice.sweepAngle * animatedProgress.value,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                }
            }

            // Center metric display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                if (selectedSlice != null) {
                    Text(
                        text = selectedSlice.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ksh %,.0f".format(selectedSlice.amount),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "%.1f%% of total".format(selectedSlice.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = selectedSlice.color,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Category Legend pills (scrollable/wrapped)
        Text(
            text = "Tap any segment to inspect category",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.chunked(2).forEach { rowSlices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSlices.forEach { slice ->
                        val isSelected = slice.category == selectedSlice?.category
                        Surface(
                            onClick = { selectedSliceCategory = slice.category },
                            shape = CircleShape,
                            color = if (isSelected) slice.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(10.dp),
                                    shape = CircleShape,
                                    color = slice.color
                                ) {}
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = slice.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%.0f%%".format(slice.percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = slice.color
                                )
                            }
                        }
                    }
                    if (rowSlices.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
