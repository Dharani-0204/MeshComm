package com.meshcomm.ui.map;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\b\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J$\u0010\u0010\u001a\u00020\u00112\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u00132\f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013H\u0002J\u0010\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0019H\u0002J\b\u0010\u001a\u001a\u00020\u0011H\u0002J$\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u001e2\b\u0010\u001f\u001a\u0004\u0018\u00010 2\b\u0010!\u001a\u0004\u0018\u00010\"H\u0016J\b\u0010#\u001a\u00020\u0011H\u0016J\u001a\u0010$\u001a\u00020\u00112\u0006\u0010%\u001a\u00020\u001c2\b\u0010&\u001a\u0004\u0018\u00010\"H\u0016J\b\u0010\'\u001a\u00020\u0011H\u0002J\b\u0010(\u001a\u00020\u0011H\u0002J\b\u0010)\u001a\u00020\u0011H\u0002R\u0010\u0010\u0003\u001a\u0004\u0018\u00010\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0005\u001a\u00020\u00048BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u000e\u0010\b\u001a\u00020\tX\u0082.\u00a2\u0006\u0002\n\u0000R\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\r\u00a8\u0006*"}, d2 = {"Lcom/meshcomm/ui/map/MapFragment;", "Landroidx/fragment/app/Fragment;", "()V", "_binding", "Lcom/meshcomm/databinding/FragmentMapBinding;", "binding", "getBinding", "()Lcom/meshcomm/databinding/FragmentMapBinding;", "sosListAdapter", "Lcom/meshcomm/ui/map/SosListAdapter;", "viewModel", "Lcom/meshcomm/ui/home/MeshViewModel;", "getViewModel", "()Lcom/meshcomm/ui/home/MeshViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "drawLocationOverview", "", "allMessages", "", "Lcom/meshcomm/data/model/Message;", "sosMessages", "formatTimestamp", "", "timestamp", "", "observeData", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "saved", "Landroid/os/Bundle;", "onDestroyView", "onViewCreated", "view", "savedInstanceState", "setupClickListeners", "setupRecyclerView", "showEmergencyZones", "app_debug"})
public final class MapFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.Nullable
    private com.meshcomm.databinding.FragmentMapBinding _binding;
    @org.jetbrains.annotations.NotNull
    private final kotlin.Lazy viewModel$delegate = null;
    private com.meshcomm.ui.map.SosListAdapter sosListAdapter;
    
    public MapFragment() {
        super();
    }
    
    private final com.meshcomm.databinding.FragmentMapBinding getBinding() {
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
    
    private final void setupRecyclerView() {
    }
    
    private final void setupClickListeners() {
    }
    
    private final void observeData() {
    }
    
    private final void drawLocationOverview(java.util.List<com.meshcomm.data.model.Message> allMessages, java.util.List<com.meshcomm.data.model.Message> sosMessages) {
    }
    
    private final java.lang.String formatTimestamp(long timestamp) {
        return null;
    }
    
    private final void showEmergencyZones() {
    }
    
    @java.lang.Override
    public void onDestroyView() {
    }
}