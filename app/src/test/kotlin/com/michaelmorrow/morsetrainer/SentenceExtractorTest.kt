package com.michaelmorrow.morsetrainer

import com.michaelmorrow.morsetrainer.service.SentenceExtractor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentenceExtractorTest {

    @Test
    fun `extracts first long sentence`() {
        val text = "Short. This is the first complete sentence with more than five words in it. Another sentence."
        val result = SentenceExtractor.extract(text)
        assertTrue(result.contains("first complete sentence"))
    }

    @Test
    fun `skips short sentences`() {
        val text = "Too short. This sentence is long enough to qualify as a valid result here. More text."
        val result = SentenceExtractor.extract(text)
        assertTrue(result.contains("long enough"))
    }

    @Test
    fun `does not split on Dr abbreviation`() {
        val text = "Dr. Smith was born in London and became a famous scientist. Another sentence here."
        val result = SentenceExtractor.extract(text)
        assertTrue(result.contains("Dr. Smith"))
        assertFalse(result.startsWith("Smith"))
    }

    @Test
    fun `does not split on Mr abbreviation`() {
        val text = "Mr. Jones said that the quick brown fox jumped over the lazy dog. Second sentence."
        val result = SentenceExtractor.extract(text)
        assertTrue(result.startsWith("Mr. Jones"))
    }

    @Test
    fun `does not split on single initial`() {
        val text = "J. K. Rowling wrote a series of books that became very popular worldwide. Done."
        val result = SentenceExtractor.extract(text)
        assertTrue(result.startsWith("J."))
    }

    @Test
    fun `strips citation brackets`() {
        val text = "This article[1] discusses the history[2] of the Roman Empire in detail."
        val result = SentenceExtractor.extract(text)
        assertFalse(result.contains("[1]"))
        assertFalse(result.contains("[2]"))
    }

    @Test
    fun `strips html tags`() {
        val text = "The <b>quick</b> brown fox jumped over the lazy dog in the forest today."
        val result = SentenceExtractor.extract(text)
        assertFalse(result.contains("<b>"))
    }

    @Test
    fun `falls back to 250 chars when no qualifying sentence`() {
        val text = "Too short. Also short. Short too."
        val result = SentenceExtractor.extract(text)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `handles exclamation mark terminator`() {
        val text = "Wow! This is an exclamation sentence that is long enough to qualify easily. More text."
        val result = SentenceExtractor.extract(text)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `handles question mark terminator`() {
        val text = "Short? This sentence asks a question that is long enough to be valid here. End."
        val result = SentenceExtractor.extract(text)
        assertTrue(result.isNotEmpty())
    }
}
