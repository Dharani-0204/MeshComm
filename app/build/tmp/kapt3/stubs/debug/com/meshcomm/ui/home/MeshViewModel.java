package com.meshcomm.ui.home;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000j\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0007\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010%\u001a\u00020&2\u0006\u0010\'\u001a\u00020(J\b\u0010)\u001a\u00020&H\u0002J\u0016\u0010*\u001a\u00020&2\u0006\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020.J&\u0010/\u001a\u00020&2\u0006\u00100\u001a\u00020,2\u0006\u00101\u001a\u00020,2\u0006\u0010+\u001a\u00020,2\u0006\u0010-\u001a\u00020.J\u0010\u00102\u001a\u00020&2\b\b\u0002\u00103\u001a\u00020,J\u000e\u00104\u001a\u00020&2\u0006\u0010\'\u001a\u00020(R\u001c\u0010\u0005\u001a\u0010\u0012\f\u0012\n \b*\u0004\u0018\u00010\u00070\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\t\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\n0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u000b\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\r0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u001d\u0010\u0010\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\r0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u000fR\"\u0010\u0014\u001a\u0004\u0018\u00010\u00132\b\u0010\u0012\u001a\u0004\u0018\u00010\u0013@BX\u0086\u000e\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0017\u0010\u0017\u001a\b\u0012\u0004\u0012\u00020\u00070\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u001aR\u0019\u0010\u001b\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\n0\u0018\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001aR\u000e\u0010\u001d\u001a\u00020\u001eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u001f\u001a\u00020 \u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010\"R\u001d\u0010#\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\r0\f\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u000f\u00a8\u00065"}, d2 = {"Lcom/meshcomm/ui/home/MeshViewModel;", "Landroidx/lifecycle/AndroidViewModel;", "application", "Landroid/app/Application;", "(Landroid/app/Application;)V", "_meshStats", "Landroidx/lifecycle/MutableLiveData;", "Lcom/meshcomm/data/model/MeshStats;", "kotlin.jvm.PlatformType", "_newMessage", "Lcom/meshcomm/data/model/Message;", "allMessages", "Lkotlinx/coroutines/flow/Flow;", "", "getAllMessages", "()Lkotlinx/coroutines/flow/Flow;", "broadcastMessages", "getBroadcastMessages", "<set-?>", "Lcom/meshcomm/mesh/MeshService;", "meshService", "getMeshService", "()Lcom/meshcomm/mesh/MeshService;", "meshStats", "Landroidx/lifecycle/LiveData;", "getMeshStats", "()Landroidx/lifecycle/LiveData;", "newMessage", "getNewMessage", "repo", "Lcom/meshcomm/data/repository/MessageRepository;", "serviceConnection", "Landroid/content/ServiceConnection;", "getServiceConnection", "()Landroid/content/ServiceConnection;", "sosMessages", "getSosMessages", "bindService", "", "context", "Landroid/content/Context;", "observeRouter", "sendBroadcast", "content", "", "includeLocation", "", "sendDirect", "targetId", "targetName", "sendSOS", "message", "unbindService", "app_debug"})
public final class MeshViewModel extends androidx.lifecycle.AndroidViewModel {
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.data.repository.MessageRepository repo = null;
    @org.jetbrains.annotations.NotNull
    private final androidx.lifecycle.MutableLiveData<com.meshcomm.data.model.MeshStats> _meshStats = null;
    @org.jetbrains.annotations.NotNull
    private final androidx.lifecycle.LiveData<com.meshcomm.data.model.MeshStats> meshStats = null;
    @org.jetbrains.annotations.NotNull
    private final androidx.lifecycle.MutableLiveData<com.meshcomm.data.model.Message> _newMessage = null;
    @org.jetbrains.annotations.NotNull
    private final androidx.lifecycle.LiveData<com.meshcomm.data.model.Message> newMessage = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.Flow<java.util.List<com.meshcomm.data.model.Message>> broadcastMessages = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.Flow<java.util.List<com.meshcomm.data.model.Message>> sosMessages = null;
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.Flow<java.util.List<com.meshcomm.data.model.Message>> allMessages = null;
    @org.jetbrains.annotations.Nullable
    private com.meshcomm.mesh.MeshService meshService;
    @org.jetbrains.annotations.NotNull
    private final android.content.ServiceConnection serviceConnection = null;
    
    public MeshViewModel(@org.jetbrains.annotations.NotNull
    android.app.Application application) {
        super(null);
    }
    
    @org.jetbrains.annotations.NotNull
    public final androidx.lifecycle.LiveData<com.meshcomm.data.model.MeshStats> getMeshStats() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final androidx.lifecycle.LiveData<com.meshcomm.data.model.Message> getNewMessage() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.meshcomm.data.model.Message>> getBroadcastMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.meshcomm.data.model.Message>> getSosMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.meshcomm.data.model.Message>> getAllMessages() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final com.meshcomm.mesh.MeshService getMeshService() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final android.content.ServiceConnection getServiceConnection() {
        return null;
    }
    
    private final void observeRouter() {
    }
    
    public final void bindService(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
    }
    
    public final void unbindService(@org.jetbrains.annotations.NotNull
    android.content.Context context) {
    }
    
    public final void sendBroadcast(@org.jetbrains.annotations.NotNull
    java.lang.String content, boolean includeLocation) {
    }
    
    public final void sendDirect(@org.jetbrains.annotations.NotNull
    java.lang.String targetId, @org.jetbrains.annotations.NotNull
    java.lang.String targetName, @org.jetbrains.annotations.NotNull
    java.lang.String content, boolean includeLocation) {
    }
    
    public final void sendSOS(@org.jetbrains.annotations.NotNull
    java.lang.String message) {
    }
}