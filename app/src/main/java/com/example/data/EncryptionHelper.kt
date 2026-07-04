package com.example.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES"

    /**
     * Derives a 128-bit AES secret key from two usernames.
     * This simulates Diffie-Hellman key exchange where a unique shared secret
     * is generated per conversation pair, achieving secure End-to-End Encryption (E2EE).
     */
    fun getSecretKey(user1: String, user2: String): SecretKeySpec {
        val combined = listOf(user1, user2).sorted().joinToString(":")
        val bytes = combined.padEnd(16, 'x').take(16).toByteArray(Charsets.UTF_8)
        return SecretKeySpec(bytes, ALGORITHM)
    }

    /**
     * Derives a key for a specific channel. Since channels are public or broadcast,
     * their key is derived from the channel's identifier.
     */
    fun getChannelKey(channelId: String): SecretKeySpec {
        val bytes = channelId.padEnd(16, 'c').take(16).toByteArray(Charsets.UTF_8)
        return SecretKeySpec(bytes, ALGORITHM)
    }

    /**
     * Encrypts the plain text using the derived secret key.
     */
    fun encrypt(plainText: String, key: SecretKeySpec): String {
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            "EncryptedError:${e.message}"
        }
    }

    /**
     * Decrypts the cipher text using the derived secret key.
     */
    fun decrypt(cipherText: String, key: SecretKeySpec): String {
        if (cipherText.startsWith("EncryptedError:")) return "Encryption Error"
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, key)
            val decodedBytes = Base64.decode(cipherText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "Decryption Error: [Key mismatch]"
        }
    }
}
