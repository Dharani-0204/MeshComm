package com.meshcomm.mesh;

/**
 * Abstraction that holds outbound write channels for each connected peer.
 * BluetoothMeshManager and WiFiDirectManager register their sockets here.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\"\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\u0018\u00002\u00020\u0001:\u0001\u0015B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0006\u0010\u0007\u001a\u00020\bJ\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u00050\nJ\u0016\u0010\u000b\u001a\u00020\b2\u0006\u0010\f\u001a\u00020\u00052\u0006\u0010\r\u001a\u00020\u000eJ\u000e\u0010\u000f\u001a\u00020\b2\u0006\u0010\u0010\u001a\u00020\u0005J\u0016\u0010\u0011\u001a\u00020\b2\u0006\u0010\u0010\u001a\u00020\u00052\u0006\u0010\u0012\u001a\u00020\u0005J\u0016\u0010\u0013\u001a\u00020\b2\u0006\u0010\f\u001a\u00020\u00052\u0006\u0010\u0010\u001a\u00020\u0005J\u000e\u0010\u0014\u001a\u00020\b2\u0006\u0010\f\u001a\u00020\u0005R\u001a\u0010\u0003\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00060\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/meshcomm/mesh/TransportLayer;", "", "()V", "channels", "Ljava/util/concurrent/ConcurrentHashMap;", "", "Lcom/meshcomm/mesh/TransportLayer$Channel;", "disconnectAll", "", "getConnectedIds", "", "registerChannel", "peerId", "outputStream", "Ljava/io/OutputStream;", "sendToAll", "data", "sendToAllExcept", "excludeId", "sendToPeer", "unregisterChannel", "Channel", "app_debug"})
public final class TransportLayer {
    @org.jetbrains.annotations.NotNull
    private final java.util.concurrent.ConcurrentHashMap<java.lang.String, com.meshcomm.mesh.TransportLayer.Channel> channels = null;
    
    public TransportLayer() {
        super();
    }
    
    public final void registerChannel(@org.jetbrains.annotations.NotNull
    java.lang.String peerId, @org.jetbrains.annotations.NotNull
    java.io.OutputStream outputStream) {
    }
    
    public final void unregisterChannel(@org.jetbrains.annotations.NotNull
    java.lang.String peerId) {
    }
    
    public final void sendToAll(@org.jetbrains.annotations.NotNull
    java.lang.String data) {
    }
    
    public final void sendToAllExcept(@org.jetbrains.annotations.NotNull
    java.lang.String data, @org.jetbrains.annotations.NotNull
    java.lang.String excludeId) {
    }
    
    public final void sendToPeer(@org.jetbrains.annotations.NotNull
    java.lang.String peerId, @org.jetbrains.annotations.NotNull
    java.lang.String data) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.Set<java.lang.String> getConnectedIds() {
        return null;
    }
    
    public final void disconnectAll() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u000b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\r\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0011\u001a\u00020\u0012H\u00d6\u0001J\t\u0010\u0013\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0014"}, d2 = {"Lcom/meshcomm/mesh/TransportLayer$Channel;", "", "peerId", "", "outputStream", "Ljava/io/OutputStream;", "(Ljava/lang/String;Ljava/io/OutputStream;)V", "getOutputStream", "()Ljava/io/OutputStream;", "getPeerId", "()Ljava/lang/String;", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class Channel {
        @org.jetbrains.annotations.NotNull
        private final java.lang.String peerId = null;
        @org.jetbrains.annotations.NotNull
        private final java.io.OutputStream outputStream = null;
        
        public Channel(@org.jetbrains.annotations.NotNull
        java.lang.String peerId, @org.jetbrains.annotations.NotNull
        java.io.OutputStream outputStream) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.lang.String getPeerId() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.io.OutputStream getOutputStream() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.lang.String component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.io.OutputStream component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.meshcomm.mesh.TransportLayer.Channel copy(@org.jetbrains.annotations.NotNull
        java.lang.String peerId, @org.jetbrains.annotations.NotNull
        java.io.OutputStream outputStream) {
            return null;
        }
        
        @java.lang.Override
        public boolean equals(@org.jetbrains.annotations.Nullable
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public java.lang.String toString() {
            return null;
        }
    }
}