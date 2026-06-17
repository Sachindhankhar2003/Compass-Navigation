package com.sachin.compassnav

import com.sachin.compassnav.utils.NavigationMath
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationMathTest {

    @Test
    fun getCardinalDirection_returnsCorrectLetters() {
        assertEquals("N", NavigationMath.getCardinalDirection(0f))
        assertEquals("N", NavigationMath.getCardinalDirection(10f))
        assertEquals("NE", NavigationMath.getCardinalDirection(45f))
        assertEquals("E", NavigationMath.getCardinalDirection(90f))
        assertEquals("S", NavigationMath.getCardinalDirection(180f))
        assertEquals("W", NavigationMath.getCardinalDirection(270f))
        assertEquals("NW", NavigationMath.getCardinalDirection(315f))
        
        // Wrap around cases
        assertEquals("N", NavigationMath.getCardinalDirection(360f))
        assertEquals("N", NavigationMath.getCardinalDirection(-10f))
        assertEquals("W", NavigationMath.getCardinalDirection(-90f))
    }

    @Test
    fun normalizeAngle_returnsWithinBounds() {
        assertEquals(0f, NavigationMath.normalizeAngle(0f), 0.01f)
        assertEquals(90f, NavigationMath.normalizeAngle(90f), 0.01f)
        assertEquals(-90f, NavigationMath.normalizeAngle(270f), 0.01f)
        assertEquals(-180f, NavigationMath.normalizeAngle(180f), 0.01f)
        assertEquals(0f, NavigationMath.normalizeAngle(360f), 0.01f)
        assertEquals(-45f, NavigationMath.normalizeAngle(-405f), 0.01f)
    }

    @Test
    fun getShortestAngleTarget_preventsSpins() {
        // Simple case: current is 0, target is 90 -> rotate +90
        assertEquals(90f, NavigationMath.getShortestAngleTarget(0f, 90f), 0.01f)
        // Wraparound case: current is 350, target is 10 -> rotate +20 degrees (to 370) instead of -340
        assertEquals(370f, NavigationMath.getShortestAngleTarget(350f, 10f), 0.01f)
        // Opposite wraparound case: current is 10, target is 350 -> rotate -20 degrees (to -10)
        assertEquals(-10f, NavigationMath.getShortestAngleTarget(10f, 350f), 0.01f)
    }
}
