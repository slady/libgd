package info.miranda.gd;

public class GdClipRectangle {
	int x0, y0, x1, y1;
	GdClipRectangle(final int x0, final int y0, final int x1, final int y1) {
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
	}


/* 2.0.10: before the drawing routines, some code to clip points that are
 * outside the drawing window.  Nick Atty (nick@canalplan.org.uk)
 *
 * This is the Sutherland Hodgman Algorithm, as implemented by
 * Duvanenko, Robbins and Gyurcsik - SH(DRG) for short.  See Dr Dobb's
 * Journal, January 1996, pp107-110 and 116-117
 *
 * Given the end points of a line, and a bounding rectangle (which we
 * know to be from (0,0) to (SX,SY)), adjust the endpoints to be on
 * the edges of the rectangle if the line should be drawn at all,
 * otherwise return a failure code */

/* this does "one-dimensional" clipping: note that the second time it
   is called, all the x parameters refer to height and the y to width
   - the comments ignore this (if you can understand it when it's
   looking at the X parameters, it should become clear what happens on
   the second call!)  The code is simplified from that in the article,
   as we know that gd images always start at (0,0) */

/* 2.0.26, TBB: we now have to respect a clipping rectangle, it won't
	necessarily start at 0. */

	public boolean clip_1d(final int mindim, final int maxdim)
	{
		double m;			/* gradient of line */
		if (x0 < mindim) {
		/* start of line is left of window */
			if (x1 < mindim)		/* as is the end, so the line never cuts the window */
				return false;
			m = (y1 - y0) / (double) (x1 - x0);	/* calculate the slope of the line */
		/* adjust x0 to be on the left boundary (ie to be zero), and y0 to match */
			y0 -= (int)(m * (x0 - mindim));
			x0 = mindim;
		/* now, perhaps, adjust the far end of the line as well */
			if (x1 > maxdim) {
				y1 += m * (maxdim - x1);
				x1 = maxdim;
			}
			return true;
		}
		if (x0 > maxdim) {
		/* start of line is right of window -
		complement of above */
			if (x1 > maxdim)		/* as is the end, so the line misses the window */
				return false;
			m = (y1 - y0) / (double) (x1 - x0);	/* calculate the slope of the line */
			y0 += (int)(m * (maxdim - x0));	/* adjust so point is on the right
							   boundary */
			x0 = maxdim;
		/* now, perhaps, adjust the end of the line */
			if (x1 < mindim) {
				y1 -= (int)(m * (x1 - mindim));
				x1 = mindim;
			}
			return true;
		}
	/* the final case - the start of the line is inside the window */
		if (x1 > maxdim) {
		/* other end is outside to the right */
			m = (y1 - y0) / (double) (x1 - x0);	/* calculate the slope of the line */
			y1 += (int)(m * (maxdim - x1));
			x1 = maxdim;
			return true;
		}
		if (x1 < mindim) {
		/* other end is outside to the left */
			m = (y1 - y0) / (double) (x1 - x0);	/* calculate the slope of the line */
			y1 -= (int)(m * (x1 - mindim));
			x1 = mindim;
			return true;
		}
	/* only get here if both points are inside the window */
		return true;
	}

}
