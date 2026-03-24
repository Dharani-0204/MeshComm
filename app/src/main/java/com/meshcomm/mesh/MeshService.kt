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
import com.meshcomm.data.model.TransportType
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
    lateinit var bleManager: BleMeshManager
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

        bleManager = BleMeshManager(this)
        wifiManager = WiFiDirectManager(this, transportLayer)

        // Handshake on new peer
        scope.launch {
            PeerRegistry.peerFlow.collect { peers ->
                if (peers.isNotEmpty()) {
                    messageRouter.sendHandshake()
                    
                    // Register BLE senders in transport layer dynamically
                    peers.forEach { peer ->
                        if (peer.transport == TransportType.BLUETOOTH) {
                            transportLayer.registerSender(peer.deviceId, object : TransportLayer.Sender {
                                override fun send(data: String) {
                                    bleManager.sendData(peer.deviceId, data)
                                }
                                override fun close() {}
                            })
                        }
                    }

                    if (storeAndForward.getPendingCount() > 0) {
                        storeAndForward.flushToAllPeers()
                    }
                }
            }
        }

        PeerRegistry.registerMessageCallback { data, fromId ->
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
                val myDeviceId = PrefsHelper.getUserId(this@MeshService)
                if (msg.deviceId == myDeviceId || msg.type == MessageType.HANDSHAKE) return@collect

                when (msg.type) {
                    MessageType.SOS -> {
                        sosManager.triggerSOSAlert()
                        NotificationHelper.showSOSNotification(this@MeshService, msg.senderName)
                    }
                    MessageType.BROADCAST, MessageType.DIRECT -> {
                        NotificationHelper.showMessageNotification(
                            this@MeshService, msg.senderName,
                            msg.content.take(60)
                        )
                    }
                    else -> {}
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
        bleManager.start()
        wifiManager.startDiscovery()

        // Continuous discovery loop
        scope.launch {
            while (isActive) {
                delay(15_000)
                bleManager.startScan()
                wifiManager.startDiscovery()
                messageRouter.sendHandshake() // Re-broadcast identity periodically
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        bleManager.stop()
        wifiManager.stopAll()
        locationProvider.stopUpdates()
        transportLayer.disconnectAll()
        PeerRegistry.clear()
        try { unregisterReceiver(wifiP2pReceiver) } catch (e: Exception) {}
    }

    fun sendBroadcastMessage(content: String, includeLocation: Boolean) {
        val (lat, lon) = if (includeLocation) locationProvider.getLastLatLon() else Pair(0.0, 0.0)
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
        messageRouter.sendMessage(msg)
    }

    fun sendDirectMessage(targetId: String, targetName: String, content: String, includeLocation: Boolean) {
        val (lat, lon) = if (includeLocation) locationProvider.getLastLatLon() else Pair(0.0, 0.0)
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
        messageRouter.sendMessage(msg)
    }

    fun sendSOS(customMessage: String = "EMERGENCY! I need help!") {
        sosManager.sendSOS(customMessage)
    }

    fun getConnectedPeerCount(): Int = PeerRegistry.getConnectedCount()
}
