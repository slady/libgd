package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

public class GdFilterCatmullrom implements GdFilterInterface {

	public double filter(final double x)
	{
		if (x < -2.0)
			return(0.0f);
		if (x < -1.0)
			return(0.5f*(4.0f+x*(8.0f+x*(5.0f+x))));
		if (x < 0.0)
			return(0.5f*(2.0f+x*x*(-5.0f-3.0f*x)));
		if (x < 1.0)
			return(0.5f*(2.0f+x*x*(-5.0f+3.0f*x)));
		if (x < 2.0)
			return(0.5f*(4.0f+x*(-8.0f+x*(5.0f-x))));
		return(0.0f);
	}

}
