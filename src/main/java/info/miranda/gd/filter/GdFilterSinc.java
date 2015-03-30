package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

public class GdFilterSinc implements GdFilterInterface {

	public double filter(final double x)
	{
	/* X-scaled Sinc(x) function. */
		if (x == 0.0) return(1.0);
		return (sin(PI * (double) x) / (PI * (double) x));
	}

}
