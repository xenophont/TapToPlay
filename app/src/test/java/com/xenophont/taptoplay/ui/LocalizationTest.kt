package com.xenophont.taptoplay.ui

import com.xenophont.taptoplay.catalog.ProductCatalog
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationTest {
    @Test
    fun everyLocaleDefinesTheSameTextKeys() {
        val expectedKeys = resourceFiles().getValue("values").strings.keys

        resourceFiles().forEach { (qualifier, file) ->
            assertEquals("Missing or extra keys for $qualifier", expectedKeys, file.strings.keys)
        }
    }

    @Test
    fun everyLocaleDefinesTheSamePluralKeys() {
        val expectedKeys = resourceFiles().getValue("values").plurals.keys

        assertEquals(
            setOf("array_item_count", "cart_item_ready", "field_count", "item_count", "json_field_count", "loaded_instance", "saved_attempt"),
            expectedKeys,
        )
        resourceFiles().forEach { (qualifier, file) ->
            assertEquals("Missing or extra plural keys for $qualifier", expectedKeys, file.plurals.keys)
            file.plurals.forEach { (name, quantities) ->
                assertEquals("$qualifier should define one/other for $name", setOf("one", "other"), quantities.keys)
            }
        }
    }

    @Test
    fun appLanguageTagsMatchResourceDirectories() {
        assertEquals("Nederlands", AppLanguage.Dutch.nativeName)
        assertEquals("Euskara", AppLanguage.Basque.nativeName)
        assertEquals("Easter egg", AppLanguage.Quenya.englishName)
        assertEquals(
            listOf("en", "es", "nl", "fr", "de", "it", "sv", "ja", "zh-Hans", "ko", "eu", "qya"),
            AppLanguage.entries.map { it.tag },
        )
        assertEquals(
            setOf("values", "values-es", "values-nl", "values-fr", "values-de", "values-it", "values-sv", "values-ja", "values-b+zh+Hans", "values-ko", "values-eu", "values-b+qya"),
            resourceFiles().keys,
        )
    }

    @Test
    fun catalogCopyIsAvailableForAllLocales() {
        resourceFiles().forEach { (qualifier, file) ->
            val strings = file.strings
            ProductCatalog.products.forEach { product ->
                assertFalse("$qualifier should name ${product.id}", strings.getValue(product.id.productNameKey()).isBlank())
                assertFalse("$qualifier should describe ${product.id}", strings.getValue(product.id.productDescriptionKey()).isBlank())
            }
            ProductCatalog.categories.forEach { category ->
                assertFalse("$qualifier should label $category", strings.getValue(category.categoryKey()).isBlank())
            }
        }
    }

    @Test
    fun supportScreensHaveLocalizedLabels() {
        resourceFiles().forEach { (_, file) ->
            val strings = file.strings
            assertTrue(strings.getValue("screen_language").isNotBlank())
            assertTrue(strings.getValue("screen_about").isNotBlank())
            assertTrue(strings.getValue("privacy_policy").isNotBlank())
        }
    }

    @Test
    fun requestedLocalesDoNotFallBackForCoreCheckoutCopy() {
        val english = resourceFiles().getValue("values").strings
        val coreKeys = listOf(
            "cart",
            "add_to_cart",
            "checkout_empty_hint",
            "payment_profile",
            "transactions_empty_title",
            "receipt",
            "privacy_policy",
            "status_ready",
        )

        resourceFiles()
            .filterKeys { it != "values" && it != "values-nl" }
            .forEach { (qualifier, file) ->
                val localized = file.strings
                coreKeys.forEach { key ->
                    assertNotEquals("$qualifier should localize $key", english.getValue(key), localized.getValue(key))
                }
                ProductCatalog.products.forEach { product ->
                    assertNotEquals(
                        "$qualifier should localize product ${product.id}",
                        english.getValue(product.id.productNameKey()),
                        localized.getValue(product.id.productNameKey()),
                    )
                }
            }
    }

    @Test
    fun requestedLocalesDoNotUseEnglishFallbackForExpandedCopy() {
        val english = resourceFiles().getValue("values").strings
        val expandedKeys = listOf(
            "scan_profile_prompt",
            "status_qr_rejected",
            "terminal_api_request",
            "raw_return_uri",
        )

        resourceFiles()
            .filterKeys { it != "values" }
            .forEach { (qualifier, file) ->
                val localized = file.strings
                expandedKeys.forEach { key ->
                    assertNotEquals("$qualifier should not fall back to English for $key", english.getValue(key), localized.getValue(key))
                }
            }
    }

    @Test
    fun protectedProductAndProtocolTermsStayStable() {
        val english = resourceFiles().getValue("values").strings
        val exactKeys = listOf(
            "app_name",
            "screen_payments_app",
            "environment_test",
            "environment_live",
            "service_id",
            "merchant_reference",
            "sale_transaction",
        )

        resourceFiles().forEach { (qualifier, file) ->
            val strings = file.strings
            exactKeys.forEach { key ->
                assertEquals("$qualifier should keep protected term $key", english.getValue(key), strings.getValue(key))
            }
            assertTrue(strings.getValue("payments_app_api").contains("Payments App"))
            assertTrue(strings.getValue("decoded_sale_to_acquirer_data").contains("SaleToAcquirerData"))
            assertTrue(strings.getValue("adyen_app_is_state").contains("Installation ID"))
        }
    }

    private val File.strings: Map<String, String>
        get() {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            val nodes = document.getElementsByTagName("string")
            return (0 until nodes.length).associate { index ->
                val node = nodes.item(index)
                node.attributes.getNamedItem("name").nodeValue to node.textContent
            }
        }

    private val File.plurals: Map<String, Map<String, String>>
        get() {
            val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            val nodes = document.getElementsByTagName("plurals")
            return (0 until nodes.length).associate { pluralIndex ->
                val pluralNode = nodes.item(pluralIndex)
                val items = pluralNode.childNodes
                val quantities = (0 until items.length)
                    .map { items.item(it) }
                    .filter { it.nodeName == "item" }
                    .associate { item ->
                        item.attributes.getNamedItem("quantity").nodeValue to item.textContent
                    }
                pluralNode.attributes.getNamedItem("name").nodeValue to quantities
            }
        }

    private fun resourceFiles(): Map<String, File> {
        val resDir = listOf(
            File("app/src/main/res"),
            File("src/main/res"),
        ).first(File::exists)
        return resDir.listFiles()
            .orEmpty()
            .filter { it.name.startsWith("values") }
            .associate { it.name to File(it, "strings.xml") }
            .filterValues(File::exists)
    }

    private fun String.productNameKey(): String =
        "product_${replace("-", "_")}_name"

    private fun String.productDescriptionKey(): String =
        "product_${replace("-", "_")}_description"

    private fun String.categoryKey(): String =
        "category_${lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}"
}
