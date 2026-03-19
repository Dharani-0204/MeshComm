package com.meshcomm.data.repository;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0012\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\bJ\u001b\u0010\u000b\u001a\u0004\u0018\u00010\n2\u0006\u0010\f\u001a\u00020\rH\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u000eJ\u0019\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\nH\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0012J\f\u0010\u0013\u001a\u00020\n*\u00020\u0014H\u0002J\f\u0010\u0015\u001a\u00020\u0014*\u00020\nH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0016"}, d2 = {"Lcom/meshcomm/data/repository/UserRepository;", "", "db", "Lcom/meshcomm/data/db/AppDatabase;", "(Lcom/meshcomm/data/db/AppDatabase;)V", "gson", "Lcom/google/gson/Gson;", "getAllUsers", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/meshcomm/data/model/User;", "getUser", "userId", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveUser", "", "user", "(Lcom/meshcomm/data/model/User;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "toDomain", "Lcom/meshcomm/data/db/UserEntity;", "toEntity", "app_debug"})
public final class UserRepository {
    @org.jetbrains.annotations.NotNull
    private final com.meshcomm.data.db.AppDatabase db = null;
    @org.jetbrains.annotations.NotNull
    private final com.google.gson.Gson gson = null;
    
    public UserRepository(@org.jetbrains.annotations.NotNull
    com.meshcomm.data.db.AppDatabase db) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object saveUser(@org.jetbrains.annotations.NotNull
    com.meshcomm.data.model.User user, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object getUser(@org.jetbrains.annotations.NotNull
    java.lang.String userId, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super com.meshcomm.data.model.User> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.meshcomm.data.model.User>> getAllUsers() {
        return null;
    }
    
    private final com.meshcomm.data.db.UserEntity toEntity(com.meshcomm.data.model.User $this$toEntity) {
        return null;
    }
    
    private final com.meshcomm.data.model.User toDomain(com.meshcomm.data.db.UserEntity $this$toDomain) {
        return null;
    }
}