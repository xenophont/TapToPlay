package com.xenophont.taptoplay.adyen

import com.xenophont.taptoplay.profiles.AdyenProfile
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Implements the protection format required for Adyen Terminal API messages.
 *
 * AES-CBC provides confidentiality while HMAC-SHA256 authenticates the plaintext. This is the
 * Adyen Nexo wire format and must not be replaced with a different cipher mode:
 * https://docs.adyen.com/point-of-sale/design-your-integration/choose-your-architecture/local/protect
 */
class NexoCrypto(
    private val random: SecureRandom = SecureRandom(),
) {
    /**
     * Encrypts a UTF-8 request for the Payments App App Link and always wipes [terminalApiRequest].
     * Callers must treat the input as consumed, including when encryption fails.
     */
    fun encryptToBase64Url(
        profile: AdyenProfile,
        terminalPassphrase: CharArray,
        terminalApiRequest: ByteArray,
    ): String {
        val nonce = ByteArray(IV_SIZE_BYTES).also(random::nextBytes)
        return try {
            val encryptedEnvelope = encrypt(profile, terminalPassphrase, terminalApiRequest, nonce)
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(encryptedEnvelope.toByteArray(Charsets.UTF_8))
        } finally {
            terminalApiRequest.fill(0)
            terminalPassphrase.fill('\u0000')
            nonce.fill(0)
        }
    }

    internal fun encrypt(
        profile: AdyenProfile,
        terminalPassphrase: CharArray,
        terminalApiRequest: ByteArray,
        nonce: ByteArray,
    ): String {
        require(nonce.size == IV_SIZE_BYTES) { "Nonce must be $IV_SIZE_BYTES bytes" }
        val keyMaterial = deriveKeyMaterial(terminalPassphrase)
        var cipherText: ByteArray? = null
        var signature: ByteArray? = null
        return try {
            cipherText = encryptPayload(terminalApiRequest, keyMaterial, nonce)
            signature = hmac(terminalApiRequest, keyMaterial.hmacKey)
            val messageHeader = extractMessageHeader(terminalApiRequest)
            val bodyKey = extractBodyKey(terminalApiRequest)
            val wrapped = buildJsonObject {
                put(bodyKey, buildJsonObject {
                    put("MessageHeader", messageHeader)
                    put("NexoBlob", Base64.getEncoder().encodeToString(cipherText))
                    put("SecurityTrailer", buildJsonObject {
                        put("KeyVersion", profile.terminalKeyVersion)
                        put("KeyIdentifier", profile.terminalKeyIdentifier)
                        put("Hmac", Base64.getEncoder().encodeToString(signature))
                        put("Nonce", Base64.getEncoder().encodeToString(nonce))
                        put("AdyenCryptoVersion", ADYEN_CRYPTO_VERSION)
                    })
                })
            }
            json.encodeToString(JsonObject.serializer(), wrapped)
        } finally {
            cipherText?.fill(0)
            signature?.fill(0)
            keyMaterial.clear()
        }
    }

    /**
     * Returns authenticated plaintext bytes. The caller owns the result and must wipe it.
     */
    fun decrypt(
        profile: AdyenProfile,
        terminalPassphrase: CharArray,
        encryptedEnvelopeJson: String,
    ): ByteArray {
        val root = json.parseToJsonElement(encryptedEnvelopeJson).jsonObject
        val saleToPoi = root["SaleToPOIRequest"]?.jsonObject
            ?: root["SaleToPOIResponse"]?.jsonObject
            ?: error("Missing SaleToPOIRequest or SaleToPOIResponse")
        val outerHeader = saleToPoi["MessageHeader"]?.jsonObject ?: error("Missing MessageHeader")
        val blob = saleToPoi["NexoBlob"]?.toStringValue() ?: error("Missing NexoBlob")
        val trailer = saleToPoi["SecurityTrailer"]?.jsonObject ?: error("Missing SecurityTrailer")
        require(trailer["AdyenCryptoVersion"]?.jsonPrimitive?.content?.toIntOrNull() == ADYEN_CRYPTO_VERSION) {
            "Unsupported Adyen crypto version"
        }
        require(trailer["KeyIdentifier"]?.toStringValue() == profile.terminalKeyIdentifier) {
            "Unexpected terminal key identifier"
        }
        require(trailer["KeyVersion"]?.jsonPrimitive?.content?.toIntOrNull() == profile.terminalKeyVersion) {
            "Unexpected terminal key version"
        }

        val nonce = Base64.getDecoder().decode(trailer["Nonce"]?.toStringValue() ?: error("Missing Nonce"))
        val expectedHmac = Base64.getDecoder().decode(trailer["Hmac"]?.toStringValue() ?: error("Missing Hmac"))
        val encrypted = Base64.getDecoder().decode(blob)
        val keyMaterial = deriveKeyMaterial(terminalPassphrase)
        var decrypted: ByteArray? = null
        var actualHmac: ByteArray? = null
        try {
            require(nonce.size == IV_SIZE_BYTES) { "Invalid nonce size" }
            decrypted = decryptPayload(encrypted, keyMaterial, nonce)
            actualHmac = hmac(decrypted, keyMaterial.hmacKey)
            require(MessageDigest.isEqual(actualHmac, expectedHmac)) { "HMAC validation failed" }
            require(extractMessageHeader(decrypted) == outerHeader) { "MessageHeader validation failed" }
            return decrypted.also { decrypted = null }
        } finally {
            decrypted?.fill(0)
            actualHmac?.fill(0)
            expectedHmac.fill(0)
            encrypted.fill(0)
            nonce.fill(0)
            keyMaterial.clear()
        }
    }

    internal fun decryptForTest(
        profile: AdyenProfile,
        terminalPassphrase: CharArray,
        encryptedEnvelopeJson: String,
    ): ByteArray = decrypt(profile, terminalPassphrase, encryptedEnvelopeJson)

    private fun encryptPayload(message: ByteArray, keyMaterial: KeyMaterial, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyMaterial.cipherKey, AES_ALGORITHM),
            IvParameterSpec(xor(keyMaterial.iv, nonce)),
        )
        return cipher.doFinal(message)
    }

    private fun decryptPayload(encrypted: ByteArray, keyMaterial: KeyMaterial, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyMaterial.cipherKey, AES_ALGORITHM),
            IvParameterSpec(xor(keyMaterial.iv, nonce)),
        )
        return cipher.doFinal(encrypted)
    }

    private fun hmac(message: ByteArray, hmacKey: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(hmacKey, HMAC_ALGORITHM))
        return mac.doFinal(message)
    }

    private fun deriveKeyMaterial(passphrase: CharArray): KeyMaterial {
        val spec = PBEKeySpec(
            passphrase,
            KEY_DERIVATION_SALT,
            KEY_DERIVATION_ROUNDS,
            KEY_MATERIAL_BITS,
        )
        var material: ByteArray? = null
        return try {
            material = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
                .generateSecret(spec)
                .encoded
            KeyMaterial(
                hmacKey = material.copyOfRange(0, HMAC_KEY_SIZE_BYTES),
                cipherKey = material.copyOfRange(HMAC_KEY_SIZE_BYTES, HMAC_KEY_SIZE_BYTES + CIPHER_KEY_SIZE_BYTES),
                iv = material.copyOfRange(HMAC_KEY_SIZE_BYTES + CIPHER_KEY_SIZE_BYTES, KEY_MATERIAL_SIZE_BYTES),
            )
        } finally {
            material?.fill(0)
            spec.clearPassword()
            passphrase.fill('\u0000')
        }
    }

    private fun extractMessageHeader(terminalApiRequest: ByteArray): JsonObject =
        json.parseToJsonElement(terminalApiRequest.decodeToString())
            .jsonObject
            .let { root -> root["SaleToPOIRequest"] ?: root["SaleToPOIResponse"] }
            ?.jsonObject
            ?.get("MessageHeader")
            ?.jsonObject
            ?: error("Terminal API payload must contain a SaleToPOI MessageHeader")

    private fun extractBodyKey(terminalApiPayload: ByteArray): String {
        val root = json.parseToJsonElement(terminalApiPayload.decodeToString()).jsonObject
        return when {
            "SaleToPOIRequest" in root -> "SaleToPOIRequest"
            "SaleToPOIResponse" in root -> "SaleToPOIResponse"
            else -> error("Terminal API payload must contain SaleToPOIRequest or SaleToPOIResponse")
        }
    }

    private fun xor(left: ByteArray, right: ByteArray): ByteArray =
        left.zip(right).map { (a, b) -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()

    private fun kotlinx.serialization.json.JsonElement.toStringValue(): String = jsonPrimitive.content

    private data class KeyMaterial(
        val hmacKey: ByteArray,
        val cipherKey: ByteArray,
        val iv: ByteArray,
    ) {
        fun clear() {
            hmacKey.fill(0)
            cipherKey.fill(0)
            iv.fill(0)
        }
    }

    companion object {
        private const val ADYEN_CRYPTO_VERSION = 1
        private const val IV_SIZE_BYTES = 16
        private const val HMAC_KEY_SIZE_BYTES = 32
        private const val CIPHER_KEY_SIZE_BYTES = 32
        private const val KEY_MATERIAL_SIZE_BYTES = 80
        private const val KEY_MATERIAL_BITS = KEY_MATERIAL_SIZE_BYTES * 8
        private const val KEY_DERIVATION_ROUNDS = 4_000
        private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val AES_ALGORITHM = "AES"
        private const val HMAC_ALGORITHM = "HmacSHA256"
        private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA1"
        private val KEY_DERIVATION_SALT = "AdyenNexoV1Salt".toByteArray(Charsets.UTF_8)
        private val json = Json { ignoreUnknownKeys = true }
    }
}
