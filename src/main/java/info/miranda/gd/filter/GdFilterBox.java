package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

public class GdFilterBox implements GdFilterInterface {

	private static final double DEFAULT_FILTER_BOX					= 0.5;

	public double filter(double x) {
		if (x < - DEFAULT_FILTER_BOX)
			return 0.0f;
		if (x < DEFAULT_FILTER_BOX)
			return 1.0f;
		return 0.0f;
	}

}
