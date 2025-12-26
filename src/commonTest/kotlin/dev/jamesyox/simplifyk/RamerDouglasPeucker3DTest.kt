package dev.jamesyox.simplifyk

import dev.jamesyox.simplifyk.simplifications.ramerDouglasPeucker3D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class Point3D(
    val x: Double,
    val y: Double,
    val z: Double
)

data class SpacePoint(
    val x: Double,
    val y: Double,
    val z: Double,
    val temperature: Double,
    val label: String
)

class RamerDouglasPeucker3DTest {
    
    @Test
    fun simplify3DPolyline() {
        // Create a 3D curve with some noise
        val points3D = listOf(
            Point3D(0.0, 0.0, 0.0),
            Point3D(1.0, 1.0, 1.0),
            Point3D(2.0, 2.1, 1.9),   // Close to line
            Point3D(3.0, 2.9, 3.1),   // Close to line
            Point3D(4.0, 4.0, 4.0),
            Point3D(5.0, 5.0, 5.0),
            Point3D(6.0, 5.9, 6.1),   // Close to line
            Point3D(7.0, 7.0, 7.0),
            Point3D(8.0, 8.0, 8.0)
        )

        val result = points3D.ramerDouglasPeucker3D(
            epsilon = 1.0,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z }
        )

        // Should simplify to fewer points
        assertTrue(result.size < points3D.size, "Expected simplification in 3D")
        
        // First and last should be preserved
        assertEquals(points3D.first(), result.first(), "First point should be preserved")
        assertEquals(points3D.last(), result.last(), "Last point should be preserved")
    }

    @Test
    fun noChangeIfOnlyTwoPoints3D() {
        val input = listOf(
            Point3D(0.0, 0.0, 0.0),
            Point3D(10.0, 10.0, 10.0)
        )

        val result = input.ramerDouglasPeucker3D(
            epsilon = 1.0,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z }
        )

        assertEquals(input, result, "Two point line should not be simplified")
    }

    @Test
    fun returnEmptyWhenInputEmpty3D() {
        val empty = emptyList<Point3D>()
        
        val result = empty.ramerDouglasPeucker3D(
            epsilon = 1.0,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z }
        )

        assertEquals(empty, result, "Empty list should return empty")
    }

    @Test
    fun testWithTransformers3D() {
        val points = listOf(
            Point3D(0.0, 0.0, 0.0),
            Point3D(1.0, 1.0, 1.0),
            Point3D(2.0, 0.1, 0.1),
            Point3D(3.0, 3.0, 3.0)
        )

        val result = points.ramerDouglasPeucker3D(
            epsilon = 2.0,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z },
            xTransformer = { it * 10 },  // Scale by 10
            yTransformer = { it * 10 },
            zTransformer = { it * 10 }
        )

        // Should still return original points, not transformed ones
        result.forEach { p ->
            assertTrue(
                points.any { it == p },
                "Result should contain original points, not transformed: $p"
            )
        }
    }

    @Test
    fun testWithCustomType3D() {
        val trajectory = listOf(
            SpacePoint(0.0, 0.0, 0.0, 20.0, "start"),
            SpacePoint(1.0, 1.0, 1.0, 21.0, "p1"),
            SpacePoint(2.0, 2.05, 1.95, 22.0, "p2"),
            SpacePoint(3.0, 2.95, 3.05, 23.0, "p3"),
            SpacePoint(4.0, 4.0, 4.0, 24.0, "p4"),
            SpacePoint(5.0, 5.0, 5.0, 25.0, "end")
        )

        val result = trajectory.ramerDouglasPeucker3D(
            epsilon = 0.5,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z }
        )

        // Should simplify
        assertTrue(result.size < trajectory.size, "Expected simplification")
        
        // First and last preserved
        assertEquals(trajectory.first(), result.first())
        assertEquals(trajectory.last(), result.last())

        // Verify all metadata is preserved
        result.forEach { p ->
            assertTrue(p.temperature in 20.0..25.0, "Temperature should be preserved")
            assertTrue(p.label.isNotEmpty(), "Label should be preserved")
        }
    }

    @Test
    fun test3DPerpendicularDistance() {
        // Test a case where 3D distance matters
        val points = listOf(
            Point3D(0.0, 0.0, 0.0),
            Point3D(5.0, 0.0, 0.0),   // On X axis
            Point3D(10.0, 0.0, 5.0),  // On X axis but elevated in Z
            Point3D(15.0, 0.0, 0.0)   // Back on XY plane
        )

        val result = points.ramerDouglasPeucker3D(
            epsilon = 5.0,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z }
        )

        // The point at (10, 0, 5) should be kept because of Z distance
        assertTrue(
            result.any { it.z > 0.0 },
            "Point with Z deviation should be preserved"
        )
    }

    @Test
    fun test3DStraightLine() {
        // Perfect straight line in 3D - all points should be removed except first and last
        val points = listOf(
            Point3D(0.0, 0.0, 0.0),
            Point3D(1.0, 1.0, 1.0),
            Point3D(2.0, 2.0, 2.0),
            Point3D(3.0, 3.0, 3.0),
            Point3D(4.0, 4.0, 4.0),
            Point3D(5.0, 5.0, 5.0)
        )

        val result = points.ramerDouglasPeucker3D(
            epsilon = 0.1,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z }
        )

        // Should only keep first and last
        assertEquals(2, result.size, "Straight line should simplify to 2 points")
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test
    fun test3DWithLargeEpsilon() {
        val points = listOf(
            Point3D(0.0, 0.0, 0.0),
            Point3D(1.0, 5.0, 5.0),
            Point3D(2.0, 10.0, 10.0),
            Point3D(3.0, 15.0, 15.0)
        )

        // Very large epsilon should simplify to just endpoints
        val result = points.ramerDouglasPeucker3D(
            epsilon = 1000.0,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z }
        )

        assertEquals(2, result.size, "Large epsilon should remove all intermediate points")
    }

    @Test
    fun test3DBackwardCompatibilityWithPoint2D() {
        // Test that 3D works with 2D data (z=0)
        val points2D = listOf(
            Point3D(0.0, 0.0, 0.0),
            Point3D(1.0, 1.0, 0.0),
            Point3D(2.0, 0.1, 0.0),
            Point3D(3.0, 3.0, 0.0)
        )

        val result = points2D.ramerDouglasPeucker3D(
            epsilon = 0.5,
            xExtractor = { it.x },
            yExtractor = { it.y },
            zExtractor = { it.z }
        )

        // Should behave like 2D simplification when z=0
        assertTrue(result.size <= points2D.size)
        assertEquals(points2D.first(), result.first())
        assertEquals(points2D.last(), result.last())
    }
}
