package com.xenophont.taptoplay.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import com.xenophont.taptoplay.catalog.Product
import com.xenophont.taptoplay.profiles.PaymentEnvironment
import java.util.Locale

internal enum class AppLanguage(val tag: String, val nativeName: String, val englishName: String) {
    English("en", "English", "English"),
    Spanish("es", "Español", "Spanish"),
    Dutch("nl", "Nederlands", "Dutch"),
    French("fr", "Français", "French"),
    German("de", "Deutsch", "German"),
    Italian("it", "Italiano", "Italian"),
    Swedish("sv", "Svenska", "Swedish"),
    Japanese("ja", "日本語", "Japanese"),
    Chinese("zh-Hans", "中文", "Chinese"),
    Korean("ko", "한국어", "Korean"),
    Basque("eu", "Euskara", "Basque"),
    Quenya("qya", "Quenya", "Easter egg");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag == tag } ?: English
    }
}

internal class AppLanguageStore(context: Context) {
    private val prefs = context.getSharedPreferences("tap_to_play_language", Context.MODE_PRIVATE)

    fun selected(): AppLanguage =
        AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, AppLanguage.English.tag))

    fun save(language: AppLanguage) {
        prefs.edit().putString(KEY_LANGUAGE, language.tag).apply()
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
    }
}

internal class TapToPlayStrings(
    val language: AppLanguage,
    private val stringValue: (Int) -> String,
    private val formattedStringValue: (Int, Array<out Any>) -> String,
    private val quantityStringValue: (Int, Int, Array<out Any>) -> String,
) {
    constructor(language: AppLanguage, resources: Resources) : this(
        language = language,
        stringValue = resources::getString,
        formattedStringValue = { resourceId, args -> resources.getString(resourceId, *args) },
        quantityStringValue = { resourceId, quantity, args -> resources.getQuantityString(resourceId, quantity, *args) },
    )

    constructor(language: AppLanguage, valuesByKey: Map<String, String>) : this(
        language = language,
        stringValue = { resourceId -> valuesByKey.getValue(keyForResourceId(resourceId)) },
        formattedStringValue = { resourceId, args ->
            String.format(
                Locale.forLanguageTag(language.tag),
                valuesByKey.getValue(keyForResourceId(resourceId)),
                *args,
            )
        },
        quantityStringValue = { resourceId, quantity, args ->
            val key = pluralKeyForResourceId(resourceId, quantity)
            String.format(Locale.forLanguageTag(language.tag), valuesByKey.getValue(key), *args)
        },
    )

    val locale: Locale = Locale.forLanguageTag(language.tag)
    val values: Map<String, String> by lazy {
        TapToPlayTextResources.mapValues { (_, resourceId) -> stringValue(resourceId) }
    }

    operator fun get(key: String): String =
        stringValue(resourceIdFor(key))

    fun format(key: String, vararg args: Any): String =
        formattedStringValue(resourceIdFor(key), args)

    fun quantity(key: String, count: Int): String =
        quantityStringValue(pluralResourceIdFor(key), count, arrayOf(count))

    fun screenLabel(screen: AppScreen): String = when (screen) {
        AppScreen.Catalog -> this["screen_catalog"]
        AppScreen.Checkout -> this["screen_checkout"]
        AppScreen.PaymentsApp -> this["screen_payments_app"]
        AppScreen.Transactions -> this["screen_transactions"]
        AppScreen.Diagnostics -> this["screen_diagnostics"]
        AppScreen.Language -> this["screen_language"]
        AppScreen.About -> this["screen_about"]
    }

    fun productName(product: Product): String =
        product.localizedString(TapToPlayProductNameResources, product.name)

    fun productDescription(product: Product): String =
        product.localizedString(TapToPlayProductDescriptionResources, product.description)

    fun categoryLabel(category: String): String =
        TapToPlayCategoryResources[category]?.let(stringValue) ?: category

    fun environmentLabel(environment: PaymentEnvironment): String = when (environment) {
        PaymentEnvironment.TEST -> this["environment_test"]
        PaymentEnvironment.LIVE -> this["environment_live"]
    }

    fun itemsReady(count: Int): String =
        quantity("cart_item_ready", count)

    fun itemCount(count: Int): String =
        quantity("item_count", count)

    fun jsonFieldCount(count: Int): String =
        quantity("json_field_count", count)

    fun savedPaymentAttempts(count: Int): String =
        quantity("saved_attempt", count)

    fun loadedPaymentInstances(count: Int): String =
        quantity("loaded_instance", count)

    fun fieldCount(count: Int): String =
        quantity("field_count", count)

    fun arrayItemCount(count: Int): String =
        quantity("array_item_count", count)

    fun saleToAcquirerDataSummary(name: String, count: Int): String =
        format("sale_to_acquirer_data_summary", name, jsonFieldCount(count))

    fun chargingProfile(profileName: String, environment: PaymentEnvironment): String =
        format("charging_profile", profileName, environmentLabel(environment))

    fun languageChanged(language: AppLanguage): String =
        format("status_language_changed", language.nativeName)

    fun secretMask(value: String): String = when {
        value.isBlank() -> this["not_set"]
        value.length <= 8 -> "****"
        else -> value.take(4) + "..." + value.takeLast(4)
    }

    fun passphraseMask(value: String): String =
        if (value.isBlank()) this["not_set"] else this["secret_set_hidden"]

    fun maskedIdentifier(value: String?): String = when {
        value.isNullOrBlank() -> this["not_set"]
        value.length <= 8 -> "****"
        else -> value.take(4) + "..." + value.takeLast(4)
    }

    private fun Product.localizedString(resourceIds: Map<String, Int>, fallback: String): String =
        resourceIds[id]?.let(stringValue) ?: fallback

    private fun resourceIdFor(key: String): Int =
        TapToPlayTextResources[key] ?: error("Missing TapToPlay string resource for key: $key")

    private fun pluralResourceIdFor(key: String): Int =
        TapToPlayPluralResources[key] ?: error("Missing TapToPlay plural resource for key: $key")
}

