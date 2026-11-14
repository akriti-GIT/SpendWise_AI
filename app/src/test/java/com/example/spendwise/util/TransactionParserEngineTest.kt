package com.example.spendwise.util

import com.example.spendwise.data.entity.TransactionEntity
import com.example.spendwise.model.ParsedTransaction
import com.example.spendwise.model.PaymentMethod
import com.example.spendwise.model.TransactionType
import org.junit.Assert.*
import org.junit.Test

class TransactionParserEngineTest {

    @Test
    fun testParseSingleUpiScreenshotGPay() {
        val ocrSample = """
            Google Pay
            Paid successfully to
            Campus Cafeteria
            ₹ 250.00
            Completed • 11 Oct 2026, 1:45 PM
            UPI Ref ID: 428716294012
        """.trimIndent()

        val parsed = TransactionParserEngine.parseOcrText(ocrSample)
        assertEquals(1, parsed.size)
        val tx = parsed.first()
        assertEquals(250.0, tx.amount, 0.001)
        assertTrue(tx.description.contains("Campus Cafeteria"))
        assertEquals("Food", tx.category)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("428716294012", tx.referenceNumber)
    }

    @Test
    fun testParsePhonePeReceivedIncome() {
        val ocrSample = """
            PhonePe
            Received from
            Amit Kumar
            ₹ 1,500.00
            Transaction ID: T26101112345678
            10 Oct 2026
        """.trimIndent()

        val parsed = TransactionParserEngine.parseOcrText(ocrSample)
        assertEquals(1, parsed.size)
        val tx = parsed.first()
        assertEquals(1500.0, tx.amount, 0.001)
        assertEquals(TransactionType.INCOME, tx.type)
        assertTrue(tx.description.contains("Amit Kumar"))
    }

    @Test
    fun testParseBankSmsDebit() {
        val smsSample = """
            Dear Customer, your A/C ending 4821 is debited for Rs 450.00 on 14-Oct-26 at DOMINOS PIZZA. Avail Bal: Rs 14,230. Ref: 428712398451
        """.trimIndent()

        val parsed = TransactionParserEngine.parseOcrText(smsSample)
        assertEquals(1, parsed.size)
        val tx = parsed.first()
        assertEquals(450.0, tx.amount, 0.001)
        assertEquals("Food", tx.category)
        assertTrue(tx.description.contains("DOMINOS", ignoreCase = true))
        assertEquals(TransactionType.EXPENSE, tx.type)
    }

    @Test
    fun testParseSwiggyOrderSummary() {
        val swiggySample = """
            Order Details
            Meghana Foods, Indiranagar
            1x Chicken Biryani ₹350
            Delivery Fee ₹40
            Taxes & Charges ₹18
            Total Bill: ₹ 408.00
            Paid via Google Pay
        """.trimIndent()

        val parsed = TransactionParserEngine.parseOcrText(swiggySample)
        assertEquals(1, parsed.size)
        val tx = parsed.first()
        assertEquals(408.0, tx.amount, 0.001)
        assertEquals("Food", tx.category)
        assertEquals(TransactionType.EXPENSE, tx.type)
    }

    @Test
    fun testParseBlinkitGroceries() {
        val blinkitSample = """
            Blinkit
            Order Delivered
            4 Items
            Amul Milk, Bread, Eggs
            Order Total: ₹ 285
            Paid via PhonePe
        """.trimIndent()

        val parsed = TransactionParserEngine.parseOcrText(blinkitSample)
        assertEquals(1, parsed.size)
        val tx = parsed.first()
        assertEquals(285.0, tx.amount, 0.001)
        assertEquals("Food", tx.category)
    }

    @Test
    fun testParseUberRideReceipt() {
        val uberSample = """
            Uber India
            Trip details
            Ride completed on 14 Oct 2026
            Trip Total: ₹ 189.50
            Payment method: Paytm UPI
        """.trimIndent()

        val parsed = TransactionParserEngine.parseOcrText(uberSample)
        assertEquals(1, parsed.size)
        val tx = parsed.first()
        assertEquals(189.50, tx.amount, 0.001)
        assertEquals("Transport", tx.category)
        assertTrue(tx.description.contains("Uber", ignoreCase = true))
    }

    @Test
    fun testParseRestaurantPaperBill() {
        val billSample = """
            CAFE COFFEE DAY
            Tax Invoice
            Cappuccino 180.00
            Croissant 120.00
            Sub Total 300.00
            CGST 2.5% 7.50
            SGST 2.5% 7.50
            GRAND TOTAL: ₹ 315.00
            Thank You Visit Again
        """.trimIndent()

        val parsed = TransactionParserEngine.parseOcrText(billSample)
        assertEquals(1, parsed.size)
        val tx = parsed.first()
        assertEquals(315.0, tx.amount, 0.001)
        assertEquals("Food", tx.category)
        assertTrue(tx.description.contains("CAFE COFFEE DAY", ignoreCase = true))
    }

    @Test
    fun testParsePaytmIntegerAmountWithoutDecimals() {
        val paytmSample = """
            Paytm
            Paid to
            Ramesh Kirana Store
            ₹ 150
            Successful
            12 Oct 2026
        """.trimIndent()

        val parsed = TransactionParserEngine.parseOcrText(paytmSample)
        assertEquals(1, parsed.size)
        val tx = parsed.first()
        assertEquals(150.0, tx.amount, 0.001)
        assertTrue(tx.description.contains("Ramesh"))
    }

