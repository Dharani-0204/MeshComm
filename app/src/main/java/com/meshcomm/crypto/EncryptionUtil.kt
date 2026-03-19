package com.meshcomm.crypto

import android.util.Base64
import com.meshcomm.data.model.Message
import com.meshcomm.data.model.MessageType
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtil {

    // In production, exchange keys via public-key handshake.
    // For hackathon demo: shared pre-known key.
    private const val SECRET_KEY = "MeshComm16ByteK!"  // 16 chars = 128-bit AES
    private const val IV_VALUE    = "MeshCommIV16Byt!"  // 16 chars

    private fun getKey(): SecretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), "AES")
    private fun getIv(): IvParameterSpec = IvParameterSpec(IV_VALUE.toByteArray())

    fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), getIv())
            val encrypted = cipher.doFinal(plainText.toByteArray())
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) {
            plainText  // fallback: send plain if encryption fails
        }
    }

    fun decrypt(cipherText: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, getKey(), getIv())
            val decoded = Base64.decode(cipherText, Base64.NO_WRAP)
            String(cipher.doFinal(decoded))
        } catch (e: Exception) {
            cipherText  // fallback: return as-is
        }
    }

    /** Encrypts content of SOS or direct messages before sending */
    fun encryptMessage(message: Message): Message {
        if (message.type != MessageType.SOS && message.targetId == null) return message
        return message.copy(
            content = encrypt(message.content),
            isEncrypted = true
        )
    }

    /** Decrypts content if the message is marked encrypted */
    fun decryptMessage(message: Message): Message {
        if (!message.isEncrypted) return message
        return message.copy(
            content = decrypt(message.content),
            isEncrypted = false
        )
    }
}
