package com.example.taptoplay.adyen

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaleToAcquirerDataConfigTest {
    private val parser = SaleToAcquirerDataQrParser()

    @Test
    fun rejectsLegacySaleToAcquirerDataWrapper() {
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

        val result = parser.parse(payload)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Legacy TapToPlay") == true)
    }

    @Test
    fun rejectsLegacyPropertiesPayload() {
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

        val result = parser.parse(payload)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Legacy TapToPlay") == true)
    }

    @Test
    fun parsesPlainAdyenSaleToAcquirerDataObjectFromQr() {
        val payload = """
            {
              "applicationInfo": {
                "merchantApplication": {
                  "name": "NAME_OF_POS_APPLICATION",
                  "version": "2.13.05"
                }
              },
              "metadata": {
                "someMetaDataKey1": "YOUR_VALUE"
              },
              "shopperEmail": "S.Hopper@example.com",
              "additionalData": {
                "authorisationType": "PreAuth",
                "manualCapture": "false",
                "taxfree.indicator": false
              }
            }
        """.trimIndent()

        val config = parser.parse(payload).getOrThrow()

        assertEquals("Scanned SaleToAcquirerData", config.displayName)
        assertEquals(false, config.mergeWithDefaults)
        assertEquals("S.Hopper@example.com", config.data["shopperEmail"]?.jsonPrimitive?.content)
        assertEquals(
            false,
            config.data["additionalData"]?.jsonObject?.get("taxfree.indicator")?.jsonPrimitive?.booleanOrNull,
        )
    }

    @Test
    fun scannedPlainPayloadIsEncodedWithoutDemoDefaults() {
        val config = parser.parse(
            """
                {
                  "metadata": {
                    "someMetaDataKey1": "YOUR_VALUE"
                  }
                }
            """.trimIndent(),
        ).getOrThrow()

        val decoded = SaleToAcquirerDataEncoder.decodeBase64ForTest(
            SaleToAcquirerDataEncoder.encodeBase64(config),
        )

        assertEquals("YOUR_VALUE", decoded["metadata"]?.jsonObject?.get("someMetaDataKey1")?.jsonPrimitive?.content)
        assertEquals(null, decoded["applicationInfo"])
        assertEquals(null, decoded["metadata"]?.jsonObject?.get("retailDemo"))
    }

    @Test
    fun rejectsWrappedPayloadBeforeEncoding() {
        val result = parser.parse(
            """
                {
                  "schema": "taptoplay.adyen.saleToAcquirerData.v1",
                  "displayName": "Wrapped",
                  "saleToAcquirerData": {
                    "metadata": {
                      "someMetaDataKey1": "YOUR_VALUE"
                    }
                  }
                }
            """.trimIndent(),
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Legacy TapToPlay") == true)
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
