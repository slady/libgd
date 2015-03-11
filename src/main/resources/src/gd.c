/* $Id$ */

#include <stdio.h>
#include <math.h>
#include <string.h>
#include <stdlib.h>
#include <stdarg.h>

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gd_intern.h"

/* 2.03: don't include zlib here or we can't build without PNG */
#include "gd.h"
#include "gdhelpers.h"
#include "gd_color.h"
#include "gd_errors.h"




/* This code is taken from http://www.acm.org/jgt/papers/SmithLyons96/hwb_rgb.html, an article
 * on colour conversion to/from RBG and HWB colour systems.
 * It has been modified to return the converted value as a * parameter.
 */

#define RETURN_HWB(h, w, b) {HWB->H = h; HWB->W = w; HWB->B = b; return HWB;}
#define RETURN_RGB(r, g, b) {RGB->R = r; RGB->G = g; RGB->B = b; return RGB;}
#define HWB_UNDEFINED -1
#define SETUP_RGB(s, r, g, b) {s.R = r/255.0; s.G = g/255.0; s.B = b/255.0;}

#define MIN(a,b) ((a)<(b)?(a):(b))
#define MIN3(a,b,c) ((a)<(b)?(MIN(a,c)):(MIN(b,c)))
#define MAX(a,b) ((a)<(b)?(b):(a))
#define MAX3(a,b,c) ((a)<(b)?(MAX(b,c)):(MAX(a,c)))


/*
 * Theoretically, hue 0 (pure red) is identical to hue 6 in these transforms. Pure
 * red always maps to 6 in this implementation. Therefore UNDEFINED can be
 * defined as 0 in situations where only unsigned numbers are desired.
 */
typedef struct {
	float R, G, B;
}
RGBType;
typedef struct {
	float H, W, B;
}
HWBType;

static HWBType *
RGB_to_HWB (RGBType RGB, HWBType * HWB)
{

	/*
	 * RGB are each on [0, 1]. W and B are returned on [0, 1] and H is
	 * returned on [0, 6]. Exception: H is returned UNDEFINED if W == 1 - B.
	 */

	float R = RGB.R, G = RGB.G, B = RGB.B, w, v, b, f;
	int i;

	w = MIN3 (R, G, B);
	v = MAX3 (R, G, B);
	b = 1 - v;
	if (v == w)
		RETURN_HWB (HWB_UNDEFINED, w, b);
	f = (R == w) ? G - B : ((G == w) ? B - R : R - G);
	i = (R == w) ? 3 : ((G == w) ? 5 : 1);
	RETURN_HWB (i - f / (v - w), w, b);

}

static float
HWB_Diff (int r1, int g1, int b1, int r2, int g2, int b2)
{
	RGBType RGB1, RGB2;
	HWBType HWB1, HWB2;
	float diff;

	SETUP_RGB (RGB1, r1, g1, b1);
	SETUP_RGB (RGB2, r2, g2, b2);

	RGB_to_HWB (RGB1, &HWB1);
	RGB_to_HWB (RGB2, &HWB2);

	/*
	 * I made this bit up; it seems to produce OK results, and it is certainly
	 * more visually correct than the current RGB metric. (PJW)
	 */

	if ((HWB1.H == HWB_UNDEFINED) || (HWB2.H == HWB_UNDEFINED)) {
		diff = 0;			/* Undefined hues always match... */
	} else {
		diff = fabs (HWB1.H - HWB2.H);
		if (diff > 3) {
			diff = 6 - diff;	/* Remember, it's a colour circle */
		}
	}

	diff =
	    diff * diff + (HWB1.W - HWB2.W) * (HWB1.W - HWB2.W) + (HWB1.B -
	            HWB2.B) * (HWB1.B -
	                       HWB2.B);

	return diff;
}


#if 0
/*
 * This is not actually used, but is here for completeness, in case someone wants to
 * use the HWB stuff for anything else...
 */
static RGBType *
HWB_to_RGB (HWBType HWB, RGBType * RGB)
{

	/*
	 * H is given on [0, 6] or UNDEFINED. W and B are given on [0, 1].
	 * RGB are each returned on [0, 1].
	 */

	float h = HWB.H, w = HWB.W, b = HWB.B, v, n, f;
	int i;

	v = 1 - b;
	if (h == HWB_UNDEFINED)
		RETURN_RGB (v, v, v);
	i = floor (h);
	f = h - i;
	if (i & 1)
		f = 1 - f;			/* if i is odd */
	n = w + f * (v - w);		/* linear interpolation between w and v */
	switch (i) {
	case 6:
	case 0:
		RETURN_RGB (v, n, w);
	case 1:
		RETURN_RGB (n, v, w);
	case 2:
		RETURN_RGB (w, v, n);
	case 3:
		RETURN_RGB (w, n, v);
	case 4:
		RETURN_RGB (n, w, v);
	case 5:
		RETURN_RGB (v, w, n);
	}

	return RGB;

}
#endif

BGD_DECLARE(int) gdImageColorClosestHWB (gdImagePtr im, int r, int g, int b)
{
	int i;
	/* long rd, gd, bd; */
	int ct = (-1);
	int first = 1;
	float mindist = 0;
	if (im->trueColor) {
		return gdTrueColor (r, g, b);
	}
	for (i = 0; (i < (im->colorsTotal)); i++) {
		float dist;
		if (im->open[i]) {
			continue;
		}
		dist = HWB_Diff (im->red[i], im->green[i], im->blue[i], r, g, b);
		if (first || (dist < mindist)) {
			mindist = dist;
			ct = i;
			first = 0;
		}
	}
	return ct;
}

BGD_DECLARE(int) gdImageColorExact (gdImagePtr im, int r, int g, int b)
{
	return gdImageColorExactAlpha (im, r, g, b, gdAlphaOpaque);
}

BGD_DECLARE(int) gdImageColorExactAlpha (gdImagePtr im, int r, int g, int b, int a)
{
	int i;
	if (im->trueColor) {
		return gdTrueColorAlpha (r, g, b, a);
	}
	for (i = 0; (i < (im->colorsTotal)); i++) {
		if (im->open[i]) {
			continue;
		}
		if ((im->red[i] == r) &&
		        (im->green[i] == g) && (im->blue[i] == b) && (im->alpha[i] == a)) {
			return i;
		}
	}
	return -1;
}

BGD_DECLARE(int) gdImageColorAllocate (gdImagePtr im, int r, int g, int b)
{
	return gdImageColorAllocateAlpha (im, r, g, b, gdAlphaOpaque);
}

BGD_DECLARE(int) gdImageColorAllocateAlpha (gdImagePtr im, int r, int g, int b, int a)
{
	int i;
	int ct = (-1);
	if (im->trueColor) {
		return gdTrueColorAlpha (r, g, b, a);
	}
	for (i = 0; (i < (im->colorsTotal)); i++) {
		if (im->open[i]) {
			ct = i;
			break;
		}
	}
	if (ct == (-1)) {
		ct = im->colorsTotal;
		if (ct == gdMaxColors) {
			return -1;
		}
		im->colorsTotal++;
	}
	im->red[ct] = r;
	im->green[ct] = g;
	im->blue[ct] = b;
	im->alpha[ct] = a;
	im->open[ct] = 0;
	return ct;
}

BGD_DECLARE(void) gdImageColorDeallocate (gdImagePtr im, int color)
{
	if (im->trueColor || (color >= gdMaxColors) || (color < 0)) {
		return;
	}
	/* Mark it open. */
	im->open[color] = 1;
}

