package com.example.spendwise.util

import com.example.spendwise.data.entity.TransactionEntity
import com.example.spendwise.model.ParsedTransaction
import com.example.spendwise.model.PaymentMethod
import com.example.spendwise.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

object TransactionParserEngine {

    val CATEGORIES = listOf(
        "Food",
        "Transport",
        "Education",
        "Shopping",
        "Entertainment",
        "Health",
        "Bills",
        "Other"
    )

    private val CATEGORY_KEYWORD_MAP = mapOf(
        "Food" to listOf(
            "swiggy", "zomato", "eatclub", "domino", "dominos", "pizza", "burger", "kfc", "mcdonald", "mcdonalds",
            "subway", "starbucks", "cafe", "ccd", "coffee", "barista", "chai", "chaayos", "tea", "bakery", "bake",
            "pastry", "cake", "sweets", "mithai", "haldiram", "bikanervala", "restaurant", "restro", "bistro",
            "eatery", "mess", "canteen", "dhaba", "biryani", "kitchen", "bar", "pub", "dining", "diner", "food",
            "meal", "lunch", "dinner", "breakfast", "snacks", "snack", "fastfood", "tiffin", "roll", "shawarma",
            "chaat", "momo", "waffle", "icecream", "cream", "juice", "shake", "grill", "bbq", "grocery", "groceries",
            "zepto", "blinkit", "instamart", "bigbasket", "bbnow", "bbdaily", "dmart", "d-mart", "spencer",
            "nature basket", "supermarket", "hypermarket", "provisions", "kirana", "milk", "dairy", "paneer",
            "vegetables", "fruits", "sabzi", "mandi", "meat", "fish", "chicken", "mutton", "egg", "eggs",
            "licious", "freshtohome", "freshmenu", "faasos", "behrouz", "oven story", "box8"
        ),
        "Transport" to listOf(
            "uber", "ola", "rapido", "namma yatri", "blusmart", "taxi", "cab", "auto", "rickshaw", "ride",
            "driver", "fare", "metro", "bmrcl", "dmrc", "mmrda", "smart card", "transit", "railway", "rail",
            "irctc", "train", "ticket", "bus", "redbus", "abhibus", "chalo", "bmtc", "ksrtc", "msrtc", "dtc", "best",
            "transport", "flight", "airline", "indigo", "air india", "spicejet", "akasa", "vistara", "airasia",
            "airport", "petrol", "fuel", "diesel", "cng", "indianoil", "iocl", "hpcl", "hp petrol", "bpcl",
            "bharat petroleum", "shell", "nayara", "gas station", "toll", "fastag", "netc", "parking", "valet",
            "garage", "mechanic", "car wash", "puncture", "service center"
        ),
        "Education" to listOf(
            "college", "university", "school", "campus", "library", "tuition", "coaching", "institute", "class",
            "classes", "allen", "aakash", "fiitjee", "unacademy", "physics wallah", "pw", "vedantu", "udemy",
            "coursera", "edx", "skillshare", "linkedin learning", "leetcode", "geeksforgeeks", "books", "book",
            "bookstore", "stationery", "paper", "pen", "pencil", "notebook", "xerox", "print", "photocopy",
            "binding", "scan", "exam", "test", "fees", "fee", "admission", "semester", "hall ticket", "course",
            "training", "degree", "diploma", "academy"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "meesho", "ajio", "nykaa", "tata cliq", "reliance trends", "trends",
            "pantaloons", "max fashion", "lifestyle", "westside", "shopper stop", "shoppers stop", "zudio",
            "zara", "h&m", "uniqlo", "marks & spencer", "decathlon", "lenskart", "titan", "fastrack", "casio",
            "boat", "noise", "fireboltt", "apple", "samsung", "electronics", "croma", "vijay sales",
            "reliance digital", "poorvika", "sangeetha", "mall", "bazaar", "retail", "outlet", "boutique",
            "footwear", "shoes", "sneakers", "slippers", "sandals", "clothing", "clothes", "apparel", "dress",
            "shirt", "jeans", "cosmetic", "makeup", "perfume", "jewelry", "gift", "shopping"
        ),
        "Entertainment" to listOf(
            "netflix", "spotify", "prime video", "hotstar", "disney", "youtube", "premium", "sonyliv", "zee5",
            "jiosaavn", "gaana", "wynk", "apple music", "bookmyshow", "ticketnew", "pvr", "inox", "cinepolis",
            "miraj", "movie", "cinema", "theatre", "show", "play", "concert", "event", "standup", "amusement",
            "gaming", "steam", "playstation", "psn", "xbox", "nintendo", "riot", "epic games", "bgmi", "pubg",
            "freefire", "valorant", "discord", "membership", "club", "party", "fun", "entertainment"
        ),
        "Health" to listOf(
            "apollo", "medplus", "1mg", "tata 1mg", "pharmeasy", "netmeds", "wellness forever", "pharmacy",
            "chemist", "medical", "medicine", "medicines", "drugs", "tablet", "syrup", "hospital", "clinic",
            "doctor", "dr.", "dental", "dentist", "eye", "optical", "optician", "specs", "glasses", "lens",
            "diagnostic", "lab", "pathlab", "blood test", "pathology", "srl", "dr lal", "lal pathlabs",
            "thyroid", "xray", "scan", "ultrasound", "consultation", "checkup", "therapy", "physiotherapy",
            "health", "wellness", "gym", "cult", "cult.fit", "fitness", "workout", "trainer", "supplement",
            "protein", "whey", "creatine", "yoga"
        ),
        "Bills" to listOf(
            "recharge", "airtel", "jio", "vi", "vodafone", "idea", "bsnl", "mtnl", "mobile", "phone bill",
            "postpaid", "prepaid", "data", "broadband", "wifi", "act fibernet", "hathway", "fiber", "electricity",
            "bescom", "tneb", "msedcl", "bses", "cesc", "tata power", "adani power", "upcl", "power", "bijli",
            "water", "jal", "water bill", "gas", "lpg", "cylinder", "indane", "bharat gas", "hp gas", "igl",
            "mgl", "piped gas", "dth", "tata play", "dish tv", "airtel dth", "sun direct", "maintenance",
            "society", "rwa", "flat maintenance", "rent", "room rent", "pg rent", "pg", "deposit", "emi",
            "loan", "credit card", "card payment", "insurance", "lic", "premium", "tax", "property tax", "bill"
        )
    )

