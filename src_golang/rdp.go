package simplifyk

// Extractor is a function that extracts a coordinate from a point
type Extractor[T any] func(T) float64

// Transformer is an optional function that transforms a coordinate value
type Transformer func(float64) float64

// RamerDouglasPeucker applies the Ramer-Douglas-Peucker algorithm to simplify a polyline.
//
// The function takes a list of points of any type T and returns a simplified list
// of the same type. It uses extractor functions to get x and y coordinates from
// each point, and optional transformer functions to transform coordinates before
// simplification (without affecting the returned values).
//
// Parameters:
//   - points: A slice of points to simplify
//   - epsilon: The epsilon threshold for the RDP algorithm
//   - xExtractor: A function to extract the x coordinate from a point
//   - yExtractor: A function to extract the y coordinate from a point
//   - xTransformer: Optional function to transform x coordinates (pass nil to skip)
//   - yTransformer: Optional function to transform y coordinates (pass nil to skip)
//
// Returns:
//   - A new slice containing the simplified polyline
func RamerDouglasPeucker[T any](
	points []T,
	epsilon float64,
	xExtractor Extractor[T],
	yExtractor Extractor[T],
	xTransformer Transformer,
	yTransformer Transformer,
) []T {
	if len(points) <= 2 {
		return points
	}

	// Default transformers to identity function if nil
	if xTransformer == nil {
		xTransformer = func(x float64) float64 { return x }
	}
	if yTransformer == nil {
		yTransformer = func(y float64) float64 { return y }
	}

	simplified := []T{points[0]}

	simplifyDPStep(
		points,
		0,
		len(points)-1,
		epsilon,
		&simplified,
		xExtractor,
		yExtractor,
		xTransformer,
		yTransformer,
	)

	simplified = append(simplified, points[len(points)-1])

	return simplified
}

// simplifyDPStep performs the recursive step of the Douglas-Peucker algorithm
func simplifyDPStep[T any](
	points []T,
	firstIndex, lastIndex int,
	epsilon float64,
	simplified *[]T,
	xExtractor Extractor[T],
	yExtractor Extractor[T],
	xTransformer Transformer,
	yTransformer Transformer,
) {
	dmax := epsilon
	index := 0

	for i := firstIndex + 1; i < lastIndex; i++ {
		sqDist := squareSegDistance(
			points[i],
			points[firstIndex],
			points[lastIndex],
			xExtractor,
			yExtractor,
			xTransformer,
			yTransformer,
		)
		if sqDist > dmax {
			index = i
			dmax = sqDist
		}
	}

	if dmax > epsilon {
		if index-firstIndex > 1 {
			simplifyDPStep(
				points,
				firstIndex,
				index,
				epsilon,
				simplified,
				xExtractor,
				yExtractor,
				xTransformer,
				yTransformer,
			)
		}
		*simplified = append(*simplified, points[index])
		if lastIndex-index > 1 {
			simplifyDPStep(
				points,
				index,
				lastIndex,
				epsilon,
				simplified,
				xExtractor,
				yExtractor,
				xTransformer,
				yTransformer,
			)
		}
	}
}

// squareSegDistance calculates the square of the perpendicular distance from a point
// to a line segment defined by two other points.
func squareSegDistance[T any](
	p, p1, p2 T,
	xExtractor Extractor[T],
	yExtractor Extractor[T],
	xTransformer Transformer,
	yTransformer Transformer,
) float64 {
	x := coord(p1, xExtractor, xTransformer)
	y := coord(p1, yExtractor, yTransformer)
	dx := coord(p2, xExtractor, xTransformer) - x
	dy := coord(p2, yExtractor, yTransformer) - y

	if dx != 0.0 || dy != 0.0 {
		t := ((coord(p, xExtractor, xTransformer)-x)*dx + (coord(p, yExtractor, yTransformer)-y)*dy) /
			(dx*dx + dy*dy)

		if t > 1.0 {
			x = coord(p2, xExtractor, xTransformer)
			y = coord(p2, yExtractor, yTransformer)
		} else if t > 0.0 {
			x += dx * t
			y += dy * t
		}
	}

	dx = coord(p, xExtractor, xTransformer) - x
	dy = coord(p, yExtractor, yTransformer) - y

	return dx*dx + dy*dy
}

// coord extracts and transforms a coordinate from a point
func coord[T any](
	point T,
	extractor Extractor[T],
	transformer Transformer,
) float64 {
	return transformer(extractor(point))
}
