package com.root.scanner.scanner

import java.net.InetSocketAddress
import java.net.Socket

/**
 * Sondage rapide sur quelques ports courants, utilisé quand ICMP (isReachable)
 * échoue mais que l'hôte peut tout de même être actif (pare-feu bloquant le ping).
 */
object TcpProbe {
    private val probePorts = listOf(80, 443, 22, 445, 139)

    fun quickCheck(ip: String, timeoutMs: Int = 200): Boolean {
        for (port in probePorts) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                socket.close()
                return true
            } catch (e: Exception) {
                // essaie le port suivant
            }
        }
        return false
    }
}
