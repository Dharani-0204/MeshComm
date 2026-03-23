package com.meshcomm.mesh

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
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

    lateinit var transportLayer: TransportLayer
    lateinit var btManager: BluetoothMeshManager
    lateinit var wifiManager: WiFiDirectManager
    lateinit var messageRouter: MessageRouter
    lateinit var sosManager: SOSManager
    lateinit var locationProvider: LocationProvider
    lateinit var storeAndForward: StoreAndForwardQueue
    lateinit var ackManager: AcknowledgementManager

    private val wifiP2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    val p2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
                    val channel = p2pManager.initialize(context, mainLooper, null)
                    @Suppress("DEPRECATION")
                    p2pManager.requestPeers(channel) { peers ->
                        wifiManager.onPeersAvailable(peers)
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val p2pManager = getSystemService(WIFI_P2P_SERVICE) as WifiP2pManager
                    val channel = p2pManager.initialize(context, mainLooper, null)
                    p2pManager.requestConnectionInfo(channel) { info: WifiP2pInfo ->
                        if (info.groupFormed) wifiManager.onConnectionInfoAvailable(info)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MeshService starting...")
        NotificationHelper.createChannels(this)
        startForeground(1, NotificationHelper.buildServiceNotification(this, 0))

        transportLayer = TransportLayer()
        locationProvider = LocationProvider(this)
        locationProvider.startUpdates()

        val db = AppDatabase.get(this)
        val repo = MessageRepository(db)

        messageRouter = MessageRouter(this, repo, transportLayer)
        sosManager = SOSManager(this, messageRouter, locationProvider)
        storeAndForward = StoreAndForwardQueue(this, transportLayer)
        ackManager = AcknowledgementManager(this, messageRouter)

        // Flush pending messages whenever a new peer connects
        scope.launch {
            PeerRegistry.peerFlow.collect { peers ->
                if (peers.isNotEmpty() && storeAndForward.getPendingCount() > 0) {
                    Log.d(TAG, "New peer connected, flushing ${storeAndForward.getPendingCount()} pending messages")
                    storeAndForward.flushToAllPeers()
                }
            }
        }

        btManager = BluetoothMeshManager(this, transportLayer)
        wifiManager = WiFiDirectManager(this, transportLayer)

        // Wire incoming messages from transport to router
        PeerRegistry.registerMessageCallback { data, fromId ->
            Log.d(TAG, "Received message from $fromId: ${data.take(50)}")
            messageRouter.onRawDataReceived(data, fromId)
        }

        // Update notification with peer count
        scope.launch {
            PeerRegistry.peerFlow.collectLatest { peers ->
                val notif = NotificationHelper.buildServiceNotification(this@MeshService, peers.size)
                val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(1, notif)
            }
        }

        // Show notifications for incoming messages
        scope.launch {
            messageRouter.incomingMessages.collect { msg ->
                when (msg.type) {
                    MessageType.SOS -> {
                        Log.i(TAG, "Received SOS message from ${msg.senderName}")
                        NotificationHelper.showSOSNotification(this@MeshService, msg.senderName)
                    }
                    MessageType.BROADCAST, MessageType.DIRECT -> {
                        if (msg.senderId != PrefsHelper.getUserId(this@MeshService)) {
                            Log.d(TAG, "Received ${msg.type} message from ${msg.senderName}")
                            NotificationHelper.showMessageNotification(
                                this@MeshService, msg.senderName,
                                msg.content.take(60)
                            )
                        }
                    }
                }
            }
        }

        // Register WiFi Direct receiver
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        }
        registerReceiver(wifiP2pReceiver, filter)

        // Start mesh
        Log.d(TAG, "Starting Bluetooth mesh...")
        btManager.startServer()
        btManager.startAdvertising()
        btManager.startDiscovery()
        btManager.startReconnectionLoop()

        Log.d(TAG, "Starting WiFi Direct...")
        wifiManager.startDiscovery()

        // Periodic discovery every 30 seconds
        scope.launch {
            while (isActive) {
                delay(30_000) // 30 seconds
                Log.d(TAG, "Periodic discovery: rescanning for peers")
                btManager.startDiscovery()
                wifiManager.startDiscovery()
            }
        }

        Log.i(TAG, "MeshService started successfully")
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "MeshService bound")
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MeshService stopping...")
        scope.cancel()
        btManager.stopAll()
        wifiManager.stopAll()
        locationProvider.stopUpdates()
        transportLayer.disconnectAll()
        PeerRegistry.clear()
        unregisterReceiver(wifiP2pReceiver)
        Log.i(TAG, "MeshService stopped")
    }

    // ── Public API for UI ─────────────────────────────────────────────────────

    fun sendBroadcastMessage(content: String, includeLocation: Boolean) {
        val (lat, lon) = if (includeLocation) locationProvider.getLastLatLon() else Pair(0.0, 0.0)
        val msg = Message(
            senderId = PrefsHelper.getUserId(this),
            senderName = PrefsHelper.getUserName(this),
            content = content,
            latitude = lat,
            longitude = lon,
            batteryLevel = BatteryHelper.getLevel(this),
            nearbyDevicesCount = PeerRegistry.getConnectedCount(),
            type = MessageType.BROADCAST
        )
        if (PeerRegistry.getConnectedCount() == 0) {
            storeAndForward.enqueue(msg)
        }
        messageRouter.sendMessage(msg)
    }

    fun sendDirectMessage(targetId: String, targetName: String, content: String, includeLocation: Boolean) {
        val (lat, lon) = if (includeLocation) locationProvider.getLastLatLon() else Pair(0.0, 0.0)
        val msg = Message(
            senderId = PrefsHelper.getUserId(this),
            senderName = PrefsHelper.getUserName(this),
            targetId = targetId,
            content = content,
            latitude = lat,
            longitude = lon,
            batteryLevel = BatteryHelper.getLevel(this),
            nearbyDevicesCount = PeerRegistry.getConnectedCount(),
            type = MessageType.DIRECT
        )
        if (PeerRegistry.getConnectedCount() == 0) {
            storeAndForward.enqueue(msg)
        }
        messageRouter.sendMessage(msg)
    }

    fun sendSOS(customMessage: String = "EMERGENCY! I need help!") {
        sosManager.sendSOS(customMessage)
    }

    fun getConnectedPeerCount(): Int = PeerRegistry.getConnectedCount()
}