BGD_DECLARE(void) gdImageColorTransparent (gdImagePtr im, int color)
{
	if (!im->trueColor) {
		if((color < -1) || (color >= gdMaxColors)) {
			return;
		}
		if (im->transparent != -1) {
			im->alpha[im->transparent] = gdAlphaOpaque;
		}
		if (color != -1) {
			im->alpha[color] = gdAlphaTransparent;
		}
	}
	im->transparent = color;
}

BGD_DECLARE(void) gdImagePaletteCopy (gdImagePtr to, gdImagePtr from)
{
	int i;
	int x, y, p;
	int xlate[256];
	if (to->trueColor) {
		return;
	}
	if (from->trueColor) {
		return;
	}

	for (i = 0; i < 256; i++) {
		xlate[i] = -1;
	};

	for (y = 0; y < (to->sy); y++) {
		for (x = 0; x < (to->sx); x++) {
			/* Optimization: no gdImageGetPixel */
			p = to->pixels[y][x];
			if (xlate[p] == -1) {
				/* This ought to use HWB, but we don't have an alpha-aware
				   version of that yet. */
				xlate[p] =
				    gdImageColorClosestAlpha (from, to->red[p], to->green[p],
				                              to->blue[p], to->alpha[p]);
				/*printf("Mapping %d (%d, %d, %d, %d) to %d (%d, %d, %d, %d)\n", */
				/*      p,  to->red[p], to->green[p], to->blue[p], to->alpha[p], */
				/*      xlate[p], from->red[xlate[p]], from->green[xlate[p]], from->blue[xlate[p]], from->alpha[xlate[p]]); */
			};
			/* Optimization: no gdImageSetPixel */
			to->pixels[y][x] = xlate[p];
		};
	};

	for (i = 0; (i < (from->colorsTotal)); i++) {
		/*printf("Copying color %d (%d, %d, %d, %d)\n", i, from->red[i], from->blue[i], from->green[i], from->alpha[i]); */
		to->red[i] = from->red[i];
		to->blue[i] = from->blue[i];
		to->green[i] = from->green[i];
		to->alpha[i] = from->alpha[i];
		to->open[i] = 0;
	};

	for (i = from->colorsTotal; (i < to->colorsTotal); i++) {
		to->open[i] = 1;
	};

	to->colorsTotal = from->colorsTotal;

}

BGD_DECLARE(int) gdImageColorReplace (gdImagePtr im, int src, int dst)
{
	register int x, y;
	int n = 0;

	if (src == dst) {
		return 0;
	}

#define REPLACING_LOOP(pixel) do {								\
		for (y = im->cy1; y <= im->cy2; y++) {					\
			for (x = im->cx1; x <= im->cx2; x++) {				\
				if (pixel(im, x, y) == src) {					\
					gdImageSetPixel(im, x, y, dst);				\
					n++;										\
				}												\
			}													\
		}														\
	} while (0)

	if (im->trueColor) {
		REPLACING_LOOP(gdImageTrueColorPixel);
	} else {
		REPLACING_LOOP(gdImagePalettePixel);
	}

#undef REPLACING_LOOP

	return n;
}

BGD_DECLARE(int) gdImageColorReplaceThreshold (gdImagePtr im, int src, int dst, float threshold)
{
	register int x, y;
	int n = 0;

	if (src == dst) {
		return 0;
	}

#define REPLACING_LOOP(pixel) do {										\
		for (y = im->cy1; y <= im->cy2; y++) {							\
			for (x = im->cx1; x <= im->cx2; x++) {						\
				if (gdColorMatch(im, src, pixel(im, x, y), threshold)) { \
					gdImageSetPixel(im, x, y, dst);						\
					n++;												\
				}														\
			}															\
		}																\
	} while (0)

	if (im->trueColor) {
		REPLACING_LOOP(gdImageTrueColorPixel);
	} else {
		REPLACING_LOOP(gdImagePalettePixel);
	}

#undef REPLACING_LOOP

	return n;
}

static int colorCmp (const void *x, const void *y)
{
	int a = *(int const *)x;
	int b = *(int const *)y;
	return (a > b) - (a < b);
}

BGD_DECLARE(int) gdImageColorReplaceArray (gdImagePtr im, int len, int *src, int *dst)
{
	register int x, y;
	int c, *d, *base;
	int i, n = 0;

	if (len <= 0 || src == dst) {
		return 0;
	}
	if (len == 1) {
		return gdImageColorReplace(im, src[0], dst[0]);
	}
	if (overflow2(len, sizeof(int)<<1)) {
		return -1;
	}
	base = (int *)gdMalloc(len * (sizeof(int)<<1));
	if (!base) {
		return -1;
	}
	for (i = 0; i < len; i++) {
		base[(i<<1)]   = src[i];
		base[(i<<1)+1] = dst[i];
	}
	qsort(base, len, sizeof(int)<<1, colorCmp);

#define REPLACING_LOOP(pixel) do {										\
		for (y = im->cy1; y <= im->cy2; y++) {							\
			for (x = im->cx1; x <= im->cx2; x++) {						\
				c = pixel(im, x, y);									\
				if ( (d = (int *)bsearch(&c, base, len, sizeof(int)<<1, colorCmp)) ) { \
					gdImageSetPixel(im, x, y, d[1]);					\
					n++;												\
				}														\
			}															\
		}																\
	} while (0)

	if (im->trueColor) {
		REPLACING_LOOP(gdImageTrueColorPixel);
	} else {
		REPLACING_LOOP(gdImagePalettePixel);
	}

#undef REPLACING_LOOP

	gdFree(base);
	return n;
}

BGD_DECLARE(int) gdImageColorReplaceCallback (gdImagePtr im, gdCallbackImageColor callback)
{
	int c, d, n = 0;

	if (!callback) {
		return 0;
	}
	if (im->trueColor) {
		register int x, y;

		for (y = im->cy1; y <= im->cy2; y++) {
			for (x = im->cx1; x <= im->cx2; x++) {
				c = gdImageTrueColorPixel(im, x, y);
				if ( (d = callback(im, c)) != c) {
					gdImageSetPixel(im, x, y, d);
					n++;
				}
			}
		}
	} else { /* palette */
		int *sarr, *darr;
		int k, len = 0;

		sarr = (int *)gdCalloc(im->colorsTotal, sizeof(int));
		if (!sarr) {
			return -1;
		}
		for (c = 0; c < im->colorsTotal; c++) {
			if (!im->open[c]) {
				sarr[len++] = c;
			}
		}
		darr = (int *)gdCalloc(len, sizeof(int));
		if (!darr) {
			gdFree(sarr);
			return -1;
		}
		for (k = 0; k < len; k++) {
			darr[k] = callback(im, sarr[k]);
		}
		n = gdImageColorReplaceArray(im, k, sarr, darr);
		gdFree(darr);
		gdFree(sarr);
	}
	return n;
}

/* end of line clipping code */

BGD_DECLARE(void) gdImageAABlend (gdImagePtr im)
{
	/* NO-OP, kept for library compatibility. */
	(void)im;
}

static void gdImageAALine (gdImagePtr im, int x1, int y1, int x2, int y2, int col);

static void dashedSet (gdImagePtr im, int x, int y, int color,
					   int *onP, int *dashStepP, int wid, int vert);

