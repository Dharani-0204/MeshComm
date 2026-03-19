package com.meshcomm.crypto;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0006\u001a\u00020\u00042\u0006\u0010\u0007\u001a\u00020\u0004J\u000e\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\tJ\u000e\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\u0004J\u000e\u0010\r\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\tJ\b\u0010\u000e\u001a\u00020\u000fH\u0002J\b\u0010\u0010\u001a\u00020\u0011H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0012"}, d2 = {"Lcom/meshcomm/crypto/EncryptionUtil;", "", "()V", "IV_VALUE", "", "SECRET_KEY", "decrypt", "cipherText", "decryptMessage", "Lcom/meshcomm/data/model/Message;", "message", "encrypt", "plainText", "encryptMessage", "getIv", "Ljavax/crypto/spec/IvParameterSpec;", "getKey", "Ljavax/crypto/spec/SecretKeySpec;", "app_debug"})
public final class EncryptionUtil {
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String SECRET_KEY = "MeshComm16ByteK!";
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String IV_VALUE = "MeshCommIV16Byt!";
    @org.jetbrains.annotations.NotNull
    public static final com.meshcomm.crypto.EncryptionUtil INSTANCE = null;
    
    private EncryptionUtil() {
        super();
    }
    
    private final javax.crypto.spec.SecretKeySpec getKey() {
        return null;
    }
    
    private final javax.crypto.spec.IvParameterSpec getIv() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String encrypt(@org.jetbrains.annotations.NotNull
    java.lang.String plainText) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String decrypt(@org.jetbrains.annotations.NotNull
    java.lang.String cipherText) {
        return null;
    }
    
    /**
     * Encrypts content of SOS or direct messages before sending
     */
    @org.jetbrains.annotations.NotNull
    public final com.meshcomm.data.model.Message encryptMessage(@org.jetbrains.annotations.NotNull
    com.meshcomm.data.model.Message message) {
        return null;
    }
    
    /**
     * Decrypts content if the message is marked encrypted
     */
    @org.jetbrains.annotations.NotNull
    public final com.meshcomm.data.model.Message decryptMessage(@org.jetbrains.annotations.NotNull
    com.meshcomm.data.model.Message message) {
        return null;
    }
}