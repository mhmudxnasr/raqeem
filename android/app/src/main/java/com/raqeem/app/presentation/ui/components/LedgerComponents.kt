package com.raqeem.app.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.raqeem.app.domain.model.Currency
import com.raqeem.app.domain.model.LedgerEntry
import com.raqeem.app.domain.model.TransactionType
import com.raqeem.app.presentation.ui.theme.AppColors
import com.raqeem.app.presentation.ui.theme.AppTypography
import com.raqeem.app.presentation.ui.theme.MonoFamily
import com.raqeem.app.util.formatAmount
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class LedgerSection(
    val label: String,
    val entries: List<LedgerEntry>,
)

fun groupLedgerEntries(entries: List<LedgerEntry>): List<LedgerSection> {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val yesterday = today.minus(DatePeriod(days = 1))

    return entries
        .groupBy { entry ->
            when (entry.date) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> formatSectionDate(entry.date)
            }
        }
        .map { (label, sectionEntries) -> LedgerSection(label, sectionEntries) }
}

@Composable
fun LedgerEntryCard(
    entry: LedgerEntry,
    onOpenTransaction: ((String) -> Unit)? = null,
    onEditTransaction: ((String) -> Unit)? = null,
    onDeleteTransaction: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val clickAction = when (entry) {
        is LedgerEntry.TransactionEntry -> onOpenTransaction?.let { { it(entry.transaction.id) } }
        is LedgerEntry.TransferEntry -> null
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (clickAction != null) Modifier.clickable(onClick = clickAction) else Modifier
            )
            .padding(vertical = 12.dp)
    ) {
        when (entry) {
            is LedgerEntry.TransactionEntry -> TransactionLedgerContent(
                entry = entry,
                onEditTransaction = onEditTransaction,
                onDeleteTransaction = onDeleteTransaction,
            )
            is LedgerEntry.TransferEntry -> TransferLedgerContent(entry)
        }
    }
}

@Composable
private fun TransactionLedgerContent(
    entry: LedgerEntry.TransactionEntry,
    onEditTransaction: ((String) -> Unit)? = null,
    onDeleteTransaction: ((String) -> Unit)? = null,
) {
    val transaction = entry.transaction
    var menuExpanded by remember(transaction.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedgerIcon(
                iconTint = transaction.type.toLedgerColor(),
                background = transaction.type.toLedgerColor().copy(alpha = 0.12f),
                image = {
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = null,
                        tint = transaction.type.toLedgerColor(),
                    )
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = transaction.note?.takeIf { it.isNotBlank() }
                        ?: transaction.category?.name
                        ?: transaction.type.name.lowercase().replaceFirstChar { char ->
                            char.titlecase(Locale.getDefault())
                        },
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = listOfNotNull(
                        transaction.account?.name,
                        transaction.category?.name,
                        formatCompactDate(transaction.date),
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AmountText(
                amountCents = transaction.amountCents,
                currency = transaction.currency,
                transactionType = transaction.type,
                style = AppTypography.largeAmount,
            )
            if (onEditTransaction != null || onDeleteTransaction != null) {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Manage transaction",
                            tint = AppColors.textSecondary,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (onEditTransaction != null) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditTransaction(transaction.id)
                                },
                            )
                        }
                        if (onDeleteTransaction != null) {
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = AppColors.negative,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteTransaction(transaction.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferLedgerContent(
    entry: LedgerEntry.TransferEntry,
) {
    val transfer = entry.transfer
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LedgerIcon(
                iconTint = AppColors.purple300,
                background = AppColors.purple500.copy(alpha = 0.12f),
                image = {
                    Icon(
                        imageVector = Icons.Rounded.CompareArrows,
                        contentDescription = null,
                        tint = AppColors.purple300,
                    )
                },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = transfer.note?.takeIf { it.isNotBlank() } ?: "Transfer",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = buildString {
                        append(transfer.fromAccount?.name ?: "From")
                        append(" → ")
                        append(transfer.toAccount?.name ?: "To")
                        transfer.goal?.name?.let { append(" • Goal: $it") }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary,
                )
                if (transfer.fromCurrency != transfer.toCurrency) {
                    Text(
                        text = buildAnnotatedString {
                            append("Rate ")
                            withStyle(
                                SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")
                            ) {
                                append(transfer.exchangeRate.toString())
                            }
                            append(" • Receive ")
                            withStyle(
                                SpanStyle(fontFamily = MonoFamily, fontFeatureSettings = "tnum")
                            ) {
                                append(transfer.toAmountCents.formatAmount(transfer.toCurrency))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textMuted,
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = transfer.fromAmountCents.formatAmount(transfer.fromCurrency, showSign = false),
                style = AppTypography.largeAmount,
                color = AppColors.textPrimary,
            )
            Text(
                text = formatCompactDate(transfer.date),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textMuted,
            )
        }
    }
}

@Composable
private fun LedgerIcon(
    iconTint: Color,
    background: Color,
    image: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        image()
    }
}

@Composable
fun MiniSparkline(
    points: List<Int>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val maxMagnitude = points.maxOf { kotlin.math.abs(it).coerceAtLeast(1) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { point ->
            val heightFraction = (kotlin.math.abs(point).toFloat() / maxMagnitude.toFloat()).coerceIn(0.12f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((32 * heightFraction).dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        if (point >= 0) AppColors.positive.copy(alpha = 0.75f)
                        else AppColors.negative.copy(alpha = 0.75f),
                    ),
            )
        }
    }
}

private fun TransactionType.toLedgerColor(): Color {
    return when (this) {
        TransactionType.INCOME -> AppColors.positive
        TransactionType.EXPENSE -> AppColors.negative
    }
}

private fun formatSectionDate(date: LocalDate): String {
    val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
    return javaDate.format(DateTimeFormatter.ofPattern("MMMM d", Locale.US))
}

private fun formatCompactDate(date: LocalDate): String {
    val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
    return javaDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
}