BGD_DECLARE(void) gdImageDashedLine (gdImagePtr im, int x1, int y1, int x2, int y2, int color)
{
	int dx, dy, incr1, incr2, d, x, y, xend, yend, xdirflag, ydirflag;
	int dashStep = 0;
	int on = 1;
	int wid;
	int vert;
	int thick = im->thick;

	dx = abs (x2 - x1);
	dy = abs (y2 - y1);
	if (dy <= dx) {
		/* More-or-less horizontal. use wid for vertical stroke */
		/* 2.0.12: Michael Schwartz: divide rather than multiply;
		   TBB: but watch out for /0! */
		double as = sin (atan2 (dy, dx));
		if (as != 0) {
			wid = thick / as;
		} else {
			wid = 1;
		}
		vert = 1;

		d = 2 * dy - dx;
		incr1 = 2 * dy;
		incr2 = 2 * (dy - dx);
		if (x1 > x2) {
			x = x2;
			y = y2;
			ydirflag = (-1);
			xend = x1;
		} else {
			x = x1;
			y = y1;
			ydirflag = 1;
			xend = x2;
		}
		dashedSet (im, x, y, color, &on, &dashStep, wid, vert);
		if (((y2 - y1) * ydirflag) > 0) {
			while (x < xend) {
				x++;
				if (d < 0) {
					d += incr1;
				} else {
					y++;
					d += incr2;
				}
				dashedSet (im, x, y, color, &on, &dashStep, wid, vert);
			}
		} else {
			while (x < xend) {
				x++;
				if (d < 0) {
					d += incr1;
				} else {
					y--;
					d += incr2;
				}
				dashedSet (im, x, y, color, &on, &dashStep, wid, vert);
			}
		}
	} else {
		/* 2.0.12: Michael Schwartz: divide rather than multiply;
		   TBB: but watch out for /0! */
		double as = sin (atan2 (dy, dx));
		if (as != 0) {
			wid = thick / as;
		} else {
			wid = 1;
		}
		vert = 0;

		d = 2 * dx - dy;
		incr1 = 2 * dx;
		incr2 = 2 * (dx - dy);
		if (y1 > y2) {
			y = y2;
			x = x2;
			yend = y1;
			xdirflag = (-1);
		} else {
			y = y1;
			x = x1;
			yend = y2;
			xdirflag = 1;
		}
		dashedSet (im, x, y, color, &on, &dashStep, wid, vert);
		if (((x2 - x1) * xdirflag) > 0) {
			while (y < yend) {
				y++;
				if (d < 0) {
					d += incr1;
				} else {
					x++;
					d += incr2;
				}
				dashedSet (im, x, y, color, &on, &dashStep, wid, vert);
			}
		} else {
			while (y < yend) {
				y++;
				if (d < 0) {
					d += incr1;
				} else {
					x--;
					d += incr2;
				}
				dashedSet (im, x, y, color, &on, &dashStep, wid, vert);
			}
		}
	}
}

static void
dashedSet (gdImagePtr im, int x, int y, int color,
		   int *onP, int *dashStepP, int wid, int vert)
{
	int dashStep = *dashStepP;
	int on = *onP;
	int w, wstart;

	dashStep++;
	if (dashStep == gdDashSize) {
		dashStep = 0;
		on = !on;
	}
	if (on) {
		if (vert) {
			wstart = y - wid / 2;
			for (w = wstart; w < wstart + wid; w++)
				gdImageSetPixel (im, x, w, color);
		} else {
			wstart = x - wid / 2;
			for (w = wstart; w < wstart + wid; w++)
				gdImageSetPixel (im, w, y, color);
		}
	}
	*dashStepP = dashStep;
	*onP = on;
}

BGD_DECLARE(int) gdImageBoundsSafe (gdImagePtr im, int x, int y)
{
	return gdImageBoundsSafeMacro (im, x, y);
}

BGD_DECLARE(void) gdImageChar (gdImagePtr im, gdFontPtr f, int x, int y, int c, int color)
{
	int cx, cy;
	int px, py;
	int fline;
	cx = 0;
	cy = 0;
#ifdef CHARSET_EBCDIC
	c = ASC (c);
#endif /*CHARSET_EBCDIC */
	if ((c < f->offset) || (c >= (f->offset + f->nchars))) {
		return;
	}
	fline = (c - f->offset) * f->h * f->w;
	for (py = y; (py < (y + f->h)); py++) {
		for (px = x; (px < (x + f->w)); px++) {
			if (f->data[fline + cy * f->w + cx]) {
				gdImageSetPixel (im, px, py, color);
			}
			cx++;
		}
		cx = 0;
		cy++;
	}
}

BGD_DECLARE(void) gdImageCharUp (gdImagePtr im, gdFontPtr f, int x, int y, int c, int color)
{
	int cx, cy;
	int px, py;
	int fline;
	cx = 0;
	cy = 0;
#ifdef CHARSET_EBCDIC
	c = ASC (c);
#endif /*CHARSET_EBCDIC */
	if ((c < f->offset) || (c >= (f->offset + f->nchars))) {
		return;
	}
	fline = (c - f->offset) * f->h * f->w;
	for (py = y; (py > (y - f->w)); py--) {
		for (px = x; (px < (x + f->h)); px++) {
			if (f->data[fline + cy * f->w + cx]) {
				gdImageSetPixel (im, px, py, color);
			}
			cy++;
		}
		cy = 0;
		cx++;
	}
}

BGD_DECLARE(void) gdImageString (gdImagePtr im, gdFontPtr f,
								 int x, int y, unsigned char *s, int color)
{
	int i;
	int l;
	l = strlen ((char *) s);
	for (i = 0; (i < l); i++) {
		gdImageChar (im, f, x, y, s[i], color);
		x += f->w;
	}
}

BGD_DECLARE(void) gdImageStringUp (gdImagePtr im, gdFontPtr f,
								   int x, int y, unsigned char *s, int color)
{
	int i;
	int l;
	l = strlen ((char *) s);
	for (i = 0; (i < l); i++) {
		gdImageCharUp (im, f, x, y, s[i], color);
		y -= f->w;
	}
}

static int strlen16 (unsigned short *s);

BGD_DECLARE(void) gdImageString16 (gdImagePtr im, gdFontPtr f,
								   int x, int y, unsigned short *s, int color)
{
	int i;
	int l;
	l = strlen16 (s);
	for (i = 0; (i < l); i++) {
		gdImageChar (im, f, x, y, s[i], color);
		x += f->w;
	}
}

BGD_DECLARE(void) gdImageStringUp16 (gdImagePtr im, gdFontPtr f,
									 int x, int y, unsigned short *s, int color)
{
	int i;
	int l;
	l = strlen16 (s);
	for (i = 0; (i < l); i++) {
		gdImageCharUp (im, f, x, y, s[i], color);
		y -= f->w;
	}
}

static int
strlen16 (unsigned short *s)
{
	int len = 0;
	while (*s) {
		s++;
		len++;
	}
	return len;
}

#ifndef HAVE_LSQRT
/* If you don't have a nice square root function for longs, you can use
   ** this hack
 */
long
lsqrt (long n)
{
	long result = (long) sqrt ((double) n);
	return result;
}
#endif

/* s and e are integers modulo 360 (degrees), with 0 degrees
   being the rightmost extreme and degrees changing clockwise.
   cx and cy are the center in pixels; w and h are the horizontal
   and vertical diameter in pixels. Nice interface, but slow.
   See gd_arc_f_buggy.c for a better version that doesn't
   seem to be bug-free yet. */

BGD_DECLARE(void) gdImageArc (gdImagePtr im, int cx, int cy, int w, int h, int s, int e,
							  int color)
{
	gdImageFilledArc (im, cx, cy, w, h, s, e, color, gdNoFill);
}

