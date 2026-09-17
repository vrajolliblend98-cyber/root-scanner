package com.root.scanner.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.root.scanner.model.Device
import com.root.scanner.model.PortResult
import com.root.scanner.ui.theme.RootGreen
import com.root.scanner.ui.theme.TextSecondary
import com.root.scanner.viewmodel.ScanViewModel

@Composable
fun DeviceDetailScreen(
    device: Device,
    viewModel: ScanViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    // On récupère la version la plus à jour de l'appareil depuis le state
    val liveDevice = state.devices.find { it.ip == device.ip } ?: device

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(liveDevice.hostname ?: liveDevice.ip) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.deepScanHost(liveDevice.ip) },
                containerColor = RootGreen
            ) {
                Text("Scan complet (65535 ports)")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            InfoRow("Adresse IP", liveDevice.ip)
            liveDevice.mac?.let { InfoRow("Adresse MAC", it) }
            liveDevice.latencyMs?.let { InfoRow("Latence", "${it} ms") }
            InfoRow("OS estimé", liveDevice.guessedOs ?: "Inconnu")

            Spacer(Modifier.height(16.dp))
            Text("Ports ouverts (${liveDevice.openPorts.size})", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            if (state.isScanning) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = RootGreen
                )
                Spacer(Modifier.height(8.dp))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(liveDevice.openPorts) { port -> PortRow(port) }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PortRow(port: PortResult) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = RootGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Port ${port.port} — ${port.service ?: "Inconnu"}", fontWeight = FontWeight.SemiBold)
                port.banner?.let {
                    Text(it, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                }
            }
        }
    }
}