    @Test
    fun testParseMultiRowStatement() {
        val ocrStatement = """
            Account Statement
            08 Oct 2026 Uber India ₹ 340.00 DR
            09 Oct 2026 Swiggy Orders ₹ 520.50 DR
            10 Oct 2026 Salary Credit ₹ 10000.00 CR
        """.trimIndent()

        val parsed = TransactionParserEngine.parseOcrText(ocrStatement)
        assertTrue(parsed.size >= 2)
        val uber = parsed.find { it.amount == 340.0 }
        assertNotNull(uber)
        assertEquals("Transport", uber?.category)
        assertEquals(TransactionType.EXPENSE, uber?.type)

        val swiggy = parsed.find { it.amount == 520.50 }
        assertNotNull(swiggy)
        assertEquals("Food", swiggy?.category)
        assertEquals(TransactionType.EXPENSE, swiggy?.type)
    }

    @Test
    fun testCategorySuggestion() {
        assertEquals("Food", TransactionParserEngine.suggestCategory("Starbucks Coffee"))
        assertEquals("Transport", TransactionParserEngine.suggestCategory("Metro Recharge Card"))
        assertEquals("Education", TransactionParserEngine.suggestCategory("University Bookstore"))
        assertEquals("Shopping", TransactionParserEngine.suggestCategory("Amazon Pay"))
        assertEquals("Entertainment", TransactionParserEngine.suggestCategory("Netflix Subscription"))
        assertEquals("Health", TransactionParserEngine.suggestCategory("Apollo Pharmacy"))
        assertEquals("Bills", TransactionParserEngine.suggestCategory("Airtel Postpaid Bill"))
        assertEquals("Other", TransactionParserEngine.suggestCategory("Miscellaneous Store"))
    }

    @Test
    fun testDuplicateDetection() {
        val now = System.currentTimeMillis()
        val existing = listOf(
            TransactionEntity(
                id = 1,
                type = TransactionType.EXPENSE,
                amount = 250.0,
                category = "Food",
                description = "Campus Cafeteria",
                date = now,
                paymentMethod = PaymentMethod.UPI
            )
        )

        val duplicateCandidate = ParsedTransaction(
            amount = 250.0,
            description = "Campus Cafeteria",
            category = "Food",
            date = now + 3600000L,
            type = TransactionType.EXPENSE
        )

        val differentCandidate = ParsedTransaction(
            amount = 500.0,
            description = "Bookstore",
            category = "Education",
            date = now,
            type = TransactionType.EXPENSE
        )

        assertTrue(TransactionParserEngine.isDuplicate(duplicateCandidate, existing))
        assertFalse(TransactionParserEngine.isDuplicate(differentCandidate, existing))
    }

    @Test
    fun testParseBusFareTicketWithRoute26() {
        val busSample = """
            DTC Bus Ticket
            Route No: 26
            Fare: Rs. 6
            Date: 28-Oct-26 14:26
            Ticket ID: 948210
        """.trimIndent()
        val parsed = TransactionParserEngine.parseOcrText(busSample)
        assertEquals(1, parsed.size)
        assertEquals(6.0, parsed[0].amount, 0.001)
        assertEquals("Transport", parsed[0].category)
    }

    @Test
    fun testParseBusFarePostfixRs() {
        val busSample = """
            City Bus Transport
            Bus Fare
            6 Rs
            28 Oct 2026
        """.trimIndent()
        val parsed = TransactionParserEngine.parseOcrText(busSample)
        assertEquals(1, parsed.size)
        assertEquals(6.0, parsed[0].amount, 0.001)
        assertEquals("Transport", parsed[0].category)
    }

    @Test
    fun testParseBusFareChaloTicket() {
        val chaloSample = """
            Chalo Pay
            Bus Ticket
            Route 26
            Total Fare
            ₹ 6.00
            26-10-2026 08:26 AM
        """.trimIndent()
        val parsed = TransactionParserEngine.parseOcrText(chaloSample)
        assertEquals(1, parsed.size)
        assertEquals(6.0, parsed[0].amount, 0.001)
        assertEquals("Transport", parsed[0].category)
    }

    @Test
    fun testParseUpiBusFarePaidOn26th() {
        val upiSample = """
            Google Pay
            Paid to
            DTC Bus Fare
            ₹6
            Paid on 26 Oct 2026
            UPI Ref: 428716294012
        """.trimIndent()
        val parsed = TransactionParserEngine.parseOcrText(upiSample)
        assertEquals(1, parsed.size)
        assertEquals(6.0, parsed[0].amount, 0.001)
        assertEquals("Transport", parsed[0].category)
    }

    @Test
    fun testParseUploadedImage1BmtcBusFareRs6() {
        val bmtcSample = """
            To BMTC
            ₹6
            Payment for 26j29E1117866mdR
            Completed
            29 Oct 2026, 9:41am
            State Bank of India
            1961
            Payment of ₹6 completed
            Do you need help with this payment?
            Having issues? Share Split
        """.trimIndent()
        val parsed = TransactionParserEngine.parseOcrText(bmtcSample)
        assertEquals(1, parsed.size)
        val tx = parsed[0]
        assertEquals(6.0, tx.amount, 0.001)
        assertEquals("BMTC", tx.description)
        assertEquals("Transport", tx.category)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("26j29E1117866mdR", tx.referenceNumber)
    }

    @Test
    fun testParseUploadedImage2PreranaRs100() {
        val preranaSample = """
            To PRERANA UTTAM CHAVAN
            ₹100
            Pay again
            Completed
            4 Oct 2026, 10:36pm
            State Bank of India
            1961
            UPI transaction ID
            627706626251
            To: PRERANA UTTAM CHAVAN
            ......2685@slc
        """.trimIndent()
        val parsed = TransactionParserEngine.parseOcrText(preranaSample)
        assertEquals(1, parsed.size)
        val tx = parsed[0]
        assertEquals(100.0, tx.amount, 0.001)
        assertEquals("PRERANA UTTAM CHAVAN", tx.description)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals("627706626251", tx.referenceNumber)
    }
}
