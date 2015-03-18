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

