package dev.jamesyox.simplifyk

import dev.jamesyox.simplifyk.simplifications.ramerDouglasPeucker
import dev.jamesyox.simplifyk.simplifications.ramerDouglasPeuckerWeighted
import dev.jamesyox.simplifyk.simplifications.ramerDouglasPeuckerWeighted3D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RamerDouglasPeuckerWeightedTest {
    private data class GpsPoint(
        val x: Double,
        val y: Double,
        val accuracyMeters: Double
    )

    private data class GpsPoint3D(
        val x: Double,
        val y: Double,
        val z: Double,
        val accuracyMeters: Double
    )

    @Test
    fun weightedRdpDownweightsLowAccuracyPoints() {
        val points = listOf(
            GpsPoint(0.0, 0.0, 1.0),
            GpsPoint(1.0, 0.0, 1.0),
            GpsPoint(2.0, 2.0, 10.0),
            GpsPoint(3.0, 0.0, 1.0),
            GpsPoint(4.0, 0.0, 1.0)
        )

        val unweighted = points.ramerDouglasPeucker(
            epsilon = 1.0,
            xExtractor = { it.x },
            yExtractor = { it.y }
        )
        val weighted = points.ramerDouglasPeuckerWeighted(
            epsilon = 1.0,
            xExtractor = { it.x },
            yExtractor = { it.y },
            weightExtractor = { it.accuracyMeters }
        )

        assertTrue(
            unweighted.any { it.accuracyMeters == 10.0 },
            "Unweighted RDP should keep the low-accuracy outlier"
        )
        assertFalse(
            weighted.any { it.accuracyMeters == 10.0 },
            "Weighted RDP should remove the low-accuracy outlier"
        )
    }

    @Test
    fun preservesEndpointsInWeightedMode() {
        val points = listOf(
            GpsPoint(0.0, 0.0, 5.0),
            GpsPoint(1.0, 0.5, 5.0),
            GpsPoint(2.0, 0.0, 5.0)
        )

        val result = points.ramerDouglasPeuckerWeighted(
            epsilon = 0.01,
            xExtractor = { it.x },
            yExtractor = { it.y },
            weightExtractor = { it.accuracyMeters }
        )

        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test
    fun minWeightMustBePositive() {
        val points = listOf(
            GpsPoint(0.0, 0.0, 1.0),
            GpsPoint(1.0, 1.0, 1.0),
            GpsPoint(2.0, 0.0, 1.0)
        )

        assertFailsWith<IllegalArgumentException> {
            points.ramerDouglasPeuckerWeighted(
                epsilon = 0.1,
                xExtractor = { it.x },
                yExtractor = { it.y },
                weightExtractor = { it.accuracyMeters },
                minWeight = 0.0
            )
        }
    }

    @Test
    fun weightedRdp3DDownweightsLowAccuracyPoints() {
        val points = listOf(
            GpsPoint3D(0.0, 0.0, 0.0, 1.0),
            GpsPoint3D(1.0, 0.0, 0.0, 1.0),
            GpsPoint3D(2.0, 2.0, 2.0, 10.0),
            GpsPoint3D(3.0, 0.0, 0.0, 1.0),
            GpsPoint3D(4.0, 0.0, 0.0, 1.0)
        )

        val weighted = points.ramerDouglasPeuckerWeighted3D(
            epsilon = 1.0,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z },
            weightExtractor = { it.accuracyMeters }
        )

        assertFalse(
            weighted.any { it.accuracyMeters == 10.0 },
            "Weighted 3D RDP should remove the low-accuracy outlier"
        )
        assertEquals(points.first(), weighted.first())
        assertEquals(points.last(), weighted.last())
    }

    @Test
    fun weightedRdp3DMinWeightMustBePositive() {
        val points = listOf(
            GpsPoint3D(0.0, 0.0, 0.0, 1.0),
            GpsPoint3D(1.0, 1.0, 1.0, 1.0),
            GpsPoint3D(2.0, 0.0, 0.0, 1.0)
        )

        assertFailsWith<IllegalArgumentException> {
            points.ramerDouglasPeuckerWeighted3D(
                epsilon = 0.1,
                xExtractor = { it.x },
                yExtractor = { it.y },
                zExtractor = { it.z },
                weightExtractor = { it.accuracyMeters },
                minWeight = 0.0
            )
        }
    }
}
