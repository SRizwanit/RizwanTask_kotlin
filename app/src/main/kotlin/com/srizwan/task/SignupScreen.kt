package com.srizwan.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.srizwan.task.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun SignupScreen(onSuccess: () -> Unit) {
    val vm: SignupViewModel = viewModel()
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        if (!vm.loadSignupContext(ctx)) {
            // সংরক্ষিত কনটেক্সট নেই, Login-এ ফিরে যাওয়া উচিত — কিন্তু আমরা এখানে nav পাস করিনি
            // তাই শুধু error দেখাই
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
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // লোগো এলাকা
            LogoMark(sizeDp = 56.dp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "প্রোফাইল তৈরি করুন",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "কাজ শুরু করতে আপনার তথ্য দিন",
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.extra.muted
            )
            Spacer(Modifier.height(8.dp))

            if (state.phone.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppColors.extra.ok.copy(alpha = 0.12f),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    Text(
                        text = "যাচাইকৃত নাম্বার: ${state.phone}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.extra.ok,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // কার্ড
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "ইউজার ডিটেইলস",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))

                    // নাম
                    Text(
                        text = "পূর্ণ নাম *",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = { vm.setName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("আপনার নাম") },
                        singleLine = true,
                        enabled = !state.busy,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // পেমেন্ট মাধ্যম
                    Text(
                        text = "পেমেন্ট মাধ্যম *",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        horizontalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        PaymentMethodButton(
                            label = "বিকাশ",
                            selected = state.pm == "bkash",
                            onClick = { vm.setPm("bkash") },
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f)
                        )
                        PaymentMethodButton(
                            label = "নগদ",
                            selected = state.pm == "nagad",
                            onClick = { vm.setPm("nagad") },
                            enabled = !state.busy,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // পেমেন্ট নাম্বার
                    Text(
                        text = "${if (state.pm == "bkash") "বিকাশ" else "নগদ"} নাম্বার *",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = state.pnum,
                        onValueChange = { vm.setPnum(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("01XXXXXXXXX") },
                        singleLine = true,
                        enabled = !state.busy,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(Modifier.height(22.dp))

                    // সাবমিট বোতাম
                    Button(
                        onClick = { vm.submit(ctx, onSuccess) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (state.busy) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = "প্রোফাইল সাবমিট করুন",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    if (state.err.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = state.err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "আপনার তথ্য নিরাপদে সংরক্ষিত থাকবে",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.extra.muted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PaymentMethodButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                enabled = enabled,
                role = Role.RadioButton
            )
            .background(bgColor)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = textColor
        )
    }
}

@Composable
private fun LogoMark(sizeDp: Dp) {
    Box(
        modifier = Modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "📑",
            fontSize = (sizeDp.value * 0.7f).sp
        )
    }
}

data class SignupState(
    val phone: String = "",
    val pin: String = "",
    val name: String = "",
    val pm: String = "bkash",
    val pnum: String = "",
    val err: String = "",
    val busy: Boolean = false
)

class SignupViewModel : ViewModel() {
    private val _state = MutableStateFlow(SignupState())
    val state: StateFlow<SignupState> = _state

    fun loadSignupContext(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences("kp_temp", android.content.Context.MODE_PRIVATE)
        val json = prefs.getString("kp_signup", null)
        if (json.isNullOrBlank()) {
            _state.value = _state.value.copy(err = "সাইনআপ তথ্য নেই — লগইন থেকে আবার শুরু করুন")
            return false
        }
        try {
            val obj = JSONObject(json)
            _state.value = _state.value.copy(
                phone = obj.optString("phone", ""),
                pin = obj.optString("pin", "")
            )
            return true
        } catch (e: Exception) {
            _state.value = _state.value.copy(err = "সাইনআপ তথ্য পড়া যায়নি")
            return false
        }
    }

    fun setName(v: String) {
        _state.value = _state.value.copy(name = v)
    }

    fun setPm(v: String) {
        _state.value = _state.value.copy(pm = v)
    }

    fun setPnum(v: String) {
        _state.value = _state.value.copy(pnum = v)
    }

    fun submit(context: android.content.Context, onSuccess: () -> Unit) {
        val st = _state.value
        val nm = st.name.trim()
        val v = Phone.validate(st.pnum)

        if (nm.isEmpty()) {
            _state.value = st.copy(err = "পূর্ণ নাম দিন")
            return
        }
        if (!v.ok) {
            _state.value = st.copy(err = "${if (st.pm == "bkash") "বিকাশ" else "নগদ"} নাম্বার: ${v.msg}")
            return
        }

        _state.value = st.copy(busy = true, err = "")

        viewModelScope.launch {
            try {
                val api = Api(context)
                val repo = UserRepo(api)
                val dev = Session.deviceId(context)

                // ডিভাইস চেক
                val existing = repo.findByDevice(dev)
                if (existing != null) {
                    throw ApiException("এই ডিভাইসে ইতিমধ্যে একটি একাউন্ট যুক্ত আছে। সাহায্যের জন্য অ্যাডমিনের সাথে যোগাযোগ করুন: ${BuildConfig.ADMIN_PHONE}")
                }

                val salt = PinHash.newSalt()
                val hash = PinHash.hash(st.pin, salt)

                val body = JSONObject().apply {
                    put("phone", st.phone)
                    put("full_name", nm)
                    put("payment_method", st.pm)
                    put("payment_number", v.phone)
                    put("password_salt", salt)
                    put("password_hash", hash)
                    put("device_id", dev)
                }

                val rows = api.post("app_users", body)
                if (rows.length() == 0) throw ApiException("সাইনআপ ব্যর্থ")

                val user = rows.getJSONObject(0)

                // temp সাফ করো
                context.getSharedPreferences("kp_temp", android.content.Context.MODE_PRIVATE).edit()
                    .remove("kp_signup")
                    .apply()

                Session.set(context, user)

                // লগ (ব্যর্থ হলেও থামবে না)
                try {
                    val logBody = JSONObject().apply {
                        put("user_id", user.optInt("id").toString())
                        put("user_name", nm)
                        put("action", "signup")
                        put("detail", "নতুন অ্যাকাউন্ট")
                    }
                    api.post("activity_log", logBody)
                } catch (e: Exception) {
                    // ignore
                }

                onSuccess()

            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("duplicate", ignoreCase = true) == true ||
                            e.message?.contains("23505") == true -> "এই নাম্বার বা ডিভাইসে ইতিমধ্যে একাউন্ট আছে"
                    else -> e.message ?: "সাইনআপ ব্যর্থ"
                }
                _state.value = _state.value.copy(err = msg, busy = false)
            }
        }
    }
}
