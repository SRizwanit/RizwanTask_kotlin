package com.srizwan.task

import android.app.Application
import android.content.Context
import android.os.CountDownTimer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.srizwan.task.data.ActivityLog
import com.srizwan.task.data.AppUser
import com.srizwan.task.data.Phone
import com.srizwan.task.data.PinHash
import com.srizwan.task.data.UserRepo
import com.srizwan.task.data.deviceEmpty
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class LoginStep { PHONE, OTP, PIN, DEV }
enum class PinMode { LOGIN, SETUP_EXISTING, SETUP_NEW }
enum class BlockReason { BOUND, USED }

sealed interface LoginEvent {
    data object GoTask : LoginEvent
    data class GoSignup(val phone: String, val pin: String) : LoginEvent
    data class Toast(val msg: String) : LoginEvent
}

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context get() = getApplication()

    var step by mutableStateOf(LoginStep.PHONE); private set
    var phoneIn by mutableStateOf("")
    var otp by mutableStateOf("")
    var pin by mutableStateOf("")
    var pin2 by mutableStateOf("")
    var mode by mutableStateOf(PinMode.LOGIN); private set
    var blockReason by mutableStateOf(BlockReason.BOUND); private set
    var err by mutableStateOf(""); private set
    var busy by mutableStateOf(false); private set
    var otpCode by mutableStateOf(""); private set
    var showOtpDialog by mutableStateOf(false)
    var left by mutableIntStateOf(0); private set
    var event by mutableStateOf<LoginEvent?>(null); private set
    /** ধাপ বদলালে ফোকাসের সংকেত (প্রথম লোডে ০ = ফোকাস নয়) */
    var focusTick by mutableIntStateOf(0); private set

    var phone = ""; private set
    private var user: AppUser? = null
    private var timer: CountDownTimer? = null

    init {
        // এই ডিভাইসে আগেই লগইন থাকলে সরাসরি টাস্কে (requireAuth-এর মতো যাচাই)
        Session.get(ctx)?.let { s ->
            viewModelScope.launch {
                runCatching {
                    val u = UserRepo.findUserById(s.id)
                    if (u == null || deviceEmpty(u.deviceId) || u.deviceId != Session.deviceId(ctx)) Session.clear(ctx)
                    else event = LoginEvent.GoTask
                }
            }
        }
    }

    fun consumeEvent() { event = null }
    fun setOtpInput(v: String) { otp = v.filter(Char::isDigit).take(6) }
    fun setPinInput(v: String) { pin = v.filter(Char::isDigit).take(4) }
    fun setPin2Input(v: String) { pin2 = v.filter(Char::isDigit).take(4) }
    fun setPhoneInput(v: String) { phoneIn = v.take(18) }

    private fun show(s: LoginStep) { step = s; err = ""; focusTick++ }
    private fun pinStep(m: PinMode) { mode = m; pin = ""; pin2 = ""; show(LoginStep.PIN) }
    private fun devStep(r: BlockReason) { blockReason = r; show(LoginStep.DEV) }

    private fun afterUserKnown(u: AppUser) {
        val dev = Session.deviceId(ctx)
        if (!deviceEmpty(u.deviceId) && u.deviceId != dev) return devStep(BlockReason.BOUND)
        pinStep(if (PinHash.hasPin(u)) PinMode.LOGIN else PinMode.SETUP_EXISTING)
    }

    private fun startResend() {
        timer?.cancel()
        left = 120
        timer = object : CountDownTimer(120_000, 1_000) {
            override fun onTick(ms: Long) { left = ((ms + 999) / 1000).toInt().coerceAtLeast(0) }
            override fun onFinish() { left = 0 }
        }.start()
    }

    private suspend fun startOtp() {
        val code = UserRepo.requestOtp(phone)
        show(LoginStep.OTP); otp = ""; otpCode = code; showOtpDialog = true; startResend()
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            try { block() } catch (e: Exception) { err = e.message ?: "" } finally { busy = false }
        }
    }

    fun onPhone() {
        val v = Phone.validate(phoneIn)
        if (!v.ok) { err = v.msg; return }
        phone = v.phone; err = ""
        run {
            user = UserRepo.findUserByPhone(phone)
            val u = user
            if (u != null && u.isBlocked && !Phone.isAdminPhone(u.phone)) {
                err = "আপনার অ্যাকাউন্ট ব্লক করা হয়েছে" + (if (!u.blockedReason.isNullOrEmpty()) ": " + u.blockedReason else "") + "। অ্যাডমিনের সাথে যোগাযোগ করুন।"
                return@run
            }
            if (u != null) afterUserKnown(u) else startOtp()
        }
    }

    fun onResend() = run { startOtp() }

    fun onOtp() {
        val c = otp.trim()
        if (c.length != 6) { err = "৬ ডিজিটের কোড দিন"; return }
        err = ""
        run {
            if (!UserRepo.verifyOtp(phone, c)) { err = "দয়া করে সঠিক ওটিপি লিখুন"; return@run }
            if (UserRepo.findUserByDevice(Session.deviceId(ctx)) != null) return@run devStep(BlockReason.USED)
            pinStep(PinMode.SETUP_NEW)
        }
    }

    private suspend fun finish(u: AppUser) {
        Session.set(ctx, u)
        ActivityLog.log("login", "", u.id, u.fullName)
        event = LoginEvent.GoTask
    }

    fun onPin() {
        if (pin.length != 4) { err = "৪ ডিজিটের PIN দিন"; return }
        if (mode != PinMode.LOGIN && pin != pin2) { err = "দুটি PIN মিলছে না"; return }
        err = ""
        val dev = Session.deviceId(ctx)
        run {
            when (mode) {
                PinMode.LOGIN -> {
                    val u = user ?: return@run
                    if (!PinHash.verifyPin(u, pin)) { pin = ""; err = "PIN সঠিক নয়"; return@run }
                    if (deviceEmpty(u.deviceId)) {
                        if (UserRepo.findUserByDevice(dev, u.id) != null) return@run devStep(BlockReason.USED)
                        UserRepo.patchUser(u.id, JSONObject().put("device_id", dev))
                        u.deviceId = dev
                    }
                    finish(u)
                }
                PinMode.SETUP_EXISTING -> {
                    val u = user ?: return@run
                    if (UserRepo.findUserByDevice(dev, u.id) != null) return@run devStep(BlockReason.USED)
                    val salt = PinHash.newSalt()
                    val hash = PinHash.hashPin(pin, salt)
                    UserRepo.patchUser(u.id, JSONObject().put("password_salt", salt).put("password_hash", hash).put("device_id", dev))
                    u.deviceId = dev
                    finish(u)
                }
                PinMode.SETUP_NEW -> event = LoginEvent.GoSignup(phone, pin)
            }
        }
    }

    fun onRetry() {
        err = ""
        run {
            if (blockReason == BlockReason.BOUND) {
                val u = UserRepo.findUserByPhone(phone).also { user = it }
                if (u == null) { err = "একাউন্ট খুঁজে পাওয়া যায়নি"; return@run }
                if (u.isBlocked && !Phone.isAdminPhone(u.phone)) { err = "আপনার অ্যাকাউন্ট ব্লক করা হয়েছে"; return@run }
                val dev = Session.deviceId(ctx)
                if (deviceEmpty(u.deviceId) || u.deviceId == dev) {
                    event = LoginEvent.Toast("ডিভাইস মিলেছে, এখন লগইন করুন")
                    pinStep(if (PinHash.hasPin(u)) PinMode.LOGIN else PinMode.SETUP_EXISTING)
                } else err = "এখনো অ্যাডমিন ডিভাইস রিসেট করেননি, একটু পর আবার চেষ্টা করুন"
            } else if (UserRepo.findUserByDevice(Session.deviceId(ctx)) != null) {
                err = "এই ডিভাইসে এখনো অন্য একাউন্ট বাঁধা আছে"
            } else {
                event = LoginEvent.Toast("ডিভাইস এখন খালি আছে")
                val u = user
                if (u != null) pinStep(if (PinHash.hasPin(u)) PinMode.LOGIN else PinMode.SETUP_EXISTING)
                else pinStep(PinMode.SETUP_NEW)
            }
        }
    }

    fun onChange() {
        timer?.cancel(); left = 0; phone = ""; user = null; phoneIn = ""; show(LoginStep.PHONE)
    }

    override fun onCleared() { timer?.cancel() }
}
