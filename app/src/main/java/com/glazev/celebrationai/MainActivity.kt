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
import com.glazev.celebrationai.ui.screens.CalendarScreen
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
        
        updateWidgets()

        setContent {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            var showPermissionDialog by remember { 
                mutableStateOf(
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms())
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(this@MainActivity, getString(R.string.msg_no_push_permission), Toast.LENGTH_LONG).show()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = android.net.Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: Exception) { e.printStackTrace() }
                }
            }

            if (showPermissionDialog) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { showPermissionDialog = false },
                    title = { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.title_notif_permission), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                    text = { androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.desc_notif_permission)) },
                    confirmButton = {
                        androidx.compose.material3.Button(onClick = {
                            showPermissionDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                                try {
                                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = android.net.Uri.parse("package:$packageName")
                                    }
                                    startActivity(intent)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                        }) {
                            androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.btn_understood))
                        }
                    },
                    dismissButton = {
                        androidx.compose.material3.OutlinedButton(onClick = { showPermissionDialog = false }) {
                            androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(R.string.btn_later))
                        }
                    }
                )
            }

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
                                val msg = getString(R.string.msg_signed_in)
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
                                webClientId = getString(R.string.default_web_client_id)
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
                onCalendarClick = { navController.navigate("calendar") },
                onAboutClick = { navController.navigate("about") },
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
        composable("calendar") {
            CalendarScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCelebrationClick = { celebration ->
                    navController.navigate("edit/${celebration.id}")
                }
            )
        }
        composable("about") {
            com.glazev.celebrationai.ui.screens.AboutScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
