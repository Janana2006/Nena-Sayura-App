package lk.nenasayura.app.ui.student.reminders



import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import lk.nenasayura.app.core.reminders.ReminderNotifications
import lk.nenasayura.app.core.reminders.ReminderScheduler
import lk.nenasayura.app.data.model.REMINDER_LEAD_OPTIONS
import lk.nenasayura.app.data.model.UpcomingReminder
import lk.nenasayura.app.ui.common.NenaTopBar
import lk.nenasayura.app.ui.student.profile.PickerField
import lk.nenasayura.app.ui.student.timetable.formatClock
import lk.nenasayura.app.ui.student.timetable.formatDayLong
import lk.nenasayura.app.ui.theme.IconPalette
import lk.nenasayura.app.ui.theme.NenaIconColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import lk.nenasayura.app.ui.common.LoadingView

private val DAILY_PRESETS = listOf(7 * 60, 21 * 60) // 7:00 am, 9:00 pm

/**
 * Study reminders: master switch, lead time before sessions, a daily summary time,
 * and the list of reminders that are waiting to fire.
 */
@Composable
fun RemindersScreen(
    onBack: () -> Unit,
    onOpenTimetable: () -> Unit,
    viewModel: RemindersViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.prefs
    val context = LocalContext.current

    var notificationsAllowed by remember { mutableStateOf(ReminderNotifications.areNotificationsAllowed(context)) }
    var exactAllowed by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }
    var showTimePicker by remember { mutableStateOf(false) }

    // The student may have changed the permissions in Settings while the app was in the background
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        notificationsAllowed = ReminderNotifications.areNotificationsAllowed(context)
        exactAllowed = ReminderScheduler.canScheduleExactAlarms(context)
        viewModel.refresh()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notificationsAllowed = ReminderNotifications.areNotificationsAllowed(context)
        if (notificationsAllowed) {
            viewModel.setEnabled(true)
        } else {
            Toast.makeText(
                context,
                "Allow notifications to get study reminders.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val onToggleAll: (Boolean) -> Unit = { turnOn ->
        when {
            !turnOn -> viewModel.setEnabled(false)
            notificationsAllowed -> viewModel.setEnabled(true)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else -> ReminderNotifications.openAppNotificationSettings(context)
        }
    }

    Scaffold(
        topBar = { NenaTopBar(title = "Reminders", onBack = onBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (!state.isLoaded) {
            LoadingView(Modifier.padding(innerPadding))
            return@Scaffold
        }
        Column(modifier = Modifier.padding(innerPadding)) {

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ---------- permission notices ----------
                if (!notificationsAllowed) {
                    item(key = "notice_notifications") {
                        NoticeCard(
                            text = "Notifications are off for Nena Sayura, so reminders cannot be shown.",
                            actionLabel = "Open settings",
                            onAction = { ReminderNotifications.openAppNotificationSettings(context) },
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                if (prefs.enabled && !exactAllowed) {
                    item(key = "notice_exact") {
                        NoticeCard(
                            text = "Exact alarms are not allowed, so a reminder can come a few minutes late.",
                            actionLabel = "Allow exact alarms",
                            onAction = { ReminderScheduler.openExactAlarmSettings(context) },
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ---------- master switch ----------
                item(key = "master") {
                    SettingsCard {
                        SwitchRow(
                            icon = Icons.Filled.Notifications,
                            palette = NenaIconColors.Reminders,
                            title = "Study reminders",
                            subtitle = if (prefs.enabled) "Reminders are on" else "Turn on to get study reminders",
                            checked = prefs.enabled,
                            enabled = true,
                            onCheckedChange = onToggleAll
                        )
                    }
                }

                // ---------- before sessions ----------
                item(key = "sessions") {
                    SettingsCard {
                        SwitchRow(
                            icon = Icons.Filled.Alarm,
                            palette = NenaIconColors.Timetable,
                            title = "Before study sessions",
                            subtitle = "A reminder before each session in your Timetable",
                            checked = prefs.sessionRemindersEnabled,
                            enabled = prefs.enabled,
                            onCheckedChange = viewModel::setSessionRemindersEnabled
                        )
                        if (prefs.enabled && prefs.sessionRemindersEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            val options = REMINDER_LEAD_OPTIONS.map { leadLabel(it) }
                            PickerField(
                                label = "Remind me",
                                value = leadLabel(prefs.leadMinutes),
                                options = options,
                                onSelect = { picked ->
                                    val index = options.indexOf(picked)
                                    if (index >= 0) viewModel.setLeadMinutes(REMINDER_LEAD_OPTIONS[index])
                                }
                            )
                        }
                    }
                }

                // ---------- daily summary ----------
                item(key = "daily") {
                    SettingsCard {
                        SwitchRow(
                            icon = Icons.Filled.Today,
                            palette = NenaIconColors.Progress,
                            title = "Daily summary",
                            subtitle = "Your study plan or progress for the day",
                            checked = prefs.dailyEnabled,
                            enabled = prefs.enabled,
                            onCheckedChange = viewModel::setDailyEnabled
                        )
                        if (prefs.enabled && prefs.dailyEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Remind me at",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DAILY_PRESETS.forEach { minute ->
                                    FilterChip(
                                        selected = prefs.dailyMinuteOfDay == minute,
                                        onClick = { viewModel.setDailyTime(minute) },
                                        label = { Text(formatClock(minute)) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose time: ${formatClock(prefs.dailyMinuteOfDay)}")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Before 3 pm you get today's plan. Later you get today's progress.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ---------- upcoming reminders ----------
                item(key = "upcoming_title") {
                    Text(
                        text = "Upcoming reminders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                when {
                    !prefs.enabled || !prefs.sessionRemindersEnabled -> item(key = "upcoming_off") {
                        InfoCard(text = "Turn on study reminders and session reminders to see them here.")
                    }

                    state.upcoming.isEmpty() -> item(key = "upcoming_empty") {
                        InfoCard(
                            text = if (state.hasTimetableData) {
                                "No planned sessions in the next 2 weeks have a reminder waiting."
                            } else {
                                "You have no timetable sessions yet. Create your timetable and " +
                                        "reminders are set for every session."
                            },
                            actionLabel = if (state.hasTimetableData) null else "Open Timetable",
                            onAction = onOpenTimetable
                        )
                    }

                    else -> items(items = state.upcoming, key = { it.session.id }) { reminder ->
                        UpcomingReminderCard(reminder)
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        ReminderTimeDialog(
            initialMinuteOfDay = prefs.dailyMinuteOfDay,
            onDismiss = { showTimePicker = false },
            onConfirm = { minute ->
                viewModel.setDailyTime(minute)
                showTimePicker = false
            }
        )
    }
}

// ---------- building blocks (same card style as the Timetable and Home screens) ----------

private fun leadLabel(minutes: Int) = "$minutes minutes before"

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun IconCircle(icon: ImageVector, palette: IconPalette) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(palette.container, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = palette.tint)
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    palette: IconPalette,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconCircle(icon = icon, palette = palette)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun NoticeCard(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.NotificationsOff,
                    contentDescription = null,
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun InfoCard(
    text: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}

@Composable
private fun UpcomingReminderCard(reminder: UpcomingReminder) {
    val session = reminder.session
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconCircle(icon = Icons.Filled.Alarm, palette = NenaIconColors.Reminders)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Session: ${formatClock(session.startMinute)} • ${dayLabel(session.date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Reminder at ${formatClock(minuteOfDay(reminder.triggerMillis))} " +
                            "(${reminder.leadMinutes} min before)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimeDialog(
    initialMinuteOfDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (minuteOfDay: Int) -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = initialMinuteOfDay / 60,
        initialMinute = initialMinuteOfDay % 60,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily reminder time") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TimePicker(state = pickerState)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour * 60 + pickerState.minute) }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------- small formatting helpers ----------

private fun dayLabel(date: String): String {
    val day = LocalDate.parse(date)
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> formatDayLong(day)
    }
}

private fun minuteOfDay(millis: Long): Int {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return time.hour * 60 + time.minute
}