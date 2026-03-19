package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.*
import android.os.Looper
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket

@SuppressLint("MissingPermission")
class WiFiDirectManager(
    private val context: Context,
    private val transportLayer: TransportLayer
) {
    companion object {
        const val PORT = 8988
    }

    private val manager: WifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
    private val channel: WifiP2pManager.Channel = manager.initialize(context, Looper.getMainLooper(), null)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    private var isGroupOwner = false

    private val connectionListener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {}
        override fun onFailure(reason: Int) {}
    }

    // ── Discovery ────────────────────────────────────────────────────────────

    fun startDiscovery() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {}
            override fun onFailure(reason: Int) {}
        })
    }

    fun onPeersAvailable(peerList: WifiP2pDeviceList) {
        peerList.deviceList.forEach { device ->
            if (device.status == WifiP2pDevice.AVAILABLE) {
                connectToDevice(device)
            }
        }
    }

    private fun connectToDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply { deviceAddress = device.deviceAddress }
        manager.connect(channel, config, connectionListener)
    }

    // ── Connection Info ───────────────────────────────────────────────────────

    fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        isGroupOwner = info.isGroupOwner
        if (info.isGroupOwner) {
            startSocketServer()
        } else {
            info.groupOwnerAddress?.hostAddress?.let { addr ->
                connectToGroupOwner(addr)
            }
        }
    }

    // ── Server (Group Owner) ──────────────────────────────────────────────────

    private fun startSocketServer() {
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT)
                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    launch { handleClientSocket(client) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun handleClientSocket(socket: Socket) {
        val deviceId = "wifi_${socket.inetAddress.hostAddress}"
        val deviceName = socket.inetAddress.hostAddress ?: deviceId

        PeerRegistry.addPeer(PeerDevice(deviceId, deviceName, TransportType.WIFI_DIRECT))
        transportLayer.registerChannel(deviceId, socket.getOutputStream())

        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { PeerRegistry.dispatchIncoming(it, deviceId) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            transportLayer.unregisterChannel(deviceId)
            PeerRegistry.removePeer(deviceId)
            runCatching { socket.close() }
        }
    }

    // ── Client (non-group-owner) ──────────────────────────────────────────────

    private fun connectToGroupOwner(address: String) {
        scope.launch {
            try {
                val socket = Socket(address, PORT)
                handleClientSocket(socket)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun stopAll() {
        scope.cancel()
        runCatching { serverSocket?.close() }
        manager.removeGroup(channel, null)
        manager.stopPeerDiscovery(channel, null)
    }
}
