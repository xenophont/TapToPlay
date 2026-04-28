package com.example.taptoplay.adyen

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleToAcquirerDataConfigTest {
    private val parser = SaleToAcquirerDataQrParser()

    @Test
    fun parsesQrPropertiesPayload() {
        val payload = """
            {
              "schema": "taptoplay.adyen.saleToAcquirerData.v1",
              "displayName": "Preauth test",
              "properties": {
                "authorisationType": "PreAuth",
                "metadata.experiment": "qr"
              }
            }
        """.trimIndent()

        val config = parser.parse(payload).getOrThrow()

        assertEquals("Preauth test", config.displayName)
        assertEquals("PreAuth", config.properties["authorisationType"]?.jsonPrimitive?.content)
    }

    @Test
    fun rejectsUnknownSchema() {
        val payload = """
            {
              "schema": "other.schema",
              "displayName": "Bad payload",
              "properties": {
                "authorisationType": "PreAuth"
              }
            }
        """.trimIndent()

        val result = parser.parse(payload)

        assertTrue(result.isFailure)
    }

    @Test
    fun encodesMergedPropertiesAsBase64Json() {
        val config = SaleToAcquirerDataConfig(
            displayName = "Installments",
            properties = buildJsonObject {
                put("metadata.retailDemo", "Overridden")
                put("installments.value", JsonPrimitive(3))
            },
        )

        val decoded = SaleToAcquirerDataEncoder.decodeBase64ForTest(
            SaleToAcquirerDataEncoder.encodeBase64(config),
        )

        assertEquals("Overridden", decoded["metadata.retailDemo"]?.jsonPrimitive?.content)
        assertEquals("3", decoded["installments.value"]?.jsonPrimitive?.content)
    }
}
