package dev.jamesyox.simplifyk

import dev.jamesyox.simplifyk.simplifications.softExponentClampedWeight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SoftExponentWeightingTest {
    @Test
    fun returnsOneAtReferenceAccuracy() {
        val weight = softExponentClampedWeight(
            accuracyMeters = 8.0,
            referenceAccuracyMeters = 8.0,
            exponent = 0.5,
            minWeight = 0.7,
            maxWeight = 2.0
        )

        assertEquals(1.0, weight)
    }

    @Test
    fun clampsToMinimumForVeryGoodAccuracy() {
        val weight = softExponentClampedWeight(
            accuracyMeters = 1.0,
            referenceAccuracyMeters = 8.0,
            exponent = 0.5,
            minWeight = 0.7,
            maxWeight = 2.0
        )

        assertEquals(0.7, weight)
    }

    @Test
    fun scalesAndClampsForPoorAccuracy() {
        val weight = softExponentClampedWeight(
            accuracyMeters = 30.0,
            referenceAccuracyMeters = 8.0,
            exponent = 0.5,
            minWeight = 0.7,
            maxWeight = 2.0
        )

        assertTrue(weight > 1.0)
        assertTrue(weight <= 2.0)
    }

    @Test
    fun throwsForInvalidParameters() {
        assertFailsWith<IllegalArgumentException> {
            softExponentClampedWeight(accuracyMeters = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            softExponentClampedWeight(accuracyMeters = 5.0, referenceAccuracyMeters = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            softExponentClampedWeight(accuracyMeters = 5.0, exponent = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            softExponentClampedWeight(accuracyMeters = 5.0, minWeight = 0.0)
        }
        assertFailsWith<IllegalArgumentException> {
            softExponentClampedWeight(accuracyMeters = 5.0, minWeight = 2.0, maxWeight = 1.0)
        }
    }
}
