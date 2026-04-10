package com.overklassniy.q25.dialer.util

object T9Matcher {

    private val CHAR_TO_DIGIT = mapOf(
        'a' to '2', 'b' to '2', 'c' to '2',
        'd' to '3', 'e' to '3', 'f' to '3',
        'g' to '4', 'h' to '4', 'i' to '4',
        'j' to '5', 'k' to '5', 'l' to '5',
        'm' to '6', 'n' to '6', 'o' to '6',
        'p' to '7', 'q' to '7', 'r' to '7', 's' to '7',
        't' to '8', 'u' to '8', 'v' to '8',
        'w' to '9', 'x' to '9', 'y' to '9', 'z' to '9',
        // Cyrillic
        'а' to '2', 'б' to '2', 'в' to '2', 'г' to '2',
        'д' to '3', 'е' to '3', 'ж' to '3', 'з' to '3',
        'и' to '4', 'й' to '4', 'к' to '4', 'л' to '4',
        'м' to '5', 'н' to '5', 'о' to '5', 'п' to '5',
        'р' to '6', 'с' to '6', 'т' to '6', 'у' to '6',
        'ф' to '7', 'х' to '7', 'ц' to '7', 'ч' to '7',
        'ш' to '8', 'щ' to '8', 'ъ' to '8', 'ы' to '8',
        'ь' to '9', 'э' to '9', 'ю' to '9', 'я' to '9',
    )

    fun nameToT9(name: String): String {
        return name.lowercase().map { ch ->
            CHAR_TO_DIGIT[ch] ?: if (ch.isDigit()) ch else ""
        }.joinToString("")
    }

    fun matchesT9(name: String, query: String): Boolean {
        if (query.isEmpty()) return true
        val t9Name = nameToT9(name)
        if (t9Name.contains(query)) return true

        // Match start of each word
        val words = name.split("\\s+".toRegex())
        for (word in words) {
            val t9Word = nameToT9(word)
            if (t9Word.startsWith(query)) return true
        }
        return false
    }

    fun matchesNumber(number: String, query: String): Boolean {
        if (query.isEmpty()) return true
        val normalizedNumber = PhoneUtils.normalizeNumber(number)
        return normalizedNumber.contains(query)
    }
}