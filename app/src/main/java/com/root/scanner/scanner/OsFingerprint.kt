package com.root.scanner.scanner

import com.root.scanner.model.Device

/**
 * Estimation heuristique du système d'exploitation d'un hôte.
 *
 * IMPORTANT : le vrai fingerprinting nmap (-O) analyse des détails bas niveau
 * de la pile TCP/IP (options SYN, taille de fenêtre initiale, ordre des options,
 * TTL initial) obtenus via des sockets raw — impossible sans root sur Android.
 * Cette heuristique se base donc sur des signaux disponibles sans privilèges :
 * TTL restant (quand disponible), combinaison de ports ouverts, et présence de
 * services caractéristiques (SMB/RDP -> Windows, SSH seul -> Linux, etc.).
 * Le résultat est une estimation, pas une certitude, et doit être présenté
 * comme telle dans l'UI.
 */
object OsFingerprint {

    fun guess(device: Device): String {
        val ports = device.openPorts.filter { it.isOpen }.map { it.port }.toSet()

        val hasSmb = 445 in ports || 139 in ports
        val hasRdp = 3389 in ports
        val hasSsh = 22 in ports
        val hasWinRpc = 135 in ports

        return when {
            hasRdp || (hasSmb && hasWinRpc) -> "Probable : Windows"
            hasSmb && !hasSsh -> "Probable : Windows ou NAS (SMB)"
            hasSsh && !hasSmb && !hasRdp -> "Probable : Linux / Unix"
            device.ttl != null && device.ttl!! <= 64 && hasSsh -> "Probable : Linux / Unix (TTL ≤ 64)"
            device.ttl != null && device.ttl!! in 65..128 -> "Probable : Windows (TTL 65-128)"
            ports.isEmpty() -> "Indéterminé (aucun port ouvert détecté)"
            else -> "Indéterminé"
        }
    }
}
