package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.core.UiState
import com.example.model.Clip
import com.example.model.CoinPurchase
import com.example.model.LiveStream
import com.example.model.ModerationReport
import com.example.model.UserProfile
import com.example.ui.theme.CoinGold
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiveCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.RoyalBlueBorder
import com.example.ui.theme.RoyalBlueDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AdminPanelScreen(
    creators: List<UserProfile>,
    reportsState: UiState<List<ModerationReport>>,
    clipsState: UiState<List<Clip>>,
    streamsState: UiState<List<LiveStream>>,
    purchasesState: UiState<List<CoinPurchase>>,
    onModerateClip: (clipId: String, status: String) -> Unit,
    onTerminateStream: (streamId: String) -> Unit,
    onUpdateUserFlags: (uid: String, verified: Boolean, suspended: Boolean) -> Unit,
    onResolveReport: (reportId: String, status: String) -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val reports = (reportsState as? UiState.Success)?.data ?: emptyList()
    val clips = (clipsState as? UiState.Success)?.data ?: emptyList()
    val streams = (streamsState as? UiState.Success)?.data ?: emptyList()
    val purchases = (purchasesState as? UiState.Success)?.data ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
            }
            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = CoinGold)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Panel Administrativo", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }

        // Real platform telemetry cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KpiCard("Usuarios", "${creators.size}", Modifier.weight(1f))
            KpiCard("Clips", "${clips.size}", Modifier.weight(1f))
            KpiCard("Directos", "${streams.count { it.status == "live" }}", Modifier.weight(1f))
            KpiCard("Denuncias", "${reports.count { it.status == "open" }}", Modifier.weight(1f))
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = NeonCyan,
            edgePadding = 0.dp,
            indicator = { positions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(positions[selectedTab]),
                    color = NeonCyan
                )
            }
        ) {
            listOf("Denuncias", "Vídeos", "Directos", "Usuarios", "Transacciones").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    if (reports.isEmpty()) {
                        item { Text("No hay denuncias registradas.", color = TextMuted) }
                    } else {
                        items(reports, key = { it.id }) { report ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = RoyalBlueDark,
                                border = BorderStroke(1.dp, RoyalBlueBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${report.targetType}: ${report.targetId}", style = MaterialTheme.typography.titleMedium, color = NeonCyan)
                                    Text("Motivo: ${report.reason} • Estado: ${report.status}", color = TextPrimary)
                                    if (report.details.isNotBlank()) Text(report.details, color = TextSecondary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onResolveReport(report.id, "resolved") },
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = DeepBlack)
                                        ) { Text("Resolver") }
                                        OutlinedButton(onClick = { onResolveReport(report.id, "dismissed") }) {
                                            Text("Descartar", color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    items(clips, key = { it.id }) { clip ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = RoyalBlueDark,
                            border = BorderStroke(1.dp, RoyalBlueBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("@${clip.authorUsername} • ${clip.status} (IA: ${clip.aiModerationStatus})", color = NeonCyan)
                                Text(clip.caption, color = TextPrimary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onModerateClip(clip.id, "active") },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = DeepBlack)
                                    ) { Text("Aprobar") }
                                    Button(
                                        onClick = { onModerateClip(clip.id, "removed") },
                                        colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = TextPrimary)
                                    ) { Text("Retirar") }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    items(streams, key = { it.id }) { stream ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = RoyalBlueDark,
                            border = BorderStroke(1.dp, RoyalBlueBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${stream.title} (${stream.status})", color = TextPrimary)
                                    Text("@${stream.hostUsername} • ${stream.viewerCount} espectadores", color = TextSecondary)
                                }
                                if (stream.status == "live") {
                                    Button(
                                        onClick = { onTerminateStream(stream.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = LiveCrimson, contentColor = TextPrimary)
                                    ) { Text("Cortar") }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    items(creators, key = { it.userId }) { user ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = RoyalBlueDark,
                            border = BorderStroke(1.dp, RoyalBlueBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${user.displayName} (@${user.username})", color = TextPrimary)
                                Text("Verificada: ${user.verified} • Suspendida: ${user.suspended}", color = TextSecondary)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onUpdateUserFlags(user.userId, !user.verified, user.suspended) }) {
                                        Text(if (user.verified) "Quitar verif." else "Verificar", color = NeonCyan)
                                    }
                                    Button(
                                        onClick = { onUpdateUserFlags(user.userId, user.verified, !user.suspended) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (user.suspended) EmeraldSuccess else LiveCrimson,
                                            contentColor = DeepBlack
                                        )
                                    ) {
                                        Text(if (user.suspended) "Reactivar" else "Suspender")
                                    }
                                }
                            }
                        }
                    }
                }
                4 -> {
                    if (purchases.isEmpty()) {
                        item { Text("No hay órdenes de compra registradas.", color = TextMuted) }
                    } else {
                        items(purchases, key = { it.id }) { p ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = RoyalBlueDark,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text("Usuario: ${p.userId} • ${p.coinsAmount} mon.", color = TextPrimary)
                                    Text("Estado: ${p.status} • Proveedor: ${p.provider}", color = CoinGold)
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
private fun KpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = RoyalBlueDark,
        border = BorderStroke(1.dp, RoyalBlueBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = NeonCyan)
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}
