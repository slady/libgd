package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

public class GdFilterLanczos3 implements GdFilterInterface {

	private static final double DEFAULT_LANCZOS3_RADIUS				= 3.0;

	/* Lanczos3 filter, default radius 3 */
	public double filter(final double x1)
	{
		final double x = x1 < 0.0 ? -x1 : x1;
		final double R = DEFAULT_LANCZOS3_RADIUS;

		if ( x == 0.0) return 1;

		if ( x < R)
		{
			return R * sin(x*PI) * sin(x * PI / R) / (x * PI * x * PI);
		}
		return 0.0;
	}

}
