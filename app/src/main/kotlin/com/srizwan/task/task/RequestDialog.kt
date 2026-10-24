package com.srizwan.task.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.srizwan.task.AppColors

/** Dialogs.jsx-এর RequestDialog: পেমেন্ট রিকোয়েস্টের পরিমাণ নিশ্চিত করা */
@Composable
fun RequestDialog(
    info: RequestUi,
    onAmt: (String) -> Unit,
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!info.sending) onCancel() },
        title = { Text("পেমেন্ট রিকোয়েস্ট", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KV("উত্তোলনযোগ্য", info.avail)
                KV("ন্যূনতম রিকোয়েস্ট", info.min)
                KV(info.pmLabel, info.number)
                OutlinedTextField(
                    value = info.amt,
                    onValueChange = onAmt,
                    label = { Text("কত টাকা চাইবেন (৳)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "অ্যাডমিন অনুমোদন দিয়ে এই নাম্বারে টাকা পাঠাবেন। নাম্বার ভুল হলে আগে প্রোফাইল থেকে ঠিক করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.extra.muted
                )
                if (info.err.isNotEmpty()) {
                    Text(
                        text = info.err,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSend, enabled = !info.sending) {
                Text(if (info.sending) "পাঠানো হচ্ছে…" else "রিকোয়েস্ট পাঠান")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !info.sending) { Text("বন্ধ") }
        }
    )
}

@Composable
private fun KV(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AppColors.extra.muted, fontSize = 13.sp, modifier = Modifier.padding(end = 8.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
