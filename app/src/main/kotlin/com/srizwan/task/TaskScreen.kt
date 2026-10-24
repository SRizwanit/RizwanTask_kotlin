package com.srizwan.task

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.srizwan.task.data.ActivityLog
import com.srizwan.task.data.Phone
import com.srizwan.task.task.GoalDialog
import com.srizwan.task.task.GuideDialog
import com.srizwan.task.task.NoticeDialog
import com.srizwan.task.task.PayPane
import com.srizwan.task.task.RankPane
import com.srizwan.task.task.RequestDialog
import com.srizwan.task.task.TaskDashboard
import com.srizwan.task.task.bn
import com.srizwan.task.task.summarize

private fun titleOf(tab: TaskTab): String = when (tab) {
    TaskTab.TASK -> "টাস্কের তালিকা"
    TaskTab.PAY -> "পেমেন্ট"
    TaskTab.RANK -> "লিডারবোর্ড"
}

@Composable
fun TaskScreen(navController: NavController, vm: TaskViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.start(context)
    }
    LaunchedEffect(Unit) {
        vm.toasts.collect { t ->
            Toast.makeText(context, t.text, if (t.isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        vm.consumeRequests()
        vm.refreshAll()
    }

    // কিতাব রিডার (ধাপ ৭) তৈরি না হওয়া পর্যন্ত রুট নেই
    val readerPending: () -> Unit = {
        Toast.makeText(context, "কিতাব রিডার (ধাপ ৭) এখনো বানানো হয়নি", Toast.LENGTH_SHORT).show()
    }

    val booksById = remember(state.books) { state.books.associateBy { it.id } }
    val sum = remember(state.fin, state.books) {
        if (state.fin.ready) summarize(state.fin.proofs, booksById, state.fin.s, state.fin.pays) else null
    }

    Scaffold(
        topBar = { TaskTopBar(state, navController) },
        bottomBar = { TaskBottomNav(state.tab, vm) },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            when (state.tab) {
                TaskTab.TASK -> TaskDashboard(
                    state = state,
                    onGoal = { vm.showGoalDialog(true) },
                    onResume = { readerPending() },
                    onQuery = vm::setQuery,
                    onReload = vm::reload,
                    rateOf = vm::rateOfBook,
                    onOpenBook = { readerPending() }
                )
                TaskTab.PAY -> PayPane(
                    me = state.me,
                    settings = state.fin.s,
                    sum = sum,
                    booksById = booksById,
                    proofs = state.fin.proofs,
                    pays = state.fin.pays,
                    rateOf = vm::rateOfBook,
                    reqBusy = state.reqBusy,
                    onRequest = vm::onRequest,
                    onOpenPage = { _, _ -> readerPending() }
                )
                TaskTab.RANK -> RankPane(
                    period = state.rankPeriod,
                    onPeriod = vm::pickPeriod,
                    loading = state.rank.loading,
                    data = state.rank.data,
                    error = state.rank.error,
                    meId = state.me?.id ?: ""
                )
            }
        }
    }

    val gl = state.goalDialog
    if (gl != null) {
        GoalDialog(
            ui = gl,
            onValue = vm::setGoalValue,
            onCancel = { vm.showGoalDialog(false) },
            onSave = vm::saveGoalFromDialog
        )
    }
    val nd = state.noticeDialog
    if (nd != null) {
        NoticeDialog(ui = nd, onIdx = vm::setNoticeIdx, onClose = vm::closeNotices)
    }
    if (state.guideOpen) {
        GuideDialog(onClosed = vm::closeGuide)
    }

    val rq = state.request
    if (rq != null) {
        RequestDialog(
            info = rq,
            onAmt = vm::setRequestAmt,
            onCancel = vm::closeRequest,
            onSend = vm::sendRequest
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTopBar(state: TaskState, nav: NavController) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val isAdmin = Phone.isAdminPhone(state.me?.phone)

    TopAppBar(
        title = {
            Text(
                text = titleOf(state.tab),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        actions = {
            // হেডারে সম্পন্ন টাস্কের সংখ্যা
            val n = state.myCount
            if (n != null) {
                val shape = RoundedCornerShape(99.dp)
                Row(
                    modifier = Modifier
                        .clip(shape)
                        .background(AppColors.extra.gold.copy(alpha = 0.16f))
                        .border(1.dp, AppColors.extra.gold.copy(alpha = 0.55f), shape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "✓", color = AppColors.extra.gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text(text = bn(n), color = AppColors.extra.gold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(3.dp))
                    Text(text = "টি সম্পন্ন", color = AppColors.extra.gold, fontSize = 11.5.sp)
                }
            }
            IconButton(onClick = { nav.navigate(Routes.PROFILE) }) {
                Box {
                    Text("👤", fontSize = 18.sp, modifier = Modifier.align(Alignment.Center))
                    if (state.badge > 0) {
                        // নতুন নোটিশের লাল ব্যাজ (React: .tbadge)
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 8.dp, y = (-6).dp)
                                .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(Color(0xFFE5484D))
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(bn(state.badge), color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            IconButton(onClick = { menuExpanded = true }) {
                Text("⋮", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                if (isAdmin) {
                    DropdownMenuItem(
                        text = { Text("অ্যাডমিন") },
                        onClick = {
                            menuExpanded = false
                            nav.navigate(Routes.ADMIN)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("লগআউট") },
                    onClick = {
                        menuExpanded = false
                        ActivityLog.log(context, "logout", "")
                        Session.clear(context)
                        nav.navigate(Routes.LOGIN) {
                            popUpTo(Routes.TASK) { inclusive = true }
                        }
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun TaskBottomNav(tab: TaskTab, vm: TaskViewModel) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            selected = tab == TaskTab.TASK,
            onClick = { vm.switchTab(TaskTab.TASK) },
            icon = { Text("📋", style = MaterialTheme.typography.titleMedium) },
            label = { Text("টাস্ক") }
        )
        NavigationBarItem(
            selected = tab == TaskTab.PAY,
            onClick = { vm.switchTab(TaskTab.PAY) },
            icon = { Text("💰", style = MaterialTheme.typography.titleMedium) },
            label = { Text("পেমেন্ট") }
        )
        NavigationBarItem(
            selected = tab == TaskTab.RANK,
            onClick = { vm.switchTab(TaskTab.RANK) },
            icon = { Text("🏆", style = MaterialTheme.typography.titleMedium) },
            label = { Text("র‍্যাংক") }
        )
    }
}
