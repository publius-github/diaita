package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberInputTest {
    @Test
    fun `parses dot and comma decimals`() {
        assertEquals(2.5, "2.5".normalizedDecimalOrNull())
        assertEquals(2.5, "2,5".normalizedDecimalOrNull())
        assertEquals(150.0, " 150 ".normalizedDecimalOrNull())
    }

    @Test
    fun `rejects garbage`() {
        assertNull("".normalizedDecimalOrNull())
        assertNull("abc".normalizedDecimalOrNull())
        assertNull("1.2.3".normalizedDecimalOrNull())
    }

    @Test
    fun `formats whole grams without trailing zero`() {
        assertEquals("100", formatGramsForInput(100.0))
        assertEquals("150.5", formatGramsForInput(150.5))
    }
}