    private val DATE_PATTERNS = listOf(
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("d MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH),
        SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yy", Locale.ENGLISH)
    )

    // Regex to match currency amounts (both prefix and postfix):
    // Prefix: ₹ 1,420.00, Rs. 500, Rs 6, INR 250, $ 45.00
    // Postfix: 6 Rs, 6 RS, 6.00 Rs, 6/-, 6 /-, 250 INR, 6₹
    private val CURRENCY_AMOUNT_REGEX = Pattern.compile(
        """(?:(?:₹|Rs\.?|INR|\$|€|£)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)|([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:₹|Rs\.?|INR|/[-=])(?!\w))""",
        Pattern.CASE_INSENSITIVE
    )

    // Common OCR misreadings of ₹ (e.g. "? 250", "= 500", "/ 150", "f 450")
    private val OCR_ARTIFACT_AMOUNT_REGEX = Pattern.compile(
        """(?:[?*=\\/~fFrR]\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?))""",
        Pattern.CASE_INSENSITIVE
    )

    // Labeled amounts (e.g. "Total: 450", "Paid: 1,200", "Amount 350.00", "Payment of ₹6 completed", "Debited 80", "Fare 140", "Fare: Rs. 6")
    private val LABELED_AMOUNT_REGEX = Pattern.compile(
        """(?:total|grand total|total amount|bill total|order total|net payable|net amount|amount paid|to pay|payment of|paid|debited|spent|fare|bus fare|ticket fare|total fare|amount)[\s:=–-]*[₹RsINR$€£\s]*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)(?![A-Za-z0-9])""",
        Pattern.CASE_INSENSITIVE
    )

    // Standalone decimal numbers (e.g. 450.00, 6.00, 1,250.50)
    private val STANDALONE_DECIMAL_REGEX = Pattern.compile(
        """\b([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{2}))\b"""
    )

    // General standalone integers (e.g. 6, 150, 1200) - supports single-digit fares (e.g. 6 Rs) with context filtering
    private val STANDALONE_INTEGER_REGEX = Pattern.compile(
        """\b([0-9]{1,6})\b"""
    )

    /**
     * Master OCR Parsing Function:
     * Handles ALL types of screenshots:
     * - UPI Apps (GPay, PhonePe, Paytm, CRED, BHIM, Amazon Pay, Navi, Super.money, etc.)
     * - Bank SMS & Debit Alerts (HDFC, SBI, ICICI, Axis, Kotak, etc.)
     * - Food Delivery & Dining (Swiggy, Zomato, Domino's, etc.)
     * - Quick Commerce & Groceries (Blinkit, Zepto, Instamart, BigBasket, DMart)
     * - E-Commerce & Retail (Amazon, Flipkart, Myntra, etc.)
     * - Rides & Travel (Uber, Ola, Rapido, Metro, IRCTC)
     * - Printed Paper Receipts & Invoices (Supermarket, Cafe, Restaurant POS slips)
     * - Multi-row Bank Statements / Passbook Histories
     */
    fun parseOcrText(rawText: String): List<ParsedTransaction> {
        val normalized = normalizeOcrText(rawText)
        val lines = normalized.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val lower = normalized.lowercase(Locale.ROOT)
        val hasSingleBillTotal = lower.contains("grand total") ||
                lower.contains("total bill") ||
                lower.contains("bill total") ||
                lower.contains("order total") ||
                lower.contains("subtotal") ||
                lower.contains("sub total") ||
                lower.contains("tax invoice") ||
                lower.contains("to pay") ||
                lower.contains("net payable") ||
                lower.contains("amount paid") ||
                lower.contains("trip total")

        val isSinglePaymentReceipt = hasSingleBillTotal ||
                lower.contains("completed") ||
                lower.contains("payment of") ||
                lower.contains("paid successfully") ||
                lower.contains("paid to") ||
                lower.contains("pay again") ||
                (lower.contains("to ") && (lower.contains("upi") || lower.contains("gpay") || lower.contains("phonepe") || lower.contains("state bank")))

        if (!isSinglePaymentReceipt) {
            // Strategy 1: Check for Multi-row Statements / Passbook / Transaction History
            val statementItems = parseStatementRows(lines, normalized)
            if (statementItems.size >= 2) {
                return deduplicateParsed(statementItems)
            }
        }

        // Strategy 2: Check for Bank SMS and Push Notification alerts
        val smsTransaction = parseBankSmsNotification(lines, normalized)
        if (smsTransaction != null) {
            return listOf(smsTransaction)
        }

        // Strategy 3: Check for Food, Grocery & E-Commerce Order Summaries (Swiggy, Zomato, Blinkit, Amazon, etc.)
        val orderTransaction = parseOrderSummaryReceipt(lines, normalized)
        if (orderTransaction != null) {
            return listOf(orderTransaction)
        }

        // Strategy 4: Check for Physical Store / Restaurant / Paper Invoices
        val paperReceipt = parsePaperReceipt(lines, normalized)
        if (paperReceipt != null) {
            return listOf(paperReceipt)
        }

        // Strategy 5: Check for Single UPI Payment Receipts (Google Pay, PhonePe, Paytm, CRED, BHIM, etc.)
        val upiReceipt = parseSingleUpiReceipt(lines, normalized)
        if (upiReceipt != null) {
            return listOf(upiReceipt)
        }

        // Strategy 6: If statement rows found, return them
        val statementFallback = parseStatementRows(lines, normalized)
        if (statementFallback.isNotEmpty()) {
            return deduplicateParsed(statementFallback)
        }

        // Strategy 7: Universal Semantic Fallback
        val fallback = parseUniversalFallback(lines, normalized)
        return if (fallback != null) listOf(fallback) else emptyList()
    }

    private fun normalizeOcrText(raw: String): String {
        return raw.replace("\u20B9", "₹")
            .replace("`", "")
            .replace("~", "")
            .replace("—", "-")
            .replace("–", "-")
            .replace(Regex("""(?m)^2\s+([0-9]+(?:\.[0-9]{1,2})?)$"""), "₹ $1")
            .replace(Regex("""(?m)^[?*=\\/~fF]\s*([0-9]+(?:\.[0-9]{1,2})?)$"""), "₹ $1")
    }

    /**
     * Parses Bank SMS and Transaction Alert Notifications:
     * Examples:
     * - "A/c *1234 debited by Rs 450.00 on 14-Oct-26 at SWIGGY"
     * - "Txn of INR 1,299.00 on HDFC Card ending 9876 at AMAZON"
     * - "Paid INR 350.00 to VPA zomato@icici from A/c XX5678"
     * - "Sent Rs 250 to Ramesh Kumar UPI Ref 428712398451"
     */
    private fun parseBankSmsNotification(lines: List<String>, fullText: String): ParsedTransaction? {
        val lower = fullText.lowercase(Locale.ROOT)

        val isSmsAlert = (lower.contains("debited") || lower.contains("spent") || lower.contains("txn of") ||
                lower.contains("transaction of") || lower.contains("credited to") || lower.contains("credited with") ||
                lower.contains("transferred to") || lower.contains("paid inr") || lower.contains("paid rs") ||
                lower.contains("sent rs") || lower.contains("sent inr")) &&
                (lower.contains("a/c") || lower.contains("acct") || lower.contains("card") || lower.contains("vpa") ||
                        lower.contains("bank") || lower.contains("upi") || lower.contains("bal"))

        if (!isSmsAlert) return null

        val isIncome = lower.contains("credited") || lower.contains("refund") || lower.contains("received")
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val amount = extractAmountFromSms(lines, fullText) ?: return null
        val merchant = extractMerchantFromSms(fullText).ifBlank {
            if (type == TransactionType.INCOME) "Bank Credit" else "Bank Debit"
        }
        val date = extractDate(lines) ?: System.currentTimeMillis()
        val refId = extractReferenceId(lines)
        val paymentMethod = if (lower.contains("card")) PaymentMethod.CARD else PaymentMethod.UPI

        return ParsedTransaction(
            amount = amount,
            description = merchant,
            category = if (type == TransactionType.INCOME) "Other" else resolveCategory(merchant, fullText),
            date = date,
            type = type,
            paymentMethod = paymentMethod,
            referenceNumber = refId,
            isSelected = true
        )
    }

    private fun extractAmountFromSms(lines: List<String>, fullText: String): Double? {
        // Regex specifically matching SMS amount patterns: "debited by/for/of Rs 450", "spent INR 1200", "txn of Rs 350"
        val smsAmountRegex = Pattern.compile(
            """(?:debited\s*(?:by|for|of|with)?|spent|txn\s*(?:of)?|paid|transferred|credited\s*(?:by|with)?|for)\s*(?:INR|Rs\.?|₹)?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = smsAmountRegex.matcher(fullText)
        if (matcher.find()) {
            val amtStr = matcher.group(1)?.replace(",", "")
            amtStr?.toDoubleOrNull()?.let { if (it in 1.0..500000.0) return it }
        }

        // Fallback to currency regex in the lines
        return extractProminentAmount(lines)
    }

    private fun extractMerchantFromSms(fullText: String): String {
        // 1. Check "at <Merchant>"
        val atMatcher = Pattern.compile("""\bat\s+([A-Za-z0-9\s&'.-]{2,30}?)(?:\s+on|\s+ref|\s+avail|\s+bal|\.|\n|$)""", Pattern.CASE_INSENSITIVE).matcher(fullText)
        if (atMatcher.find()) {
            val merchant = atMatcher.group(1)?.trim()
            if (!merchant.isNullOrBlank() && !merchant.contains("₹") && !merchant.contains("Rs")) {
                return cleanMerchantName(merchant)
            }
        }

        // 2. Check "to <Merchant>" or "to VPA <VPA>"
        val toMatcher = Pattern.compile("""\b(?:to\s+vpa|to)\s+([A-Za-z0-9@\s&'.-]{2,30}?)(?:\s+on|\s+ref|\s+avail|\s+from|\.|\n|$)""", Pattern.CASE_INSENSITIVE).matcher(fullText)
        if (toMatcher.find()) {
            val to = toMatcher.group(1)?.trim()
            if (!to.isNullOrBlank() && !to.contains("₹") && !to.contains("Rs")) {
                return cleanMerchantName(to)
            }
        }

        // 3. Check for known brand keywords anywhere in text
        val knownBrand = findKnownBrandInText(fullText)
        if (knownBrand.isNotBlank()) return knownBrand

        return ""
    }

    /**
     * Parses Food Delivery, Grocery & E-Commerce Order Summaries:
     * Examples:
     * - Swiggy: "Order details / Meghana Foods / Total Bill: ₹408"
     * - Zomato: "Order summary / Empire Restaurant / To Pay: ₹350"
     * - Blinkit / Zepto: "Order Total: ₹275"
     * - Uber / Ola: "Trip Total: ₹214.50"
     * - Amazon / Flipkart: "Order Total: ₹1,499.00"
     */
    private fun parseOrderSummaryReceipt(lines: List<String>, fullText: String): ParsedTransaction? {
        val lower = fullText.lowercase(Locale.ROOT)

        val isOrderReceipt = lower.contains("swiggy") || lower.contains("zomato") || lower.contains("blinkit") ||
                lower.contains("zepto") || lower.contains("instamart") || lower.contains("uber") ||
                lower.contains("ola") || lower.contains("rapido") || lower.contains("amazon") ||
                lower.contains("flipkart") || lower.contains("myntra") || lower.contains("order total") ||
                lower.contains("bill total") || lower.contains("total bill") || lower.contains("to pay") ||
                lower.contains("trip total") || lower.contains("fare breakup") || lower.contains("order summary") ||
                lower.contains("bill details") || lower.contains("items total")

        if (!isOrderReceipt) return null

        // In order receipts, find the FINAL TOTAL (not individual line item prices or delivery fees)
        val amount = extractOrderTotalAmount(lines) ?: extractProminentAmount(lines) ?: return null

        // Extract Merchant: e.g. Restaurant name or App name
        val merchant = extractOrderMerchant(lines, fullText)
        val date = extractDate(lines) ?: System.currentTimeMillis()
        val category = resolveCategory(merchant, fullText)

        return ParsedTransaction(
            amount = amount,
            description = merchant.ifBlank { "Online Order" },
            category = category,
            date = date,
            type = TransactionType.EXPENSE,
            paymentMethod = if (lower.contains("cash")) PaymentMethod.CASH else PaymentMethod.UPI,
            isSelected = true
        )
    }

    private fun extractOrderTotalAmount(lines: List<String>): Double? {
        val totalKeywords = listOf(
            "bill total", "total bill", "order total", "grand total", "to pay", "total payable",
            "net payable", "total amount", "amount paid", "trip total", "total fare", "bus fare",
            "ticket fare", "final total", "net amount", "fare", "total"
        )

        for (kw in totalKeywords) {
            for (i in lines.indices) {
                val line = lines[i]
                val lower = line.lowercase(Locale.ROOT)
                if (lower.contains(kw)) {
                    val candidate = extractAmountFromLineOrValue(line)
                    if (candidate != null && candidate in 1.0..500000.0 && isValidAmount(candidate, line)) {
                        return candidate
                    }
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1].trim()
                        val nextCandidate = extractAmountFromLineOrValue(nextLine) ?: nextLine.toDoubleOrNull()
                        if (nextCandidate != null && nextCandidate in 1.0..500000.0 && isValidAmount(nextCandidate, nextLine)) {
                            return nextCandidate
                        }
                    }
                }
            }
        }
        return null
    }

    private fun extractOrderMerchant(lines: List<String>, fullText: String): String {
        val lower = fullText.lowercase(Locale.ROOT)

        // 1. Uber / Ola / Rapido
        if (lower.contains("uber")) return "Uber Ride"
        if (lower.contains("ola")) return "Ola Ride"
        if (lower.contains("rapido")) return "Rapido Ride"

        // 2. Quick Commerce & Groceries
        if (lower.contains("blinkit")) return "Blinkit Groceries"
        if (lower.contains("zepto")) return "Zepto Groceries"
        if (lower.contains("instamart")) return "Instamart Groceries"
        if (lower.contains("bigbasket")) return "BigBasket Groceries"

        // 3. Shopping
        if (lower.contains("amazon")) return "Amazon Purchase"
        if (lower.contains("flipkart")) return "Flipkart Purchase"
        if (lower.contains("myntra")) return "Myntra Shopping"

        // 4. Restaurant / Store name from top lines (Swiggy, Zomato, or generic order receipt)
        val skipKeywords = listOf(
            "order", "bill", "receipt", "invoice", "summary", "details", "total",
            "payment", "paid", "delivery", "delivered to", "items", "taxes", "view",
            "google pay", "phonepe", "paytm", "cred", "upi"
        )
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i].trim()
            val l = line.lowercase(Locale.ROOT)
            if (l.contains("order from") || l.contains("delivered from") || l.contains("restaurant") || l.contains("store")) {
                val cleaned = cleanMerchantName(line.replace(Regex("(?i)order from|delivered from|restaurant|store"), ""))
                if (cleaned.length in 2..40) return cleaned
                if (i + 1 < lines.size && lines[i + 1].length in 2..40) {
                    return cleanMerchantName(lines[i + 1])
                }
            }
        }
        for (i in 0 until minOf(5, lines.size)) {
            val line = lines[i].trim()
            val l = line.lowercase(Locale.ROOT)
            val hasSkipWord = skipKeywords.any { l.contains(it) }
            if (line.length in 3..40 && !line.contains("₹") && !line.contains(":") && !hasSkipWord) {
                val cleaned = cleanMerchantName(line)
                if (cleaned.isNotBlank()) return cleaned
            }
        }

        if (lower.contains("swiggy")) return "Swiggy Order"
        if (lower.contains("zomato")) return "Zomato Order"

        // 5. General check for brand name
        val brand = findKnownBrandInText(fullText)
        if (brand.isNotBlank()) return brand

        return ""
    }

