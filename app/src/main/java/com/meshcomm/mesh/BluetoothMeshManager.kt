package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("MissingPermission")
class BluetoothMeshManager(
    private val context: Context,
    private val transportLayer: TransportLayer
) {
    companion object {
        private const val TAG = "BluetoothMeshManager"
        val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val APP_UUID: UUID     = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: BluetoothServerSocket? = null
    private var leAdvertiser: BluetoothLeAdvertiser? = null
    private var leScanner: BluetoothLeScanner? = null
    private val knownDevices = ConcurrentHashMap<String, BluetoothDevice>()

    // ── Classic RFCOMM Server ────────────────────────────────────────────────

    fun startServer() {
        scope.launch {
            try {
                serverSocket = adapter?.listenUsingRfcommWithServiceRecord("MeshComm", SERVICE_UUID)
                Log.d(TAG, "BT server started on RFCOMM")
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    Log.d(TAG, "Incoming connection from ${socket.remoteDevice.address}")
                    launch { handleSocket(socket) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "BT server error: ${e.message}", e)
            }
        }
    }

    private fun handleSocket(socket: BluetoothSocket) {
        val deviceId = socket.remoteDevice.address
        val deviceName = socket.remoteDevice.name ?: deviceId

        Log.d(TAG, "Peer connected: $deviceId ($deviceName)")
        PeerRegistry.addPeer(PeerDevice(deviceId, deviceName, TransportType.BLUETOOTH))
        transportLayer.registerChannel(deviceId, socket.outputStream)
        knownDevices[deviceId] = socket.remoteDevice

        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { PeerRegistry.dispatchIncoming(it, deviceId) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Socket error for $deviceId: ${e.message}")
        } finally {
            Log.d(TAG, "Peer disconnected: $deviceId")
            transportLayer.unregisterChannel(deviceId)
            PeerRegistry.removePeer(deviceId)
            runCatching { socket.close() }
        }
    }

    // ── Classic RFCOMM Client ────────────────────────────────────────────────

    fun connectToDevice(device: BluetoothDevice) {
        scope.launch {
            val deviceId = device.address
            val deviceName = device.name ?: deviceId
            Log.d(TAG, "Connecting to $deviceName ($deviceId)...")
            try {
                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()
                Log.d(TAG, "Connected to $deviceName")
                handleSocket(socket)
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed to $deviceId: ${e.message}")
            }
        }
    }

    // ── BLE Advertising (be discoverable) ───────────────────────────────────

    fun startAdvertising() {
        leAdvertiser = adapter?.bluetoothLeAdvertiser
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(APP_UUID))
            .setIncludeDeviceName(true)
            .build()
        leAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        Log.d(TAG, "BLE advertising started")
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "BLE advertising started successfully")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE advertising failed: errorCode=$errorCode")
        }
    }

    // ── BLE Scanning ─────────────────────────────────────────────────────────

    fun startDiscovery() {
        leScanner = adapter?.bluetoothLeScanner
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(APP_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        leScanner?.startScan(listOf(filter), settings, scanCallback)
        Log.d(TAG, "BT discovery started (BLE scan)")

        // Also do classic discovery for older devices
        adapter?.startDiscovery()
        Log.d(TAG, "BT discovery started (Classic scan)")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceId = device.address
            val deviceName = device.name ?: deviceId
            val rssi = result.rssi
            Log.d(TAG, "BLE device discovered: $deviceName ($deviceId) RSSI: $rssi")

            knownDevices[deviceId] = device
            if (!transportLayer.getConnectedIds().contains(deviceId)) {
                connectToDevice(device)
            }
        }
    }

    // ── Automatic Reconnection ───────────────────────────────────────────────

    fun startReconnectionLoop() {
        scope.launch {
            while (isActive) {
                delay(60_000) // Every 60 seconds
                Log.d(TAG, "Checking for disconnected devices to reconnect...")
                knownDevices.forEach { (id, device) ->
                    if (!transportLayer.getConnectedIds().contains(id)) {
                        Log.d(TAG, "Reconnecting to $id...")
                        connectToDevice(device)
                    }
                }
            }
        }
    }

    fun stopAll() {
        Log.d(TAG, "Stopping BluetoothMeshManager")
        scope.cancel()
        leScanner?.stopScan(scanCallback)
        leAdvertiser?.stopAdvertising(advertiseCallback)
        adapter?.cancelDiscovery()
        runCatching { serverSocket?.close() }
    }
}
