package com.michaelmorrow.morsetrainer.service

object SentenceExtractor {

    private val abbreviations = setOf(
        "Dr", "Mr", "Mrs", "Ms", "Prof",
        "vs", "etc", "Jr", "Sr", "Fig", "No", "St", "Ave", "Blvd", "Dept", "Est", "Approx",
        "Inc", "Ltd", "Corp",
        "Gov", "Gen", "Col", "Sgt", "Cpl", "Pvt", "Rep", "Sen", "Rev",
        "Jan", "Feb", "Mar", "Apr", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun",
        "Vol", "pp", "ed", "al", "ie", "eg", "op", "ca", "cf", "et",
    )

    fun extract(rawText: String): String {
        val cleaned = clean(rawText)
        val sentences = splitSentences(cleaned)
        return sentences.firstOrNull { it.trim().split(Regex("\\s+")).size > 5 }
            ?: cleaned.take(250)
    }

    private fun clean(text: String): String =
        text
            .replace(Regex("\\[\\d+]"), "")
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("&[a-z]+;"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun splitSentences(text: String): List<String> {
        val sentences = mutableListOf<String>()
        var start = 0
        var i = 0

        while (i < text.length) {
            val ch = text[i]
            if (ch == '.' || ch == '!' || ch == '?') {
                val next = i + 1
                if (next >= text.length || text[next].isWhitespace()) {
                    if (ch == '.' && isAbbreviationEndingAt(text, i)) {
                        i++
                        continue
                    }
                    val sentence = text.substring(start, i + 1).trim()
                    if (sentence.isNotBlank()) sentences.add(sentence)
                    start = if (next < text.length) next else text.length
                }
            }
            i++
        }

        val tail = text.substring(start).trim()
        if (tail.isNotBlank()) sentences.add(tail)

        return sentences
    }

    private fun isAbbreviationEndingAt(text: String, periodIndex: Int): Boolean {
        var wordEnd = periodIndex
        var wordStart = wordEnd - 1
        while (wordStart >= 0 && !text[wordStart].isWhitespace()) wordStart--
        wordStart++
        val word = text.substring(wordStart, wordEnd)
        if (word.length == 1 && word[0].isUpperCase()) return true
        return word in abbreviations
    }
}
