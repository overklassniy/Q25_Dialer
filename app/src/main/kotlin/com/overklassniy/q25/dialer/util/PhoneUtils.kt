package com.overklassniy.q25.dialer.util

object PhoneUtils {

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