internal fun stringsFor(context: Context, language: AppLanguage): TapToPlayStrings =
    TapToPlayStrings(language, context.localizedResources(language))

internal fun textResourceKeys(): Set<String> =
    TapToPlayTextResources.keys

internal fun productNameResourceKeys(): Set<String> =
    TapToPlayProductNameResources.keys

internal fun productDescriptionResourceKeys(): Set<String> =
    TapToPlayProductDescriptionResources.keys

internal fun categoryResourceKeys(): Set<String> =
    TapToPlayCategoryResources.keys

internal fun pluralResourceKeys(): Set<String> =
    TapToPlayPluralResources.keys

private fun Context.localizedResources(language: AppLanguage): Resources {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(Locale.forLanguageTag(language.tag))
    return createConfigurationContext(configuration).resources
}

private val TapToPlayResourceKeysById: Map<Int, String> =
    TapToPlayTextResources.entries.associate { (key, resourceId) -> resourceId to key } +
        TapToPlayProductNameResources.entries.associate { (key, resourceId) -> resourceId to "product_${key.replace("-", "_")}_name" } +
        TapToPlayProductDescriptionResources.entries.associate { (key, resourceId) -> resourceId to "product_${key.replace("-", "_")}_description" } +
        TapToPlayCategoryResources.entries.associate { (key, resourceId) -> resourceId to "category_${key.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}" }

private fun keyForResourceId(resourceId: Int): String =
    TapToPlayResourceKeysById[resourceId] ?: error("Missing TapToPlay resource key for id: $resourceId")

private val TapToPlayPluralKeysById: Map<Int, String> =
    TapToPlayPluralResources.entries.associate { (key, resourceId) -> resourceId to key }

private fun pluralKeyForResourceId(resourceId: Int, quantity: Int): String {
    val key = TapToPlayPluralKeysById[resourceId] ?: error("Missing TapToPlay plural key for id: $resourceId")
    return if (quantity == 1) "${key}_one" else "${key}_many"
}
