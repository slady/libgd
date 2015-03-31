package info.miranda.gd.utils;

import info.miranda.gd.GdPointF;

/**
 * Title: Matrix
 * Group: Affine Matrix
 */
public class GdAffine {

	final double[] affine = new double[6];

	/**
	 * Function: gdAffineApplyToPointF
	 *  Applies an affine transformation to a point (floating point
	 *  gdPointF)
	 *
	 *
	 * Parameters:
	 * 	dst - Where to store the resulting point
	 *  affine - Source Point
	 *  flip_horz - affine matrix
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 */
	public GdPointF applyToPointF(final GdPointF src) {
		final GdPointF dst = new GdPointF();
		double x = src.x;
		double y = src.y;
		dst.x = x * affine[0] + y * affine[2] + affine[4];
		dst.y = x * affine[1] + y * affine[3] + affine[5];
		return dst;
	}

	/**
	 * Function: gdAffineInvert
	 *  Find the inverse of an affine transformation.
	 *
	 * All non-degenerate affine transforms are invertible. Applying the
	 * inverted matrix will restore the original values. Multiplying <src>
	 * by <dst> (commutative) will return the identity affine (rounding
	 * error possible).
	 *
	 * Parameters:
	 * 	dst - Where to store the resulting affine transform
	 *  src_affine - Original affine matrix
	 *  flip_horz - Whether or not to flip horizontally
	 *  flip_vert - Whether or not to flip vertically
	 *
	 * See also:
	 *  <gdAffineIdentity>
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 */
	public boolean invert(final GdAffine dst) {
		double r_det = (this.affine[0] * this.affine[3] - this.affine[1] * this.affine[2]);

		if (r_det <= 0.0) {
			return false;
		}

		r_det = 1.0 / r_det;
		dst.affine[0] = this.affine[3] * r_det;
		dst.affine[1] = -this.affine[1] * r_det;
		dst.affine[2] = -this.affine[2] * r_det;
		dst.affine[3] = this.affine[0] * r_det;
		dst.affine[4] = -this.affine[4] * dst.affine[0] - this.affine[5] * dst.affine[2];
		dst.affine[5] = -this.affine[4] * dst.affine[1] - this.affine[5] * dst.affine[3];
		return true;
	}

	/**
	 * Function: gdAffineFlip
	 *  Flip an affine transformation horizontally or vertically.
	 *
	 * Flips the affine transform, giving GD_FALSE for <flip_horz> and
	 * <flip_vert> will clone the affine matrix. GD_TRUE for both will
	 * copy a 180° rotation.
	 *
	 * Parameters:
	 * 	dst - Where to store the resulting affine transform
	 *  src_affine - Original affine matrix
	 *  flip_h - Whether or not to flip horizontally
	 *  flip_v - Whether or not to flip vertically
	 *
	 * Returns:
	 *  GD_TRUE on success or GD_FALSE
	 */
	public void flip(final GdAffine dst, final boolean flip_h, final boolean flip_v) {
		dst.affine[0] = flip_h ? - this.affine[0] : this.affine[0];
		dst.affine[1] = flip_h ? - this.affine[1] : this.affine[1];
		dst.affine[2] = flip_v ? - this.affine[2] : this.affine[2];
		dst.affine[3] = flip_v ? - this.affine[3] : this.affine[3];
		dst.affine[4] = flip_h ? - this.affine[4] : this.affine[4];
		dst.affine[5] = flip_v ? - this.affine[5] : this.affine[5];
	}

	/**
	 * Function: gdAffineConcat
	 * Concat (Multiply) two affine transformation matrices.
	 *
	 * Concats two affine transforms together, i.e. the result
	 * will be the equivalent of doing first the transformation m1 and then
	 * m2. All parameters can be the same matrix (safe to call using
	 * the same array for all three arguments).
	 *
	 * Parameters:
	 * 	dst - Where to store the resulting affine transform
	 *  m1 - First affine matrix
	 *  m2 - Second affine matrix
	 *
	 * Returns:
	 *  GD_TRUE on success or GD_FALSE
	 */
	public GdAffine concat(final GdAffine m1, final GdAffine m2) {
		final double dst0, dst1, dst2, dst3, dst4, dst5;

		dst0 = m1.affine[0] * m2.affine[0] + m1.affine[1] * m2.affine[2];
		dst1 = m1.affine[0] * m2.affine[1] + m1.affine[1] * m2.affine[3];
		dst2 = m1.affine[2] * m2.affine[0] + m1.affine[3] * m2.affine[2];
		dst3 = m1.affine[2] * m2.affine[1] + m1.affine[3] * m2.affine[3];
		dst4 = m1.affine[4] * m2.affine[0] + m1.affine[5] * m2.affine[2] + m2.affine[4];
		dst5 = m1.affine[4] * m2.affine[1] + m1.affine[5] * m2.affine[3] + m2.affine[5];
		dst.affine[0] = dst0;
		dst.affine[1] = dst1;
		dst.affine[2] = dst2;
		dst.affine[3] = dst3;
		dst.affine[4] = dst4;
		dst.affine[5] = dst5;
	}

	/**
	 * Function: gdAffineIdentity
	 * Set up the identity matrix.
	 *
	 * Parameters:
	 * 	dst - Where to store the resulting affine transform
	 *
	 * Returns:
	 *  GD_TRUE on success or GD_FALSE
	 */
	public int gdAffineIdentity (double dst[6])
	{
		dst[0] = 1;
		dst[1] = 0;
		dst[2] = 0;
		dst[3] = 1;
		dst[4] = 0;
		dst[5] = 0;
		return GD_TRUE;
	}

