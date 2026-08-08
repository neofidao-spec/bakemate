package com.bakemate.ui.calculator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state computes hydration and total`() {
        val vm = CalculatorViewModel()
        val state = vm.uiState.value
        // default: 500 flour + 100 starter -> 550 total flour; 350 water + 50 starter water = 400
        // 400/550 = 72.7%
        assertEquals(72.7, state.hydration, 0.1)
        assertEquals(960.0, state.totalWeight, 0.1)
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun `update flour recomputes hydration`() {
        val vm = CalculatorViewModel()
        vm.updateFlour("600")
        val state = vm.uiState.value
        // 600 flour + 100 starter -> 650 total flour; 350 water + 50 starter water = 400
        // 400/650 = 61.5%
        assertEquals(61.5, state.hydration, 0.1)
    }

    @Test
    fun `invalid input shows error`() {
        val vm = CalculatorViewModel()
        vm.updateFlour("abc")
        val state = vm.uiState.value
        assertTrue(state.errorMessage != null)
        assertEquals(0.0, state.hydration, 0.0)
    }

    @Test
    fun `zero flour shows error`() {
        val vm = CalculatorViewModel()
        vm.updateFlour("0")
        val state = vm.uiState.value
        assertTrue(state.errorMessage != null)
    }

    @Test
    fun `scaling with target yield`() {
        val vm = CalculatorViewModel()
        vm.updateTargetYield("480")
        val state = vm.uiState.value
        assertEquals(4, state.scaled.size)
        assertEquals(250.0, state.scaled[0], 0.1)
    }

    @Test
    fun `clear resets to default`() {
        val vm = CalculatorViewModel()
        vm.updateFlour("100")
        vm.clear()
        val state = vm.uiState.value
        assertEquals("500", state.flourGrams)
    }
}
