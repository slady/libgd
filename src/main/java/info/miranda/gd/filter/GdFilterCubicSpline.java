package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.pow;

public class GdFilterCubicSpline implements GdFilterInterface {

	/* CubicSpline filter, default radius 2 */
	public double filter(final double x1)
	{
		final double x = x1 < 0.0 ? -x1 : x1;

		if (x < 1.0 ) {
			final double x2 = x*x;

			return (0.5 * x2 * x - x2 + 2.0 / 3.0);
		}
		if (x < 2.0) {
			return (pow(2.0 - x, 3.0)/6.0);
		}
		return 0;
	}

}
