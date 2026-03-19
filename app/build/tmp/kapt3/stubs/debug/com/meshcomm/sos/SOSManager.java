package com.meshcomm.sos;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u00002\u00020\u0001B\u001d\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0010\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\fJ\b\u0010\r\u001a\u00020\nH\u0002J\u0006\u0010\u000e\u001a\u00020\nR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/meshcomm/sos/SOSManager;", "", "context", "Landroid/content/Context;", "router", "Lcom/meshcomm/mesh/MessageRouter;", "locationProvider", "Lcom/meshcomm/location/LocationProvider;", "(Landroid/content/Context;Lcom/meshcomm/mesh/MessageRouter;Lcom/meshcomm/location/LocationProvider;)V", "sendSOS", "", "customMessage", "", "triggerLocalAlert", "triggerSOSAlert", "app_debug"})
public final class SOSManager {
    @org.jetbrains.annotations.NotNull
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.mesh.MessageRouter router = null;
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.location.LocationProvider locationProvider = null;
    
    public SOSManager(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    com.meshcomm.mesh.MessageRouter router, @org.jetbrains.annotations.NotNull
    com.meshcomm.location.LocationProvider locationProvider) {
        super();
    }
    
    public final void sendSOS(@org.jetbrains.annotations.NotNull
    java.lang.String customMessage) {
    }
    
    /**
     * Play alert + vibrate on the RECEIVING device
     */
    public final void triggerSOSAlert() {
    }
    
    private final void triggerLocalAlert() {
    }
}