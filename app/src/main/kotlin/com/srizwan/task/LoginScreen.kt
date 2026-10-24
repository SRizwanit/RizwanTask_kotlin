package com.srizwan.task

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.srizwan.task.data.Api
import com.srizwan.task.data.Phone
import com.srizwan.task.data.PinHash
import com.srizwan.task.data.UserRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder

@Composable
fun LoginScreen(
    onSuccess: () -> Unit,
    onSignup: () -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.success) {
        if (state.success) {
            delay(200)
            onSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))

            // Brand + logo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(22.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📑",
                        fontSize = 36.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text(
                    text = "কিতাব প্রুফিং",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "মোবাইল নাম্বার দিয়ে লগইন করুন",
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.extra.muted
                )
            }

            Spacer(Modifier.height(22.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Box {
                    Column(Modifier.padding(20.dp)) {
                        when (state.step) {
                            LoginStep.PHONE -> PhoneStep(
                                phone = state.phoneIn,
                                onPhoneChange = { vm.setPhone(it) },
                                onNext = { vm.onPhone(context) },
                                busy = state.busy,
                                err = state.err
                            )

                            LoginStep.OTP -> OtpStep(
                                otp = state.otp,
                                onOtpChange = { vm.setOtp(it) },
                                onVerify = { vm.onOtp(context) },
                                onResend = { vm.onResend(context) },
                                busy = state.busy,
                                err = state.err,
                                leftSec = state.leftSec,
                                onChange = { vm.reset() }
                            )

                            LoginStep.PIN -> PinStep(
                                mode = state.mode,
                                pin = state.pin,
                                pin2 = state.pin2,
                                onPinChange = { vm.setPin(it) },
                                onPin2Change = { vm.setPin2(it) },
                                onSubmit = { vm.onPin(context, onSignup) },
                                busy = state.busy,
                                err = state.err,
                                phone = state.phoneNorm,
                                onChange = { vm.reset() }
                            )

                            LoginStep.DEVICE -> DeviceStep(
                                reason = state.blockReason,
                                phone = state.phoneNorm,
                                busy = state.busy,
                                err = state.err,
                                onRetry = { vm.onRetry(context) },
                                onWhatsapp = { url -> vm.openWhatsapp(context, url) }
                            )
                        }
                    }

                    if (state.busy) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (state.step == LoginStep.OTP || state.step == LoginStep.PIN || state.step == LoginStep.DEVICE) {
                Spacer(Modifier.height(10.dp))
                TextButton(onClick = { vm.reset() }) {
                    Text(
                        "অন্য নাম্বার ব্যবহার করবেন?",
                        color = AppColors.extra.muted,
                        fontSize = 12.5.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "নতুন ইউজার? OTP ভেরিফাই করার পর প্রোফাইল তৈরি করুন",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted,
                textAlign = TextAlign.Center
            )
        }
    }

    if (state.showOtpDialog) {
        OtpDialog(code = state.otpCode, onDismiss = { vm.dismissOtpDialog() })
    }
}

@Composable
private fun PhoneStep(
    phone: String,
    onPhoneChange: (String) -> Unit,
    onNext: () -> Unit,
    busy: Boolean,
    err: String
) {
    val focusReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusReq.requestFocus() }

    Column {
        Text(
            text = "ফোন নাম্বার",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.extra.muted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusReq),
            placeholder = { Text("017XXXXXXXX") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onNext() }),
            singleLine = true
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text("পরবর্তী")
        }
        ErrorText(err)
    }
}

