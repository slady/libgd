package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

public class GdFilterQuadratic implements GdFilterInterface {

	/* Quadratic filter, default radius 1.5 */
	public double filter(final double x1)
	{
		final double x = x1 < 0.0 ? -x1 : x1;

		if (x <= 0.5) return (- 2.0 * x * x + 1);
		if (x <= 1.5) return (x * x - 2.5* x + 1.5);
		return 0.0;
	}

}
