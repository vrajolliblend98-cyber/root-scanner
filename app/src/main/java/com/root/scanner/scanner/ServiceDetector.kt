package com.root.scanner.scanner

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

/**
 * Récupère la "banner" envoyée par un service réseau juste après connexion
 * (technique standard de fingerprinting utilisée par nmap -sV en version simplifiée).
 */
object ServiceDetector {

    fun grabBanner(socket: Socket, port: Int): String? {
        return try {
            socket.soTimeout = 500

            // Pour HTTP/HTTPS, le serveur attend une requête avant de répondre.
            if (port == 80 || port == 8080 || port == 443 || port == 8443) {
                val out = socket.getOutputStream()
                out.write("HEAD / HTTP/1.0\r\nHost: localhost\r\n\r\n".toByteArray())
                out.flush()
            }

            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val line = reader.readLine()
            line?.trim()?.take(200)
        } catch (e: Exception) {
            null
        }
    }

    /** Devine un nom de service générique à partir du contenu de la banner. */
    fun guessFromBanner(banner: String?): String? {
        if (banner == null) return null
        val lower = banner.lowercase()
        return when {
            lower.contains("http") -> "HTTP"
            lower.contains("ssh") -> "SSH"
            lower.contains("ftp") -> "FTP"
            lower.contains("smtp") -> "SMTP"
            else -> "Inconnu"
        }
    }
}