    /**
     * Parses Single UPI Payment Screens:
     * Works for Google Pay, PhonePe, Paytm, CRED, BHIM, Amazon Pay, Navi, Super.money, etc.
     */
    private fun parseSingleUpiReceipt(lines: List<String>, fullText: String): ParsedTransaction? {
        val lowerFull = fullText.lowercase(Locale.ROOT)

        val isUpiIndicator = lowerFull.contains("paid") ||
                lowerFull.contains("payment") ||
                lowerFull.contains("transfer") ||
                lowerFull.contains("sent") ||
                lowerFull.contains("received") ||
                lowerFull.contains("completed") ||
                lowerFull.contains("successful") ||
                lowerFull.contains("upi") ||
                lowerFull.contains("txn id") ||
                lowerFull.contains("rrn") ||
                lowerFull.contains("ref id") ||
                lowerFull.contains("google pay") ||
                lowerFull.contains("gpay") ||
                lowerFull.contains("phonepe") ||
                lowerFull.contains("paytm") ||
                lowerFull.contains("cred") ||
                lowerFull.contains("bhim")

        if (!isUpiIndicator) return null

        val isIncome = lowerFull.contains("received from") ||
                lowerFull.contains("money received") ||
                lowerFull.contains("cashback received") ||
                lowerFull.contains("credited to") ||
                lowerFull.contains("cashback")

        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

        val amount = extractProminentAmount(lines) ?: return null
        val merchant = extractMerchantName(lines)
        val refId = extractReferenceId(lines)
        val date = extractDate(lines) ?: System.currentTimeMillis()
        val category = if (type == TransactionType.INCOME) "Other" else resolveCategory(merchant, fullText)

        return ParsedTransaction(
            amount = amount,
            description = merchant.ifBlank { if (type == TransactionType.EXPENSE) "UPI Payment" else "UPI Received" },
            category = category,
            date = date,
            type = type,
            paymentMethod = PaymentMethod.UPI,
            referenceNumber = refId,
            isSelected = true
        )
    }

