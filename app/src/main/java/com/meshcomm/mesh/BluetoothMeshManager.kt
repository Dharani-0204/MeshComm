package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * FIXED BLUETOOTH MESH MANAGER - Hybrid Discovery (BLE) + Data Transfer (RFCOMM)
 */
@SuppressLint("MissingPermission")
class BluetoothMeshManager(
    private val context: Context,
    private val transportLayer: TransportLayer,
    private val onPeerConnected: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "BT_MeshManager"
        // Standard Serial Port Profile (SPP) UUID
        val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        // Custom UUID for BLE Discovery
        val APP_UUID: UUID     = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        const val DISCOVERABLE_DURATION = 300 // 5 minutes
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var serverSocket: BluetoothServerSocket? = null
    private var leAdvertiser: BluetoothLeAdvertiser? = null
    private var leScanner: BluetoothLeScanner? = null
    
    private val discoveredDevices = ConcurrentHashMap<String, BluetoothDevice>()
    private val connectingDevices = ConcurrentHashMap.newKeySet<String>()
    
    private var isScanning = false

    // ── Classic Discovery Receiver ──────────────────────────────────────────

    private val classicReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    
                    device?.let {
                        val address = it.address
                        discoveredDevices[address] = it
                        if (!transportLayer.getConnectedIds().contains(address)) {
                            autoConnect(it)
                        }
                    }
                }
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(classicReceiver, filter)
    }

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    fun makeDiscoverable(activity: Activity) {
        val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION)
        activity.startActivity(intent)
    }

    // ── Server Side (Accepting connections) ──────────────────────────────────

    fun startServer() {
        scope.launch {
            try {
                if (adapter?.isEnabled != true) return@launch
                serverSocket = adapter.listenUsingRfcommWithServiceRecord("MeshComm", SERVICE_UUID)
                Log.i(TAG, "RFCOMM Server Started")
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    launch { handleSocket(socket) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server Error: ${e.message}")
                delay(5000)
                if (isActive) startServer()
            }
        }
    }

    private suspend fun handleSocket(socket: BluetoothSocket) {
        val deviceId = socket.remoteDevice.address
        val deviceName = socket.remoteDevice.name ?: "BT Peer ($deviceId)"

        if (transportLayer.getConnectedIds().contains(deviceId)) {
            runCatching { socket.close() }
            return
        }

        Log.i(TAG, "BT Connection established with $deviceId")
        PeerRegistry.addPeer(PeerDevice(deviceId, deviceName, TransportType.BLUETOOTH))
        transportLayer.registerChannel(deviceId, socket.outputStream)
        
        onPeerConnected(deviceId)

        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            while (currentCoroutineContext().isActive) {
                val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
                PeerRegistry.dispatchIncoming(line, deviceId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "BT Link Lost ($deviceId): ${e.message}")
        } finally {
            transportLayer.unregisterSender(deviceId)
            PeerRegistry.removePeer(deviceId)
            connectingDevices.remove(deviceId)
            runCatching { socket.close() }
        }
    }

    // ── Client Side (Initiating connections) ────────────────────────────────

    fun autoConnect(device: BluetoothDevice) {
        val deviceId = device.address
        if (transportLayer.getConnectedIds().contains(deviceId) || connectingDevices.contains(deviceId)) {
            return
        }

        scope.launch {
            connectingDevices.add(deviceId)
            Log.d(TAG, "Attempting connection to $deviceId")
            
            if (adapter?.isDiscovering == true) adapter.cancelDiscovery()

            try {
                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                withContext(Dispatchers.IO) { socket.connect() }
                handleSocket(socket)
            } catch (e: Exception) {
                Log.w(TAG, "Connection failed to $deviceId: ${e.message}")
                connectingDevices.remove(deviceId)
            }
        }
    }

    // ── Discovery & Advertising ─────────────────────────────────────────────

    fun startAdvertising() {
        if (!isBluetoothEnabled()) return
        leAdvertiser = adapter?.bluetoothLeAdvertiser ?: return

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(APP_UUID))
            .setIncludeDeviceName(false)
            .build()

        leAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        Log.i(TAG, "BLE Identity Advertising started")
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {}
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE Advertising Failed: $errorCode")
        }
    }

    fun startDiscovery() {
        if (!isBluetoothEnabled()) return
        
        // BLE Scan
        if (!isScanning) {
            leScanner = adapter?.bluetoothLeScanner
            val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(APP_UUID)).build()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            try {
                isScanning = true
                leScanner?.startScan(listOf(filter), settings, scanCallback)
            } catch (e: Exception) { isScanning = false }
        }

        // Classic Discovery
        if (adapter?.isDiscovering == false) {
            adapter.startDiscovery()
        }
    }

    fun stopDiscovery() {
        try {
            leScanner?.stopScan(scanCallback)
            if (adapter?.isDiscovering == true) adapter.cancelDiscovery()
        } catch (e: Exception) {}
        isScanning = false
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            discoveredDevices[device.address] = device
            if (!transportLayer.getConnectedIds().contains(device.address)) {
                autoConnect(device)
            }
        }
    }

    fun startReconnectionLoop() {
        scope.launch {
            while (isActive) {
                delay(30_000)
                val connected = transportLayer.getConnectedIds()
                discoveredDevices.values.forEach { device ->
                    if (!connected.contains(device.address) && !connectingDevices.contains(device.address)) {
                        autoConnect(device)
                    }
                }
            }
        }
    }

    fun stopAll() {
        scope.cancel()
        stopDiscovery()
        try { context.unregisterReceiver(classicReceiver) } catch (e: Exception) {}
        try { leAdvertiser?.stopAdvertising(advertiseCallback) } catch (e: Exception) {}
        runCatching { serverSocket?.close() }
        connectingDevices.clear()
    }
}
