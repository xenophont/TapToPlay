package com.example.taptoplay.adyen

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleToAcquirerDataConfigTest {
    private val parser = SaleToAcquirerDataQrParser()

    @Test
    fun parsesStructuredSaleToAcquirerDataPayload() {
        val payload = """
            {
              "schema": "taptoplay.adyen.saleToAcquirerData.v1",
              "displayName": "Preauth test",
              "saleToAcquirerData": {
                "applicationInfo": {
                  "merchantApplication": {
                    "name": "TapToPlay",
                    "version": "2.13.05"
                  }
                },
                "metadata": {
                  "experiment": "qr"
                },
                "additionalData": {
                  "authorisationType": "PreAuth"
                }
              }
            }
        """.trimIndent()

        val config = parser.parse(payload).getOrThrow()

        assertEquals("Preauth test", config.displayName)
        assertEquals(
            "PreAuth",
            config.data["additionalData"]?.jsonObject?.get("authorisationType")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun remainsCompatibleWithLegacyPropertiesPayload() {
        val payload = """
            {
              "schema": "taptoplay.adyen.saleToAcquirerData.v1",
              "displayName": "Legacy preauth",
              "properties": {
                "additionalData": {
                  "authorisationType": "PreAuth"
                }
              }
            }
        """.trimIndent()

        val config = parser.parse(payload).getOrThrow()

        assertEquals(
            "PreAuth",
            config.data["additionalData"]?.jsonObject?.get("authorisationType")?.jsonPrimitive?.content,
        )
    }

    @Test
    fun rejectsUnknownSchema() {
        val payload = """
            {
              "schema": "other.schema",
              "displayName": "Bad payload",
              "saleToAcquirerData": {
                "additionalData": {
                  "authorisationType": "PreAuth"
                }
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
            data = buildJsonObject {
                put("metadata", buildJsonObject {
                    put("retailDemo", "Overridden")
                    put("experiment", "qr")
                })
                put("additionalData", buildJsonObject {
                    put("installments.value", 3)
                })
            },
        )

        val decoded = SaleToAcquirerDataEncoder.decodeBase64ForTest(
            SaleToAcquirerDataEncoder.encodeBase64(config),
        )

        assertEquals("Overridden", decoded["metadata"]?.jsonObject?.get("retailDemo")?.jsonPrimitive?.content)
        assertEquals("qr", decoded["metadata"]?.jsonObject?.get("experiment")?.jsonPrimitive?.content)
        assertEquals("3", decoded["additionalData"]?.jsonObject?.get("installments.value")?.jsonPrimitive?.content)
        assertEquals("TapToPlay", decoded["applicationInfo"]?.jsonObject?.get("merchantApplication")?.jsonObject?.get("name")?.jsonPrimitive?.content)
    }

    @Test
    fun editsNestedFieldValue() {
        val config = SaleToAcquirerDataConfig(
            displayName = "Editable",
            data = buildJsonObject {
                put("metadata", buildJsonObject {
                    put("experiment", "qr")
                })
            },
        )

        val edited = SaleToAcquirerDataEditor.update(config, listOf("metadata", "experiment"), "manual")

        assertEquals("manual", edited.data["metadata"]?.jsonObject?.get("experiment")?.jsonPrimitive?.content)
        assertEquals("Editable (edited)", edited.displayName)
    }

    @Test
    fun removesNestedFieldAndPrunesEmptyObjects() {
        val config = SaleToAcquirerDataConfig(
            displayName = "Editable",
            data = buildJsonObject {
                put("metadata", buildJsonObject {
                    put("experiment", "qr")
                })
            },
        )

        val edited = SaleToAcquirerDataEditor.remove(config, listOf("metadata", "experiment"))

        assertEquals(null, edited.data["metadata"])
    }
}
