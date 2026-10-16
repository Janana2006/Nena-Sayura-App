package lk.nenasayura.app.ui.student.teachercontent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import lk.nenasayura.app.data.model.TeacherProfile
import lk.nenasayura.app.ui.common.ErrorView
import lk.nenasayura.app.ui.common.LoadingView
import lk.nenasayura.app.ui.theme.NenaIconColors
import lk.nenasayura.app.ui.theme.NenaPrimary
import lk.nenasayura.app.ui.theme.NenaPrimaryContainer
import lk.nenasayura.app.ui.theme.NenaSecondaryContainer

private val SuccessGreen = Color(0xFF2E7D32)
private val LineGrey = Color(0xFFE2E5EE)

/**
 * Payment dashboard for one teacher:
 * review the fee -> choose a method -> "Confirm payment" -> success -> back to the materials.
 */
@Composable
fun TeacherPaymentScreen(
    teacherId: String,
    onBack: () -> Unit,
    /** Called after a successful payment (or if the student had already paid). */
    onPaid: () -> Unit,
    viewModel: TeacherPaymentViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(teacherId) { viewModel.load(teacherId) }

    // Do not let the student leave while the payment is being processed.
    // On the success page, Back goes to the materials too.
    BackHandler(enabled = state.step != PayStep.REVIEW) {
        if (state.step == PayStep.SUCCESS) onPaid()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        val teacher = state.teacher

        when {
            state.step == PayStep.PROCESSING -> ProcessingView(modifier)

            state.step == PayStep.SUCCESS -> SuccessView(
                teacher = teacher,
                methodLabel = state.method.label,
                onContinue = onPaid,
                modifier = modifier
            )

            state.loading -> Column(modifier = modifier) {
                ScreenHeader(title = "Payment", onBack = onBack)
                LoadingView(modifier = Modifier.weight(1f))
            }

            state.errorMessage != null -> Column(modifier = modifier) {
                ScreenHeader(title = "Payment", onBack = onBack)
                ErrorView(
                    message = state.errorMessage ?: "",
                    onRetry = { viewModel.load(teacherId) },
                    modifier = Modifier.weight(1f)
                )
            }

            teacher == null -> Column(modifier = modifier) {
                ScreenHeader(title = "Payment", onBack = onBack)
                ErrorView(
                    message = "Teacher not found.",
                    onRetry = { viewModel.load(teacherId) },
                    modifier = Modifier.weight(1f)
                )
            }

            state.alreadyPaid -> InfoView(
                message = "You have already paid ${teacher.name.ifBlank { "this teacher" }}. All materials are unlocked.",
                buttonLabel = "View materials",
                onBack = onBack,
                onClick = onPaid,
                modifier = modifier
            )

            !teacher.isPaidTeacher -> InfoView(
                message = "This teacher is free. No payment is needed.",
                buttonLabel = "View materials",
                onBack = onBack,
                onClick = onPaid,
                modifier = modifier
            )

            else -> ReviewView(
                teacher = teacher,
                selected = state.method,
                payError = state.payError,
                onBack = onBack,
                onMethod = viewModel::onMethod,
                onConfirm = viewModel::confirm,
                modifier = modifier
            )
        }
    }
}

// ---------------------------------------------------------------- dashboard

@Composable
private fun ReviewView(
    teacher: TeacherProfile,
    selected: PayMethod,
    payError: String?,
    onBack: () -> Unit,
    onMethod: (PayMethod) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // ---- header: big amount
            GradientHero(onBack = onBack, bottomPadding = 56.dp) {
                Text(
                    text = "Unlock ${teacher.name.ifBlank { "your teacher" }}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatFee(teacher.fee),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Secure checkout",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            // ---- cards floating over the header
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .pullUp(36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // who you are paying
                DashCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TeacherAvatar(name = teacher.name, size = 54)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = teacher.name.ifBlank { "Teacher" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (teacher.streamName.isNotBlank()) {
                                Text(
                                    text = teacher.streamName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NenaPrimary
                                )
                            }
                            if (teacher.subjectNames.isNotEmpty()) {
                                Text(
                                    text = teacher.subjectNames.joinToString("  \u2022  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // what you get
                DashCard {
                    Text(
                        text = "What you get",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Benefit("All paid notes, tutes and model papers")
                    Benefit("Answer sheets and video lessons")
                    Benefit("New paid uploads from this teacher")
                }

                // method
                DashCard {
                    Text(
                        text = "Payment method",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PayMethod.entries.forEach { method ->
                            MethodTile(
                                method = method,
                                selected = method == selected,
                                onClick = { onMethod(method) }
                            )
                        }
                    }
                }

                if (payError != null) {
                    Text(
                        text = payError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // ---- bottom bar: total + confirm
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 14.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatFee(teacher.fee),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm payment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DashCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) { content() }
    }
}

@Composable
private fun Benefit(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(NenaSecondaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, null, tint = Color(0xFF00796B), modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MethodTile(method: PayMethod, selected: Boolean, onClick: () -> Unit) {
    val icon: ImageVector = when (method) {
        PayMethod.CARD -> Icons.Filled.CreditCard
        PayMethod.BANK -> Icons.Filled.AccountBalance
        PayMethod.WALLET -> Icons.Filled.AccountBalanceWallet
    }
    val palette = when (method) {
        PayMethod.CARD -> NenaIconColors.Papers
        PayMethod.BANK -> NenaIconColors.Timetable
        PayMethod.WALLET -> NenaIconColors.MarkingSchemes
    }
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) NenaPrimaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) NenaPrimary else LineGrey)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBadge(icon, palette, size = 42)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = method.hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

// ---------------------------------------------------------------- other states

@Composable
private fun ProcessingView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(NenaPrimaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = NenaPrimary)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Processing your payment...",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Please do not close the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessView(
    teacher: TeacherProfile?,
    methodLabel: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Go to the materials automatically; the button does the same, so guard against a double call
    var done by remember { mutableStateOf(false) }
    val goOnce = {
        if (!done) {
            done = true
            onContinue()
        }
    }
    LaunchedEffect(Unit) {
        delay(3000)
        goOnce()
    }

    // the tick pops in
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val tickScale by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tick"
    )

    Column(modifier = modifier.fillMaxSize()) {
        GradientHero(onBack = null, bottomPadding = 64.dp) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .scale(tickScale)
                        .size(92.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(64.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Payment successful!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "All paid materials are now unlocked",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }

        // receipt
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .pullUp(36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashCard {
                Text(
                    text = "Receipt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (teacher != null) {
                    ReceiptRow("Teacher", teacher.name.ifBlank { "-" })
                    ReceiptRow("Amount", formatFee(teacher.fee), bold = true)
                }
                ReceiptRow("Method", methodLabel)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconPill("Paid", Color(0xFFDFF3E1), SuccessGreen, Icons.Filled.CheckCircle)
                }
            }

            Button(
                onClick = goOnce,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("View materials", fontWeight = FontWeight.Bold)
            }
            Text(
                text = "Taking you to the materials...",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, bold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun InfoView(
    message: String,
    buttonLabel: String,
    onBack: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = "Payment", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(NenaPrimaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CheckCircle, null, tint = NenaPrimary, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = onClick, shape = RoundedCornerShape(14.dp)) { Text(buttonLabel) }
        }
    }
}