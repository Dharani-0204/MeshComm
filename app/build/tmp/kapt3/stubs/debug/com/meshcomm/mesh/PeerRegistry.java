package com.meshcomm.mesh;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000L\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0010\b\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0012\u001a\u00020\u000b2\u0006\u0010\u0013\u001a\u00020\u0006J\u0006\u0010\u0014\u001a\u00020\u000bJ\u0016\u0010\u0015\u001a\u00020\u000b2\u0006\u0010\u0016\u001a\u00020\n2\u0006\u0010\u0017\u001a\u00020\nJ\u0006\u0010\u0018\u001a\u00020\u0019J\f\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005J>\u0010\u001b\u001a\u00020\u000b26\u0010\u001c\u001a2\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\u001d\u0012\b\b\u001e\u0012\u0004\b\b(\u0016\u0012\u0013\u0012\u00110\n\u00a2\u0006\f\b\u001d\u0012\b\b\u001e\u0012\u0004\b\b(\u0017\u0012\u0004\u0012\u00020\u000b0\tJ\u000e\u0010\u001f\u001a\u00020\u000b2\u0006\u0010 \u001a\u00020\nR\u001a\u0010\u0003\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R&\u0010\u0007\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u000b0\t0\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\f\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00060\u00050\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u001a\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u00060\u0011X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2 = {"Lcom/meshcomm/mesh/PeerRegistry;", "", "()V", "_peerFlow", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "Lcom/meshcomm/data/model/PeerDevice;", "messageCallbacks", "", "Lkotlin/Function2;", "", "", "peerFlow", "Lkotlinx/coroutines/flow/StateFlow;", "getPeerFlow", "()Lkotlinx/coroutines/flow/StateFlow;", "peers", "Ljava/util/concurrent/ConcurrentHashMap;", "addPeer", "peer", "clear", "dispatchIncoming", "data", "fromId", "getConnectedCount", "", "getConnectedPeers", "registerMessageCallback", "cb", "Lkotlin/ParameterName;", "name", "removePeer", "deviceId", "app_debug"})
public final class PeerRegistry {
    @org.jetbrains.annotations.NotNull
    private static final java.util.concurrent.ConcurrentHashMap<java.lang.String, com.meshcomm.data.model.PeerDevice> peers = null;
    @org.jetbrains.annotations.NotNull
    private static final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.meshcomm.data.model.PeerDevice>> _peerFlow = null;
    @org.jetbrains.annotations.NotNull
    private static final kotlinx.coroutines.flow.StateFlow<java.util.List<com.meshcomm.data.model.PeerDevice>> peerFlow = null;
    @org.jetbrains.annotations.NotNull
    private static final java.util.List<kotlin.jvm.functions.Function2<java.lang.String, java.lang.String, kotlin.Unit>> messageCallbacks = null;
    @org.jetbrains.annotations.NotNull
    public static final com.meshcomm.mesh.PeerRegistry INSTANCE = null;
    
    private PeerRegistry() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<java.util.List<com.meshcomm.data.model.PeerDevice>> getPeerFlow() {
        return null;
    }
    
    public final void addPeer(@org.jetbrains.annotations.NotNull
    com.meshcomm.data.model.PeerDevice peer) {
    }
    
    public final void removePeer(@org.jetbrains.annotations.NotNull
    java.lang.String deviceId) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<com.meshcomm.data.model.PeerDevice> getConnectedPeers() {
        return null;
    }
    
    public final int getConnectedCount() {
        return 0;
    }
    
    public final void registerMessageCallback(@org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function2<? super java.lang.String, ? super java.lang.String, kotlin.Unit> cb) {
    }
    
    public final void dispatchIncoming(@org.jetbrains.annotations.NotNull
    java.lang.String data, @org.jetbrains.annotations.NotNull
    java.lang.String fromId) {
    }
    
    public final void clear() {
    }
}