package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

public class GdFilterTriangle implements GdFilterInterface {

	/* Trangle filter, default radius 1 */
	public double filter(final double x1)
	{
		final double x = x1 < 0.0 ? -x1 : x1;
		if (x < 1.0) return (1.0 - x);
		return 0.0;
	}

}
