# 4D Simplification Features

## Overview

SimplifyK Go now supports **4-dimensional polyline simplification** using the
Ramer-Douglas-Peucker algorithm extended to 4D space.

## What is 4D Simplification?

4D simplification works with data that has four coordinate dimensions, commonly:

- **X, Y, Z** - Three spatial dimensions
- **V** - A fourth dimension (often time, but can be any variable)

The algorithm calculates distances in 4D Euclidean space:

```
distance² = (x₂-x₁)² + (y₂-y₁)² + (z₂-z₁)² + (v₂-v₁)²
```

## Use Cases

### 1. Space-Time Trajectories

Track objects moving through 3D space over time:

```go
type Trajectory struct {
    X, Y, Z float64  // Position
    Time    float64  // When
}
```

**Applications:**

- GPS tracks with elevation and time
- Drone flight paths
- Satellite trajectories
- Vehicle routing with temporal data

### 2. Animation Paths

3D object movement with temporal component:

```go
type AnimationKeyframe struct {
    PosX, PosY, PosZ float64
    TimeStamp        float64
}
```

**Applications:**

- 3D animation simplification
- Motion capture data reduction
- Game pathfinding optimization

### 3. Scientific Data

Multi-dimensional sensor readings:

```go
type SensorReading struct {
    Temperature float64
    Pressure    float64
    Humidity    float64
    Time        float64
}
```

**Applications:**

- Climate data analysis
- Medical monitoring
- Industrial process control

### 4. Multi-Variable Systems

Any system with four related variables:

```go
type DataPoint struct {
    X, Y, Z, V float64
}
```

**Applications:**

- Financial time series (price, volume, volatility, time)
- Physical simulations (x, y, z, energy)
- Custom coordinate systems

## API Comparison

### 2D Simplification

```go
RamerDouglasPeucker[T any](
    points []T,
    epsilon float64,
    xExtractor Extractor[T],
    yExtractor Extractor[T],
    xTransformer Transformer,
    yTransformer Transformer,
) []T
```

### 4D Simplification

```go
RamerDouglasPeucker4D[T any](
    points []T,
    epsilon float64,
    xExtractor Extractor[T],
    yExtractor Extractor[T],
    zExtractor Extractor[T],  // Added
    vExtractor Extractor[T],  // Added
    xTransformer Transformer,
    yTransformer Transformer,
    zTransformer Transformer, // Added
    vTransformer Transformer, // Added
) []T
```

## Examples

### Basic 4D Usage

```go
type Point4D struct {
    X, Y, Z, Time float64
}

points := []Point4D{
    {X: 0, Y: 0, Z: 0, Time: 0},
    {X: 1, Y: 1, Z: 1, Time: 1},
    // ... more points
}

simplified := simplifyk.RamerDouglasPeucker4D(
    points,
    1.0, // epsilon
    func(p Point4D) float64 { return p.X },
    func(p Point4D) float64 { return p.Y },
    func(p Point4D) float64 { return p.Z },
    func(p Point4D) float64 { return p.Time },
    nil, nil, nil, nil, // no transformers
)
```

### With Metadata Preservation

```go
type SpaceTimePoint struct {
    X, Y, Z    float64
    Time       float64
    Temperature float64 // Preserved!
    Label      string   // Preserved!
}

// All non-coordinate fields are automatically preserved
simplified := simplifyk.RamerDouglasPeucker4D(
    trajectory,
    0.5,
    func(p SpaceTimePoint) float64 { return p.X },
    func(p SpaceTimePoint) float64 { return p.Y },
    func(p SpaceTimePoint) float64 { return p.Z },
    func(p SpaceTimePoint) float64 { return p.Time },
    nil, nil, nil, nil,
)
// simplified points still have Temperature and Label!
```

### With Coordinate Transformation

```go
// Scale different dimensions independently
simplified := simplifyk.RamerDouglasPeucker4D(
    points,
    100.0, // epsilon
    func(p Point4D) float64 { return p.X },
    func(p Point4D) float64 { return p.Y },
    func(p Point4D) float64 { return p.Z },
    func(p Point4D) float64 { return p.Time },
    func(x float64) float64 { return x * 100 },   // meters to cm
    func(y float64) float64 { return y * 100 },   // meters to cm
    func(z float64) float64 { return z * 100 },   // meters to cm
    func(t float64) float64 { return t * 1000 },  // seconds to ms
)
// Returns original values, transformers only affect simplification
```

## Performance

Benchmark results on AMD Ryzen 7 PRO 7840U:

| Algorithm | Time/op | Memory/op | Allocations |
| --------- | ------- | --------- | ----------- |
| 2D RDP    | 31.7 μs | 4,080 B   | 8 allocs    |
| 4D RDP    | 36.7 μs | 4,800 B   | 4 allocs    |

**Performance impact:** ~16% slower than 2D (5 μs overhead per 100 points)

The 4D version is highly optimized and suitable for real-time applications.

## Implementation Details

### Distance Calculation

The perpendicular distance from point `p` to line segment `(p1, p2)` in 4D:

1. Calculate direction vector: `d = p2 - p1`
2. Project point onto line: `t = dot(p - p1, d) / dot(d, d)`
3. Clamp `t` to [0, 1] (point on segment)
4. Find closest point: `closest = p1 + t * d`
5. Calculate distance: `dist² = (p - closest)²`

### Algorithm Flow

1. Start with first and last points
2. Find point with maximum perpendicular distance
3. If distance > epsilon:
   - Recursively simplify left segment
   - Keep the maximum distance point
   - Recursively simplify right segment
4. Otherwise, discard all intermediate points

## Testing

Comprehensive test suite includes:

- Empty/single/two point edge cases
- General simplification tests
- Transformer function tests
- Custom type tests (SpaceTimePoint)
- Performance benchmarks

**Test coverage:** 92.8% of statements

## Choosing Between 2D and 4D

Use **2D** when:

- Data is naturally 2-dimensional (lat/lon, x/y)
- Maximum performance is critical
- Simpler API is preferred

Use **4D** when:

- Data has 3+ related dimensions
- Time is an important factor
- Multi-variable systems need simplification
- Space-time analysis is required

## Future Enhancements

Potential extensions:

- 3D version (RamerDouglasPeucker3D)
- N-dimensional version (variadic extractors)
- Parallel processing for very large datasets
- GPU acceleration

## Resources

- [Implementation notes](IMPLEMENTATION.md)
- [Quick start guide](QUICKSTART.md)
- [Main README](README.md)
- [Examples](examples/main.go)
