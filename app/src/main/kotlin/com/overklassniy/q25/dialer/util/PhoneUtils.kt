package com.overklassniy.q25.dialer.util

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.NumberParseException

object PhoneUtils {

    private val phoneUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

    fun formatNumber(number: String, defaultCountry: String = "RU"): String {
        return try {
            val parsed = phoneUtil.parse(number, defaultCountry)
            phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        } catch (_: NumberParseException) {
            number
        }
    }

    fun normalizeNumber(number: String): String {
        return number.filter { it.isDigit() || it == '+' }
    }

    fun numbersMatch(a: String, b: String): Boolean {
        val normA = normalizeNumber(a)
        val normB = normalizeNumber(b)
        if (normA == normB) return true
        return normA.takeLast(10) == normB.takeLast(10)
    }
}