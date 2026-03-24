# MeshComm — Fully Offline Disaster Management System

A completely offline, peer-to-peer Android disaster management application using **Bluetooth Mesh + OSMDroid offline maps**.
**No internet. No servers. No Firebase. No Google Maps required.**

Built for disaster scenarios where all infrastructure is down.

---

## 🎯 **Core Features**

| Feature | Status | Description |
|---|---|---|
| **🗺️ Offline Maps** | ✅ | OSMDroid with offline tile caching |
| **📡 Bluetooth Mesh Network** | ✅ | Multi-hop message relay without internet |
| **🆘 SOS Emergency System** | ✅ | One-tap emergency alerts with GPS location |
| **👨‍⚕️ Role-Based Profiles** | ✅ | Civilian (medical profile) vs Rescuer access |
| **🧭 Real-Time Navigation** | ✅ | Live distance tracking like Swiggy (offline) |
| **📍 Location Tracking** | ✅ | GPS + manual location input (offline) |
| **🔥 SOS Heatmaps** | ✅ | Emergency hotspot visualization |
| **🧭 Cluster Zones** | ✅ | Distance-based alert grouping |
| **🚶 Movement Tracking** | ✅ | Real-time path drawing and updates |
| **💾 Local Database** | ✅ | Room SQLite for offline-first storage |
| **🔔 Emergency Notifications** | ✅ | SOS alerts with sound + vibration |
| **📱 Device Discovery** | ✅ | Bluetooth mesh peer finding |
| **🔋 Battery Optimization** | ✅ | Adaptive discovery intervals |
| **📞 Emergency Contacts** | ✅ | SMS integration for critical alerts |

---

## 📚 **Libraries & Dependencies**

### **🎨 UI & Design**
```kotlin
// Material Design 3
implementation 'com.google.android.material:material:1.12.0'

// View Binding
buildFeatures { viewBinding = true }

// Navigation Component
implementation 'androidx.navigation:navigation-fragment-ktx:2.7.6'
implementation 'androidx.navigation:navigation-ui-ktx:2.7.6'
```

### **🗺️ Offline Maps (OSMDroid)**
```kotlin
// OpenStreetMap offline maps
implementation 'org.osmdroid:osmdroid-android:6.1.18'

// HTTP client for tile downloading
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

### **💾 Database & Storage**
```kotlin
// Room Database (offline-first)
implementation 'androidx.room:room-runtime:2.6.1'
implementation 'androidx.room:room-ktx:2.6.1'
kapt 'androidx.room:room-compiler:2.6.1'

// JSON Serialization
implementation 'com.google.code.gson:gson:2.10.1'
```

### **🏗️ Architecture Components**
```kotlin
// MVVM Architecture
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'

// Fragment & Activity
implementation 'androidx.fragment:fragment-ktx:1.6.2'
implementation 'androidx.activity:activity-ktx:1.8.2'
```

### **⚡ Asynchronous Programming**
```kotlin
// Kotlin Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

// Background Work
implementation 'androidx.work:work-runtime-ktx:2.9.0'
```

### **📱 Android Core**
```kotlin
// AndroidX Core
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
```

---

## 🏗️ **Architecture Overview**

```
┌─────────────────────────────────────────────────────────────┐
│                    MeshComm App                             │
│                 (Offline-First MVVM)                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer                               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Setup     │  │   Profile   │  │  Navigation         │ │
│  │  Activity   │  │  Fragment   │  │   Activity          │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Rescue    │  │   SOS       │  │    Home             │ │
│  │ Dashboard   │  │  Fragment   │  │   Activity          │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   ViewModel Layer                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │    Mesh     │  │   Profile   │  │                     │ │
│  │  ViewModel  │  │  ViewModel  │  │   Other ViewModels  │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                 Repository Layer                            │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │   Message   │  │  SOSAlert   │  │    Profile          │ │
│  │ Repository  │  │ Repository  │  │   Repository        │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Data Layer                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │    Room     │  │   Shared    │  │    OSMDroid         │ │
│  │  Database   │  │ Preferences │  │   Maps Cache        │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              Mesh Networking Layer                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ Bluetooth   │  │   Message   │  │    Location         │ │
│  │   Service   │  │    Router   │  │    Provider         │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │    SOS      │  │  Transport  │  │   Peer Registry     │ │
│  │  Manager    │  │   Layer     │  │                     │ │
│  └─────────────┘  └─────────────┘  └─────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

---

## 📂 **Updated Project Structure**

