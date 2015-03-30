package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.PI;
import static java.lang.Math.cos;

public class GdFilterCosine implements GdFilterInterface {

	/* Cosine filter, default radius 1 */
	public double filter(final double x)
	{
		if ((x >= -1.0) && (x <= 1.0)) return ((cos(x * PI) + 1.0)/2.0);

		return 0;
	}

}
