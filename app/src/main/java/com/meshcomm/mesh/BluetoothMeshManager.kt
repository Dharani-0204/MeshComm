package com.meshcomm.mesh

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.meshcomm.data.model.PeerDevice
import com.meshcomm.data.model.TransportType
import com.meshcomm.utils.PrefsHelper
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothMeshManager(
    private val context: Context,
    private val transportLayer: TransportLayer
) {
    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val APP_UUID: UUID     = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    }

    private val adapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: BluetoothServerSocket? = null
    private var leAdvertiser: BluetoothLeAdvertiser? = null
    private var leScanner: BluetoothLeScanner? = null

    // ── Classic RFCOMM Server ────────────────────────────────────────────────

    fun startServer() {
        scope.launch {
            try {
                serverSocket = adapter?.listenUsingRfcommWithServiceRecord("MeshComm", SERVICE_UUID)
                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    launch { handleSocket(socket) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun handleSocket(socket: BluetoothSocket) {
        val deviceId = socket.remoteDevice.address
        val deviceName = socket.remoteDevice.name ?: deviceId

        PeerRegistry.addPeer(PeerDevice(deviceId, deviceName, TransportType.BLUETOOTH))
        transportLayer.registerChannel(deviceId, socket.outputStream)

        try {
            val reader = BufferedReader(InputStreamReader(socket.inputStream))
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

    // ── Classic RFCOMM Client ────────────────────────────────────────────────

    fun connectToDevice(device: BluetoothDevice) {
        scope.launch {
            try {
                val socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
                socket.connect()
                handleSocket(socket)
            } catch (e: Exception) { e.printStackTrace() }
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
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {}
        override fun onStartFailure(errorCode: Int) {}
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

        // Also do classic discovery for older devices
        adapter?.startDiscovery()
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!transportLayer.getConnectedIds().contains(device.address)) {
                connectToDevice(device)
            }
        }
    }

    fun stopAll() {
        scope.cancel()
        leScanner?.stopScan(scanCallback)
        leAdvertiser?.stopAdvertising(advertiseCallback)
        adapter?.cancelDiscovery()
        runCatching { serverSocket?.close() }
    }
}
