package com.bakemate.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BakerMathTest {

    @Test
    fun `hydration 70 persen untuk 500g tepung 350g air`() {
        val result = BakerMath.hydrationPercent(500.0, 350.0, 0.0)
        assertEquals(70.0, result, 0.1)
    }

    @Test
    fun `hydration termasuk starter`() {
        // 500g tepung + 100g starter(50/50) + 300g air
        // total flour = 550, total water = 350 -> 63.6%
        val result = BakerMath.hydrationPercent(500.0, 300.0, 100.0)
        assertEquals(63.6, result, 0.1)
    }

    @Test
    fun `hydration nol jika tepung nol`() {
        assertEquals(0.0, BakerMath.hydrationPercent(0.0, 350.0, 0.0), 0.0)
    }

    @Test
    fun `total weight menjumlahkan semua bahan`() {
        assertEquals(960.0, BakerMath.totalWeight(500.0, 350.0, 100.0, 10.0), 0.1)
    }

    @Test
    fun `scaling ke yield target`() {
        // total 960g, target 480g -> factor 0.5
        val scaled = BakerMath.scaleToYield(500.0, 350.0, 100.0, 10.0, 480.0)
        assertNotNull(scaled)
        assertEquals(250.0, scaled!![0], 0.1)
        assertEquals(175.0, scaled[1], 0.1)
        assertEquals(50.0, scaled[2], 0.1)
        assertEquals(5.0, scaled[3], 0.1)
    }

    @Test
    fun `scaling null jika target tidak valid`() {
        assertNull(BakerMath.scaleToYield(500.0, 350.0, 100.0, 10.0, 0.0))
        assertNull(BakerMath.scaleToYield(0.0, 350.0, 100.0, 10.0, 500.0))
    }

    @Test
    fun `flour per dough`() {
        val result = BakerMath.flourPerDough(500.0, 960.0)
        assertEquals(52.1, result, 0.1)
    }

    @Test
    fun `hydration tidak sensitif terhadap pembulatan`() {
        val a = BakerMath.hydrationPercent(450.0, 320.0, 90.0)
        val b = BakerMath.hydrationPercent(450.0, 320.0, 90.0)
        assertEquals(a, b, 0.0)
    }
}
