package com.meshcomm.mesh;

/**
 * Stores outbound messages when no peers are connected.
 * Flushes them automatically when a peer connects.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000P\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0000\n\u0002\u0010 \n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0006\u0010\u0010\u001a\u00020\u0011J\u000e\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\u0014J\u0006\u0010\u0015\u001a\u00020\u0011J\u000e\u0010\u0016\u001a\u00020\u00112\u0006\u0010\u0017\u001a\u00020\bJ\u0006\u0010\u0018\u001a\u00020\u0019J\u000e\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00140\u001bH\u0002J\u0016\u0010\u001c\u001a\u00020\u00112\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00140\u001bH\u0002R\u000e\u0010\u0007\u001a\u00020\bX\u0082D\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000b\u001a\n \r*\u0004\u0018\u00010\f0\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001e"}, d2 = {"Lcom/meshcomm/mesh/StoreAndForwardQueue;", "", "context", "Landroid/content/Context;", "transportLayer", "Lcom/meshcomm/mesh/TransportLayer;", "(Landroid/content/Context;Lcom/meshcomm/mesh/TransportLayer;)V", "QUEUE_KEY", "", "gson", "Lcom/google/gson/Gson;", "prefs", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "scope", "Lkotlinx/coroutines/CoroutineScope;", "clearQueue", "", "enqueue", "message", "Lcom/meshcomm/data/model/Message;", "flushToAllPeers", "flushToPeer", "peerId", "getPendingCount", "", "loadQueue", "", "saveQueue", "queue", "app_debug"})
public final class StoreAndForwardQueue {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.mesh.TransportLayer transportLayer = null;
    @org.jetbrains.annotations.NotNull
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.CoroutineScope scope = null;
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String QUEUE_KEY = "pending_messages";
    
    public StoreAndForwardQueue(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    com.meshcomm.mesh.TransportLayer transportLayer) {
        super();
    }
    
    /**
     * Call this when no peers available — queues the message for later delivery
     */
    public final void enqueue(@org.jetbrains.annotations.NotNull
    com.meshcomm.data.model.Message message) {
    }
    
    /**
     * Call this when a new peer connects — flushes all queued messages
     */
    public final void flushToAllPeers() {
    }
    
    /**
     * Call this when a specific peer connects
     */
    public final void flushToPeer(@org.jetbrains.annotations.NotNull
    java.lang.String peerId) {
    }
    
    public final int getPendingCount() {
        return 0;
    }
    
    public final void clearQueue() {
    }
    
    private final java.util.List<com.meshcomm.data.model.Message> loadQueue() {
        return null;
    }
    
    private final void saveQueue(java.util.List<com.meshcomm.data.model.Message> queue) {
    }
}