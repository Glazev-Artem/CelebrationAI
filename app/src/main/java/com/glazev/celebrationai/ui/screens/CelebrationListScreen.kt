package com.glazev.celebrationai.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.glazev.celebrationai.R
import com.glazev.celebrationai.data.*
import com.glazev.celebrationai.service.AuthManager
import com.glazev.celebrationai.ui.theme.*
import com.glazev.celebrationai.ui.viewmodel.CelebrationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CelebrationListScreen(
    viewModel: CelebrationViewModel,
    authManager: AuthManager,
    appSettings: AppSettings,
    onAddClick: () -> Unit,
    onEditClick: (Celebration) -> Unit,
    onGoogleSignInClick: () -> Unit,
    onThemeUpdated: (AppTheme) -> Unit,
    onLanguageUpdated: (AppLanguage) -> Unit
) {
    val celebrations by viewModel.allCelebrations.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val user by authManager.user.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf(CelebrationGroup.NONE) }
    
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedCelebrationId by remember { mutableStateOf<Int?>(null) }
    var funFactsText by remember { mutableStateOf<String?>(null) }
    
    var showGreetingDialog by remember { mutableStateOf(false) }
    var generatedGreeting by remember { mutableStateOf<String?>(null) }
    var isApology by remember { mutableStateOf(false) }
    
    var showGiftIdeasDialog by remember { mutableStateOf(false) }
    var giftIdeasText by remember { mutableStateOf<String?>(null) }
    var wishlistText by remember { mutableStateOf("") }
    var isWishlistSaved by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDeveloperInfoDialog by remember { mutableStateOf(false) }
    
    var tempSelectedTheme by remember { mutableStateOf(appSettings.selectedTheme) }
    var tempSelectedLanguage by remember { mutableStateOf(appSettings.selectedLanguage) }
    var tempIsBiometricEnabled by remember { mutableStateOf(appSettings.isBiometricEnabled) }
    var tempDefaultReminderDays by remember { mutableIntStateOf(appSettings.defaultReminderDaysBefore) }
    var isEditingTime by remember { mutableStateOf(false) }
    
    val settingsTimePickerState = rememberTimePickerState(
        initialHour = appSettings.defaultHour,
        initialMinute = appSettings.defaultMinute,
        is24Hour = true
    )

    val filteredCelebrations = celebrations.filter {
        (it.name.contains(searchQuery, ignoreCase = true)) &&
        (selectedGroupFilter == CelebrationGroup.NONE || it.group == selectedGroupFilter)
    }.sortedBy { it.daysUntil() }

    val currentCelebration = celebrations.find { it.id == selectedCelebrationId }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) viewModel.importFromContacts()
    }

    val backgroundBrush = Brush.verticalGradient(
        colors = when (appSettings.selectedTheme) {
            AppTheme.DARK -> listOf(DarkGradientStart, DarkGradientEnd)
            AppTheme.LIGHT -> listOf(LightGradientStart, LightGradientEnd)
            AppTheme.CELEBRATION -> listOf(CelebrationGradientStart, CelebrationGradientEnd)
        }
    )

    Scaffold(
        topBar = {
            Column(Modifier.background(Color.Transparent)) {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary) },
                    actions = {
                        IconButton(onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) viewModel.importFromContacts()
                            else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }) { Icon(Icons.Default.ContactPage, null) }
                        IconButton(onClick = { 
                            tempSelectedTheme = appSettings.selectedTheme
                            tempSelectedLanguage = appSettings.selectedLanguage
                            tempIsBiometricEnabled = appSettings.isBiometricEnabled
                            tempDefaultReminderDays = appSettings.defaultReminderDaysBefore
                            showSettingsDialog = true 
                        }) { Icon(Icons.Default.Settings, null) }
                        IconButton(onClick = { showDeveloperInfoDialog = true }) { 
                            Icon(Icons.Default.Info, contentDescription = "Info") 
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.hint_name)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent
                    )
                )
                ScrollableTabRow(
                    selectedTabIndex = maxOf(0, CelebrationGroup.entries.indexOf(selectedGroupFilter)),
                    edgePadding = 16.dp, containerColor = Color.Transparent, divider = {}, indicator = {}
                ) {
                    CelebrationGroup.entries.forEach { group ->
                        FilterChip(
                            selected = (selectedGroupFilter == group),
                            onClick = { selectedGroupFilter = group },
                            label = { 
                                Text(when(group) {
                                    CelebrationGroup.NONE -> stringResource(R.string.group_none)
                                    CelebrationGroup.FAMILY -> stringResource(R.string.group_family)
                                    CelebrationGroup.FRIENDS -> stringResource(R.string.group_friends)
                                    CelebrationGroup.WORK -> stringResource(R.string.group_work)
                                    CelebrationGroup.IMPORTANT -> stringResource(R.string.group_important)
                                }) 
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick, containerColor = MaterialTheme.colorScheme.primaryContainer) { Icon(Icons.Default.Add, null) }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush).padding(padding)) {
            if (filteredCelebrations.isEmpty()) {
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Cake, null, Modifier.size(64.dp), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text(stringResource(R.string.empty_list), textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCelebrations, key = { it.id }) { item ->
                        CelebrationItem(
                            celebration = item,
                            isDark = appSettings.selectedTheme == AppTheme.DARK,
                            onClick = { onEditClick(item) },
                            onDelete = { viewModel.deleteCelebration(item) },
                            onInfo = {
                                selectedCelebrationId = item.id
                                if (item.funFacts != null) { funFactsText = item.funFacts; showInfoDialog = true }
                                else {
                                    scope.launch {
                                        funFactsText = context.getString(R.string.loading_ai); showInfoDialog = true
                                        val facts = viewModel.generateFunFacts(item)
                                        funFactsText = facts ?: context.getString(R.string.error_ai)
                                        if (facts != null) viewModel.updateCelebration(item.copy(funFacts = facts))
                                    }
                                }
                            },
                            onGenerate = {
                                selectedCelebrationId = item.id
                                isApology = false
                                showGreetingDialog = true
                            },
                            onViewGifts = {
                                selectedCelebrationId = item.id
                                giftIdeasText = item.giftIdeas
                                wishlistText = item.wishlist
                                isWishlistSaved = false
                                showGiftIdeasDialog = true
                            }
                        )
                    }
                }
            }
        }

        if (showInfoDialog && currentCelebration != null) {
            if (currentCelebration.daysUntil() == 0L) ConfettiEffect()
            AlertDialog(onDismissRequest = { showInfoDialog = false },
                title = { Text(currentCelebration.name, fontWeight = FontWeight.Bold) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(12.dp)) {
                        InfoRow(Icons.Default.AutoAwesome, stringResource(R.string.label_zodiac), stringResource(CelebrationUtils.getZodiacSignRes(currentCelebration.date)))
                        InfoRow(Icons.Default.Pets, stringResource(R.string.label_chinese_zodiac), stringResource(CelebrationUtils.getChineseZodiacRes(currentCelebration.date)))
                        InfoRow(Icons.Default.HourglassEmpty, stringResource(R.string.label_lived), CelebrationUtils.getLocalizedLifeStats(context, currentCelebration.date))
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Text(stringResource(R.string.label_fun_facts), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(funFactsText ?: stringResource(R.string.loading_ai))
                    }
                },
                confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text(stringResource(R.string.btn_close)) } }
            )
        }

        if (showGreetingDialog && currentCelebration != null) {
            if (currentCelebration.daysUntil() == 0L) ConfettiEffect()
            AlertDialog(onDismissRequest = { showGreetingDialog = false },
                title = { Text(stringResource(R.string.title_ai_greeting)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isApology = !isApology }) {
                            Checkbox(checked = isApology, onCheckedChange = { isApology = it })
                            Text(stringResource(R.string.label_late_greeting), style = MaterialTheme.typography.bodyMedium)
                        }

                        Button(onClick = {
                            scope.launch {
                                generatedGreeting = context.getString(R.string.loading_ai)
                                val greeting = viewModel.generateAiGreeting(currentCelebration, isApology)
                                generatedGreeting = greeting ?: context.getString(R.string.error_ai)
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.btn_think_greeting)) }

                        if (generatedGreeting == context.getString(R.string.loading_ai)) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (generatedGreeting != null && generatedGreeting != context.getString(R.string.error_ai)) {
                            val variants = generatedGreeting!!.split("---").filter { it.isNotBlank() }
                            variants.forEach { variant ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(variant.trim(), fontSize = 14.sp)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { clipboardManager.setText(AnnotatedString(variant.trim())) }) {
                                                Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(stringResource(R.string.btn_copy))
                                            }
                                            TextButton(onClick = {
                                                viewModel.saveGreeting(currentCelebration, variant.trim())
                                                clipboardManager.setText(AnnotatedString(variant.trim()))
                                                showGreetingDialog = false
                                            }) {
                                                Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(stringResource(R.string.btn_select))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (currentCelebration.greetingHistory.isNotEmpty()) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text(stringResource(R.string.label_greeting_archive), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            val historyItems = currentCelebration.greetingHistory.split("|").reversed()
                            historyItems.forEachIndexed { index, entry ->
                                val parts = entry.split(":", limit = 2)
                                if (parts.size == 2) {
                                    key(entry) {
                                        var isExpanded by remember { mutableStateOf(false) }
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { isExpanded = !isExpanded },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                        ) {
                                            Column(Modifier.padding(8.dp)) {
                                                Row(
                                                    Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(parts[0], fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = { viewModel.deleteHistoryEntry(currentCelebration, index) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(Icons.Default.Close, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                                        }
                                                        Spacer(Modifier.width(4.dp))
                                                        Icon(
                                                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                            null,
                                                            modifier = Modifier.size(20.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                if (isExpanded) {
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(parts[1], fontSize = 12.sp)
                                                    TextButton(
                                                        onClick = { clipboardManager.setText(AnnotatedString(parts[1])) },
                                                        modifier = Modifier.align(Alignment.End)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp))
                                                        Text(" " + stringResource(R.string.btn_copy), style = MaterialTheme.typography.labelSmall)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showGreetingDialog = false }) { Text(stringResource(R.string.btn_close)) } }
            )
        }

        if (showGiftIdeasDialog && currentCelebration != null) {
            AlertDialog(onDismissRequest = { showGiftIdeasDialog = false },
                title = { Text(stringResource(R.string.title_gifts_for, currentCelebration.name)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.label_wishlist), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(
                            value = wishlistText,
                            onValueChange = { wishlistText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(stringResource(R.string.hint_wishlist)) },
                            minLines = 3
                        )

                        // Детектор ссылок в вишлисте
                        val foundUrls = remember(wishlistText) {
                            Regex("(https?://[\\w-]+(\\.[\\w-]+)+(/\\S*)?)").findAll(wishlistText).map { it.value }.toList()
                        }
                        
                        if (foundUrls.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                foundUrls.forEach { url ->
                                    val isWb = url.contains("wildberries.ru")
                                    val isOzon = url.contains("ozon.ru")
                                    AssistChip(
                                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
                                        label = { Text(if (isWb) "WB" else if (isOzon) "Ozon" else "Открыть", maxLines = 1) },
                                        leadingIcon = {
                                            Icon(
                                                if (isWb) Icons.Default.ShoppingBag else if (isOzon) Icons.Default.ShoppingCart else Icons.Default.Link,
                                                null,
                                                modifier = Modifier.size(16.dp),
                                                tint = if (isWb) Color(0xFF9C27B0) else if (isOzon) Color(0xFF005BFF) else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        val saveButtonColor by animateColorAsState(
                            if (isWishlistSaved) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )

                        Button(
                            onClick = {
                                viewModel.updateCelebration(currentCelebration.copy(wishlist = wishlistText))
                                scope.launch {
                                    isWishlistSaved = true
                                    delay(2000)
                                    isWishlistSaved = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = saveButtonColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isWishlistSaved) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.label_saved))
                            } else {
                                Text(stringResource(R.string.btn_save_list))
                            }
                        }

                        HorizontalDivider(Modifier.padding(vertical = 8.dp))

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.label_ai_ideas), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Button(onClick = {
                                scope.launch {
                                    giftIdeasText = context.getString(R.string.loading_ai)
                                    val ideas = viewModel.generateGiftIdeas(currentCelebration)
                                    giftIdeasText = ideas ?: context.getString(R.string.error_ai)
                                    if (ideas != null) viewModel.updateCelebration(currentCelebration.copy(giftIdeas = ideas))
                                }
                            }) { Text(stringResource(R.string.btn_generate)) }
                        }
                        
                        if (giftIdeasText != null && giftIdeasText != context.getString(R.string.loading_ai) && giftIdeasText != context.getString(R.string.error_ai)) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                giftIdeasText!!.split("\n").forEach { line ->
                                    if (line.isNotBlank()) {
                                        // Проверяем формат [N]. Название | Поиск | Тип | Описание
                                        val parts = line.split("|")
                                        if (parts.size >= 3) {
                                            val titlePart = parts[0].trim()
                                            val searchQuery = parts[1].trim()
                                            val type = parts[2].trim().lowercase()
                                            val description = parts.getOrNull(3)?.trim() ?: ""

                                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                                Text(titlePart, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                
                                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (type.contains("услуг")) {
                                                        // Кнопка Авито для услуг
                                                        AssistChip(
                                                            onClick = {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.avito.ru/rossiya?q=$searchQuery"))
                                                                context.startActivity(intent)
                                                            },
                                                            label = { Text("Avito", fontSize = 10.sp) },
                                                            leadingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = Color(0xFF00AAFF)) }
                                                        )
                                                    } else {
                                                        // Кнопки WB и Ozon для товаров
                                                        AssistChip(
                                                            onClick = {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.wildberries.ru/catalog/0/search.aspx?search=$searchQuery"))
                                                                context.startActivity(intent)
                                                            },
                                                            label = { Text("WB", fontSize = 10.sp) },
                                                            leadingIcon = { Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(14.dp), tint = Color(0xFF9C27B0)) }
                                                        )
                                                        AssistChip(
                                                            onClick = {
                                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ozon.ru/search/?text=$searchQuery"))
                                                                context.startActivity(intent)
                                                            },
                                                            label = { Text("Ozon", fontSize = 10.sp) },
                                                            leadingIcon = { Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(14.dp), tint = Color(0xFF005BFF)) }
                                                        )
                                                    }
                                                }
                                                if (description.isNotBlank()) {
                                                    Text(description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        } else {
                                            // Если формат не совпал (старые записи или сбой AI), выводим как текст
                                            Text(line, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(giftIdeasText ?: stringResource(R.string.hint_generate_gifts))
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showGiftIdeasDialog = false }) { Text(stringResource(R.string.btn_close)) } }
            )
        }

        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text(stringResource(R.string.title_settings)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.label_account), fontWeight = FontWeight.Bold)
                        if (user != null) {
                            Text(stringResource(R.string.label_logged_as, user?.displayName ?: user?.email ?: ""))
                            Button(onClick = { authManager.signOut(); showSettingsDialog = false }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.btn_sign_out))
                            }
                        } else {
                            Button(onClick = { onGoogleSignInClick(); showSettingsDialog = false }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.AccountCircle, null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_sign_in_google))
                            }
                        }

                        HorizontalDivider()

                        Text(stringResource(R.string.label_security), fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.label_biometric_login), modifier = Modifier.weight(1f))
                            Switch(checked = tempIsBiometricEnabled, onCheckedChange = { tempIsBiometricEnabled = it })
                        }

                        HorizontalDivider()

                        Text(stringResource(R.string.label_language), fontWeight = FontWeight.Bold)
                        AppLanguage.entries.forEach { language ->
                            Row(
                                Modifier.fillMaxWidth().clickable { tempSelectedLanguage = language },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (tempSelectedLanguage == language), onClick = { tempSelectedLanguage = language })
                                Text(language.displayName)
                            }
                        }

                        HorizontalDivider()

                        Text(stringResource(R.string.label_theme), fontWeight = FontWeight.Bold)
                        AppTheme.entries.forEach { theme ->
                            Row(
                                Modifier.fillMaxWidth().clickable { tempSelectedTheme = theme },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = (tempSelectedTheme == theme), onClick = { tempSelectedTheme = theme })
                                Text(when(theme) {
                                    AppTheme.LIGHT -> stringResource(R.string.theme_light)
                                    AppTheme.DARK -> stringResource(R.string.theme_dark)
                                    AppTheme.CELEBRATION -> stringResource(R.string.theme_celebration)
                                })
                            }
                        }

                        HorizontalDivider()

                        Text(stringResource(R.string.label_default_reminder), fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.label_remind_before))
                            OutlinedTextField(
                                value = tempDefaultReminderDays.toString(),
                                onValueChange = { tempDefaultReminderDays = it.toIntOrNull() ?: 0 },
                                modifier = Modifier.width(60.dp),
                                singleLine = true
                            )
                            Text(stringResource(R.string.label_days_before))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isEditingTime = true }) {
                            Icon(Icons.Default.AccessTime, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.label_time, String.format(Locale.getDefault(), "%02d:%02d", settingsTimePickerState.hour, settingsTimePickerState.minute)))
                        }

                        if (isEditingTime) {
                            TimePicker(state = settingsTimePickerState)
                            TextButton(onClick = { isEditingTime = false }) { Text("OK") }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val languageChanged = appSettings.selectedLanguage != tempSelectedLanguage

                        appSettings.selectedTheme = tempSelectedTheme
                        appSettings.selectedLanguage = tempSelectedLanguage
                        appSettings.isBiometricEnabled = tempIsBiometricEnabled
                        appSettings.defaultReminderDaysBefore = tempDefaultReminderDays
                        appSettings.defaultHour = settingsTimePickerState.hour
                        appSettings.defaultMinute = settingsTimePickerState.minute

                        onThemeUpdated(tempSelectedTheme)
                        if (languageChanged) {
                            onLanguageUpdated(tempSelectedLanguage)
                        }
                        showSettingsDialog = false
                    }) { Text(stringResource(R.string.btn_save)) }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsDialog = false }) { Text(stringResource(R.string.btn_cancel)) }
                }
            )
        }

        if (showDeveloperInfoDialog) {
            AlertDialog(
                onDismissRequest = { showDeveloperInfoDialog = false },
                title = { Text(stringResource(R.string.title_about)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.footer_line1), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.footer_line2))
                        Text("Версия: 1.3.2")
                        
                        HorizontalDivider()
                        
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://vk.com/lavka_apps"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
                        ) {
                            Icon(Icons.Default.OpenInNew, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.btn_feedback))
                        }

                        TextButton(
                            onClick = {
                                // ЗАМЕНИТЕ ЭТУ ССЫЛКУ ПОСЛЕ ВЫГРУЗКИ НА GITHUB
                                // Нужно вставить ссылку "Raw" на ваш файл PRIVACY_POLICY.md
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/glazev/CelebrationAI/blob/main/PRIVACY_POLICY.md"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(stringResource(R.string.label_privacy_policy), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDeveloperInfoDialog = false }) {
                        Text(stringResource(R.string.btn_close))
                    }
                }
            )
        }
    }
}

