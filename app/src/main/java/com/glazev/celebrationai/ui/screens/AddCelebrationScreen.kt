package com.glazev.celebrationai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.glazev.celebrationai.R
import com.glazev.celebrationai.data.AppSettings
import com.glazev.celebrationai.data.AppTheme
import com.glazev.celebrationai.data.Celebration
import com.glazev.celebrationai.data.CelebrationGroup
import com.glazev.celebrationai.data.CelebrationTone
import com.glazev.celebrationai.data.CelebrationType
import com.glazev.celebrationai.ui.theme.*
import org.koin.compose.koinInject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCelebrationScreen(
    celebration: Celebration? = null,
    defaultHour: Int = 12,
    defaultMinute: Int = 0,
    onSave: (Celebration) -> Unit,
    onBack: () -> Unit
) {
    val appSettings: AppSettings = koinInject()
    
    val backgroundBrush = Brush.verticalGradient(
        colors = when (appSettings.selectedTheme) {
            AppTheme.DARK -> listOf(DarkGradientStart, DarkGradientEnd)
            AppTheme.LIGHT -> listOf(LightGradientStart, LightGradientEnd)
            AppTheme.CELEBRATION -> listOf(CelebrationGradientStart, CelebrationGradientEnd)
        }
    )

    var name by remember { mutableStateOf(celebration?.name ?: "") }
    var selectedType by remember { mutableStateOf(celebration?.type ?: CelebrationType.BIRTHDAY) }
    var customType by remember { mutableStateOf(celebration?.customType ?: "") }
    var hobby by remember { mutableStateOf(celebration?.hobby ?: "") }
    var profession by remember { mutableStateOf(celebration?.profession ?: "") }
    var selectedTone by remember { mutableStateOf(celebration?.tone ?: CelebrationTone.SOLEMN) }
    var selectedGroup by remember { mutableStateOf(celebration?.group ?: CelebrationGroup.NONE) }
    
    var reminderHour by remember { mutableIntStateOf(celebration?.reminderHour ?: defaultHour) }
    var reminderMinute by remember { mutableIntStateOf(celebration?.reminderMinute ?: defaultMinute) }
    var reminderDaysBefore by remember { mutableIntStateOf(celebration?.reminderDaysBefore ?: appSettings.defaultReminderDaysBefore) }

    val calendar = remember { Calendar.getInstance() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = celebration?.date ?: calendar.timeInMillis
    )
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = reminderHour,
        initialMinute = reminderMinute
    )

    val autoSave = {
        if (name.isNotBlank()) {
            onSave(
                Celebration(
                    id = celebration?.id ?: 0,
                    name = name,
                    type = selectedType,
                    customType = if (selectedType == CelebrationType.OTHER) customType else "",
                    date = datePickerState.selectedDateMillis ?: calendar.timeInMillis,
                    hobby = hobby,
                    profession = profession,
                    tone = selectedTone,
                    group = selectedGroup,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    reminderDaysBefore = reminderDaysBefore,
                    savedGreeting = celebration?.savedGreeting,
                    giftIdeas = celebration?.giftIdeas,
                    greetingHistory = celebration?.greetingHistory ?: ""
                )
            )
        }
    }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (celebration == null) stringResource(R.string.title_new_celebration) else stringResource(R.string.title_edit_celebration), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    IconButton(onClick = { 
                        autoSave()
                        onBack() 
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.btn_cancel), tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.label_type), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CelebrationType.entries.forEach { type ->
                                FilterChip(
                                    selected = (selectedType == type),
                                    onClick = { 
                                        selectedType = type 
                                        autoSave()
                                    },
                                    label = { 
                                        Text(when(type) {
                                            CelebrationType.BIRTHDAY -> stringResource(R.string.type_birthday)
                                            CelebrationType.WEDDING -> stringResource(R.string.type_wedding)
                                            CelebrationType.ANNIVERSARY -> stringResource(R.string.type_anniversary)
                                            CelebrationType.OTHER -> stringResource(R.string.type_other)
                                        }) 
                                    },
                                    colors = chipColors
                                )
                            }
                        }

                        if (selectedType == CelebrationType.OTHER) {
                            OutlinedTextField(
                                value = customType,
                                onValueChange = { 
                                    customType = it 
                                    autoSave()
                                },
                                label = { Text(stringResource(R.string.label_type)) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { 
                                name = it 
                                autoSave()
                            },
                            label = { Text(stringResource(R.string.label_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            val sdf = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
                            val date = datePickerState.selectedDateMillis ?: calendar.timeInMillis
                            Text(stringResource(R.string.label_date) + ": ${sdf.format(Date(date))}")
                        }

                        OutlinedTextField(
                            value = profession,
                            onValueChange = { 
                                profession = it 
                                autoSave()
                            },
                            label = { Text(stringResource(R.string.label_profession)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        OutlinedTextField(
                            value = hobby,
                            onValueChange = { 
                                hobby = it 
                                autoSave()
                            },
                            label = { Text(stringResource(R.string.label_hobby)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(stringResource(R.string.label_group), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CelebrationGroup.entries.forEach { group ->
                                FilterChip(
                                    selected = (selectedGroup == group),
                                    onClick = { 
                                        selectedGroup = group 
                                        autoSave()
                                    },
                                    label = { 
                                        Text(when(group) {
                                            CelebrationGroup.NONE -> stringResource(R.string.group_none)
                                            CelebrationGroup.FAMILY -> stringResource(R.string.group_family)
                                            CelebrationGroup.FRIENDS -> stringResource(R.string.group_friends)
                                            CelebrationGroup.WORK -> stringResource(R.string.group_work)
                                            CelebrationGroup.IMPORTANT -> stringResource(R.string.group_important)
                                        }) 
                                    },
                                    colors = chipColors
                                )
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.label_notifications), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Button(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer, 
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute)
                            Text(stringResource(R.string.label_time, timeStr))
                        }
                        
                        Spacer(Modifier.height(8.dp))
                        val daysText = if (reminderDaysBefore == 0) stringResource(R.string.label_on_day) else stringResource(R.string.label_days_format, reminderDaysBefore)
                        Text(stringResource(R.string.label_remind_ahead, daysText), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        
                        val reminderOptions = listOf(0, 3, 5, 7, 10, 14, 21)
                        Slider(
                            value = reminderOptions.indexOf(reminderDaysBefore).toFloat(),
                            onValueChange = { 
                                reminderDaysBefore = reminderOptions[it.roundToInt()]
                                autoSave()
                            },
                            valueRange = 0f..(reminderOptions.size - 1).toFloat(),
                            steps = reminderOptions.size - 2,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        )
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            reminderOptions.forEach { Text(it.toString(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) }
                        }
                    }
                }

                Text(stringResource(R.string.label_tone), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface)
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CelebrationTone.entries.forEach { tone ->
                        FilterChip(
                            selected = (selectedTone == tone),
                            onClick = { 
                                selectedTone = tone 
                                autoSave()
                            },
                            label = { 
                                Text(when(tone) {
                                    CelebrationTone.ROMANTIC -> stringResource(R.string.tone_romantic)
                                    CelebrationTone.SOLEMN -> stringResource(R.string.tone_solemn)
                                    CelebrationTone.OFFICIAL -> stringResource(R.string.tone_official)
                                    CelebrationTone.HUMOROUS -> stringResource(R.string.tone_humorous)
                                    CelebrationTone.DARK_HUMOR -> stringResource(R.string.tone_dark_humor)
                                }) 
                            },
                            colors = chipColors
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        autoSave()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(stringResource(R.string.btn_done), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }

        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = { 
                        showDatePicker = false 
                        autoSave()
                    }) { Text("OK") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        reminderHour = timePickerState.hour
                        reminderMinute = timePickerState.minute
                        showTimePicker = false
                        autoSave()
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.btn_cancel)) }
                },
                text = {
                    TimePicker(state = timePickerState)
                }
            )
        }
    }
}
