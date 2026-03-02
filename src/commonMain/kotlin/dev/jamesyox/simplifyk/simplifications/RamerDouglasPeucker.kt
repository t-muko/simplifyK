package dev.jamesyox.simplifyk.simplifications

import dev.jamesyox.simplifyk.simplifications.util.coord

/**
 * Takes a list of objects representing a polyline with a xExtractor and yExtractor functions and applies
 * the Ramer-Douglas-Peucker (RDP) algorithm returning the resulting list.
 *
 * @receiver A list of any data type that you wish to apply the Ramer-Douglas-Peucker algorithm to.
 *
 * @param T The type of the data points you wish to simplify
 * @param epsilon The epsilon in the RDP algorithm in the same units as xyz
 * @param xExtractor A function that given an input T can extract the x value
 * @param yExtractor A function that given an input T can extract the y value
 * @param xTransformer Optional function that transforms the x value sent to the simplification algorithms
 *                      but not what is returned by the simplification.
 * @param yTransformer Optional function that transforms the y value sent to the simplification algorithms
 *                      but not what is returned by the simplification.
 *
 * @return A List<T> which represents the receiver with the RDP algorithm applied
 */
public inline fun <T>List<T>.ramerDouglasPeucker(
    epsilon: Double,
    crossinline xExtractor: (T) -> Double,
    crossinline yExtractor: (T) -> Double,
    crossinline xTransformer: (Double) -> Double = { it },
    crossinline yTransformer: (Double) -> Double = { it }
): List<T> {
    if (size <= 2) return this
    val simplified = mutableListOf(first())
    getSimplifyDpStep(
        epsilon = epsilon,
        xExtractor = xExtractor,
        yExtractor = yExtractor,
        xTransformer = xTransformer,
        yTransformer = yTransformer
    ).invoke(
        DpStepParams(
            points = this,
            firstIndex = 0,
            lastIndex = lastIndex,
            epsilon = epsilon,
            simplified = simplified
        )
    )
    simplified.add(last())
    return simplified.toList()
}

@PublishedApi
internal data class DpStepParams<T>(
    val points: List<T>,
    val firstIndex: Int,
    val lastIndex: Int,
    val epsilon: Double,
    val simplified: MutableList<T>
)

@PublishedApi
internal inline fun <T> getSimplifyDpStep(
    epsilon: Double,
    crossinline xExtractor: (T) -> Double,
    crossinline yExtractor: (T) -> Double,
    crossinline xTransformer: (Double) -> Double,
    crossinline yTransformer: (Double) -> Double,
): DeepRecursiveFunction<DpStepParams<T>, Unit> {
    return DeepRecursiveFunction { params ->
        var dmax = 0.0 // initialize to zero in order to recurse 3 point lists correctly
        var index = 0

        for (i in (params.firstIndex + 1) until params.lastIndex) {
            val sqDist = getSquareSegDistance(
                p = params.points[i],
                p1 = params.points[params.firstIndex],
                p2 = params.points[params.lastIndex],
                xExtractor = xExtractor,
                yExtractor = yExtractor,
                xTransformer = xTransformer,
                yTransformer = yTransformer
            )
            if (sqDist > dmax) {
                index = i
                dmax = sqDist
            }
        }
        if (dmax > epsilon * epsilon) { // the real epsilon is on the same units as x and y. 
            // The comparison uses squared values
            if ((index - params.firstIndex) > 1) {
                callRecursive(
                    DpStepParams(params.points, params.firstIndex, index, epsilon, params.simplified)
                )
            }
            params.simplified.add(params.points[index])
            if ((params.lastIndex - index) > 1) {
                callRecursive(
                    DpStepParams(params.points, index, params.lastIndex, epsilon, params.simplified)
                )
            }
        }
    }
}

@PublishedApi
@Suppress("LongParameterList")
internal inline fun <T>getSquareSegDistance(
    p: T,
    p1: T,
    p2: T,
    crossinline xExtractor: (T) -> Double,
    crossinline yExtractor: (T) -> Double,
    crossinline xTransformer: (Double) -> Double,
    crossinline yTransformer: (Double) -> Double,
): Double {
    var x = p1.coord(xExtractor, xTransformer)
    var y = p1.coord(yExtractor, yTransformer)
    var dx = p2.coord(xExtractor, xTransformer) - x
    var dy = p2.coord(yExtractor, yTransformer) - y

    if (dx != 0.0 || dy != 0.0) {
        val t = ((p.coord(xExtractor, xTransformer) - x) * dx + (p.coord(yExtractor, yTransformer) - y) * dy) /
            (dx * dx + dy * dy)

        if (t > 1.0) {
            x = p2.coord(xExtractor, xTransformer)
            y = p2.coord(yExtractor, yTransformer)
        } else if (t > 0.0) {
            x += dx * t
            y += dy * t
        }
    }

    dx = p.coord(xExtractor, xTransformer) - x
    dy = p.coord(yExtractor, yTransformer) - y

    return dx * dx + dy * dy
}

