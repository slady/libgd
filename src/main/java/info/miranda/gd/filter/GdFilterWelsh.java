package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

public class GdFilterWelsh implements GdFilterInterface {

	public double filter(final double x)
	{
	/* Welsh parabolic windowing filter */
		if (x <  1.0)
			return(1 - x*x);
		return(0.0);
	}

}
