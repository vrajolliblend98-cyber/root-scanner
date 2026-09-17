package com.root.scanner.model

/**
 * Représente un hôte découvert sur le réseau local.
 */
data class Device(
    val ip: String,
    var hostname: String? = null,
    var mac: String? = null,
    var vendor: String? = null,
    var isAlive: Boolean = false,
    var latencyMs: Long? = null,
    var ttl: Int? = null,
    var guessedOs: String? = null,
    val openPorts: MutableList<PortResult> = mutableListOf()
)

/**
 * Résultat de scan pour un port unique.
 */
data class PortResult(
    val port: Int,
    val isOpen: Boolean,
    var service: String? = null,
    var banner: String? = null
)

/**
 * État global d'une session de scan, utilisé par le ViewModel.
 */
data class ScanState(
    val isScanning: Boolean = false,
    val progress: Float = 0f,
    val statusText: String = "",
    val devices: List<Device> = emptyList()
)

/**
 * Table de correspondance port -> service (équivalent simplifié de nmap-services).
 */
object WellKnownPorts {
    val map: Map<Int, String> = mapOf(
        21 to "FTP",
        22 to "SSH",
        23 to "Telnet",
        25 to "SMTP",
        53 to "DNS",
        80 to "HTTP",
        110 to "POP3",
        135 to "MSRPC",
        139 to "NetBIOS",
        143 to "IMAP",
        443 to "HTTPS",
        445 to "SMB",
        587 to "SMTP-TLS",
        993 to "IMAPS",
        995 to "POP3S",
        1723 to "PPTP",
        3306 to "MySQL",
        3389 to "RDP",
        5432 to "PostgreSQL",
        5900 to "VNC",
        6379 to "Redis",
        8080 to "HTTP-Alt",
        8443 to "HTTPS-Alt",
        9200 to "Elasticsearch",
        27017 to "MongoDB"
    )

    /** Liste de ports utilisée pour le scan "rapide" (équivalent du top-1000 réduit). */
    val commonPorts: List<Int> = map.keys.sorted()
}
