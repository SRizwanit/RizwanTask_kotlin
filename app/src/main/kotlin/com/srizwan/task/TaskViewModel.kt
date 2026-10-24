package com.srizwan.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.ActivityLog
import com.srizwan.task.data.Api
import com.srizwan.task.data.enc
import com.srizwan.task.task.AppSettings
import com.srizwan.task.task.Book
import com.srizwan.task.task.BooksStatus
import com.srizwan.task.task.Fin
import com.srizwan.task.task.GoalUi
import com.srizwan.task.task.GuideRequest
import com.srizwan.task.task.Notice
import com.srizwan.task.task.NoticeUi
import com.srizwan.task.task.MeInfo
import com.srizwan.task.task.PM
import com.srizwan.task.task.Proof
import com.srizwan.task.task.RankData
import com.srizwan.task.task.RankPeriod
import com.srizwan.task.task.RankRow
import com.srizwan.task.task.RankUi
import com.srizwan.task.task.RequestUi
import com.srizwan.task.task.Resume
import com.srizwan.task.task.Summary
import com.srizwan.task.task.bn
import com.srizwan.task.task.getResume
import com.srizwan.task.task.goalHitShown
import com.srizwan.task.task.goalReached
import com.srizwan.task.task.guideSeen
import com.srizwan.task.task.markGoalHitShown
import com.srizwan.task.task.markGuideSeen
import com.srizwan.task.task.parseNotice
import com.srizwan.task.task.parseTime
import com.srizwan.task.task.readSeen
import com.srizwan.task.task.writeSeen
import com.srizwan.task.task.minPayout
import com.srizwan.task.task.money
import com.srizwan.task.task.parseBook
import com.srizwan.task.task.parsePay
import com.srizwan.task.task.parseProof
import com.srizwan.task.task.parseSettings
import com.srizwan.task.task.r2
import com.srizwan.task.task.readGoal
import com.srizwan.task.task.rateOf
import com.srizwan.task.task.str
import com.srizwan.task.task.summarize
import com.srizwan.task.task.writeGoal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

enum class TaskTab { TASK, PAY, RANK }

data class TaskState(
    val me: MeInfo? = null,
    val tab: TaskTab = TaskTab.TASK,
    val query: String = "",
    val books: List<Book> = emptyList(),
    val booksStatus: BooksStatus = BooksStatus.LOADING,
    val spinning: Boolean = false,
    val bookCounts: Map<String, Int> = emptyMap(),
    val myCount: Int? = null,
    val fin: Fin = Fin(),
    val reqBusy: Boolean = false,
    val goal: Int = 20,
    val resume: Resume? = null,
    val goalDialog: GoalUi? = null,
    val noticeDialog: NoticeUi? = null,
    val guideOpen: Boolean = false,
    /** প্রোফাইল আইকনের লাল ব্যাজ: নতুন নোটিশের সংখ্যা */
    val badge: Int = 0,
    val request: RequestUi? = null,
    val rankPeriod: RankPeriod = RankPeriod.DAY,
    val rank: RankUi = RankUi()
) {
    val booksById: Map<String, Book> get() = books.associateBy { it.id }

    /** আয়ের হিসাব; তথ্য আসার আগে null (স্কেলিটন দেখানো হয়) */
    val sum: Summary?
        get() = if (fin.ready) summarize(fin.proofs, booksById, fin.s, fin.pays) else null
}

/** স্ক্রিনে দেখানোর ছোট বার্তা (React-এর toast) */
data class UiToast(val text: String, val isError: Boolean = false)

