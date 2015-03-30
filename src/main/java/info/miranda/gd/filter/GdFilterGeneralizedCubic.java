package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.abs;

public class GdFilterGeneralizedCubic implements GdFilterInterface {

	private static final double DEFAULT_FILTER_GENERALIZED_CUBIC	= 0.5;

	/**
	 * Generalized cubic kernel (for a=-1 it is the same as BicubicKernel):
	 \verbatim
	 /
	 | (a+2)|t|**3 - (a+3)|t|**2 + 1     , |t| <= 1
	 h(t) = | a|t|**3 - 5a|t|**2 + 8a|t| - 4a   , 1 < |t| <= 2
	 | 0                                 , otherwise
	 \
	 \endverbatim
	 * Often used values for a are -1 and -1/2.
	 */
	public double filter(final double t)
	{
		final double a = -DEFAULT_FILTER_GENERALIZED_CUBIC;
		double abs_t = (double)abs(t);
		double abs_t_sq = abs_t * abs_t;
		if (abs_t < 1) return (a + 2) * abs_t_sq * abs_t - (a + 3) * abs_t_sq + 1;
		if (abs_t < 2) return a * abs_t_sq * abs_t - 5 * a * abs_t_sq + 8 * a * abs_t - 4 * a;
		return 0;
	}

}
