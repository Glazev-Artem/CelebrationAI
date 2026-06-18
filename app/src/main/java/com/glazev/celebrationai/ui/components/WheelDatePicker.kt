package com.glazev.celebrationai.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    itemLabel: (T) -> String = { it.toString() }
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val initialIndex = items.indexOf(selectedItem).takeIf { it >= 0 } ?: 0
    
    LaunchedEffect(initialIndex) {
        listState.scrollToItem(initialIndex)
    }

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val itemHeight = 48.dp
    val visibleItems = 5
    val spacerCount = visibleItems / 2

    // Haptic feedback on scroll
    val currentCenterIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(currentCenterIndex) {
        if (listState.isScrollInProgress) {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        }
    }

    // Update selected item when scrolling stops
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            val centerItem = visibleItemsInfo.minByOrNull { Math.abs(it.offset + it.size / 2 - layoutInfo.viewportSize.height / 2) }
            centerItem?.let {
                val index = it.index - spacerCount
                if (index in items.indices) {
                    onItemSelected(items[index])
                }
            }
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * visibleItems)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Selection Highlight
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), shape = MaterialTheme.shapes.medium)
        )

        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(spacerCount) { Spacer(modifier = Modifier.height(itemHeight)) }
            
            items(items.size) { index ->
                val item = items[index]
                val isSelected = item == selectedItem
                
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = itemLabel(item),
                        fontSize = if (isSelected) 20.sp else 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            items(spacerCount) { Spacer(modifier = Modifier.height(itemHeight)) }
        }
    }
}

@Composable
fun WheelDatePickerDialog(
    initialDate: Long,
    hasYear: Boolean,
    onHasYearChange: (Boolean) -> Unit,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialDate }
    
    var selectedDay by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }

    val daysInMonth = remember(selectedMonth, selectedYear) {
        val tempCal = Calendar.getInstance().apply {
            set(Calendar.MONTH, selectedMonth)
            set(Calendar.YEAR, selectedYear)
        }
        tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    if (selectedDay > daysInMonth) {
        selectedDay = daysInMonth
    }

    val days = (1..daysInMonth).toList()
    val months = (0..11).toList()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = ((currentYear - 100)..currentYear).toList().reversed()

    val monthNames = listOf("Январь", "Февраль", "Март", "Апрель", "Май", "Июнь", "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выберите дату") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPicker(
                        items = days,
                        selectedItem = selectedDay,
                        onItemSelected = { selectedDay = it },
                        modifier = Modifier.weight(1f)
                    )
                    
                    WheelPicker(
                        items = months,
                        selectedItem = selectedMonth,
                        onItemSelected = { selectedMonth = it },
                        itemLabel = { monthNames[it] },
                        modifier = Modifier.weight(1.5f)
                    )

                    if (hasYear) {
                        WheelPicker(
                            items = years,
                            selectedItem = selectedYear,
                            onItemSelected = { selectedYear = it },
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = !hasYear,
                        onCheckedChange = { onHasYearChange(!it) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Text("Не знаю год рождения", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, selectedDay)
                    set(Calendar.MONTH, selectedMonth)
                    if (hasYear) {
                        set(Calendar.YEAR, selectedYear)
                    } else {
                        set(Calendar.YEAR, 2004)
                    }
                }
                onDateSelected(newCal.timeInMillis)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
