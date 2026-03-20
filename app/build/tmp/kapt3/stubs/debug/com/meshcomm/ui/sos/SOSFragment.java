package com.meshcomm.ui.sos;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0006\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\n\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0002J\b\u0010\u0016\u001a\u00020\u0017H\u0002J$\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u001b2\b\u0010\u001c\u001a\u0004\u0018\u00010\u001d2\b\u0010\u001e\u001a\u0004\u0018\u00010\u001fH\u0016J\b\u0010 \u001a\u00020\u0017H\u0016J\u001a\u0010!\u001a\u00020\u00172\u0006\u0010\"\u001a\u00020\u00192\b\u0010#\u001a\u0004\u0018\u00010\u001fH\u0016J\u0010\u0010$\u001a\u00020\u00172\u0006\u0010%\u001a\u00020&H\u0002J\u0010\u0010\'\u001a\u00020\u00172\u0006\u0010(\u001a\u00020)H\u0002J\b\u0010*\u001a\u00020\u0017H\u0002J\b\u0010+\u001a\u00020\u0017H\u0002J\b\u0010,\u001a\u00020\u0017H\u0002J\b\u0010-\u001a\u00020\u0017H\u0002J\u0010\u0010.\u001a\u00020\u00172\u0006\u0010(\u001a\u00020)H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\tR\u0010\u0010\n\u001a\u0004\u0018\u00010\u000bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\f\u001a\u0004\u0018\u00010\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000e\u001a\u00020\u000f8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0012\u0010\u0013\u001a\u0004\b\u0010\u0010\u0011\u00a8\u0006/"}, d2 = {"Lcom/meshcomm/ui/sos/SOSFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/meshcomm/databinding/FragmentSosBinding;", "adapter", "Lcom/meshcomm/ui/broadcast/MessageAdapter;", "binding", "getBinding", "()Lcom/meshcomm/databinding/FragmentSosBinding;", "speechHelper", "Lcom/meshcomm/utils/SpeechToTextHelper;", "vibrator", "Landroid/os/Vibrator;", "viewModel", "Lcom/meshcomm/ui/home/MeshViewModel;", "getViewModel", "()Lcom/meshcomm/ui/home/MeshViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "getCurrentLocation", "Landroid/location/Location;", "observeData", "", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "saved", "Landroid/os/Bundle;", "onDestroyView", "onViewCreated", "view", "savedInstanceState", "sendQuickSOS", "message", "", "sendSOS", "isCritical", "", "setupClickListeners", "setupRecyclerView", "setupSpeechToText", "setupVibrator", "vibrateEmergency", "app_debug"})
public final class SOSFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.Nullable
    private com.meshcomm.databinding.FragmentSosBinding _binding;
    @org.jetbrains.annotations.NotNull
    private final kotlin.Lazy viewModel$delegate = null;
    private com.meshcomm.ui.broadcast.MessageAdapter adapter;
    @org.jetbrains.annotations.Nullable
    private com.meshcomm.utils.SpeechToTextHelper speechHelper;
    @org.jetbrains.annotations.Nullable
    private android.os.Vibrator vibrator;
    
    public SOSFragment() {
        super();
    }
    
    private final com.meshcomm.databinding.FragmentSosBinding getBinding() {
        return null;
    }
    
    private final com.meshcomm.ui.home.MeshViewModel getViewModel() {
        return null;
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public android.view.View onCreateView(@org.jetbrains.annotations.NotNull
    android.view.LayoutInflater inflater, @org.jetbrains.annotations.Nullable
    android.view.ViewGroup container, @org.jetbrains.annotations.Nullable
    android.os.Bundle saved) {
        return null;
    }
    
    @java.lang.Override
    public void onViewCreated(@org.jetbrains.annotations.NotNull
    android.view.View view, @org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void setupVibrator() {
    }
    
    private final void setupRecyclerView() {
    }
    
    private final void setupSpeechToText() {
    }
    
    private final void observeData() {
    }
    
    private final void setupClickListeners() {
    }
    
    private final void sendSOS(boolean isCritical) {
    }
    
    private final void sendQuickSOS(java.lang.String message) {
    }
    
    private final android.location.Location getCurrentLocation() {
        return null;
    }
    
    private final void vibrateEmergency(boolean isCritical) {
    }
    
    @java.lang.Override
    public void onDestroyView() {
    }
}