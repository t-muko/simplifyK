# 3D Simplification Support in SimplifyK (Kotlin)

## Overview

SimplifyK now supports **3-dimensional polyline simplification** using the
Ramer-Douglas-Peucker algorithm extended to 3D space, maintaining full backward
compatibility with the existing 2D API.

## What is 3D Simplification?

3D simplification works with data that has three coordinate dimensions (X, Y,
Z), calculating distances in 3D Euclidean space:

```
distance² = (x₂-x₁)² + (y₂-y₁)² + (z₂-z₁)²
```

## API

### 2D Simplification (Existing)

```kotlin
fun <T> List<T>.ramerDouglasPeucker(
    epsilon: Double,
    xExtractor: (T) -> Double,
    yExtractor: (T) -> Double,
    xTransformer: (Double) -> Double = { it },
    yTransformer: (Double) -> Double = { it }
): List<T>
```

### 3D Simplification (New)

```kotlin
fun <T> List<T>.ramerDouglasPeucker3D(
    epsilon: Double,
    xExtractor: (T) -> Double,
    yExtractor: (T) -> Double,
    zExtractor: (T) -> Double,
    xTransformer: (Double) -> Double = { it },
    yTransformer: (Double) -> Double = { it },
    zTransformer: (Double) -> Double = { it }
): List<T>
```

## Usage Examples

### Basic 3D Usage

```kotlin
data class Point3D(
    val x: Double,
    val y: Double,
    val z: Double
)

val points = listOf(
    Point3D(0.0, 0.0, 0.0),
    Point3D(1.0, 1.0, 1.0),
    Point3D(2.0, 2.1, 1.9),   // Close to line
    Point3D(3.0, 2.9, 3.1),   // Close to line
    Point3D(4.0, 4.0, 4.0)
)

val simplified = points.ramerDouglasPeucker3D(
    epsilon = 1.0,
    xExtractor = { it.x },
    yExtractor = { it.y },
    zExtractor = { it.z }
)
```

### With Custom Types and Metadata

```kotlin
data class GPSTrack(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timestamp: Long,      // Preserved!
    val accuracy: Float       // Preserved!
)

val track: List<GPSTrack> = loadGPSData()

val simplified = track.ramerDouglasPeucker3D(
    epsilon = 0.001,  // ~111 meters at equator
    xExtractor = { it.latitude },
    yExtractor = { it.longitude },
    zExtractor = { it.elevation }
)
// All fields (timestamp, accuracy) are automatically preserved!
```

### With Coordinate Transformers

```kotlin
data class ScientificData(
    val x: Double,  // meters
    val y: Double,  // meters
    val z: Double   // meters
)

val data: List<ScientificData> = collectData()

// Simplify in centimeter space while keeping meter values
val simplified = data.ramerDouglasPeucker3D(
    epsilon = 100.0,  // 10cm tolerance (squared: 10*10=100)
    xExtractor = { it.x },
    yExtractor = { it.y },
    zExtractor = { it.z },
    xTransformer = { it * 100 },  // meters to cm
    yTransformer = { it * 100 },
    zTransformer = { it * 100 }
)
// Returns points with original meter values
```

## Use Cases

### 1. GPS Tracks with Elevation

```kotlin
data class GPSPoint(
    val lat: Double,
    val lon: Double,
    val elevation: Double
)

// Simplify hiking trail data
val trail = loadTrail()
val simplified = trail.ramerDouglasPeucker3D(
    epsilon = 0.0001,
    xExtractor = { it.lat },
    yExtractor = { it.lon },
    zExtractor = { it.elevation }
)
```

### 2. 3D Graphics and Modeling

```kotlin
data class Vertex(
    val x: Double,
    val y: Double,
    val z: Double,
    val normal: Vector3D,  // Preserved
    val uv: Vector2D       // Preserved
)

// Simplify 3D mesh edges
val edges = extractEdges()
val simplified = edges.ramerDouglasPeucker3D(
    epsilon = 0.01,
    xExtractor = { it.x },
    yExtractor = { it.y },
    zExtractor = { it.z }
)
```

### 3. Scientific Simulations

```kotlin
data class ParticlePosition(
    val x: Double,
    val y: Double,
    val z: Double,
    val velocity: Vector3D,  // Preserved
    val energy: Double       // Preserved
)

// Simplify particle trajectory
val trajectory = simulateParticle()
val simplified = trajectory.ramerDouglasPeucker3D(
    epsilon = 0.1,
    xExtractor = { it.x },
    yExtractor = { it.y },
    zExtractor = { it.z }
)
```

### 4. Drone Flight Paths

```kotlin
data class FlightPoint(
    val x: Double,      // Easting
    val y: Double,      // Northing
    val altitude: Double,
    val timestamp: Instant,  // Preserved
    val battery: Int         // Preserved
)

val flightPath = recordFlight()
val simplified = flightPath.ramerDouglasPeucker3D(
    epsilon = 25.0,  // 5 meter tolerance
    xExtractor = { it.x },
    yExtractor = { it.y },
    zExtractor = { it.altitude }
)
```

