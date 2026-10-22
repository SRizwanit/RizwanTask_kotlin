package com.srizwan.task

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.net.URLEncoder

private fun mmss(s: Int) = "%02d:%02d".format(s / 60, s % 60)

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onSignup: (phone: String, pin: String) -> Unit,
    vm: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val extra = AppColors.extra
    val cs = MaterialTheme.colorScheme

    fun wa(msg: String) {
        val url = "https://wa.me/${BuildConfig.ADMIN_WA}?text=" + URLEncoder.encode(msg, "UTF-8").replace("+", "%20")
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    LaunchedEffect(vm.event) {
        when (val e = vm.event) {
            LoginEvent.GoTask -> { vm.consumeEvent(); onLoggedIn() }
            is LoginEvent.GoSignup -> { vm.consumeEvent(); onSignup(e.phone, e.pin) }
            is LoginEvent.Toast -> { Toast.makeText(context, e.msg, Toast.LENGTH_SHORT).show(); vm.consumeEvent() }
            null -> Unit
        }
    }

    val phoneFocus = remember { FocusRequester() }
    val otpFocus = remember { FocusRequester() }
    val pinFocus = remember { FocusRequester() }
    LaunchedEffect(vm.focusTick, vm.mode) {
        if (vm.focusTick == 0) return@LaunchedEffect
        runCatching {
            when (vm.step) {
                LoginStep.PHONE -> phoneFocus.requestFocus()
                LoginStep.OTP -> otpFocus.requestFocus()
                LoginStep.PIN -> pinFocus.requestFocus()
                LoginStep.DEV -> Unit
            }
        }
    }

    val setup = vm.mode != PinMode.LOGIN
    val pinLabel = when (vm.mode) {
        PinMode.LOGIN -> "৪ ডিজিটের PIN দিন"
        PinMode.SETUP_EXISTING -> "আপনার অ্যাকাউন্টের জন্য নতুন ৪ ডিজিটের PIN সেট করুন"
        PinMode.SETUP_NEW -> "আপনার নতুন একাউন্টের জন্য ৪ ডিজিটের PIN সেট করুন"
    }
    val pinBtn = when (vm.mode) {
        PinMode.LOGIN -> "লগইন করুন"
        PinMode.SETUP_EXISTING -> "PIN সেট করুন"
        PinMode.SETUP_NEW -> "পরবর্তী ধাপ"
    }
    val devMsg = if (vm.blockReason == BlockReason.BOUND)
        "এই একাউন্টটি অন্য একটি ডিভাইসে যুক্ত আছে। অ্যাডমিন ডিভাইস রিসেট করে দিলে নিচের \u2018আবার চেষ্টা করুন\u2019 বাটনে চাপ দিন।"
    else "এই মোবাইলে ইতিমধ্যে অন্য একটি একাউন্ট ব্যবহার করা হচ্ছে। একটি ডিভাইসে একটি মাত্র একাউন্ট চালানো যায়।"

    Box(
        Modifier
            .fillMaxSize()
            .background(extra.canvas)
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .widthIn(max = 420.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // .brand
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0A4F40), Color(0xFF12896D)))),
                contentAlignment = Alignment.Center
            ) {
                Image(painterResource(R.drawable.rizwantask), contentDescription = null, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text("কিতাব প্রুফিং", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = cs.onBackground)
            Spacer(Modifier.height(8.dp))
            Text("মোবাইল নাম্বার দিয়ে লগইন করুন", fontSize = 13.sp, color = extra.muted)
            Spacer(Modifier.height(22.dp))

            // .card
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black.copy(alpha = .06f))
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.background)
                    .border(1.dp, extra.line, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                when (vm.step) {
                    LoginStep.PHONE -> {
                        FieldLabel("ফোন নাম্বার")
                        OutlinedTextField(
                            value = vm.phoneIn,
                            onValueChange = vm::setPhoneInput,
                            placeholder = { Text("017XXXXXXXX") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onAny = { vm.onPhone() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(phoneFocus)
                        )
                        PrimaryButton("পরবর্তী", !vm.busy, Modifier.padding(top = 16.dp)) { vm.onPhone() }
                    }
                    LoginStep.OTP -> {
                        FieldLabel("৬ ডিজিটের OTP কোড দিন")
                        PinField(vm.otp, vm::setOtpInput, "------", false, otpFocus) { vm.onOtp() }
                        PrimaryButton("ভেরিফাই করুন", !vm.busy, Modifier.padding(top = 16.dp)) { vm.onOtp() }
                        LinkButton(
                            if (vm.left > 0) "OTP আবার পাঠান (${mmss(vm.left)})" else "OTP আবার পাঠান",
                            enabled = vm.left <= 0
                        ) { vm.onResend() }
                    }
                    LoginStep.PIN -> {
                        FieldLabel(pinLabel)
                        PinField(vm.pin, vm::setPinInput, "----", true, pinFocus) { vm.onPin() }
                        if (setup) {
                            Spacer(Modifier.height(12.dp))
                            FieldLabel("PIN আবার দিন")
                            PinField(vm.pin2, vm::setPin2Input, "----", true, null) { vm.onPin() }
                        }
                        PrimaryButton(pinBtn, !vm.busy, Modifier.padding(top = 16.dp)) { vm.onPin() }
                        if (!setup) {
                            LinkButton("PIN ভুলে গেছেন? অ্যাডমিনকে WhatsApp করুন") {
                                wa("আসসালামু আলাইকুম, আমি কিতাব প্রুফিং অ্যাপে আমার PIN ভুলে গিয়েছি। আমার ফোন নাম্বার: ${vm.phone}। দয়া করে সাহায্য করুন।")
                            }
                        }
                    }
                    LoginStep.DEV -> {
                        Text(devMsg, textAlign = TextAlign.Center, lineHeight = 1.7.em, color = cs.onBackground, modifier = Modifier.fillMaxWidth())
                        OutlinedButton(
                            onClick = vm::onRetry, enabled = !vm.busy,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(48.dp)
                        ) { Text("আবার চেষ্টা করুন", fontWeight = FontWeight.SemiBold) }
                        PrimaryButton("অ্যাডমিনকে WhatsApp এ জানান", !vm.busy, Modifier.padding(top = 10.dp)) {
                            wa("আসসালামু আলাইকুম, আমি কিতাব প্রুফিং অ্যাপে অন্য একটি ডিভাইস ব্যবহার করতে চাই। আমার ফোন নাম্বার: ${vm.phone}। দয়া করে আমার একাউন্টের ডিভাইস রিসেট করে দিন।")
                        }
                    }
                }

                if (vm.busy) {
                    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = cs.primary)
                    }
                }
                Text(
                    vm.err, color = cs.error, fontSize = 12.5.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 18.dp)
                )
                if (vm.step != LoginStep.PHONE) {
                    LinkButton("অন্য নাম্বার ব্যবহার করবেন?", muted = true) { vm.onChange() }
                }
            }

            Text(
                "নতুন ইউজার? OTP ভেরিফাই করার পর প্রোফাইল তৈরি করুন",
                fontSize = 12.5.sp, color = extra.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
        }
    }

    if (vm.showOtpDialog) {
        AlertDialog(
            onDismissRequest = { vm.showOtpDialog = false },
            title = { Text("আপনার OTP কোড") },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        vm.otpCode, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = cs.primary,
                        letterSpacing = 0.6.em, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                    Text("২ মিনিটের মধ্যে কোডটি লিখুন", fontSize = 12.5.sp, color = extra.muted, textAlign = TextAlign.Center)
                }
            },
            confirmButton = { Button(onClick = { vm.showOtpDialog = false }) { Text("ঠিক আছে") } }
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun PinField(
    value: String, onChange: (String) -> Unit, placeholder: String, password: Boolean,
    focus: FocusRequester?, onDone: () -> Unit
) {
    val style = TextStyle(textAlign = TextAlign.Center, letterSpacing = 0.6.em, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        placeholder = { Text(placeholder, style = style, modifier = Modifier.fillMaxWidth()) },
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.NumberPassword else KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onAny = { onDone() }),
        modifier = Modifier.fillMaxWidth().let { if (focus != null) it.focusRequester(focus) else it }
    )
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Button(
        onClick = onClick, enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        modifier = modifier.fillMaxWidth().height(48.dp)
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(cs.primary, AppColors.extra.primary2)), RoundedCornerShape(12.dp))
                .let { if (!enabled) it.background(cs.background.copy(alpha = .4f), RoundedCornerShape(12.dp)) else it },
            contentAlignment = Alignment.Center
        ) { Text(text, color = cs.onPrimary, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun LinkButton(text: String, enabled: Boolean = true, muted: Boolean = false, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text, fontSize = 12.5.sp, textAlign = TextAlign.Center,
            fontWeight = if (muted) FontWeight.Normal else FontWeight.SemiBold,
            color = if (muted) AppColors.extra.muted else MaterialTheme.colorScheme.primary
        )
    }
}
