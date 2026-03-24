package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
import com.meshcomm.utils.PrefsHelper
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * FIXED BLE MESH MANAGER - Identity-based Filtering (BitChat Style)
 */
@SuppressLint("MissingPermission")
class BluetoothMeshManager(
    private val context: Context,
    private val transportLayer: TransportLayer
) {
    companion object {
        private const val TAG = "BLE_Mesh"
        // CRITICAL: BitChat-compatible Service UUID
        val MESH_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val MESH_CHARACTERISTIC_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? = bluetoothManager.adapter
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var gattServer: BluetoothGattServer? = null

    private val activeGatts = ConcurrentHashMap<String, BluetoothGatt>()

    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    /**
     * Requirement 5: REAL BATTERY VALUE
     */
    private fun getBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    /**
     * Requirement 2: MANDATORY IDENTITY BROADCAST
     */
    fun startAdvertising() {
        if (!isBluetoothEnabled()) return
        advertiser = adapter?.bluetoothLeAdvertiser ?: return

        val userName = PrefsHelper.getUserName(context).trim().take(10)
        val battery = getBatteryLevel()
        // Identity payload format: "name|battery"
        val payload = "$userName|$battery".toByteArray(Charsets.UTF_8)

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        // Requirement 1: Service UUID in main packet
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(MESH_SERVICE_UUID))
            .setIncludeDeviceName(false)
            .build()

        // Requirement 2: Identity in Scan Response to fit 31-byte limit
        val scanResponse = AdvertiseData.Builder()
            .addServiceData(ParcelUuid(MESH_SERVICE_UUID), payload)
            .build()

        advertiser?.startAdvertising(settings, data, scanResponse, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            Log.i(TAG, "Identity Advertising started successfully")
        }
        override fun onStartFailure(errorCode: Int) {
            Log.e(TAG, "Advertising failed: $errorCode")
        }
    }

    /**
     * Requirement 1: SHOW ONLY APP USERS (Filtering)
     */
    fun startDiscovery() {
        if (!isBluetoothEnabled()) return
        scanner = adapter?.bluetoothLeScanner ?: return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .build()

        // Raw scanning to avoid vendor-specific filter bugs, manual filtering in callback
        scanner?.startScan(null, settings, scanCallback)
        Log.i(TAG, "Identity-based scanning active")
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val record = result.scanRecord ?: return
            
            // Requirement 1: STRICT UUID FILTERING
            val serviceUuids = record.serviceUuids
            if (serviceUuids == null || !serviceUuids.contains(ParcelUuid(MESH_SERVICE_UUID))) {
                return // IGNORE NON-APP DEVICES COMPLETELY
            }

            // Requirement 3: STRICT DATA PARSING
            val data = record.getServiceData(ParcelUuid(MESH_SERVICE_UUID)) ?: return
            
            try {
                val raw = String(data, Charsets.UTF_8)
                val parts = raw.split("|")
                if (parts.size < 2) return

                val userName = parts[0].trim()
                val battery = parts[1].toIntOrNull() ?: return

                // Requirement 4: REMOVE UNKNOWN DEVICES
                if (userName.isEmpty()) return

                // Requirement 6: DEVICE LIST CONTROL (Unique MAC)
                val address = result.device.address
                PeerRegistry.addPeer(PeerDevice(
                    deviceId = address,
                    deviceName = userName,
                    batteryLevel = battery,
                    transport = TransportType.BLUETOOTH,
                    rssi = result.rssi
                ))
                
                Log.d(TAG, "Found Mesh User: $userName (Battery: $battery%) Address: $address")
                
                // Requirement: Auto-connect for data exchange
                connectToDevice(result.device)
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (activeGatts.containsKey(device.address)) return
        Log.d(TAG, "Client: Connecting to ${device.address}")
        device.connectGatt(context, false, gattClientCallback, BluetoothDevice.TRANSPORT_LE)
    }

    private val gattClientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Client: Connected to $address. Discovering services...")
                gatt.discoverServices()
                activeGatts[address] = gatt
                PeerRegistry.updateConnectionStatus(address, true)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Client: Disconnected from $address")
                activeGatts.remove(address)?.close()
                PeerRegistry.updateConnectionStatus(address, false)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(MESH_SERVICE_UUID)
                val characteristic = service?.getCharacteristic(MESH_CHARACTERISTIC_UUID)
                if (characteristic != null) {
                    Log.d(TAG, "Client: Mesh characteristic found on ${gatt.device.address}")
                    gatt.setCharacteristicNotification(characteristic, true)
                    val descriptor = characteristic.getDescriptor(CCCD_UUID)
                    if (descriptor != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        } else {
                            @Suppress("DEPRECATION")
                            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            @Suppress("DEPRECATION")
                            gatt.writeDescriptor(descriptor)
                        }
                    }
                    transportLayer.registerChannel(gatt.device.address, BLEOutputStream(gatt, characteristic))
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == MESH_CHARACTERISTIC_UUID) {
                val value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    characteristic.value
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value
                }
                PeerRegistry.dispatchIncoming(String(value, Charsets.UTF_8), gatt.device.address)
            }
        }
        
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == MESH_CHARACTERISTIC_UUID) {
                PeerRegistry.dispatchIncoming(String(value, Charsets.UTF_8), gatt.device.address)
            }
        }
    }

    fun startServer() {
        if (gattServer != null) return
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        
        val service = BluetoothGattService(MESH_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val characteristic = BluetoothGattCharacteristic(
            MESH_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
        Log.i(TAG, "Mesh GATT Server initialized")
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice, requestId: Int, characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            if (characteristic.uuid == MESH_CHARACTERISTIC_UUID) {
                value?.let {
                    PeerRegistry.dispatchIncoming(String(it, Charsets.UTF_8), device.address)
                }
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            }
        }

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            Log.d(TAG, "Server: Connection state change for ${device.address} -> $newState")
        }
    }

    fun stopAll() {
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            scanner?.stopScan(scanCallback)
            activeGatts.values.forEach { it.close() }
            activeGatts.clear()
            gattServer?.close()
            gattServer = null
        } catch (e: Exception) {
            Log.e(TAG, "Cleanup error: ${e.message}")
        }
    }

    private inner class BLEOutputStream(private val gatt: BluetoothGatt, private val char: BluetoothGattCharacteristic) : java.io.OutputStream() {
        override fun write(b: Int) {}
        override fun write(b: ByteArray, off: Int, len: Int) {
            val data = b.sliceArray(off until off + len)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(char, data, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                @Suppress("DEPRECATION")
                char.value = data
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        }
    }
}
