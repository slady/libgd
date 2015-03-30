package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

public class GdFilterFilter implements GdFilterInterface {

	public double filter(double t)
	{
	/* f(t) = 2|t|^3 - 3|t|^2 + 1, -1 <= t <= 1 */
		if(t < 0.0) t = -t;
		if(t < 1.0) return((2.0 * t - 3.0) * t * t + 1.0);
		return(0.0);
	}

}
