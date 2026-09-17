package com.root.scanner.scanner

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Lit /proc/net/arp pour retrouver l'adresse MAC associée à une IP locale.
 * Fonctionne sans root sur la plupart des versions d'Android, mais peut être
 * restreint sur certaines versions récentes (Android 10+ limite l'accès).
 */
object ArpReader {

    fun lookupMac(ip: String): String? {
        return try {
            val file = File("/proc/net/arp")
            if (!file.exists() || !file.canRead()) return null

            val reader: BufferedReader = FileReader(file).buffered()
            reader.use {
                it.readLine() // ligne d'en-tête
                var line: String?
                while (it.readLine().also { l -> line = l } != null) {
                    val tokens = line!!.trim().split(Regex("\\s+"))
                    if (tokens.size >= 4 && tokens[0] == ip) {
                        val mac = tokens[3]
                        if (mac != "00:00:00:00:00:00") return mac
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
