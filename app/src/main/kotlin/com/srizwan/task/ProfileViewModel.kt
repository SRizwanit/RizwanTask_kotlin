package com.srizwan.task

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.ActivityLog
import com.srizwan.task.data.Api
import com.srizwan.task.data.Auth
import com.srizwan.task.data.AuthResult
import com.srizwan.task.data.Phone
import com.srizwan.task.data.enc
import com.srizwan.task.task.AppSettings
import com.srizwan.task.task.Book
import com.srizwan.task.task.Notice
import com.srizwan.task.task.NoticeUi
import com.srizwan.task.task.Pay
import com.srizwan.task.task.Summary
import com.srizwan.task.task.parseBook
import com.srizwan.task.task.parseNotice
import com.srizwan.task.task.parsePay
import com.srizwan.task.task.parseProof
import com.srizwan.task.task.parseSettings
import com.srizwan.task.task.parseTime
import com.srizwan.task.task.readSeen
import com.srizwan.task.task.str
import com.srizwan.task.task.summarize
import com.srizwan.task.task.writeSeen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.cancellation.CancellationException

/* Profile.jsx-এর লজিক */

/** `app_users` সারির দরকারি অংশ */
data class ProfileUser(
    val id: String,
    val fullName: String,
    val phone: String,
    val paymentMethod: String?,
    val paymentNumber: String?,
    val createdAt: String?
)

private fun parseUser(o: JSONObject) = ProfileUser(
    id = o.opt("id").toString(),
    fullName = o.str("full_name") ?: "",
    phone = o.str("phone") ?: "",
    paymentMethod = o.str("payment_method"),
    paymentNumber = o.str("payment_number"),
    createdAt = o.str("created_at")
)

/** পেমেন্ট নাম্বার বদলের ডায়ালগ (React: edit) */
data class EditUi(val pm: String, val num: String, val err: String = "", val saving: Boolean = false)

/** স্ক্রিন ছেড়ে কোথায় যেতে হবে */
enum class AuthRedirect { LOGIN, TASK }

data class ProfileState(
    val me: ProfileUser? = null,
    val myCount: Int? = null,
    val sum: Summary? = null,
    val booksById: Map<String, Book> = emptyMap(),
    val bookCounts: Map<String, Int> = emptyMap(),
    val badge: Int = 0,
    val edit: EditUi? = null,
    val noticeDialog: NoticeUi? = null,
    val blocked: String? = null,       // ব্লক হলে কারণ (বার্তা)
    val blockedName: String = "",
    val blockedPhone: String = "",
    val redirect: AuthRedirect? = null
) {
    val isAdmin: Boolean get() = me != null && Phone.isAdminPhone(me.phone)
}

private inline fun <T> attempt(block: () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    null
}