## Key Features

### ✅ Type Safety

- Works with any Kotlin data type
- No need to implement interfaces
- Type-safe extractors

### ✅ Metadata Preservation

- All non-coordinate fields are automatically preserved
- No data copying required
- Efficient memory usage

### ✅ Coordinate Transformation

- Optional transformers for each axis
- Transform for algorithm, return original values
- Useful for unit conversions or screen coordinate mapping

### ✅ Backward Compatibility

- Existing 2D API unchanged
- Can use both 2D and 3D in the same project
- No breaking changes

### ✅ Performance

- Uses `DeepRecursiveFunction` to prevent stack overflow
- Handles large datasets efficiently
- Same performance characteristics as 2D version

## Implementation Details

### Distance Calculation

The perpendicular distance from point `p` to line segment `(p1, p2)` in 3D:

1. Calculate direction vector: `d = p2 - p1`
2. Project point onto line: `t = dot(p - p1, d) / dot(d, d)`
3. Clamp `t` to [0, 1] to stay on segment
4. Find closest point: `closest = p1 + t * d`
5. Calculate squared distance: `dist² = |p - closest|²`

In 3D:

```kotlin
val dx = p2.x - p1.x
val dy = p2.y - p1.y
val dz = p2.z - p1.z

val t = ((p.x - p1.x) * dx + (p.y - p1.y) * dy + (p.z - p1.z) * dz) / 
        (dx * dx + dy * dy + dz * dz)

// ... rest of calculation
```

### Stack Safety

Uses Kotlin's `DeepRecursiveFunction` to prevent stack overflow on large
datasets:

```kotlin
internal inline fun <T> getSimplifyDpStep3D(
    // ... parameters
): DeepRecursiveFunction<DpStepParams<T>, Unit> {
    return DeepRecursiveFunction { params ->
        // Recursive logic using callRecursive()
    }
}
```

## Testing

Comprehensive test suite in `RamerDouglasPeucker3DTest.kt`:

- ✅ Basic 3D simplification
- ✅ Edge cases (empty, single point, two points)
- ✅ Coordinate transformers
- ✅ Custom types with metadata
- ✅ Perpendicular distance in 3D
- ✅ Straight line detection
- ✅ Large epsilon behavior
- ✅ Backward compatibility with 2D data (z=0)

## Comparison: 2D vs 3D

| Feature      | 2D                    | 3D                      |
| ------------ | --------------------- | ----------------------- |
| Extractors   | x, y                  | x, y, z                 |
| Transformers | x, y                  | x, y, z                 |
| Distance     | √(dx² + dy²)          | √(dx² + dy² + dz²)      |
| Use Cases    | Maps, charts          | GPS, 3D graphics        |
| API          | `ramerDouglasPeucker` | `ramerDouglasPeucker3D` |

## Choosing Between 2D and 3D

### Use 2D when:

- Data is naturally 2-dimensional (maps, charts)
- Z-axis is not significant
- Simpler API is preferred
- Working with existing 2D codebase

### Use 3D when:

- Elevation/altitude matters (GPS with elevation)
- Working with 3D spatial data
- All three dimensions are significant
- Scientific or engineering applications

## Integration with Existing Code

The 3D extension is fully compatible with existing SimplifyK code:

```kotlin
// Existing 2D code continues to work
val points2D = loadMapData()
val simplified2D = points2D.simplify(
    tolerance = 5.0,
    xExtractor = { it.x },
    yExtractor = { it.y }
)

// New 3D code in the same project
val points3D = loadGPSData()
val simplified3D = points3D.ramerDouglasPeucker3D(
    epsilon = 25.0,
    xExtractor = { it.lat },
    yExtractor = { it.lon },
    zExtractor = { it.elevation }
)
```

## Future Enhancements

Potential future additions:

- 4D support (adding time dimension)
- N-dimensional support
- Additional simplification algorithms in 3D
- Performance optimizations for specific use cases

## Related Files

- **Implementation**:
  `src/commonMain/kotlin/dev/jamesyox/simplifyk/simplifications/RamerDouglasPeucker.kt`
- **Tests**:
  `src/commonTest/kotlin/dev/jamesyox/simplifyk/RamerDouglasPeucker3DTest.kt`
- **Documentation**: `README.md`

## Migration Guide

### From 2D to 3D

If you have existing 2D code and want to add 3D support:

```kotlin
// Before (2D)
data class Point(val x: Double, val y: Double)

val simplified = points.ramerDouglasPeucker(
    epsilon = 1.0,
    xExtractor = { it.x },
    yExtractor = { it.y }
)

// After (3D)
data class Point3D(val x: Double, val y: Double, val z: Double)

val simplified = points.ramerDouglasPeucker3D(
    epsilon = 1.0,
    xExtractor = { it.x },
    yExtractor = { it.y },
    zExtractor = { it.z }  // Add z extractor
)
```

That's it! Just add the `z` parameter and you're working in 3D.
