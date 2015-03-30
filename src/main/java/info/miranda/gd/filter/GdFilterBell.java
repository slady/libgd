package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.pow;

public class GdFilterBell implements GdFilterInterface {

	/* Bell filter, default radius 1.5 */
	public double filter(final double x1)
	{
		final double x = x1 < 0.0 ? -x1 : x1;

		if (x < 0.5) return (0.75 - x*x);
		if (x < 1.5) return (0.5 * pow(x - 1.5, 2.0));
		return 0.0;
	}

}
