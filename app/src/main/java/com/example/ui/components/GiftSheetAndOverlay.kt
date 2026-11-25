package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.GiftBurstEvent
import com.example.model.GiftCatalogItem
import com.example.model.TaviqoCatalog
import com.example.ui.theme.CoinGold
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalBlueBorder
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.RoyalBlueElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

fun giftIcon(giftId: String): ImageVector = when (giftId) {
    "rose" -> Icons.Default.LocalFlorist
    "heart" -> Icons.Default.Favorite
    "diamond" -> Icons.Default.Diamond
    "crown" -> Icons.Default.EmojiEvents
    "rocket" -> Icons.Default.RocketLaunch
    "taviqo_nova" -> Icons.Default.WorkspacePremium
    else -> Icons.Default.AutoAwesome
}

fun giftTint(giftId: String): Color = when (giftId) {
    "rose" -> Color(0xFFFF5C8A)
    "heart" -> LiveCrimson
    "diamond" -> NeonCyan
    "crown" -> CoinGold
    "rocket" -> ElectricCyan
    "taviqo_nova" -> Color(0xFF7DD3FC)
    else -> NeonCyan
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendGiftBottomSheet(
    recipientUsername: String,
    coinBalance: Long,
    onSelectGift: (GiftCatalogItem) -> Unit,
    onOpenWallet: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Enviar regalo a @$recipientUsername",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "70% directo al creador • 30% plataforma",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RoyalBlueElevated,
                    border = BorderStroke(1.dp, CoinGold.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable {
                        onDismiss()
                        onOpenWallet()
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Saldo",
                            tint = CoinGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$coinBalance",
                            style = MaterialTheme.typography.labelLarge,
                            color = CoinGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
                modifier = Modifier.height(270.dp)
            ) {
                items(TaviqoCatalog.gifts, key = { it.id }) { gift ->
                    val canAfford = coinBalance >= gift.coinCost
                    val tint = giftTint(gift.id)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (gift.isExclusive) RoyalBlueElevated else RoyalBlueDark,
                        border = BorderStroke(
                            width = if (gift.isExclusive) 1.5.dp else 1.dp,
                            color = if (canAfford) tint.copy(alpha = 0.65f) else RoyalBlueBorder.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .testTag("gift_item_${gift.id}")
                            .clickable {
                                onSelectGift(gift)
                                onDismiss()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(tint.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = giftIcon(gift.id),
                                    contentDescription = gift.name,
                                    tint = tint,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = gift.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "${gift.coinCost} mon.",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (canAfford) CoinGold else LiveCrimson
                            )
                        }
                    }
                }
            }

            if (coinBalance < 5L) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onOpenWallet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Adquirir monedas en Billetera")
                }
            }
        }
    }
}

@Composable
fun GiftBurstOverlay(
    burst: GiftBurstEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gift_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    AnimatedVisibility(
        visible = burst != null,
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit = fadeOut() + scaleOut(targetScale = 1.15f),
        modifier = modifier
    ) {
        if (burst != null) {
            val tint = giftTint(burst.gift.id)
            Box(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .scale(pulseScale)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(RoyalBlueDark.copy(alpha = 0.95f), DeepBlack.copy(alpha = 0.95f))
                        )
                    )
                    .border(2.dp, tint, RoundedCornerShape(28.dp))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = giftIcon(burst.gift.id),
                            contentDescription = burst.gift.name,
                            tint = tint,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "@${burst.senderUsername} envió ${burst.gift.name}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Para @${burst.recipientUsername} • +${burst.gift.creatorNetCoins} monedas netas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = tint
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar animación",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
