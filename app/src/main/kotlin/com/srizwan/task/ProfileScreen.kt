package com.srizwan.task

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.srizwan.task.data.enc
import com.srizwan.task.task.Book
import com.srizwan.task.task.BookSum
import com.srizwan.task.task.NoticeDialog
import com.srizwan.task.task.GuideRequest
import com.srizwan.task.task.bn
import com.srizwan.task.task.money
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/* Profile.jsx → ProfileScreen */

private val PM = ProfileViewModel.PM_LABEL

@Composable
fun ProfileScreen(navController: NavController, vm: ProfileViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.start(context) }
    LaunchedEffect(Unit) {
        vm.toasts.collect { t ->
            Toast.makeText(context, t.text, if (t.isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (state.me == null && state.blocked == null) vm.start(context) else vm.refreshAll()
    }
    LaunchedEffect(state.redirect) {
        when (state.redirect) {
            AuthRedirect.LOGIN -> {
                vm.consumeRedirect()
                navController.navigate(Routes.LOGIN) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            AuthRedirect.TASK -> {
                vm.consumeRedirect()
                navController.navigate(Routes.TASK) {
                    popUpTo(Routes.TASK) { inclusive = false }
                    launchSingleTop = true
                }
            }
            null -> Unit
        }
    }

    val whatsapp: (String) -> Unit = { msg ->
        val url = "https://wa.me/${BuildConfig.ADMIN_WA}?text=" + enc(msg).replace("+", "%20")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(context, "হোয়াটসঅ্যাপ খোলা যায়নি", Toast.LENGTH_LONG).show()
        }
    }

    // ব্লক করা ইউজারের পূর্ণ-পর্দা বার্তা
    if (state.blocked != null) {
        BlockedScreen(
            reason = state.blocked!!,
            onWhatsapp = {
                whatsapp("আসসালামু আলাইকুম, আমার অ্যাকাউন্ট ব্লক দেখাচ্ছে। নাম: ${state.blockedName}, ফোন: ${state.blockedPhone}")
            },
            onLogout = vm::logout
        )
        return
    }

    /** টাস্ক স্ক্রিনে ফিরে ট্যাব খোলা (React: go('task#pay')) */
    val toTask: (String?, Boolean) -> Unit = { tab, guide ->
        GuideRequest.tab = tab
        GuideRequest.pending = guide
        navController.navigate(Routes.TASK) {
            popUpTo(Routes.TASK) { inclusive = false }
            launchSingleTop = true
        }
    }

    Scaffold(
        topBar = {
            ProfileTopBar(
                state = state,
                onThemeToggle = vm::cycleTheme,
                onAdmin = { navController.navigate(Routes.ADMIN) },
                onLogout = vm::logout
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = false, onClick = { toTask("task", false) },
                    icon = { Text("📋", style = MaterialTheme.typography.titleMedium) }, label = { Text("টাস্ক") }
                )
                NavigationBarItem(
                    selected = false, onClick = { toTask("pay", false) },
                    icon = { Text("💰", style = MaterialTheme.typography.titleMedium) }, label = { Text("পেমেন্ট") }
                )
                NavigationBarItem(
                    selected = false, onClick = { toTask("rank", false) },
                    icon = { Text("🏆", style = MaterialTheme.typography.titleMedium) }, label = { Text("র‍্যাংক") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            ProfileCard(
                state = state,
                onTheme = vm::pickTheme,
                onEdit = vm::openEdit,
                onLogout = vm::logout,
                onNotices = vm::openNotices,
                onGuide = { toTask(null, true) },
                onTicket = { navController.navigate(Routes.SUPPORT) },
                onWhatsapp = {
                    val me = state.me
                    whatsapp(
                        "আসসালামু আলাইকুম, আমি কিতাব প্রুফিং অ্যাপের ইউজার। নাম: ${me?.fullName?.ifEmpty { null } ?: "-"}। " +
                            "ফোন নাম্বার: ${me?.phone?.ifEmpty { null } ?: "-"}। আমার একটি সাহায্য প্রয়োজন।"
                    )
                }
            )
        }
    }

    state.edit?.let { e ->
        EditPaymentDialog(
            ui = e,
            onPm = vm::setEditPm,
            onNum = vm::setEditNum,
            onCancel = vm::closeEdit,
            onSave = vm::saveEdit
        )
    }
    state.noticeDialog?.let { nd ->
        NoticeDialog(ui = nd, onIdx = vm::setNoticeIdx, onClose = vm::closeNotices)
    }
}

/* ---------------- টপ বার ---------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    state: ProfileState,
    onThemeToggle: () -> Unit,
    onAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.rizwantask),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                )
                Spacer(Modifier.width(10.dp))
                Text("প্রোফাইল", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        },
        actions = {
            val n = state.myCount
            if (n != null) {
                val gold = AppColors.extra.gold
                val shape = RoundedCornerShape(99.dp)
                Row(
                    modifier = Modifier
                        .clip(shape)
                        .background(gold.copy(alpha = 0.16f))
                        .border(1.dp, gold.copy(alpha = 0.55f), shape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✓", color = gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text(bn(n), color = gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(3.dp))
                    Text("টি সম্পন্ন", color = gold, fontSize = 11.5.sp)
                }
            }
            IconButton(onClick = onThemeToggle) {
                Text(
                    when (ThemeStore.mode) {
                        ThemeMode.Dark -> "🌙"
                        ThemeMode.Light -> "☀️"
                        ThemeMode.System -> "🌓"
                    },
                    fontSize = 18.sp
                )
            }
            if (state.isAdmin) {
                IconButton(onClick = onAdmin) { Text("🛡", fontSize = 18.sp) }
                IconButton(onClick = onLogout) { Text("⏻", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/* ---------------- প্রোফাইল কার্ড ---------------- */

@Composable
private fun ProfileCard(
    state: ProfileState,
    onTheme: (ThemeMode) -> Unit,
    onEdit: () -> Unit,
    onLogout: () -> Unit,
    onNotices: () -> Unit,
    onGuide: () -> Unit,
    onTicket: () -> Unit,
    onWhatsapp: () -> Unit
) {
    val me = state.me
    val sum = state.sum
    val gold = AppColors.extra.gold
    val muted = AppColors.extra.muted

    Surface(
        modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AppColors.extra.line)
    ) {
        Column(
            Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // অবতার
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF0A4F40), Color(0xFF16A583)))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = me?.fullName?.trim()?.ifEmpty { "?" }?.take(1)?.uppercase() ?: "",
                    color = Color(0xFFF7E2A0), fontSize = 28.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(me?.fullName ?: "", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(AppColors.extra.panel2)
                    .padding(horizontal = 8.dp, vertical = 1.dp)
            ) {
                Text(if (state.isAdmin) "অ্যাডমিন" else "ইউজার", color = muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }

            // সম্পন্ন টাস্ক
            StatCard(
                icon = "✓",
                number = if (state.myCount == null && me == null) "০" else (state.myCount?.let { bn(it) } ?: "–"),
                label = " টি টাস্ক সম্পন্ন",
                sub = null,
                modifier = Modifier.padding(top = 16.dp)
            )
            // মোট আয়
            StatCard(
                icon = "৳",
                number = if (sum != null) money(sum.earned) else "০.০০ ৳",
                label = " মোট আয়",
                sub = if (sum != null) {
                    "উত্তোলনযোগ্য " + money(sum.available) +
                        (if (sum.pendingAmt > 0) " • রিভিউ বাকি " + money(sum.pendingAmt) else "")
                } else "",
                modifier = Modifier.padding(top = 10.dp)
            )

            // তথ্য
            Column(Modifier.padding(top = 16.dp)) {
                KV("মোবাইল নাম্বার", me?.phone ?: "")
                KV(
                    if (me != null) (PM[me.paymentMethod] ?: "পেমেন্ট") + " নাম্বার" else "বিকাশ নাম্বার",
                    if (me != null) (me.paymentNumber?.ifEmpty { null } ?: "-") else ""
                )
                KV("যোগ দিয়েছেন", if (me != null) (if (me.createdAt != null) bnDate(me.createdAt) else "-") else "")
            }

            // কিতাব অনুযায়ী কাজ
            val ids = sum?.byBook?.keys?.toList() ?: emptyList()
            if (sum != null && ids.isNotEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Text("আমার কাজ — কিতাব অনুযায়ী", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    ids.forEach { id ->
                        val b = state.booksById[id]
                        val x = sum.byBook.getValue(id)
                        BookProgress(b, x, state.bookCounts[id] ?: x.n)
                    }
                }
            }

            // থিম
            Text(
                "থিম (দিন/রাত)",
                fontSize = 13.sp, color = muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 6.dp)
            )
            ThemeSeg(onTheme)

            // বোতাম
            BlockButton("✎  পেমেন্ট নাম্বার পরিবর্তন", Modifier.padding(top = 16.dp), onClick = onEdit)
            BlockButton(
                "⏻  লগআউট", Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.error, onClick = onLogout
            )
            BlockButton(
                "🔔  নোটিশ ও ঘোষণা", Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.primary, onClick = onNotices, badge = state.badge
            )
            BlockButton(
                "ⓘ  ব্যবহারের গাইড", Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.primary, onClick = onGuide
            )
            // "হোম স্ক্রিনে ইনস্টল করুন" (PWA) নেটিভ অ্যাপে অপ্রযোজ্য, তাই বাদ
            BlockButton(
                "🎫  টিকেট খুলুন", Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.primary, onClick = onTicket
            )
            BlockButton(
                "💬  হোয়াটসঅ্যাপে যোগাযোগ করুন", Modifier.padding(top = 10.dp),
                color = Color(0xFF128C4A), onClick = onWhatsapp
            )
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun StatCard(icon: String, number: String, label: String, sub: String?, modifier: Modifier = Modifier) {
    val gold = AppColors.extra.gold
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = .10f), gold.copy(alpha = .16f))
                )
            )
            .border(1.dp, gold.copy(alpha = .5f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF0A4F40), Color(0xFF16A583)))),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = Color(0xFFF7E2A0), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f, fill = false), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(number, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = AppColors.extra.muted)
            }
            if (!sub.isNullOrEmpty()) {
                Text(sub, fontSize = 12.sp, color = AppColors.extra.muted, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun KV(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = AppColors.extra.muted, fontSize = 14.sp, modifier = Modifier.padding(end = 12.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
    }
}

/** ProgLine: অগ্রগতির বার; মোট পেইজ জানা না থাকলে শুধু সংখ্যা */
@Composable
private fun BookProgress(book: Book?, x: BookSum, done: Int) {
    val total = book?.totalPages ?: 0
    Column(Modifier.padding(bottom = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(book?.name ?: "কিতাব", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("আমার ${bn(x.n)} পেইজ", fontSize = 12.sp, color = AppColors.extra.muted)
        }
        Spacer(Modifier.height(4.dp))
        if (total > 0) {
            val pct = min(100.0, done.toDouble() / total * 100)
            val left = max(0, total - done)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AppColors.extra.line)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((pct / 100).toFloat())
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AppColors.extra.primary2)
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("সম্পন্ন ${bn(done)} / ${bn(total)} (${bn(pct.roundToInt())}%)", fontSize = 11.5.sp, color = AppColors.extra.muted)
                Text("বাকি ${bn(left)}", fontSize = 11.5.sp, color = AppColors.extra.muted)
            }
        } else {
            Text("মোট সম্পন্ন ${bn(done)} পেইজ", fontSize = 11.5.sp, color = AppColors.extra.muted)
        }
    }
}

