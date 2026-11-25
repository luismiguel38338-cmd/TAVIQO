package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.model.CoinPackage
import com.example.model.CoinPurchase
import com.example.model.GiftTransaction
import com.example.model.TaviqoCatalog
import com.example.model.PayoutRequest
import com.example.model.PrivateAccount
import com.example.ui.components.TaviqoLogoMark
import com.example.ui.components.giftIcon
import com.example.ui.components.giftTint
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoinGold
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalBlueBorder
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GiftsAndWalletScreen(
    currentUserId: String,
    walletState: UiState<PrivateAccount?>,
    giftHistoryState: UiState<List<GiftTransaction>>,
    purchasesState: UiState<List<CoinPurchase>>,
    payoutsState: UiState<List<PayoutRequest>>,
    onCreateCoinOrder: (CoinPackage, String) -> Unit,
    onRequestPayout: (Long, String, String) -> Unit
) {
    val wallet = (walletState as? UiState.Success)?.data
    val coinBalance = wallet?.coinBalance ?: 0L
    val creatorEarnings = wallet?.creatorEarningsCoins ?: 0L
    val gifts = (giftHistoryState as? UiState.Success)?.data ?: emptyList()
    val purchases = (purchasesState as? UiState.Success)?.data ?: emptyList()
    val payouts = (payoutsState as? UiState.Success)?.data ?: emptyList()

    var showPayoutDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TaviqoLogoMark(size = 30.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Tienda y Regalos TAVIQO", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            }
            Text(
                "Transacciones validadas en servidor. Sin compras ni ganancias simuladas.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(listOf(RoyalBlueElevated.copy(alpha = 0.7f), ObsidianSurface))
                        )
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Saldo de Monedas", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = CoinGold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$coinBalance",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = CoinGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Ganancias de Creador", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$creatorEarnings",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showPayoutDialog = true },
                        enabled = creatorEarnings >= 100L,
                        border = BorderStroke(1.dp, if (creatorEarnings >= 100L) NeonCyan else RoyalBlueBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("request_payout_button")
                    ) {
                        Icon(Icons.Default.Payments, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (creatorEarnings >= 100L) "Solicitar retiro (mín. 100 mon.)" else "Retiro disponible desde 100 monedas",
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        item {
            Text("Catálogo Oficial de Regalos", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        items(TaviqoCatalog.gifts, key = { it.id }) { gift ->
            val tint = giftTint(gift.id)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = RoyalBlueDark,
                border = BorderStroke(1.dp, if (gift.isExclusive) tint else RoyalBlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(giftIcon(gift.id), contentDescription = gift.name, tint = tint)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(gift.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(gift.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        Text(
                            "Creador: ${gift.creatorNetCoins} • Plataforma: ${gift.platformFeeCoins}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = RoyalBlueElevated) {
                        Text(
                            text = "${gift.coinCost} mon.",
                            style = MaterialTheme.typography.labelLarge,
                            color = CoinGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Text("Paquetes de Monedas (Pago Verificado)", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }

        items(TaviqoCatalog.coinPackages, key = { it.id }) { pkg ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ObsidianSurface,
                border = BorderStroke(1.dp, RoyalBlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${pkg.title} • ${pkg.coinsAmount} monedas", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            "${pkg.bonusLabel} • $${"%.2f".format(pkg.priceUsdCents / 100.0)} USD",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoinGold
                        )
                    }
                    Button(
                        onClick = { onCreateCoinOrder(pkg, "google_play_billing") },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack),
                        modifier = Modifier.testTag("buy_coins_${pkg.id}")
                    ) {
                        Text("Adquirir")
                    }
                }
            }
        }

        if (purchases.isNotEmpty()) {
            item {
                Text("Órdenes de Monedas", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
            items(purchases, key = { it.id }) { order ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RoyalBlueDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${order.coinsAmount} monedas (${order.provider})", style = MaterialTheme.typography.titleMedium)
                            Text("Estado: ${order.status}", style = MaterialTheme.typography.bodyMedium, color = AmberWarning)
                        }
                        Text("$${"%.2f".format(order.priceUsdCents / 100.0)}", color = CoinGold)
                    }
                }
            }
        }

        if (payouts.isNotEmpty()) {
            item {
                Text("Solicitudes de Retiro", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
            items(payouts, key = { it.id }) { req ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RoyalBlueDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${req.coinsAmount} monedas • ${req.provider}", style = MaterialTheme.typography.titleMedium)
                            Text("${req.destinationHint} (${req.status})", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                        Text("$${"%.2f".format(req.estimatedUsdCents / 100.0)}", color = EmeraldSuccess)
                    }
                }
            }
        }

        item {
            Text("Historial de Regalos (${gifts.size})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        }
        if (gifts.isEmpty()) {
            item {
                Text(
                    "Aún no has enviado ni recibido regalos virtuales.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            items(gifts, key = { it.id }) { tx ->
                val isSender = tx.senderId == currentUserId
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = RoyalBlueDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = if (isSender) AmberWarning else EmeraldSuccess)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSender) "Enviaste ${tx.giftName} a @${tx.recipientUsername}"
                                else "Recibiste ${tx.giftName} de @${tx.senderUsername}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Contexto: ${tx.contextType} • Comisión plataforma: ${tx.platformFeeCoins} mon.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Text(
                            text = if (isSender) "-${tx.coinCost}" else "+${tx.creatorNetCoins}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSender) AmberWarning else EmeraldSuccess
                        )
                    }
                }
            }
        }
    }

    if (showPayoutDialog) {
        var amountText by remember { mutableStateOf("100") }
        var provider by remember { mutableStateOf("Stripe Connect") }
        var destination by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPayoutDialog = false },
            containerColor = ObsidianSurface,
            titleContentColor = TextPrimary,
            title = { Text("Solicitar Retiro de Creador") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Monedas a retirar (mín. 100)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = provider,
                        onValueChange = { provider = it },
                        label = { Text("Proveedor autorizado") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Referencia de cuenta / Payoneer / Stripe") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toLongOrNull() ?: 0L
                        onRequestPayout(amt, provider, destination)
                        showPayoutDialog = false
                    },
                    enabled = (amountText.toLongOrNull() ?: 0L) >= 100L && destination.trim().length >= 3,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack)
                ) {
                    Text("Confirmar solicitud")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPayoutDialog = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}