class ProfileViewModel : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val _toasts = MutableSharedFlow<UiToast>(extraBufferCapacity = 16)
    val toasts: SharedFlow<UiToast> = _toasts.asSharedFlow()

    private lateinit var appContext: Context
    private var started = false
    private var lastRefresh = 0L
    private var busyCount = false
    private var busyMoney = false
    private var allNotices: List<Notice> = emptyList()
    private var books: List<Book> = emptyList()
    private var createdAtMs = 0L

    private fun api() = Api(appContext)
    private fun toast(text: String, error: Boolean = false) {
        _toasts.tryEmit(UiToast(text, error))
    }

    /* ---------- শুরু: requireAuth + প্রাথমিক লোড ---------- */

    fun start(context: Context) {
        if (started) return
        appContext = context.applicationContext
        started = true
        viewModelScope.launch {
            when (val r = Auth.requireAuth(appContext)) {
                is AuthResult.Ok -> {
                    val u = parseUser(r.user)
                    createdAtMs = parseTime(u.createdAt) ?: 0L
                    _state.update { it.copy(me = u) }
                    lastRefresh = System.currentTimeMillis()
                    launch { loadCount() }
                    launch { loadMoney() }
                    launch { loadCounts() }
                    launch { loadNotices() }
                    launch {
                        while (true) {   // প্রতি ৩ মিনিটে নতুন নোটিশ
                            delay(180_000)
                            loadNotices()
                        }
                    }
                }
                is AuthResult.Logout -> _state.update { it.copy(redirect = AuthRedirect.LOGIN) }
                is AuthResult.NotAdmin -> _state.update { it.copy(redirect = AuthRedirect.TASK) }
                is AuthResult.Blocked -> {
                    val o = r.user
                    _state.update {
                        it.copy(
                            blocked = o.str("blocked_reason")?.ifEmpty { null }
                                ?: "অ্যাডমিন আপনার অ্যাকাউন্টটি সাময়িকভাবে বন্ধ করেছেন।",
                            blockedName = o.str("full_name") ?: "-",
                            blockedPhone = o.str("phone") ?: "-"
                        )
                    }
                }
                is AuthResult.NetworkError -> {
                    started = false   // আবার চেষ্টা করা যাবে
                    toast("নেটওয়ার্ক সমস্যা: ${r.message}", true)
                }
            }
        }
    }

    /** অ্যাপে ফিরে এলে (React: visibilitychange/pageshow) */
    fun refreshAll() {
        if (_state.value.me == null) return
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

    fun consumeRedirect() {
        _state.update { it.copy(redirect = null) }
    }

    /* ---------- সম্পন্ন টাস্কের সংখ্যা ---------- */

    private suspend fun loadCount() {
        val u = _state.value.me ?: return
        if (busyCount) return
        busyCount = true
        try {
            var n: Long? = attempt {
                val r = api().rpc("my_proof_count", JSONObject().put("p_user", u.id))
                val v = if (r is JSONArray) (if (r.length() > 0) r.get(0) else null) else r
                (v as? Number)?.toLong()?.takeIf { it >= 0 } ?: throw IllegalStateException("bad")
            }
            if (n == null) {
                n = attempt {
                    var total = 0L
                    var off = 0
                    while (true) {
                        val r = api().get(
                            "page_proofs?user_id=eq.${enc(u.id)}&select=stem&order=stem.asc&limit=1000&offset=$off"
                        )
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

    /* ---------- আয়ের হিসাব ---------- */

    private suspend fun loadMoney() {
        val u = _state.value.me ?: return
        if (busyMoney) return
        busyMoney = true
        try {
            var s = AppSettings()
            attempt {
                val a = api().get("app_settings?id=eq.1&select=page_rate,min_payout,warn_pct,warn_min&limit=1")
                if (a.length() > 0) s = parseSettings(a.getJSONObject(0))
            }
            if (books.isEmpty()) {
                attempt {
                    val arr = api().get("books?select=*&order=created_at.desc")
                    books = (0 until arr.length()).map { parseBook(arr.getJSONObject(it)) }
                    _state.update { it.copy(booksById = books.associateBy { b -> b.id }) }
                }
            }
            val rows = try {
                api().getAll(
                    "page_proofs?user_id=eq.${enc(u.id)}" +
                        "&select=book_id,stem,review_status,review_note,rate,created_at,reviewed_at" +
                        "&order=created_at.desc,book_id.asc,stem.asc"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                api().getAll("page_proofs?user_id=eq.${enc(u.id)}&select=book_id,stem&order=book_id.asc,stem.asc")
            }
            val proofs = rows.map { parseProof(it) }
            val pays: List<Pay> = attempt {
                api().getAll("payments?user_id=eq.${enc(u.id)}&order=created_at.desc").map { parsePay(it) }
            } ?: emptyList()
            val byId = books.associateBy { it.id }
            _state.update { it.copy(sum = summarize(proofs, byId, s, pays)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // নেটওয়ার্ক/টেবিল সমস্যা: আগের তথ্য থাকুক
        } finally {
            busyMoney = false
        }
    }

    /** কিতাব অনুযায়ী সবার মোট অগ্রগতি (বার) */
    private suspend fun loadCounts() {
        attempt {
            val counts = HashMap<String, Int>()
            val rpc = attempt {
                api().rpc("book_proof_counts", JSONObject()) as? JSONArray ?: throw IllegalStateException("bad")
            }
            if (rpc != null) {
                for (i in 0 until rpc.length()) {
                    val o = rpc.getJSONObject(i)
                    val id = o.str("book_id") ?: continue
                    counts[id] = o.optDouble("n", 0.0).toInt()
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
    }

    /* ---------- নোটিশ ---------- */

    private suspend fun loadNotices(): List<Notice> {
        val u = _state.value.me ?: return emptyList()
        val list = attempt {
            val arr = api().get(
                "notices?or=" + enc("(user_id.is.null,user_id.eq.${u.id})") + "&order=created_at.desc&limit=40"
            )
            (0 until arr.length()).map { parseNotice(arr.getJSONObject(it)) }
        } ?: return emptyList()   // notices টেবিল নেই
        allNotices = list
        val seen = readSeen(appContext, u.id)
        val joined = if (createdAtMs > 0) createdAtMs - 86_400_000L else 0L
        val n = list.count { it.id !in seen && it.createdMs >= joined }
        _state.update { it.copy(badge = n) }
        return list
    }

    /** "নোটিশ ও ঘোষণা" বোতাম: সব নোটিশ খোলে (নতুন-পুরোনো সব) */
    fun openNotices() {
        viewModelScope.launch {
            val list = loadNotices()
            if (list.isEmpty()) {
                toast("এখনো কোনো নোটিশ নেই")
                return@launch
            }
            if (_state.value.noticeDialog == null) {
                _state.update { it.copy(noticeDialog = NoticeUi(list.toList(), 0)) }
            }
        }
    }

    fun setNoticeIdx(i: Int) {
        _state.update { st ->
            st.copy(noticeDialog = st.noticeDialog?.let { it.copy(idx = i.coerceIn(0, it.list.size - 1)) })
        }
    }

    /** দেখানো সব নোটিশ "দেখা" হিসেবে মনে রাখি */
    fun closeNotices() {
        val me = _state.value.me
        val shown = _state.value.noticeDialog?.list ?: emptyList()
        if (me == null) {
            _state.update { it.copy(noticeDialog = null) }
            return
        }
        val seen = readSeen(appContext, me.id).toMutableSet()
        shown.forEach { seen.add(it.id) }
        writeSeen(appContext, me.id, seen)
        _state.update { it.copy(noticeDialog = null, badge = allNotices.count { n -> n.id !in seen }) }
    }

    /* ---------- পেমেন্ট নাম্বার পরিবর্তন ---------- */

    fun openEdit() {
        val me = _state.value.me ?: return
        _state.update { it.copy(edit = EditUi(me.paymentMethod ?: "bkash", me.paymentNumber ?: "")) }
    }

    fun closeEdit() {
        _state.update { it.copy(edit = null) }
    }

    fun setEditPm(pm: String) {
        _state.update { st -> st.copy(edit = st.edit?.copy(pm = pm)) }
    }

    fun setEditNum(v: String) {
        _state.update { st -> st.copy(edit = st.edit?.copy(num = v.take(18))) }
    }

    fun saveEdit() {
        val e = _state.value.edit ?: return
        val me = _state.value.me ?: return
        if (e.saving) return
        val v = Phone.validate(e.num)
        if (!v.ok) {
            _state.update { st -> st.copy(edit = st.edit?.copy(err = v.msg)) }
            return
        }
        viewModelScope.launch {
            _state.update { st -> st.copy(edit = st.edit?.copy(saving = true, err = "")) }
            try {
                val body = JSONObject().put("payment_method", e.pm).put("payment_number", v.phone)
                val r = api().patch("app_users?id=eq.${enc(me.id)}", body)
                val nu = if (r.length() > 0) parseUser(r.getJSONObject(0))
                else me.copy(paymentMethod = e.pm, paymentNumber = v.phone)
                _state.update { it.copy(me = nu, edit = null) }
                toast("সংরক্ষিত হয়েছে")
                ActivityLog.log(appContext, "payment_number_changed", (PM_LABEL[e.pm] ?: e.pm) + " " + v.phone)
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                _state.update { st -> st.copy(edit = st.edit?.copy(err = ex.message ?: "সংরক্ষণ করা যায়নি", saving = false)) }
            }
        }
    }

    /* ---------- থিম / লগআউট ---------- */

    fun pickTheme(m: ThemeMode) {
        ThemeStore.set(appContext, m)
        toast("থিম: " + ThemeStore.label(m))
    }

    fun cycleTheme() {
        val n = ThemeStore.next()
        ThemeStore.set(appContext, n)
        toast("থিম: " + ThemeStore.label(n))
    }

    /** লগআউট: লগ লিখে সেশন মুছে (React-এর App.logout) */
    fun logout() {
        ActivityLog.log(appContext, "logout", "")
        Session.clear(appContext)
        _state.update { it.copy(redirect = AuthRedirect.LOGIN) }
    }

    companion object {
        val PM_LABEL = mapOf("bkash" to "বিকাশ", "nagad" to "নগদ")
    }
}

/** তারিখ: React-এর toLocaleDateString('bn-BD') → ২৪/১০/২০২৬ */
fun bnDate(t: String?): String {
    val ms = parseTime(t) ?: return "-"
    val d = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val s = "${d.dayOfMonth}/${d.monthValue}/${d.year}"
    return buildString { for (ch in s) append(if (ch in '0'..'9') "০১২৩৪৫৬৭৮৯"[ch - '0'] else ch) }
}
