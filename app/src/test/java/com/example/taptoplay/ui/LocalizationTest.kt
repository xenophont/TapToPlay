package com.example.taptoplay.ui

import com.example.taptoplay.catalog.ProductCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