/** ব্যর্থ হলে null; বাতিল (cancel) হলে ব্যতিক্রম আবার ছুড়ে দেয় */
private inline fun <T> attempt(block: () -> T): T? {
    return try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

class TaskViewModel : ViewModel() {
    private val _state = MutableStateFlow(TaskState())
    val state = _state.asStateFlow()

    private val _toasts = MutableSharedFlow<UiToast>(extraBufferCapacity = 16)
    val toasts: SharedFlow<UiToast> = _toasts.asSharedFlow()

    private lateinit var appContext: Context
    private var started = false
    private var lastRefresh = 0L

    private var busyCount = false
    private var busyMoney = false
    private var rankReq = 0
    private val rankCache = HashMap<RankPeriod, RankCache>()

    private class RankCache(val t: Long, val rows: List<RankRow>, val names: Map<String, String>)

    private fun api() = Api(appContext)
    private fun toast(text: String, error: Boolean = false) {
        _toasts.tryEmit(UiToast(text, error))
    }

    /* ---------- শুরু ---------- */

    fun start(context: Context) {
        if (started) return
        val s = Session.get(context) ?: return
        val uid = s.optInt("id", 0)
        if (uid == 0) return
        started = true
        appContext = context.applicationContext

        val idStr = uid.toString()
        when (GuideRequest.tab) {
            "pay" -> _state.update { it.copy(tab = TaskTab.PAY) }
            "rank" -> _state.update { it.copy(tab = TaskTab.RANK) }
        }
        GuideRequest.tab = null
        val fallbackMe = MeInfo(
            id = idStr,
            phone = s.optString("phone", ""),
            fullName = s.optString("name", ""),
            paymentMethod = null,
            paymentNumber = null,
            createdAt = null
        )
        _state.update {
            it.copy(
                me = fallbackMe,
                goal = readGoal(appContext, idStr),
                resume = getResume(appContext, idStr)
            )
        }

        viewModelScope.launch {
            // পেমেন্ট নাম্বার/মাধ্যম লাগে, তাই ইউজারের তাজা তথ্য আনি
            attempt {
                val arr = api().get("app_users?id=eq.${enc(idStr)}&limit=1")
                if (arr.length() > 0) {
                    val u = arr.getJSONObject(0)
                    _state.update {
                        it.copy(
                            me = MeInfo(
                                id = idStr,
                                phone = u.str("phone") ?: fallbackMe.phone,
                                fullName = u.str("full_name") ?: fallbackMe.fullName,
                                paymentMethod = u.str("payment_method"),
                                paymentNumber = u.str("payment_number"),
                                createdAt = u.str("created_at")
                            )
                        )
                    }
                }
            }
            lastRefresh = System.currentTimeMillis()
            launch { loadCount() }
            // প্রথমবারের গাইড (বা প্রোফাইলের "ব্যবহারের গাইড" বোতাম); দেখা থাকলে নোটিশ লোড
            launch {
                val requested = GuideRequest.pending
                GuideRequest.pending = false
                if (requested) {
                    delay(200)
                    openGuide()
                } else if (!guideSeen(appContext, idStr)) {
                    delay(500)
                    openGuide()
                } else {
                    loadNotices()
                }
            }
            // প্রতি ৩ মিনিটে নতুন নোটিশ খোঁজা
            launch {
                while (true) {
                    delay(180_000)
                    loadNotices()
                }
            }
            load()
        }
    }

    /** অ্যাপে ফিরে এলে (React-এর visibilitychange/pageshow) */
    fun refreshAll() {
        if (!started) return
        val now = System.currentTimeMillis()
        if (now - lastRefresh < 20000) return
        lastRefresh = now
        viewModelScope.launch {
            launch { loadCount() }
            launch { loadCounts() }
            launch { loadMoney() }
            launch { loadNotices() }
        }
    }

    /** রিফ্রেশ বোতাম (React-এর bReload) */
    fun reload() {
        if (!started) return
        lastRefresh = System.currentTimeMillis()
        viewModelScope.launch {
            launch { loadCount() }
            launch { loadNotices() }
            load()
        }
    }

    /**
     * প্রোফাইল স্ক্রিন থেকে ফিরে এলে (React: `task#pay`, `task#guide` হ্যাশ) অনুরোধ পূরণ:
     * ট্যাব বদল / গাইড খোলা।
     */
    fun consumeRequests() {
        if (!started) return
        val tab = GuideRequest.tab
        GuideRequest.tab = null
        when (tab) {
            "task" -> switchTab(TaskTab.TASK)
            "pay" -> switchTab(TaskTab.PAY)
            "rank" -> switchTab(TaskTab.RANK)
        }
        if (GuideRequest.pending) {
            GuideRequest.pending = false
            viewModelScope.launch {
                delay(200)
                openGuide()
            }
        }
    }

    /** ধাপ 7-এর রিডার/প্রোফাইল থেকেও গাইড খোলা যাবে */
    fun openGuide() {
        _state.update { it.copy(guideOpen = true) }
    }

    /** গাইড বন্ধ: "দেখা হয়েছে" মনে রাখা, তারপর নোটিশ লোড */
    fun closeGuide() {
        val me = _state.value.me
        if (me != null) markGuideSeen(appContext, me.id)
        _state.update { it.copy(guideOpen = false) }
        viewModelScope.launch { loadNotices() }
    }

    /* ---------- নোটিশ / ঘোষণা ---------- */

    private var allNotices: List<Notice> = emptyList()

    private suspend fun loadNotices() {
        val u = _state.value.me ?: return
        val list = attempt {
            val arr = api().get(
                "notices?or=" + enc("(user_id.is.null,user_id.eq.${u.id})") + "&order=created_at.desc&limit=40"
            )
            (0 until arr.length()).map { parseNotice(arr.getJSONObject(it)) }
        } ?: return   // notices টেবিল না থাকলে চুপচাপ বাদ
        allNotices = list
        val seen = readSeen(appContext, u.id)
        val joined = (parseTime(u.createdAt) ?: 0L).let { if (it > 0) it - 86_400_000L else 0L }
        val fresh = list.filter { it.id !in seen && it.createdMs >= joined }.reversed()
        _state.update { it.copy(badge = fresh.size) }
        val st = _state.value
        if (fresh.isNotEmpty() && st.noticeDialog == null && !st.guideOpen) {
            _state.update { it.copy(noticeDialog = NoticeUi(fresh, 0)) }
        }
    }

    fun setNoticeIdx(i: Int) {
        _state.update { st -> st.copy(noticeDialog = st.noticeDialog?.let { it.copy(idx = i.coerceIn(0, it.list.size - 1)) }) }
    }

    /** দেখানো সব নোটিশ "দেখা" হিসেবে মনে রাখি */
    fun closeNotices() {
        val me = _state.value.me
        val shown = _state.value.noticeDialog?.list ?: emptyList()
        if (me != null) {
            val seen = readSeen(appContext, me.id).toMutableSet()
            shown.forEach { seen.add(it.id) }
            writeSeen(appContext, me.id, seen)
            _state.update { it.copy(noticeDialog = null, badge = allNotices.count { n -> n.id !in seen }) }
        } else {
            _state.update { it.copy(noticeDialog = null) }
        }
    }

    /* ---------- ট্যাব ও ছোট স্টেট ---------- */

    fun switchTab(t: TaskTab) {
        _state.update { it.copy(tab = t) }
        viewModelScope.launch {
            when (t) {
                TaskTab.PAY -> loadMoney()
                TaskTab.RANK -> loadRank(_state.value.rankPeriod)
                TaskTab.TASK -> loadCounts()
            }
        }
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
    }

    /** React-এর openGoal: ডায়ালগ খোলার সময় বর্তমান লক্ষ্য ঘরে বসে */
    fun showGoalDialog(show: Boolean) {
        _state.update {
            it.copy(goalDialog = if (show) GoalUi(it.goal.toString()) else null)
        }
    }

    fun setGoalValue(v: String) {
        _state.update { st -> st.copy(goalDialog = st.goalDialog?.copy(value = v, err = "")) }
    }

    /** React-এর saveGoal: ১–১০০০ যাচাই, সংরক্ষণ, বার্তা */
    fun saveGoalFromDialog() {
        val me = _state.value.me ?: return
        val g = _state.value.goalDialog?.value?.trim()?.toDoubleOrNull()?.let { Math.floor(it).toInt() }
        if (g == null || g < 1 || g > 1000) {
            _state.update { st -> st.copy(goalDialog = st.goalDialog?.copy(err = "১ থেকে ১০০০-এর মধ্যে একটি সংখ্যা দিন")) }
            return
        }
        writeGoal(appContext, me.id, g)
        _state.update { it.copy(goal = g, goalDialog = null) }
        toast("দৈনিক লক্ষ্য: ${bn(g)} পেইজ")
    }

    fun rateOfBook(b: Book?): Double = rateOf(b, _state.value.fin.s)

    /* ---------- আমার সম্পন্ন টাস্ক ---------- */

    private suspend fun loadCount() {
        val u = _state.value.me ?: return
        if (busyCount) return
        busyCount = true
        try {
            var n: Long? = attempt {
                val r = api().rpc("my_proof_count", JSONObject().put("p_user", u.id))
                val v = if (r is org.json.JSONArray) (if (r.length() > 0) r.get(0) else null) else r
                (v as? Number)?.toLong()?.takeIf { it >= 0 } ?: throw IllegalStateException("bad")
            }
            if (n == null) {
                // ফাংশন না থাকলে সরাসরি গুনে নিন
                val path = "page_proofs?user_id=eq.${enc(u.id)}&select=stem&order=stem.asc"
                n = attempt {
                    var total = 0L
                    var off = 0
                    while (true) {
                        val r = api().get("$path&limit=1000&offset=$off")
                        total += r.length()
                        if (r.length() < 1000) break
                        off += 1000
                    }
                    total
                }
            }
            if (n != null) _state.update { it.copy(myCount = n.toInt()) }
        } finally {
            busyCount = false
        }
    }

    /* ---------- কিতাব তালিকা + অগ্রগতির বার ---------- */

    private suspend fun fetchBooks(): List<Book> {
        val arr = api().get("books?select=*&order=created_at.desc")
        return (0 until arr.length()).map { parseBook(arr.getJSONObject(it)) }
    }

    private suspend fun loadCounts() {
        attempt {
            val counts = HashMap<String, Int>()
            val rpc = attempt {
                val r = api().rpc("book_proof_counts", JSONObject())
                r as? org.json.JSONArray ?: throw IllegalStateException("bad")
            }
            if (rpc != null) {
                for (i in 0 until rpc.length()) {
                    val o = rpc.getJSONObject(i)
                    val id = o.str("book_id") ?: continue
                    counts[id] = (o.optDouble("n", 0.0)).toInt()
                }
            } else {
                val all = api().getAll("page_proofs?select=book_id,stem&order=book_id.asc,stem.asc")
                for (r in all) {
                    val id = r.str("book_id") ?: continue
                    counts[id] = (counts[id] ?: 0) + 1
                }
            }
            _state.update { it.copy(bookCounts = counts) }
        }
        // টেবিল না থাকলে বার ছাড়াই চলবে
    }

    private suspend fun load() {
        val hasBooks = _state.value.books.isNotEmpty()
        if (!hasBooks) _state.update { it.copy(booksStatus = BooksStatus.LOADING) }
        else _state.update { it.copy(spinning = true) }
        try {
            val b = fetchBooks()
            _state.update { it.copy(books = b, booksStatus = BooksStatus.READY) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            toast("কিতাব লোড হয়নি: ${e.message ?: ""}", true)
            if (_state.value.books.isEmpty()) _state.update { it.copy(booksStatus = BooksStatus.ERROR) }
        } finally {
            _state.update { it.copy(spinning = false) }
        }
        viewModelScope.launch { loadCounts() }
        viewModelScope.launch { loadMoney() }
    }

    /* ---------- সেটিংস + আয়ের হিসাব ---------- */

    private suspend fun fetchMine(uid: String): List<Proof> {
        val rows = try {
            api().getAll(
                "page_proofs?user_id=eq.${enc(uid)}" +
                    "&select=book_id,stem,review_status,review_note,rate,created_at,reviewed_at" +
                    "&order=created_at.desc,book_id.asc,stem.asc"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            api().getAll(
                "page_proofs?user_id=eq.${enc(uid)}&select=book_id,stem&order=book_id.asc,stem.asc"
            )
        }
        return rows.map { parseProof(it) }
    }

    /** সর্বশেষ তথ্য ফেরত দেয় (রিকোয়েস্টের আগে তাজা তথ্য লাগে) */
    private suspend fun loadMoney(): Fin {
        val u = _state.value.me ?: return _state.value.fin
        if (busyMoney) return _state.value.fin
        busyMoney = true
        try {
            var s: AppSettings = _state.value.fin.s
            attempt {
                val a = api().get("app_settings?id=eq.1&select=page_rate,min_payout,warn_pct,warn_min&limit=1")
                s = if (a.length() > 0) parseSettings(a.getJSONObject(0)) else AppSettings()
            }
            if (_state.value.books.isEmpty()) {
                attempt {
                    val b = fetchBooks()
                    _state.update { it.copy(books = b) }
                }
            }
            val proofs = fetchMine(u.id)
            val pays = attempt {
                api().getAll("payments?user_id=eq.${enc(u.id)}&order=created_at.desc").map { parsePay(it) }
            } ?: emptyList()
            val next = Fin(s = s, proofs = proofs, pays = pays, ready = true)
            _state.update { it.copy(fin = next) }
            goalToast(proofs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // নেটওয়ার্ক/টেবিল সমস্যা: আগের তথ্য থাকুক
        } finally {
            busyMoney = false
        }
        return _state.value.fin
    }

    /** লক্ষ্য পূরণ হলে দিনে একবার বার্তা */
    private fun goalToast(proofs: List<Proof>) {
        val me = _state.value.me ?: return
        val goal = _state.value.goal
        val s = goalReached(proofs, goal) ?: return
        if (goalHitShown(appContext, me.id)) return
        markGoalHitShown(appContext, me.id)
        toast("মাশাআল্লাহ! আজকের লক্ষ্য পূরণ হয়েছে (${bn(s.today)}/${bn(goal)} পেইজ)")
    }

    private fun summaryOf(f: Fin): Summary? =
        if (f.ready) summarize(f.proofs, _state.value.booksById, f.s, f.pays) else null

    /* ---------- পেমেন্ট রিকোয়েস্ট ---------- */

    fun onRequest() {
        viewModelScope.launch {
            _state.update { it.copy(reqBusy = true) }
            try {
                val f = loadMoney()
                val s = summaryOf(f)
                val min = minPayout(f.s)
                val u = _state.value.me
                if (s == null || u == null) {
                    toast("তথ্য আনা যায়নি, আবার চেষ্টা করুন", true)
                    return@launch
                }
                val number = u.paymentNumber
                if (number.isNullOrEmpty()) {
                    toast("আগে পেমেন্ট নাম্বার দিন", true)
                    return@launch
                }
                if (s.available < min || s.available <= 0) {
                    toast("ন্যূনতম ${money(min)} হলে রিকোয়েস্ট করা যাবে", true)
                    return@launch
                }
                _state.update {
                    it.copy(
                        request = RequestUi(
                            avail = money(s.available),
                            min = money(min),
                            pmLabel = (PM[u.paymentMethod] ?: "পেমেন্ট") + " নাম্বার",
                            number = number,
                            max = s.available,
                            amt = amtText(s.available)
                        )
                    )
                }
            } finally {
                _state.update { it.copy(reqBusy = false) }
            }
        }
    }

    fun setRequestAmt(v: String) {
        _state.update { st -> st.copy(request = st.request?.copy(amt = v)) }
    }

    fun closeRequest() {
        _state.update { it.copy(request = null) }
    }

    fun sendRequest() {
        val current = _state.value.request ?: return
        if (current.sending) return
        viewModelScope.launch {
            _state.update { st -> st.copy(request = st.request?.copy(sending = true, err = "")) }
            try {
                val f = loadMoney()
                val s = summaryOf(f)
                val min = minPayout(f.s)
                val amt = r2(_state.value.request?.amt?.trim()?.toDoubleOrNull() ?: 0.0)
                val u = _state.value.me
                if (s == null || u == null) throw IllegalStateException("তথ্য আনা যায়নি, আবার চেষ্টা করুন")
                if (amt <= 0) throw IllegalStateException("সঠিক পরিমাণ দিন")
                if (amt < min) throw IllegalStateException("ন্যূনতম ${money(min)} চাইতে হবে")
                if (amt > s.available) throw IllegalStateException("উত্তোলনযোগ্য ${money(s.available)}-এর বেশি চাওয়া যাবে না")

                val body = JSONObject().apply {
                    put("user_id", u.id)
                    put("user_name", u.fullName)
                    put("amount", amt)
                    put("pages", s.approved)
                    put("method", u.paymentMethod)
                    put("number", u.paymentNumber)
                    put("status", "requested")
                }
                api().post("payments", body)
                ActivityLog.log(
                    appContext, "payment_request",
                    money(amt) + " • " + (PM[u.paymentMethod] ?: "") + " " + (u.paymentNumber ?: "")
                )
                _state.update { it.copy(request = null) }
                toast("পেমেন্ট রিকোয়েস্ট পাঠানো হয়েছে")
                launch { loadMoney() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val m = e.message ?: "রিকোয়েস্ট পাঠানো যায়নি"
                val msg = if (Regex("payments", RegexOption.IGNORE_CASE).containsMatchIn(m) &&
                    Regex("404|PGRST205|42P01", RegexOption.IGNORE_CASE).containsMatchIn(m)
                ) "পেমেন্ট টেবিল নেই — অ্যাডমিন supabase_v2.sql চালাননি" else m
                _state.update { st -> st.copy(request = st.request?.copy(err = msg)) }
            } finally {
                _state.update { st -> st.copy(request = st.request?.copy(sending = false)) }
            }
        }
    }

    private fun amtText(x: Double): String {
        // JS String(number) এর মতো: পূর্ণসংখ্যা হলে দশমিক ছাড়া
        return if (x == Math.floor(x) && x < 1e15) x.toLong().toString() else x.toString()
    }

    /* ---------- লিডারবোর্ড (প্রতি সময়কালে ৬০ সেকেন্ড ক্যাশ) ---------- */

    fun pickPeriod(p: RankPeriod) {
        _state.update { it.copy(rankPeriod = p) }
        viewModelScope.launch { loadRank(p) }
    }

    private suspend fun loadRank(period: RankPeriod) {
        if (_state.value.me == null) return
        val token = ++rankReq   // দ্রুত ট্যাব বদলালে পুরনো উত্তর নতুনটাকে ঢাকবে না
        _state.update { it.copy(rank = it.rank.copy(loading = true)) }
        try {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val since: Instant? = when (period) {
                RankPeriod.DAY -> today.atStartOfDay(zone).toInstant()
                RankPeriod.WEEK -> today.minusDays(6).atStartOfDay(zone).toInstant()
                RankPeriod.ALL -> null
            }
            val c = rankCache[period]
            val rows: List<RankRow>
            val names: Map<String, String>
            if (c != null && System.currentTimeMillis() - c.t < 60000) {
                rows = c.rows
                names = c.names
            } else {
                val fetched = fetchLeaderboard(since)
                val nm = HashMap<String, String>()
                val users = api().get("app_users?select=id,full_name&limit=5000")
                for (i in 0 until users.length()) {
                    val u = users.getJSONObject(i)
                    val id = u.str("id") ?: continue
                    nm[id] = u.str("full_name") ?: ""
                }
                rows = fetched
                names = nm
                rankCache[period] = RankCache(System.currentTimeMillis(), rows, names)
            }
            val clean = rows.filter { it.n > 0 }
            if (token != rankReq) return
            _state.update { it.copy(rank = RankUi(loading = false, data = RankData(period, clean, names), error = "")) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (token != rankReq) return
            _state.update { it.copy(rank = it.rank.copy(loading = false, error = e.message ?: "অজানা ত্রুটি")) }
        }
    }

    /** rpc/leaderboard; না থাকলে page_proofs গুনে নেয় */
    private suspend fun fetchLeaderboard(since: Instant?): List<RankRow> {
        val sinceIso = since?.toString()
        val viaRpc = attempt {
            val body = JSONObject()
            body.put("p_since", sinceIso ?: JSONObject.NULL)
            val r = api().rpc("leaderboard", body)
            val arr = r as? org.json.JSONArray ?: throw IllegalStateException("bad")
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val id = o.str("user_id") ?: return@mapNotNull null
                RankRow(id, (o.optDouble("n", 0.0)).toLong())
            }
        }
        if (viaRpc != null) return viaRpc

        val f = if (sinceIso != null) "&created_at=gte.${enc(sinceIso)}" else ""
        val all: List<JSONObject> = try {
            val list = api().getAll(
                "page_proofs?select=user_id,review_status,book_id,stem$f&order=user_id.asc,book_id.asc,stem.asc"
            )
            list.filter { it.str("review_status") != "correction" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            api().getAll("page_proofs?select=user_id,book_id,stem&order=user_id.asc,book_id.asc,stem.asc")
        }
        val m = LinkedHashMap<String, Long>()
        for (r in all) {
            val id = r.str("user_id") ?: continue
            m[id] = (m[id] ?: 0L) + 1L
        }
        return m.map { RankRow(it.key, it.value) }.sortedByDescending { it.n }
    }
}