```
app/src/main/java/com/meshcomm/
├── MeshCommApp.kt                    # Application class (OSMDroid config)
│
├── data/
│   ├── model/
│   │   ├── Message.kt               # Mesh message model
│   │   ├── SOSAlert.kt              # Emergency alert model
│   │   ├── UserProfile.kt           # Role-based profile model
│   │   ├── EmergencyContact.kt      # Emergency contact model
│   │   └── Enums.kt                 # MessageType, UserRole, etc.
│   ├── db/
│   │   ├── AppDatabase.kt           # Room database singleton
│   │   └── DatabaseEntities.kt      # Room entities + DAOs
│   └── repository/
│       ├── MessageRepository.kt     # Message CRUD operations
│       ├── SOSAlertRepository.kt    # SOS alert management
│       └── ProfileRepository.kt     # Profile management
│
├── mesh/ (Bluetooth Networking)
│   ├── MeshService.kt               # Foreground mesh service
│   ├── MeshManager.kt               # Mesh orchestrator
│   ├── BluetoothService.kt          # Enhanced BT discovery
│   ├── BluetoothMeshManager.kt      # BLE scan/advertise + RFCOMM
│   ├── MessageRelayHandler.kt       # Multi-hop relay logic
│   ├── TransportLayer.kt            # BT communication abstraction
│   └── PeerRegistry.kt              # Thread-safe peer management
│
├── sos/
│   └── SOSManager.kt                # Emergency alert system
│
├── location/
│   └── LocationProvider.kt          # GPS location (offline)
│
├── ui/
│   ├── splash/SplashActivity.kt     # App splash screen
│   ├── setup/SetupActivity.kt       # Role selection + profile setup
│   ├── home/
│   │   ├── HomeActivity.kt          # Main navigation host
│   │   ├── MeshViewModel.kt         # Shared mesh state
│   │   └── MeshStatusFragment.kt    # Network status display
│   ├── dashboard/
│   │   └── RescueDashboardActivity.kt # Rescuer-only offline dashboard
│   ├── navigation/
│   │   └── NavigationActivity.kt    # Real-time offline navigation
│   ├── map/
│   │   ├── MapFragment.kt           # Map overview fragment
│   │   ├── OfflineTileDownloader.kt # Tile download system
│   │   └── OfflineMapManagerActivity.kt # Map management
│   ├── profile/
│   │   ├── ProfileFragment.kt       # Role-based profile management
│   │   └── ProfileViewModel.kt      # Profile state management
│   ├── sos/SOSFragment.kt           # Emergency interface
│   ├── broadcast/
│   │   ├── BroadcastFragment.kt     # Group messaging
│   │   └── MessageAdapter.kt        # Message list adapter
│   └── contacts/
│       └── ContactPickerDialog.kt   # Emergency contact picker
│
└── utils/
    ├── PrefsHelper.kt               # SharedPreferences helper
    ├── BatteryHelper.kt             # Battery status utilities
    ├── NotificationHelper.kt        # Emergency notifications
    ├── SmsHelper.kt                 # SMS emergency alerts
    ├── GeocoderUtil.kt              # Offline geocoding
    └── EmergencyContactsManager.kt  # Contact management
```

---

## 🎯 **Key Components Explained**

### **🗺️ Offline Maps System**
- **OSMDroid**: Renders OpenStreetMap tiles without internet
- **OfflineTileDownloader**: Downloads map tiles for offline use
- **Tile Caching**: Stores maps locally for disaster scenarios
- **Custom Areas**: Download specific regions (e.g., Hyderabad)

### **📡 Bluetooth Mesh Network**
- **Multi-hop Relay**: Messages bounce between devices
- **TTL Management**: Prevents infinite message loops
- **Deduplication**: Avoid processing same message twice
- **Adaptive Discovery**: Battery-aware scanning intervals
- **Peer Registry**: Track nearby devices automatically

### **🆘 Emergency System**
- **One-tap SOS**: Large red button for emergencies
- **GPS Integration**: Automatic location sharing
- **Medical Profiles**: Blood group, conditions, allergies
- **SMS Alerts**: Notify emergency contacts automatically
- **Sound + Vibration**: SOS pattern alerts

### **🧭 Real-Time Navigation**
- **Live Distance Updates**: Like Swiggy delivery tracking
- **Direction Guidance**: "Head North-East" instructions
- **Arrival Detection**: "10 meters away" → "Arrived!"
- **Route Visualization**: Draw path from current to destination
- **GPS Updates**: 3-second location refresh intervals

### **👥 Role-Based Access**
- **Civilians**: Must complete medical emergency profile
- **Rescuers**: Access to dashboard, no profile required
- **Authorities**: Enhanced permissions (future feature)

---

## 🚀 **Getting Started**

### **Prerequisites**
- Android Studio Arctic Fox or later
- Android SDK 26+ (Android 8.0)
- Physical Android device (Bluetooth required)

### **Build**
```bash
git clone <repository-url>
cd MeshComm
./gradlew assembleDebug
```

### **Install**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔧 **Configuration**

### **Offline Maps Setup**
1. Open **OfflineMapManagerActivity**
2. Download Hyderabad region or custom area
3. Maps stored in `app/files/osmdroid/`

### **Emergency Contacts**
1. Complete profile setup (Civilians only)
2. Add emergency contacts via contact picker
3. SMS alerts sent automatically during SOS

### **Bluetooth Mesh**
1. Enable Bluetooth + Location permissions
2. App automatically discovers nearby devices
3. Messages relay through 10+ hops automatically

---

## 📱 **Usage Scenarios**

### **🌪️ Natural Disaster**
- **Infrastructure Down**: No cell towers, no internet
- **Device Mesh**: Phones create automatic network
- **SOS Propagation**: Emergency alerts spread device-to-device
- **Offline Maps**: Navigate using pre-downloaded tiles

### **⛰️ Remote Areas**
- **No Connectivity**: Mountains, forests, remote locations
- **GPS Navigation**: Works without internet connection
- **Bluetooth Range**: 100m+ device-to-device communication
- **Store & Forward**: Messages saved until network available

### **🏢 Building Search & Rescue**
- **Rescuer Dashboard**: See all SOS alerts on offline map
- **Real-time Tracking**: Live distance to emergency locations
- **Medical Info**: Access victim's blood type, allergies instantly
- **Team Coordination**: Messages relay between rescue teams

---

## ⚠️ **Limitations**

- **Bluetooth Range**: ~100 meters between devices
- **Network Density**: Requires multiple devices for multi-hop
- **Battery Consumption**: Continuous Bluetooth scanning
- **Map Storage**: Offline tiles can use significant storage

---

## 🤝 **Contributing**

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

---

## 📄 **License**

MIT License - free to use and modify for disaster preparedness.

---

## 🆘 **Built for Real Emergencies**

This app was designed for **actual disaster scenarios** where traditional communication infrastructure fails. Every feature works completely offline to ensure reliability when it matters most.
