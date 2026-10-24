package com.srizwan.task

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val TASK = "task"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val SUPPORT = "support"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(0xFF041A15.toInt()))
        enableEdgeToEdge()
        ThemeStore.load(this)
        setContent {
            RizwanTheme(mode = ThemeStore.mode) {
                AppNav()
            }
        }
    }
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val hasSession = remember { Session.exists(context) }

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(hasSession = hasSession) { route ->
                nav.navigate(route) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onSuccess = {
                    nav.navigate(Routes.TASK) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onSignup = {
                    nav.navigate(Routes.SIGNUP)
                }
            )
        }
        composable(Routes.SIGNUP) {
            SignupScreen(
                onSuccess = {
                    nav.navigate(Routes.TASK) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.TASK) {
            TaskScreen(navController = nav)
        }
        composable(Routes.PROFILE) { ProfileScreen(navController = nav) }
        composable(Routes.SUPPORT) { PlaceholderScreen("সাপোর্ট") }
        composable(Routes.ADMIN) { PlaceholderScreen("অ্যাডমিন") }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "পরের ধাপে বানানো হবে",
                color = AppColors.extra.muted
            )
        }
    }
}
