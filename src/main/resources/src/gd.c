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

BGD_DECLARE(void) gdImageAABlend (gdImagePtr im)
{
	/* NO-OP, kept for library compatibility. */
	(void)im;
}

static void gdImageAALine (gdImagePtr im, int x1, int y1, int x2, int y2, int col);

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
