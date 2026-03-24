package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Optimized BLE Mesh Manager for reliable peer-to-peer connectivity.
 * Removed restrictive collision avoidance and improved connection flow.
 */
@SuppressLint("MissingPermission")
class BleMeshManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "BleMeshManager"
        val SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val CHAR_UUID: UUID    = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        private const val SCAN_PERIOD: Long = 10000
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private val scanner: BluetoothLeScanner? by lazy { adapter?.bluetoothLeScanner }
    
    private var gattServer: BluetoothGattServer? = null
    private val connectedGatts = ConcurrentHashMap<String, BluetoothGatt>()
    private val serverDevices = ConcurrentHashMap<String, BluetoothDevice>()
    
    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        if (adapter == null || !adapter.isEnabled) {
            Log.e(TAG, "Bluetooth not enabled or not supported")
            return
        }
        setupServer()
        startAdvertising()
        startScan()
    }

    // ── Peripheral Role (GATT Server) ──

    private fun setupServer() {
        if (gattServer != null) return
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE or BluetoothGattCharacteristic.PERMISSION_READ
        )
        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
        Log.d(TAG, "GATT Server setup complete and listening")
    }

    private fun startAdvertising() {
        val advertiser = adapter?.bluetoothLeAdvertiser ?: return
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .build()
        
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false) 
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .build()

        advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.d(TAG, "BLE Advertising started - Mesh is discoverable")
            }
            override fun onStartFailure(errorCode: Int) {
                Log.e(TAG, "BLE Advertising failed: $errorCode")
            }
        })
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Server: Peer ${device.address} connected to our GATT server")
                serverDevices[device.address] = device
                addPeer(device)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Server: Peer ${device.address} disconnected")
                serverDevices.remove(device.address)
                PeerRegistry.removePeer(device.address)
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray
        ) {
            val data = String(value)
            Log.d(TAG, "Server: Received ${value.size} bytes from ${device.address}")
            PeerRegistry.dispatchIncoming(data, device.address)
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    // ── Central Role (GATT Client) ──

    fun startScan() {
        if (isScanning || scanner == null) {
            Log.d(TAG, "Scan already in progress or scanner unavailable")
            return
        }
        
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        isScanning = true
        scanner?.startScan(listOf(filter), settings, scanCallback)
        Log.d(TAG, "BLE Discovery started")
        
        handler.postDelayed({ stopScan() }, SCAN_PERIOD)
    }

    fun stopScan() {
        if (!isScanning) return
        try {
            scanner?.stopScan(scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan: ${e.message}")
        }
        isScanning = false
        Log.d(TAG, "BLE Discovery stopped")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            // Connect if we don't have an active client OR server connection with this device
            if (!connectedGatts.containsKey(device.address) && !serverDevices.containsKey(device.address)) {
                Log.i(TAG, "Discovered potential mesh node: ${device.address}. Connecting...")
                connectToDevice(device)
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        // Stopping scan is critical for connection stability on many Android devices
        stopScan()
        Log.d(TAG, "Client: Attempting GATT connection to ${device.address}")
        device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Client: Successfully connected to $address")
                connectedGatts[address] = gatt
                // Request MTU before service discovery to ensure large packets can be received
                gatt.requestMtu(512)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Client: Disconnected from $address (status: $status)")
                connectedGatts.remove(address)
                PeerRegistry.removePeer(address)
                gatt.close()
                // Restart scan to find more peers or reconnect
                startScan()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "Client: MTU negotiated at $mtu for ${gatt.device.address}")
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHAR_UUID)
                if (characteristic != null) {
                    Log.i(TAG, "Client: Service discovered for ${gatt.device.address}. Link active.")
                    addPeer(gatt.device)
                } else {
                    Log.w(TAG, "Client: Service found but characteristic missing on ${gatt.device.address}")
                }
            } else {
                Log.e(TAG, "Client: Service discovery failed for ${gatt.device.address}, status: $status")
            }
        }
    }

    private fun addPeer(device: BluetoothDevice) {
        val peer = PeerDevice(
            deviceId = device.address,
            deviceName = device.name ?: "Mesh Node (${device.address.takeLast(5)})",
            transport = TransportType.BLUETOOTH,
            isConnected = true
        )
        PeerRegistry.addPeer(peer)
    }

    fun sendData(deviceId: String, data: String) {
        val value = data.toByteArray()
        
        // 1. Try as client (write to characteristic)
        val gatt = connectedGatts[deviceId]
        if (gatt != null) {
            val service = gatt.getService(SERVICE_UUID)
            val char = service?.getCharacteristic(CHAR_UUID)
            if (char != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(char, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    @Suppress("DEPRECATION")
                    char.value = value
                    gatt.writeCharacteristic(char)
                }
                Log.d(TAG, "Client: Sent ${value.size} bytes to $deviceId")
                return
            }
        }
        
        // 2. Try as server (notify characteristic)
        val device = serverDevices[deviceId]
        if (device != null && gattServer != null) {
            val service = gattServer?.getService(SERVICE_UUID)
            val char = service?.getCharacteristic(CHAR_UUID)
            if (char != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gattServer?.notifyCharacteristicChanged(device, char, false, value)
                } else {
                    @Suppress("DEPRECATION")
                    char.value = value
                    gattServer?.notifyCharacteristicChanged(device, char, false)
                }
                Log.d(TAG, "Server: Notified $deviceId with ${value.size} bytes")
            }
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping BLE Mesh Manager")
        stopScan()
        connectedGatts.values.forEach { it.close() }
        connectedGatts.clear()
        gattServer?.close()
        gattServer = null
        serverDevices.clear()
    }
}
