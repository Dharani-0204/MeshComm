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

## Message Packet

```json
{
  "messageId": "uuid",
  "senderId": "abc12345",
  "senderName": "Rahul",
  "targetId": null,
  "type": "NORMAL | SOS",
  "content": "Hello mesh",
  "latitude": 17.3850,
  "longitude": 78.4867,
  "batteryLevel": 78,
  "nearbyDevicesCount": 3,
  "timestamp": 1710000000000,
  "ttl": 7,
  "status": "SENT | RELAYED | DELIVERED",
  "isEncrypted": false
}
```

---

## Mesh Forwarding Logic

```
onMessageReceived(json, fromPeerId):
  message = parse(json)
  if message.messageId in seenMessages → DROP
  seenMessages.add(message.messageId)

  message = decrypt(message)         // AES if isEncrypted
  store to Room DB
  emit to UI via SharedFlow

  if battery > 30% AND ttl > 0 AND senderId != self:
    message.ttl -= 1
    forward to all peers EXCEPT fromPeerId
```

SOS messages use TTL = 10 (higher than normal TTL = 7).

---

## Build & Run

### Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android device/emulator API 26+

### Steps
1. Unzip `MeshComm.zip`
2. Open `MeshComm/` in Android Studio → `File > Open`
3. Wait for Gradle sync to complete
4. Connect a physical device (Bluetooth/WiFi Direct require real hardware)
5. Run → `app`

### First run
- Enter your name and role (Civilian / Rescuer / Authority)
- Grant all permissions (Location, Bluetooth, WiFi)
- The mesh service starts automatically as a foreground service

### Testing on multiple devices
1. Install the APK on 2+ devices
2. Enable Bluetooth on all devices
3. Open the app — discovery starts automatically
4. Send a broadcast message from Device A
5. Watch it appear on Device B, C, etc.
6. Test SOS from Device A → red alert appears on all others

---

## Encryption

- **SOS messages** and **direct (targeted) messages** are encrypted with AES-128/CBC before sending
- The shared key is hardcoded for hackathon purposes: `MeshComm16ByteK!`
- In production: replace with Diffie-Hellman key exchange or pre-shared key distribution

---

## Battery-Aware Routing

| Battery Level | Behavior |
|---|---|
| > 30% | Full relay node — forwards all messages |
| ≤ 30% | Passive mode — receives and displays, does NOT relay |
| Any | SOS messages are always relayed regardless |

---

## Store-and-Forward

If a message is sent when no peers are connected:
- It is saved to `StoreAndForwardQueue` (SharedPreferences)
- Queue capacity: 50 messages (FIFO, oldest dropped)
- On next peer connection: all queued messages are flushed automatically

---

## Navigation

| Tab | Description |
|---|---|
| 📡 Mesh | Broadcast group chat |
| 💬 Chat | Direct peer-to-peer messages |
| 🚨 SOS | Emergency alert button + received SOS list |
| 🗺️ Map | Offline location pins (GPS coordinates) |
| 👤 Profile | Identity, role, emergency contacts |

---

## Permissions Required

```xml
BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT
ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
ACCESS_WIFI_STATE, CHANGE_WIFI_STATE
FOREGROUND_SERVICE
VIBRATE
```

---

## Hackathon Tips

- **Demo mode**: Keep 3 phones side-by-side; send an SOS from one — watch the alert pop on the others
- **Relay demo**: Put Phone B between A and C (out of range of each other) — messages hop A→B→C
- **Battery relay demo**: Drop Phone B below 30% battery (or mock via `BatteryHelper`) — it stops relaying

---

## Known Limitations (hackathon scope)

- Encryption key is hardcoded (replace with ECDH for production)
- WiFi Direct group management is simplified (one group at a time)
- Map tab shows coordinates as text, not rendered tiles (no internet = no tile map)
- `NEARBY_WIFI_DEVICES` permission for Android 13+ needs manual grant if not auto-requested

---

## License

MIT — free to use and modify.
