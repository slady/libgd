package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

public class GdFilterHamming implements GdFilterInterface {

	public double filter(final double x)
	{
	/* should be
	(0.54+0.46*cos(PI*(double) x));
	but this approximation is sufficient */
		if (x < -1.0f)
			return 0.0f;
		if (x < 0.0f)
			return 0.92f*(-2.0f*x-3.0f)*x*x+1.0f;
		if (x < 1.0f)
			return 0.92f*(2.0f*x-3.0f)*x*x+1.0f;
		return 0.0f;
	}

}
