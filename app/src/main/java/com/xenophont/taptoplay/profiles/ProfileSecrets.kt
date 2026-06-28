package com.xenophont.taptoplay.profiles

/**
 * Short-lived plaintext credentials. Instances own their arrays and must be closed after use.
 *
 * Some Android/JVM APIs (QR scanners, JSON decoders, and OkHttp headers) necessarily create
 * immutable String values. This type minimizes the lifetime of our own mutable plaintext copies;
 * it cannot promise complete JVM heap zeroization.
 */
class ProfileSecrets(
    apiKey: CharArray,
    clientKey: CharArray,
    terminalPassphrase: CharArray,
) : AutoCloseable {
    private val apiKey = apiKey.copyOf()
    private val clientKey = clientKey.copyOf()
    private val terminalPassphrase = terminalPassphrase.copyOf()

    fun apiKeyString(): String = String(apiKey)

    fun clientKeyString(): String = String(clientKey)

    internal fun apiKeyCopy(): CharArray = apiKey.copyOf()

    internal fun clientKeyCopy(): CharArray = clientKey.copyOf()

    fun terminalPassphraseCopy(): CharArray = terminalPassphrase.copyOf()

    fun copy(): ProfileSecrets = ProfileSecrets(apiKey, clientKey, terminalPassphrase)

    override fun close() {
        apiKey.fill('\u0000')
        clientKey.fill('\u0000')
        terminalPassphrase.fill('\u0000')
    }

    internal fun isClearedForTest(): Boolean =
        apiKey.all { it == '\u0000' } &&
            clientKey.all { it == '\u0000' } &&
            terminalPassphrase.all { it == '\u0000' }

    companion object {
        fun fromStrings(apiKey: String, clientKey: String, terminalPassphrase: String): ProfileSecrets {
            val apiChars = apiKey.toCharArray()
            val clientChars = clientKey.toCharArray()
            val passphraseChars = terminalPassphrase.toCharArray()
            return try {
                ProfileSecrets(apiChars, clientChars, passphraseChars)
            } finally {
                apiChars.fill('\u0000')
                clientChars.fill('\u0000')
                passphraseChars.fill('\u0000')
            }
        }
    }
}

data class ImportedAdyenProfile(
    val profile: AdyenProfile,
    val secrets: ProfileSecrets,
) : AutoCloseable {
    override fun close() = secrets.close()
}
