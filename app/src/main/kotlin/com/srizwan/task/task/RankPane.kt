package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors

/**
 * র‍্যাংক ট্যাব (RankPane.jsx): লিডারবোর্ড (আজ / এই সপ্তাহ / সব সময়), নিজের অবস্থান।
 * নতুন অনুরোধ ব্যর্থ হলে তালিকার জায়গায় ত্রুটি দেখায়; "আপনার অবস্থান" আগের সফল ফলাফলেরই থাকে।
 */
@Composable
fun RankPane(
    period: RankPeriod,
    onPeriod: (RankPeriod) -> Unit,
    loading: Boolean,
    data: RankData?,
    error: String,
    meId: String
) {
    // নিজের অবস্থানের লেখা
    var myRank = ""
    var idx = -1
    if (data != null) {
        idx = data.rows.indexOfFirst { it.id == meId }
        val lab = data.period.label
        myRank = if (idx < 0) {
            "$lab আপনার কোনো পেইজ নেই — একটি পেইজ প্রুফ করলেই তালিকায় আসবেন!"
        } else if (idx == 0) {
            "$lab আপনি ১ নম্বরে! মাশাআল্লাহ, এগিয়ে থাকুন।"
        } else {
            "$lab আপনার অবস্থান " + bn(idx + 1) + " নম্বর • পরের জনের থেকে " +
                bn(data.rows[idx - 1].n - data.rows[idx].n) + " পেইজ পিছনে"
        }
    }

    val muted = AppColors.extra.muted

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // হেডার কার্ড
            item {
                TaskCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(Color(0xFFA8761F), Color(0xFFF2D27A)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🏆", fontSize = 28.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "লিডারবোর্ড",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (myRank.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = myRank,
                                style = MaterialTheme.typography.bodySmall,
                                color = muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // সময়কাল বাছাই
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    RankPeriod.values().forEach { p ->
                        PeriodChip(text = p.chip, on = period == p, onClick = { onPeriod(p) })
                    }
                }
            }

            if (loading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            if (error.isNotEmpty()) {
                item { EmptyNote2("লিডারবোর্ড আনা যায়নি: $error") }
            } else if (data != null) {
                if (data.rows.isEmpty()) {
                    item { EmptyNote2("এই সময়ে এখনো কেউ কোনো পেইজ প্রুফড করেননি — প্রথম হওয়ার সুযোগ আপনার!") }
                } else {
                    itemsIndexed(data.rows.take(20), key = { _, r -> r.id }) { i, r ->
                        RankRowItem(r = r, i = i, meId = meId, names = data.names)
                    }
                    if (idx >= 20) {
                        item {
                            Text(
                                text = "⋮",
                                color = muted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            )
                        }
                        item {
                            RankRowItem(r = data.rows[idx], i = idx, meId = meId, names = data.names)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyNote2(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = AppColors.extra.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 22.dp, horizontal = 8.dp)
    )
}

@Composable
private fun PeriodChip(text: String, on: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(99.dp)
    val bg = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, if (on) MaterialTheme.colorScheme.primary else AppColors.extra.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 13.5.sp,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun RankRowItem(r: RankRow, i: Int, meId: String, names: Map<String, String>) {
    val mine = r.id == meId
    val gold = Color(0xFFD4A62A)
    val silver = Color(0xFF9AA5A0)
    val bronze = Color(0xFFB87333)
    val medal: Color? = when (i) {
        0 -> gold
        1 -> silver
        2 -> bronze
        else -> null
    }
    val shape = RoundedCornerShape(12.dp)
    val bg = if (mine) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else if (medal != null) {
        medal.copy(alpha = 0.14f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (mine) {
        MaterialTheme.colorScheme.primary
    } else if (medal != null) {
        medal.copy(alpha = 0.6f)
    } else {
        AppColors.extra.line
    }
    val name = (names[r.id]?.takeIf { it.isNotEmpty() } ?: "ইউজার") + (if (mine) " (আপনি)" else "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = bn(i + 1),
            fontWeight = if (medal != null) FontWeight.Bold else FontWeight.Normal,
            color = medal ?: MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = name,
            fontWeight = if (mine) FontWeight.SemiBold else FontWeight.Normal,
            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = bn(r.n) + " পেইজ",
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = AppColors.extra.muted
        )
    }
}
