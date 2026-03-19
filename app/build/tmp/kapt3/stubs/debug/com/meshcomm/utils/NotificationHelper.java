package com.meshcomm.utils;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fJ\u000e\u0010\r\u001a\u00020\u000e2\u0006\u0010\t\u001a\u00020\nJ\u001e\u0010\u000f\u001a\u00020\u000e2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0010\u001a\u00020\u00042\u0006\u0010\u0011\u001a\u00020\u0004J\u0016\u0010\u0012\u001a\u00020\u000e2\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u0010\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/meshcomm/utils/NotificationHelper;", "", "()V", "CHANNEL_MESH", "", "CHANNEL_MSG", "CHANNEL_SOS", "buildServiceNotification", "Landroid/app/Notification;", "context", "Landroid/content/Context;", "peerCount", "", "createChannels", "", "showMessageNotification", "senderName", "preview", "showSOSNotification", "app_debug"})
public final class NotificationHelper {
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String CHANNEL_MESH = "mesh_service";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String CHANNEL_SOS = "sos_alerts";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String CHANNEL_MSG = "messages";
    @org.jetbrains.annotations.NotNull
    public static final com.meshcomm.utils.NotificationHelper INSTANCE = null;
    
    private NotificationHelper() {
        super();
    }
    
    public final void createChannels(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final android.app.Notification buildServiceNotification(@org.jetbrains.annotations.NotNull
    android.content.Context context, int peerCount) {
        return null;
    }
    
    public final void showSOSNotification(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    java.lang.String senderName) {
    }
    
    public final void showMessageNotification(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    java.lang.String senderName, @org.jetbrains.annotations.NotNull
    java.lang.String preview) {
    }
}