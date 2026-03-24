package com.meshcomm.mesh

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.meshcomm.data.db.AppDatabase
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import com.meshcomm.data.repository.MessageRepository
import com.meshcomm.location.LocationProvider
import com.meshcomm.sos.SOSManager
import com.meshcomm.utils.BatteryHelper
import com.meshcomm.utils.NotificationHelper
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class MeshService : Service() {

    companion object {
        private const val TAG = "MeshService"

        fun start(context: Context) {
            val intent = Intent(context, MeshService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MeshService::class.java))
        }
    }

    inner class MeshBinder : Binder() {
        fun getService(): MeshService = this@MeshService
    }

    private val binder = MeshBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var _transportLayer: TransportLayer? = null
    val transportLayer get() = _transportLayer!!

    private var _btManager: BluetoothMeshManager? = null

    private var _wifiManager: WiFiDirectManager? = null

    private var _messageRouter: MessageRouter? = null
    val messageRouter get() = _messageRouter!!

    private var _sosManager: SOSManager? = null

    private var _locationProvider: LocationProvider? = null
    val locationProvider get() = _locationProvider!!

    private var _storeAndForward: StoreAndForwardQueue? = null
    val storeAndForward get() = _storeAndForward!!

    private var _ackManager: AcknowledgementManager? = null

    private val wifiP2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            Log.d(TAG, "WiFi P2P Broadcast: $action")
            
            val p2pManager = getSystemService(WIFI_P2P_SERVICE) as? WifiP2pManager ?: return
            val channel = p2pManager.initialize(context, mainLooper, null)

            when (action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        Log.i(TAG, "WiFi P2P Enabled - Starting Discovery")
                        _wifiManager?.startDiscovery()
                    } else {
                        Log.w(TAG, "WiFi P2P Disabled")
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    } else {
                        Manifest.permission.ACCESS_FINE_LOCATION
                    }

                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            p2pManager.requestPeers(channel) { peers ->
                                Log.d(TAG, "Peers discovered: ${peers.deviceList.size}")
                                _wifiManager?.onPeersAvailable(peers)
                            }
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Permission denied for requestPeers: ${e.message}")
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.NEARBY_WIFI_DEVICES
                    } else {
                        Manifest.permission.ACCESS_FINE_LOCATION
                    }
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        p2pManager.requestConnectionInfo(channel) { info: WifiP2pInfo ->
                            _wifiManager?.onConnectionInfoAvailable(info)
                        }
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    // Could update local device status if needed
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LifeLink Mesh Service creating...")
        
        NotificationHelper.createChannels(this)
        startForeground(1, NotificationHelper.buildServiceNotification(this, 0))

        _transportLayer = TransportLayer()
        _locationProvider = LocationProvider(this)
        _locationProvider?.startUpdates()

        val db = AppDatabase.get(this)
        val repo = MessageRepository(db)

        _messageRouter = MessageRouter(this, repo, transportLayer)
        _sosManager = SOSManager(this, messageRouter, locationProvider)
        _storeAndForward = StoreAndForwardQueue(this, transportLayer)
        _ackManager = AcknowledgementManager(this) { _messageRouter }

        _btManager = BluetoothMeshManager(this, transportLayer)
        _wifiManager = WiFiDirectManager(this, transportLayer)

        // 1. Flush pending messages whenever a new peer connects (Multi-hop support)
        scope.launch {
            PeerRegistry.peerFlow.collect { peers ->
                if (peers.isNotEmpty() && (_storeAndForward?.getPendingCount() ?: 0) > 0) {
                    Log.d(TAG, "New peer connected, flushing ${_storeAndForward?.getPendingCount()} pending messages")
                    _storeAndForward?.flushToAllPeers()
                }
            }
        }

        // 2. Wire incoming messages from transport to router
        PeerRegistry.registerMessageCallback { data, fromId ->
            _messageRouter?.onRawDataReceived(data, fromId)
        }

        // 3. Update notification with peer count
        scope.launch {
            PeerRegistry.peerFlow.collectLatest { peers ->
                val notif = NotificationHelper.buildServiceNotification(this@MeshService, peers.size)
                val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(1, notif)
            }
        }

        // 4. Handle incoming messages via Router (Multi-hopping and Priority based)
        scope.launch {
            _messageRouter?.incomingMessages?.collect { msg ->
                val myDeviceId = PrefsHelper.getUserId(this@MeshService)
                if (msg.deviceId == myDeviceId) return@collect

                when (msg.type) {
                    MessageType.SOS -> {
                        _sosManager?.triggerSOSAlert()
                        NotificationHelper.showSOSNotification(this@MeshService, msg.senderName)
                    }
                    MessageType.BROADCAST, MessageType.DIRECT, MessageType.AUDIO, MessageType.IMAGE -> {
                        NotificationHelper.showMessageNotification(
                            this@MeshService, msg.senderName,
                            if (msg.type == MessageType.AUDIO) "Audio message" else if (msg.type == MessageType.IMAGE) "Image message" else msg.content.take(60)
                        )
                    }
                    MessageType.CHUNK_ACK -> {
                        _ackManager?.onChunkAckReceived(msg)
                    }
                    else -> {}
                }
            }
        }

        // Register WiFi Direct receiver
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        registerReceiver(wifiP2pReceiver, filter)

        // Start mesh if permissions allow
        startMeshNetwork()

        // Periodic maintenance loop (Every 15s) - Aggressive auto-connect
        scope.launch {
            while (isActive) {
                delay(15_000)
                Log.d(TAG, "Mesh Maintenance: Ensuring connectivity...")
                
                // Re-trigger discovery if peer count is low
                if (PeerRegistry.getConnectedCount() < 3) {
                    _btManager?.startDiscovery()
                    _wifiManager?.startDiscovery()
                }
                
                // Keep battery optimized: reduce discovery frequency if battery is low
                val battery = BatteryHelper.getLevel(this@MeshService)
                if (battery < 20) {
                    Log.w(TAG, "Low battery ($battery%), slowing down discovery frequency")
                    delay(30_000) 
                }
            }
        }

        Log.i(TAG, "MeshService started successfully")
    }

    private fun startMeshNetwork() {
        Log.d(TAG, "Initializing Bluetooth and WiFi Mesh...")
        
        if (hasBluetoothConnectPermission()) {
            _btManager?.startServer()
        }
        if (hasBluetoothAdvertisePermission()) {
            _btManager?.startAdvertising()
        }
        if (hasBluetoothScanPermission()) {
            _btManager?.startDiscovery()
        }
        _btManager?.startReconnectionLoop()

        _wifiManager?.startDiscovery()
    }

    private fun hasBluetoothScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasBluetoothAdvertisePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "MeshService stopping...")
        scope.cancel()
        _btManager?.stopAll()
        _wifiManager?.stopAll()
        _locationProvider?.stopUpdates()
        _transportLayer?.disconnectAll()
        PeerRegistry.clear()
        try { unregisterReceiver(wifiP2pReceiver) } catch (_: Exception) {}
    }

    // ── Public API for UI ─────────────────────────────────────────────────────

    fun sendBroadcastMessage(content: String, includeLocation: Boolean) {
        val (lat, lon) = if (includeLocation) _locationProvider?.getLastLatLon() ?: Pair(0.0, 0.0) else Pair(0.0, 0.0)
        val deviceId = PrefsHelper.getUserId(this)
        val msg = Message(
            senderId = deviceId,
            senderName = PrefsHelper.getUserName(this),
            content = content,
            latitude = lat,
            longitude = lon,
            batteryLevel = BatteryHelper.getLevel(this),
            nearbyDevicesCount = PeerRegistry.getConnectedCount(),
            type = MessageType.BROADCAST,
            deviceId = deviceId
        )
        if (PeerRegistry.getConnectedCount() == 0) {
            _storeAndForward?.enqueue(msg)
        }
        _messageRouter?.sendMessage(msg)
    }

    fun sendDirectMessage(targetId: String, targetName: String, content: String, includeLocation: Boolean) {
        val (lat, lon) = if (includeLocation) _locationProvider?.getLastLatLon() ?: Pair(0.0, 0.0) else Pair(0.0, 0.0)
        val deviceId = PrefsHelper.getUserId(this)
        val msg = Message(
            senderId = deviceId,
            senderName = PrefsHelper.getUserName(this),
            targetId = targetId,
            content = content,
            latitude = lat,
            longitude = lon,
            batteryLevel = BatteryHelper.getLevel(this),
            nearbyDevicesCount = PeerRegistry.getConnectedCount(),
            type = MessageType.DIRECT,
            deviceId = deviceId
        )
        if (PeerRegistry.getConnectedCount() == 0) {
            _storeAndForward?.enqueue(msg)
        }
        _messageRouter?.sendMessage(msg)
        Log.d(TAG, "Direct message queued for $targetName ($targetId)")
    }

    fun sendSOS(customMessage: String = "EMERGENCY! I need help!") {
        _sosManager?.sendSOS(customMessage)
    }

    fun getConnectedPeerCount(): Int = PeerRegistry.getConnectedCount()
}