	/**
	 * Function: gdAffineScale
	 * Set up a scaling matrix.
	 *
	 * Parameters:
	 * 	scale_x - X scale factor
	 * 	scale_y - Y scale factor
	 *
	 * Returns:
	 *  GD_TRUE on success or GD_FALSE
	 */
	public int gdAffineScale (double dst[6], final double scale_x, final double scale_y)
	{
		dst[0] = scale_x;
		dst[1] = 0;
		dst[2] = 0;
		dst[3] = scale_y;
		dst[4] = 0;
		dst[5] = 0;
		return GD_TRUE;
	}

	/**
	 * Function: gdAffineRotate
	 * Set up a rotation affine transform.
	 *
	 * Like the other angle in libGD, in which increasing y moves
	 * downward, this is a counterclockwise rotation.
	 *
	 * Parameters:
	 * 	dst - Where to store the resulting affine transform
	 * 	angle - Rotation angle in degrees
	 *
	 * Returns:
	 *  GD_TRUE on success or GD_FALSE
	 */
	public int gdAffineRotate (double dst[6], final double angle)
	{
		final double sin_t = sin (angle * M_PI / 180.0);
		final double cos_t = cos (angle * M_PI / 180.0);

		dst[0] = cos_t;
		dst[1] = sin_t;
		dst[2] = -sin_t;
		dst[3] = cos_t;
		dst[4] = 0;
		dst[5] = 0;
		return GD_TRUE;
	}

	/**
	 * Function: gdAffineShearHorizontal
	 * Set up a horizontal shearing matrix || becomes \\.
	 *
	 * Parameters:
	 * 	dst - Where to store the resulting affine transform
	 * 	angle - Shear angle in degrees
	 *
	 * Returns:
	 *  GD_TRUE on success or GD_FALSE
	 */
	public int gdAffineShearHorizontal(double dst[6], final double angle)
	{
		dst[0] = 1;
		dst[1] = 0;
		dst[2] = tan(angle * M_PI / 180.0);
		dst[3] = 1;
		dst[4] = 0;
		dst[5] = 0;
		return GD_TRUE;
	}

	/**
	 * Function: gdAffineShearVertical
	 * Set up a vertical shearing matrix, columns are untouched.
	 *
	 * Parameters:
	 * 	dst - Where to store the resulting affine transform
	 * 	angle - Shear angle in degrees
	 *
	 * Returns:
	 *  GD_TRUE on success or GD_FALSE
	 */
	public int gdAffineShearVertical(double dst[6], final double angle)
	{
		dst[0] = 1;
		dst[1] = tan(angle * M_PI / 180.0);
		dst[2] = 0;
		dst[3] = 1;
		dst[4] = 0;
		dst[5] = 0;
		return GD_TRUE;
	}

	/**
	 * Function: gdAffineTranslate
	 * Set up a translation matrix.
	 *
	 * Parameters:
	 * 	dst - Where to store the resulting affine transform
	 * 	offset_x - Horizontal translation amount
	 * 	offset_y - Vertical translation amount
	 *
	 * Returns:
	 *  GD_TRUE on success or GD_FALSE
	 */
	public int gdAffineTranslate (double dst[6], final double offset_x, final double offset_y)
	{
		dst[0] = 1;
		dst[1] = 0;
		dst[2] = 0;
		dst[3] = 1;
		dst[4] = offset_x;
		dst[5] = offset_y;
		return GD_TRUE;
	}

	/**
	 * gdAffineexpansion: Find the affine's expansion factor.
	 * @src: The affine transformation.
	 *
	 * Finds the expansion factor, i.e. the square root of the factor
	 * by which the affine transform affects area. In an affine transform
	 * composed of scaling, rotation, shearing, and translation, returns
	 * the amount of scaling.
	 *
	 *  GD_TRUE on success or GD_FALSE
	 **/
	public double gdAffineExpansion ()
	{
		return sqrt (fabs (this.affine[0] * this.affine[3] - this.affine[1] * this.affine[2]));
	}

	/**
	 * Function: gdAffineRectilinear
	 * Determines whether the affine transformation is axis aligned. A
	 * tolerance has been implemented using GD_EPSILON.
	 *
	 * Parameters:
	 * 	m - The affine transformation
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 */
	public int gdAffineRectilinear (final double m[6])
	{
		return ((fabs (m[1]) < GD_EPSILON && fabs (m[2]) < GD_EPSILON) ||
				(fabs (m[0]) < GD_EPSILON && fabs (m[3]) < GD_EPSILON));
	}

	/**
	 * Function: gdAffineEqual
	 * Determines whether two affine transformations are equal. A tolerance
	 * has been implemented using GD_EPSILON.
	 *
	 * Parameters:
	 * 	m1 - The first affine transformation
	 * 	m2 - The first affine transformation
	 *
	 * Returns:
	 * 	GD_TRUE on success or GD_FALSE
	 */
	public boolean equal(final GdAffine m1, final GdAffine m2) {
		return (fabs (m1.affine[0] - m2.affine[0]) < GD_EPSILON &&
				fabs (m1.affine[1] - m2.affine[1]) < GD_EPSILON &&
				fabs (m1.affine[2] - m2.affine[2]) < GD_EPSILON &&
				fabs (m1.affine[3] - m2.affine[3]) < GD_EPSILON &&
				fabs (m1.affine[4] - m2.affine[4]) < GD_EPSILON &&
				fabs (m1.affine[5] - m2.affine[5]) < GD_EPSILON);
	}


}
