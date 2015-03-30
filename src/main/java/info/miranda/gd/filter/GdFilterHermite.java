package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

public class GdFilterHermite implements GdFilterInterface {

	/* Hermite filter, default radius 1 */
	public double filter(final double x1)
	{
		final double x = x1 < 0.0 ? -x1 : x1;

		if (x < 1.0) return ((2.0 * x - 3) * x * x + 1.0 );

		return 0.0;
	}

}
