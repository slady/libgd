package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.exp;

public class GdFilterGaussian implements GdFilterInterface {

	public double filter(final double x)
	{
	/* return(exp((double) (-2.0 * x * x)) * sqrt(2.0 / PI)); */
		return (double)(exp(-2.0f * x * x) * 0.79788456080287f);
	}

}
