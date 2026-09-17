package com.root.scanner.scanner

import com.root.scanner.model.PortResult
import com.root.scanner.model.WellKnownPorts
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Scanner de ports TCP par "connect scan" : on tente une connexion TCP complète
 * (SYN/SYN-ACK/ACK géré par le système) sur chaque port. C'est l'équivalent
 * fonctionnel du "-sT" de nmap ; contrairement au "-sS" (SYN scan furtif), cette
 * approche ne nécessite pas de sockets raw ni de privilèges root.
 */
class PortScanner {

    suspend fun scanPorts(
        ip: String,
        ports: List<Int> = WellKnownPorts.commonPorts,
        timeoutMs: Int = 400,
        maxConcurrent: Int = 64
    ): List<PortResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PortResult>()
        val dispatcher = Dispatchers.IO.limitedParallelism(maxConcurrent)

        val jobs = ports.map { port ->
            async(dispatcher) { checkPort(ip, port, timeoutMs) }
        }
        jobs.awaitAll().filterNotNull().forEach { results.add(it) }
        results.sortedBy { it.port }
    }

    /** Scan complet des 65535 ports, utilisé pour un scan approfondi manuel. */
    suspend fun scanAllPorts(
        ip: String,
        timeoutMs: Int = 250,
        maxConcurrent: Int = 128,
        onProgress: (Float) -> Unit = {}
    ): List<PortResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<PortResult>()
        val dispatcher = Dispatchers.IO.limitedParallelism(maxConcurrent)
        val total = 65535
        var completed = 0

        val jobs = (1..65535).map { port ->
            async(dispatcher) {
                val result = checkPort(ip, port, timeoutMs)
                completed++
                if (completed % 200 == 0) onProgress(completed / total.toFloat())
                result
            }
        }
        jobs.awaitAll().filterNotNull().forEach { results.add(it) }
        onProgress(1f)
        results.sortedBy { it.port }
    }

    private fun checkPort(ip: String, port: Int, timeoutMs: Int): PortResult? {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            val banner = ServiceDetector.grabBanner(socket, port)
            socket.close()
            PortResult(
                port = port,
                isOpen = true,
                service = WellKnownPorts.map[port] ?: ServiceDetector.guessFromBanner(banner),
                banner = banner
            )
        } catch (e: Exception) {
            null // Port fermé, filtré, ou timeout
        }
    }
}
