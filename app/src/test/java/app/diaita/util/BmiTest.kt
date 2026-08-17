package app.diaita.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BmiTest {
    @Test
    fun `computes kg over squared meters`() {
        assertEquals(29.6, bmi(weightKg = 96.0, heightCm = 180.0)!!, 0.05)
    }

    @Test
    fun `missing or non-positive measurements give no value`() {
        assertNull(bmi(null, 180.0))
        assertNull(bmi(96.0, null))
        assertNull(bmi(0.0, 180.0))
        assertNull(bmi(96.0, 0.0))
    }

    @Test
    fun `who bands use the standard cut-offs`() {
        assertEquals(BmiBand.UNDERWEIGHT, bmiBand(18.4))
        assertEquals(BmiBand.NORMAL, bmiBand(18.5))
        assertEquals(BmiBand.NORMAL, bmiBand(24.9))
        assertEquals(BmiBand.OVERWEIGHT, bmiBand(25.0))
        assertEquals(BmiBand.OVERWEIGHT, bmiBand(29.9))
        assertEquals(BmiBand.OBESE, bmiBand(30.0))
    }
}
