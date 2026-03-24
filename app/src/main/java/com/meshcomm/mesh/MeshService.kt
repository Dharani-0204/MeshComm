package com.meshcomm.mesh

import android.app.Service
import android.content.Context
import android.content.Intent
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

/**
 * PRODUCTION-READY MESH SERVICE
 * Orchestrates BLE Mesh operations and message routing in the background.
 */
class MeshService : Service() {

    companion object {
        private const val TAG = "MeshService"
        private const val DISCOVERY_INTERVAL = 30000L

        fun start(context: Context) {
            val intent = Intent(context, MeshService::class.java)
            context.startForegroundService(intent)
        }
    }

    inner class MeshBinder : Binder() {
        fun getService(): MeshService = this@MeshService
    }

    private val binder = MeshBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    lateinit var transportLayer: TransportLayer
    lateinit var btManager: BluetoothMeshManager
    lateinit var messageRouter: MessageRouter
    lateinit var sosManager: SOSManager
    lateinit var locationProvider: LocationProvider
    lateinit var storeAndForward: StoreAndForwardQueue

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MeshService onCreate")
        
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

        btManager = BluetoothMeshManager(this, transportLayer)

        // Wire incoming messages to the router
        PeerRegistry.registerMessageCallback { data, fromId ->
            messageRouter.onRawDataReceived(data, fromId)
        }

        // Start BLE Operations
        btManager.startServer()
        btManager.startAdvertising()
        btManager.startDiscovery()

        // Observe peer changes for UI and Notification updates
        scope.launch {
            PeerRegistry.peerFlow.collectLatest { peers ->
                val notif = NotificationHelper.buildServiceNotification(this@MeshService, peers.size)
                val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(1, notif)
            }
        }

        // Periodic maintenance loop
        scope.launch {
            while (isActive) {
                delay(DISCOVERY_INTERVAL)
                if (btManager.isBluetoothEnabled()) {
                    btManager.startDiscovery()
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        btManager.stopAll()
        transportLayer.disconnectAll()
        locationProvider.stopUpdates()
        scope.cancel()
        Log.i(TAG, "MeshService destroyed")
    }

    // ── Public API ──

    fun sendBroadcastMessage(content: String, includeLocation: Boolean) {
        val (lat, lon) = if (includeLocation) locationProvider.getLastLatLon() else Pair(0.0, 0.0)
        val msg = Message(
            senderId = PrefsHelper.getUserId(this),
            senderName = PrefsHelper.getUserName(this),
            content = content,
            latitude = lat,
            longitude = lon,
            batteryLevel = BatteryHelper.getLevel(this),
            type = MessageType.BROADCAST
        )
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
            type = MessageType.DIRECT
        )
        messageRouter.sendMessage(msg)
    }

    fun sendSOS(customMessage: String) {
        sosManager.sendSOS(customMessage)
    }

    fun getConnectedPeerCount(): Int = PeerRegistry.getConnectedCount()
}
