package com.example.taptoplay.adyen

import com.example.taptoplay.profiles.AdyenProfile
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

class NexoCrypto(
    private val random: SecureRandom = SecureRandom(),
) {
    fun encryptToBase64Url(profile: AdyenProfile, terminalApiRequestJson: String): String {
        val encryptedEnvelope = encrypt(profile, terminalApiRequestJson)
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(encryptedEnvelope.toByteArray(Charsets.UTF_8))
    }

    fun encrypt(profile: AdyenProfile, terminalApiRequestJson: String): String {
        val nonce = ByteArray(IV_SIZE_BYTES).also(random::nextBytes)
        return encrypt(profile, terminalApiRequestJson, nonce)
    }

    internal fun encrypt(profile: AdyenProfile, terminalApiRequestJson: String, nonce: ByteArray): String {
        require(nonce.size == IV_SIZE_BYTES) { "Nonce must be $IV_SIZE_BYTES bytes" }
        val keyMaterial = deriveKeyMaterial(profile.terminalPassphrase)
        val messageBytes = terminalApiRequestJson.toByteArray(Charsets.UTF_8)
        val cipherText = encryptPayload(messageBytes, keyMaterial, nonce)
        val hmac = hmac(messageBytes, keyMaterial.hmacKey)
        val messageHeader = extractMessageHeader(terminalApiRequestJson)
        val wrapped = buildJsonObject {
            put("SaleToPOIRequest", buildJsonObject {
                put("MessageHeader", messageHeader)
                put("NexoBlob", Base64.getEncoder().encodeToString(cipherText))
                put("SecurityTrailer", buildJsonObject {
                    put("KeyVersion", profile.terminalKeyVersion)
                    put("KeyIdentifier", profile.terminalKeyIdentifier)
                    put("Hmac", Base64.getEncoder().encodeToString(hmac))
                    put("Nonce", Base64.getEncoder().encodeToString(nonce))
                    put("AdyenCryptoVersion", ADYEN_CRYPTO_VERSION)
                })
            })
        }
        return json.encodeToString(JsonObject.serializer(), wrapped)
    }

    internal fun decryptForTest(profile: AdyenProfile, encryptedEnvelopeJson: String): String {
        val saleToPoi = json.parseToJsonElement(encryptedEnvelopeJson)
            .jsonObject["SaleToPOIRequest"]
            ?.jsonObject
            ?: error("Missing SaleToPOIRequest")
        val blob = saleToPoi["NexoBlob"]?.toStringValue() ?: error("Missing NexoBlob")
        val trailer = saleToPoi["SecurityTrailer"]?.jsonObject ?: error("Missing SecurityTrailer")
        val nonce = Base64.getDecoder().decode(trailer["Nonce"]?.toStringValue() ?: error("Missing Nonce"))
        val expectedHmac = Base64.getDecoder().decode(trailer["Hmac"]?.toStringValue() ?: error("Missing Hmac"))
        val keyMaterial = deriveKeyMaterial(profile.terminalPassphrase)
        val decrypted = decryptPayload(Base64.getDecoder().decode(blob), keyMaterial, nonce)
        require(hmac(decrypted, keyMaterial.hmacKey).contentEquals(expectedHmac)) { "HMAC validation failed" }
        return decrypted.toString(Charsets.UTF_8)
    }

    private fun encryptPayload(message: ByteArray, keyMaterial: KeyMaterial, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(keyMaterial.cipherKey, "AES"),
            IvParameterSpec(xor(keyMaterial.iv, nonce)),
        )
        return cipher.doFinal(message)
    }

    private fun decryptPayload(encrypted: ByteArray, keyMaterial: KeyMaterial, nonce: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(keyMaterial.cipherKey, "AES"),
            IvParameterSpec(xor(keyMaterial.iv, nonce)),
        )
        return cipher.doFinal(encrypted)
    }

    private fun hmac(message: ByteArray, hmacKey: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(hmacKey, "HmacSHA256"))
        return mac.doFinal(message)
    }

    private fun deriveKeyMaterial(passphrase: String): KeyMaterial {
        val spec = PBEKeySpec(
            passphrase.toCharArray(),
            "AdyenNexoV1Salt".toByteArray(Charsets.UTF_8),
            4000,
            640,
        )
        val material = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(spec)
            .encoded
        return KeyMaterial(
            hmacKey = material.copyOfRange(0, 32),
            cipherKey = material.copyOfRange(32, 64),
            iv = material.copyOfRange(64, 80),
        )
    }

    private fun extractMessageHeader(terminalApiRequestJson: String): JsonObject =
        json.parseToJsonElement(terminalApiRequestJson)
            .jsonObject["SaleToPOIRequest"]
            ?.jsonObject
            ?.get("MessageHeader")
            ?.jsonObject
            ?: error("Terminal API request must contain SaleToPOIRequest.MessageHeader")

    private fun xor(left: ByteArray, right: ByteArray): ByteArray =
        left.zip(right).map { (a, b) -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()

    private fun kotlinx.serialization.json.JsonElement.toStringValue(): String = jsonPrimitive.content

    private data class KeyMaterial(
        val hmacKey: ByteArray,
        val cipherKey: ByteArray,
        val iv: ByteArray,
    )

    companion object {
        private const val ADYEN_CRYPTO_VERSION = 1
        private const val IV_SIZE_BYTES = 16
        private val json = Json { ignoreUnknownKeys = true }
    }
}
