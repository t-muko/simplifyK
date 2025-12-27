package main

import (
	"fmt"

	"github.com/t-muko/simplifyk/src_golang"
)

type Point struct {
	X, Y float64
}

func main() {
	// Create a simple polyline with some noise
	points := []Point{
		{X: 0, Y: 0},
		{X: 1, Y: 0.1},
		{X: 2, Y: -0.1},
		{X: 3, Y: 0.15},
		{X: 4, Y: 4},
		{X: 4.1, Y: 4.2},
		{X: 4.2, Y: 4.1},
		{X: 5, Y: 5},
	}

	fmt.Println("Original points:", len(points))
	for i, p := range points {
		fmt.Printf("  %d: (%.1f, %.1f)\n", i, p.X, p.Y)
	}

	// Simplify the polyline
	// Note: In simplify-js, tolerance is squared before being passed to RDP
	// So tolerance=1.0 means epsilon=1.0 (1.0 * 1.0)
	tolerance := 1.0
	epsilon := tolerance * tolerance

	simplified := simplifyk.RamerDouglasPeucker(
		points,
		epsilon,
		func(p Point) float64 { return p.X }, // xExtractor
		func(p Point) float64 { return p.Y }, // yExtractor
		nil,                                  // xTransformer (optional, pass nil to skip)
		nil,                                  // yTransformer (optional, pass nil to skip)
	)

	fmt.Println("\nSimplified points:", len(simplified))
	for i, p := range simplified {
		fmt.Printf("  %d: (%.1f, %.1f)\n", i, p.X, p.Y)
	}

	// Example with transformers - simplify based on pixel coordinates
	// but keep original data values
	fmt.Println("\n--- Example with coordinate transformation ---")

	// Simulate converting data coordinates to screen pixels
	dataToPixelX := func(x float64) float64 { return x * 100 } // 1 unit = 100 pixels
	dataToPixelY := func(y float64) float64 { return y * 100 }

	simplifiedWithTransform := simplifyk.RamerDouglasPeucker(
		points,
		25.0, // 5 pixel tolerance squared (5*5=25)
		func(p Point) float64 { return p.X },
		func(p Point) float64 { return p.Y },
		dataToPixelX,
		dataToPixelY,
	)

	fmt.Println("Simplified with pixel-space tolerance:", len(simplifiedWithTransform))
	for i, p := range simplifiedWithTransform {
		fmt.Printf("  %d: (%.1f, %.1f) -> screen: (%.0f, %.0f) px\n",
			i, p.X, p.Y, dataToPixelX(p.X), dataToPixelY(p.Y))
	}

	// Example with custom type
	fmt.Println("\n--- Example with custom GPS coordinate type ---")

	type GPSPoint struct {
		Lat, Lon  float64
		Timestamp int64
		Elevation float64
	}

	gpsTrack := []GPSPoint{
		{Lat: 40.7128, Lon: -74.0060, Timestamp: 1000, Elevation: 10},
		{Lat: 40.7129, Lon: -74.0061, Timestamp: 2000, Elevation: 11},
		{Lat: 40.7130, Lon: -74.0062, Timestamp: 3000, Elevation: 10},
		{Lat: 40.7589, Lon: -73.9851, Timestamp: 4000, Elevation: 50},
		{Lat: 40.7590, Lon: -73.9850, Timestamp: 5000, Elevation: 51},
	}

	simplifiedGPS := simplifyk.RamerDouglasPeucker(
		gpsTrack,
		0.0001, // Small epsilon for GPS coordinates (degrees)
		func(p GPSPoint) float64 { return p.Lat },
		func(p GPSPoint) float64 { return p.Lon },
		nil,
		nil,
	)

	fmt.Printf("Original GPS track: %d points\n", len(gpsTrack))
	fmt.Printf("Simplified GPS track: %d points\n", len(simplifiedGPS))
	fmt.Println("All metadata (timestamp, elevation) preserved in simplified points!")

	// Example with 4D simplification
	fmt.Println("\n--- Example with 4D space-time trajectory ---")

	type SpaceTimePoint struct {
		X, Y, Z     float64 // Spatial coordinates
		Time        float64 // Time dimension
		Temperature float64 // Additional metadata
	}

	trajectory := []SpaceTimePoint{
		{X: 0, Y: 0, Z: 0, Time: 0, Temperature: 20},
		{X: 1, Y: 1, Z: 1, Time: 1, Temperature: 21},
		{X: 2, Y: 2.05, Z: 1.95, Time: 2, Temperature: 22},
		{X: 3, Y: 2.95, Z: 3.05, Time: 3, Temperature: 23},
		{X: 4, Y: 4, Z: 4, Time: 4, Temperature: 24},
		{X: 5, Y: 5, Z: 5, Time: 5, Temperature: 25},
		{X: 6, Y: 5.9, Z: 6.1, Time: 6, Temperature: 26},
		{X: 7, Y: 7, Z: 7, Time: 7, Temperature: 27},
		{X: 8, Y: 8, Z: 8, Time: 8, Temperature: 28},
	}

	simplified4D := simplifyk.RamerDouglasPeucker4D(
		trajectory,
		1.0, // epsilon
		func(p SpaceTimePoint) float64 { return p.X },
		func(p SpaceTimePoint) float64 { return p.Y },
		func(p SpaceTimePoint) float64 { return p.Z },
		func(p SpaceTimePoint) float64 { return p.Time },
		nil, nil, nil, nil, // no transformers
	)

	fmt.Printf("Original 4D trajectory: %d points\n", len(trajectory))
	fmt.Printf("Simplified 4D trajectory: %d points\n", len(simplified4D))
	fmt.Println("Simplified points:")
	for i, p := range simplified4D {
		fmt.Printf("  %d: Position(%.1f, %.1f, %.1f) Time=%.1f Temp=%.1f°C\n",
			i, p.X, p.Y, p.Z, p.Time, p.Temperature)
	}
	fmt.Println("All metadata (Temperature) preserved!")
}
