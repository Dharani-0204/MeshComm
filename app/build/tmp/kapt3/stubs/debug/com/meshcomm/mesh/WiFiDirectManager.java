package com.meshcomm.mesh;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0007\u0018\u0000 &2\u00020\u0001:\u0001&B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0016H\u0002J\u0010\u0010\u0017\u001a\u00020\u00142\u0006\u0010\u0018\u001a\u00020\u0019H\u0002J\u0010\u0010\u001a\u001a\u00020\u00142\u0006\u0010\u001b\u001a\u00020\u001cH\u0002J\u000e\u0010\u001d\u001a\u00020\u00142\u0006\u0010\u001e\u001a\u00020\u001fJ\u000e\u0010 \u001a\u00020\u00142\u0006\u0010!\u001a\u00020\"J\u0006\u0010#\u001a\u00020\u0014J\b\u0010$\u001a\u00020\u0014H\u0002J\u0006\u0010%\u001a\u00020\u0014R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0011\u001a\u0004\u0018\u00010\u0012X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\'"}, d2 = {"Lcom/meshcomm/mesh/WiFiDirectManager;", "", "context", "Landroid/content/Context;", "transportLayer", "Lcom/meshcomm/mesh/TransportLayer;", "(Landroid/content/Context;Lcom/meshcomm/mesh/TransportLayer;)V", "channel", "Landroid/net/wifi/p2p/WifiP2pManager$Channel;", "connectionListener", "Landroid/net/wifi/p2p/WifiP2pManager$ActionListener;", "isGroupOwner", "", "manager", "Landroid/net/wifi/p2p/WifiP2pManager;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "serverSocket", "Ljava/net/ServerSocket;", "connectToDevice", "", "device", "Landroid/net/wifi/p2p/WifiP2pDevice;", "connectToGroupOwner", "address", "", "handleClientSocket", "socket", "Ljava/net/Socket;", "onConnectionInfoAvailable", "info", "Landroid/net/wifi/p2p/WifiP2pInfo;", "onPeersAvailable", "peerList", "Landroid/net/wifi/p2p/WifiP2pDeviceList;", "startDiscovery", "startSocketServer", "stopAll", "Companion", "app_debug"})
@android.annotation.SuppressLint(value = {"MissingPermission"})
public final class WiFiDirectManager {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.mesh.TransportLayer transportLayer = null;
    public static final int PORT = 8988;
    @org.jetbrains.annotations.NotNull
    private final android.net.wifi.p2p.WifiP2pManager manager = null;
    @org.jetbrains.annotations.NotNull
    private final android.net.wifi.p2p.WifiP2pManager.Channel channel = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.Nullable
    private java.net.ServerSocket serverSocket;
    private boolean isGroupOwner = false;
    @org.jetbrains.annotations.NotNull
    private final android.net.wifi.p2p.WifiP2pManager.ActionListener connectionListener = null;
    @org.jetbrains.annotations.NotNull
    public static final com.meshcomm.mesh.WiFiDirectManager.Companion Companion = null;
    
    public WiFiDirectManager(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    com.meshcomm.mesh.TransportLayer transportLayer) {
        super();
    }
    
    public final void startDiscovery() {
    }
    
    public final void onPeersAvailable(@org.jetbrains.annotations.NotNull
    android.net.wifi.p2p.WifiP2pDeviceList peerList) {
    }
    
    private final void connectToDevice(android.net.wifi.p2p.WifiP2pDevice device) {
    }
    
    public final void onConnectionInfoAvailable(@org.jetbrains.annotations.NotNull
    android.net.wifi.p2p.WifiP2pInfo info) {
    }
    
    private final void startSocketServer() {
    }
    
    private final void handleClientSocket(java.net.Socket socket) {
    }
    
    private final void connectToGroupOwner(java.lang.String address) {
    }
    
    public final void stopAll() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/meshcomm/mesh/WiFiDirectManager$Companion;", "", "()V", "PORT", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}