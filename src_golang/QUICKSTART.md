# SimplifyK Go - Quick Start Guide

## Installation

```bash
go get github.com/jamesyox/simplifyk
```

## Basic Usage

### Simple Example

```go
package main

import (
    "fmt"
    "github.com/jamesyox/simplifyk"
)

type Point struct {
    X, Y float64
}

func main() {
    points := []Point{
        {X: 0, Y: 0},
        {X: 1, Y: 1},
        {X: 2, Y: 0.1},
        {X: 3, Y: 3},
    }
    
    // Simplify with tolerance of 1.0
    // Important: tolerance is squared in the original algorithm
    // So tolerance=1.0 means epsilon=1.0 (1.0 * 1.0)
    tolerance := 1.0
    epsilon := tolerance * tolerance
    
    simplified := simplifyk.RamerDouglasPeucker(
        points,
        epsilon,
        func(p Point) float64 { return p.X }, // X extractor
        func(p Point) float64 { return p.Y }, // Y extractor
        nil, // No X transformation
        nil, // No Y transformation
    )
    
    fmt.Printf("Simplified from %d to %d points\n", len(points), len(simplified))
}
```

## With Custom Types

```go
type GPSPoint struct {
    Latitude  float64
    Longitude float64
    Timestamp int64
    Elevation float64
}

track := []GPSPoint{ /* ... */ }

simplified := simplifyk.RamerDouglasPeucker(
    track,
    0.0001, // Small epsilon for GPS (degrees)
    func(p GPSPoint) float64 { return p.Latitude },
    func(p GPSPoint) float64 { return p.Longitude },
    nil,
    nil,
)
// All fields (Timestamp, Elevation) are preserved!
```

## With Coordinate Transformation

Transform coordinates for simplification without changing returned values:

```go
// Simplify based on screen pixels, not data coordinates
dataToPixelX := func(x float64) float64 { return x * 100 }
dataToPixelY := func(y float64) float64 { return y * 100 }

simplified := simplifyk.RamerDouglasPeucker(
    points,
    25.0, // 5-pixel tolerance squared (5*5=25)
    func(p Point) float64 { return p.X },
    func(p Point) float64 { return p.Y },
    dataToPixelX,    // Transform for algorithm
    dataToPixelY,    // Transform for algorithm
)
// Returns points with original X,Y values, not transformed ones
```

## 4-Dimensional Simplification

SimplifyK Go supports 4D simplification for space-time data and
multi-dimensional applications:

```go
type SpaceTimePoint struct {
    X, Y, Z    float64 // Spatial coordinates
    Time       float64 // Time dimension
    Temperature float64 // Additional data (preserved)
    Label      string  // Metadata (preserved)
}

trajectory := []SpaceTimePoint{
    {X: 0, Y: 0, Z: 0, Time: 0, Temperature: 20, Label: "start"},
    {X: 1, Y: 1, Z: 1, Time: 1, Temperature: 21, Label: "p1"},
    // ... more points
}

simplified := simplifyk.RamerDouglasPeucker4D(
    trajectory,
    0.5, // epsilon
    func(p SpaceTimePoint) float64 { return p.X },
    func(p SpaceTimePoint) float64 { return p.Y },
    func(p SpaceTimePoint) float64 { return p.Z },
    func(p SpaceTimePoint) float64 { return p.Time },
    nil, nil, nil, nil, // transformers (optional)
)
// All fields (Temperature, Label) are preserved!
```

### 4D Use Cases

- **Space-time trajectories**: GPS tracks with time dimension
- **Animation paths**: 3D movement with temporal component
- **Scientific data**: Multi-dimensional sensor readings
- **XYZV systems**: Any 4-coordinate system

### 4D with Transformers

```go
// Scale different dimensions independently for simplification
simplified := simplifyk.RamerDouglasPeucker4D(
    trajectory,
    100.0, // epsilon
    func(p SpaceTimePoint) float64 { return p.X },
    func(p SpaceTimePoint) float64 { return p.Y },
    func(p SpaceTimePoint) float64 { return p.Z },
    func(p SpaceTimePoint) float64 { return p.Time },
    func(x float64) float64 { return x * 100 },   // meters to cm
    func(y float64) float64 { return y * 100 },   // meters to cm
    func(z float64) float64 { return z * 100 },   // meters to cm
    func(t float64) float64 { return t * 1000 },  // seconds to ms
)
```

## Understanding Epsilon vs Tolerance

The RDP algorithm uses `epsilon` as the maximum perpendicular distance. In
simplify-js and SimplifyK:

- **tolerance** = User-friendly parameter
- **epsilon** = tolerance² (used internally)

So if you want a tolerance of 5:

```go
tolerance := 5.0
epsilon := tolerance * tolerance  // 25.0
```

## Running the Examples

```bash
cd src_golang/examples
go run main.go
```

## Running Tests

```bash
cd src_golang
go test -v
```

## Running Benchmarks

```bash
cd src_golang
go test -bench=. -benchmem
```

## API Reference

### RamerDouglasPeucker[T any]

```go
func RamerDouglasPeucker[T any](
    points []T,
    epsilon float64,
    xExtractor Extractor[T],
    yExtractor Extractor[T],
    xTransformer Transformer,
    yTransformer Transformer,
) []T
```

**Parameters:**

- `points` - Slice of points to simplify (any type)
- `epsilon` - Maximum perpendicular distance threshold (typically tolerance²)
- `xExtractor` - Function to extract X coordinate from point
- `yExtractor` - Function to extract Y coordinate from point
- `xTransformer` - Optional transformation for X (pass `nil` to skip)
- `yTransformer` - Optional transformation for Y (pass `nil` to skip)

**Returns:**

- New slice with simplified polyline (same type as input)

**Guarantees:**

- First and last points are always preserved
- Points maintain original order
- All fields in your struct are preserved (not just X,Y)
- No mutation of input slice

### RamerDouglasPeucker4D[T any]

```go
func RamerDouglasPeucker4D[T any](
    points []T,
    epsilon float64,
    xExtractor Extractor[T],
    yExtractor Extractor[T],
    zExtractor Extractor[T],
    vExtractor Extractor[T],
    xTransformer Transformer,
    yTransformer Transformer,
    zTransformer Transformer,
    vTransformer Transformer,
) []T
```

**Parameters:**

- `points` - Slice of points to simplify (any type)
- `epsilon` - Maximum perpendicular distance threshold in 4D space
- `xExtractor` - Function to extract X coordinate from point
- `yExtractor` - Function to extract Y coordinate from point
- `zExtractor` - Function to extract Z coordinate from point
- `vExtractor` - Function to extract V coordinate from point (often time)
- `xTransformer` - Optional transformation for X (pass `nil` to skip)
- `yTransformer` - Optional transformation for Y (pass `nil` to skip)
- `zTransformer` - Optional transformation for Z (pass `nil` to skip)
- `vTransformer` - Optional transformation for V (pass `nil` to skip)

**Returns:**

- New slice with simplified polyline (same type as input)

**Guarantees:**

- Same as 2D version, but operates in 4-dimensional space
- All non-coordinate fields are preserved