BGD_DECLARE(void) gdImageFilledArc (gdImagePtr im, int cx, int cy, int w, int h, int s, int e,
									int color, int style)
{
	gdPoint pts[3];
	int i;
	int lx = 0, ly = 0;
	int fx = 0, fy = 0;

	if ((s % 360)  == (e % 360)) {
		s = 0;
		e = 360;
	} else {
		if (s > 360) {
			s = s % 360;
		}

		if (e > 360) {
			e = e % 360;
		}

		while (s < 0) {
			s += 360;
		}

		while (e < s) {
			e += 360;
		}

		if (s == e) {
			s = 0;
			e = 360;
		}
	}

	for (i = s; (i <= e); i++) {
		int x, y;
		x = ((long) gdCosT[i % 360] * (long) w / (2 * 1024)) + cx;
		y = ((long) gdSinT[i % 360] * (long) h / (2 * 1024)) + cy;
		if (i != s) {
			if (!(style & gdChord)) {
				if (style & gdNoFill) {
					gdImageLine (im, lx, ly, x, y, color);
				} else {
					/* This is expensive! */
					pts[0].x = lx;
					pts[0].y = ly;
					pts[1].x = x;
					pts[1].y = y;
					pts[2].x = cx;
					pts[2].y = cy;
					gdImageFilledPolygon (im, pts, 3, color);
				}
			}
		} else {
			fx = x;
			fy = y;
		}
		lx = x;
		ly = y;
	}
	if (style & gdChord) {
		if (style & gdNoFill) {
			if (style & gdEdged) {
				gdImageLine (im, cx, cy, lx, ly, color);
				gdImageLine (im, cx, cy, fx, fy, color);
			}
			gdImageLine (im, fx, fy, lx, ly, color);
		} else {
			pts[0].x = fx;
			pts[0].y = fy;
			pts[1].x = lx;
			pts[1].y = ly;
			pts[2].x = cx;
			pts[2].y = cy;
			gdImageFilledPolygon (im, pts, 3, color);
		}
	} else {
		if (style & gdNoFill) {
			if (style & gdEdged) {
				gdImageLine (im, cx, cy, lx, ly, color);
				gdImageLine (im, cx, cy, fx, fy, color);
			}
		}
	}
}

BGD_DECLARE(void) gdImageEllipse(gdImagePtr im, int mx, int my, int w, int h, int c)
{
	int x=0,mx1=0,mx2=0,my1=0,my2=0;
	long aq,bq,dx,dy,r,rx,ry,a,b;

	a=w>>1;
	b=h>>1;
	gdImageSetPixel(im,mx+a, my, c);
	gdImageSetPixel(im,mx-a, my, c);
	mx1 = mx-a;
	my1 = my;
	mx2 = mx+a;
	my2 = my;

	aq = a * a;
	bq = b * b;
	dx = aq << 1;
	dy = bq << 1;
	r  = a * bq;
	rx = r << 1;
	ry = 0;
	x = a;
	while (x > 0) {
		if (r > 0) {
			my1++;
			my2--;
			ry +=dx;
			r  -=ry;
		}
		if (r <= 0) {
			x--;
			mx1++;
			mx2--;
			rx -=dy;
			r  +=rx;
		}
		gdImageSetPixel(im,mx1, my1, c);
		gdImageSetPixel(im,mx1, my2, c);
		gdImageSetPixel(im,mx2, my1, c);
		gdImageSetPixel(im,mx2, my2, c);
	}
}


BGD_DECLARE(void) gdImageFilledEllipse (gdImagePtr im, int mx, int my, int w, int h, int c)
{
	int x=0,mx1=0,mx2=0,my1=0,my2=0;
	long aq,bq,dx,dy,r,rx,ry,a,b;
	int i;
	int old_y2;

	a=w>>1;
	b=h>>1;

	for (x = mx-a; x <= mx+a; x++) {
		gdImageSetPixel(im, x, my, c);
	}

	mx1 = mx-a;
	my1 = my;
	mx2 = mx+a;
	my2 = my;

	aq = a * a;
	bq = b * b;
	dx = aq << 1;
	dy = bq << 1;
	r  = a * bq;
	rx = r << 1;
	ry = 0;
	x = a;
	old_y2=-2;
	while (x > 0) {
		if (r > 0) {
			my1++;
			my2--;
			ry +=dx;
			r  -=ry;
		}
		if (r <= 0) {
			x--;
			mx1++;
			mx2--;
			rx -=dy;
			r  +=rx;
		}
		if(old_y2!=my2) {
			for(i=mx1; i<=mx2; i++) {
				gdImageSetPixel(im,i,my1,c);
			}
		}
		if(old_y2!=my2) {
			for(i=mx1; i<=mx2; i++) {
				gdImageSetPixel(im,i,my2,c);
			}
		}
		old_y2 = my2;
	}
}

BGD_DECLARE(void) gdImageFillToBorder (gdImagePtr im, int x, int y, int border, int color)
{
	int lastBorder;
	/* Seek left */
	int leftLimit, rightLimit;
	int i;
	int restoreAlphaBleding;

	if (border < 0) {
		/* Refuse to fill to a non-solid border */
		return;
	}

	leftLimit = (-1);

	restoreAlphaBleding = im->alphaBlendingFlag;
	im->alphaBlendingFlag = 0;

	for (i = x; (i >= 0); i--) {
		if (gdImageGetPixel (im, i, y) == border) {
			break;
		}
		gdImageSetPixel (im, i, y, color);
		leftLimit = i;
	}
	if (leftLimit == (-1)) {
		im->alphaBlendingFlag = restoreAlphaBleding;
		return;
	}
	/* Seek right */
	rightLimit = x;
	for (i = (x + 1); (i < im->sx); i++) {
		if (gdImageGetPixel (im, i, y) == border) {
			break;
		}
		gdImageSetPixel (im, i, y, color);
		rightLimit = i;
	}
	/* Look at lines above and below and start paints */
	/* Above */
	if (y > 0) {
		lastBorder = 1;
		for (i = leftLimit; (i <= rightLimit); i++) {
			int c;
			c = gdImageGetPixel (im, i, y - 1);
			if (lastBorder) {
				if ((c != border) && (c != color)) {
					gdImageFillToBorder (im, i, y - 1, border, color);
					lastBorder = 0;
				}
			} else if ((c == border) || (c == color)) {
				lastBorder = 1;
			}
		}
	}
	/* Below */
	if (y < ((im->sy) - 1)) {
		lastBorder = 1;
		for (i = leftLimit; (i <= rightLimit); i++) {
			int c = gdImageGetPixel (im, i, y + 1);
			if (lastBorder) {
				if ((c != border) && (c != color)) {
					gdImageFillToBorder (im, i, y + 1, border, color);
					lastBorder = 0;
				}
			} else if ((c == border) || (c == color)) {
				lastBorder = 1;
			}
		}
	}
	im->alphaBlendingFlag = restoreAlphaBleding;
}

/*
 * set the pixel at (x,y) and its 4-connected neighbors
 * with the same pixel value to the new pixel value nc (new color).
 * A 4-connected neighbor:  pixel above, below, left, or right of a pixel.
 * ideas from comp.graphics discussions.
 * For tiled fill, the use of a flag buffer is mandatory. As the tile image can
 * contain the same color as the color to fill. To do not bloat normal filling
 * code I added a 2nd private function.
 */

