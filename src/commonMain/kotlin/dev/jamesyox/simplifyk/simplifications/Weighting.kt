package dev.jamesyox.simplifyk.simplifications

import kotlin.math.pow

/**
 * Calculates a bounded soft-exponent weight from a point accuracy value.
 *
 * This helper is intended for weighted RDP workflows where larger weights reduce a point's
 * influence (because weighted RDP compares squared distance divided by weight).
 *
 * Formula:
 * `weight = clamp((accuracyMeters / referenceAccuracyMeters)^exponent, minWeight, maxWeight)`
 *
 * Interpretation:
 * - `accuracyMeters == referenceAccuracyMeters` gives a weight near `1.0`
 * - Smaller accuracy values (better fixes) produce lower weights (clamped by `minWeight`)
 * - Larger accuracy values (worse fixes) produce higher weights (clamped by `maxWeight`)
 *
 * Suggested defaults for GPS tracks in meters:
 * - `referenceAccuracyMeters = 8.0`
 * - `exponent = 0.5` (soft/sublinear response)
 * - `minWeight = 0.7`, `maxWeight = 2.0`
 *
 * Example usage with 2D weighted RDP:
 * ```kotlin
 * val simplified = points.ramerDouglasPeuckerWeighted(
 *     epsilon = 5.0,
 *     xExtractor = { it.x },
 *     yExtractor = { it.y },
 *     weightExtractor = {
 *         softExponentClampedWeight(
 *             accuracyMeters = it.accuracyMeters,
 *             referenceAccuracyMeters = 8.0,
 *             exponent = 0.5,
 *             minWeight = 0.7,
 *             maxWeight = 2.0
 *         )
 *     }
 * )
 * ```
 *
 * Example usage with 3D weighted RDP:
 * ```kotlin
 * val simplified3D = points.ramerDouglasPeuckerWeighted3D(
 *     epsilon = 5.0,
 *     xExtractor = { it.x },
 *     yExtractor = { it.y },
 *     zExtractor = { it.z },
 *     weightExtractor = { softExponentClampedWeight(it.accuracyMeters) }
 * )
 * ```
 *
 * @param accuracyMeters Accuracy for a point in meters (for example GPS reported accuracy)
 * @param referenceAccuracyMeters Reference "good" accuracy in meters used to normalize `accuracyMeters`
 * @param exponent Soft-exponent factor controlling response curve steepness
 * @param minWeight Minimum clamp for resulting weight
 * @param maxWeight Maximum clamp for resulting weight
 *
 * @return A clamped weight value suitable for weighted RDP
 *
 * @throws IllegalArgumentException if any required parameter is non-positive or `maxWeight < minWeight`
 */
@Suppress("LongParameterList")
public fun softExponentClampedWeight(
    accuracyMeters: Double,
    referenceAccuracyMeters: Double = 8.0,
    exponent: Double = 0.5,
    minWeight: Double = 0.7,
    maxWeight: Double = 2.0
): Double {
    require(accuracyMeters > 0.0) { "accuracyMeters must be greater than zero" }
    require(referenceAccuracyMeters > 0.0) { "referenceAccuracyMeters must be greater than zero" }
    require(exponent > 0.0) { "exponent must be greater than zero" }
    require(minWeight > 0.0) { "minWeight must be greater than zero" }
    require(maxWeight >= minWeight) { "maxWeight must be greater than or equal to minWeight" }

    val ratio = accuracyMeters / referenceAccuracyMeters
    val scaledWeight = ratio.pow(exponent)
    return scaledWeight.coerceIn(minWeight, maxWeight)
}
