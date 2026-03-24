package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import android.util.Log
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
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
        private const val TAG = "BT_MeshManager"
        val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val APP_UUID: UUID     = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: BluetoothServerSocket? = null
    private var leAdvertiser: BluetoothLeAdvertiser? = null
    private var leScanner: BluetoothLeScanner? = null
    
    private val discoveredDevices = ConcurrentHashMap<String, BluetoothDevice>()
    private val connectingDevices = ConcurrentHashMap.newKeySet<String>()

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    
                    device?.let {
                        val name = it.name ?: "Unknown"
                        val address = it.address
                        Log.d("MESH_DEBUG", "Device found: $name $address")
                        Log.d(TAG, "Device found: $name [$address]")
                        discoveredDevices[address] = it
                        autoConnect(it)
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> Log.d(TAG, "Classic discovery started")
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> Log.d(TAG, "Classic discovery finished")
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(discoveryReceiver, filter)
    }

    fun startServer() {
        scope.launch {
            try {
                serverSocket = adapter?.listenUsingRfcommWithServiceRecord("MeshComm", SERVICE_UUID)
                Log.i(TAG, "Server started, waiting for connections...")
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    Log.i(TAG, "Incoming connection accepted from ${socket.remoteDevice.address}")
                    launch { handleSocket(socket) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server socket error: ${e.message}")
            }
        }
    }

    private fun autoConnect(device: BluetoothDevice) {
        val address = device.address
        if (transportLayer.getConnectedIds().contains(address) || connectingDevices.contains(address)) return
        
        scope.launch {
            connectingDevices.add(address)
            Log.d(TAG, "Attempting auto-connect to $address")
            
            // Cancel discovery before connecting
            if (adapter?.isDiscovering == true) {
                adapter.cancelDiscovery()
            }

            Log.d("MESH_DEBUG", "Trying to connect...")
            try {
                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()
                Log.d("MESH_DEBUG", "Connected successfully")
                Log.i(TAG, "Successfully connected to $address")
                handleSocket(socket)
            } catch (e: Exception) {
                Log.d("MESH_DEBUG", "Connection failed")
                Log.e(TAG, "Connection failed to $address: ${e.message}")
                connectingDevices.remove(address)
            }
        }
    }

    private fun handleSocket(socket: BluetoothSocket) {
        val deviceId = socket.remoteDevice.address
        val deviceName = socket.remoteDevice.name ?: deviceId

        PeerRegistry.addPeer(PeerDevice(deviceId, deviceName, TransportType.BLUETOOTH))
        transportLayer.registerChannel(deviceId, socket.outputStream)
        connectingDevices.remove(deviceId)

        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { PeerRegistry.dispatchIncoming(it, deviceId) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Socket read error for $deviceId: ${e.message}")
        } finally {
            transportLayer.unregisterChannel(deviceId)
            PeerRegistry.removePeer(deviceId)
            runCatching { socket.close() }
            Log.w(TAG, "Socket closed for $deviceId")
        }
    }

    fun startAdvertising() {
        leAdvertiser = adapter?.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(APP_UUID))
            .setIncludeDeviceName(true)
            .build()
        leAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.d(TAG, "BLE Advertising started")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "BLE Advertising failed: $errorCode")
        }
    }

    fun startDiscovery() {
        Log.d("MESH_DEBUG", "Starting discovery...")
        if (adapter?.isEnabled == true) {
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
            Log.d(TAG, "Starting classic discovery")
            
            // Also start BLE scan for newer devices
            startBleScan()
        }
    }

    private fun startBleScan() {
        leScanner = adapter?.bluetoothLeScanner ?: return
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(APP_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        leScanner?.startScan(listOf(filter), settings, bleScanCallback)
    }

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            Log.d(TAG, "BLE device found: ${device.address}")
            discoveredDevices[device.address] = device
            autoConnect(device)
        }
    }

    fun startReconnectionLoop() {
        scope.launch {
            while (isActive) {
                delay(15_000) // Check every 15s
                discoveredDevices.values.forEach { device ->
                    if (!transportLayer.getConnectedIds().contains(device.address)) {
                        autoConnect(device)
                    }
                }
            }
        }
    }

    fun stopAll() {
        scope.cancel()
        leScanner?.stopScan(bleScanCallback)
        leAdvertiser?.stopAdvertising(advertiseCallback)
        adapter?.cancelDiscovery()
        runCatching { serverSocket?.close() }
        runCatching { context.unregisterReceiver(discoveryReceiver) }
    }
}
