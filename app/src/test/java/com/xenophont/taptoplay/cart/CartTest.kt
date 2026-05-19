package com.xenophont.taptoplay.cart

import androidx.compose.ui.graphics.Color
import com.xenophont.taptoplay.catalog.Product
import org.junit.Assert.assertEquals
import org.junit.Test

class CartTest {
    private val shirt = Product(
        id = "shirt",
        name = "Shirt",
        category = "Shirts",
        description = "Test shirt",
        priceMinor = 12900,
        color = Color.White,
        accentColor = Color.Black,
    )

    @Test
    fun totalsTrackQuantityChanges() {
        val cart = Cart()

        cart.add(shirt)
        cart.add(shirt)

        assertEquals(2, cart.itemCount())
        assertEquals(25800, cart.totalMinor())

        cart.removeOne(shirt.id)

        assertEquals(1, cart.itemCount())
        assertEquals(12900, cart.totalMinor())
    }

    @Test
    fun removingLastItemClearsLine() {
        val cart = Cart()

        cart.add(shirt)
        cart.removeOne(shirt.id)

        assertEquals(emptyList<CartLine>(), cart.lines())
        assertEquals(0, cart.totalMinor())
    }

    @Test
    fun customAmountProductsCanUseDistinctLineIds() {
        val cart = Cart()
        val customOneEuro = shirt.copy(id = "custom-test-garment-100", priceMinor = 100)
        val customTwoEuros = shirt.copy(id = "custom-test-garment-200", priceMinor = 200)

        cart.add(customOneEuro)
        cart.add(customTwoEuros)
        cart.add(customTwoEuros)

        assertEquals(3, cart.itemCount())
        assertEquals(500, cart.totalMinor())
        assertEquals(listOf(100L, 400L), cart.lines().map { it.lineTotalMinor })
    }
}
