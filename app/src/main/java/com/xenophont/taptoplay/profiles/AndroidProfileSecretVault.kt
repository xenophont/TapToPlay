package com.xenophont.taptoplay.profiles

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores only AES-GCM ciphertext in preferences. The non-exportable wrapping key lives in Android
 * Keystore, so profile lists and Compose state never need to retain plaintext credentials.
 */
class AndroidProfileSecretVault(context: Context) : ProfileSecretVault {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun put(profileId: String, secrets: ProfileSecrets) {
        val plaintext = SecretBinaryCodec.encode(secrets)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        try {
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            cipher.updateAAD(profileId.encodeToByteArray())
            val ciphertext = cipher.doFinal(plaintext)
            try {
                val record = Base64.getEncoder().encodeToString(cipher.iv) +
                    RECORD_SEPARATOR +
                    Base64.getEncoder().encodeToString(ciphertext)
                check(prefs.edit().putString(profileId, record).commit()) {
                    "Could not persist encrypted profile secrets"
                }
            } finally {
                ciphertext.fill(0)
            }
        } finally {
            plaintext.fill(0)
        }
    }

    override fun <T> withSecrets(profileId: String, block: (ProfileSecrets) -> T): T? {
        val record = prefs.getString(profileId, null) ?: return null
        val parts = record.split(RECORD_SEPARATOR, limit = 2)
        require(parts.size == 2) { "Invalid encrypted profile secret record" }
        val iv = Base64.getDecoder().decode(parts[0])
        val ciphertext = Base64.getDecoder().decode(parts[1])
        var plaintext: ByteArray? = null
        var secrets: ProfileSecrets? = null
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(profileId.encodeToByteArray())
            plaintext = cipher.doFinal(ciphertext)
            secrets = SecretBinaryCodec.decode(plaintext)
            block(secrets)
        } finally {
            secrets?.close()
            plaintext?.fill(0)
            ciphertext.fill(0)
            iv.fill(0)
        }
    }

    override fun contains(profileId: String): Boolean = prefs.contains(profileId)

    override fun remove(profileId: String) {
        check(prefs.edit().remove(profileId).commit()) { "Could not remove encrypted profile secrets" }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFS_NAME = "adyen_profile_secret_vault"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "taptoplay.profile-secrets.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val RECORD_SEPARATOR = ":"
    }
}

internal object SecretBinaryCodec {
    fun encode(secrets: ProfileSecrets): ByteArray {
        val values = arrayOf(
            secrets.apiKeyCopy(),
            secrets.clientKeyCopy(),
            secrets.terminalPassphraseCopy(),
        )
        val encoded = values.map(::utf8)
        values.forEach { it.fill('\u0000') }
        return try {
            val output = ByteBuffer.allocate(encoded.sumOf { Int.SIZE_BYTES + it.size })
            encoded.forEach {
                output.putInt(it.size)
                output.put(it)
            }
            output.array()
        } finally {
            encoded.forEach { it.fill(0) }
        }
    }

    fun decode(bytes: ByteArray): ProfileSecrets {
        val input = ByteBuffer.wrap(bytes)
        val values = ArrayList<CharArray>(3)
        repeat(3) {
            require(input.remaining() >= Int.SIZE_BYTES) { "Invalid secret payload" }
            val size = input.int
            require(size in 0..input.remaining()) { "Invalid secret field length" }
            val encoded = ByteArray(size)
            input.get(encoded)
            try {
                values += chars(encoded)
            } finally {
                encoded.fill(0)
            }
        }
        require(!input.hasRemaining()) { "Unexpected secret payload data" }
        return try {
            ProfileSecrets(values[0], values[1], values[2])
        } finally {
            values.forEach { it.fill('\u0000') }
        }
    }

    private fun utf8(chars: CharArray): ByteArray {
        val buffer = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(chars))
        return try {
            ByteArray(buffer.remaining()).also(buffer::get)
        } finally {
            if (buffer.hasArray()) buffer.array().fill(0)
        }
    }

    private fun chars(bytes: ByteArray): CharArray {
        val buffer = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes))
        return try {
            CharArray(buffer.remaining()).also(buffer::get)
        } finally {
            if (buffer.hasArray()) buffer.array().fill('\u0000')
        }
    }
}
