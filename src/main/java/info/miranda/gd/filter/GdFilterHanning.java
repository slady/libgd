package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.PI;
import static java.lang.Math.cos;

public class GdFilterHanning implements GdFilterInterface {

	public double filter(final double x)
	{
	/* A Cosine windowing function */
		return(0.5 + 0.5 * cos(PI * x));
	}

}