static int gdImageTileGet (gdImagePtr im, int x, int y)
{
	int srcx, srcy;
	int tileColor,p;
	if (!im->tile) {
		return -1;
	}
	srcx = x % gdImageSX(im->tile);
	srcy = y % gdImageSY(im->tile);
	p = gdImageGetPixel(im->tile, srcx, srcy);
	if (p == im->tile->transparent) {
		tileColor = im->transparent;
	} else if (im->trueColor) {
		if (im->tile->trueColor) {
			tileColor = p;
		} else {
			tileColor = gdTrueColorAlpha( gdImageRed(im->tile,p), gdImageGreen(im->tile,p), gdImageBlue (im->tile,p), gdImageAlpha (im->tile,p));
		}
	} else {
		if (im->tile->trueColor) {
			tileColor = gdImageColorResolveAlpha(im, gdTrueColorGetRed (p), gdTrueColorGetGreen (p), gdTrueColorGetBlue (p), gdTrueColorGetAlpha (p));
		} else {
			tileColor = gdImageColorResolveAlpha(im, gdImageRed (im->tile,p), gdImageGreen (im->tile,p), gdImageBlue (im->tile,p), gdImageAlpha (im->tile,p));
		}
	}
	return tileColor;
}



/* horizontal segment of scan line y */
struct seg {
	int y, xl, xr, dy;
};

/* max depth of stack */
#define FILL_MAX ((int)(im->sy*im->sx)/4)
#define FILL_PUSH(Y, XL, XR, DY) \
	if (sp<stack+FILL_MAX && Y+(DY)>=0 && Y+(DY)<wy2) \
	{sp->y = Y; sp->xl = XL; sp->xr = XR; sp->dy = DY; sp++;}

#define FILL_POP(Y, XL, XR, DY) \
	{sp--; Y = sp->y+(DY = sp->dy); XL = sp->xl; XR = sp->xr;}

static void _gdImageFillTiled(gdImagePtr im, int x, int y, int nc);
BGD_DECLARE(void) gdImageFill(gdImagePtr im, int x, int y, int nc)
{
	int l, x1, x2, dy;
	int oc;   /* old pixel value */
	int wx2,wy2;

	int alphablending_bak;

	/* stack of filled segments */
	/* struct seg stack[FILL_MAX],*sp = stack; */
	struct seg *stack;
	struct seg *sp;

	if (!im->trueColor && nc > (im->colorsTotal - 1)) {
		return;
	}

	alphablending_bak = im->alphaBlendingFlag;
	im->alphaBlendingFlag = 0;

	if (nc==gdTiled) {
		_gdImageFillTiled(im,x,y,nc);
		im->alphaBlendingFlag = alphablending_bak;
		return;
	}

	wx2=im->sx;
	wy2=im->sy;
	oc = gdImageGetPixel(im, x, y);
	if (oc==nc || x<0 || x>wx2 || y<0 || y>wy2) {
		im->alphaBlendingFlag = alphablending_bak;
		return;
	}

	/* Do not use the 4 neighbors implementation with
	* small images
	*/
	if (im->sx < 4) {
		int ix = x, iy = y, c;
		do {
			do {
				c = gdImageGetPixel(im, ix, iy);
				if (c != oc) {
					goto done;
				}
				gdImageSetPixel(im, ix, iy, nc);
			} while(ix++ < (im->sx -1));
			ix = x;
		} while(iy++ < (im->sy -1));
		goto done;
	}

	if(overflow2(im->sy, im->sx)) {
		return;
	}

	if(overflow2(sizeof(struct seg), ((im->sy * im->sx) / 4))) {
		return;
	}

	stack = (struct seg *)gdMalloc(sizeof(struct seg) * ((int)(im->sy*im->sx)/4));
	if (!stack) {
		return;
	}
	sp = stack;

	/* required! */
	FILL_PUSH(y,x,x,1);
	/* seed segment (popped 1st) */
	FILL_PUSH(y+1, x, x, -1);
	while (sp>stack) {
		FILL_POP(y, x1, x2, dy);

		for (x=x1; x>=0 && gdImageGetPixel(im,x, y)==oc; x--) {
			gdImageSetPixel(im,x, y, nc);
		}
		if (x>=x1) {
			goto skip;
		}
		l = x+1;

		/* leak on left? */
		if (l<x1) {
			FILL_PUSH(y, l, x1-1, -dy);
		}
		x = x1+1;
		do {
			for (; x<=wx2 && gdImageGetPixel(im,x, y)==oc; x++) {
				gdImageSetPixel(im, x, y, nc);
			}
			FILL_PUSH(y, l, x-1, dy);
			/* leak on right? */
			if (x>x2+1) {
				FILL_PUSH(y, x2+1, x-1, -dy);
			}
skip:
			for (x++; x<=x2 && (gdImageGetPixel(im, x, y)!=oc); x++);

			l = x;
		} while (x<=x2);
	}

	gdFree(stack);

done:
	im->alphaBlendingFlag = alphablending_bak;
}

static void _gdImageFillTiled(gdImagePtr im, int x, int y, int nc)
{
	int l, x1, x2, dy;
	int oc;   /* old pixel value */
	int wx2,wy2;
	/* stack of filled segments */
	struct seg *stack;
	struct seg *sp;
	char *pts;

	if (!im->tile) {
		return;
	}

	wx2=im->sx;
	wy2=im->sy;

	if(overflow2(im->sy, im->sx)) {
		return;
	}

	if(overflow2(sizeof(struct seg), ((im->sy * im->sx) / 4))) {
		return;
	}

	pts = (char *) gdCalloc(im->sy * im->sx, sizeof(char));
	if (!pts) {
		return;
	}

	stack = (struct seg *)gdMalloc(sizeof(struct seg) * ((int)(im->sy*im->sx)/4));
	if (!stack) {
		gdFree(pts);
		return;
	}
	sp = stack;

	oc = gdImageGetPixel(im, x, y);

	/* required! */
	FILL_PUSH(y,x,x,1);
	/* seed segment (popped 1st) */
	FILL_PUSH(y+1, x, x, -1);
	while (sp>stack) {
		FILL_POP(y, x1, x2, dy);
		for (x=x1; x>=0 && (!pts[y + x*wx2] && gdImageGetPixel(im,x,y)==oc); x--) {
			nc = gdImageTileGet(im,x,y);
			pts[y + x*wx2]=1;
			gdImageSetPixel(im,x, y, nc);
		}
		if (x>=x1) {
			goto skip;
		}
		l = x+1;

		/* leak on left? */
		if (l<x1) {
			FILL_PUSH(y, l, x1-1, -dy);
		}
		x = x1+1;
		do {
			for (; x<wx2 && (!pts[y + x*wx2] && gdImageGetPixel(im,x, y)==oc) ; x++) {
				if (pts[y + x*wx2]) {
					/* we should never be here */
					break;
				}
				nc = gdImageTileGet(im,x,y);
				pts[y + x*wx2]=1;
				gdImageSetPixel(im, x, y, nc);
			}
			FILL_PUSH(y, l, x-1, dy);
			/* leak on right? */
			if (x>x2+1) {
				FILL_PUSH(y, x2+1, x-1, -dy);
			}
skip:
			for (x++; x<=x2 && (pts[y + x*wx2] || gdImageGetPixel(im,x, y)!=oc); x++);
			l = x;
		} while (x<=x2);
	}

	gdFree(pts);
	gdFree(stack);
}