@Composable
private fun ThemeSeg(onTheme: (ThemeMode) -> Unit) {
    val items = listOf(ThemeMode.System to "সিস্টেম", ThemeMode.Light to "লাইট", ThemeMode.Dark to "ডার্ক")
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, AppColors.extra.line, shape)
    ) {
        items.forEach { (m, label) ->
            val on = ThemeStore.mode == m
            Box(
                Modifier
                    .weight(1f)
                    .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onTheme(m) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun BlockButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    badge: Int = 0,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (color == MaterialTheme.colorScheme.onSurface) AppColors.extra.line else color),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, textAlign = TextAlign.Center)
        if (badge > 0) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(bn(badge), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ---------------- পেমেন্ট নাম্বার ডায়ালগ ---------------- */

@Composable
private fun EditPaymentDialog(
    ui: EditUi,
    onPm: (String) -> Unit,
    onNum: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!ui.saving) onCancel() },
        title = { Text("পেমেন্ট নাম্বার", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("মাধ্যম", fontSize = 13.sp, color = AppColors.extra.muted)
                val shape = RoundedCornerShape(10.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .border(1.dp, AppColors.extra.line, shape)
                ) {
                    listOf("bkash" to "বিকাশ", "nagad" to "নগদ").forEach { (k, label) ->
                        val on = ui.pm == k
                        Box(
                            Modifier
                                .weight(1f)
                                .background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { onPm(k) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label, fontSize = 14.sp,
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = ui.num,
                    onValueChange = onNum,
                    label = { Text("নাম্বার") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                if (ui.err.isNotEmpty()) {
                    Text(ui.err, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { Button(onClick = onSave, enabled = !ui.saving) { Text("সংরক্ষণ") } },
        dismissButton = { TextButton(onClick = onCancel, enabled = !ui.saving) { Text("বন্ধ") } }
    )
}

/* ---------------- ব্লকড স্ক্রিন (common.js: blockedScreen) ---------------- */

@Composable
private fun BlockedScreen(reason: String, onWhatsapp: () -> Unit, onLogout: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 40.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, AppColors.extra.line)
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text("আপনার অ্যাকাউন্ট ব্লক করা হয়েছে", fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(reason, fontSize = 13.sp, color = AppColors.extra.muted, textAlign = TextAlign.Center)
                BlockButton(
                    "হোয়াটসঅ্যাপে অ্যাডমিনের সাথে যোগাযোগ", Modifier.padding(top = 14.dp),
                    color = MaterialTheme.colorScheme.primary, onClick = onWhatsapp
                )
                BlockButton("লগআউট", Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.error, onClick = onLogout)
            }
        }
    }
}