@Composable
fun CelebrationItem(
    celebration: Celebration,
    isDark: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onInfo: () -> Unit,
    onGenerate: () -> Unit,
    onViewGifts: () -> Unit
) {
    val hasInfo = celebration.funFacts != null
    val hasGreeting = celebration.savedGreeting != null
    val hasGifts = celebration.giftIdeas != null || celebration.wishlist.isNotBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (celebration.type) {
                            CelebrationType.BIRTHDAY -> Icons.Default.Cake
                            CelebrationType.WEDDING -> Icons.Default.Favorite
                            CelebrationType.ANNIVERSARY -> Icons.Default.Celebration
                            else -> Icons.Default.Star
                        },
                        null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = celebration.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (celebration.type == CelebrationType.OTHER && celebration.customType.isNotBlank()) {
                            celebration.customType
                        } else {
                            when(celebration.type) {
                                CelebrationType.BIRTHDAY -> stringResource(R.string.type_birthday)
                                CelebrationType.WEDDING -> stringResource(R.string.type_wedding)
                                CelebrationType.ANNIVERSARY -> stringResource(R.string.type_anniversary)
                                CelebrationType.OTHER -> stringResource(R.string.type_other)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    val days = celebration.daysUntil()
                    Text(
                        text = if (days == 0L) stringResource(R.string.label_today) else stringResource(R.string.label_days_format, days),
                        color = if (days <= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (celebration.type == CelebrationType.BIRTHDAY) {
                        Text("${celebration.calculateAge()} " + stringResource(R.string.label_years_short), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Инфо (Сиреневая лампочка)
                FilledIconButton(
                    onClick = onInfo,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (hasInfo) Color(0xFFE1BEE7) else Color(0xFFE1F5FE),
                        contentColor = if (hasInfo) Color(0xFF9C27B0) else Color(0xFF0288D1)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Lightbulb, null)
                }

                // Поздравление (Зеленый чат)
                FilledIconButton(
                    onClick = onGenerate,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (hasGreeting) Color(0xFFC8E6C9) else Color(0xFFE1F5FE),
                        contentColor = if (hasGreeting) Color(0xFF2E7D32) else Color(0xFF0288D1)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null)
                }

                // Подарки (Хохлома: красный + золото)
                FilledIconButton(
                    onClick = onViewGifts,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (hasGifts) Color(0xFFD32F2F) else Color(0xFFE1F5FE),
                        contentColor = if (hasGifts) Color(0xFFFFD700) else Color(0xFF0288D1)
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.CardGiftcard, null)
                }

                // Удаление
                FilledIconButton(
                    onClick = onDelete,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFFF5F5F5),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, null)
                }
            }
        }
    }
}

@Composable
fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ConfettiEffect() {
    val particles = remember { List(100) { ConfettiParticle() } }
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        particles.forEach { particle ->
            val y = (particle.posY + progress * 1000f * particle.speed) % size.height
            val x = particle.posX + (Math.sin(progress.toDouble() * 10 * particle.speed.toDouble()) * 20).toFloat()

            drawCircle(
                color = particle.color,
                radius = particle.size,
                center = Offset(x, y)
            )
        }
    }
}

class ConfettiParticle {
    val posX = Random.nextFloat() * 1000f
    val posY = Random.nextFloat() * -1000f
    val size = Random.nextFloat() * 8f + 4f
    val speed = Random.nextFloat() * 0.5f + 0.5f
    val color = Color(
        Random.nextInt(256),
        Random.nextInt(256),
        Random.nextInt(256)
    )
}
