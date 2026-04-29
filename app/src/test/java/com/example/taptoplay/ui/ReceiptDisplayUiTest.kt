package com.example.taptoplay.ui

import com.example.taptoplay.adyen.PaymentReceipt
import com.example.taptoplay.adyen.ReceiptLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptDisplayUiTest {
    @Test
    fun keepsAllAdyenReceiptFieldsReadable() {
        val receipt = PaymentReceipt(
            documentQualifier = "CustomerReceipt",
            requiredSignature = false,
            lines = listOf(
                line("header1=TapToPlay Boutique"),
                line("header2=Calle Mayor 1"),
                line("cardholderHeader=CARDHOLDER COPY"),
                line("txdate=2026-04-30"),
                line("txtime=15:14"),
                line("txtype=GOODS_SERVICES"),
                line("paymentMethod=visa"),
                line("pan=1234"),
                line("authCode=ABC123"),
                line("rrn=999888777"),
                line("stan=42"),
                line("aid=A0000000031010"),
                line("additionalEmvData=9F2608ABCDEF"),
                line("mid=Merchant-1"),
                line("tid=Terminal-7"),
                line("totalAmount=EUR 12.00"),
                line("approved=APPROVED"),
                line("QRCode=https://receipt.example/PSP123"),
                line("retain=Please retain for your records"),
                line("thanks=Thank you"),
            ),
        )

        val display = receipt.toReceiptDisplay(stringsFor(AppLanguage.English))
        val textLines = display.items.filterIsInstance<ReceiptDisplayItem.TextLine>()
        val rows = display.items.filterIsInstance<ReceiptDisplayItem.Row>()
        val notes = display.items.filterIsInstance<ReceiptDisplayItem.Note>()

        assertEquals("Customer receipt", display.title)
        assertTrue(textLines.any { it.text == "TapToPlay Boutique" })
        assertTrue(display.items.any { it == ReceiptDisplayItem.Total("Total", "EUR 12.00") })
        assertTrue(display.items.any { it == ReceiptDisplayItem.Status("APPROVED") })
        assertTrue(rows.any { it.label == "Authorisation" && it.value == "ABC123" })
        assertTrue(rows.any { it.label == "Additional EMV data" && it.value == "9F2608ABCDEF" })
        assertTrue(rows.any { it.label == "Merchant ID" && it.value == "Merchant-1" })
        assertTrue(rows.any { it.label == "Terminal" && it.value == "Terminal-7" })
        assertTrue(display.items.any { it == ReceiptDisplayItem.QrCode("https://receipt.example/PSP123") })
        assertTrue(notes.any { it.text == "Please retain for your records" })
        assertTrue(notes.any { it.text == "Thank you" })
    }

    @Test
    fun appendsSignatureLineWhenReceiptRequiresSignature() {
        val receipt = PaymentReceipt(
            documentQualifier = "CashierReceipt",
            requiredSignature = true,
            lines = listOf(line("header1=TapToPlay Boutique")),
        )

        val display = receipt.toReceiptDisplay(stringsFor(AppLanguage.English))

        assertEquals("Merchant receipt", display.title)
        assertTrue(display.items.any { it == ReceiptDisplayItem.SignatureLine("Signature") })
    }

    @Test
    fun combinesOutputTextFragmentsUntilEndOfLine() {
        val receipt = PaymentReceipt(
            documentQualifier = "CustomerReceipt",
            requiredSignature = false,
            lines = listOf(
                line(text = "header", endOfLine = false),
                line(text = "1=TapToPlay Boutique", endOfLine = true),
            ),
        )

        val display = receipt.toReceiptDisplay(stringsFor(AppLanguage.English))

        assertTrue(display.items.any { it == ReceiptDisplayItem.TextLine("TapToPlay Boutique", ReceiptTextAlignment.Center, true) })
    }

    @Test
    fun parsesAdyenStructuredReceiptTextFields() {
        val receipt = PaymentReceipt(
            documentQualifier = "CashierReceipt",
            requiredSignature = false,
            lines = listOf(
                line("key=header1"),
                line("key=header2"),
                line("name=COPIA P/ COMERCIANTE&key=merchantTitle"),
                line("key=filler"),
                line("name=Fecha&value=29/04/2026&key=txdate"),
                line("name=Hora&value=20:57:25&key=txtime"),
                line("name=Tarjeta&value=****7579&key=pan"),
                line("name=PAN seq.&value=01&key=panSeq"),
                line("name=Nombre pref.&value=MASTERCARD&key=preferred"),
            ),
        )

        val display = receipt.toReceiptDisplay(stringsFor(AppLanguage.Spanish))
        val rows = display.items.filterIsInstance<ReceiptDisplayItem.Row>()

        assertTrue(display.items.any { it == ReceiptDisplayItem.TextLine("COPIA P/ COMERCIANTE", ReceiptTextAlignment.Center, true) })
        assertTrue(rows.any { it.label == "Fecha" && it.value == "29/04/2026" })
        assertTrue(rows.any { it.label == "Hora" && it.value == "20:57:25" })
        assertTrue(rows.any { it.label == "Tarjeta" && it.value == "****7579" })
        assertTrue(rows.none { it.label.equals("KEY", ignoreCase = true) })
        assertTrue(rows.none { it.label.equals("NAME", ignoreCase = true) })
    }

    private fun line(
        text: String,
        alignment: String? = null,
        characterStyle: String? = null,
        endOfLine: Boolean = true,
    ): ReceiptLine = ReceiptLine(
        text = text,
        alignment = alignment,
        characterStyle = characterStyle,
        endOfLine = endOfLine,
    )
}
