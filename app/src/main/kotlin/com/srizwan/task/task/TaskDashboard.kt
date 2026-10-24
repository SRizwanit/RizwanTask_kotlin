package com.srizwan.task.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors
import com.srizwan.task.TaskState

/**
 * টাস্ক ট্যাব = Dashboard + সার্চ বার + কিতাবের তালিকা (সবই একটাই LazyVerticalGrid-এ,
 * যাতে নেস্টেড স্ক্রল ক্র্যাশ না হয়)।
 */
@Composable
fun TaskDashboard(
    state: TaskState,
    onGoal: () -> Unit,
    onResume: (ResumeTarget) -> Unit,
    onQuery: (String) -> Unit,
    onReload: () -> Unit,
    rateOf: (Book?) -> Double,
    onOpenBook: (String) -> Unit
) {
    val booksById = remember(state.books) { state.books.associateBy { it.id } }
    val sum = remember(state.fin, state.books) {
        if (state.fin.ready) summarize(state.fin.proofs, booksById, state.fin.s, state.fin.pays) else null
    }
    val byBook = sum?.byBook ?: emptyMap()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            DashboardCard(
                ready = state.fin.ready,
                proofs = state.fin.proofs,
                booksById = booksById,
                goal = state.goal,
                resume = state.resume,
                onGoal = onGoal,
                onResume = onResume
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SearchRow(query = state.query, onQuery = onQuery, onReload = onReload)
        }
        bookGridItems(
            status = state.booksStatus,
            spinning = state.spinning,
            books = state.books,
            query = state.query,
            bookCounts = state.bookCounts,
            byBook = byBook,
            resume = state.resume,
            rateOf = rateOf,
            onOpen = onOpenBook
        )
    }
}

@Composable
private fun SearchRow(query: String, onQuery: (String) -> Unit, onReload: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, AppColors.extra.line, shape)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🔍", fontSize = 15.sp)
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "কিতাব বা লেখকের নাম খুঁজুন",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.extra.muted
                                )
                            }
                            inner()
                        }
                    }
                }
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, AppColors.extra.line, shape)
                .clickable(onClick = onReload)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "⟳", fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Dashboard.jsx: দৈনিক লক্ষ্য, টানা দিন, ব্যাজ, "চালিয়ে যান" */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DashboardCard(
    ready: Boolean,
    proofs: List<Proof>,
    booksById: Map<String, Book>,
    goal: Int,
    resume: Resume?,
    onGoal: () -> Unit,
    onResume: (ResumeTarget) -> Unit
) {
    val rt = resumeTarget(resume, booksById, proofs)
    val hasStat = ready && (proofs.isEmpty() || proofs[0].hasCreatedAt)
    if (!hasStat && rt == null) return

    val muted = AppColors.extra.muted
    val gold = AppColors.extra.gold
    val ok = AppColors.extra.ok
    val primary = MaterialTheme.colorScheme.primary
    val line = AppColors.extra.line
    val tileShape = RoundedCornerShape(12.dp)

    TaskCard {
        if (hasStat) {
            val s = streakInfo(proofs)
            val left = maxOf(0, goal - s.today)
            val hit = s.today >= goal
            val pct = if (goal > 0) (s.today.toFloat() / goal).coerceIn(0f, 1f) else 0f
            val next = BADGES.firstOrNull { it.first > s.best }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // আজকের লক্ষ্য (চাপলে বদলানো যায়)
                Column(
                    modifier = Modifier
                        .weight(1.25f)
                        .clip(tileShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, if (hit) ok else line, tileShape)
                        .clickable(onClick = onGoal)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("🎯 আজকের লক্ষ্য", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = primary)
                    Text(
                        text = countText(bn(s.today), " / ${bn(goal)} পেইজ", muted),
                        fontSize = 13.sp
                    )
                    LinearProgressIndicator(
                        progress = { pct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(99.dp)),
                        color = if (hit) ok else primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = (if (hit) "লক্ষ্য পূরণ হয়েছে ✓" else "আর ${bn(left)} পেইজ বাকি") + " • বদলাতে চাপুন",
                        fontSize = 11.5.sp,
                        color = if (hit) ok else muted,
                        fontWeight = if (hit) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                // টানা কাজ
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(tileShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, line, tileShape)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("⚡ টানা কাজ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = gold)
                    Text(
                        text = countText(bn(s.cur), " দিন", muted),
                        fontSize = 13.sp
                    )
                    val streakHint =
                        if (s.doneToday) "আজকের দিন গোনা হয়েছে"
                        else if (s.cur > 0) "আজ কাজ করলে ${bn(s.cur + 1)} দিন হবে"
                        else "আজ একটি পেইজ করলেই শুরু"
                    Text(
                        text = "$streakHint • সেরা ${bn(s.best)} দিন",
                        fontSize = 11.5.sp,
                        color = muted
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BADGES.forEach { b -> BadgeChip(label = b.second, on = s.best >= b.first) }
            }

            if (next != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "পরের ব্যাজ \"${next.second}\": টানা ${bn(next.first)} দিন (সেরা রেকর্ড থেকে আরও ${bn(next.first - s.best)} দিন)।",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )
            }
        }

        if (rt != null) {
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onResume(rt) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "▶  চালিয়ে যান — " + rt.book.name + (if (rt.stem.isNotEmpty()) " • পেইজ " + rt.stem else ""),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun countText(big: String, rest: String, muted: Color): AnnotatedString =
    buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp)) { append(big) }
        withStyle(SpanStyle(color = muted)) { append(rest) }
    }

@Composable
private fun BadgeChip(label: String, on: Boolean) {
    val shape = RoundedCornerShape(99.dp)
    val gold = AppColors.extra.gold
    val base = Modifier.clip(shape)
    val styled = if (on) {
        base
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFA8761F).copy(alpha = 0.22f), Color(0xFFF2D27A).copy(alpha = 0.38f))
                )
            )
            .border(1.dp, gold.copy(alpha = 0.7f), shape)
    } else {
        base.border(1.dp, AppColors.extra.line, shape)
    }
    Box(modifier = styled.padding(horizontal = 10.dp, vertical = 3.dp)) {
        Text(
            text = "🏆 $label",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (on) gold else AppColors.extra.muted.copy(alpha = 0.75f)
        )
    }
}
