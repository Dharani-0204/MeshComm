package com.meshcomm.ui.map;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 \u00102\u0012\u0012\u0004\u0012\u00020\u0002\u0012\b\u0012\u00060\u0003R\u00020\u00000\u0001:\u0002\u0010\u0011B\u001b\u0012\u0014\b\u0002\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\u0002\u0010\u0007J\u001c\u0010\b\u001a\u00020\u00062\n\u0010\t\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\n\u001a\u00020\u000bH\u0016J\u001c\u0010\f\u001a\u00060\u0003R\u00020\u00002\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u000bH\u0016R\u001a\u0010\u0004\u001a\u000e\u0012\u0004\u0012\u00020\u0002\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/meshcomm/ui/map/SosListAdapter;", "Landroidx/recyclerview/widget/ListAdapter;", "Lcom/meshcomm/data/model/Message;", "Lcom/meshcomm/ui/map/SosListAdapter$VH;", "onSosClick", "Lkotlin/Function1;", "", "(Lkotlin/jvm/functions/Function1;)V", "onBindViewHolder", "h", "pos", "", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "DiffCB", "VH", "app_debug"})
public final class SosListAdapter extends androidx.recyclerview.widget.ListAdapter<com.meshcomm.data.model.Message, com.meshcomm.ui.map.SosListAdapter.VH> {
    @org.jetbrains.annotations.NotNull
    private final kotlin.jvm.functions.Function1<com.meshcomm.data.model.Message, kotlin.Unit> onSosClick = null;
    @org.jetbrains.annotations.NotNull
    public static final com.meshcomm.ui.map.SosListAdapter.DiffCB DiffCB = null;
    
    public SosListAdapter(@org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function1<? super com.meshcomm.data.model.Message, kotlin.Unit> onSosClick) {
        super(null);
    }
    
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public com.meshcomm.ui.map.SosListAdapter.VH onCreateViewHolder(@org.jetbrains.annotations.NotNull
    android.view.ViewGroup parent, int viewType) {
        return null;
    }
    
    @java.lang.Override
    public void onBindViewHolder(@org.jetbrains.annotations.NotNull
    com.meshcomm.ui.map.SosListAdapter.VH h, int pos) {
    }
    
    public SosListAdapter() {
        super(null);
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\u0003\u0018\u00002\b\u0012\u0004\u0012\u00020\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0003J\u0018\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016J\u0018\u0010\b\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00022\u0006\u0010\u0007\u001a\u00020\u0002H\u0016\u00a8\u0006\t"}, d2 = {"Lcom/meshcomm/ui/map/SosListAdapter$DiffCB;", "Landroidx/recyclerview/widget/DiffUtil$ItemCallback;", "Lcom/meshcomm/data/model/Message;", "()V", "areContentsTheSame", "", "a", "b", "areItemsTheSame", "app_debug"})
    public static final class DiffCB extends androidx.recyclerview.widget.DiffUtil.ItemCallback<com.meshcomm.data.model.Message> {
        
        private DiffCB() {
            super();
        }
        
        @java.lang.Override
        public boolean areItemsTheSame(@org.jetbrains.annotations.NotNull
        com.meshcomm.data.model.Message a, @org.jetbrains.annotations.NotNull
        com.meshcomm.data.model.Message b) {
            return false;
        }
        
        @java.lang.Override
        public boolean areContentsTheSame(@org.jetbrains.annotations.NotNull
        com.meshcomm.data.model.Message a, @org.jetbrains.annotations.NotNull
        com.meshcomm.data.model.Message b) {
            return false;
        }
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u000e\u0010\u000b\u001a\u00020\f2\u0006\u0010\r\u001a\u00020\u000eR\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/meshcomm/ui/map/SosListAdapter$VH;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "v", "Landroid/view/View;", "(Lcom/meshcomm/ui/map/SosListAdapter;Landroid/view/View;)V", "tvBattery", "Landroid/widget/TextView;", "tvCoords", "tvMsg", "tvName", "tvTime", "bind", "", "m", "Lcom/meshcomm/data/model/Message;", "app_debug"})
    public final class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
        @org.jetbrains.annotations.NotNull
        private final android.widget.TextView tvName = null;
        @org.jetbrains.annotations.NotNull
        private final android.widget.TextView tvCoords = null;
        @org.jetbrains.annotations.NotNull
        private final android.widget.TextView tvMsg = null;
        @org.jetbrains.annotations.NotNull
        private final android.widget.TextView tvTime = null;
        @org.jetbrains.annotations.NotNull
        private final android.widget.TextView tvBattery = null;
        
        public VH(@org.jetbrains.annotations.NotNull
        android.view.View v) {
            super(null);
        }
        
        public final void bind(@org.jetbrains.annotations.NotNull
        com.meshcomm.data.model.Message m) {
        }
    }
}