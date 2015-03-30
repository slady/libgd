package info.miranda.gd.filter;

import info.miranda.gd.interfaces.GdFilterInterface;

import static java.lang.Math.abs;

public class GdFilterBicubic implements GdFilterInterface {

	/**
	 * Bicubic interpolation kernel (a=-1):
	 \verbatim
	 /
	 | 1-2|t|**2+|t|**3          , if |t| < 1
	 h(t) = | 4-8|t|+5|t|**2-|t|**3     , if 1<=|t|<2
	 | 0                         , otherwise
	 \
	 \endverbatim
	 * ***bd*** 2.2004
	 */
	public double filter(final double t) {
		final double abs_t = (double)abs(t);
		final double abs_t_sq = abs_t * abs_t;
		if (abs_t<1) return 1-2*abs_t_sq+abs_t_sq*abs_t;
		if (abs_t<2) return 4 - 8*abs_t +5*abs_t_sq - abs_t_sq*abs_t;
		return 0;
	}

}
