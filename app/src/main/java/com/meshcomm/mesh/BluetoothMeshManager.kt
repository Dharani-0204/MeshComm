package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
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
        const val DISCOVERABLE_DURATION = 300 // 5 minutes
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: BluetoothServerSocket? = null
    private var leAdvertiser: BluetoothLeAdvertiser? = null
    private var leScanner: BluetoothLeScanner? = null
    private val knownDevices = ConcurrentHashMap<String, BluetoothDevice>()

    // ── Classic Bluetooth Discovery Receiver ─────────────────────────────────

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        val deviceName = it.name ?: "Unknown"
                        val deviceAddress = it.address
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                        Log.d(TAG, "Classic BT device discovered: $deviceName ($deviceAddress) RSSI: $rssi")

                        knownDevices[deviceAddress] = it
                        if (!transportLayer.getConnectedIds().contains(deviceAddress)) {
                            Log.d(TAG, "Auto-connecting to $deviceName...")
                            connectToDevice(it)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.d(TAG, "Classic discovery started")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "Classic discovery finished (found ${knownDevices.size} total devices)")
                }
            }
        }
    }

    init {
        // Register discovery receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, filter)
        Log.d(TAG, "BluetoothMeshManager initialized, discovery receiver registered")
    }

    // ── Device State Checks ──────────────────────────────────────────────────

    fun isBluetoothEnabled(): Boolean {
        val enabled = adapter?.isEnabled == true
        Log.d(TAG, "Bluetooth enabled: $enabled")
        return enabled
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val enabled = gpsEnabled || networkEnabled
        Log.d(TAG, "Location enabled: $enabled (GPS: $gpsEnabled, Network: $networkEnabled)")
        return enabled
    }

    // ── Make Device Discoverable ─────────────────────────────────────────────

    fun makeDiscoverable(activity: Activity) {
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION)
        }
        activity.startActivityForResult(discoverableIntent, 1)
        Log.d(TAG, "Requesting device discoverability for $DISCOVERABLE_DURATION seconds")
    }

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
        // Check prerequisites
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "Cannot start discovery: Bluetooth is OFF")
            return
        }
        if (!isLocationEnabled()) {
            Log.e(TAG, "Cannot start discovery: Location/GPS is OFF (required for BLE scan)")
            return
        }

        // Start BLE scan
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
        runCatching { context.unregisterReceiver(discoveryReceiver) }
    }
}
