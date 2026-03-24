package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.*
import android.os.Looper
import android.util.Log
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class WiFiDirectManager(
    private val context: Context,
    private val transportLayer: TransportLayer
) {
    companion object {
        private const val TAG = "WiFiDirectManager"
        const val PORT = 8988
    }

    private val manager: WifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel = manager.initialize(context, Looper.getMainLooper(), null)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    
    private val connectingDevices = ConcurrentHashMap.newKeySet<String>()

    // ── Discovery ────────────────────────────────────────────────────────────

    fun startDiscovery() {
        Log.d(TAG, "Starting WiFi Direct discovery...")
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Discovery initiated")
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Discovery failed: $reason")
            }
        })
    }

    fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        peerList.deviceList.forEach { device ->
            Log.d(TAG, "Peer found: ${device.deviceName} (${device.deviceAddress}) status: ${device.status}")
            if (device.status == WifiP2pDevice.AVAILABLE) {
                autoConnect(device)
            }
        }
    }

    private fun autoConnect(device: WifiP2pDevice) {
        if (transportLayer.getConnectedIds().contains(device.deviceAddress) || 
            connectingDevices.contains(device.deviceAddress)) {
            return
        }

        Log.i(TAG, "Auto-connecting to ${device.deviceName} (${device.deviceAddress})")
        connectingDevices.add(device.deviceAddress)
        
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            groupOwnerIntent = 0 // Let the system decide or prefer NOT being owner
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Connect command success for ${device.deviceAddress}")
            }
            override fun onFailure(reason: Int) {
                Log.e(TAG, "Connect failed for ${device.deviceAddress}: $reason")
                connectingDevices.remove(device.deviceAddress)
            }
        })
    }

    // ── Connection Info ───────────────────────────────────────────────────────

    fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        Log.d(TAG, "Connection info available: groupFormed=${info.groupFormed}, isGroupOwner=${info.isGroupOwner}")
        
        if (info.groupFormed) {
            if (info.isGroupOwner) {
                startSocketServer()
            } else {
                info.groupOwnerAddress?.hostAddress?.let { addr ->
                    connectToGroupOwner(addr)
                }
            }
        }
    }

    // ── Server (Group Owner) ──────────────────────────────────────────────────

    private fun startSocketServer() {
        if (serverSocket != null && !serverSocket!!.isClosed) return
        
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                Log.i(TAG, "Socket Server started on port $PORT")
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    Log.d(TAG, "Client connected: ${client.inetAddress.hostAddress}")
                    launch { handleClientSocket(client) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server Socket Error: ${e.message}")
            }
        }
    }

    private fun handleClientSocket(socket: Socket) {
        val hostAddress = socket.inetAddress.hostAddress ?: "unknown"
        val deviceId = "wifi_$hostAddress"
        
        Log.i(TAG, "Handling socket connection from $hostAddress")
        
        // Use host address as temporary device ID until handshake
        PeerRegistry.addPeer(PeerDevice(deviceId, "WiFi Peer ($hostAddress)", TransportType.WIFI_DIRECT))
        transportLayer.registerChannel(deviceId, socket.getOutputStream())

        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { PeerRegistry.dispatchIncoming(it, deviceId) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Socket connection closed for $deviceId: ${e.message}")
        } finally {
            transportLayer.unregisterSender(deviceId)
            PeerRegistry.removePeer(deviceId)
            // Cleanup based on IP might be tricky if it changes, but WiFi Direct IPs are usually stable within a group
            runCatching { socket.close() }
        }
    }

    // ── Client (non-group-owner) ──────────────────────────────────────────────

    private fun connectToGroupOwner(address: String) {
        scope.launch {
            var socket: Socket? = null
            var attempts = 0
            while (isActive && attempts < 5) {
                try {
                    delay(1000) // Give server time to start
                    Log.d(TAG, "Connecting to group owner at $address (attempt ${attempts + 1})")
                    socket = Socket()
                    socket.connect(InetSocketAddress(address, PORT), 5000)
                    Log.i(TAG, "Connected to Group Owner at $address")
                    handleClientSocket(socket)
                    break
                } catch (e: Exception) {
                    attempts++
                    Log.w(TAG, "Failed to connect to group owner: ${e.message}")
                    delay(2000)
                }
            }
        }
    }

    fun stopAll() {
        Log.i(TAG, "Stopping WiFiDirectManager")
        scope.cancel()
        runCatching { serverSocket?.close() }
        serverSocket = null
        manager.removeGroup(channel, null)
        manager.stopPeerDiscovery(channel, null)
        connectingDevices.clear()
    }
}
