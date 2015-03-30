package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.abs;
import static java.lang.Math.pow;

public class GdFilterPower implements GdFilterInterface {

	public double filter(final double x)
	{
		final double a = 2.0f;
		if (abs(x)>1) return 0.0f;
		return (1.0f - (double)abs(pow(x, a)));
	}

}
