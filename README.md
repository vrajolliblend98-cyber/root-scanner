# Root — Scanner réseau pour Android

Application Android native (Kotlin + Jetpack Compose) inspirée de nmap, avec une interface moderne.

## Fonctionnalités

- **Découverte d'hôtes** : ping sweep sur le sous-réseau Wi-Fi local (/24), avec fallback TCP si l'ICMP est bloqué.
- **Scan de ports** : scan TCP "connect" multi-thread (via coroutines), rapide (ports courants) ou complet (65535 ports).
- **Détection de service** : banner grabbing + table de correspondance ports/services.
- **Fingerprinting OS** : estimation heuristique (TTL, combinaison de ports ouverts) — voir limites ci-dessous.

## Comment l'ouvrir

1. Ouvre le dossier `Root/` avec Android Studio (Koala ou plus récent).
2. Laisse Gradle synchroniser (télécharge les dépendances Compose/Coroutines).
3. Lance sur un appareil ou émulateur connecté au même réseau Wi-Fi que les cibles.

## ⚠️ Limites importantes par rapport à nmap

Android sans root **interdit l'accès aux sockets raw**. Cela signifie :

- Pas de vrai scan SYN furtif (`-sS`) : ce projet fait un **connect scan** (`-sT`), plus lent et plus détectable, mais qui ne nécessite aucun privilège.
- Pas de vrai fingerprinting OS par empreinte de pile TCP/IP (`-O`) : l'estimation ici est **heuristique** (basée sur les ports ouverts et le TTL quand disponible), pas une empreinte fiable.
- Pas de forge de paquets ICMP/ARP bruts : la découverte d'hôtes utilise `InetAddress.isReachable()` et un fallback TCP.

Si tu obtiens un accès root sur l'appareil de test, il serait possible d'ajouter une couche JNI utilisant des sockets raw pour un scan SYN et un fingerprinting plus proche de nmap — mais cela sort du cadre d'une app Play Store standard.

## ⚖️ Usage légal

Cette application ne doit être utilisée que sur des réseaux et appareils dont tu es propriétaire ou pour lesquels tu as une autorisation explicite. Scanner un réseau sans autorisation peut être illégal selon la juridiction.

## Structure du projet

```
app/src/main/java/com/root/scanner/
├── MainActivity.kt
├── model/Models.kt              # Device, PortResult, ScanState
├── scanner/
│   ├── HostDiscovery.kt         # Ping sweep du sous-réseau
│   ├── ArpReader.kt             # Lecture MAC via /proc/net/arp
│   ├── PortScanner.kt           # Scan TCP connect multi-thread
│   ├── ServiceDetector.kt       # Banner grabbing
│   ├── TcpProbe.kt              # Fallback de détection de vie
│   └── OsFingerprint.kt         # Heuristique d'OS
├── viewmodel/ScanViewModel.kt   # Orchestration + état UI
└── ui/
    ├── theme/                   # Couleurs, typographie, thème dark
    └── screens/                 # ScanScreen, DeviceDetailScreen
```
