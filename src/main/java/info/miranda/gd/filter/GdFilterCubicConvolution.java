package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

public class GdFilterCubicConvolution implements GdFilterInterface {

	/* CubicConvolution filter, default radius 3 */
	public double filter(final double x1)
	{
		final double x = x1 < 0.0 ? -x1 : x1;
		final double x2 = x1 * x1;
		final double x2_x = x2 * x;

		if (x <= 1.0) return ((4.0 / 3.0)* x2_x - (7.0 / 3.0) * x2 + 1.0);
		if (x <= 2.0) return (- (7.0 / 12.0) * x2_x + 3 * x2 - (59.0 / 12.0) * x + 2.5);
		if (x <= 3.0) return ( (1.0/12.0) * x2_x - (2.0 / 3.0) * x2 + 1.75 * x - 1.5);
		return 0;
	}


}
