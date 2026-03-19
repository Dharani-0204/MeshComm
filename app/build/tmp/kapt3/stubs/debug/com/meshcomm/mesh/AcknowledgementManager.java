package com.meshcomm.mesh;

/**
 * Handles sending and receiving ACK / "I'm coming" replies to SOS messages.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001:\u0001\u0015B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u0010\u0010\t\u001a\u0004\u0018\u00010\n2\u0006\u0010\u000b\u001a\u00020\fJ\u000e\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000b\u001a\u00020\fJ \u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\n2\u0006\u0010\u0012\u001a\u00020\n2\b\b\u0002\u0010\u0013\u001a\u00020\u0014R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0016"}, d2 = {"Lcom/meshcomm/mesh/AcknowledgementManager;", "", "context", "Landroid/content/Context;", "router", "Lcom/meshcomm/mesh/MessageRouter;", "(Landroid/content/Context;Lcom/meshcomm/mesh/MessageRouter;)V", "gson", "Lcom/google/gson/Gson;", "extractOriginalId", "", "message", "Lcom/meshcomm/data/model/Message;", "isAckMessage", "", "sendAck", "", "originalMessageId", "targetId", "ackType", "Lcom/meshcomm/mesh/AcknowledgementManager$AckType;", "AckType", "app_debug"})
public final class AcknowledgementManager {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.mesh.MessageRouter router = null;
    @org.jetbrains.annotations.NotNull
    private final com.google.gson.Gson gson = null;
    
    public AcknowledgementManager(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    com.meshcomm.mesh.MessageRouter router) {
        super();
    }
    
    public final void sendAck(@org.jetbrains.annotations.NotNull
    java.lang.String originalMessageId, @org.jetbrains.annotations.NotNull
    java.lang.String targetId, @org.jetbrains.annotations.NotNull
    com.meshcomm.mesh.AcknowledgementManager.AckType ackType) {
    }
    
    public final boolean isAckMessage(@org.jetbrains.annotations.NotNull
    com.meshcomm.data.model.Message message) {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String extractOriginalId(@org.jetbrains.annotations.NotNull
    com.meshcomm.data.model.Message message) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0002\b\u0005\b\u0086\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002j\u0002\b\u0003j\u0002\b\u0004j\u0002\b\u0005\u00a8\u0006\u0006"}, d2 = {"Lcom/meshcomm/mesh/AcknowledgementManager$AckType;", "", "(Ljava/lang/String;I)V", "ACK", "IM_COMING", "HELP_ON_WAY", "app_debug"})
    public static enum AckType {
        /*public static final*/ ACK /* = new ACK() */,
        /*public static final*/ IM_COMING /* = new IM_COMING() */,
        /*public static final*/ HELP_ON_WAY /* = new HELP_ON_WAY() */;
        
        AckType() {
        }
        
        @org.jetbrains.annotations.NotNull
        public static kotlin.enums.EnumEntries<com.meshcomm.mesh.AcknowledgementManager.AckType> getEntries() {
            return null;
        }
    }
}