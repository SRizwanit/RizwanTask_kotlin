package com.srizwan.task

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DebugActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "কোনো তথ্য পাওয়া যায়নি"
        val report = buildReport(trace)
        setContent {
            RizwanTheme {
                DebugScreen(
                    report = report,
                    onRestart = { restartApp() },
                    onClose = { finishAndRemoveTask() }
                )
            }
        }
    }

    private fun restartApp() {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(launch)
        }
        finish()
    }

    private fun buildReport(trace: String): String {
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (_: Throwable) {
            "?"
        }
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        return buildString {
            append("অ্যাপ: ").append(packageName).append(" v").append(version).append('\n')
            append("ডিভাইস: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
            append("Android: ").append(Build.VERSION.RELEASE)
                .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
            append("সময়: ").append(time).append("\n\n")
            append(trace)
        }
    }

    companion object {
        const val EXTRA_TRACE = "trace"
    }
}

@Composable
private fun DebugScreen(
    report: String,
    onRestart: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "অ্যাপ ক্র্যাশ করেছে",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "নিচের লেখা কপি করে পাঠিয়ে দিন",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.extra.muted
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("crash", report))
                    copied = true
                }) {
                    Text(if (copied) "কপি হয়েছে" else "এরর কপি")
                }
                OutlinedButton(onClick = onRestart) { Text("আবার চালু") }
                OutlinedButton(onClick = onClose) { Text("বন্ধ") }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = report,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
