package com.example.taptoplay.catalog

import androidx.compose.ui.graphics.Color

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val priceMinor: Long,
    val color: Color,
    val accentColor: Color,
)

object ProductCatalog {
    val products = listOf(
        Product(
            id = "linen-blazer",
            name = "Linen Atelier Blazer",
            category = "Outerwear",
            description = "Relaxed tailoring in stone-washed linen with corozo buttons.",
            priceMinor = 18900,
            color = Color(0xFFE6D8C9),
            accentColor = Color(0xFF38463F),
        ),
        Product(
            id = "silk-shirt",
            name = "Washed Silk Shirt",
            category = "Shirts",
            description = "Fluid ivory silk with a softly structured collar.",
            priceMinor = 12900,
            color = Color(0xFFF5EFE4),
            accentColor = Color(0xFF8D4E3A),
        ),
        Product(
            id = "tailored-trouser",
            name = "Tapered Wool Trouser",
            category = "Trousers",
            description = "Clean front trouser with a cropped taper and satin waistband.",
            priceMinor = 14900,
            color = Color(0xFF6D7470),
            accentColor = Color(0xFFC7A66A),
        ),
        Product(
            id = "cotton-dress",
            name = "Poplin Midi Dress",
            category = "Dresses",
            description = "Crisp cotton poplin, deep pockets, and an architectural waist.",
            priceMinor = 16900,
            color = Color(0xFFD9E2DF),
            accentColor = Color(0xFF1E4A5A),
        ),
        Product(
            id = "knit-cardigan",
            name = "Merino Rib Cardigan",
            category = "Knitwear",
            description = "Fine-gauge merino with horn buttons and a close rib finish.",
            priceMinor = 11900,
            color = Color(0xFFB9A795),
            accentColor = Color(0xFF513D34),
        ),
        Product(
            id = "leather-tote",
            name = "Soft Market Tote",
            category = "Accessories",
            description = "Unlined grained leather tote sized for shop floor essentials.",
            priceMinor = 21000,
            color = Color(0xFF9E7258),
            accentColor = Color(0xFF22211F),
        ),
    )

    val categories = listOf("All") + products.map { it.category }.distinct()
}
