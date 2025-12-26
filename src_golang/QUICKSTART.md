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