    /**
     * Parses Printed Paper Invoices, Cafe Slips, Restaurant Checks & Supermarket Bills
     */
    private fun parsePaperReceipt(lines: List<String>, fullText: String): ParsedTransaction? {
        val lower = fullText.lowercase(Locale.ROOT)

        val isPaperBill = lower.contains("tax invoice") || lower.contains("bill") ||
                lower.contains("receipt") || lower.contains("pos") || lower.contains("cash memo") ||
                lower.contains("subtotal") || lower.contains("cgst") || lower.contains("sgst") ||
                lower.contains("vat") || lower.contains("gstin") || lower.contains("round off") ||
                lower.contains("grand total") || lower.contains("net amount")

        if (!isPaperBill) return null

        val amount = extractOrderTotalAmount(lines) ?: extractProminentAmount(lines) ?: return null

        // Store name is usually the first non-trivial line at the very top of a paper bill
        var storeName = ""
        for (i in 0 until minOf(4, lines.size)) {
            val line = lines[i].trim()
            val l = line.lowercase(Locale.ROOT)
            if (line.length in 3..40 && !line.contains("₹") && !line.contains(":") &&
                !l.contains("tax invoice") && !l.contains("receipt") && !l.contains("welcome") &&
                !l.contains("cash memo") && !l.contains("gstin")) {
                storeName = cleanMerchantName(line)
                break
            }
        }

        if (storeName.isBlank()) {
            storeName = findKnownBrandInText(fullText).ifBlank { "Store Bill" }
        }

        val date = extractDate(lines) ?: System.currentTimeMillis()
        val paymentMethod = if (lower.contains("cash")) PaymentMethod.CASH else if (lower.contains("card") || lower.contains("visa")) PaymentMethod.CARD else PaymentMethod.UPI

        return ParsedTransaction(
            amount = amount,
            description = storeName,
            category = resolveCategory(storeName, fullText),
            date = date,
            type = TransactionType.EXPENSE,
            paymentMethod = paymentMethod,
            isSelected = true
        )
    }

