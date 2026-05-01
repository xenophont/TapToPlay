package com.xenophont.taptoplay.adyen

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class SaleToAcquirerDataFavoriteSerializationTest {
    @Test
    fun serializesFavoriteConfigs() {
        val json = Json { ignoreUnknownKeys = true }
        val favorites = listOf(
            SaleToAcquirerDataConfig(
                displayName = "Preauth",
                data = buildJsonObject {
                    put("additionalData", buildJsonObject {
                        put("authorisationType", "PreAuth")
                    })
                },
                mergeWithDefaults = false,
            ),
        )

        val encoded = json.encodeToString(ListSerializer(SaleToAcquirerDataConfig.serializer()), favorites)
        val decoded = json.decodeFromString(ListSerializer(SaleToAcquirerDataConfig.serializer()), encoded)

        assertEquals("Preauth", decoded.first().displayName)
        assertEquals(false, decoded.first().mergeWithDefaults)
        assertEquals(
            "PreAuth",
            decoded.first().data["additionalData"]?.jsonObject?.get("authorisationType")?.jsonPrimitive?.content,
        )
    }
}
