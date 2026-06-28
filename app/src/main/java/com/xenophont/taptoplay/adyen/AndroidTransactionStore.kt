package com.xenophont.taptoplay.adyen

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Encrypted local diagnostics history. Full Terminal API request/response payloads are retained
 * intentionally for this demo's inspector, so this store must not be replaced with plain prefs.
 */
class AndroidTransactionStore(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "adyen_transactions",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun records(): List<TransactionRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(TransactionRecord.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    fun save(record: TransactionRecord) {
        val updated = (listOf(record) + records().filterNot { it.id == record.id })
            .take(MAX_RECORDS)
        write(updated)
    }

    fun update(recordId: String, update: (TransactionRecord) -> TransactionRecord) {
        val updated = records().map { record ->
            if (record.id == recordId) update(record) else record
        }
        write(updated)
    }

    fun clear() = write(emptyList())

    private fun write(records: List<TransactionRecord>) {
        prefs.edit()
            .putString(KEY_RECORDS, json.encodeToString(ListSerializer(TransactionRecord.serializer()), records))
            .apply()
    }

    companion object {
        private const val KEY_RECORDS = "records"
        private const val MAX_RECORDS = 30
    }
}
