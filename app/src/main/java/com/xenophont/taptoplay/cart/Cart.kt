package com.xenophont.taptoplay.cart

import com.xenophont.taptoplay.catalog.Product

data class CartLine(
    val product: Product,
    val quantity: Int,
) {
    val lineTotalMinor: Long = product.priceMinor * quantity
}

class Cart {
    private val quantities = linkedMapOf<String, Pair<Product, Int>>()

    fun add(product: Product) {
        val current = quantities[product.id]?.second ?: 0
        quantities[product.id] = product to current + 1
    }

    fun removeOne(productId: String) {
        val current = quantities[productId] ?: return
        if (current.second <= 1) {
            quantities.remove(productId)
        } else {
            quantities[productId] = current.first to current.second - 1
        }
    }

    fun clear() = quantities.clear()

    fun lines(): List<CartLine> = quantities.values.map { CartLine(it.first, it.second) }

    fun itemCount(): Int = quantities.values.sumOf { it.second }

    fun totalMinor(): Long = lines().sumOf { it.lineTotalMinor }
}
