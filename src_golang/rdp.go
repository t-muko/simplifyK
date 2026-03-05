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
	dmax := 0.0
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

	if dmax > (epsilon * epsilon) {
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

// RamerDouglasPeucker4D applies the Ramer-Douglas-Peucker algorithm to simplify a 4D polyline.
//
// This extends the 2D RDP algorithm to work with 4-dimensional data (x, y, z, v).
// It uses extractor functions to get coordinates from each point, and optional
// transformer functions to transform coordinates before simplification.
//
// Parameters:
//   - points: A slice of points to simplify
//   - epsilon: The epsilon threshold for the RDP algorithm
//   - xExtractor: A function to extract the x coordinate from a point
//   - yExtractor: A function to extract the y coordinate from a point
//   - zExtractor: A function to extract the z coordinate from a point
//   - vExtractor: A function to extract the v coordinate from a point
//   - xTransformer: Optional function to transform x coordinates (pass nil to skip)
//   - yTransformer: Optional function to transform y coordinates (pass nil to skip)
//   - zTransformer: Optional function to transform z coordinates (pass nil to skip)
//   - vTransformer: Optional function to transform v coordinates (pass nil to skip)
//
// Returns:
//   - A new slice containing the simplified polyline
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
	if zTransformer == nil {
		zTransformer = func(z float64) float64 { return z }
	}
	if vTransformer == nil {
		vTransformer = func(v float64) float64 { return v }
	}

	simplified := []T{points[0]}

	simplifyDPStep4D(
		points,
		0,
		len(points)-1,
		epsilon,
		&simplified,
		xExtractor,
		yExtractor,
		zExtractor,
		vExtractor,
		xTransformer,
		yTransformer,
		zTransformer,
		vTransformer,
	)

	simplified = append(simplified, points[len(points)-1])

	return simplified
}

// simplifyDPStep4D performs the recursive step of the Douglas-Peucker algorithm for 4D data
func simplifyDPStep4D[T any](
	points []T,
	firstIndex, lastIndex int,
	epsilon float64,
	simplified *[]T,
	xExtractor Extractor[T],
	yExtractor Extractor[T],
	zExtractor Extractor[T],
	vExtractor Extractor[T],
	xTransformer Transformer,
	yTransformer Transformer,
	zTransformer Transformer,
	vTransformer Transformer,
) {
	dmax := 0.0
	index := 0

	for i := firstIndex + 1; i < lastIndex; i++ {
		sqDist := squareSegDistance4D(
			points[i],
			points[firstIndex],
			points[lastIndex],
			xExtractor,
			yExtractor,
			zExtractor,
			vExtractor,
			xTransformer,
			yTransformer,
			zTransformer,
			vTransformer,
		)
		if sqDist > dmax {
			index = i
			dmax = sqDist
		}
	}

	if dmax > (epsilon * epsilon) {
		if index-firstIndex > 1 {
			simplifyDPStep4D(
				points,
				firstIndex,
				index,
				epsilon,
				simplified,
				xExtractor,
				yExtractor,
				zExtractor,
				vExtractor,
				xTransformer,
				yTransformer,
				zTransformer,
				vTransformer,
			)
		}
		*simplified = append(*simplified, points[index])
		if lastIndex-index > 1 {
			simplifyDPStep4D(
				points,
				index,
				lastIndex,
				epsilon,
				simplified,
				xExtractor,
				yExtractor,
				zExtractor,
				vExtractor,
				xTransformer,
				yTransformer,
				zTransformer,
				vTransformer,
			)
		}
	}
}

// squareSegDistance4D calculates the square of the perpendicular distance from a point
// to a line segment defined by two other points in 4D space.
func squareSegDistance4D[T any](
	p, p1, p2 T,
	xExtractor Extractor[T],
	yExtractor Extractor[T],
	zExtractor Extractor[T],
	vExtractor Extractor[T],
	xTransformer Transformer,
	yTransformer Transformer,
	zTransformer Transformer,
	vTransformer Transformer,
) float64 {
	x := coord(p1, xExtractor, xTransformer)
	y := coord(p1, yExtractor, yTransformer)
	z := coord(p1, zExtractor, zTransformer)
	v := coord(p1, vExtractor, vTransformer)

	dx := coord(p2, xExtractor, xTransformer) - x
	dy := coord(p2, yExtractor, yTransformer) - y
	dz := coord(p2, zExtractor, zTransformer) - z
	dv := coord(p2, vExtractor, vTransformer) - v

	if dx != 0.0 || dy != 0.0 || dz != 0.0 || dv != 0.0 {
		t := ((coord(p, xExtractor, xTransformer)-x)*dx +
			(coord(p, yExtractor, yTransformer)-y)*dy +
			(coord(p, zExtractor, zTransformer)-z)*dz +
			(coord(p, vExtractor, vTransformer)-v)*dv) /
			(dx*dx + dy*dy + dz*dz + dv*dv)

		if t > 1.0 {
			x = coord(p2, xExtractor, xTransformer)
			y = coord(p2, yExtractor, yTransformer)
			z = coord(p2, zExtractor, zTransformer)
			v = coord(p2, vExtractor, vTransformer)
		} else if t > 0.0 {
			x += dx * t
			y += dy * t
			z += dz * t
			v += dv * t
		}
	}

	dx = coord(p, xExtractor, xTransformer) - x
	dy = coord(p, yExtractor, yTransformer) - y
	dz = coord(p, zExtractor, zTransformer) - z
	dv = coord(p, vExtractor, vTransformer) - v

	return dx*dx + dy*dy + dz*dz + dv*dv
}
