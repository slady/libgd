package info.miranda.gd.utils;

public class GdMath {


//fmod
//
//Compute remainder of division
//Returns the floating-point remainder of numer/denom (rounded towards zero):
//
//fmod = numer - tquot * denom
//
//Where tquot is the truncated (i.e., rounded towards zero) result of: numer/denom.
//
//
//Operator % on floating-point operations behaves analogously to the integer remainder operator;
//this may be compared with the C library function fmod.
	public static double fmod(final double number, final double denom) {
		return number % denom;
	}

	public static int fmod(final int number, final int denom) {
		return number % denom;
	}

}
