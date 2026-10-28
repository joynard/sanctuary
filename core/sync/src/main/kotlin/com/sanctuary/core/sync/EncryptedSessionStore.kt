package com.sanctuary.core.sync

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Tokens are encrypted with a non-exportable Android Keystore AES/GCM key before
 * they touch SharedPreferences. Never log or back up the ciphertext.
 */
class EncryptedSessionStore(context: Context) : SessionStore {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun read(): AuthSession? = withContext(Dispatchers.IO) {
        mutex.withLock {
            val payload = prefs.getString(KEY_SESSION, null) ?: return@withLock null
            runCatching {
                val plaintext = decrypt(payload)
                json.decodeFromString<AuthSession>(plaintext)
            }.getOrElse {
                // A reset/restored device can lose the Keystore key. Do not leave
                // an undecryptable token that would block a fresh activation.
                prefs.edit().remove(KEY_SESSION).commit()
                null
            }
        }
    }

    override suspend fun write(session: AuthSession) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val ciphertext = encrypt(json.encodeToString(session))
                prefs.edit().putString(KEY_SESSION, ciphertext).commit()
            }
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                prefs.edit().remove(KEY_SESSION).commit()
            }
        }
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return base64(cipher.iv) + "." + base64(ciphertext)
    }

    private fun decrypt(payload: String): String {
        val parts = payload.split('.', limit = 2)
        require(parts.size == 2) { "Malformed encrypted session" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun base64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP)

    private companion object {
        const val PREFS_NAME = "sanctuary_cloud_session"
        const val KEY_SESSION = "encrypted_session"
        const val KEY_ALIAS = "sanctuary_cloud_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
