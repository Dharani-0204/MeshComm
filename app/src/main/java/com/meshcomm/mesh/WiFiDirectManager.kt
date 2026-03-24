package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.*
import android.util.Log
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

/**
 * WiFiDirectManager: High-throughput mesh transport layer.
 * Optimized for automatic group formation and robust P2P sessions.
 */
class WiFiDirectManager(
    private val context: Context,
    private val transportLayer: TransportLayer
) {
    companion object {
        private const val TAG = "WiFiDirectManager"
        private const val PORT = 8888
    }

    private val manager: WifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel = manager.initialize(context, context.mainLooper, null)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var isSearching = false
    private var serverSocket: ServerSocket? = null
    private val connectedAddresses = ConcurrentHashMap.newKeySet<String>()

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (isSearching) return
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "WiFi Direct peer discovery started")
                isSearching = true
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "WiFi Direct discovery failed: $reason")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun onPeersAvailable(peers: WifiP2pDeviceList) {
        if (peers.deviceList.isEmpty()) return
        
        Log.d(TAG, "WiFi Direct peers found: ${peers.deviceList.size}")
        
        // Priority logic: Try to connect to existing groups or invite peers
        peers.deviceList.forEach { device ->
            if (device.status == WifiP2pDevice.AVAILABLE) {
                connect(device)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            groupOwnerIntent = 15 // High preference to be owner
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "WiFi Direct connection initiated to ${device.deviceName}")
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "WiFi Direct connection failed: $reason")
            }
        })
    }

    fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (info.groupFormed && info.isGroupOwner) {
            Log.i(TAG, "WiFi Group Owner: starting server")
            startServer()
        } else if (info.groupFormed) {
            Log.i(TAG, "WiFi Client: connecting to owner at ${info.groupOwnerAddress.hostAddress}")
            startClient(info.groupOwnerAddress.hostAddress ?: "")
        }
    }

    private fun startServer() {
        if (serverSocket != null && !serverSocket!!.isClosed) return
        
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                Log.i(TAG, "WiFi Mesh Server listening on port $PORT...")
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    Log.i(TAG, "WiFi connection accepted from ${socket.inetAddress.hostAddress}")
                    launch { handleSocket(socket) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "WiFi Server error: ${e.message}")
                delay(5000)
                if (isActive) startServer()
            }
        }
    }

    private fun startClient(host: String) {
        scope.launch {
            var retry = 0
            while (retry < 5 && isActive) {
                try {
                    val socket = Socket()
                    socket.bind(null)
                    socket.connect(InetSocketAddress(host, PORT), 10000)
                    Log.i(TAG, "WiFi Client connected to owner")
                    handleSocket(socket)
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "WiFi Client connect attempt ${retry + 1} failed: ${e.message}")
                    retry++
                    delay(3000)
                }
            }
        }
    }

    private suspend fun handleSocket(socket: Socket) {
        val address = socket.inetAddress.hostAddress ?: "unknown"
        if (connectedAddresses.contains(address)) {
            runCatching { socket.close() }
            return
        }
        connectedAddresses.add(address)

        val deviceId = "wifi_$address"
        val deviceName = "WiFi Peer ($address)"

        PeerRegistry.addPeer(PeerDevice(deviceId, deviceName, TransportType.WIFI_DIRECT))
        transportLayer.registerChannel(deviceId, socket.getOutputStream())

        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { PeerRegistry.dispatchIncoming(it, deviceId) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WiFi session with $address ended: ${e.message}")
        } finally {
            connectedAddresses.remove(address)
            transportLayer.unregisterChannel(deviceId, socket.getOutputStream())
            PeerRegistry.removePeer(deviceId)
            runCatching { socket.close() }
            
            // If connection lost and we aren't group owner, try to reconnect or rediscover
            scope.launch {
                delay(5000)
                startDiscovery()
            }
        }
    }

    fun stopAll() {
        scope.cancel()
        runCatching { serverSocket?.close() }
        manager.removeGroup(channel, null)
        manager.stopPeerDiscovery(channel, null)
    }
}