BGD_DECLARE(void) gdImageRectangle (gdImagePtr im, int x1, int y1, int x2, int y2, int color)
{
	int thick = im->thick;

	if (x1 == x2 && y1 == y2 && thick == 1) {
		gdImageSetPixel(im, x1, y1, color);
		return;
	}

	if (y2 < y1) {
		int t;
		t = y1;
		y1 = y2;
		y2 = t;

		t = x1;
		x1 = x2;
		x2 = t;
	}

	if (thick > 1) {
		int cx, cy, x1ul, y1ul, x2lr, y2lr;
		int half = thick >> 1;
		x1ul = x1 - half;
		y1ul = y1 - half;

		x2lr = x2 + half;
		y2lr = y2 + half;

		cy = y1ul + thick;
		while (cy-- > y1ul) {
			cx = x1ul - 1;
			while (cx++ < x2lr) {
				gdImageSetPixel(im, cx, cy, color);
			}
		}

		cy = y2lr - thick;
		while (cy++ < y2lr) {
			cx = x1ul - 1;
			while (cx++ < x2lr) {
				gdImageSetPixel(im, cx, cy, color);
			}
		}

		cy = y1ul + thick - 1;
		while (cy++ < y2lr -thick) {
			cx = x1ul - 1;
			while (cx++ < x1ul + thick) {
				gdImageSetPixel(im, cx, cy, color);
			}
		}

		cy = y1ul + thick - 1;
		while (cy++ < y2lr -thick) {
			cx = x2lr - thick - 1;
			while (cx++ < x2lr) {
				gdImageSetPixel(im, cx, cy, color);
			}
		}

		return;
	} else {
		gdImageLine(im, x1, y1, x2, y1, color);
		gdImageLine(im, x1, y2, x2, y2, color);
		gdImageLine(im, x1, y1 + 1, x1, y2 - 1, color);
		gdImageLine(im, x2, y1 + 1, x2, y2 - 1, color);
	}
}

BGD_DECLARE(gdImagePtr) gdImageClone (gdImagePtr src) {
	gdImagePtr dst;
	register int i, x;

	if (src->trueColor) {
		dst = gdImageCreateTrueColor(src->sx , src->sy);
	} else {
		dst = gdImageCreate(src->sx , src->sy);
	}

	if (dst == NULL) {
		return NULL;
	}

	if (src->trueColor == 0) {
		dst->colorsTotal = src->colorsTotal;
		for (i = 0; i < gdMaxColors; i++) {
			dst->red[i]   = src->red[i];
			dst->green[i] = src->green[i];
			dst->blue[i]  = src->blue[i];
			dst->alpha[i] = src->alpha[i];
			dst->open[i]  = src->open[i];
		}
		for (i = 0; i < src->sy; i++) {
			for (x = 0; x < src->sx; x++) {
				dst->pixels[i][x] = src->pixels[i][x];
			}
		}
	} else {
		for (i = 0; i < src->sy; i++) {
			for (x = 0; x < src->sx; x++) {
				dst->tpixels[i][x] = src->tpixels[i][x];
			}
		}
	}

	if (src->styleLength > 0) {
		dst->styleLength = src->styleLength;
		dst->stylePos    = src->stylePos;
		for (i = 0; i < src->styleLength; i++) {
			dst->style[i] = src->style[i];
		}
	}

	dst->interlace   = src->interlace;

	dst->alphaBlendingFlag = src->alphaBlendingFlag;
	dst->saveAlphaFlag     = src->saveAlphaFlag;
	dst->AA                = src->AA;
	dst->AA_color          = src->AA_color;
	dst->AA_dont_blend     = src->AA_dont_blend;

	dst->cx1 = src->cx1;
	dst->cy1 = src->cy1;
	dst->cx2 = src->cx2;
	dst->cy2 = src->cy2;

	dst->res_x = src->res_x;
	dst->res_y = src->res_x;

	dst->paletteQuantizationMethod     = src->paletteQuantizationMethod;
	dst->paletteQuantizationSpeed      = src->paletteQuantizationSpeed;
	dst->paletteQuantizationMinQuality = src->paletteQuantizationMinQuality;
	dst->paletteQuantizationMinQuality = src->paletteQuantizationMinQuality;

	dst->interpolation_id = src->interpolation_id;
	dst->interpolation    = src->interpolation;

	if (src->brush) {
		dst->brush = gdImageClone(src->brush);
	}

	if (src->tile) {
		dst->tile = gdImageClone(src->tile);
	}

	if (src->style) {
		gdImageSetStyle(dst, src->style, src->styleLength);
	}

	for (i = 0; i < gdMaxColors; i++) {
		dst->brushColorMap[i] = src->brushColorMap[i];
		dst->tileColorMap[i] = src->tileColorMap[i];
	}

	if (src->polyAllocated > 0) {
		dst->polyAllocated = src->polyAllocated;
		for (i = 0; i < src->polyAllocated; i++) {
			dst->polyInts[i] = src->polyInts[i];
		}
	}

	return dst;
}

BGD_DECLARE(void) gdImageCopy (gdImagePtr dst, gdImagePtr src, int dstX, int dstY, int srcX,
							   int srcY, int w, int h)
{
	int c;
	int x, y;
	int tox, toy;
	int i;
	int colorMap[gdMaxColors];

	if (dst->trueColor) {
		/* 2.0: much easier when the destination is truecolor. */
		/* 2.0.10: needs a transparent-index check that is still valid if
		 *          * the source is not truecolor. Thanks to Frank Warmerdam.
		 */

		if (src->trueColor) {
			for (y = 0; (y < h); y++) {
				for (x = 0; (x < w); x++) {
					int c = gdImageGetTrueColorPixel (src, srcX + x, srcY + y);
					if (c != src->transparent) {
						gdImageSetPixel (dst, dstX + x, dstY + y, c);
					}
				}
			}
		} else {
			/* source is palette based */
			for (y = 0; (y < h); y++) {
				for (x = 0; (x < w); x++) {
					int c = gdImageGetPixel (src, srcX + x, srcY + y);
					if (c != src->transparent) {
						gdImageSetPixel(dst, dstX + x, dstY + y, gdTrueColorAlpha(src->red[c], src->green[c], src->blue[c], src->alpha[c]));
					}
				}
			}
		}
		return;
	}

	for (i = 0; (i < gdMaxColors); i++) {
		colorMap[i] = (-1);
	}
	toy = dstY;
	for (y = srcY; (y < (srcY + h)); y++) {
		tox = dstX;
		for (x = srcX; (x < (srcX + w)); x++) {
			int nc;
			int mapTo;
			c = gdImageGetPixel (src, x, y);
			/* Added 7/24/95: support transparent copies */
			if (gdImageGetTransparent (src) == c) {
				tox++;
				continue;
			}
			/* Have we established a mapping for this color? */
			if (src->trueColor) {
				/* 2.05: remap to the palette available in the
				 destination image. This is slow and
				 works badly, but it beats crashing! Thanks
				 to Padhrig McCarthy. */
				mapTo = gdImageColorResolveAlpha (dst,
				                                  gdTrueColorGetRed (c),
				                                  gdTrueColorGetGreen (c),
				                                  gdTrueColorGetBlue (c),
				                                  gdTrueColorGetAlpha (c));
			} else if (colorMap[c] == (-1)) {
				/* If it's the same image, mapping is trivial */
				if (dst == src) {
					nc = c;
				} else {
					/* Get best match possible. This
					   function never returns error. */
					nc = gdImageColorResolveAlpha (dst,
					                               src->red[c], src->green[c],
					                               src->blue[c], src->alpha[c]);
				}
				colorMap[c] = nc;
				mapTo = colorMap[c];
			} else {
				mapTo = colorMap[c];
			}
			gdImageSetPixel (dst, tox, toy, mapTo);
			tox++;
		}
		toy++;
	}
}

