package com.meshcomm.mesh;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000V\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010#\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u0018\u001a\u00020\u0019J\u0016\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\u00162\u0006\u0010\u001d\u001a\u00020\u0016J!\u0010\u001e\u001a\u00020\u001b2\u0006\u0010\u001f\u001a\u00020\u000b2\u0006\u0010\u001d\u001a\u00020\u0016H\u0082@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010 J\u000e\u0010!\u001a\u00020\u001b2\u0006\u0010\u001f\u001a\u00020\u000bR\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u000b0\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u0011R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\""}, d2 = {"Lcom/meshcomm/mesh/MessageRouter;", "", "context", "Landroid/content/Context;", "repository", "Lcom/meshcomm/data/repository/MessageRepository;", "transportLayer", "Lcom/meshcomm/mesh/TransportLayer;", "(Landroid/content/Context;Lcom/meshcomm/data/repository/MessageRepository;Lcom/meshcomm/mesh/TransportLayer;)V", "_incomingMessages", "Lkotlinx/coroutines/flow/MutableSharedFlow;", "Lcom/meshcomm/data/model/Message;", "gson", "Lcom/google/gson/Gson;", "incomingMessages", "Lkotlinx/coroutines/flow/SharedFlow;", "getIncomingMessages", "()Lkotlinx/coroutines/flow/SharedFlow;", "scope", "Lkotlinx/coroutines/CoroutineScope;", "seenMessages", "", "", "selfId", "getSeenCount", "", "onRawDataReceived", "", "json", "fromPeerId", "processMessage", "message", "(Lcom/meshcomm/data/model/Message;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sendMessage", "app_debug"})
public final class MessageRouter {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.data.repository.MessageRepository repository = null;
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.mesh.TransportLayer transportLayer = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String selfId = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.Set<java.lang.String> seenMessages = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.CoroutineScope scope = null;
    @org.jetbrains.annotations.NotNull
    private final com.google.gson.Gson gson = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.MutableSharedFlow<com.meshcomm.data.model.Message> _incomingMessages = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.SharedFlow<com.meshcomm.data.model.Message> incomingMessages = null;
    
    public MessageRouter(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    com.meshcomm.data.repository.MessageRepository repository, @org.jetbrains.annotations.NotNull
    com.meshcomm.mesh.TransportLayer transportLayer) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.SharedFlow<com.meshcomm.data.model.Message> getIncomingMessages() {
        return null;
    }
    
    /**
     * Called when raw JSON arrives from any transport
     */
    public final void onRawDataReceived(@org.jetbrains.annotations.NotNull
    java.lang.String json, @org.jetbrains.annotations.NotNull
    java.lang.String fromPeerId) {
    }
    
    private final java.lang.Object processMessage(com.meshcomm.data.model.Message message, java.lang.String fromPeerId, kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Send a new message originating from this device
     */
    public final void sendMessage(@org.jetbrains.annotations.NotNull
    com.meshcomm.data.model.Message message) {
    }
    
    public final int getSeenCount() {
        return 0;
    }
}