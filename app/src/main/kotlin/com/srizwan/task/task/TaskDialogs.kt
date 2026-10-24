package com.srizwan.task.task

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.srizwan.task.AppColors
import org.json.JSONObject

/* Dialogs.jsx: GoalDialog, NoticeDialog (RequestDialog আগে থেকেই RequestDialog.kt-তে আছে) */

/** helpers.js-এর KIND */
val NOTICE_KIND = mapOf("notice" to "ঘোষণা", "review" to "রিভিউ", "payment" to "পেমেন্ট")

/** `notices` টেবিলের সারি */
data class Notice(
    val id: String,
    val kind: String?,
    val title: String,
    val body: String,
    val createdAt: String?,
    val createdMs: Long
)

fun parseNotice(o: JSONObject): Notice = Notice(
    id = o.str("id") ?: "",
    kind = o.str("kind"),
    title = o.str("title") ?: "",
    body = o.str("body") ?: "",
    createdAt = o.str("created_at"),
    createdMs = parseTime(o.str("created_at")) ?: 0L
)

/** দৈনিক লক্ষ্যের ডায়ালগের অবস্থা (React: glVal, glErr) */
data class GoalUi(val value: String, val err: String = "")

/** নোটিশ ডায়ালগের অবস্থা (React: nList, nIdx) */
data class NoticeUi(val list: List<Notice>, val idx: Int = 0)

/* ---------------- GoalDialog ---------------- */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalDialog(
    ui: GoalUi,
    onValue: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("দৈনিক লক্ষ্য", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "প্রতিদিন কত পেইজ প্রুফড করতে চান? (এই ডিভাইসে সংরক্ষিত থাকবে)",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.extra.muted
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    listOf(10, 20, 30, 50).forEach { g ->
                        AssistChip(
                            onClick = { onValue(g.toString()) },
                            label = { Text(bn(g), fontSize = 12.5.sp) },
                            shape = RoundedCornerShape(99.dp)
                        )
                    }
                }
                OutlinedTextField(
                    value = ui.value,
                    onValueChange = { v -> onValue(v.filter { it.isDigit() }.take(4)) },
                    label = { Text("অথবা নিজের সংখ্যা") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (ui.err.isNotEmpty()) {
                    Text(text = ui.err, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("সংরক্ষণ") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("বন্ধ") } }
    )
}

/* ---------------- NoticeDialog ---------------- */

@Composable
fun NoticeDialog(
    ui: NoticeUi,
    onIdx: (Int) -> Unit,
    onClose: () -> Unit
) {
    val list = ui.list
    if (list.isEmpty()) return
    val idx = ui.idx.coerceIn(0, list.size - 1)
    val cur = list[idx]
    val last = idx >= list.size - 1
    val gold = AppColors.extra.gold

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 560.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, gold),
            shadowElevation = 12.dp
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(gold.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = NOTICE_KIND[cur.kind] ?: "নোটিশ",
                            color = gold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = if (cur.createdAt != null) fmtDT(cur.createdAt) else "",
                        color = AppColors.extra.muted,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = cur.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Column(
                    Modifier
                        .padding(top = 10.dp)
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = cur.body, lineHeight = 26.sp, fontSize = 15.sp)
                }
                if (list.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        list.indices.forEach { i ->
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (i == idx) MaterialTheme.colorScheme.primary else AppColors.extra.line)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (idx > 0) TextButton(onClick = { onIdx(maxOf(0, idx - 1)) }) { Text("আগের") }
                    if (!last) TextButton(onClick = { onIdx(minOf(list.size - 1, idx + 1)) }) { Text("পরের") }
                    Button(onClick = onClose) { Text(if (last) "ঠিক আছে" else "বন্ধ করুন") }
                }
            }
        }
    }
}
