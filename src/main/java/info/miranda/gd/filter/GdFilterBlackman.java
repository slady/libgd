package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.PI;
import static java.lang.Math.cos;

public class GdFilterBlackman implements GdFilterInterface {

	public double filter(final double x) {
		return (0.42f+0.5f*(double)cos(PI*x)+0.08f*(double)cos(2.0f*PI*x));
	}

}
