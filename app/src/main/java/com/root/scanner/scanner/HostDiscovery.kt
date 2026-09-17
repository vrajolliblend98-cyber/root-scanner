package com.root.scanner.scanner

import com.root.scanner.model.Device
import kotlinx.coroutines.*
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Découverte des hôtes actifs sur le sous-réseau local.
 *
 * Approche : sur Android non-rooté on ne peut pas envoyer de vrais paquets ICMP
 * bruts, donc on utilise InetAddress.isReachable (qui retombe sur un "TCP echo"
 * via le port 7, ou un ping système selon la plateforme) combiné à une tentative
 * de connexion TCP sur des ports courants comme fallback de détection de vie.
 */
class HostDiscovery {

    /**
     * Détermine le préfixe réseau (ex: "192.168.1.") à partir de l'interface Wi-Fi active.
     */
    fun getLocalSubnetPrefix(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue
                for (addr in iface.interfaceAddresses) {
                    val ip = addr.address
                    if (ip.hostAddress?.contains(":") == true) continue // skip IPv6
                    val parts = ip.hostAddress?.split(".") ?: continue
                    if (parts.size == 4) {
                        return "${parts[0]}.${parts[1]}.${parts[2]}."
                    }
                }
            }
        } catch (e: Exception) {
            // Pas d'interface exploitable
        }
        return null
    }

    /**
     * Scanne les 254 adresses possibles du sous-réseau en parallèle (nombre de
     * coroutines limité) et retourne la liste des hôtes qui répondent.
     */
    suspend fun scanSubnet(
        prefix: String,
        timeoutMs: Int = 300,
        onHostFound: suspend (Device) -> Unit,
        onProgress: (Float) -> Unit
    ) = withContext(Dispatchers.IO) {
        val total = 254
        var completed = 0
        val semaphore = Semaphore(32)

        val jobs = (1..254).map { hostNum ->
            async {
                semaphore.withPermit {
                    val ip = "$prefix$hostNum"
                    val device = probeHost(ip, timeoutMs)
                    completed++
                    onProgress(completed / total.toFloat())
                    if (device.isAlive) onHostFound(device)
                }
            }
        }
        jobs.awaitAll()
    }

    private suspend fun probeHost(ip: String, timeoutMs: Int): Device = withContext(Dispatchers.IO) {
        val device = Device(ip = ip)
        try {
            val addr = InetAddress.getByName(ip)
            val start = System.currentTimeMillis()
            val reachable = addr.isReachable(timeoutMs)
            val elapsed = System.currentTimeMillis() - start

            if (reachable) {
                device.isAlive = true
                device.latencyMs = elapsed
                device.hostname = addr.canonicalHostName.takeIf { it != ip }
                device.ttl = readTtlFromProc(ip)
            } else {
                // Fallback : certains hôtes bloquent ICMP mais ont un port TCP ouvert
                if (TcpProbe.quickCheck(ip)) {
                    device.isAlive = true
                    device.latencyMs = System.currentTimeMillis() - start
                }
            }

            if (device.isAlive) {
                device.mac = ArpReader.lookupMac(ip)
            }
        } catch (e: Exception) {
            device.isAlive = false
        }
        device
    }

    /**
     * Tente de lire le TTL observé pour une IP donnée depuis /proc/net (si accessible).
     * Utilisé plus tard par OsFingerprint pour affiner la détection d'OS.
     */
    private fun readTtlFromProc(ip: String): Int? {
        return try {
            val file = File("/proc/net/route")
            if (!file.exists() || !file.canRead()) return null
            null // Lecture TTL réelle nécessite un accès raw socket ; laissé null sans root.
        } catch (e: Exception) {
            null
        }
    }
}

/** Petit sémaphore de coroutines pour limiter le parallélisme du ping sweep. */
class Semaphore(limit: Int) {
    private val channel = kotlinx.coroutines.channels.Channel<Unit>(limit)
    init { repeat(limit) { channel.trySend(Unit) } }
    suspend fun <T> withPermit(block: suspend () -> T): T {
        channel.receive()
        try {
            return block()
        } finally {
            channel.trySend(Unit)
        }
    }
}
