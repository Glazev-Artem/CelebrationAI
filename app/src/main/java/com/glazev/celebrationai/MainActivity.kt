package com.glazev.celebrationai

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.glazev.celebrationai.data.AppLanguage
import com.glazev.celebrationai.data.AppSettings
import com.glazev.celebrationai.data.AppTheme
import com.glazev.celebrationai.service.AuthManager
import com.glazev.celebrationai.service.BiometricHelper
import com.glazev.celebrationai.ui.screens.AddCelebrationScreen
import com.glazev.celebrationai.ui.screens.CelebrationListScreen
import com.glazev.celebrationai.ui.theme.CelebrationAITheme
import com.glazev.celebrationai.ui.viewmodel.CelebrationViewModel
import com.glazev.celebrationai.widget.CelebrationWidgetProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.util.*

class MainActivity : AppCompatActivity() {
    
    private val authManager: AuthManager by inject()
    private val biometricHelper: BiometricHelper by inject()

    override fun attachBaseContext(newBase: Context) {
        val settings = AppSettings(newBase)
        val locale = Locale(settings.selectedLanguage.name.lowercase())
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        checkAndRequestPermissions()
        updateWidgets()

        setContent {
            val settings: AppSettings = koinInject()
            var currentTheme by remember { mutableStateOf(settings.selectedTheme) }
            
            // Если биометрия включена, но не поддерживается или не настроена, пропускаем её
            var isUnlocked by remember { 
                mutableStateOf(!settings.isBiometricEnabled || !biometricHelper.canAuthenticate()) 
            }
            val celebrationViewModel: CelebrationViewModel = koinViewModel()

            // Биометрия при запуске
            LaunchedEffect(Unit) {
                if (settings.isBiometricEnabled && biometricHelper.canAuthenticate()) {
                    delay(300)
                    biometricHelper.showBiometricPrompt(
                        activity = this@MainActivity,
                        onSuccess = { isUnlocked = true },
                        onError = { error ->
                            Toast.makeText(this@MainActivity, error, Toast.LENGTH_LONG).show()
                            // Если пользователь отменил вход, закрываем приложение, иначе - даем шанс (можно добавить кнопку Retry)
                            if (error.contains("отмена", true) || error.contains("cancel", true)) {
                                finish()
                            }
                        }
                    )
                }
            }

            if (isUnlocked) {
                LaunchedEffect(Unit) {
                    if (authManager.isUserSignedIn()) {
                        celebrationViewModel.loadFromCloud()
                    }
                }
                
                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        account.idToken?.let { token ->
                            lifecycleScope.launch {
                                authManager.signInWithGoogle(token)
                                celebrationViewModel.loadFromCloud()
                                val msg = if (settings.selectedLanguage == AppLanguage.RU) "Вход выполнен!" else "Signed in!"
                                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        val status = (e as? ApiException)?.statusCode ?: -1
                        Toast.makeText(this@MainActivity, "Error: $status", Toast.LENGTH_LONG).show()
                    }
                }

                CelebrationAITheme(appTheme = currentTheme) {
                    AppNavigation(
                        viewModel = celebrationViewModel,
                        onThemeChange = { currentTheme = it },
                        onLanguageChange = { lang ->
                            val intent = intent
                            finish()
                            startActivity(intent)
                        },
                        onGoogleSignInClick = {
                            val signInIntent = authManager.getSignInIntent(
                                webClientId = Config.GOOGLE_WEB_CLIENT_ID
                            )
                            googleSignInLauncher.launch(signInIntent)
                        }
                    )
                }
            } else {
                // Экран блокировки (можно добавить красивую заглушку)
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun updateWidgets() {
        try {
            val intent = Intent(this, CelebrationWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(this)
                .getAppWidgetIds(ComponentName(this, CelebrationWidgetProvider::class.java))
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            sendBroadcast(intent)
        } catch (e: Exception) { e.printStackTrace() }
    }
}

@Composable
fun AppNavigation(
    viewModel: CelebrationViewModel,
    onThemeChange: (AppTheme) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    val navController = rememberNavController()
    val appSettings: AppSettings = koinInject()
    val authManager: AuthManager = koinInject()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            CelebrationListScreen(
                viewModel = viewModel,
                authManager = authManager,
                appSettings = appSettings,
                onAddClick = { navController.navigate("add") },
                onEditClick = { celebration ->
                    navController.navigate("edit/${celebration.id}")
                },
                onThemeUpdated = onThemeChange,
                onLanguageUpdated = onLanguageChange,
                onGoogleSignInClick = onGoogleSignInClick
            )
        }
        composable("add") {
            var currentId by remember { mutableIntStateOf(0) }
            AddCelebrationScreen(
                defaultHour = appSettings.defaultHour,
                defaultMinute = appSettings.defaultMinute,
                onSave = { celebration ->
                    if (currentId == 0) {
                        viewModel.addCelebration(celebration) { newId -> currentId = newId }
                    } else {
                        viewModel.updateCelebration(celebration.copy(id = currentId))
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "edit/{celebrationId}",
            arguments = listOf(navArgument("celebrationId") { type = NavType.IntType })
        ) { backStackEntry ->
            val celebrationId = backStackEntry.arguments?.getInt("celebrationId") ?: 0
            val celebrations by viewModel.allCelebrations.collectAsState()
            val celebration = celebrations.find { it.id == celebrationId }
            if (celebration != null) {
                AddCelebrationScreen(
                    celebration = celebration,
                    onSave = { updatedCelebration -> viewModel.updateCelebration(updatedCelebration) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
