package com.glazev.celebrationai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import com.glazev.celebrationai.R
import com.glazev.celebrationai.data.Celebration
import com.glazev.celebrationai.data.CelebrationGroup
import com.glazev.celebrationai.ui.viewmodel.CelebrationViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: CelebrationViewModel,
    onBack: () -> Unit,
    onCelebrationClick: (Celebration) -> Unit
) {
    val celebrations by viewModel.allCelebrations.collectAsState()
    
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf<Calendar?>(null) }
    var selectedGroup by remember { mutableStateOf<com.glazev.celebrationai.data.CelebrationGroup?>(null) }

    // Обновление месяца
    fun changeMonth(amount: Int) {
        val newCal = currentMonth.clone() as Calendar
        newCal.add(Calendar.MONTH, amount)
        currentMonth = newCal
    }

    val currentMonthSdf = remember { SimpleDateFormat("LLLL yyyy", Locale.getDefault()) }

    // Группируем праздники по дню и месяцу для отображения в текущем календаре
    val monthCelebrations = celebrations.filter { 
        val cal = Calendar.getInstance().apply { timeInMillis = it.date }
        cal.get(Calendar.MONTH) == currentMonth.get(Calendar.MONTH) 
    }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    val totalBudget = monthCelebrations.filter { 
        selectedGroup == null || it.group == selectedGroup
    }.sumOf { it.estimatedBudget }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.title_calendar_screen), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.btn_back)) }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            
            // Заголовок месяца и переключатели
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { changeMonth(-1) }) {
                    Icon(Icons.Default.ChevronLeft, androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.content_desc_prev_month))
                }
                Text(
                    text = currentMonthSdf.format(currentMonth.time).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { changeMonth(1) }) {
                    Icon(Icons.Default.ChevronRight, androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.content_desc_next_month))
                }
            }

            // Бюджет на месяц
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                val budgetPrefix = if (selectedGroup == null) 
                    androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.calendar_total_budget)
                else 
                    androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.calendar_category_budget)
                Text(
                    text = "$budgetPrefix $totalBudget руб.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Легенда и фильтр
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val groups = listOf(
                    CelebrationGroup.FAMILY to Color(0xFFE53935),
                    CelebrationGroup.FRIENDS to Color(0xFF1E88E5),
                    CelebrationGroup.WORK to Color(0xFF43A047),
                    CelebrationGroup.IMPORTANT to Color(0xFFFDD835)
                )
                items(groups) { (group, color) ->
                    val isSelected = selectedGroup == group
                    val groupName = when(group) {
                        CelebrationGroup.FAMILY -> androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.group_family)
                        CelebrationGroup.FRIENDS -> androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.group_friends)
                        CelebrationGroup.WORK -> androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.group_work)
                        CelebrationGroup.IMPORTANT -> androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.group_important)
                        else -> ""
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                            .clickable { selectedGroup = if (isSelected) null else group }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = groupName,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Календарь (Сетка)
            CalendarGrid(
                currentMonth = currentMonth,
                celebrations = monthCelebrations,
                onDayClick = { day ->
                    val clicked = currentMonth.clone() as Calendar
                    clicked.set(Calendar.DAY_OF_MONTH, day)
                    selectedDate = clicked
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Список именинников в выбранный день
            selectedDate?.let { date ->
                val dayCelebrations = monthCelebrations.filter { 
                    val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                    cal.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH)
                }
                
                Text(
                    text = androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.label_events_on_day, date.get(Calendar.DAY_OF_MONTH)),
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (dayCelebrations.isEmpty()) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.glazev.celebrationai.R.string.label_no_events_day),
                        modifier = Modifier.padding(16.dp),
                        color = Color.Gray
                    )
                } else {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        items(dayCelebrations) { celeb ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onCelebrationClick(celeb) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(celeb.name, fontWeight = FontWeight.Bold)
                                    if (celeb.estimatedBudget > 0) {
                                        Text("${celeb.estimatedBudget} руб.", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(
    currentMonth: Calendar,
    celebrations: List<Celebration>,
    onDayClick: (Int) -> Unit
) {
    val tempCal = currentMonth.clone() as Calendar
    tempCal.set(Calendar.DAY_OF_MONTH, 1)
    
    // Сдвиг (день недели 1-го числа). В Calendar SUNDAY = 1, MONDAY = 2.
    // Приводим к 0..6 где 0 = Понедельник
    var firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 2
    if (firstDayOfWeek < 0) firstDayOfWeek += 7
    
    val daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Дни недели
        Row(modifier = Modifier.fillMaxWidth()) {
            val daysOfWeek = androidx.compose.ui.res.stringArrayResource(R.array.days_of_week_short)
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Сетка
        val totalCells = firstDayOfWeek + daysInMonth
        val rows = Math.ceil(totalCells / 7.0).toInt()
        
        var currentDay = 1
        for (i in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0 until 7) {
                    if (i == 0 && j < firstDayOfWeek) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else if (currentDay <= daysInMonth) {
                        val day = currentDay
                        val dayCelebs = celebrations.filter { 
                            val c = Calendar.getInstance().apply { timeInMillis = it.date }
                            c.get(Calendar.DAY_OF_MONTH) == day 
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (dayCelebs.isNotEmpty()) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { onDayClick(day) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = day.toString(), fontWeight = if (dayCelebs.isNotEmpty()) FontWeight.Bold else FontWeight.Normal)
                                if (dayCelebs.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.Center) {
                                        // Отрисовываем цветные точки
                                        dayCelebs.take(3).forEach { celeb ->
                                            val color = when(celeb.group) {
                                                CelebrationGroup.FAMILY -> Color(0xFFE53935)
                                                CelebrationGroup.FRIENDS -> Color(0xFF1E88E5)
                                                CelebrationGroup.WORK -> Color(0xFF43A047)
                                                CelebrationGroup.IMPORTANT -> Color(0xFFFDD835)
                                                else -> Color.Gray
                                            }
                                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(color).padding(1.dp))
                                        }
                                    }
                                }
                            }
                        }
                        currentDay++
                    } else {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }
    }
}