    /**
     * Parses bank statements, Passbooks, or multi-row transaction history screens.
     */
    private fun parseStatementRows(lines: List<String>, fullText: String): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()

        for (i in lines.indices) {
            val line = lines[i]
            val matcher = CURRENCY_AMOUNT_REGEX.matcher(line)
            var amount: Double? = null
            var matchStr: String? = null

            if (matcher.find()) {
                val clean = (matcher.group(1) ?: matcher.group(2))?.replace(",", "")
                amount = clean?.toDoubleOrNull()
                matchStr = matcher.group(0)
            } else {
                // Try standalone decimal
                val decMatcher = STANDALONE_DECIMAL_REGEX.matcher(line)
                if (decMatcher.find()) {
                    val clean = decMatcher.group(1)?.replace(",", "")
                    amount = clean?.toDoubleOrNull()
                    matchStr = decMatcher.group(0)
                }
            }

            if (amount != null && amount in 1.0..500000.0) {
                // Check if this looks like a date/year
                if (amount in 2020.0..2030.0 && line.matches(Regex(".*\\b202[0-9]\\b.*"))) continue

                val surrounding = buildList {
                    if (i > 0) add(lines[i - 1])
                    add(line)
                    if (i + 1 < lines.size) add(lines[i + 1])
                }

                val rowDate = extractDate(surrounding) ?: System.currentTimeMillis()

                val isIncome = line.contains("CR", ignoreCase = true) ||
                        line.contains("+") ||
                        line.contains("Credit", ignoreCase = true) ||
                        line.contains("Received", ignoreCase = true)

                val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE

                // Extract description from remainder of line or adjacent line
                val cleanLine = line.replace(matchStr ?: "", "")
                    .replace(Regex("""\b(DR|CR|Debit|Credit)\b""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""[₹+\-]"""), "")
                    .trim()

                val description = if (cleanLine.length >= 3 && !cleanLine.matches(Regex("""[\d\s:/-]+"""))) {
                    cleanMerchantName(cleanLine)
                } else if (i > 0 && lines[i - 1].length >= 3 && !lines[i - 1].contains("₹")) {
                    cleanMerchantName(lines[i - 1])
                } else {
                    if (type == TransactionType.EXPENSE) "Expense" else "Income"
                }

                val category = if (type == TransactionType.INCOME) "Other" else suggestCategory(description)

                // Avoid duplicate additions from same line
                if (results.none { it.amount == amount && it.description == description }) {
                    results.add(
                        ParsedTransaction(
                            amount = amount,
                            description = description.take(40),
                            category = category,
                            date = rowDate,
                            type = type,
                            paymentMethod = PaymentMethod.UPI,
                            isSelected = true
                        )
                    )
                }
            }
        }

