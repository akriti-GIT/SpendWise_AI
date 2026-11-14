package com.example.spendwise.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spendwise.ui.theme.*

@Composable
fun SpendWiseLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Outer Glow / Shield shape
        val corner = w * 0.28f
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(DarkPine, Color(0xFF162E20))
            ),
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner)
        )

        // Shield subtle inner border
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFF4CE86).copy(alpha = 0.6f), Color(0xFFC1866B).copy(alpha = 0.3f))
            ),
            size = Size(w, h),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = w * 0.03f)
        )

        // "S" Brand Ribbon Path
        val cx = w * 0.5f
        val cy = h * 0.5f
        val path = Path().apply {
            moveTo(cx + w * 0.16f, cy - h * 0.16f)
            cubicTo(
                cx, cy - h * 0.24f,
                cx - w * 0.18f, cy - h * 0.10f,
                cx, cy
            )
            cubicTo(
                cx + w * 0.18f, cy + h * 0.10f,
                cx, cy + h * 0.24f,
                cx - w * 0.16f, cy + h * 0.16f
            )
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFE8A3), Color(0xFFF4BA5E), Terracotta)
            ),
            style = Stroke(
                width = w * 0.08f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Sparkle / Star at top-right
        val starCx = cx + w * 0.18f
        val starCy = cy - h * 0.22f
        val starR = w * 0.06f

        val starPath = Path().apply {
            moveTo(starCx, starCy - starR)
            lineTo(starCx + starR * 0.35f, starCy - starR * 0.35f)
            lineTo(starCx + starR, starCy)
            lineTo(starCx + starR * 0.35f, starCy + starR * 0.35f)
            lineTo(starCx, starCy + starR)
            lineTo(starCx - starR * 0.35f, starCy + starR * 0.35f)
            lineTo(starCx - starR, starCy)
            lineTo(starCx - starR * 0.35f, starCy - starR * 0.35f)
            close()
        }

        drawPath(
            path = starPath,
            color = Color(0xFFFFE8A3)
        )
    }
}

@Composable
fun SpendWiseBrandHeader(
    modifier: Modifier = Modifier,
    logoSize: Dp = 68.dp,
    showTagline: Boolean = true
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SpendWiseLogoBadge(size = logoSize)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "SpendWise",
            fontFamily = SerifFamily,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (showTagline) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Smart Student & Personal Expense Tracker",
                fontFamily = SansFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                letterSpacing = 0.4.sp
            )
        }
    }
}