@Composable
private fun OtpStep(
    otp: String,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    busy: Boolean,
    err: String,
    leftSec: Int,
    onChange: () -> Unit
) {
    val focusReq = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusReq.requestFocus() }

    Column {
        Text(
            text = "৬ ডিজিটের OTP কোড দিন",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.extra.muted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) onOtpChange(it.filter { c -> c.isDigit() }) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusReq),
            placeholder = { Text("------") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onVerify() }),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onVerify,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text("ভেরিফাই করুন")
        }
        ErrorText(err)
        TextButton(
            onClick = onResend,
            enabled = leftSec <= 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (leftSec > 0) "OTP আবার পাঠান (${mmss(leftSec)})" else "OTP আবার পাঠান",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.5.sp
            )
        }
    }
}

@Composable
private fun PinStep(
    mode: LoginMode,
    pin: String,
    pin2: String,
    onPinChange: (String) -> Unit,
    onPin2Change: (String) -> Unit,
    onSubmit: () -> Unit,
    busy: Boolean,
    err: String,
    phone: String,
    onChange: () -> Unit
) {
    val focusReq = remember { FocusRequester() }
    LaunchedEffect(mode) { focusReq.requestFocus() }
    val setup = mode != LoginMode.LOGIN

    val label = when (mode) {
        LoginMode.LOGIN -> "৪ ডিজিটের PIN দিন"
        LoginMode.SETUP_EXISTING -> "আপনার অ্যাকাউন্টের জন্য নতুন ৪ ডিজিটের PIN সেট করুন"
        LoginMode.SETUP_NEW -> "আপনার নতুন একাউন্টের জন্য ৪ ডিজিটের PIN সেট করুন"
    }
    val btnText = when (mode) {
        LoginMode.LOGIN -> "লগইন করুন"
        LoginMode.SETUP_EXISTING -> "PIN সেট করুন"
        LoginMode.SETUP_NEW -> "পরবর্তী ধাপ"
    }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.extra.muted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 4) onPinChange(it.filter { c -> c.isDigit() }) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusReq),
            placeholder = { Text("----") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textAlign = TextAlign.Center,
                letterSpacing = 6.sp,
                fontWeight = FontWeight.Bold
            )
        )

        AnimatedVisibility(setup, enter = fadeIn(), exit = fadeOut()) {
            Column {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "PIN আবার দিন",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = AppColors.extra.muted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = pin2,
                    onValueChange = { if (it.length <= 4) onPin2Change(it.filter { c -> c.isDigit() }) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("----") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 6.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text(btnText)
        }
        ErrorText(err)

        if (mode == LoginMode.LOGIN) {
            TextButton(
                onClick = {
                    onChange()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "PIN ভুলে গেছেন? অ্যাডমিনকে WhatsApp করুন",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DeviceStep(
    reason: BlockReason,
    phone: String,
    busy: Boolean,
    err: String,
    onRetry: () -> Unit,
    onWhatsapp: (String) -> Unit
) {
    val msg = when (reason) {
        BlockReason.BOUND -> "এই একাউন্টটি অন্য একটি ডিভাইসে যুক্ত আছে। অ্যাডমিন ডিভাইস রিসেট করে দিলে নিচের 'আবার চেষ্টা করুন' বাটনে চাপ দিন।"
        BlockReason.USED -> "এই মোবাইলে ইতিমধ্যে অন্য একটি একাউন্ট ব্যবহার করা হচ্ছে। একটি ডিভাইসে একটি মাত্র একাউন্ট চালানো যায়।"
    }
    val waMsg = "আসসালামু আলাইকুম, আমি কিতাব প্রুফিং অ্যাপে অন্য একটি ডিভাইস ব্যবহার করতে চাই। আমার ফোন নাম্বার: $phone। দয়া করে আমার একাউন্টের ডিভাইস রিসেট করে দিন।"

    Column {
        Text(
            text = msg,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text("আবার চেষ্টা করুন")
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = { onWhatsapp(waMsg) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !busy
        ) {
            Text("অ্যাডমিনকে WhatsApp এ জানান")
        }
        ErrorText(err)
    }
}

@Composable
private fun ErrorText(err: String) {
    if (err.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = err,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OtpDialog(code: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "আপনার OTP কোড",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = code,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 6.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "২ মিনিটের মধ্যে কোডটি লিখুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.extra.muted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("ঠিক আছে")
                }
            }
        }
    }
}

private fun mmss(s: Int): String {
    val m = s / 60
    val sec = s % 60
    return "%02d:%02d".format(m, sec)
}

// ViewModel
enum class LoginStep { PHONE, OTP, PIN, DEVICE }
enum class LoginMode { LOGIN, SETUP_EXISTING, SETUP_NEW }
enum class BlockReason { BOUND, USED }

data class LoginState(
    val step: LoginStep = LoginStep.PHONE,
    val mode: LoginMode = LoginMode.LOGIN,
    val blockReason: BlockReason = BlockReason.BOUND,
    val phoneIn: String = "",
    val phoneNorm: String = "",
    val otp: String = "",
    val pin: String = "",
    val pin2: String = "",
    val otpCode: String = "",
    val showOtpDialog: Boolean = false,
    val leftSec: Int = 0,
    val busy: Boolean = false,
    val err: String = "",
    val success: Boolean = false
)

class LoginViewModel : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state = _state.asStateFlow()

    private var user: JSONObject? = null
    private var timerJob: Job? = null

    fun setPhone(v: String) {
        _state.value = _state.value.copy(phoneIn = v, err = "")
    }

    fun setOtp(v: String) {
        _state.value = _state.value.copy(otp = v, err = "")
    }

    fun setPin(v: String) {
        _state.value = _state.value.copy(pin = v, err = "")
    }

    fun setPin2(v: String) {
        _state.value = _state.value.copy(pin2 = v, err = "")
    }

    fun reset() {
        timerJob?.cancel()
        user = null
        _state.value = LoginState()
    }

    fun onPhone(context: Context) {
        val st = _state.value
        val v = Phone.validate(st.phoneIn)
        if (!v.ok) {
            _state.value = st.copy(err = v.msg)
            return
        }
        _state.value = st.copy(phoneNorm = v.phone, busy = true, err = "")

        viewModelScope.launch {
            try {
                val repo = UserRepo(Api(context))
                user = repo.findByPhone(v.phone)
                val u = user
                if (u != null) {
                    if (u.optBoolean("is_blocked", false) && !Phone.isAdminPhone(u.optString("phone"))) {
                        val reason = u.optString("blocked_reason", "")
                        val msg = if (reason.isNotEmpty()) "আপনার অ্যাকাউন্ট ব্লক করা হয়েছে: $reason। অ্যাডমিনের সাথে যোগাযোগ করুন।"
                        else "আপনার অ্যাকাউন্ট ব্লক করা হয়েছে। অ্যাডমিনের সাথে যোগাযোগ করুন।"
                        _state.value = _state.value.copy(busy = false, err = msg)
                        return@launch
                    }
                    afterUserKnown(context, u)
                } else {
                    startOtp(context, v.phone)
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, err = e.message ?: "Error")
            }
        }
    }

    private suspend fun afterUserKnown(context: Context, u: JSONObject) {
        val dev = Session.deviceId(context)
        val stored = u.optString("device_id", "")
        if (!Session.deviceEmpty(stored) && stored != dev) {
            _state.value = _state.value.copy(
                step = LoginStep.DEVICE,
                blockReason = BlockReason.BOUND,
                busy = false
            )
        } else {
            val mode = if (PinHash.hasPin(u)) LoginMode.LOGIN else LoginMode.SETUP_EXISTING
            _state.value = _state.value.copy(
                step = LoginStep.PIN,
                mode = mode,
                pin = "",
                pin2 = "",
                busy = false
            )
        }
    }

    private suspend fun startOtp(context: Context, phone: String) {
        val repo = UserRepo(Api(context))
        val code = repo.requestOtp(phone)
        _state.value = _state.value.copy(
            step = LoginStep.OTP,
            otp = "",
            otpCode = code,
            showOtpDialog = true,
            busy = false
        )
        startResend()
    }

    fun dismissOtpDialog() {
        _state.value = _state.value.copy(showOtpDialog = false)
    }

    private fun startResend() {
        timerJob?.cancel()
        _state.value = _state.value.copy(leftSec = 120)
        timerJob = viewModelScope.launch {
            for (i in 119 downTo 0) {
                delay(1000)
                _state.value = _state.value.copy(leftSec = i)
            }
        }
    }

    fun onResend(context: Context) {
        val st = _state.value
        _state.value = st.copy(busy = true, err = "")
        viewModelScope.launch {
            try {
                val repo = UserRepo(Api(context))
                val code = repo.requestOtp(st.phoneNorm)
                _state.value = _state.value.copy(
                    otpCode = code,
                    showOtpDialog = true,
                    busy = false
                )
                startResend()
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, err = e.message ?: "Error")
            }
        }
    }

    fun onOtp(context: Context) {
        val st = _state.value
        val c = st.otp.trim()
        if (c.length != 6) {
            _state.value = st.copy(err = "৬ ডিজিটের কোড দিন")
            return
        }
        _state.value = st.copy(busy = true, err = "")

        viewModelScope.launch {
            try {
                val repo = UserRepo(Api(context))
                if (!repo.verifyOtp(st.phoneNorm, c)) {
                    _state.value = _state.value.copy(busy = false, err = "দয়া করে সঠিক ওটিপি লিখুন")
                    return@launch
                }
                val devUser = repo.findByDevice(Session.deviceId(context))
                if (devUser != null) {
                    _state.value = _state.value.copy(
                        step = LoginStep.DEVICE,
                        blockReason = BlockReason.USED,
                        busy = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        step = LoginStep.PIN,
                        mode = LoginMode.SETUP_NEW,
                        pin = "",
                        pin2 = "",
                        busy = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, err = e.message ?: "Error")
            }
        }
    }

    fun onPin(context: Context, onSignup: () -> Unit) {
        val st = _state.value
        if (st.pin.length != 4) {
            _state.value = st.copy(err = "৪ ডিজিটের PIN দিন")
            return
        }
        if (st.mode != LoginMode.LOGIN && st.pin != st.pin2) {
            _state.value = st.copy(err = "দুটি PIN মিলছে না")
            return
        }
        _state.value = st.copy(busy = true, err = "")

        viewModelScope.launch {
            try {
                val api = Api(context)
                val repo = UserRepo(api)
                val dev = Session.deviceId(context)
                val u = user

                when (st.mode) {
                    LoginMode.LOGIN -> {
                        if (u == null) {
                            _state.value = _state.value.copy(busy = false, err = "একাউন্ট খুঁজে পাওয়া যায়নি")
                            return@launch
                        }
                        if (!PinHash.verify(u, st.pin)) {
                            _state.value = _state.value.copy(busy = false, pin = "", err = "PIN সঠিক নয়")
                            return@launch
                        }
                        if (Session.deviceEmpty(u.optString("device_id", ""))) {
                            val existing = repo.findByDevice(dev, u.optInt("id"))
                            if (existing != null) {
                                _state.value = _state.value.copy(
                                    step = LoginStep.DEVICE,
                                    blockReason = BlockReason.USED,
                                    busy = false
                                )
                                return@launch
                            }
                            api.patch(
                                "app_users?id=eq.${u.getInt("id")}",
                                JSONObject().put("device_id", dev)
                            )
                            u.put("device_id", dev)
                        }
                        finish(context, u)
                    }

                    LoginMode.SETUP_EXISTING -> {
                        if (u == null) {
                            _state.value = _state.value.copy(busy = false, err = "একাউন্ট খুঁজে পাওয়া যায়নি")
                            return@launch
                        }
                        val existing = repo.findByDevice(dev, u.optInt("id"))
                        if (existing != null) {
                            _state.value = _state.value.copy(
                                step = LoginStep.DEVICE,
                                blockReason = BlockReason.USED,
                                busy = false
                            )
                            return@launch
                        }
                        val salt = PinHash.newSalt()
                        val hash = PinHash.hash(st.pin, salt)
                        api.patch(
                            "app_users?id=eq.${u.getInt("id")}",
                            JSONObject()
                                .put("password_salt", salt)
                                .put("password_hash", hash)
                                .put("device_id", dev)
                        )
                        u.put("device_id", dev)
                        finish(context, u)
                    }

                    LoginMode.SETUP_NEW -> {
                        context.getSharedPreferences("kp_signup", Context.MODE_PRIVATE).edit()
                            .putString(
                                "kp_signup",
                                JSONObject()
                                    .put("phone", st.phoneNorm)
                                    .put("pin", st.pin)
                                    .toString()
                            )
                            .apply()
                        _state.value = _state.value.copy(busy = false)
                        onSignup()
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, err = e.message ?: "Error")
            }
        }
    }

    private fun finish(context: Context, u: JSONObject) {
        Session.set(context, u)
        _state.value = _state.value.copy(busy = false, success = true)
    }

    fun onRetry(context: Context) {
        val st = _state.value
        _state.value = st.copy(busy = true, err = "")

        viewModelScope.launch {
            try {
                val repo = UserRepo(Api(context))
                if (st.blockReason == BlockReason.BOUND) {
                    val u = repo.findByPhone(st.phoneNorm)
                    if (u == null) {
                        _state.value = _state.value.copy(busy = false, err = "একাউন্ট খুঁজে পাওয়া যায়নি")
                        return@launch
                    }
                    if (u.optBoolean("is_blocked", false) && !Phone.isAdminPhone(u.optString("phone"))) {
                        _state.value =
                            _state.value.copy(busy = false, err = "আপনার অ্যাকাউন্ট ব্লক করা হয়েছে")
                        return@launch
                    }
                    user = u
                    val stored = u.optString("device_id", "")
                    val dev = Session.deviceId(context)
                    if (Session.deviceEmpty(stored) || stored == dev) {
                        val mode = if (PinHash.hasPin(u)) LoginMode.LOGIN else LoginMode.SETUP_EXISTING
                        _state.value = _state.value.copy(
                            step = LoginStep.PIN,
                            mode = mode,
                            pin = "",
                            pin2 = "",
                            busy = false
                        )
                    } else {
                        _state.value = _state.value.copy(
                            busy = false,
                            err = "এখনো অ্যাডমিন ডিভাইস রিসেট করেননি, একটু পর আবার চেষ্টা করুন"
                        )
                    }
                } else {
                    val existing = repo.findByDevice(Session.deviceId(context))
                    if (existing != null) {
                        _state.value =
                            _state.value.copy(busy = false, err = "এই ডিভাইসে এখনো অন্য একাউন্ট বাঁধা আছে")
                    } else {
                        val u = user
                        if (u != null) {
                            val mode = if (PinHash.hasPin(u)) LoginMode.LOGIN else LoginMode.SETUP_EXISTING
                            _state.value = _state.value.copy(
                                step = LoginStep.PIN,
                                mode = mode,
                                pin = "",
                                pin2 = "",
                                busy = false
                            )
                        } else {
                            _state.value = _state.value.copy(
                                step = LoginStep.PIN,
                                mode = LoginMode.SETUP_NEW,
                                pin = "",
                                pin2 = "",
                                busy = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(busy = false, err = e.message ?: "Error")
            }
        }
    }

    fun openWhatsapp(context: Context, msg: String) {
        val url = "https://wa.me/${BuildConfig.ADMIN_WA}?text=${URLEncoder.encode(msg, "UTF-8")}"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            _state.value = _state.value.copy(err = "WhatsApp খোলা যায়নি")
        }
    }
}
