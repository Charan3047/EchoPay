package com.example.upiexpensetracker

data class ParserTestCase(
    val input: String,
    val shouldBeTransaction: Boolean,
    val expectedAmount: Double?,
    val expectedMerchant: String?,
    val expectedType: String?
)

val parserTestCases = listOf(

    // -------------------------
    // VALID DEBIT TRANSACTIONS
    // -------------------------

    ParserTestCase(
        "₹450 paid to Uber",
        true,
        450.0,
        "UBER",
        "DEBIT"
    ),

    ParserTestCase(
        "INR 100 payment at Ekart using UPI",
        true,
        100.0,
        "EKART",
        "DEBIT"
    ),

    ParserTestCase(
        "Rs. 1200 debited from account for Amazon",
        true,
        1200.0,
        null,
        "DEBIT"
    ),

    ParserTestCase(
        "₹299 spent at Netflix",
        true,
        299.0,
        "NETFLIX",
        "DEBIT"
    ),

    ParserTestCase(
        "Rs 1499 purchase from Flipkart",
        true,
        1499.0,
        "FLIPKART",
        "DEBIT"
    ),

    ParserTestCase(
        "₹750 paid to Big Basket via UPI",
        true,
        750.0,
        "BIG BASKET",
        "DEBIT"
    ),

    // -------------------------
    // CREDIT
    // -------------------------

    ParserTestCase(
        "Rs. 50 credited to your bank account",
        true,
        50.0,
        null,
        "CREDIT"
    ),

    ParserTestCase(
        "INR 500 received in your account",
        true,
        500.0,
        null,
        "CREDIT"
    ),

    // -------------------------
    // REFUNDS
    // -------------------------

    ParserTestCase(
        "Rs. 450 refunded from Uber Eats",
        true,
        450.0,
        "UBER EATS",
        "REFUND"
    ),

    ParserTestCase(
        "450/- refund from Zomato",
        true,
        450.0,
        "ZOMATO",
        "REFUND"
    ),

    // -------------------------
    // NON-TRANSACTIONS
    // -------------------------

    ParserTestCase(
        "Hey are you there?",
        false,
        null,
        null,
        null
    ),

    ParserTestCase(
        "Your bank statement is ready",
        false,
        null,
        null,
        null
    ),

    ParserTestCase(
        "Your OTP is 123456",
        false,
        null,
        null,
        null
    ),

    ParserTestCase(
        "Your account balance is ₹5000",
        false,
        null,
        null,
        null
    ),

    ParserTestCase(
        "Your card will expire soon",
        false,
        null,
        null,
        null
    )
)