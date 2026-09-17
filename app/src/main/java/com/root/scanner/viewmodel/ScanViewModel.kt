package com.root.scanner.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.root.scanner.model.Device
import com.root.scanner.model.ScanState
import com.root.scanner.scanner.HostDiscovery
import com.root.scanner.scanner.OsFingerprint
import com.root.scanner.scanner.PortScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ScanViewModel : ViewModel() {

    private val hostDiscovery = HostDiscovery()
    private val portScanner = PortScanner()

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state

    private val devicesMap = linkedMapOf<String, Device>()

    /** Lance une découverte d'hôtes sur le sous-réseau local, suivie d'un scan de ports rapide. */
    fun startNetworkScan() {
        if (_state.value.isScanning) return
        devicesMap.clear()
        _state.update { it.copy(isScanning = true, progress = 0f, devices = emptyList(), statusText = "Détection du réseau...") }

        viewModelScope.launch {
            val prefix = hostDiscovery.getLocalSubnetPrefix()
            if (prefix == null) {
                _state.update { it.copy(isScanning = false, statusText = "Impossible de détecter le réseau Wi-Fi.") }
                return@launch
            }

            _state.update { it.copy(statusText = "Recherche des hôtes actifs sur ${prefix}0/24...") }

            hostDiscovery.scanSubnet(
                prefix = prefix,
                onHostFound = { device ->
                    devicesMap[device.ip] = device
                    _state.update { it.copy(devices = devicesMap.values.toList()) }
                },
                onProgress = { progress ->
                    _state.update { it.copy(progress = progress * 0.4f) }
                }
            )

            _state.update { it.copy(statusText = "Scan des ports sur ${devicesMap.size} hôte(s)...") }

            val hostList = devicesMap.values.toList()
            hostList.forEachIndexed { index, device ->
                val ports = portScanner.scanPorts(device.ip)
                device.openPorts.clear()
                device.openPorts.addAll(ports)
                device.guessedOs = OsFingerprint.guess(device)
                _state.update {
                    it.copy(
                        devices = devicesMap.values.toList(),
                        progress = 0.4f + 0.6f * (index + 1) / hostList.size.coerceAtLeast(1)
                    )
                }
            }

            _state.update { it.copy(isScanning = false, progress = 1f, statusText = "Scan terminé : ${devicesMap.size} hôte(s) trouvé(s).") }
        }
    }

    /** Scan approfondi de tous les ports pour un hôte spécifique. */
    fun deepScanHost(ip: String) {
        viewModelScope.launch {
            val device = devicesMap[ip] ?: return@launch
            _state.update { it.copy(isScanning = true, statusText = "Scan complet de $ip (65535 ports)...", progress = 0f) }

            val ports = portScanner.scanAllPorts(ip, onProgress = { p ->
                _state.update { it.copy(progress = p) }
            })

            device.openPorts.clear()
            device.openPorts.addAll(ports)
            device.guessedOs = OsFingerprint.guess(device)

            _state.update {
                it.copy(
                    isScanning = false,
                    progress = 1f,
                    devices = devicesMap.values.toList(),
                    statusText = "Scan complet terminé pour $ip."
                )
            }
        }
    }

    fun stopScan() {
        _state.update { it.copy(isScanning = false, statusText = "Scan interrompu.") }
    }
}
