# MeshComm — Disaster Offline Communication Mesh

A fully offline, peer-to-peer Android mesh communication system using **Bluetooth LE + WiFi Direct**.  
No internet. No servers. No SIM card required.

---

## Features

| Feature | Status |
|---|---|
| Bluetooth LE discovery + RFCOMM Classic | ✅ |
| WiFi Direct P2P sockets | ✅ |
| Multi-hop message flooding (TTL-based) | ✅ |
| Deduplication via `seenMessages` set | ✅ |
| Broadcast (group) chat | ✅ |
| Direct (encrypted) messaging | ✅ |
| SOS alert with GPS, battery, vibrate | ✅ |
| AES-128 encryption for SOS + direct | ✅ |
| Battery-aware relay (>30% threshold) | ✅ |
| Store-and-forward queue (offline buffer) | ✅ |
| ACK / "I'm coming" response system | ✅ |
| GPS location sharing (optional toggle) | ✅ |
| Offline location heatmap / SOS dashboard | ✅ |
| Room persistence (SQLite) | ✅ |
| Notification for SOS + new messages | ✅ |
| Foreground service (survives minimise) | ✅ |
| User identity: name / role / emergency contacts | ✅ |
| Message status: Sent / Relayed / Delivered | ✅ |

---

## Project Structure

```
app/src/main/java/com/meshcomm/
├── MeshCommApp.kt                    Application class
│
├── data/
│   ├── model/Models.kt               Message, User, PeerDevice, enums
│   ├── db/
│   │   ├── AppDatabase.kt            Room database singleton
│   │   └── DatabaseEntities.kt       Entities + DAOs
│   └── repository/
│       ├── MessageRepository.kt
│       └── UserRepository.kt
│
├── mesh/
│   ├── MeshService.kt                Foreground service — orchestrator
│   ├── BluetoothMeshManager.kt       BLE scan/advertise + RFCOMM server/client
│   ├── WiFiDirectManager.kt          P2P discovery + TCP socket server/client
│   ├── WiFiDirectBroadcastReceiver.kt
│   ├── MessageRouter.kt              ★ Core flood-forward with TTL + dedup
│   ├── TransportLayer.kt             Abstraction over BT + WiFi streams
│   ├── PeerRegistry.kt               Thread-safe live peer list
│   ├── StoreAndForwardQueue.kt       Buffer messages when offline
│   └── AcknowledgementManager.kt    ACK / "I'm coming" replies
│
├── sos/
│   └── SOSManager.kt                 One-tap SOS with GPS + vibrate + alert
│
├── crypto/
│   └── EncryptionUtil.kt             AES-128/CBC for SOS + direct messages
│
├── location/
│   └── LocationProvider.kt           GPS-only (no internet), StateFlow
│
├── ui/
│   ├── splash/SplashActivity.kt
│   ├── setup/SetupActivity.kt        First-run: name, role, permissions
│   ├── home/
│   │   ├── HomeActivity.kt           BottomNav host + SOS snackbar
│   │   ├── MeshViewModel.kt          Shared ViewModel + service binding
│   │   └── MeshStatusFragment.kt     Live peer list + relay/battery stats
│   ├── broadcast/
│   │   ├── BroadcastFragment.kt      Group chat UI
│   │   └── MessageAdapter.kt         RecyclerView adapter (all message types)
│   ├── chat/ChatFragment.kt           Direct P2P chat
│   ├── sos/SOSFragment.kt             Big red SOS button + received alerts list
│   ├── map/
│   │   ├── MapFragment.kt            Offline location overview
│   │   └── SosListAdapter.kt
│   └── profile/ProfileFragment.kt    Identity + emergency contacts
│
└── utils/
    ├── BatteryHelper.kt
    ├── NotificationHelper.kt
    └── PrefsHelper.kt
```

---

## Architecture

```
UI (Fragments/Activities)
        ↕  LiveData / SharedFlow
    MeshViewModel
        ↕  Service binding (IBinder)
    MeshService (Foreground)
    ┌─────────────┬──────────────┐
    │ BluetoothMgr│ WiFiDirectMgr│
    └─────────────┴──────────────┘
              ↕ raw JSON lines
         TransportLayer
              ↕
         MessageRouter  ←→  StoreAndForwardQueue
              ↕
         MessageRepository (Room / SQLite)
              ↕
         EncryptionUtil / SOSManager / AcknowledgementManager
```

---



## License

MIT — free to use and modify.