/**
 * Takes a list of objects representing a 3D polyline and applies the Ramer-Douglas-Peucker (RDP)
 * algorithm returning the resulting list.
 *
 * @receiver A list of any data type that you wish to apply the Ramer-Douglas-Peucker algorithm to.
 *
 * @param T The type of the data points you wish to simplify
 * @param epsilon The epsilon in the RDP algorithm in the same units as x, y and z.
 * @param xExtractor A function that given an input T can extract the x value
 * @param yExtractor A function that given an input T can extract the y value
 * @param zExtractor A function that given an input T can extract the z value
 * @param xTransformer Optional function that transforms the x value sent to the simplification algorithms
 *                      but not what is returned by the simplification.
 * @param yTransformer Optional function that transforms the y value sent to the simplification algorithms
 *                      but not what is returned by the simplification.
 * @param zTransformer Optional function that transforms the z value sent to the simplification algorithms
 *                      but not what is returned by the simplification.
 *
 * @return A List<T> which represents the receiver with the RDP algorithm applied in 3D space
 */
@Suppress("LongParameterList")
public inline fun <T>List<T>.ramerDouglasPeucker3D(
    epsilon: Double,
    crossinline xExtractor: (T) -> Double,
    crossinline yExtractor: (T) -> Double,
    crossinline zExtractor: (T) -> Double,
    crossinline xTransformer: (Double) -> Double = { it },
    crossinline yTransformer: (Double) -> Double = { it },
    crossinline zTransformer: (Double) -> Double = { it }
): List<T> {
    if (size <= 2) return this
    val simplified = mutableListOf(first())
    getSimplifyDpStep3D(
        epsilon = epsilon,
        xExtractor = xExtractor,
        yExtractor = yExtractor,
        zExtractor = zExtractor,
        xTransformer = xTransformer,
        yTransformer = yTransformer,
        zTransformer = zTransformer
    ).invoke(
        DpStepParams(
            points = this,
            firstIndex = 0,
            lastIndex = lastIndex,
            epsilon = epsilon,
            simplified = simplified
        )
    )
    simplified.add(last())
    return simplified.toList()
}

@PublishedApi
@Suppress("LongParameterList")
internal inline fun <T> getSimplifyDpStep3D(
    epsilon: Double,
    crossinline xExtractor: (T) -> Double,
    crossinline yExtractor: (T) -> Double,
    crossinline zExtractor: (T) -> Double,
    crossinline xTransformer: (Double) -> Double,
    crossinline yTransformer: (Double) -> Double,
    crossinline zTransformer: (Double) -> Double,
): DeepRecursiveFunction<DpStepParams<T>, Unit> {
    return DeepRecursiveFunction { params ->
        var dmax = 0.0 // initialize to zero in order to recurse 3 point lists correctly
        var index = 0

        for (i in (params.firstIndex + 1) until params.lastIndex) {
            val sqDist = getSquareSegDistance3D(
                p = params.points[i],
                p1 = params.points[params.firstIndex],
                p2 = params.points[params.lastIndex],
                xExtractor = xExtractor,
                yExtractor = yExtractor,
                zExtractor = zExtractor,
                xTransformer = xTransformer,
                yTransformer = yTransformer,
                zTransformer = zTransformer
            )
            if (sqDist > dmax) {
                index = i
                dmax = sqDist
            }
        }
        if (dmax > epsilon * epsilon) { // The comparison uses squared values
            if ((index - params.firstIndex) > 1) {
                callRecursive(
                    DpStepParams(params.points, params.firstIndex, index, epsilon, params.simplified)
                )
            }
            params.simplified.add(params.points[index])
            if ((params.lastIndex - index) > 1) {
                callRecursive(
                    DpStepParams(params.points, index, params.lastIndex, epsilon, params.simplified)
                )
            }
        }
    }
}

@PublishedApi
@Suppress("LongParameterList")
internal inline fun <T>getSquareSegDistance3D(
    p: T,
    p1: T,
    p2: T,
    crossinline xExtractor: (T) -> Double,
    crossinline yExtractor: (T) -> Double,
    crossinline zExtractor: (T) -> Double,
    crossinline xTransformer: (Double) -> Double,
    crossinline yTransformer: (Double) -> Double,
    crossinline zTransformer: (Double) -> Double,
): Double {
    var x = p1.coord(xExtractor, xTransformer)
    var y = p1.coord(yExtractor, yTransformer)
    var z = p1.coord(zExtractor, zTransformer)
    var dx = p2.coord(xExtractor, xTransformer) - x
    var dy = p2.coord(yExtractor, yTransformer) - y
    var dz = p2.coord(zExtractor, zTransformer) - z

    if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
        val t = ((p.coord(xExtractor, xTransformer) - x) * dx +
            (p.coord(yExtractor, yTransformer) - y) * dy +
            (p.coord(zExtractor, zTransformer) - z) * dz) /
            (dx * dx + dy * dy + dz * dz)

        if (t > 1.0) {
            x = p2.coord(xExtractor, xTransformer)
            y = p2.coord(yExtractor, yTransformer)
            z = p2.coord(zExtractor, zTransformer)
        } else if (t > 0.0) {
            x += dx * t
            y += dy * t
            z += dz * t
        }
    }

    dx = p.coord(xExtractor, xTransformer) - x
    dy = p.coord(yExtractor, yTransformer) - y
    dz = p.coord(zExtractor, zTransformer) - z

    return dx * dx + dy * dy + dz * dz
}

