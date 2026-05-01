package com.xenophont.taptoplay.ui

import com.xenophont.taptoplay.catalog.ProductCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizationTest {
    @Test
    fun everyLanguageDefinesTheSameTextKeys() {
        val expectedKeys = stringsFor(AppLanguage.English).values.keys

        allLocalizedStringSets().forEach { strings ->
            assertEquals("Missing or extra keys for ${strings.language}", expectedKeys, strings.values.keys)
        }
    }

    @Test
    fun aboutMessageIsLocalized() {
        assertEquals(
            "Hecho por Javier de No, con ayuda de Codex y GPT-5.5, para el equipo 💚🚀",
            stringsFor(AppLanguage.Spanish)["about_message"],
        )
        assertEquals(
            "Made by Javier de No, with help from Codex and GPT-5.5, for the team 💚🚀",
            stringsFor(AppLanguage.English)["about_message"],
        )
        assertEquals(
            "Gemaakt door Javier de No, met hulp van Codex en GPT-5.5, voor het team 💚🚀",
            stringsFor(AppLanguage.Dutch)["about_message"],
        )
        assertTrue(stringsFor(AppLanguage.French)["about_message"].contains("Javier de No"))
        assertTrue(stringsFor(AppLanguage.Japanese)["about_message"].contains("Javier de No"))
        assertTrue(stringsFor(AppLanguage.Quenya)["about_message"].contains("Javier de No"))
    }

    @Test
    fun requestedLanguagesAreAvailable() {
        assertEquals("Nederlands", AppLanguage.Dutch.nativeName)
        assertEquals("Euskara", AppLanguage.Basque.nativeName)
        assertEquals("Easter egg", AppLanguage.Quenya.englishName)
        assertEquals(
            listOf("en", "es", "nl", "fr", "de", "it", "sv", "ja", "zh-Hans", "ko", "eu", "qya"),
            AppLanguage.entries.map { it.tag },
        )
    }

    @Test
    fun catalogCopyIsAvailableForAllLanguages() {
        allLocalizedStringSets().forEach { strings ->
            ProductCatalog.products.forEach { product ->
                assertFalse(strings.productName(product).isBlank())
                assertFalse(strings.productDescription(product).isBlank())
            }
            ProductCatalog.categories.forEach { category ->
                assertFalse(strings.categoryLabel(category).isBlank())
            }
        }
    }

    @Test
    fun newSupportScreensHaveLocalizedLabels() {
        allLocalizedStringSets().forEach { strings ->
            assertTrue(strings.screenLabel(AppScreen.Language).isNotBlank())
            assertTrue(strings.screenLabel(AppScreen.About).isNotBlank())
            assertTrue(strings["privacy_policy"].isNotBlank())
        }
    }

    @Test
    fun requestedLanguagesDoNotFallBackForCoreCheckoutCopy() {
        val english = stringsFor(AppLanguage.English)
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
        val localizedLanguages = listOf(
            AppLanguage.French,
            AppLanguage.German,
            AppLanguage.Italian,
            AppLanguage.Swedish,
            AppLanguage.Japanese,
            AppLanguage.Chinese,
            AppLanguage.Korean,
            AppLanguage.Basque,
            AppLanguage.Quenya,
        )

        localizedLanguages.forEach { language ->
            val localized = stringsFor(language)
            coreKeys.forEach { key ->
                assertNotEquals("$language should localize $key", english[key], localized[key])
            }
            ProductCatalog.products.forEach { product ->
                assertNotEquals(
                    "$language should localize product ${product.id}",
                    product.name,
                    localized.productName(product),
                )
            }
        }
    }

    @Test
    fun requestedLanguagesDoNotUseEnglishFallbackForExpandedCopy() {
        val english = stringsFor(AppLanguage.English)
        val expandedKeys = listOf(
            "scan_profile_prompt",
            "status_qr_rejected",
            "terminal_api_request",
            "raw_return_uri",
        )
        val localizedLanguages = AppLanguage.entries - AppLanguage.English - AppLanguage.Spanish

        localizedLanguages.forEach { language ->
            val localized = stringsFor(language)
            expandedKeys.forEach { key ->
                assertNotEquals("$language should not fall back to English for $key", english[key], localized[key])
            }
        }
    }

    @Test
    fun protectedProductAndProtocolTermsStayStable() {
        val exactKeys = listOf(
            "app_name",
            "screen_payments_app",
            "environment_test",
            "environment_live",
            "service_id",
            "merchant_reference",
            "sale_transaction",
        )

        allLocalizedStringSets().forEach { strings ->
            exactKeys.forEach { key ->
                assertEquals("${strings.language} should keep protected term $key", stringsFor(AppLanguage.English)[key], strings[key])
            }
            assertTrue(strings["payments_app_api"].contains("Payments App"))
            assertTrue(strings["decoded_sale_to_acquirer_data"].contains("SaleToAcquirerData"))
            assertTrue(strings["adyen_app_is_state"].contains("Installation ID"))
        }
    }
}
