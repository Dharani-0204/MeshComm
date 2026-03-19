package com.meshcomm.utils;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0007\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0014\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00040\u000b2\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u000e\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u000f\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u0010\u001a\u00020\u00112\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u0012\u001a\u00020\u00132\u0006\u0010\f\u001a\u00020\rJ\u0010\u0010\u0014\u001a\u00020\u00152\u0006\u0010\f\u001a\u00020\rH\u0002J\u001c\u0010\u0016\u001a\u00020\u00172\u0006\u0010\f\u001a\u00020\r2\f\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u00040\u000bJ\u000e\u0010\u0019\u001a\u00020\u00172\u0006\u0010\f\u001a\u00020\rJ\u0016\u0010\u001a\u001a\u00020\u00172\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\u0004J\u0016\u0010\u001c\u001a\u00020\u00172\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u001d\u001a\u00020\u0011R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001e"}, d2 = {"Lcom/meshcomm/utils/PrefsHelper;", "", "()V", "KEY_EMERGENCY_CONTACTS", "", "KEY_SETUP_DONE", "KEY_USER_ID", "KEY_USER_NAME", "KEY_USER_ROLE", "PREFS_NAME", "getEmergencyContacts", "", "ctx", "Landroid/content/Context;", "getUserId", "getUserName", "getUserRole", "Lcom/meshcomm/data/model/UserRole;", "isSetupDone", "", "prefs", "Landroid/content/SharedPreferences;", "setEmergencyContacts", "", "contacts", "setSetupDone", "setUserName", "name", "setUserRole", "role", "app_debug"})
public final class PrefsHelper {
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String PREFS_NAME = "meshcomm_prefs";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String KEY_USER_ID = "user_id";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String KEY_USER_NAME = "user_name";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String KEY_USER_ROLE = "user_role";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String KEY_SETUP_DONE = "setup_done";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String KEY_EMERGENCY_CONTACTS = "emergency_contacts";
    @org.jetbrains.annotations.NotNull
    public static final com.meshcomm.utils.PrefsHelper INSTANCE = null;
    
    private PrefsHelper() {
        super();
    }
    
    private final android.content.SharedPreferences prefs(android.content.Context ctx) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getUserId(@org.jetbrains.annotations.NotNull
    android.content.Context ctx) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getUserName(@org.jetbrains.annotations.NotNull
    android.content.Context ctx) {
        return null;
    }
    
    public final void setUserName(@org.jetbrains.annotations.NotNull
    android.content.Context ctx, @org.jetbrains.annotations.NotNull
    java.lang.String name) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final com.meshcomm.data.model.UserRole getUserRole(@org.jetbrains.annotations.NotNull
    android.content.Context ctx) {
        return null;
    }
    
    public final void setUserRole(@org.jetbrains.annotations.NotNull
    android.content.Context ctx, @org.jetbrains.annotations.NotNull
    com.meshcomm.data.model.UserRole role) {
    }
    
    public final boolean isSetupDone(@org.jetbrains.annotations.NotNull
    android.content.Context ctx) {
        return false;
    }
    
    public final void setSetupDone(@org.jetbrains.annotations.NotNull
    android.content.Context ctx) {
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<java.lang.String> getEmergencyContacts(@org.jetbrains.annotations.NotNull
    android.content.Context ctx) {
        return null;
    }
    
    public final void setEmergencyContacts(@org.jetbrains.annotations.NotNull
    android.content.Context ctx, @org.jetbrains.annotations.NotNull
    java.util.List<java.lang.String> contacts) {
    }
}