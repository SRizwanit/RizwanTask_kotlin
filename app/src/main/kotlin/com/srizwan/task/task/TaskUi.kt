package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors

/** React-এর `.card`: সারফেস রং + হালকা বর্ডার + গোল কোণা */
@Composable
fun TaskCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    container: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container ?: MaterialTheme.colorScheme.surface)
            .border(1.dp, borderColor ?: AppColors.extra.line, shape)
            .padding(14.dp),
        content = content
    )
}

/** React-এর `.badge` (ok / warn / bad / gold) */
@Composable
fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(99.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.55f), shape)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
