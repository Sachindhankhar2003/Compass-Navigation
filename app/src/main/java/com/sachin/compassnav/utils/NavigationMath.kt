package com.sachin.compassnav.utils

object NavigationMath {
    /**
     * Converts a heading/bearing degree (0-360) to a cardinal direction string (N, NE, E, etc.).
     */
    fun getCardinalDirection(heading: Float): String {
        val normalized = (heading % 360f + 360f) % 360f
        val index = (((normalized + 22.5f) / 45f).toInt()) % 8
        val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        return directions[index]
    }

    /**
     * Calculates the shortest angle target to rotate from 'current' to 'target'
     * preventing unnecessary multi-rotation spins (wrapping around).
     */
    fun getShortestAngleTarget(current: Float, target: Float): Float {
        var difference = (target - current) % 360f
        if (difference < -180f) {
            difference += 360f
        } else if (difference > 180f) {
            difference -= 360f
        }
        return current + difference
    }

    /**
     * Normalizes any angle degree to fall between -180.0 and 180.0 degrees.
     */
    fun normalizeAngle(angle: Float): Float {
        var result = angle % 360f
        if (result < -180f) result += 360f
        if (result > 180f) result -= 360f
        return result
    }
}
