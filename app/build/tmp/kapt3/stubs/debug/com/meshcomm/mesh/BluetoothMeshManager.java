package com.meshcomm.mesh;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000X\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u0007\u0018\u0000  2\u00020\u0001:\u0001 B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0018J\u0010\u0010\u0019\u001a\u00020\u00162\u0006\u0010\u001a\u001a\u00020\u001bH\u0002J\u0006\u0010\u001c\u001a\u00020\u0016J\u0006\u0010\u001d\u001a\u00020\u0016J\u0006\u0010\u001e\u001a\u00020\u0016J\u0006\u0010\u001f\u001a\u00020\u0016R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0013\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lcom/meshcomm/mesh/BluetoothMeshManager;", "", "context", "Landroid/content/Context;", "transportLayer", "Lcom/meshcomm/mesh/TransportLayer;", "(Landroid/content/Context;Lcom/meshcomm/mesh/TransportLayer;)V", "adapter", "Landroid/bluetooth/BluetoothAdapter;", "advertiseCallback", "Landroid/bluetooth/le/AdvertiseCallback;", "leAdvertiser", "Landroid/bluetooth/le/BluetoothLeAdvertiser;", "leScanner", "Landroid/bluetooth/le/BluetoothLeScanner;", "scanCallback", "Landroid/bluetooth/le/ScanCallback;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "serverSocket", "Landroid/bluetooth/BluetoothServerSocket;", "connectToDevice", "", "device", "Landroid/bluetooth/BluetoothDevice;", "handleSocket", "socket", "Landroid/bluetooth/BluetoothSocket;", "startAdvertising", "startDiscovery", "startServer", "stopAll", "Companion", "app_debug"})
@android.annotation.SuppressLint(value = {"MissingPermission"})
public final class BluetoothMeshManager {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.mesh.TransportLayer transportLayer = null;
    @org.jetbrains.annotations.NotNull
    private static final java.util.UUID SERVICE_UUID = null;
    @org.jetbrains.annotations.NotNull
    private static final java.util.UUID APP_UUID = null;
    @org.jetbrains.annotations.Nullable
    private final android.bluetooth.BluetoothAdapter adapter = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.Nullable
    private android.bluetooth.BluetoothServerSocket serverSocket;
    @org.jetbrains.annotations.Nullable
    private android.bluetooth.le.BluetoothLeAdvertiser leAdvertiser;
    @org.jetbrains.annotations.Nullable
    private android.bluetooth.le.BluetoothLeScanner leScanner;
    @org.jetbrains.annotations.NotNull
    private final android.bluetooth.le.AdvertiseCallback advertiseCallback = null;
    @org.jetbrains.annotations.NotNull
    private final android.bluetooth.le.ScanCallback scanCallback = null;
    @org.jetbrains.annotations.NotNull
    public static final com.meshcomm.mesh.BluetoothMeshManager.Companion Companion = null;
    
    public BluetoothMeshManager(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    com.meshcomm.mesh.TransportLayer transportLayer) {
        super();
    }
    
    public final void startServer() {
    }
    
    private final void handleSocket(android.bluetooth.BluetoothSocket socket) {
    }
    
    public final void connectToDevice(@org.jetbrains.annotations.NotNull
    android.bluetooth.BluetoothDevice device) {
    }
    
    public final void startAdvertising() {
    }
    
    public final void startDiscovery() {
    }
    
    public final void stopAll() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0014\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u0011\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006R\u0011\u0010\u0007\u001a\u00020\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u0006\u00a8\u0006\t"}, d2 = {"Lcom/meshcomm/mesh/BluetoothMeshManager$Companion;", "", "()V", "APP_UUID", "Ljava/util/UUID;", "getAPP_UUID", "()Ljava/util/UUID;", "SERVICE_UUID", "getSERVICE_UUID", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.util.UUID getSERVICE_UUID() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.util.UUID getAPP_UUID() {
            return null;
        }
    }
}