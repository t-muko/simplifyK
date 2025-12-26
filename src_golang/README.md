# SimplifyK Go Port

This is a Go port of the SimplifyK library for polyline simplification. It
implements the Ramer-Douglas-Peucker (RDP) algorithm with support for custom
data types through extractor functions.

## Quick Start

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
    
    simplified := simplifyk.RamerDouglasPeucker(
        points,
        0.5, // epsilon
        func(p Point) float64 { return p.X }, // xExtractor
        func(p Point) float64 { return p.Y }, // yExtractor
        nil, // xTransformer (optional)
        nil, // yTransformer (optional)
    )
    
    fmt.Println(simplified)
}
```

## Features

- Generic implementation supporting any data type
- Extractor functions for flexible point coordinate access
- Optional transformer functions for coordinate transformation
- Efficient implementation avoiding stack overflow on large datasets
