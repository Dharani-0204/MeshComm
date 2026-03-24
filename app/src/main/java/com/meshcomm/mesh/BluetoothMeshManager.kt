package com.meshcomm.mesh

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * BluetoothMeshManager: Advanced zero-intervention Bluetooth Mesh.
 * Optimized for discovery, automatic reconnection, and collision avoidance.
 */
class BluetoothMeshManager(
    private val context: Context,
    private val transportLayer: TransportLayer
) {
    companion object {
        private const val TAG = "BluetoothMeshManager"
        // Standardized Service UUID for LifeLink Mesh (Classic & BLE)
        val MESH_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: BluetoothServerSocket? = null
    
    private var leAdvertiser: BluetoothLeAdvertiser? = null
    private var leScanner: BluetoothLeScanner? = null
    
    private val knownDevices = ConcurrentHashMap<String, BluetoothDevice>()
    private val activeSockets = ConcurrentHashMap<String, BluetoothSocket>()
    private val connectingDevices = ConcurrentHashMap.newKeySet<String>()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        Log.i(TAG, "Bluetooth turned ON - Restarting Mesh Components")
                        restartMesh()
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    } ?: return
                    
                    // Fallback identification via name if BLE fails
                    if (device.name?.contains("LifeLink", ignoreCase = true) == true) {
                        Log.d(TAG, "Classic discovery found app peer: ${device.address}")
                        knownDevices[device.address] = device
                        autoConnect(device)
                    }
                }
            }
        }
    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!transportLayer.getConnectedIds().contains(device.address)) {
                Log.d(TAG, "BLE Mesh Scan found peer: ${device.address}")
                knownDevices[device.address] = device
                autoConnect(device)
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun restartMesh() {
        stopAll()
        startServer()
        startAdvertising()
        startDiscovery()
        startReconnectionLoop()
    }

    @SuppressLint("MissingPermission")
    fun startServer() {
        if (adapter?.isEnabled != true) return
        
        scope.launch {
            try {
                serverSocket?.close()
                // listenUsingInsecureRfcomm avoids the "Bluetooth Pairing Request" popup
                serverSocket = adapter.listenUsingInsecureRfcommWithServiceRecord("LifeLinkMesh", MESH_UUID)
                Log.i(TAG, "Insecure RFCOMM Server listening for Mesh peers...")
                
                while (isActive) {
                    val socket = try { serverSocket?.accept() } catch (e: Exception) { null } ?: break
                    Log.i(TAG, "Accepted incoming Mesh connection from ${socket.remoteDevice.address}")
                    launch { handleSocket(socket) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "RFCOMM Server error: ${e.message}")
                delay(10000)
                if (isActive) startServer()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleSocket(socket: BluetoothSocket) {
        val address = socket.remoteDevice.address
        
        try {
            val writer = PrintWriter(socket.outputStream, true)
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            
            // Handshake to exchange peer identity and avoid dual-channel collisions
            val myId = PrefsHelper.getUserId(context)
            writer.println("HANDSHAKE:$myId")
            
            val handshakeLine = withTimeoutOrNull(5000) { 
                withContext(Dispatchers.IO) { reader.readLine() } 
            }
            
            if (handshakeLine == null || !handshakeLine.startsWith("HANDSHAKE:")) {
                Log.w(TAG, "Handshake failed/timeout for $address")
                runCatching { socket.close() }
                return
            }
            
            val remoteId = handshakeLine.substringAfter("HANDSHAKE:")
            Log.d(TAG, "Mesh Handshake success with $address (RemoteID: $remoteId)")

            // Anti-collision logic: Ensure only one stable channel per peer pair
            val existing = activeSockets[address]
            if (existing != null && existing.isConnected) {
                if (myId < remoteId) {
                    Log.d(TAG, "Collision: dropping redundant incoming channel from $address")
                    runCatching { socket.close() }
                    return
                } else {
                    Log.d(TAG, "Collision: replacing existing outgoing channel to $address")
                    runCatching { existing.close() }
                }
            }

            activeSockets[address] = socket
            val deviceName = try { socket.remoteDevice.name } catch (e: Exception) { null } ?: "Peer ${remoteId.take(4)}"
            
            PeerRegistry.addPeer(PeerDevice(address, deviceName, TransportType.BLUETOOTH))
            transportLayer.registerChannel(address, socket.outputStream)

            withContext(Dispatchers.IO) {
                while (isActive) {
                    val line = reader.readLine() ?: break
                    PeerRegistry.dispatchIncoming(line, address)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Bluetooth Mesh session with $address ended: ${e.message}")
        } finally {
            if (activeSockets[address] == socket) {
                activeSockets.remove(address)
                transportLayer.unregisterChannel(address, socket.outputStream)
                PeerRegistry.removePeer(address)
            }
            runCatching { socket.close() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun autoConnect(device: BluetoothDevice) {
        val address = device.address
        if (activeSockets.containsKey(address) || connectingDevices.contains(address)) return
        
        connectingDevices.add(address)
        scope.launch {
            try {
                // Jitter to reduce connection failures when multiple devices try at once
                delay(Random.nextLong(1000, 5000))
                if (activeSockets.containsKey(address)) return@launch

                Log.d(TAG, "Initiating Mesh auto-connect to $address")
                if (adapter?.isDiscovering == true) adapter.cancelDiscovery()
                
                // createInsecureRfcommSocketToServiceRecord avoids user intervention (no pairing popup)
                val socket = device.createInsecureRfcommSocketToServiceRecord(MESH_UUID)
                socket.connect()
                handleSocket(socket)
            } catch (e: Exception) {
                Log.w(TAG, "Auto-connect to $address failed: ${e.message}")
            } finally {
                connectingDevices.remove(address)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startAdvertising() {
        if (adapter?.isEnabled != true) return
        try {
            leAdvertiser = adapter.bluetoothLeAdvertiser ?: return
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .build()
            
            val data = AdvertiseData.Builder()
                .addServiceUuid(ParcelUuid(MESH_UUID))
                // Note: Device name excluded to ensure packet size stays within 31 bytes
                .setIncludeDeviceName(false)
                .build()
            
            leAdvertiser?.startAdvertising(settings, data, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                    Log.i(TAG, "BLE identification advertising successfully active")
                }
                override fun onStartFailure(errorCode: Int) {
                    Log.e(TAG, "BLE advertising failed: $errorCode")
                }
            })
        } catch (e: Exception) { Log.e(TAG, "BLE Advertise error: ${e.message}") }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (adapter?.isEnabled != true) return
        try {
            leScanner = adapter.bluetoothLeScanner ?: return
            val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(MESH_UUID)).build()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            
            leScanner?.startScan(listOf(filter), settings, bleScanCallback)
            Log.d(TAG, "Mesh BLE Scanner active")
            
            // Also trigger classic discovery periodically as a robust fallback
            if (adapter.isDiscovering == false) {
                adapter.startDiscovery()
            }
        } catch (e: Exception) { Log.e(TAG, "BLE scan error: ${e.message}") }
    }

    fun startReconnectionLoop() {
        scope.launch {
            while (isActive) {
                delay(45_000) // Check every 45s
                if (adapter?.isEnabled != true) continue
                val connected = transportLayer.getConnectedIds()
                knownDevices.forEach { (address, device) ->
                    if (!connected.contains(address)) {
                        autoConnect(device)
                        delay(2000)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopAll() {
        scope.cancel()
        runCatching {
            leScanner?.stopScan(bleScanCallback)
            leAdvertiser?.stopAdvertising(object : AdvertiseCallback() {})
            serverSocket?.close()
            activeSockets.values.forEach { it.close() }
            activeSockets.clear()
            connectingDevices.clear()
            context.unregisterReceiver(bluetoothReceiver)
        }
    }
}
