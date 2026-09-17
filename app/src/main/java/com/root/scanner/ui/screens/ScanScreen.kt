package com.root.scanner.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.root.scanner.model.Device
import com.root.scanner.ui.theme.RootGreen
import com.root.scanner.ui.theme.TextSecondary
import com.root.scanner.viewmodel.ScanViewModel

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onDeviceClick: (Device) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { RootTopBar() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (state.isScanning) viewModel.stopScan() else viewModel.startNetworkScan()
                },
                containerColor = RootGreen,
                contentColor = Color.Black
            ) {
                Icon(
                    if (state.isScanning) Icons.Filled.Stop else Icons.Filled.Radar,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(if (state.isScanning) "Arrêter" else "Scanner le réseau")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            AnimatedVisibility(visible = state.isScanning || state.statusText.isNotEmpty()) {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        state.statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    if (state.isScanning) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth(),
                            color = RootGreen
                        )
                    }
                }
            }

            if (state.devices.isEmpty() && !state.isScanning) {
                EmptyState()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.devices) { device ->
                        DeviceCard(device = device, onClick = { onDeviceClick(device) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun RootTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, contentDescription = null, tint = RootGreen)
                Spacer(Modifier.width(8.dp))
                Text("Root", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.WifiFind,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextSecondary
        )
        Spacer(Modifier.height(16.dp))
        Text("Aucun scan effectué", color = TextSecondary)
        Text("Appuie sur \"Scanner le réseau\" pour commencer", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DeviceCard(device: Device, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(RootGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Devices, contentDescription = null, tint = RootGreen)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.hostname ?: device.ip, fontWeight = FontWeight.SemiBold)
                Text(device.ip, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                if (device.openPorts.isNotEmpty()) {
                    Text(
                        "${device.openPorts.size} port(s) ouvert(s) · ${device.guessedOs ?: ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RootGreen
                    )
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}