/* This function is a substitute for real alpha channel operations,
   so it doesn't pay attention to the alpha channel. */
BGD_DECLARE(void) gdImageCopyMerge (gdImagePtr dst, gdImagePtr src, int dstX, int dstY,
									int srcX, int srcY, int w, int h, int pct)
{

	int c, dc;
	int x, y;
	int tox, toy;
	int ncR, ncG, ncB;
	toy = dstY;
	for (y = srcY; (y < (srcY + h)); y++) {
		tox = dstX;
		for (x = srcX; (x < (srcX + w)); x++) {
			int nc;
			c = gdImageGetPixel (src, x, y);
			/* Added 7/24/95: support transparent copies */
			if (gdImageGetTransparent (src) == c) {
				tox++;
				continue;
			}
			/* If it's the same image, mapping is trivial */
			if (dst == src) {
				nc = c;
			} else {
				dc = gdImageGetPixel (dst, tox, toy);

				ncR = gdImageRed (src, c) * (pct / 100.0)
				      + gdImageRed (dst, dc) * ((100 - pct) / 100.0);
				ncG = gdImageGreen (src, c) * (pct / 100.0)
				      + gdImageGreen (dst, dc) * ((100 - pct) / 100.0);
				ncB = gdImageBlue (src, c) * (pct / 100.0)
				      + gdImageBlue (dst, dc) * ((100 - pct) / 100.0);

				/* Find a reasonable color */
				nc = gdImageColorResolve (dst, ncR, ncG, ncB);
			}
			gdImageSetPixel (dst, tox, toy, nc);
			tox++;
		}
		toy++;
	}
}

/* This function is a substitute for real alpha channel operations,
   so it doesn't pay attention to the alpha channel. */
BGD_DECLARE(void) gdImageCopyMergeGray (gdImagePtr dst, gdImagePtr src, int dstX, int dstY,
										int srcX, int srcY, int w, int h, int pct)
{

	int c, dc;
	int x, y;
	int tox, toy;
	int ncR, ncG, ncB;
	float g;
	toy = dstY;
	for (y = srcY; (y < (srcY + h)); y++) {
		tox = dstX;
		for (x = srcX; (x < (srcX + w)); x++) {
			int nc;
			c = gdImageGetPixel (src, x, y);
			/* Added 7/24/95: support transparent copies */
			if (gdImageGetTransparent (src) == c) {
				tox++;
				continue;
			}
			/*
			 * If it's the same image, mapping is NOT trivial since we
			 * merge with greyscale target, but if pct is 100, the grey
			 * value is not used, so it becomes trivial. pjw 2.0.12.
			 */
			if (dst == src && pct == 100) {
				nc = c;
			} else {
				dc = gdImageGetPixel (dst, tox, toy);
				g = 0.29900 * gdImageRed(dst, dc)
				    + 0.58700 * gdImageGreen(dst, dc) + 0.11400 * gdImageBlue(dst, dc);

				ncR = gdImageRed (src, c) * (pct / 100.0)
				      + g * ((100 - pct) / 100.0);
				ncG = gdImageGreen (src, c) * (pct / 100.0)
				      + g * ((100 - pct) / 100.0);
				ncB = gdImageBlue (src, c) * (pct / 100.0)
				      + g * ((100 - pct) / 100.0);

				/* First look for an exact match */
				nc = gdImageColorExact (dst, ncR, ncG, ncB);
				if (nc == (-1)) {
					/* No, so try to allocate it */
					nc = gdImageColorAllocate (dst, ncR, ncG, ncB);
					/* If we're out of colors, go for the
					   closest color */
					if (nc == (-1)) {
						nc = gdImageColorClosest (dst, ncR, ncG, ncB);
					}
				}
			}
			gdImageSetPixel (dst, tox, toy, nc);
			tox++;
		}
		toy++;
	}
}

BGD_DECLARE(void) gdImageCopyResized (gdImagePtr dst, gdImagePtr src, int dstX, int dstY,
									  int srcX, int srcY, int dstW, int dstH, int srcW,
									  int srcH)
{
	int c;
	int x, y;
	int tox, toy;
	int ydest;
	int i;
	int colorMap[gdMaxColors];
	/* Stretch vectors */
	int *stx;
	int *sty;
	/* We only need to use floating point to determine the correct
	   stretch vector for one line's worth. */
	if (overflow2(sizeof (int), srcW)) {
		return;
	}
	if (overflow2(sizeof (int), srcH)) {
		return;
	}
	stx = (int *) gdMalloc (sizeof (int) * srcW);
	if (!stx) {
		return;
	}

	sty = (int *) gdMalloc (sizeof (int) * srcH);
	if (!sty) {
		gdFree(stx);
		return;
	}

	/* Fixed by Mao Morimoto 2.0.16 */
	for (i = 0; (i < srcW); i++) {
		stx[i] = dstW * (i + 1) / srcW - dstW * i / srcW;
	}
	for (i = 0; (i < srcH); i++) {
		sty[i] = dstH * (i + 1) / srcH - dstH * i / srcH;
	}
	for (i = 0; (i < gdMaxColors); i++) {
		colorMap[i] = (-1);
	}
	toy = dstY;
	for (y = srcY; (y < (srcY + srcH)); y++) {
		for (ydest = 0; (ydest < sty[y - srcY]); ydest++) {
			tox = dstX;
			for (x = srcX; (x < (srcX + srcW)); x++) {
				int nc = 0;
				int mapTo;
				if (!stx[x - srcX]) {
					continue;
				}
				if (dst->trueColor) {
					/* 2.0.9: Thorben Kundinger: Maybe the source image is not
					   a truecolor image */
					if (!src->trueColor) {
						int tmp = gdImageGetPixel (src, x, y);
						mapTo = gdImageGetTrueColorPixel (src, x, y);
						if (gdImageGetTransparent (src) == tmp) {
							/* 2.0.21, TK: not tox++ */
							tox += stx[x - srcX];
							continue;
						}
					} else {
						/* TK: old code follows */
						mapTo = gdImageGetTrueColorPixel (src, x, y);
						/* Added 7/24/95: support transparent copies */
						if (gdImageGetTransparent (src) == mapTo) {
							/* 2.0.21, TK: not tox++ */
							tox += stx[x - srcX];
							continue;
						}
					}
				} else {
					c = gdImageGetPixel (src, x, y);
					/* Added 7/24/95: support transparent copies */
					if (gdImageGetTransparent (src) == c) {
						tox += stx[x - srcX];
						continue;
					}
					if (src->trueColor) {
						/* Remap to the palette available in the
						   destination image. This is slow and
						   works badly. */
						mapTo = gdImageColorResolveAlpha (dst,
						                                  gdTrueColorGetRed (c),
						                                  gdTrueColorGetGreen
						                                  (c),
						                                  gdTrueColorGetBlue
						                                  (c),
						                                  gdTrueColorGetAlpha
						                                  (c));
					} else {
						/* Have we established a mapping for this color? */
						if (colorMap[c] == (-1)) {
							/* If it's the same image, mapping is trivial */
							if (dst == src) {
								nc = c;
							} else {
								/* Find or create the best match */
								/* 2.0.5: can't use gdTrueColorGetRed, etc with palette */
								nc = gdImageColorResolveAlpha (dst,
								                               gdImageRed (src,
								                                       c),
								                               gdImageGreen
								                               (src, c),
								                               gdImageBlue (src,
								                                       c),
								                               gdImageAlpha
								                               (src, c));
							}
							colorMap[c] = nc;
						}
						mapTo = colorMap[c];
					}
				}
				for (i = 0; (i < stx[x - srcX]); i++) {
					gdImageSetPixel (dst, tox, toy, mapTo);
					tox++;
				}
			}
			toy++;
		}
	}
	gdFree (stx);
	gdFree (sty);
}