        return results
    }

    /**
     * Universal Semantic Fallback:
     * When no specific app template matches, extracts any clear expense data with high confidence.
     */
    private fun parseUniversalFallback(lines: List<String>, fullText: String): ParsedTransaction? {
        val amount = extractProminentAmount(lines) ?: return null
        val merchant = extractMerchantName(lines).ifBlank {
            findKnownBrandInText(fullText).ifBlank { "Scanned Expense" }
        }
        val date = extractDate(lines) ?: System.currentTimeMillis()
        val category = suggestCategory(merchant.ifBlank { fullText })

        return ParsedTransaction(
            amount = amount,
            description = merchant,
            category = category,
            date = date,
            type = TransactionType.EXPENSE,
            paymentMethod = PaymentMethod.UPI,
            isSelected = true
        )
    }

    private val LABEL_KEYWORDS = listOf(
        "grand total", "bill total", "total bill", "order total", "trip total",
        "total fare", "bus fare", "ticket fare", "final total", "net payable",
        "net amount", "total payable", "amount paid", "to pay", "payment of", "fare",
        "paid", "debited", "spent", "amount", "total"
    )

    private fun extractLabeledAmount(lines: List<String>): Double? {
        for (kw in LABEL_KEYWORDS) {
            for (i in lines.indices) {
                val line = lines[i]
                val lower = line.lowercase(Locale.ROOT)
                if (lower.contains(kw)) {
                    // 1. Check if the line itself contains an amount
                    val sameLineAmt = extractAmountFromLineOrValue(line)
                    if (sameLineAmt != null && isValidAmount(sameLineAmt, line)) {
                        return sameLineAmt
                    }

                    // 2. Check next line (i + 1)
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1].trim()
                        val nextAmt = extractAmountFromLineOrValue(nextLine)
                        if (nextAmt != null && isValidAmount(nextAmt, nextLine)) {
                            return nextAmt
                        }
                        val standalone = nextLine.toDoubleOrNull()
                        if (standalone != null && isValidAmount(standalone, nextLine)) {
                            return standalone
                        }
                    }

                    // 3. Check line (i + 2) if i + 1 is just a symbol or separator like "₹" or ":" or "-"
                    if (i + 2 < lines.size && lines[i + 1].trim().matches(Regex("""^[₹:\-—–=]+$"""))) {
                        val line2 = lines[i + 2].trim()
                        val amt2 = extractAmountFromLineOrValue(line2) ?: line2.toDoubleOrNull()
                        if (amt2 != null && isValidAmount(amt2, line2)) {
                            return amt2
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Extracts prominent transaction amount:
     * 1. First priority: Labeled total or paid amount (e.g. "Total ₹450", "Paid ₹1200", "Fare: Rs. 6")
     * 2. Second priority: Explicit currency symbol (₹, Rs, INR, $, or postfix Rs, /-)
     * 3. Third priority: Standalone decimal amounts
     * 4. Fourth priority: Isolated numbers with currency-like position
     */
    private fun extractProminentAmount(lines: List<String>): Double? {
        // 1. Check Labeled Amounts across lines and same line (highest confidence)
        val labeledAmt = extractLabeledAmount(lines)
        if (labeledAmt != null) {
            return labeledAmt
        }

        // 2. Check Currency Amounts (₹, Rs, INR, postfix Rs, /-)
        val currencyAmounts = mutableListOf<Double>()
        for (line in lines) {
            val matcher = CURRENCY_AMOUNT_REGEX.matcher(line)
            while (matcher.find()) {
                val match = (matcher.group(1) ?: matcher.group(2))?.replace(",", "")
                match?.toDoubleOrNull()?.let { value ->
                    if (isValidAmount(value, line)) {
                        currencyAmounts.add(value)
                    }
                }
            }
        }
        if (currencyAmounts.isNotEmpty()) {
            return currencyAmounts.first()
        }

        // 3. Check OCR Artifact Symbols (?, *, =, f)
        for (line in lines) {
            val matcher = OCR_ARTIFACT_AMOUNT_REGEX.matcher(line)
            if (matcher.find()) {
                val match = matcher.group(1)?.replace(",", "")
                match?.toDoubleOrNull()?.let { value ->
                    if (isValidAmount(value, line)) {
                        return value
                    }
                }
            }
        }

        // 4. Check Standalone Decimals (e.g. 450.00, 6.00)
        for (line in lines) {
            val matcher = STANDALONE_DECIMAL_REGEX.matcher(line)
            while (matcher.find()) {
                val match = matcher.group(1)?.replace(",", "")
                match?.toDoubleOrNull()?.let { value ->
                    if (isValidAmount(value, line)) {
                        return value
                    }
                }
            }
        }

        // 5. Check Standalone Integers on their own line (allow 1 to 5 digits, e.g. 6, 150)
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.matches(Regex("""^[0-9]{1,5}$"""))) {
                val value = trimmed.toDoubleOrNull()
                if (value != null && isValidAmount(value, line)) {
                    return value
                }
            }
        }

        return null
    }

    private fun extractAmountFromLineOrValue(line: String): Double? {
        val currMatcher = CURRENCY_AMOUNT_REGEX.matcher(line)
        if (currMatcher.find()) {
            val str = currMatcher.group(1) ?: currMatcher.group(2)
            return str?.replace(",", "")?.toDoubleOrNull()
        }
        val labeledMatcher = LABELED_AMOUNT_REGEX.matcher(line)
        if (labeledMatcher.find()) {
            return labeledMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        }
        val decMatcher = STANDALONE_DECIMAL_REGEX.matcher(line)
        if (decMatcher.find()) {
            return decMatcher.group(1)?.replace(",", "")?.toDoubleOrNull()
        }
        return null
    }

    private fun isValidAmount(value: Double, line: String): Boolean {
        if (value < 0.5 || value > 500000.0) return false

        val lower = line.lowercase(Locale.ROOT)
        val hasCurrencySymbol = line.contains("₹") || lower.contains("rs") || lower.contains("inr") ||
                line.contains("$") || line.contains("€") || line.contains("/-")

        // 1. Filter out years (2020..2030)
        if (value in 2020.0..2030.0 && line.matches(Regex(".*\\b202[0-9]\\b.*"))) return false

        // 2. Filter out times (e.g. 10:45 or 14:26 or 08:26 AM)
        if (line.contains(":") || lower.contains("am") || lower.contains("pm")) {
            if (!hasCurrencySymbol && line.matches(Regex(".*\\b[0-2]?[0-9]:[0-5][0-9]\\b.*"))) {
                return false
            }
        }

        // 3. Filter out dates without currency symbols (e.g. "28-Oct-26", "26/10/2026", "26 Oct 2026", "Date: 26")
        if (!hasCurrencySymbol) {
            val hasDateWord = lower.contains("date") || lower.contains("dated") || lower.contains("on ") ||
                    lower.contains("jan") || lower.contains("feb") || lower.contains("mar") ||
                    lower.contains("apr") || lower.contains("may") || lower.contains("jun") ||
                    lower.contains("jul") || lower.contains("aug") || lower.contains("sep") ||
                    lower.contains("oct") || lower.contains("nov") || lower.contains("dec")
            if (hasDateWord && (line.contains("-") || line.contains("/") || line.contains(","))) {
                return false
            }
        }

        // 4. Filter out transit metadata when without currency symbol:
        // (e.g. "Route No: 26", "Route 26", "Bus No: 26", "Seat 26", "Platform 2", "26 km", "26 mins")
        if (!hasCurrencySymbol && !lower.contains("fare") && !lower.contains("paid") && !lower.contains("total")) {
            val isTransitMeta = lower.contains("route") || lower.contains("bus no") || lower.contains("seat") ||
                    lower.contains("platform") || lower.contains("ticket id") || lower.contains("ticket no") ||
                    lower.contains("km") || lower.contains("kms") || lower.contains("stops") ||
                    lower.contains("min") || lower.contains("mins")
            if (isTransitMeta) return false
        }

        // 5. Filter out phone numbers / OTP / Pin
        if (lower.contains("otp") || lower.contains("pin") || lower.contains("passcode")) return false

        // 6. Filter out percentage (e.g. 2.5%, 5%, 18%)
        if (line.contains("%")) return false

        // 7. Filter out item counts (e.g. "1 item", "4 items", "2x")
        if (line.matches(Regex(""".*\b\d+\s*(?:items?|qty|pcs|x)\b.*""", RegexOption.IGNORE_CASE))) {
            if (!hasCurrencySymbol) return false
        }

        return true
    }

    private fun extractMerchantName(lines: List<String>): String {
        for (i in lines.indices) {
            val line = lines[i]
            val lower = line.lowercase(Locale.ROOT)

            // Pattern: "Paid to <Name>", "To <Name>", "Transfer to <Name>", "Received from <Name>"
            val prefixes = listOf(
                "paid successfully to", "paid to", "payment to", "transfer to", "transferred to",
                "sent to", "received from", "payment of", "to ", "from "
            )
            for (prefix in prefixes) {
                if (lower.startsWith(prefix)) {
                    val remainder = line.substring(prefix.length).trim().trimStart(':', '-').trim()
                    if (remainder.length in 2..50 && !remainder.contains("₹") && !remainder.contains("Rs")) {
                        return cleanMerchantName(remainder)
                    }
                    if (remainder.isBlank() && i + 1 < lines.size) {
                        val next = lines[i + 1].trim()
                        if (next.length in 2..50 && !next.contains("₹") && !next.lowercase().contains("successful")) {
                            return cleanMerchantName(next)
                        }
                    }
                }
            }
        }

        // Alternative: line right before "Completed" or "Payment Successful"
        for (i in lines.indices) {
            val line = lines[i].lowercase(Locale.ROOT)
            if (line.contains("completed") || line.contains("successful")) {
                if (i > 0 && lines[i - 1].length in 2..40 && !lines[i - 1].contains("₹") && !lines[i - 1].contains("Rs")) {
                    return cleanMerchantName(lines[i - 1])
                }
                if (i + 1 < lines.size && lines[i + 1].length in 2..40 && !lines[i + 1].contains("₹") && !lines[i + 1].contains("Rs")) {
                    return cleanMerchantName(lines[i + 1])
                }
            }
        }

        return ""
    }

    private fun cleanMerchantName(raw: String): String {
        return raw.replace(Regex("""^[@\.\-_:\s]+"""), "")
            .replace(Regex("""[\(\[].*?[\)\]]"""), "")
            .replace(Regex("""\b(Private Limited|Pvt Ltd|Ltd|LLP)\b""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""[0-9]{10,}"""), "") // Remove phone numbers or long account numbers
            .trim()
            .take(40)
    }

    private fun findKnownBrandInText(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val prominentBrands = listOf(
            "Swiggy", "Zomato", "Blinkit", "Zepto", "Instamart", "BigBasket", "DMart",
            "Uber", "Ola", "Rapido", "Namma Yatri", "IRCTC", "IndiGo",
            "Amazon", "Flipkart", "Myntra", "Meesho", "Ajio", "Nykaa", "Tata CLiQ",
            "Starbucks", "McDonald's", "KFC", "Domino's", "Pizza Hut", "Burger King", "Subway", "Cafe Coffee Day", "Chaayos",
            "Netflix", "Spotify", "YouTube", "BookMyShow", "PVR", "INOX",
            "Apollo Pharmacy", "Tata 1mg", "PharmEasy", "Medplus",
            "Airtel", "Jio", "Vodafone Idea", "BESCOM"
        )

        for (brand in prominentBrands) {
            if (lower.contains(brand.lowercase(Locale.ROOT))) {
                return brand
            }
        }
        return ""
    }

    private fun extractReferenceId(lines: List<String>): String? {
        val refPatterns = listOf(
            Regex("""(?:UPI Ref(?:erence)?\s*(?:No|ID)?|Txn\s*ID|Transaction\s*ID|Ref\s*No|RRN|Payment for)[\s:]*([0-9A-Za-z]{6,25})""", RegexOption.IGNORE_CASE),
            Regex("""\b([0-9]{12})\b""") // Standard 12-digit UPI RRN
        )

        for (line in lines) {
            for (pattern in refPatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    val group = match.groups[1]?.value
                    if (group != null && group.length >= 6) {
                        return group
                    }
                }
            }
        }
        return null
    }

    private fun extractDate(lines: List<String>): Long? {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)

        for (line in lines) {
            // Check standard date formats
            for (format in DATE_PATTERNS) {
                try {
                    val matcher = Pattern.compile(
                        """\b\d{1,2}[\s/-][A-Za-z]{3,9}[\s/-]\d{2,4}\b|\b[A-Za-z]{3,9}\s+\d{1,2},\s*\d{4}\b|\b\d{2}[/-]\d{2}[/-]\d{2,4}\b"""
                    ).matcher(line)
                    if (matcher.find()) {
                        val dateStr = matcher.group(0)
                        if (dateStr != null) {
                            val parsed = format.parse(dateStr)
                            if (parsed != null) {
                                return parsed.time
                            }
                        }
                    }
                } catch (_: Exception) { }
            }

            // Relative date: "Today", "Yesterday"
            val lower = line.lowercase(Locale.ROOT)
            if (lower.contains("today")) {
                return System.currentTimeMillis()
            }
            if (lower.contains("yesterday")) {
                return System.currentTimeMillis() - 86400000L
            }
        }
        return null
    }

    private fun deduplicateParsed(items: List<ParsedTransaction>): List<ParsedTransaction> {
        val distinct = mutableListOf<ParsedTransaction>()
        for (item in items) {
            if (distinct.none { it.amount == item.amount && it.description.equals(item.description, ignoreCase = true) }) {
                distinct.add(item)
            }
        }
        return distinct
    }

    /**
     * Rule-based category suggestion from merchant or note keyword matching.
     */
    fun suggestCategory(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        for ((category, keywords) in CATEGORY_KEYWORD_MAP) {
            for (kw in keywords) {
                if (lower.contains(kw)) {
                    return category
                }
            }
        }
        return "Other"
    }

    /**
     * Resolves category using primary text (e.g. merchant name), falling back to full text if needed.
     */
    fun resolveCategory(primary: String, fallbackText: String): String {
        val cat = if (primary.isNotBlank()) suggestCategory(primary) else "Other"
        return if (cat != "Other") cat else suggestCategory(fallbackText)
    }

    /**
     * Intelligent Duplicate Detection:
     * Flags a transaction as duplicate ONLY if:
     * - Same Reference Number (UPI RRN / Txn ID) if present, OR
     * - Exact same amount AND close timestamp (within 4 hours) AND matching description.
     * Prevents false duplicates on different items bought on the same day for the same price.
     */
    fun isDuplicate(
        parsed: ParsedTransaction,
        existingTransactions: List<TransactionEntity>
    ): Boolean {
        if (existingTransactions.isEmpty()) return false

        return existingTransactions.any { existing ->
            // 1. Reference Number match (highest confidence)
            if (!parsed.referenceNumber.isNullOrBlank() && !existing.description.isNullOrBlank()) {
                if (existing.description.contains(parsed.referenceNumber)) {
                    return@any true
                }
            }

            // 2. Exact amount match
            val amountMatch = abs(existing.amount - parsed.amount) < 0.01
            if (!amountMatch) return@any false

            // 3. Time difference: within 4 hours
            val timeDiffMs = abs(existing.date - parsed.date)
            val closeTime = timeDiffMs <= (4 * 3600000L)

            // 4. Description similarity
            val descA = existing.description.lowercase(Locale.ROOT).trim()
            val descB = parsed.description.lowercase(Locale.ROOT).trim()
            val descSimilar = descA.isNotBlank() && descB.isNotBlank() && (
                descA == descB ||
                descA.contains(descB) ||
                descB.contains(descA)
            )

            // Same day and matching description
            amountMatch && descSimilar && (closeTime || timeDiffMs <= 86400000L)
        }
    }
}
