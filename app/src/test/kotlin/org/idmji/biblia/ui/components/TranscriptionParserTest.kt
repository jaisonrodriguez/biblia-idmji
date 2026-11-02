package org.idmji.biblia.ui.components

import org.junit.Assert.*
import org.junit.Test

class TranscriptionParserTest {
    @Test fun hourTimestampOpensAtExactSecond() {
        val paragraph = TranscriptionParser.parse("[[01:06:57]](https://www.youtube.com/watch?v=X3IFaTH0huk&t=0s) Enseñanza").single()
        assertEquals("01:06:57", paragraph.timestamp)
        assertEquals("https://www.youtube.com/watch?v=X3IFaTH0huk&t=4017s", paragraph.url)
        assertEquals("Enseñanza", paragraph.text)
    }

    @Test fun minuteTimestampPreservesVideoAndOtherParameters() {
        val p = TranscriptionParser.parse("[[9:22]](https://www.youtube.com/watch?v=abc&list=def&t=1s#fragment) Texto").single()
        assertEquals("https://www.youtube.com/watch?v=abc&list=def&t=562s#fragment", p.url)
    }

    @Test fun allTimestampsAndPrefixArePreserved() {
        val paragraphs = TranscriptionParser.parse("Antes [[01:02]](https://youtu.be/abc?t=62s) Uno [[01:00:00]](https://youtu.be/abc?t=3600s) Dos")
        assertEquals(listOf("Antes", "Uno", "Dos"), paragraphs.map { it.text })
        assertEquals(listOf(null, "01:02", "01:00:00"), paragraphs.map { it.timestamp })
    }

    @Test fun plainTextAndInvalidClockAreNotLost() {
        val text = "[[01:99]](https://youtu.be/abc) Texto"
        assertEquals(text, TranscriptionParser.parse(text).single().text)
        assertNull(TranscriptionParser.parse(text).single().url)
    }

    @Test fun unrelatedHostIsNotMadeClickable() {
        val text = "[[01:02]](https://example.com/watch?v=abc) Texto"
        assertEquals(text, TranscriptionParser.parse(text).single().text)
        assertNull(TranscriptionParser.parse(text).single().url)
    }

    @Test fun emptyLinesDoNotCreateParagraphs() {
        assertEquals(listOf("Uno", "Dos"), TranscriptionParser.parse("\n Uno\n\nDos\n").map { it.text })
    }
}