/* gd 2.0.8: gdImageCopyRotated is added. Source
	is a rectangle, with its upper left corner at
	srcX and srcY. Destination is the *center* of
		the rotated copy. Angle is in degrees, same as
		gdImageArc. Floating point destination center
	coordinates allow accurate rotation of
	objects of odd-numbered width or height. */

BGD_DECLARE(void) gdImageCopyRotated (gdImagePtr dst,
									  gdImagePtr src,
									  double dstX, double dstY,
									  int srcX, int srcY,
									  int srcWidth, int srcHeight, int angle)
{
	double dx, dy;
	double radius = sqrt (srcWidth * srcWidth + srcHeight * srcHeight);
	double aCos = cos (angle * .0174532925);
	double aSin = sin (angle * .0174532925);
	double scX = srcX + ((double) srcWidth) / 2;
	double scY = srcY + ((double) srcHeight) / 2;
	int cmap[gdMaxColors];
	int i;

	/*
		 2.0.34: transparency preservation. The transparentness of
		 the transparent color is more important than its hue.
	*/
	if (src->transparent != -1) {
		if (dst->transparent == -1) {
			dst->transparent = src->transparent;
		}
	}

	for (i = 0; (i < gdMaxColors); i++) {
		cmap[i] = (-1);
	}
	for (dy = dstY - radius; (dy <= dstY + radius); dy++) {
		for (dx = dstX - radius; (dx <= dstX + radius); dx++) {
			double sxd = (dx - dstX) * aCos - (dy - dstY) * aSin;
			double syd = (dy - dstY) * aCos + (dx - dstX) * aSin;
			int sx = sxd + scX;
			int sy = syd + scY;
			if ((sx >= srcX) && (sx < srcX + srcWidth) &&
			        (sy >= srcY) && (sy < srcY + srcHeight)) {
				int c = gdImageGetPixel (src, sx, sy);
				/* 2.0.34: transparency wins */
				if (c == src->transparent) {
					gdImageSetPixel (dst, dx, dy, dst->transparent);
				} else if (!src->trueColor) {
					/* Use a table to avoid an expensive
					   lookup on every single pixel */
					if (cmap[c] == -1) {
						cmap[c] = gdImageColorResolveAlpha (dst,
						                                    gdImageRed (src, c),
						                                    gdImageGreen (src,
						                                            c),
						                                    gdImageBlue (src,
						                                            c),
						                                    gdImageAlpha (src,
						                                            c));
					}
					gdImageSetPixel (dst, dx, dy, cmap[c]);
				} else {
					gdImageSetPixel (dst,
					                 dx, dy,
					                 gdImageColorResolveAlpha (dst,
					                         gdImageRed (src,
					                                     c),
					                         gdImageGreen
					                         (src, c),
					                         gdImageBlue (src,
					                                      c),
					                         gdImageAlpha
					                         (src, c)));
				}
			}
		}
	}
}

/* When gd 1.x was first created, floating point was to be avoided.
   These days it is often faster than table lookups or integer
   arithmetic. The routine below is shamelessly, gloriously
   floating point. TBB */

/* 2.0.10: cast instead of floor() yields 35% performance improvement.
	Thanks to John Buckman. */

#define floor2(exp) ((long) exp)
/*#define floor2(exp) floor(exp)*/

BGD_DECLARE(void) gdImageCopyResampled (gdImagePtr dst,
										gdImagePtr src,
										int dstX, int dstY,
										int srcX, int srcY,
										int dstW, int dstH, int srcW, int srcH)
{
	int x, y;
	double sy1, sy2, sx1, sx2;
	if (!dst->trueColor) {
		gdImageCopyResized (dst, src, dstX, dstY, srcX, srcY, dstW, dstH,
		                    srcW, srcH);
		return;
	}
	for (y = dstY; (y < dstY + dstH); y++) {
		sy1 = ((double) y - (double) dstY) * (double) srcH / (double) dstH;
		sy2 = ((double) (y + 1) - (double) dstY) * (double) srcH /
		      (double) dstH;
		for (x = dstX; (x < dstX + dstW); x++) {
			double sx, sy;
			double spixels = 0;
			double red = 0.0, green = 0.0, blue = 0.0, alpha = 0.0;
			double alpha_sum = 0.0, contrib_sum = 0.0;

			sx1 = ((double) x - (double) dstX) * (double) srcW / dstW;
			sx2 = ((double) (x + 1) - (double) dstX) * (double) srcW / dstW;
			sy = sy1;
			do {
				double yportion;
				if (floor2 (sy) == floor2 (sy1)) {
					yportion = 1.0 - (sy - floor2 (sy));
					if (yportion > sy2 - sy1) {
						yportion = sy2 - sy1;
					}
					sy = floor2 (sy);
				} else if (sy == floor2 (sy2)) {
					yportion = sy2 - floor2 (sy2);
				} else {
					yportion = 1.0;
				}
				sx = sx1;
				do {
					double xportion;
					double pcontribution;
					int p;
					if (floor2 (sx) == floor2 (sx1)) {
						xportion = 1.0 - (sx - floor2 (sx));
						if (xportion > sx2 - sx1) {
							xportion = sx2 - sx1;
						}
						sx = floor2 (sx);
					} else if (sx == floor2 (sx2)) {
						xportion = sx2 - floor2 (sx2);
					} else {
						xportion = 1.0;
					}
					pcontribution = xportion * yportion;
					/* 2.08: previously srcX and srcY were ignored.
					   Andrew Pattison */
					p = gdImageGetTrueColorPixel (src,
					                              (int) sx + srcX,
					                              (int) sy + srcY);
					red += gdTrueColorGetRed (p) * pcontribution;
					green += gdTrueColorGetGreen (p) * pcontribution;
					blue += gdTrueColorGetBlue (p) * pcontribution;
					alpha += gdTrueColorGetAlpha (p) * pcontribution;
					spixels += xportion * yportion;
					sx += 1.0;
				} while (sx < sx2);
				sy += 1.0;
			} while (sy < sy2);
			if (spixels != 0.0) {
				red /= spixels;
				green /= spixels;
				blue /= spixels;
				alpha /= spixels;
				alpha += 0.5;
			}
			if ( alpha_sum != 0.0f) {
				if( contrib_sum != 0.0f) {
					alpha_sum /= contrib_sum;
				}
				red /= alpha_sum;
				green /= alpha_sum;
				blue /= alpha_sum;
			}
			/* Clamping to allow for rounding errors above */
			if (red > 255.0) {
				red = 255.0;
			}
			if (green > 255.0) {
				green = 255.0;
			}
			if (blue > 255.0) {
				blue = 255.0;
			}
			if (alpha > gdAlphaMax) {
				alpha = gdAlphaMax;
			}
			gdImageSetPixel (dst,
			                 x, y,
			                 gdTrueColorAlpha ((int) red,
			                                   (int) green,
			                                   (int) blue, (int) alpha));
		}
	}
}
