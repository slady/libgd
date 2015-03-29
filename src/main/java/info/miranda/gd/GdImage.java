package info.miranda.gd;

import info.miranda.gd.enums.GdEffect;
import info.miranda.gd.enums.GdImageColorType;
import info.miranda.gd.enums.GdInterpolationMethod;
import info.miranda.gd.interfaces.GdCallbackImageColor;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

/*
   The data structure in which gd stores images.
*/
public class GdImage {
	/* Palette-based image pixels */
	int[][] pixels;
	int sx;
	int sy;
	/* These are valid in palette images only. See also
	   'alpha', which appears later in the structure to
	   preserve binary backwards compatibility */
	int colorsTotal;
	int[] red = new int[GdUtils.MAX_COLORS];
	int[] green = new int[GdUtils.MAX_COLORS];
	int[] blue = new int[GdUtils.MAX_COLORS];
	boolean[] open = new boolean[GdUtils.MAX_COLORS];
	/* For backwards compatibility, this is set to the
	   first palette entry with 100% transparency,
	   and is also set and reset by the
	   colorTransparent function. Newer
	   applications can allocate palette entries
	   with any desired level of transparency; however,
	   bear in mind that many viewers, notably
	   many web browsers, fail to implement
	   full alpha channel for PNG and provide
	   support for full opacity or transparency only. */
	int transparent;
	int[] polyInts;
	int polyAllocated;
	GdImage brush;
	GdImage tile;
	int[] brushColorMap = new int[GdUtils.MAX_COLORS];
	int[] tileColorMap = new int[GdUtils.MAX_COLORS];
	int styleLength;
	int stylePos;
	int[] style;
	int interlace;
	/* New in 2.0: thickness of line. Initialized to 1. */
	int thick;
	/* New in 2.0: alpha channel for palettes. Note that only
	   Macintosh Internet Explorer and (possibly) Netscape 6
	   really support multiple levels of transparency in
	   palettes, to my knowledge, as of 2/15/01. Most
	   common browsers will display 100% opaque and
	   100% transparent correctly, and do something
	   unpredictable and/or undesirable for levels
	   in between. TBB */
	int[] alpha = new int[GdUtils.MAX_COLORS];
	/* Truecolor flag and pixels. New 2.0 fields appear here at the
	   end to minimize breakage of existing object code. */
	boolean trueColor;
	int[][] tpixels;
	/* Should alpha channel be copied, or applied, each time a
	   pixel is drawn? This applies to truecolor images only.
	   No attempt is made to alpha-blend in palette images,
	   even if semitransparent palette entries exist.
	   To do that, build your image as a truecolor image,
	   then quantize down to 8 bits. */
	GdEffect alphaBlendingFlag;
	/* Should the alpha channel of the image be saved? This affects
	   PNG at the moment; other future formats may also
	   have that capability. JPEG doesn't. */
	int saveAlphaFlag;

	/* There should NEVER BE ACCESSOR MACROS FOR ITEMS BELOW HERE, so this
	   part of the structure can be safely changed in new releases. */

	/* 2.0.12: anti-aliased globals. 2.0.26: just a few vestiges after
	  switching to the fast, memory-cheap implementation from PHP-gd. */
	int AA;
	int AA_color;
	int AA_dont_blend;

	/* 2.0.12: simple clipping rectangle. These values
	  must be checked for safety when set; please use
	  setClip */
	int cx1;
	int cy1;
	int cx2;
	int cy2;

	/* 2.1.0: allows to specify resolution in dpi */
	int res_x;
	int res_y;

	/* Selects quantization method, see trueColorToPaletteSetMethod() and gdPaletteQuantizationMethod enum. */
	int paletteQuantizationMethod;
	/* speed/quality trade-off. 1 = best quality, 10 = best speed. 0 = method-specific default.
	   Applicable to GD_QUANT_LIQ and GD_QUANT_NEUQUANT. */
	int paletteQuantizationSpeed;
	/* Image will remain true-color if conversion to palette cannot achieve given quality.
	   Value from 1 to 100, 1 = ugly, 100 = perfect. Applicable to GD_QUANT_LIQ.*/
	int paletteQuantizationMinQuality;
	/* Image will use minimum number of palette colors needed to achieve given quality. Must be higher than paletteQuantizationMinQuality
	   Value from 1 to 100, 1 = ugly, 100 = perfect. Applicable to GD_QUANT_LIQ.*/
	int paletteQuantizationMaxQuality;
	GdInterpolationMethod interpolation_id;
//	interpolation_method interpolation;

	/* 2.0.12: this now checks the clipping rectangle */
	private boolean isBoundsSafe(final int x, final int y) {
		return (!(((y < cy1) || (y > cy2)) || ((x < cx1) || (x > cx2))));
	}

	/**
	 * @param sx The image width.
	 * @param sy The image height.
	 * @param colorType image color type, either palette or true color.
	 */
	public GdImage(final int sx, final int sy, final GdImageColorType colorType) {
		this.sx = sx;
		this.sy = sy;
		transparent = (-1);
		interlace = 0;
		thick = 1;
		AA = 0;
		cx1 = 0;
		cy1 = 0;
		cx2 = sx - 1;
		cy2 = sy - 1;
		res_x = GdUtils.GD_RESOLUTION;
		res_y = GdUtils.GD_RESOLUTION;
//		interpolation = NULL;
		interpolation_id = GdInterpolationMethod.GD_BILINEAR_FIXED;

		switch (colorType) {
			case TRUE_COLOR:
				tpixels = new int[sy][sx];
				trueColor = true;
	/* 2.0.2: alpha blending is now on by default, and saving of alpha is
	   off by default. This allows font antialiasing to work as expected
	   on the first try in JPEGs -- quite important -- and also allows
	   for smaller PNGs when saving of alpha channel is not really
	   desired, which it usually isn't! */
				saveAlphaFlag = 0;
				alphaBlendingFlag = GdEffect.ALPHA_BLEND;
				break;

			case PALETTE_BASED_COLOR:
				pixels = new int[sy][sx];
				colorsTotal = 0;
				for (int i = 0; (i < GdUtils.MAX_COLORS); i++) {
					open[i] = true;
				};
				trueColor = false;
				break;

			default:
				throw new IllegalArgumentException("unsupported image color type: " + colorType);
		}
	}

	public int getPixel(final int x, final int y) {
		if (isBoundsSafe(x, y)) {
			if (trueColor) {
				return tpixels[y][x];
			} else {
				return pixels[y][x];
			}
		} else {
			return 0;
		}
	}

	public int getTrueColorPixel(final int x, final int y) {
		int p = getPixel(x, y);
		if (!trueColor) {
			return GdUtils.trueColorMixAlpha(red[p], green[p], blue[p],
					(transparent == p) ? GdUtils.ALPHA_TRANSPARENT : alpha[p]);
		} else {
			return p;
		}
	}

	public int getPalettePixel(final int x, final int y) {
		return pixels[(y)][(x)];
	}

	/* This function accepts truecolor pixel values only. The
     source color is composited with the destination color
     based on the alpha channel value of the source color.
     The resulting color is opaque. */

/* Thanks to Frank Warmerdam for this superior implementation
	of gdAlphaBlend(), which merges alpha in the
	destination color much better. */

	private int gdAlphaBlend(final int dst, final int src) {
		int src_alpha = GdUtils.trueColorGetAlpha(src);
		int dst_alpha, alpha, red, green, blue;
		int src_weight, dst_weight, tot_weight;

	/* -------------------------------------------------------------------- */
	/*      Simple cases we want to handle fast.                            */
	/* -------------------------------------------------------------------- */
		if (src_alpha == GdUtils.ALPHA_OPAQUE)
			return src;

		dst_alpha = GdUtils.trueColorGetAlpha(dst);
		if (src_alpha == GdUtils.ALPHA_TRANSPARENT)
			return dst;
		if (dst_alpha == GdUtils.ALPHA_TRANSPARENT)
			return src;

	/* -------------------------------------------------------------------- */
	/*      What will the source and destination alphas be?  Note that      */
	/*      the destination weighting is substantially reduced as the       */
	/*      overlay becomes quite opaque.                                   */
	/* -------------------------------------------------------------------- */
		src_weight = GdUtils.ALPHA_TRANSPARENT - src_alpha;
		dst_weight = (GdUtils.ALPHA_TRANSPARENT - dst_alpha) * src_alpha / GdUtils.ALPHA_MAX;
		tot_weight = src_weight + dst_weight;

	/* -------------------------------------------------------------------- */
	/*      What red, green and blue result values will we use?             */
	/* -------------------------------------------------------------------- */
		alpha = src_alpha * dst_alpha / GdUtils.ALPHA_MAX;

		red = (GdUtils.trueColorGetRed(src) * src_weight
				+ GdUtils.trueColorGetRed(dst) * dst_weight) / tot_weight;
		green = (GdUtils.trueColorGetGreen(src) * src_weight
				+ GdUtils.trueColorGetGreen(dst) * dst_weight) / tot_weight;
		blue = (GdUtils.trueColorGetBlue(src) * src_weight
				+ GdUtils.trueColorGetBlue(dst) * dst_weight) / tot_weight;

	/* -------------------------------------------------------------------- */
	/*      Return merged result.                                           */
	/* -------------------------------------------------------------------- */
		return ((alpha << 24) + (red << 16) + (green << 8) + blue);
	}

	private int gdLayerOverlay(final int dst, final int src) {
		int a1, a2;
		a1 = GdUtils.ALPHA_MAX - GdUtils.trueColorGetAlpha(dst);
		a2 = GdUtils.ALPHA_MAX - GdUtils.trueColorGetAlpha(src);
		return (((GdUtils.ALPHA_MAX - a1 * a2 / GdUtils.ALPHA_MAX) << 24) +
				(gdAlphaOverlayColor(GdUtils.trueColorGetRed(src),
						GdUtils.trueColorGetRed(dst), GdUtils.RED_MAX) << 16) +
				(gdAlphaOverlayColor(GdUtils.trueColorGetGreen(src),
						GdUtils.trueColorGetGreen(dst), GdUtils.GREEN_MAX) << 8) +
				(gdAlphaOverlayColor(GdUtils.trueColorGetBlue(src),
						GdUtils.trueColorGetBlue(dst), GdUtils.BLUE_MAX)));
	}

	/* Apply 'overlay' effect - background pixels are colourised by the foreground colour */
	private int gdAlphaOverlayColor(final int src, int dst, final int max) {
		dst = dst << 1;
		if( dst > max ) {
		/* in the "light" zone */
			return dst + (src << 1) - (dst * src / max) - max;
		} else {
		/* in the "dark" zone */
			return dst * src / max;
		}
	}


	/* Apply 'multiply' effect */
	private int gdLayerMultiply(final int dst, final int src) {
		int a1, a2, r1, r2, g1, g2, b1, b2;
		a1 = GdUtils.ALPHA_MAX - GdUtils.trueColorGetAlpha(src);
		a2 = GdUtils.ALPHA_MAX - GdUtils.trueColorGetAlpha(dst);

		r1 = GdUtils.RED_MAX - (a1 * (GdUtils.RED_MAX - GdUtils.trueColorGetRed(src))) / GdUtils.ALPHA_MAX;
		r2 = GdUtils.RED_MAX - (a2 * (GdUtils.RED_MAX - GdUtils.trueColorGetRed(dst))) / GdUtils.ALPHA_MAX;
		g1 = GdUtils.GREEN_MAX - (a1 * (GdUtils.GREEN_MAX - GdUtils.trueColorGetGreen(src))) / GdUtils.ALPHA_MAX;
		g2 = GdUtils.GREEN_MAX - (a2 * (GdUtils.GREEN_MAX - GdUtils.trueColorGetGreen(dst))) / GdUtils.ALPHA_MAX;
		b1 = GdUtils.BLUE_MAX - (a1 * (GdUtils.BLUE_MAX - GdUtils.trueColorGetBlue(src))) / GdUtils.ALPHA_MAX;
		b2 = GdUtils.BLUE_MAX - (a2 * (GdUtils.BLUE_MAX - GdUtils.trueColorGetBlue(dst))) / GdUtils.ALPHA_MAX ;

		a1 = GdUtils.ALPHA_MAX - a1;
		a2 = GdUtils.ALPHA_MAX - a2;
		return (((a1*a2/GdUtils.ALPHA_MAX) << 24) +
				((r1*r2/GdUtils.RED_MAX) << 16) +
				((g1*g2/GdUtils.GREEN_MAX) << 8) +
				((b1*b2/GdUtils.BLUE_MAX)));
	}


	/* Replaces or blends with the background depending on the
	   most recent call to alphaBlending and the
	   alpha channel value of 'color'; default is to overwrite.
	   Tiling and line styling are also implemented
	   here. All other gd drawing functions pass through this call,
	   allowing for many useful effects.
	   Overlay and multiply effects are used when alphaBlending
	   is passed gdEffectOverlay and gdEffectMultiply */
	public void setPixel(final int x, final int y, final int color) {
		int p;
		switch (color) {
			case GdUtils.SPECIAL_COLOR_STYLED:
				if (style == null) {
			/* Refuse to draw if no style is set. */
					return;
				} else {
					p = style[stylePos++];
				}
				if (p != (GdUtils.SPECIAL_COLOR_TRANSPARENT)) {
					setPixel(x, y, p);
				}
				stylePos = stylePos % styleLength;
				break;
			case GdUtils.SPECIAL_COLOR_STYLED_BRUSHED:
				if (style == null) {
			/* Refuse to draw if no style is set. */
					return;
				}
				p = style[stylePos++];
				if ((p != GdUtils.SPECIAL_COLOR_TRANSPARENT) && (p != 0)) {
					setPixel(x, y, GdUtils.SPECIAL_COLOR_BRUSHED);
				}
				stylePos = stylePos % styleLength;
				break;
			case GdUtils.SPECIAL_COLOR_BRUSHED:
				brushApply(x, y);
				break;
			case GdUtils.SPECIAL_COLOR_TILED:
				tileApply(x, y);
				break;
			case GdUtils.SPECIAL_COLOR_ANTI_ALIASED:
		/* This shouldn't happen (2.0.26) because we just call
		  AALine now, but do something sane. */
				setPixel(x, y, AA_color);
				break;
			default:
				if (isBoundsSafe(x, y)) {
					if (trueColor) {
						switch (alphaBlendingFlag) {
							default:
							case REPLACE:
								tpixels[y][x] = color;
								break;
							case ALPHA_BLEND:
							case NORMAL:
								tpixels[y][x] = gdAlphaBlend(tpixels[y][x], color);
								break;
							case OVERLAY:
								tpixels[y][x] = gdLayerOverlay(tpixels[y][x], color);
								break;
							case MULTIPLY:
								tpixels[y][x] = gdLayerMultiply(tpixels[y][x], color);
								break;
						}
					} else {
						pixels[y][x] = color;
					}
				}
				break;
		}
	}

	private void brushApply(final int x, final int y) {
		int x1, y1, x2, y2;
		int srcx, srcy;
		if (brush == null) {
			return;
		}
		y1 = y - (brush.sy / 2);
		y2 = y1 + brush.sy;
		x1 = x - (brush.sx / 2);
		x2 = x1 + brush.sx;
		srcy = 0;
		if (trueColor) {
			if (brush.trueColor) {
				for (int ly = y1; (ly < y2); ly++) {
					srcx = 0;
					for (int lx = x1; (lx < x2); lx++) {
						int p;
						p = brush.getTrueColorPixel(srcx, srcy);
					/* 2.0.9, Thomas Winzig: apply simple full transparency */
						if (p != brush.transparent) {
							setPixel(lx, ly, p);
						}
						srcx++;
					}
					srcy++;
				}
			} else {
			/* 2.0.12: Brush palette, image truecolor (thanks to Thorben Kundinger
			   for pointing out the issue) */
				for (int ly = y1; (ly < y2); ly++) {
					srcx = 0;
					for (int lx = x1; (lx < x2); lx++) {
						int p, tc;
						p = getPixel(srcx, srcy);
						tc = getTrueColorPixel(srcx, srcy);
					/* 2.0.9, Thomas Winzig: apply simple full transparency */
						if (p != brush.transparent) {
							setPixel(lx, ly, tc);
						}
						srcx++;
					}
					srcy++;
				}
			}
		} else {
			for (int ly = y1; (ly < y2); ly++) {
				srcx = 0;
				for (int lx = x1; (lx < x2); lx++) {
					int p;
					p = getPixel(srcx, srcy);
				/* Allow for non-square brushes! */
					if (p != brush.transparent) {
					/* Truecolor brush. Very slow
					   on a palette destination. */
						if (brush.trueColor) {
							setPixel(lx, ly,
									colorResolveAlpha(
											GdUtils.trueColorGetRed(p),
											GdUtils.trueColorGetGreen(p),
											GdUtils.trueColorGetBlue(p),
											GdUtils.trueColorGetAlpha(p)));
						} else {
							setPixel(lx, ly, brushColorMap[p]);
						}
					}
					srcx++;
				}
				srcy++;
			}
		}
	}

	void tileApply(int x, int y) {
		int srcx, srcy;
		int p;
		if (tile == null) {
			return;
		}
		srcx = x % tile.sx;
		srcy = y % tile.sy;
		if (trueColor) {
			p = tile.getPixel(srcx, srcy);
			if (p != tile.transparent) {
				if (!tile.trueColor) {
					p = GdUtils.trueColorMixAlpha(tile.red[p], tile.green[p], tile.blue[p], tile.alpha[p]);
				}
				setPixel(x, y, p);
			}
		} else {
			p = tile.getPixel(srcx, srcy);
		/* Allow for transparency */
			if (p != tile.transparent) {
				if (tile.trueColor) {
				/* Truecolor tile. Very slow
				   on a palette destination. */
					setPixel(x, y,
							colorResolveAlpha(
									GdUtils.trueColorGetRed(p),
									GdUtils.trueColorGetGreen(p),
									GdUtils.trueColorGetBlue(p),
									GdUtils.trueColorGetAlpha(p)));
				} else {
					setPixel(x, y, tileColorMap[p]);
				}
			}
		}
	}


/*
 * colorResolve is an alternative for the code fragment:
 *
 *      if ((color=colorExact(im,R,G,B)) < 0)
 *        if ((color=colorAllocate(im,R,G,B)) < 0)
 *          color=colorClosest(im,R,G,B);
 *
 * in a single function.    Its advantage is that it is guaranteed to
 * return a color index in one search over the color table.
 */

	/* Opaque only */
	private int colorResolve(int r, int g, int b) {
		return colorResolveAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	/* Based on gdImageColorExactAlpha and gdImageColorClosestAlpha */
	private int colorResolveAlpha(int r, int g, int b, int a) {
		int c;
		int ct = -1;
		int op = -1;
		long rd, gd, bd, ad, dist;
		long mindist = 4 * 255 * 255;	/* init to max poss dist */
		if (trueColor) {
			return GdUtils.trueColorMixAlpha(r, g, b, a);
		}

		for (c = 0; c < colorsTotal; c++) {
			if (open[c]) {
				op = c;		/* Save open slot */
				continue;		/* Color not in use */
			}
			if (c == transparent) {
			/* don't ever resolve to the color that has
			 * been designated as the transparent color */
				continue;
			}
			rd = (long) (red[c] - r);
			gd = (long) (green[c] - g);
			bd = (long) (blue[c] - b);
			ad = (long) (alpha[c] - a);
			dist = rd * rd + gd * gd + bd * bd + ad * ad;
			if (dist < mindist) {
				if (dist == 0) {
					return c;		/* Return exact match color */
				}
				mindist = dist;
				ct = c;
			}
		}
	/* no exact match.  We now know closest, but first try to allocate exact */
		if (op == -1) {
			op = colorsTotal;
			if (op == GdUtils.MAX_COLORS) {
			/* No room for more colors */
				return ct;		/* Return closest available color */
			}
			colorsTotal++;
		}
		red[op] = r;
		green[op] = g;
		blue[op] = b;
		alpha[op] = a;
		open[op] = false;
		return op;			/* Return newly allocated color */
	}

	/* Returns exact, 100% opaque matches only */
	public int colorExact(final int r, final int g, final int b) {
		return colorExactAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	/* Returns an exact match only, including alpha */
	public int colorExactAlpha(final int r, final int g, final int b, final int a) {
		if (trueColor) {
			return GdUtils.trueColorMixAlpha(r, g, b, a);
		}
		for (int i = 0; (i < (colorsTotal)); i++) {
			if (open[i]) {
				continue;
			}
			if ((red[i] == r) && (green[i] == g) && (blue[i] == b) && (alpha[i] == a)) {
				return i;
			}
		}
		return -1;
	}

	/* These functions still work with truecolor images,
	   for which they never return error. */
	public int colorAllocate(final int r, int g, int b) {
		return colorAllocateAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	/* gd 2.0: palette entries with non-opaque transparency are permitted. */
	public int colorAllocateAlpha(int r, int g, int b, int a) {
		int i;
		int ct = (-1);
		if (trueColor) {
			return GdUtils.trueColorMixAlpha(r, g, b, a);
		}
		for (i = 0; (i < (colorsTotal)); i++) {
			if (open[i]) {
				ct = i;
				break;
			}
		}
		if (ct == (-1)) {
			ct = colorsTotal;
			if (ct == GdUtils.MAX_COLORS) {
				return -1;
			}
			colorsTotal++;
		}
		red[ct] = r;
		green[ct] = g;
		blue[ct] = b;
		alpha[ct] = a;
		open[ct] = false;
		return ct;
	}

	public void gdImageColorDeallocate(int color) {
		if (trueColor || (color >= GdUtils.MAX_COLORS) || (color < 0)) {
			return;
		}
	/* Mark it open. */
		open[color] = true;
	}

	/* Bresenham as presented in Foley & Van Dam */
	public void drawLine(int x1, int y1, int x2, int y2, final int color) {
		int dx, dy, incr1, incr2, d, x, y, xend, yend, xdirflag, ydirflag;
		int wid;
		int w, wstart;

		if (color == GdUtils.SPECIAL_COLOR_ANTI_ALIASED) {
		/*
		  gdAntiAliased passed as color: use the much faster, much cheaper
		  and equally attractive AALine implementation. That
		  clips too, so don't clip twice.
		*/
			AALine(x1, y1, x2, y2, AA_color);
			return;
		}
	/* 2.0.10: Nick Atty: clip to edges of drawing rectangle, return if no
	   points need to be drawn. 2.0.26, TBB: clip to edges of clipping
	   rectangle. We were getting away with this because setPixel
	   is used for actual drawing, but this is still more efficient and opens
	   the way to skip per-pixel bounds checking in the future. */

		final GdClipRectangle clip1 = new GdClipRectangle(x1, y1, x2, y2);
		if (!clip1.clip_1d(cx1, cx2))
			return;
		x1 = clip1.x0;
		y1 = clip1.y0;
		x2 = clip1.x1;
		y2 = clip1.y1;

		final GdClipRectangle clip2 = new GdClipRectangle(y1, x1, y2, x2);
		if (!clip2.clip_1d(cy1, cy2))
			return;
		y1 = clip2.x0;
		x1 = clip2.y0;
		y2 = clip2.x1;
		x2 = clip2.y1;

		dx = Math.abs(x2 - x1);
		dy = Math.abs(y2 - y1);

		if (dx == 0) {
			vLine(x1, y1, y2, color);
			return;
		} else if (dy == 0) {
			hLine(y1, x1, x2, color);
			return;
		}

		if (dy <= dx) {
		/* More-or-less horizontal. use wid for vertical stroke */
		/* Doug Claar: watch out for NaN in atan2 (2.0.5) */
			if ((dx == 0) && (dy == 0)) {
				wid = 1;
			} else {
			/* 2.0.12: Michael Schwartz: divide rather than multiply;
			   TBB: but watch out for /0! */
				double ac = Math.cos(Math.atan2(dy, dx));
				if (ac != 0) {
					wid = (int) (thick / ac);
				} else {
					wid = 1;
				}
				if (wid == 0) {
					wid = 1;
				}
			}
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

		/* Set up line thickness */
			wstart = y - wid / 2;
			for (w = wstart; w < wstart + wid; w++)
				setPixel(x, w, color);

			if (((y2 - y1) * ydirflag) > 0) {
				while (x < xend) {
					x++;
					if (d < 0) {
						d += incr1;
					} else {
						y++;
						d += incr2;
					}
					wstart = y - wid / 2;
					for (w = wstart; w < wstart + wid; w++)
						setPixel(x, w, color);
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
					wstart = y - wid / 2;
					for (w = wstart; w < wstart + wid; w++)
						setPixel(x, w, color);
				}
			}
		} else {
		/* More-or-less vertical. use wid for horizontal stroke */
		/* 2.0.12: Michael Schwartz: divide rather than multiply;
		   TBB: but watch out for /0! */
			double as = Math.sin(Math.atan2(dy, dx));
			if (as != 0) {
				wid = (int) (thick / as);
			} else {
				wid = 1;
			}
			if (wid == 0)
				wid = 1;

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

		/* Set up line thickness */
			wstart = x - wid / 2;
			for (w = wstart; w < wstart + wid; w++)
				setPixel(w, y, color);

			if (((x2 - x1) * xdirflag) > 0) {
				while (y < yend) {
					y++;
					if (d < 0) {
						d += incr1;
					} else {
						x++;
						d += incr2;
					}
					wstart = x - wid / 2;
					for (w = wstart; w < wstart + wid; w++)
						setPixel(w, y, color);
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
					wstart = x - wid / 2;
					for (w = wstart; w < wstart + wid; w++)
						setPixel(w, y, color);
				}
			}
		}
	}

	private void vLine(final int x, int y1, int y2, final int col) {
		if (thick > 1) {
			int thickhalf = thick >> 1;
			fillRectangle(x - thickhalf, y1, x + thick - thickhalf - 1, y2, col);
		} else {
			if (y2 < y1) {
				int t = y1;
				y1 = y2;
				y2 = t;
			}

			for (; y1 <= y2; y1++) {
				setPixel(x, y1, col);
			}
		}
		return;
	}

	private void hLine(int y, int x1, int x2, final int col)
	{
		if (thick > 1) {
			int thickhalf = thick >> 1;
			fillRectangle(x1, y - thickhalf, x2, y + thick - thickhalf - 1, col);
		} else {
			if (x2 < x1) {
				int t = x2;
				x2 = x1;
				x1 = t;
			}

			for (; x1 <= x2; x1++) {
				setPixel(x1, y, col);
			}
		}
		return;
	}


	private void AALine(int x1, int y1, int x2, int y2, final int col) {
	/* keep them as 32bits */
		long x, y, inc, frac;
		long dx, dy,tmp;
		int w, wid, wstart;

		if (!trueColor) {
		/* TBB: don't crash when the image is of the wrong type */
			drawLine(x1, y1, x2, y2, col);
			return;
		}

	/* TBB: use the clipping rectangle */
		final GdClipRectangle clip1 = new GdClipRectangle(x1, y1, x2, y2);
		if (!clip1.clip_1d(cx1, cx2))
			return;
		x1 = clip1.x0;
		y1 = clip1.y0;
		x2 = clip1.x1;
		y2 = clip1.y1;

		final GdClipRectangle clip2 = new GdClipRectangle(y1, x1, y2, x2);
		if (!clip2.clip_1d(cy1, cy2))
			return;
		y1 = clip2.x0;
		x1 = clip2.y0;
		y2 = clip2.x1;
		x2 = clip2.y1;

		dx = x2 - x1;
		dy = y2 - y1;

		if (dx == 0 && dy == 0) {
		/* TBB: allow setting points */
			setAAPixelColor(x1, y1, col, 0xFF);
			return;
		} else {
			double ag;
			ag = (Math.abs(dy) < Math.abs(dx)) ? Math.cos(Math.atan2(dy, dx)) : Math.sin(Math.atan2(dy, dx));
			if (ag != 0) {
				wid = (int) Math.abs(thick / ag);
			} else {
				wid = 1;
			}
			if (wid == 0) {
				wid = 1;
			}
		}

	/* Axis aligned lines */
		if (dx == 0) {
			vLine(x1, y1, y2, col);
			return;
		} else if (dy == 0) {
			hLine(y1, x1, x2, col);
			return;
		}

		if (Math.abs(dx) > Math.abs(dy)) {
			if (dx < 0) {
				tmp = x1;
				x1 = x2;
				x2 = (int) tmp;
				tmp = y1;
				y1 = y2;
				y2 = (int) tmp;
				dx = x2 - x1;
				dy = y2 - y1;
			}
			y = y1;
			inc = (dy * 65536) / dx;
			frac = 0;
		/* TBB: set the last pixel for consistency (<=) */
			for (x = x1 ; x <= x2 ; x++) {
				wstart = (int) (y - wid / 2);
				for (w = wstart; w < wstart + wid; w++) {
					setAAPixelColor((int) x, w, col, (int) ((frac >> 8) & 0xFF));
					setAAPixelColor((int) x, w + 1, col, (int) ((~frac >> 8) & 0xFF));
				}
				frac += inc;
				if (frac >= 65536) {
					frac -= 65536;
					y++;
				} else if (frac < 0) {
					frac += 65536;
					y--;
				}
			}
		} else {
			if (dy < 0) {
				tmp = x1;
				x1 = x2;
				x2 = (int) tmp;
				tmp = y1;
				y1 = y2;
				y2 = (int) tmp;
				dx = x2 - x1;
				dy = y2 - y1;
			}
			x = x1;
			inc = (dx * 65536) / dy;
			frac = 0;
		/* TBB: set the last pixel for consistency (<=) */
			for (y = y1 ; y <= y2 ; y++) {
				wstart = (int) (x - wid / 2);
				for (w = wstart; w < wstart + wid; w++) {
					setAAPixelColor(w, (int) y, col, (int) ((frac >> 8) & 0xFF));
					setAAPixelColor(w + 1, (int) y, col, (int) ((~frac >> 8) & 0xFF));
				}
				frac += inc;
				if (frac >= 65536) {
					frac -= 65536;
					x++;
				} else if (frac < 0) {
					frac += 65536;
					x--;
				}
			}
		}
	}

	private void setAAPixelColor(final int x, final int y, final int color, final int t) {
		int dr,dg,db,p,r,g,b;

	/* 2.0.34: watch out for out of range calls */
		if (!isBoundsSafe(x, y)) {
			return;
		}
		p = getPixel(x, y);
	/* TBB: we have to implement the dont_blend stuff to provide
	  the full feature set of the old implementation */
		if ((p == color)
				|| ((p == AA_dont_blend)
				&& (t != 0x00))) {
		return;
	}
		dr = GdUtils.trueColorGetRed(color);
		dg = GdUtils.trueColorGetGreen(color);
		db = GdUtils.trueColorGetBlue(color);

		r = GdUtils.trueColorGetRed(p);
		g = GdUtils.trueColorGetGreen(p);
		b = GdUtils.trueColorGetBlue(p);

		dr = blendColor(t, r, dr);
		dg = blendColor(t, g, dg);
		db = blendColor(t, b, db);
		tpixels[y][x] = GdUtils.trueColorMixAlpha(dr, dg, db, GdUtils.ALPHA_OPAQUE);
	}

	/**
 	 * Added on 2003/12 by Pierre-Alain Joye (pajoye@pearfr.org)
	 **/
	private int blendColor(final int a, final int c, final int cc) {
		return (cc) + (((((c) - (cc)) * (a)) + ((((c) - (cc)) * (a)) >> 8) + 0x80) >> 8);
	}

	/* Solid bar. Upper left corner first, lower right corner second. */
	public void fillRectangle(int x1, int y1, int x2, int y2, final int color) {
		int x, y;

		if (x1 == x2 && y1 == y2) {
			setPixel(x1, y1, color);
			return;
		}

		if (x1 > x2) {
			x = x1;
			x1 = x2;
			x2 = x;
		}

		if (y1 > y2) {
			y = y1;
			y1 = y2;
			y2 = y;
		}

		if (x1 < 0) {
			x1 = 0;
		}

		if (x2 >= sx) {
			x2 = sx - 1;
		}

		if (y1 < 0) {
			y1 = 0;
		}

		if (y2 >= sy) {
			y2 = sy - 1;
		}

		for (y = y1; (y <= y2); y++) {
			for (x = x1; (x <= x2); x++) {
				setPixel(x, y, color);
			}
		}
	}

	/* Corners specified (not width and height). Upper left first, lower right
	   second. */
	public void drawRectangle(int x1, int y1, int x2, int y2, final int color) {
		if (x1 == x2 && y1 == y2 && thick == 1) {
			setPixel(x1, y1, color);
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
					setPixel(cx, cy, color);
				}
			}

			cy = y2lr - thick;
			while (cy++ < y2lr) {
				cx = x1ul - 1;
				while (cx++ < x2lr) {
					setPixel(cx, cy, color);
				}
			}

			cy = y1ul + thick - 1;
			while (cy++ < y2lr -thick) {
				cx = x1ul - 1;
				while (cx++ < x1ul + thick) {
					setPixel(cx, cy, color);
				}
			}

			cy = y1ul + thick - 1;
			while (cy++ < y2lr -thick) {
				cx = x2lr - thick - 1;
				while (cx++ < x2lr) {
					setPixel(cx, cy, color);
				}
			}

			return;
		} else {
			drawLine(x1, y1, x2, y1, color);
			drawLine(x1, y2, x2, y2, color);
			drawLine(x1, y1 + 1, x1, y2 - 1, color);
			drawLine(x2, y1 + 1, x2, y2 - 1, color);
		}
	}

	public void drawPolygon(final GdPoint[] p, final int c) {
		drawLine(p[0].x, p[0].y, p[p.length - 1].x, p[p.length - 1].y, c);
		drawOpenPolygon(p, c);
	}

	public void drawOpenPolygon(final GdPoint[] p, final int c) {
		int i;
		int lx, ly;

		lx = p[0].x;
		ly = p[0].y;
		for (i = 1; (i < p.length); i++) {
			drawLine(lx, ly, p[i].x, p[i].y, c);
			lx = p[i].x;
			ly = p[i].y;
		}

	}

/* THANKS to Kirsten Schulz for the polygon fixes! */

/* The intersection finding technique of this code could be improved  */
/* by remembering the previous intersection, and by using the slope. */
/* That could help to adjust intersections  to produce a nice */
/* interior_extrema. */
	public void fillPolygon(final GdPoint[] p, final int c) {
		int i;
		int j;
		int index;
		int y;
		int miny, maxy, pmaxy;
		int x1, y1;
		int x2, y2;
		int ind1, ind2;
		int ints;
		int fill_color;

		if (c == GdUtils.SPECIAL_COLOR_ANTI_ALIASED) {
			fill_color = AA_color;
		} else {
			fill_color = c;
		}
		if (polyAllocated == 0) {
			polyInts = new int[p.length];
			polyAllocated = p.length;
		}
		if (polyAllocated < p.length) {
			while (polyAllocated < p.length) {
				polyAllocated *= 2;
			}
			polyInts = new int[polyAllocated];
		}
		miny = p[0].y;
		maxy = p[0].y;
		for (i = 1; (i < p.length); i++) {
			if (p[i].y < miny) {
				miny = p[i].y;
			}
			if (p[i].y > maxy) {
				maxy = p[i].y;
			}
		}
		pmaxy = maxy;
	/* 2.0.16: Optimization by Ilia Chipitsine -- don't waste time offscreen */
	/* 2.0.26: clipping rectangle is even better */
		if (miny < cy1) {
			miny = cy1;
		}
		if (maxy > cy2) {
			maxy = cy2;
		}
	/* Fix in 1.3: count a vertex only once */
		for (y = miny; (y <= maxy); y++) {
			ints = 0;
			for (i = 0; (i < p.length); i++) {
				if (i == 0) {
					ind1 = p.length - 1;
					ind2 = 0;
				} else {
					ind1 = i - 1;
					ind2 = i;
				}
				y1 = p[ind1].y;
				y2 = p[ind2].y;
				if (y1 < y2) {
					x1 = p[ind1].x;
					x2 = p[ind2].x;
				} else if (y1 > y2) {
					y2 = p[ind1].y;
					y1 = p[ind2].y;
					x2 = p[ind1].x;
					x1 = p[ind2].x;
				} else {
					continue;
				}

			/* Do the following math as float intermediately, and round to ensure
			 * that Polygon and FilledPolygon for the same set of points have the
			 * same footprint. */

				if ((y >= y1) && (y < y2)) {
					polyInts[ints++] = (int) ((float) ((y - y1) * (x2 - x1)) /
							(float) (y2 - y1) + 0.5 + x1);
				} else if ((y == pmaxy) && (y == y2)) {
					polyInts[ints++] = x2;
				}
			}
		/*
		  2.0.26: polygons pretty much always have less than 100 points,
		  and most of the time they have considerably less. For such trivial
		  cases, insertion sort is a good choice. Also a good choice for
		  future implementations that may wish to indirect through a table.
		*/
			for (i = 1; (i < ints); i++) {
				index = polyInts[i];
				j = i;
				while ((j > 0) && (polyInts[j - 1] > index)) {
					polyInts[j] = polyInts[j - 1];
					j--;
				}
				polyInts[j] = index;
			}
			for (i = 0; (i < (ints-1)); i += 2) {
			/* 2.0.29: back to line to prevent segfaults when
			  performing a pattern fill */
				drawLine(polyInts[i], y, polyInts[i + 1], y,
						fill_color);
			}
		}
	/* If we are drawing this AA, then redraw the border with AA lines. */
	/* This doesn't work as well as I'd like, but it doesn't clash either. */
		if (c == GdUtils.SPECIAL_COLOR_ANTI_ALIASED) {
			drawPolygon(p, c);
		}
	}

	public void setStyle(int[] style, int noOfPixels) {
		this.style = new int[noOfPixels];
		System.arraycopy(style, 0, this.style, 0, noOfPixels);
		styleLength = noOfPixels;
		stylePos = 0;
	}

/* s and e are integers modulo 360 (degrees), with 0 degrees
   being the rightmost extreme and degrees changing clockwise.
   cx and cy are the center in pixels; w and h are the horizontal
   and vertical diameter in pixels. Nice interface, but slow.
   See gd_arc_f_buggy.c for a better version that doesn't
   seem to be bug-free yet. */

	public void drawArc(final int cx, final int cy, final int w, final int h,
						   final int s, final int e, final int color) {
		fillArc(cx, cy, w, h, s, e, color, GdUtils.gdNoFill);
	}

	public void fillArc(final int cx, final int cy, final int w, final int h, int s, int e,
								 final int color, final int style) {
		GdPoint[] pts = new GdPoint[3];
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

		for (int i = s; (i <= e); i++) {
			final int x = (int) (((long) GdUtils.COS_T[i % 360] * (long) w / (2 * 1024)) + cx);
			final int y = (int) (((long) GdUtils.SIN_T[i % 360] * (long) h / (2 * 1024)) + cy);
			if (i != s) {
				if ((style & GdUtils.gdChord) == 0) {
					if ((style & GdUtils.gdNoFill) != 0) {
						drawLine(lx, ly, x, y, color);
					} else {
					/* This is expensive! */
						pts[0].x = lx;
						pts[0].y = ly;
						pts[1].x = x;
						pts[1].y = y;
						pts[2].x = cx;
						pts[2].y = cy;
						fillPolygon(pts, color);
					}
				}
			} else {
				fx = x;
				fy = y;
			}
			lx = x;
			ly = y;
		}
		if ((style & GdUtils.gdChord) != 0) {
			if ((style & GdUtils.gdNoFill) != 0) {
				if ((style & GdUtils.gdEdged) != 0) {
					drawLine(cx, cy, lx, ly, color);
					drawLine(cx, cy, fx, fy, color);
				}
				drawLine(fx, fy, lx, ly, color);
			} else {
				pts[0].x = fx;
				pts[0].y = fy;
				pts[1].x = lx;
				pts[1].y = ly;
				pts[2].x = cx;
				pts[2].y = cy;
				fillPolygon(pts, color);
			}
		} else {
			if ((style & GdUtils.gdNoFill) != 0) {
				if ((style & GdUtils.gdEdged) != 0) {
					drawLine(cx, cy, lx, ly, color);
					drawLine(cx, cy, fx, fy, color);
				}
			}
		}
	}

	public void drawEllipse(int mx, int my, int w, int h, int c) {
		int mx1=0,mx2=0,my1=0,my2=0;
		long aq,bq,dx,dy,r,rx,ry;

		final int a=w>>1;
		final int b=h>>1;
		setPixel(mx + a, my, c);
		setPixel(mx - a, my, c);
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
		int x = a;
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
			setPixel(mx1, my1, c);
			setPixel(mx1, my2, c);
			setPixel(mx2, my1, c);
			setPixel(mx2, my2, c);
		}
	}

	public void fillEllipse(final int mx, final int my, final int w, final int h, final int c) {
		int mx1=0,mx2=0,my1=0,my2=0;
		long aq,bq,dx,dy,r,rx,ry;
		int i;
		int old_y2;

		final int a=w>>1;
		final int b=h>>1;

		for (int x = mx-a; x <= mx+a; x++) {
			setPixel(x, my, c);
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
		int x = a;
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
					setPixel(i, my1, c);
				}
			}
			if(old_y2!=my2) {
				for(i=mx1; i<=mx2; i++) {
					setPixel(i, my2, c);
				}
			}
			old_y2 = my2;
		}
	}

	public void fillToBorder(final int x, final int y, final int border, final int color) {
		boolean lastBorder;
	/* Seek left */
		int leftLimit, rightLimit;
		int i;
		GdEffect restoreAlphaBleding;

		if (border < 0) {
		/* Refuse to fill to a non-solid border */
			return;
		}

		leftLimit = (-1);

		restoreAlphaBleding = alphaBlendingFlag;
		alphaBlendingFlag = GdEffect.REPLACE;

		for (i = x; (i >= 0); i--) {
			if (getPixel(i, y) == border) {
				break;
			}
			setPixel(i, y, color);
			leftLimit = i;
		}
		if (leftLimit == (-1)) {
			alphaBlendingFlag = restoreAlphaBleding;
			return;
		}
	/* Seek right */
		rightLimit = x;
		for (i = (x + 1); (i < sx); i++) {
		if (getPixel(i, y) == border) {
			break;
		}
		setPixel(i, y, color);
		rightLimit = i;
	}
	/* Look at lines above and below and start paints */
	/* Above */
		if (y > 0) {
			lastBorder = true;
			for (i = leftLimit; (i <= rightLimit); i++) {
				int c;
				c = getPixel(i, y - 1);
				if (lastBorder) {
					if ((c != border) && (c != color)) {
						fillToBorder(i, y - 1, border, color);
						lastBorder = false;
					}
				} else if ((c == border) || (c == color)) {
					lastBorder = true;
				}
			}
		}
	/* Below */
		if (y < ((sy) - 1)) {
			lastBorder = true;
			for (i = leftLimit; (i <= rightLimit); i++) {
				int c = getPixel(i, y + 1);
				if (lastBorder) {
					if ((c != border) && (c != color)) {
						fillToBorder(i, y + 1, border, color);
						lastBorder = false;
					}
				} else if ((c == border) || (c == color)) {
					lastBorder = true;
				}
			}
		}
		alphaBlendingFlag = restoreAlphaBleding;
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
	private int gdImageTileGet(final int x, final int y) {
		int tileColor;
		if (tile == null) {
			return -1;
		}
		final int srcx = x % (tile.sx);
		final int srcy = y % (tile.sy);
		final int p = tile.getPixel(srcx, srcy);
		if (p == tile.transparent) {
			tileColor = transparent;
		} else if (trueColor) {
			if (tile.trueColor) {
				tileColor = p;
			} else {
				tileColor = GdUtils.trueColorMixAlpha(tile.getRed(p), tile.getGreen(p), tile.getBlue(p), tile.getAlpha(p));
			}
		} else {
			if (tile.trueColor) {
				tileColor = colorResolveAlpha(GdUtils.trueColorGetRed(p), GdUtils.trueColorGetGreen(p), GdUtils.trueColorGetBlue(p), GdUtils.trueColorGetAlpha(p));
			} else {
				tileColor = colorResolveAlpha(tile.getRed(p), tile.getGreen(p), tile.getBlue(p), tile.getAlpha(p));
			}
		}
		return tileColor;
	}

	/* horizontal segment of scan line y */
	private class FillStack {
		private Stack<FillStack> stack = new Stack<FillStack>();
		int y, xl, xr, dy;

		public void push(final int Y, final int XL, final int XR, final int DY) {
			final FillStack sp = new FillStack();
			if (Y+(DY)>=0 && Y+(DY)<sy)
			{sp.y = Y; sp.xl = XL; sp.xr = XR; sp.dy = DY;}
			stack.push(sp);
		}

		public FillStack pop() {
			return stack.pop();
		}

		public boolean empty() {
			return stack.empty();
		}
	};

/* max depth of stack */
//	#define FILL_MAX ((int)(sy*sx)/4)
//			#define FILL_PUSH(Y, XL, XR, DY) \
//			if (sp<stack+FILL_MAX && Y+(DY)>=0 && Y+(DY)<wy2) \
//	{sp->y = Y; sp->xl = XL; sp->xr = XR; sp->dy = DY; sp++;}
//
//	#define FILL_POP(Y, XL, XR, DY) \
//	{sp--; Y = sp->y+(DY = sp->dy); XL = sp->xl; XR = sp->xr;}

	public void gdImageFill(int x, int y, final int nc) {
		int l, x1, x2, dy;
		int oc;   /* old pixel value */

		final GdEffect alphablending_bak;

	/* stack of filled segments */
	/* struct seg stack[FILL_MAX],*sp = stack; */
		final FillStack stack = new FillStack();

		if (!trueColor && nc > (colorsTotal - 1)) {
			return;
		}

		alphablending_bak = alphaBlendingFlag;
		alphaBlendingFlag = GdEffect.REPLACE;

		if (nc==GdUtils.SPECIAL_COLOR_TILED) {
			fillTiled(x, y, nc);
			alphaBlendingFlag = alphablending_bak;
			return;
		}

		final int wx2=sx;
		final int wy2=sy;
		oc = getPixel(x, y);
		if (oc==nc || x<0 || x>wx2 || y<0 || y>wy2) {
			alphaBlendingFlag = alphablending_bak;
			return;
		}

	/* Do not use the 4 neighbors implementation with
	* small images
	*/
		if (sx < 4) {
			int ix = x, iy = y, c;
			do {
				do {
					c = getPixel(ix, iy);
					if (c != oc) {
						alphaBlendingFlag = alphablending_bak;
						return;
					}
					setPixel(ix, iy, nc);
				} while(ix++ < (sx -1));
				ix = x;
			} while(iy++ < (sy -1));
			alphaBlendingFlag = alphablending_bak;
			return;
		}

		if(GdUtils.overflow2(sy, sx)) {
			return;
		}

	/* required! */
		stack.push(y, x, x, 1);
	/* seed segment (popped 1st) */
		stack.push(y + 1, x, x, -1);
		while (!stack.empty()) {
			final FillStack sp = stack.pop();
			y = sp.y+(dy = sp.dy); x1 = sp.xl; x2 = sp.xr;

			for (x=x1; x>=0 && getPixel(x, y)==oc; x--) {
				setPixel(x, y, nc);
			}
			if (x>=x1) {
				for (x++; x<=x2 && (getPixel(x, y)!=oc); x++);

				l = x;
			} else {
				l = x+1;

		/* leak on left? */
				if (l<x1) {
					stack.push(y, l, x1-1, -dy);
				}
				x = x1+1;
			}

			do {
				for (; x<=wx2 && getPixel(x, y)==oc; x++) {
					setPixel(x, y, nc);
				}
				stack.push(y, l, x-1, dy);
			/* leak on right? */
				if (x>x2+1) {
					stack.push(y, x2+1, x-1, -dy);
				}
				for (x++; x<=x2 && (getPixel(x, y)!=oc); x++);

				l = x;
			} while (x<=x2);
		}

		alphaBlendingFlag = alphablending_bak;
	}

	private void fillTiled(int x, int y, int nc) {
		int l, x1, x2, dy;
		int oc;   /* old pixel value */
	/* stack of filled segments */
		final FillStack stack = new FillStack();
		boolean[] pts = new boolean[sx * sy];

		if (tile == null) {
			return;
		}

		final int wx2=sx;
		final int wy2=sy;

		if(GdUtils.overflow2(sy, sx)) {
			return;
		}

		oc = getPixel(x, y);

	/* required! */
		stack.push(y,x, x, 1);
	/* seed segment (popped 1st) */
		stack.push(y+1, x, x, -1);
		while (!stack.empty()) {
			final FillStack sp = stack.pop();
			y = sp.y+(dy = sp.dy); x1 = sp.xl; x2 = sp.xr;
			for (x=x1; x>=0 && (!pts[y + x*wx2] && getPixel(x,y)==oc); x--) {
				nc = gdImageTileGet(x,y);
				pts[y + x*wx2]=true;
				setPixel(x, y, nc);
			}
			if (x>=x1) {
				for (x++; x<=x2 && (pts[y + x*wx2] || getPixel(x, y)!=oc); x++);
				l = x;
			} else {
				l = x+1;

		/* leak on left? */
				if (l<x1) {
					stack.push(y, l, x1-1, -dy);
				}
				x = x1+1;
			}

			do {
				for (; x<wx2 && (!pts[y + x*wx2] && getPixel(x, y)==oc) ; x++) {
					if (pts[y + x*wx2]) {
					/* we should never be here */
						break;
					}
					nc = gdImageTileGet(x,y);
					pts[y + x*wx2]=true;
					setPixel(x, y, nc);
				}
				stack.push(y, l, x-1, dy);
			/* leak on right? */
				if (x>x2+1) {
					stack.push(y, x2+1, x-1, -dy);
				}
				for (x++; x<=x2 && (pts[y + x*wx2] || getPixel(x, y)!=oc); x++);
				l = x;
			} while (x<=x2);
		}
	}

	/* Line thickness (defaults to 1). Affects lines, ellipses,
	   rectangles, polygons and so forth. */
	public void setThickness(int thickness) {
		thick = thickness;
	}

	public int getRed(final int c) {
		return trueColor ? GdUtils.trueColorGetRed(c) : red[(c)];
	}

	public int getGreen(final int c) {
		return trueColor ? GdUtils.trueColorGetGreen(c) : green[(c)];
	}

	public int getBlue(final int c) {
		return trueColor ? GdUtils.trueColorGetBlue(c) : blue[(c)];
	}

	public int getAlpha(final int c) {
		return trueColor ? GdUtils.trueColorGetAlpha(c) : alpha[(c)];
	}

	public void setBrush(final GdImage brush) {
		int i;
		this.brush = brush;
		if ((!trueColor) && (!this.brush.trueColor)) {
			for (i = 0; (i < brush.colorsTotal); i++) {
				int index;
				index = colorResolveAlpha(
						brush.getRed(i),
						brush.getGreen(i),
						brush.getBlue(i),
						brush.getAlpha(i));
				brushColorMap[i] = index;
			}
		}
	}

	public void setTile(final GdImage tile) {
		int i;
		this.tile = tile;
		if ((!trueColor) && (!tile.trueColor)) {
			for (i = 0; (i < tile.colorsTotal); i++) {
				int index;
				index = colorResolveAlpha(
						tile.getRed(i),
						tile.getGreen(i),
						tile.getBlue(i),
						tile.getAlpha(i));
				tileColorMap[i] = index;
			}
		}
	}

	public void setAntiAliased(final int c) {
		AA = 1;
		AA_color = c;
		AA_dont_blend = -1;
	}

	public void setAntiAliasedDontBlend(final int c, final int dont_blend) {
		AA = 1;
		AA_color = c;
		AA_dont_blend = dont_blend;
	}

	/* On or off (1 or 0) for all three of these. */
	public void setInterlace(final int interlaceArg) {
		interlace = interlaceArg;
	}

	/* Image comparison definitions */
	public int compare(final GdImage im1, final GdImage im2) {
		int x, y;
		int p1, p2;
		int cmpStatus = 0;
		int sx, sy;

		if (im1.interlace != im2.interlace) {
			cmpStatus |= GdUtils.GD_CMP_INTERLACE;
		}

		if (im1.transparent != im2.transparent) {
			cmpStatus |= GdUtils.GD_CMP_TRANSPARENT;
		}

		if (im1.trueColor != im2.trueColor) {
			cmpStatus |= GdUtils.GD_CMP_TRUECOLOR;
		}

		sx = im1.sx;
		if (im1.sx != im2.sx) {
			cmpStatus |= GdUtils.GD_CMP_SIZE_X + GdUtils.GD_CMP_IMAGE;
			if (im2.sx < im1.sx) {
				sx = im2.sx;
			}
		}

		sy = im1.sy;
		if (im1.sy != im2.sy) {
			cmpStatus |= GdUtils.GD_CMP_SIZE_Y + GdUtils.GD_CMP_IMAGE;
			if (im2.sy < im1.sy) {
				sy = im2.sy;
			}
		}

		if (im1.colorsTotal != im2.colorsTotal) {
			cmpStatus |= GdUtils.GD_CMP_NUM_COLORS;
		}

		for (y = 0; (y < sy); y++) {
			for (x = 0; (x < sx); x++) {
				p1 = im1.getPixel(x, y);
				p2 = im2.getPixel(x, y);
				if (im1.getRed(p1) != im2.getRed(p2)) {
					cmpStatus |= GdUtils.GD_CMP_COLOR + GdUtils.GD_CMP_IMAGE;
					break;
				}
				if (im1.getGreen(p1) != im2.getGreen(p2)) {
					cmpStatus |= GdUtils.GD_CMP_COLOR + GdUtils.GD_CMP_IMAGE;
					break;
				}
				if (im1.getBlue(p1) != im2.getBlue(p2)) {
					cmpStatus |= GdUtils.GD_CMP_COLOR + GdUtils.GD_CMP_IMAGE;
					break;
				}
			/* Soon we'll add alpha channel to palettes */
				if (im1.getAlpha(p1) != im2.getAlpha(p2)) {
					cmpStatus |= GdUtils.GD_CMP_COLOR + GdUtils.GD_CMP_IMAGE;
					break;
				}
			}
			if ((cmpStatus & GdUtils.GD_CMP_COLOR) != 0) {
				break;
			};
		}

		return cmpStatus;
	}


	public void setAlphaBlending(final GdEffect alphaBlendingArg) {
		this.alphaBlendingFlag = alphaBlendingArg;
	}

	public void setSaveAlpha(final int saveAlphaArg) {
		this.saveAlphaFlag = saveAlphaArg;
	}

	public void setClip(int x1, int y1, int x2, int y2) {
		if (x1 < 0) {
			x1 = 0;
		}
		if (x1 >= sx) {
			x1 = sx - 1;
		}
		if (x2 < 0) {
			x2 = 0;
		}
		if (x2 >= sx) {
			x2 = sx - 1;
		}
		if (y1 < 0) {
			y1 = 0;
		}
		if (y1 >= sy) {
			y1 = sy - 1;
		}
		if (y2 < 0) {
			y2 = 0;
		}
		if (y2 >= sy) {
			y2 = sy - 1;
		}
		cx1 = x1;
		cy1 = y1;
		cx2 = x2;
		cy2 = y2;
	}

	public GdClipRectangle getClip() {
		return new GdClipRectangle(cx1, cy1, cx2, cy2);
	}

	public void setResolution(final int res_x, final int res_y) {
		if (res_x > 0) this.res_x = res_x;
		if (res_y > 0) this.res_y = res_y;
	}


	/* convert a palette image to true color */
	public void paletteToTrueColor() {
		int y;
		int yy;

		if (trueColor) {
			return;
		}

		int x;
		final int sy = this.sy;
		final int sx = this.sx;

		tpixels = new int[sy][sx];

		for (y = 0; y < sy; y++) {
			int[] src_row = pixels[y];
			int[] dst_row = tpixels[y];

			for (x = 0; x < sx; x++) {
				final int c = src_row[x];
				if (c == transparent) {
					dst_row[x] = GdUtils.trueColorMixAlpha(0, 0, 0, 127);
				} else {
					dst_row[x] = GdUtils.trueColorMixAlpha(red[c], green[c], blue[c], alpha[c]);
				}
			}
		}

		/* free old palette buffer */
		pixels = null;
		trueColor = true;
		alphaBlendingFlag = GdEffect.REPLACE;
		saveAlphaFlag = 1;
	}

	/* Assumes opaque is the preferred alpha channel value */
	public int findColorClosest(final int r, final int g, final int b) {
		return findColorClosestAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	/* Closest match taking all four parameters into account.
	   A slightly different color with the same transparency
	   beats the exact same color with radically different
	   transparency */
	public int findColorClosestAlpha(final int r, final int g, final int b, final int a) {
		int i;
		long rd, gd, bd, ad;
		int ct = (-1);
		boolean first = true;
		long mindist = 0;
		if (trueColor) {
			return GdUtils.trueColorMixAlpha(r, g, b, a);
		}
		for (i = 0; (i < (colorsTotal)); i++) {
			long dist;
			if (open[i]) {
				continue;
			}
			rd = (red[i] - r);
			gd = (green[i] - g);
			bd = (blue[i] - b);
		/* gd 2.02: whoops, was - b (thanks to David Marwood) */
		/* gd 2.16: was blue rather than alpha! Geez! Thanks to
		   Artur Jakub Jerzak */
			ad = (alpha[i] - a);
			dist = rd * rd + gd * gd + bd * bd + ad * ad;
			if (first || (dist < mindist)) {
				mindist = dist;
				ct = i;
				first = false;
			}
		}
		return ct;
	}



	public GdImage imageClone() {
		GdImage dst;
		int i, x;

		if (this.trueColor) {
			dst = new GdImage(this.sx , this.sy, GdImageColorType.TRUE_COLOR);
		} else {
			dst = new GdImage(this.sx , this.sy, GdImageColorType.PALETTE_BASED_COLOR);
		}

		if (!this.trueColor) {
			dst.colorsTotal = this.colorsTotal;
			for (i = 0; i < GdUtils.MAX_COLORS; i++) {
				dst.red[i]   = this.red[i];
				dst.green[i] = this.green[i];
				dst.blue[i]  = this.blue[i];
				dst.alpha[i] = this.alpha[i];
				dst.open[i]  = this.open[i];
			}
			for (i = 0; i < this.sy; i++) {
				for (x = 0; x < this.sx; x++) {
					dst.pixels[i][x] = this.pixels[i][x];
				}
			}
		} else {
			for (i = 0; i < this.sy; i++) {
				for (x = 0; x < this.sx; x++) {
					dst.tpixels[i][x] = this.tpixels[i][x];
				}
			}
		}

		if (this.styleLength > 0) {
			dst.styleLength = this.styleLength;
			dst.stylePos    = this.stylePos;
			for (i = 0; i < this.styleLength; i++) {
				dst.style[i] = this.style[i];
			}
		}

		dst.interlace   = this.interlace;

		dst.alphaBlendingFlag = this.alphaBlendingFlag;
		dst.saveAlphaFlag     = this.saveAlphaFlag;
		dst.AA                = this.AA;
		dst.AA_color          = this.AA_color;
		dst.AA_dont_blend     = this.AA_dont_blend;

		dst.cx1 = this.cx1;
		dst.cy1 = this.cy1;
		dst.cx2 = this.cx2;
		dst.cy2 = this.cy2;

		dst.res_x = this.res_x;
		dst.res_y = this.res_x;

		dst.paletteQuantizationMethod     = this.paletteQuantizationMethod;
		dst.paletteQuantizationSpeed      = this.paletteQuantizationSpeed;
		dst.paletteQuantizationMinQuality = this.paletteQuantizationMinQuality;
		dst.paletteQuantizationMinQuality = this.paletteQuantizationMinQuality;

		dst.interpolation_id = this.interpolation_id;
//		dst.interpolation    = this.interpolation;

		if (this.brush != null) {
			dst.brush = this.brush.imageClone();
		}

		if (this.tile != null) {
			dst.tile = this.tile.imageClone();
		}

		if (this.style != null) {
			dst.setStyle(this.style, this.styleLength);
		}

		for (i = 0; i < GdUtils.MAX_COLORS; i++) {
			dst.brushColorMap[i] = this.brushColorMap[i];
			dst.tileColorMap[i] = this.tileColorMap[i];
		}

		if (this.polyAllocated > 0) {
			dst.polyAllocated = this.polyAllocated;
			for (i = 0; i < this.polyAllocated; i++) {
				dst.polyInts[i] = this.polyInts[i];
			}
		}

		return dst;
	}

	public static void imageCopy(final GdImage dst, final GdImage src, final int dstX, final int dstY, final int srcX,
						  final int srcY, final int w, final int h) {
		if (dst.trueColor) {
		/* 2.0: much easier when the destination is truecolor. */
		/* 2.0.10: needs a transparent-index check that is still valid if
		 * the source is not truecolor. Thanks to Frank Warmerdam.
		 */

			if (src.trueColor) {
				for (int y = 0; (y < h); y++) {
					for (int x = 0; (x < w); x++) {
						int c = src.getTrueColorPixel(srcX + x, srcY + y);
						if (c != src.transparent) {
							dst.setPixel(dstX + x, dstY + y, c);
						}
					}
				}
			} else {
			/* source is palette based */
				for (int y = 0; (y < h); y++) {
					for (int x = 0; (x < w); x++) {
						int c = src.getPixel(srcX + x, srcY + y);
						if (c != src.transparent) {
							dst.setPixel(dstX + x, dstY + y, GdUtils.trueColorMixAlpha(
									src.red[c], src.green[c], src.blue[c], src.alpha[c]));
						}
					}
				}
			}
			return;
		}


		int[] colorMap = new int[GdUtils.MAX_COLORS];

		for (int i = 0; (i < GdUtils.MAX_COLORS); i++) {
			colorMap[i] = (-1);
		}
		int toy = dstY;
		for (int y = srcY; (y < (srcY + h)); y++) {
			int tox = dstX;
			for (int x = srcX; (x < (srcX + w)); x++) {
				int nc;
				int mapTo;
				int c = src.getPixel(x, y);
			/* Added 7/24/95: support transparent copies */
				if (src.transparent == c) {
					tox++;
					continue;
				}
			/* Have we established a mapping for this color? */
				if (src.trueColor) {
				/* 2.05: remap to the palette available in the
				 destination image. This is slow and
				 works badly, but it beats crashing! Thanks
				 to Padhrig McCarthy. */
					mapTo = dst.colorResolveAlpha(
							GdUtils.trueColorGetRed(c),
							GdUtils.trueColorGetGreen(c),
							GdUtils.trueColorGetBlue(c),
							GdUtils.trueColorGetAlpha(c));
				} else if (colorMap[c] == (-1)) {
				/* If it's the same image, mapping is trivial */
					if (dst == src) {
						nc = c;
					} else {
					/* Get best match possible. This
					   function never returns error. */
						nc = dst.colorResolveAlpha(
								src.red[c], src.green[c],
								src.blue[c], src.alpha[c]);
					}
					colorMap[c] = nc;
					mapTo = colorMap[c];
				} else {
					mapTo = colorMap[c];
				}
				dst.setPixel(tox, toy, mapTo);
				tox++;
			}
			toy++;
		}
	}

	/* This function is a substitute for real alpha channel operations,
	   so it doesn't pay attention to the alpha channel. */
	public static void imageCopyMerge(final GdImage dst, final GdImage src, final int dstX, final int dstY,
									  final int srcX, final int srcY, final int w, final int h, final int pct) {
		int toy = dstY;
		for (int y = srcY; (y < (srcY + h)); y++) {
			int tox = dstX;
			for (int x = srcX; (x < (srcX + w)); x++) {
				int nc;
				final int c = src.getPixel(x, y);
			/* Added 7/24/95: support transparent copies */
				if (src.transparent == c) {
					tox++;
					continue;
				}
			/* If it's the same image, mapping is trivial */
				if (dst == src) {
					nc = c;
				} else {
					final int dc = dst.getPixel(tox, toy);

					final int ncR = (int) (src.getRed(c) * (pct / 100.0)
							+ dst.getRed(dc) * ((100 - pct) / 100.0));
					final int ncG = (int) (src.getGreen(c) * (pct / 100.0)
							+ dst.getGreen(dc) * ((100 - pct) / 100.0));
					final int ncB = (int) (src.getBlue(c) * (pct / 100.0)
							+ dst.getBlue(dc) * ((100 - pct) / 100.0));

				/* Find a reasonable color */
					nc = dst.colorResolve(ncR, ncG, ncB);
				}
				dst.setPixel(tox, toy, nc);
				tox++;
			}
			toy++;
		}
	}

	/* This function is a substitute for real alpha channel operations,
	   so it doesn't pay attention to the alpha channel. */
	public static void imageCopyMergeGray(GdImage dst, GdImage src, int dstX, int dstY,
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
				c = src.getPixel(x, y);
			/* Added 7/24/95: support transparent copies */
				if (src.transparent == c) {
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
					dc = dst.getPixel(tox, toy);
					g = (int) (0.29900 * dst.getRed(dc)
							+ 0.58700 * dst.getGreen(dc) + 0.11400 * dst.getBlue(dc));

					ncR = (int) (src.getRed(c) * (pct / 100.0)
							+ g * ((100 - pct) / 100.0));
					ncG = (int) (src.getGreen(c) * (pct / 100.0)
							+ g * ((100 - pct) / 100.0));
					ncB = (int) (src.getBlue(c) * (pct / 100.0)
							+ g * ((100 - pct) / 100.0));

				/* First look for an exact match */
					nc = dst.colorExact(ncR, ncG, ncB);
					if (nc == (-1)) {
					/* No, so try to allocate it */
						nc = dst.colorAllocate(ncR, ncG, ncB);
					/* If we're out of colors, go for the
					   closest color */
						if (nc == (-1)) {
							nc = dst.findColorClosest(ncR, ncG, ncB);
						}
					}
				}
				dst.setPixel(tox, toy, nc);
				tox++;
			}
			toy++;
		}
	}

/* Stretches or shrinks to fit, as needed. Does NOT attempt
   to average the entire set of source pixels that scale down onto the
   destination pixel. */
	public static void imageCopyResized(final GdImage dst, final GdImage src, final int dstX, final int dstY,
								   final int srcX, final int srcY, final int dstW, final int dstH,
								   final int srcW, final int srcH) {
		int[] colorMap = new int[GdUtils.MAX_COLORS];
	/* Stretch vectors */
	/* We only need to use floating point to determine the correct
	   stretch vector for one line's worth. */
		int[] stx = new int[srcW];
		int[] sty = new int[srcH];

	/* Fixed by Mao Morimoto 2.0.16 */
		for (int i = 0; (i < srcW); i++) {
			stx[i] = dstW * (i + 1) / srcW - dstW * i / srcW;
		}
		for (int i = 0; (i < srcH); i++) {
			sty[i] = dstH * (i + 1) / srcH - dstH * i / srcH;
		}
		for (int i = 0; (i < GdUtils.MAX_COLORS); i++) {
			colorMap[i] = (-1);
		}
		int toy = dstY;
		for (int y = srcY; (y < (srcY + srcH)); y++) {
			for (int ydest = 0; (ydest < sty[y - srcY]); ydest++) {
				int tox = dstX;
				for (int x = srcX; (x < (srcX + srcW)); x++) {
					int nc = 0;
					int mapTo;
					if (stx[x - srcX] == 0) {
						continue;
					}
					if (dst.trueColor) {
					/* 2.0.9: Thorben Kundinger: Maybe the source image is not
					   a truecolor image */
						if (!src.trueColor) {
							int tmp = src.getPixel(x, y);
							mapTo = src.getTrueColorPixel(x, y);
							if (src.transparent == tmp) {
							/* 2.0.21, TK: not tox++ */
								tox += stx[x - srcX];
								continue;
							}
						} else {
						/* TK: old code follows */
							mapTo = src.getTrueColorPixel(x, y);
						/* Added 7/24/95: support transparent copies */
							if (src.transparent == mapTo) {
							/* 2.0.21, TK: not tox++ */
								tox += stx[x - srcX];
								continue;
							}
						}
					} else {
						final int c = src.getPixel(x, y);
					/* Added 7/24/95: support transparent copies */
						if (src.transparent == c) {
							tox += stx[x - srcX];
							continue;
						}
						if (src.trueColor) {
						/* Remap to the palette available in the
						   destination image. This is slow and
						   works badly. */
							mapTo = dst.colorResolveAlpha(
									GdUtils.trueColorGetRed(c),
									GdUtils.trueColorGetGreen(c),
									GdUtils.trueColorGetBlue(c),
									GdUtils.trueColorGetAlpha(c));
						} else {
						/* Have we established a mapping for this color? */
							if (colorMap[c] == (-1)) {
							/* If it's the same image, mapping is trivial */
								if (dst == src) {
									nc = c;
								} else {
								/* Find or create the best match */
								/* 2.0.5: can't use gdTrueColorGetRed, etc with palette */
									nc = dst.colorResolveAlpha(
											src.getRed(c),
											src.getGreen(c),
											src.getBlue(c),
											src.getAlpha(c));
								}
								colorMap[c] = nc;
							}
							mapTo = colorMap[c];
						}
					}
					for (int i = 0; (i < stx[x - srcX]); i++) {
						dst.setPixel(tox, toy, mapTo);
						tox++;
					}
				}
				toy++;
			}
		}
	}

/* gd 2.0.8: gdImageCopyRotated is added. Source
	is a rectangle, with its upper left corner at
	srcX and srcY. Destination is the *center* of
		the rotated copy. Angle is in degrees, same as
		gdImageArc. Floating point destination center
	coordinates allow accurate rotation of
	objects of odd-numbered width or height. */

/* gd 2.0.8: gdImageCopyRotated is added. Source
   is a rectangle, with its upper left corner at
   srcX and srcY. Destination is the *center* of
   the rotated copy. Angle is in degrees, same as
   gdImageArc. Floating point destination center
   coordinates allow accurate rotation of
   objects of odd-numbered width or height. */
	public static void imageCopyRotated(final GdImage dst, final GdImage src, final double dstX, final double dstY,
								 final int srcX, final int srcY,
								 final int srcWidth, final int srcHeight, final int angle) {
		double radius = sqrt(srcWidth * srcWidth + srcHeight * srcHeight);
		double aCos = cos(angle * .0174532925);
		double aSin = sin(angle * .0174532925);
		double scX = srcX + ((double) srcWidth) / 2;
		double scY = srcY + ((double) srcHeight) / 2;
		int[] cmap = new int[GdUtils.MAX_COLORS];
		int i;

	/*
		 2.0.34: transparency preservation. The transparentness of
		 the transparent color is more important than its hue.
	*/
		if (src.transparent != -1) {
			if (dst.transparent == -1) {
				dst.transparent = src.transparent;
			}
		}

		for (i = 0; (i < GdUtils.MAX_COLORS); i++) {
			cmap[i] = (-1);
		}
		for (double dy = dstY - radius; (dy <= dstY + radius); dy++) {
			for (double dx = dstX - radius; (dx <= dstX + radius); dx++) {
				double sxd = (dx - dstX) * aCos - (dy - dstY) * aSin;
				double syd = (dy - dstY) * aCos + (dx - dstX) * aSin;
				int sx = (int) (sxd + scX);
				int sy = (int) (syd + scY);
				if ((sx >= srcX) && (sx < srcX + srcWidth) &&
						(sy >= srcY) && (sy < srcY + srcHeight)) {
					int c = src.getPixel(sx, sy);
				/* 2.0.34: transparency wins */
					if (c == src.transparent) {
						dst.setPixel((int) dx, (int) dy, dst.transparent);
					} else if (!src.trueColor) {
					/* Use a table to avoid an expensive
					   lookup on every single pixel */
						if (cmap[c] == -1) {
							cmap[c] = dst.colorResolveAlpha(
									src.getRed(c),
									src.getGreen(c),
									src.getBlue(c),
									src.getAlpha(c));
						}
						dst.setPixel((int) dx, (int) dy, cmap[c]);
					} else {
						dst.setPixel((int) dx, (int) dy, dst.colorResolveAlpha(
								src.getRed(c),
								src.getGreen(c),
								src.getBlue(c),
								src.getAlpha(c)));
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

//	#define floor2(exp) ((long) exp)
	private static long floor2(final double exp) {
		return (long) exp;
	}
/*#define floor2(exp) floor(exp)*/

/* gd 2.0: stretches or shrinks to fit, as needed. When called with a
   truecolor destination image, this function averages the
   entire set of source pixels that scale down onto the
   destination pixel, taking into account what portion of the
   destination pixel each source pixel represents. This is a
   floating point operation, but this is not a performance issue
   on modern hardware, except for some embedded devices. If the
   destination is a palette image, gdImageCopyResized is
   substituted automatically. */
	public static void imageCopyResampled(final GdImage dst, final GdImage src,
										  final int dstX, final int dstY, final int srcX, final int srcY,
										  final int dstW, final int dstH, final int srcW, final int srcH) {
		double sy1, sy2, sx1, sx2;
		if (!dst.trueColor) {
			imageCopyResized(dst, src, dstX, dstY, srcX, srcY, dstW, dstH, srcW, srcH);
			return;
		}
		for (int y = dstY; (y < dstY + dstH); y++) {
			sy1 = ((double) y - (double) dstY) * (double) srcH / (double) dstH;
			sy2 = ((double) (y + 1) - (double) dstY) * (double) srcH /
					(double) dstH;
			for (int x = dstX; (x < dstX + dstW); x++) {
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
						p = src.getTrueColorPixel(
								(int) sx + srcX,
								(int) sy + srcY);
						red += GdUtils.trueColorGetRed(p) * pcontribution;
						green += GdUtils.trueColorGetGreen(p) * pcontribution;
						blue += GdUtils.trueColorGetBlue(p) * pcontribution;
						alpha += GdUtils.trueColorGetAlpha(p) * pcontribution;
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
				if (alpha > GdUtils.ALPHA_MAX) {
					alpha = GdUtils.ALPHA_MAX;
				}
				dst.setPixel(
						x, y,
						GdUtils.trueColorMixAlpha((int) red,
								(int) green,
								(int) blue, (int) alpha));
			}
		}
	}

	private boolean isColorMatch(final int col1, final int col2, final float threshold) {
		final int dr = getRed(col1) - getRed(col2);
		final int dg = getGreen(col1) - getGreen(col2);
		final int db = getBlue(col1) - getBlue(col2);
		final int da = getAlpha(col1) - getAlpha(col2);
		final int dist = dr * dr + dg * dg + db * db + da * da;

		return (100.0 * dist / 195075) < threshold;
	}

	public int colorReplace(final int src, final int dst) {
		if (src == dst) {
			return 0;
		}

		int n = 0;

		if (trueColor) {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					if (getTrueColorPixel(x, y) == src) {
						setPixel(x, y, dst);
						n++;
					}
				}
			}
		} else {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					if (getPalettePixel(x, y) == src) {
						setPixel(x, y, dst);
						n++;
					}
				}
			}
		}

		return n;
	}

	public int colorReplaceThreshold(final int src, final int dst, final float threshold) {
		if (src == dst) {
			return 0;
		}

		int n = 0;

		if (trueColor) {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					if (isColorMatch(src, getTrueColorPixel(x, y), threshold)) {
						setPixel(x, y, dst);
						n++;
					}
				}
			}
		} else {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					if (isColorMatch(src, getPalettePixel(x, y), threshold)) {
						setPixel(x, y, dst);
						n++;
					}
				}
			}
		}

		return n;
	}

	public int colorReplaceArray(final int[] src, final int[] dst) {
		if (src.length != dst.length || src == dst) {
			return 0;
		}
		if (src.length == 1) {
			return colorReplace(src[0], dst[0]);
		}

		final Map<Integer, Integer> base = new HashMap<Integer, Integer>();
		for (int i = 0; i < src.length; i++) {
			base.put(src[i], dst[i]);
		}

		int n = 0;

		if (trueColor) {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					final int c = getTrueColorPixel(x, y);
					if (base.containsKey(c)) {
						setPixel(x, y, base.get(c));
						n++;
					}
				}
			}
		} else {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					final int c = getPalettePixel(x, y);
					if (base.containsKey(c)) {
						setPixel(x, y, base.get(c));
						n++;
					}
				}
			}
		}

		return n;
	}

	public int colorReplaceCallback(GdCallbackImageColor callback) {
		if (callback == null) {
			return 0;
		}

		int n = 0;

		if (trueColor) {
			for (int y = cy1; y <= cy2; y++) {
				for (int x = cx1; x <= cx2; x++) {
					final int c = getTrueColorPixel(x, y);
					final int d = callback.callbackImageColor(this, c);
					if (d != c) {
						setPixel(x, y, d);
						n++;
					}
				}
			}
		} else { /* palette */
			int len = 0;

			int[] sarr = new int[colorsTotal];
			for (int c = 0; c < colorsTotal; c++) {
				if (!open[c]) {
					sarr[len++] = c;
				}
			}
			final int[] darr = new int[len];
			for (int k = 0; k < len; k++) {
				darr[k] = callback.callbackImageColor(this, sarr[k]);
			}
			n = colorReplaceArray(sarr, darr);
		}
		return n;
	}

	public void writeChar(final GdFont f, final int x, final int y, final int c, final int color) {
		int cx = 0;
		int cy = 0;
		if ((c < f.offset) || (c >= (f.offset + f.nchars))) {
			return;
		}
		final int fline = (c - f.offset) * f.h * f.w;
		for (int py = y; (py < (y + f.h)); py++) {
			for (int px = x; (px < (x + f.w)); px++) {
				if (f.data[fline + cy * f.w + cx] != 0) {
					setPixel(px, py, color);
				}
				cx++;
			}
			cx = 0;
			cy++;
		}
	}

	public void writeCharUp(final GdFont f, final int x, final int y, final int c, final int color) {
		int cx = 0;
		int cy = 0;
		if ((c < f.offset) || (c >= (f.offset + f.nchars))) {
			return;
		}

		final int fline = (c - f.offset) * f.h * f.w;
		for (int py = y; (py > (y - f.w)); py--) {
			for (int px = x; (px < (x + f.h)); px++) {
				if (f.data[fline + cy * f.w + cx] != 0) {
					setPixel(px, py, color);
				}
				cy++;
			}
			cy = 0;
			cx++;
		}
	}

	public void writeString(final GdFont f, int x, final int y, final String s, final int color) {
		final int l = s.length();
		for (int i = 0; (i < l); i++) {
			writeChar(f, x, y, s.charAt(i), color);
			x += f.w;
		}
	}

	public void writeStringUp(final GdFont f, final int x, int y, final String s, final int color) {
		final int l = s.length();
		for (int i = 0; (i < l); i++) {
			writeCharUp(f, x, y, s.charAt(i), color);
			y -= f.w;
		}
	}

	public void flipVertical() {
		if (trueColor) {
			for (int y = 0; y < sy / 2; y++) {
				int[] row_dst = tpixels[y];
				int[] row_src = tpixels[sy - 1 - y];
				for (int x = 0; x < sx; x++) {
					int p;
					p = row_dst[x];
					row_dst[x] = tpixels[sy - 1 - y][x];
					row_src[x] = p;
				}
			}
		} else {
			for (int y = 0; y < sy / 2; y++) {
				for (int x = 0; x < sx; x++) {
					int p = tpixels[y][x];
					tpixels[y][x] =	tpixels[sy - 1 - y][x];
					tpixels[sy - 1 - y][x] = p;
				}
			}
		}
	}

	public void flipHorizontal() {
		if (trueColor) {
			for (int y = 0; y < sy; y++) {
				final int[] row = tpixels[y];
				for (int x = 0; x < (sx >> 1); x++) {
					final int z = sx - 1 - x;
					final int tmp = row[x];
					row[x] = row[z];
					row[z] = tmp;
				}
			}
		} else {
			for (int y = 0; y < sy; y++) {
				final int[] row = pixels[y];
				for (int x = 0; x < (sx >> 1); x++) {
					final int z = sx - 1 - x;
					final int tmp = row[x];
					row[x] = row[z];
					row[z] = tmp;
				}
			}
		}
	}

	public void flipBoth() {
		flipVertical();
		flipHorizontal();
	}

/* This code is taken from http://www.acm.org/jgt/papers/SmithLyons96/hwb_rgb.html, an article
 * on colour conversion to/from RBG and HWB colour systems.
 * It has been modified to return the converted value as a * parameter.
 */

//	#define RETURN_HWB(h, w, b) {HWB->H = h; HWB->W = w; HWB->B = b; return HWB;}
//	#define RETURN_RGB(r, g, b) {RGB->R = r; RGB->G = g; RGB->B = b; return RGB;}
	private static double HWB_UNDEFINED = -1;

	private static double MIN(final double a, final double b) {
		return ((a)<(b)?(a):(b));
	}

	private static double MIN3(final double a,final double b,final double c) {
		return ((a)<(b)?(MIN(a,c)):(MIN(b,c)));
	}

	private static double MAX(final double a, final double b) {
		return ((a)<(b)?(b):(a));
	}

	private static double MAX3(final double a, final double b, final double c) {
		return ((a)<(b)?(MAX(b,c)):(MAX(a,c)));
	}


	/*
	 * Theoretically, hue 0 (pure red) is identical to hue 6 in these transforms. Pure
	 * red always maps to 6 in this implementation. Therefore UNDEFINED can be
	 * defined as 0 in situations where only unsigned numbers are desired.
	 */
	static class GdRGBType {
		double R, G, B;
		public GdRGBType(final double r, final double g, final double b) {
			this.R = r/255.0; this.G = g/255.0; this.B = b/255.0;
		}
	}

	static class GdHWBType {
		double H, W, B;
		public GdHWBType(final double h, final double w, final double b) {
			this.H = h; this.W = w; this.B = b;
		}
	}

	private static GdHWBType RGB_to_HWB(GdRGBType RGB) {

	/*
	 * RGB are each on [0, 1]. W and B are returned on [0, 1] and H is
	 * returned on [0, 6]. Exception: H is returned UNDEFINED if W == 1 - B.
	 */

		double R = RGB.R, G = RGB.G, B = RGB.B, w, v, b, f;
		int i;

		w = MIN3(R, G, B);
		v = MAX3(R, G, B);
		b = 1 - v;
		if (v == w)
			return new GdHWBType(HWB_UNDEFINED, w, b);
		f = (R == w) ? G - B : ((G == w) ? B - R : R - G);
		i = (R == w) ? 3 : ((G == w) ? 5 : 1);
		return new GdHWBType(i - f / (v - w), w, b);
	}

	static double HWB_Diff(final int r1, final int g1, final int b1, final int r2, final int g2, final int b2) {
		double diff;

		final GdRGBType RGB1 = new GdRGBType(r1, g1, b1);
		final GdRGBType RGB2 = new GdRGBType(r2, g2, b2);

		final GdHWBType HWB1 = RGB_to_HWB(RGB1);
		final GdHWBType HWB2 = RGB_to_HWB (RGB2);

	/*
	 * I made this bit up; it seems to produce OK results, and it is certainly
	 * more visually correct than the current RGB metric. (PJW)
	 */

		if ((HWB1.H == HWB_UNDEFINED) || (HWB2.H == HWB_UNDEFINED)) {
			diff = 0;			/* Undefined hues always match... */
		} else {
			diff = (HWB1.H - HWB2.H);
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

	/* An alternate method */
	public int colorClosestHWB(final int r, final int g, final int b) {
		int i;
	/* long rd, gd, bd; */
		int ct = (-1);
		boolean first = true;
		double mindist = 0;
		if (trueColor) {
			return GdUtils.trueColorMix(r, g, b);
		}
		for (i = 0; (i < (colorsTotal)); i++) {
			double dist;
			if (open[i]) {
				continue;
			}
			dist = HWB_Diff (red[i], green[i], blue[i], r, g, b);
			if (first || (dist < mindist)) {
				mindist = dist;
				ct = i;
				first = false;
			}
		}
		return ct;
	}

	/* Specifies a color index (if a palette image) or an
	   RGB color (if a truecolor image) which should be
	   considered 100% transparent. FOR TRUECOLOR IMAGES,
	   THIS IS IGNORED IF AN ALPHA CHANNEL IS BEING
	   SAVED. Use gdImageSaveAlpha(im, 0); to
	   turn off the saving of a full alpha channel in
	   a truecolor image. Note that gdImageColorTransparent
	   is usually compatible with older browsers that
	   do not understand full alpha channels well. TBB */
	public void gdImageColorTransparent(int color) {
		if (!trueColor) {
			if((color < -1) || (color >= GdUtils.MAX_COLORS)) {
				return;
			}
			if (transparent != -1) {
				alpha[transparent] = GdUtils.ALPHA_OPAQUE;
			}
			if (color != -1) {
				alpha[color] = GdUtils.ALPHA_TRANSPARENT;
			}
		}
		transparent = color;
	}

	public static void paletteCopy(GdImage to, GdImage from) {
		int i;
		int x, y, p;
		int[] xlate = new int[256];
		if (to.trueColor) {
			return;
		}
		if (from.trueColor) {
			return;
		}

		for (i = 0; i < 256; i++) {
			xlate[i] = -1;
		};

		for (y = 0; y < (to.sy); y++) {
			for (x = 0; x < (to.sx); x++) {
			/* Optimization: no gdImageGetPixel */
				p = to.pixels[y][x];
				if (xlate[p] == -1) {
				/* This ought to use HWB, but we don't have an alpha-aware
				   version of that yet. */
					xlate[p] = from.findColorClosestAlpha(to.red[p], to.green[p], to.blue[p], to.alpha[p]);
				/*printf("Mapping %d (%d, %d, %d, %d) to %d (%d, %d, %d, %d)\n", */
				/*      p,  to->red[p], to->green[p], to->blue[p], to->alpha[p], */
				/*      xlate[p], from->red[xlate[p]], from->green[xlate[p]], from->blue[xlate[p]], from->alpha[xlate[p]]); */
				};
			/* Optimization: no gdImageSetPixel */
				to.pixels[y][x] = xlate[p];
			};
		};

		for (i = 0; (i < (from.colorsTotal)); i++) {
		/*printf("Copying color %d (%d, %d, %d, %d)\n", i, from->red[i], from->blue[i], from->green[i], from->alpha[i]); */
			to.red[i] = from.red[i];
			to.blue[i] = from.blue[i];
			to.green[i] = from.green[i];
			to.alpha[i] = from.alpha[i];
			to.open[i] = false;
		};

		for (i = from.colorsTotal; (i < to.colorsTotal); i++) {
			to.open[i] = true;
		};

		to.colorsTotal = from.colorsTotal;
	}
/*
 * The two pass scaling function is based on:
 * Filtered Image Rescaling
 * Based on Gems III
 *  - Schumacher general filtered image rescaling
 * (pp. 414-424)
 * by Dale Schumacher
 *
 * 	Additional changes by Ray Gardener, Daylon Graphics Ltd.
 * 	December 4, 1999
 *
 * 	Ported to libgd by Pierre Joye. Support for multiple channels
 * 	added (argb for now).
 *
 * 	Initial sources code is avaibable in the Gems Source Code Packages:
 * 	http://www.acm.org/pubs/tog/GraphicsGems/GGemsIII.tar.gz
 *
 */

/*
	Summary:

		- Horizontal filter contributions are calculated on the fly,
		  as each column is mapped from src to dst image. This lets
		  us omit having to allocate a temporary full horizontal stretch
		  of the src image.

		- If none of the src pixels within a sampling region differ,
		  then the output pixel is forced to equal (any of) the source pixel.
		  This ensures that filters do not corrupt areas of constant color.

		- Filter weight contribution results, after summing, are
		  rounded to the nearest pixel color value instead of
		  being casted to ILubyte (usually an int or char). Otherwise,
		  artifacting occurs.

*/

/*
	Additional functions are available for simple rotation or up/downscaling.
	downscaling using the fixed point implementations are usually much faster
	than the existing gdImageCopyResampled while having a similar or better
	quality.

	For image rotations, the optimized versions have a lazy antialiasing for
	the edges of the images. For a much better antialiased result, the affine
	function is recommended.
*/

/*
TODO:
 - Optimize pixel accesses and loops once we have continuous buffer
 - Add scale support for a portion only of an image (equivalent of copyresized/resampled)
 */

	#define CLAMP(x, low, high)  (((x) > (high)) ? (high) : (((x) < (low)) ? (low) : (x)))

/* only used here, let do a generic fixed point integers later if required by other
   part of GD */
	typedef long gdFixed;
/* Integer to fixed point */
	#define gd_itofx(x) ((x) << 8)

/* Float to fixed point */
			#define gd_ftofx(x) (long)((x) * 256)

/*  Double to fixed point */
			#define gd_dtofx(x) (long)((x) * 256)

/* Fixed point to integer */
			#define gd_fxtoi(x) ((x) >> 8)

/* Fixed point to float */
			# define gd_fxtof(x) ((float)(x) / 256)

/* Fixed point to double */
			#define gd_fxtod(x) ((double)(x) / 256)

/* Multiply a fixed by a fixed */
			#define gd_mulfx(x,y) (((x) * (y)) >> 8)

/* Divide a fixed by a fixed */
			#define gd_divfx(x,y) (((x) << 8) / (y))

	typedef struct
	{
		double *Weights;  /* Normalized weights of neighboring pixels */
		int Left,Right;   /* Bounds of source pixels window */
	} ContributionType;  /* Contirbution information for a single pixel */

	typedef struct
	{
		ContributionType *ContribRow; /* Row (or column) of contribution weights */
		unsigned int WindowSize,      /* Filter window size (of affecting source pixels) */
			LineLength;      /* Length of line (no. or rows / cols) */
	} LineContribType;

/* Each core filter has its own radius */
	#define DEFAULT_FILTER_BICUBIC				3.0
			#define DEFAULT_FILTER_BOX					0.5
			#define DEFAULT_FILTER_GENERALIZED_CUBIC	0.5
			#define DEFAULT_FILTER_RADIUS				1.0
			#define DEFAULT_LANCZOS8_RADIUS				8.0
			#define DEFAULT_LANCZOS3_RADIUS				3.0
			#define DEFAULT_HERMITE_RADIUS				1.0
			#define DEFAULT_BOX_RADIUS					0.5
			#define DEFAULT_TRIANGLE_RADIUS				1.0
			#define DEFAULT_BELL_RADIUS					1.5
			#define DEFAULT_CUBICSPLINE_RADIUS			2.0
			#define DEFAULT_MITCHELL_RADIUS				2.0
			#define DEFAULT_COSINE_RADIUS				1.0
			#define DEFAULT_CATMULLROM_RADIUS			2.0
			#define DEFAULT_QUADRATIC_RADIUS			1.5
			#define DEFAULT_QUADRATICBSPLINE_RADIUS		1.5
			#define DEFAULT_CUBICCONVOLUTION_RADIUS		3.0
			#define DEFAULT_GAUSSIAN_RADIUS				1.0
			#define DEFAULT_HANNING_RADIUS				1.0
			#define DEFAULT_HAMMING_RADIUS				1.0
			#define DEFAULT_SINC_RADIUS					1.0
			#define DEFAULT_WELSH_RADIUS				1.0

	static double KernelBessel_J1(const double x)
	{
		double p, q;

		register long i;

		static const double
			Pone[] =
			{
					0.581199354001606143928050809e+21,
					-0.6672106568924916298020941484e+20,
					0.2316433580634002297931815435e+19,
					-0.3588817569910106050743641413e+17,
					0.2908795263834775409737601689e+15,
					-0.1322983480332126453125473247e+13,
					0.3413234182301700539091292655e+10,
					-0.4695753530642995859767162166e+7,
					0.270112271089232341485679099e+4
			},
			Qone[] =
					{
							0.11623987080032122878585294e+22,
							0.1185770712190320999837113348e+20,
							0.6092061398917521746105196863e+17,
							0.2081661221307607351240184229e+15,
							0.5243710262167649715406728642e+12,
							0.1013863514358673989967045588e+10,
							0.1501793594998585505921097578e+7,
							0.1606931573481487801970916749e+4,
							0.1e+1
					};

		p = Pone[8];
		q = Qone[8];
		for (i=7; i >= 0; i--)
		{
			p = p*x*x+Pone[i];
			q = q*x*x+Qone[i];
		}
		return (double)(p/q);
	}

	static double KernelBessel_P1(const double x)
	{
		double p, q;

		register long i;

		static const double
			Pone[] =
			{
					0.352246649133679798341724373e+5,
					0.62758845247161281269005675e+5,
					0.313539631109159574238669888e+5,
					0.49854832060594338434500455e+4,
					0.2111529182853962382105718e+3,
					0.12571716929145341558495e+1
			},
			Qone[] =
					{
							0.352246649133679798068390431e+5,
							0.626943469593560511888833731e+5,
							0.312404063819041039923015703e+5,
							0.4930396490181088979386097e+4,
							0.2030775189134759322293574e+3,
							0.1e+1
					};

		p = Pone[5];
		q = Qone[5];
		for (i=4; i >= 0; i--)
		{
			p = p*(8.0/x)*(8.0/x)+Pone[i];
			q = q*(8.0/x)*(8.0/x)+Qone[i];
		}
		return (double)(p/q);
	}

	static double KernelBessel_Q1(const double x)
	{
		double p, q;

		register long i;

		static const double
			Pone[] =
			{
					0.3511751914303552822533318e+3,
					0.7210391804904475039280863e+3,
					0.4259873011654442389886993e+3,
					0.831898957673850827325226e+2,
					0.45681716295512267064405e+1,
					0.3532840052740123642735e-1
			},
			Qone[] =
					{
							0.74917374171809127714519505e+4,
							0.154141773392650970499848051e+5,
							0.91522317015169922705904727e+4,
							0.18111867005523513506724158e+4,
							0.1038187585462133728776636e+3,
							0.1e+1
					};

		p = Pone[5];
		q = Qone[5];
		for (i=4; i >= 0; i--)
		{
			p = p*(8.0/x)*(8.0/x)+Pone[i];
			q = q*(8.0/x)*(8.0/x)+Qone[i];
		}
		return (double)(p/q);
	}

	static double KernelBessel_Order1(double x)
	{
		double p, q;

		if (x == 0.0)
			return (0.0f);
		p = x;
		if (x < 0.0)
			x=(-x);
		if (x < 8.0)
			return (p*KernelBessel_J1(x));
		q = (double)sqrt(2.0f/(M_PI*x))*(double)(KernelBessel_P1(x)*(1.0f/sqrt(2.0f)*(sin(x)-cos(x)))-8.0f/x*KernelBessel_Q1(x)*
				(-1.0f/sqrt(2.0f)*(sin(x)+cos(x))));
		if (p < 0.0f)
			q = (-q);
		return (q);
	}

	static double filter_bessel(const double x)
	{
		if (x == 0.0f)
			return (double)(M_PI/4.0f);
		return (KernelBessel_Order1((double)M_PI*x)/(2.0f*x));
	}


	static double filter_blackman(const double x)
	{
		return (0.42f+0.5f*(double)cos(M_PI*x)+0.08f*(double)cos(2.0f*M_PI*x));
	}

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
	static double filter_bicubic(const double t)
	{
		const double abs_t = (double)fabs(t);
		const double abs_t_sq = abs_t * abs_t;
		if (abs_t<1) return 1-2*abs_t_sq+abs_t_sq*abs_t;
		if (abs_t<2) return 4 - 8*abs_t +5*abs_t_sq - abs_t_sq*abs_t;
		return 0;
	}

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
	static double filter_generalized_cubic(const double t)
	{
		const double a = -DEFAULT_FILTER_GENERALIZED_CUBIC;
		double abs_t = (double)fabs(t);
		double abs_t_sq = abs_t * abs_t;
		if (abs_t < 1) return (a + 2) * abs_t_sq * abs_t - (a + 3) * abs_t_sq + 1;
		if (abs_t < 2) return a * abs_t_sq * abs_t - 5 * a * abs_t_sq + 8 * a * abs_t - 4 * a;
		return 0;
	}

	#ifdef FUNCTION_NOT_USED_YET
	/* CubicSpline filter, default radius 2 */
	static double filter_cubic_spline(const double x1)
	{
		const double x = x1 < 0.0 ? -x1 : x1;

		if (x < 1.0 ) {
			const double x2 = x*x;

			return (0.5 * x2 * x - x2 + 2.0 / 3.0);
		}
		if (x < 2.0) {
			return (pow(2.0 - x, 3.0)/6.0);
		}
		return 0;
	}
	#endif

	#ifdef FUNCTION_NOT_USED_YET
	/* CubicConvolution filter, default radius 3 */
	static double filter_cubic_convolution(const double x1)
	{
		const double x = x1 < 0.0 ? -x1 : x1;
		const double x2 = x1 * x1;
		const double x2_x = x2 * x;

		if (x <= 1.0) return ((4.0 / 3.0)* x2_x - (7.0 / 3.0) * x2 + 1.0);
		if (x <= 2.0) return (- (7.0 / 12.0) * x2_x + 3 * x2 - (59.0 / 12.0) * x + 2.5);
		if (x <= 3.0) return ( (1.0/12.0) * x2_x - (2.0 / 3.0) * x2 + 1.75 * x - 1.5);
		return 0;
	}
	#endif

	static double filter_box(double x) {
		if (x < - DEFAULT_FILTER_BOX)
			return 0.0f;
		if (x < DEFAULT_FILTER_BOX)
			return 1.0f;
		return 0.0f;
	}

	static double filter_catmullrom(const double x)
	{
		if (x < -2.0)
			return(0.0f);
		if (x < -1.0)
			return(0.5f*(4.0f+x*(8.0f+x*(5.0f+x))));
		if (x < 0.0)
			return(0.5f*(2.0f+x*x*(-5.0f-3.0f*x)));
		if (x < 1.0)
			return(0.5f*(2.0f+x*x*(-5.0f+3.0f*x)));
		if (x < 2.0)
			return(0.5f*(4.0f+x*(-8.0f+x*(5.0f-x))));
		return(0.0f);
	}

	#ifdef FUNCTION_NOT_USED_YET
	static double filter_filter(double t)
	{
	/* f(t) = 2|t|^3 - 3|t|^2 + 1, -1 <= t <= 1 */
		if(t < 0.0) t = -t;
		if(t < 1.0) return((2.0 * t - 3.0) * t * t + 1.0);
		return(0.0);
	}
	#endif

	#ifdef FUNCTION_NOT_USED_YET
	/* Lanczos8 filter, default radius 8 */
	static double filter_lanczos8(const double x1)
	{
		const double x = x1 < 0.0 ? -x1 : x1;
		#define R DEFAULT_LANCZOS8_RADIUS

		if ( x == 0.0) return 1;

		if ( x < R) {
			return R * sin(x*M_PI) * sin(x * M_PI/ R) / (x * M_PI * x * M_PI);
		}
		return 0.0;
		#undef R
	}
	#endif

	#ifdef FUNCTION_NOT_USED_YET
	/* Lanczos3 filter, default radius 3 */
	static double filter_lanczos3(const double x1)
	{
		const double x = x1 < 0.0 ? -x1 : x1;
		#define R DEFAULT_LANCZOS3_RADIUS

		if ( x == 0.0) return 1;

		if ( x < R)
		{
			return R * sin(x*M_PI) * sin(x * M_PI / R) / (x * M_PI * x * M_PI);
		}
		return 0.0;
		#undef R
	}
	#endif

	/* Hermite filter, default radius 1 */
	static double filter_hermite(const double x1)
	{
		const double x = x1 < 0.0 ? -x1 : x1;

		if (x < 1.0) return ((2.0 * x - 3) * x * x + 1.0 );

		return 0.0;
	}

	/* Trangle filter, default radius 1 */
	static double filter_triangle(const double x1)
	{
		const double x = x1 < 0.0 ? -x1 : x1;
		if (x < 1.0) return (1.0 - x);
		return 0.0;
	}

	/* Bell filter, default radius 1.5 */
	static double filter_bell(const double x1)
	{
		const double x = x1 < 0.0 ? -x1 : x1;

		if (x < 0.5) return (0.75 - x*x);
		if (x < 1.5) return (0.5 * pow(x - 1.5, 2.0));
		return 0.0;
	}

	/* Mitchell filter, default radius 2.0 */
	static double filter_mitchell(const double x)
	{
		#define KM_B (1.0f/3.0f)
		#define KM_C (1.0f/3.0f)
		#define KM_P0 ((  6.0f - 2.0f * KM_B ) / 6.0f)
		#define KM_P2 ((-18.0f + 12.0f * KM_B + 6.0f * KM_C) / 6.0f)
		#define KM_P3 (( 12.0f - 9.0f  * KM_B - 6.0f * KM_C) / 6.0f)
		#define KM_Q0 ((  8.0f * KM_B + 24.0f * KM_C) / 6.0f)
		#define KM_Q1 ((-12.0f * KM_B - 48.0f * KM_C) / 6.0f)
		#define KM_Q2 ((  6.0f * KM_B + 30.0f * KM_C) / 6.0f)
		#define KM_Q3 (( -1.0f * KM_B -  6.0f * KM_C) / 6.0f)

		if (x < -2.0)
			return(0.0f);
		if (x < -1.0)
			return(KM_Q0-x*(KM_Q1-x*(KM_Q2-x*KM_Q3)));
		if (x < 0.0f)
			return(KM_P0+x*x*(KM_P2-x*KM_P3));
		if (x < 1.0f)
			return(KM_P0+x*x*(KM_P2+x*KM_P3));
		if (x < 2.0f)
			return(KM_Q0+x*(KM_Q1+x*(KM_Q2+x*KM_Q3)));
		return(0.0f);
	}



	#ifdef FUNCTION_NOT_USED_YET
	/* Cosine filter, default radius 1 */
	static double filter_cosine(const double x)
	{
		if ((x >= -1.0) && (x <= 1.0)) return ((cos(x * M_PI) + 1.0)/2.0);

		return 0;
	}
	#endif

	/* Quadratic filter, default radius 1.5 */
	static double filter_quadratic(const double x1)
	{
		const double x = x1 < 0.0 ? -x1 : x1;

		if (x <= 0.5) return (- 2.0 * x * x + 1);
		if (x <= 1.5) return (x * x - 2.5* x + 1.5);
		return 0.0;
	}

	static double filter_bspline(const double x)
	{
		if (x>2.0f) {
			return 0.0f;
		} else {
			double a, b, c, d;
		/* Was calculated anyway cause the "if((x-1.0f) < 0)" */
			const double xm1 = x - 1.0f;
			const double xp1 = x + 1.0f;
			const double xp2 = x + 2.0f;

			if ((xp2) <= 0.0f) a = 0.0f; else a = xp2*xp2*xp2;
			if ((xp1) <= 0.0f) b = 0.0f; else b = xp1*xp1*xp1;
			if (x <= 0) c = 0.0f; else c = x*x*x;
			if ((xm1) <= 0.0f) d = 0.0f; else d = xm1*xm1*xm1;

			return (0.16666666666666666667f * (a - (4.0f * b) + (6.0f * c) - (4.0f * d)));
		}
	}

	#ifdef FUNCTION_NOT_USED_YET
	/* QuadraticBSpline filter, default radius 1.5 */
	static double filter_quadratic_bspline(const double x1)
	{
		const double x = x1 < 0.0 ? -x1 : x1;

		if (x <= 0.5) return (- x * x + 0.75);
		if (x <= 1.5) return (0.5 * x * x - 1.5 * x + 1.125);
		return 0.0;
	}
	#endif

	static double filter_gaussian(const double x)
	{
	/* return(exp((double) (-2.0 * x * x)) * sqrt(2.0 / M_PI)); */
		return (double)(exp(-2.0f * x * x) * 0.79788456080287f);
	}

	static double filter_hanning(const double x)
	{
	/* A Cosine windowing function */
		return(0.5 + 0.5 * cos(M_PI * x));
	}

	static double filter_hamming(const double x)
	{
	/* should be
	(0.54+0.46*cos(M_PI*(double) x));
	but this approximation is sufficient */
		if (x < -1.0f)
			return 0.0f;
		if (x < 0.0f)
			return 0.92f*(-2.0f*x-3.0f)*x*x+1.0f;
		if (x < 1.0f)
			return 0.92f*(2.0f*x-3.0f)*x*x+1.0f;
		return 0.0f;
	}

	static double filter_power(const double x)
	{
		const double a = 2.0f;
		if (fabs(x)>1) return 0.0f;
		return (1.0f - (double)fabs(pow(x,a)));
	}

	static double filter_sinc(const double x)
	{
	/* X-scaled Sinc(x) function. */
		if (x == 0.0) return(1.0);
		return (sin(M_PI * (double) x) / (M_PI * (double) x));
	}

	#ifdef FUNCTION_NOT_USED_YET
	static double filter_welsh(const double x)
	{
	/* Welsh parabolic windowing filter */
		if (x <  1.0)
			return(1 - x*x);
		return(0.0);
	}
	#endif

	#if defined(_MSC_VER) && !defined(inline)
	# define inline __inline
	#endif

/* Copied from upstream's libgd */
	static inline int _color_blend (const int dst, const int src)
	{
		const int src_alpha = gdTrueColorGetAlpha(src);

		if( src_alpha == gdAlphaOpaque ) {
			return src;
		} else {
			const int dst_alpha = gdTrueColorGetAlpha(dst);

			if( src_alpha == gdAlphaTransparent ) return dst;
			if( dst_alpha == gdAlphaTransparent ) {
				return src;
			} else {
				register int alpha, red, green, blue;
				const int src_weight = gdAlphaTransparent - src_alpha;
				const int dst_weight = (gdAlphaTransparent - dst_alpha) * src_alpha / gdAlphaMax;
				const int tot_weight = src_weight + dst_weight;

				alpha = src_alpha * dst_alpha / gdAlphaMax;

				red = (gdTrueColorGetRed(src) * src_weight
						+ gdTrueColorGetRed(dst) * dst_weight) / tot_weight;
				green = (gdTrueColorGetGreen(src) * src_weight
						+ gdTrueColorGetGreen(dst) * dst_weight) / tot_weight;
				blue = (gdTrueColorGetBlue(src) * src_weight
						+ gdTrueColorGetBlue(dst) * dst_weight) / tot_weight;

				return ((alpha << 24) + (red << 16) + (green << 8) + blue);
			}
		}
	}

	static inline int _setEdgePixel(const gdImagePtr src, unsigned int x, unsigned int y, gdFixed coverage, const int bgColor)
	{
		const gdFixed f_127 = gd_itofx(127);
		register int c = src->tpixels[y][x];
		c = c | (( (int) (gd_fxtof(gd_mulfx(coverage, f_127)) + 50.5f)) << 24);
		return _color_blend(bgColor, c);
	}

	static inline int getPixelOverflowTC(gdImagePtr im, const int x, const int y, const int bgColor)
	{
		if (gdImageBoundsSafe(im, x, y)) {
			const int c = im->tpixels[y][x];
			if (c == im->transparent) {
				return bgColor == -1 ? gdTrueColorAlpha(0, 0, 0, 127) : bgColor;
			}
			return c;
		} else {
			register int border = 0;

			if (y < im->cy1) {
				border = im->tpixels[0][im->cx1];
				goto processborder;
			}

			if (y < im->cy1) {
				border = im->tpixels[0][im->cx1];
				goto processborder;
			}

			if (y > im->cy2) {
				if (x >= im->cx1 && x <= im->cx1) {
					border = im->tpixels[im->cy2][x];
					goto processborder;
				} else {
					return gdTrueColorAlpha(0, 0, 0, 127);
				}
			}

		/* y is bound safe at this point */
			if (x < im->cx1) {
				border = im->tpixels[y][im->cx1];
				goto processborder;
			}

			if (x > im->cx2) {
				border = im->tpixels[y][im->cx2];
			}

			processborder:
			if (border == im->transparent) {
				return gdTrueColorAlpha(0, 0, 0, 127);
			} else{
				return gdTrueColorAlpha(gdTrueColorGetRed(border), gdTrueColorGetGreen(border), gdTrueColorGetBlue(border), 127);
			}
		}
	}

	#define colorIndex2RGBA(c) gdTrueColorAlpha(im->red[(c)], im->green[(c)], im->blue[(c)], im->alpha[(c)])
			#define colorIndex2RGBcustomA(c, a) gdTrueColorAlpha(im->red[(c)], im->green[(c)], im->blue[(c)], im->alpha[(a)])
	static inline int getPixelOverflowPalette(gdImagePtr im, const int x, const int y, const int bgColor)
	{
		if (gdImageBoundsSafe(im, x, y)) {
			const int c = im->pixels[y][x];
			if (c == im->transparent) {
				return bgColor == -1 ? gdTrueColorAlpha(0, 0, 0, 127) : bgColor;
			}
			return colorIndex2RGBA(c);
		} else {
			register int border = 0;
			if (y < im->cy1) {
				border = gdImageGetPixel(im, im->cx1, 0);
				goto processborder;
			}

			if (y < im->cy1) {
				border = gdImageGetPixel(im, im->cx1, 0);
				goto processborder;
			}

			if (y > im->cy2) {
				if (x >= im->cx1 && x <= im->cx1) {
					border = gdImageGetPixel(im, x,  im->cy2);
					goto processborder;
				} else {
					return gdTrueColorAlpha(0, 0, 0, 127);
				}
			}

		/* y is bound safe at this point */
			if (x < im->cx1) {
				border = gdImageGetPixel(im, im->cx1, y);
				goto processborder;
			}

			if (x > im->cx2) {
				border = gdImageGetPixel(im, im->cx2, y);
			}

			processborder:
			if (border == im->transparent) {
				return gdTrueColorAlpha(0, 0, 0, 127);
			} else{
				return colorIndex2RGBcustomA(border, 127);
			}
		}
	}

	static int getPixelInterpolateWeight(gdImagePtr im, const double x, const double y, const int bgColor)
	{
	/* Closest pixel <= (xf,yf) */
		int sx = (int)(x);
		int sy = (int)(y);
		const double xf = x - (double)sx;
		const double yf = y - (double)sy;
		const double nxf = (double) 1.0 - xf;
		const double nyf = (double) 1.0 - yf;
		const double m1 = xf * yf;
		const double m2 = nxf * yf;
		const double m3 = xf * nyf;
		const double m4 = nxf * nyf;

	/* get color values of neighbouring pixels */
		const int c1 = im->trueColor == 1 ? getPixelOverflowTC(im, sx, sy, bgColor)         : getPixelOverflowPalette(im, sx, sy, bgColor);
		const int c2 = im->trueColor == 1 ? getPixelOverflowTC(im, sx - 1, sy, bgColor)     : getPixelOverflowPalette(im, sx - 1, sy, bgColor);
		const int c3 = im->trueColor == 1 ? getPixelOverflowTC(im, sx, sy - 1, bgColor)     : getPixelOverflowPalette(im, sx, sy - 1, bgColor);
		const int c4 = im->trueColor == 1 ? getPixelOverflowTC(im, sx - 1, sy - 1, bgColor) : getPixelOverflowPalette(im, sx, sy - 1, bgColor);
		int r, g, b, a;

		if (x < 0) sx--;
		if (y < 0) sy--;

	/* component-wise summing-up of color values */
		if (im->trueColor) {
			r = (int)(m1*gdTrueColorGetRed(c1)   + m2*gdTrueColorGetRed(c2)   + m3*gdTrueColorGetRed(c3)   + m4*gdTrueColorGetRed(c4));
			g = (int)(m1*gdTrueColorGetGreen(c1) + m2*gdTrueColorGetGreen(c2) + m3*gdTrueColorGetGreen(c3) + m4*gdTrueColorGetGreen(c4));
			b = (int)(m1*gdTrueColorGetBlue(c1)  + m2*gdTrueColorGetBlue(c2)  + m3*gdTrueColorGetBlue(c3)  + m4*gdTrueColorGetBlue(c4));
			a = (int)(m1*gdTrueColorGetAlpha(c1) + m2*gdTrueColorGetAlpha(c2) + m3*gdTrueColorGetAlpha(c3) + m4*gdTrueColorGetAlpha(c4));
		} else {
			r = (int)(m1*im->red[(c1)]   + m2*im->red[(c2)]   + m3*im->red[(c3)]   + m4*im->red[(c4)]);
			g = (int)(m1*im->green[(c1)] + m2*im->green[(c2)] + m3*im->green[(c3)] + m4*im->green[(c4)]);
			b = (int)(m1*im->blue[(c1)]  + m2*im->blue[(c2)]  + m3*im->blue[(c3)]  + m4*im->blue[(c4)]);
			a = (int)(m1*im->alpha[(c1)] + m2*im->alpha[(c2)] + m3*im->alpha[(c3)] + m4*im->alpha[(c4)]);
		}

		r = CLAMP(r, 0, 255);
		g = CLAMP(g, 0, 255);
		b = CLAMP(b, 0, 255);
		a = CLAMP(a, 0, gdAlphaMax);
		return gdTrueColorAlpha(r, g, b, a);
	}

	/**
	 * InternalFunction: getPixelInterpolated
	 *  Returns the interpolated color value using the default interpolation
	 *  method. The returned color is always in the ARGB format (truecolor).
	 *
	 * Parameters:
	 * 	im - Image to set the default interpolation method
	 *  y - X value of the ideal position
	 *  y - Y value of the ideal position
	 *  method - Interpolation method <gdInterpolationMethod>
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 *
	 * See also:
	 *  <gdSetInterpolationMethod>
	 */
	int getPixelInterpolated(gdImagePtr im, const double x, const double y, const int bgColor)
	{
		const int xi=(int)((x) < 0 ? x - 1: x);
		const int yi=(int)((y) < 0 ? y - 1: y);
		int yii;
		int i;
		double kernel, kernel_cache_y;
		double kernel_x[12], kernel_y[4];
		double new_r = 0.0f, new_g = 0.0f, new_b = 0.0f, new_a = 0.0f;

	/* These methods use special implementations */
		if (im->interpolation_id == GD_BILINEAR_FIXED || im->interpolation_id == GD_BICUBIC_FIXED || im->interpolation_id == GD_NEAREST_NEIGHBOUR) {
			return -1;
		}

		if (im->interpolation_id == GD_WEIGHTED4) {
			return getPixelInterpolateWeight(im, x, y, bgColor);
		}

		if (im->interpolation_id == GD_NEAREST_NEIGHBOUR) {
			if (im->trueColor == 1) {
				return getPixelOverflowTC(im, xi, yi, bgColor);
			} else {
				return getPixelOverflowPalette(im, xi, yi, bgColor);
			}
		}
		if (im->interpolation) {
			for (i=0; i<4; i++) {
				kernel_x[i] = (double) im->interpolation((double)(xi+i-1-x));
				kernel_y[i] = (double) im->interpolation((double)(yi+i-1-y));
			}
		} else {
			return -1;
		}

	/*
	 * TODO: use the known fast rgba multiplication implementation once
	 * the new formats are in place
	 */
		for (yii = yi-1; yii < yi+3; yii++) {
			int xii;
			kernel_cache_y = kernel_y[yii-(yi-1)];
			if (im->trueColor) {
				for (xii=xi-1; xii<xi+3; xii++) {
					const int rgbs = getPixelOverflowTC(im, xii, yii, bgColor);

					kernel = kernel_cache_y * kernel_x[xii-(xi-1)];
					new_r += kernel * gdTrueColorGetRed(rgbs);
					new_g += kernel * gdTrueColorGetGreen(rgbs);
					new_b += kernel * gdTrueColorGetBlue(rgbs);
					new_a += kernel * gdTrueColorGetAlpha(rgbs);
				}
			} else {
				for (xii=xi-1; xii<xi+3; xii++) {
					const int rgbs = getPixelOverflowPalette(im, xii, yii, bgColor);

					kernel = kernel_cache_y * kernel_x[xii-(xi-1)];
					new_r += kernel * gdTrueColorGetRed(rgbs);
					new_g += kernel * gdTrueColorGetGreen(rgbs);
					new_b += kernel * gdTrueColorGetBlue(rgbs);
					new_a += kernel * gdTrueColorGetAlpha(rgbs);
				}
			}
		}

		new_r = CLAMP(new_r, 0, 255);
		new_g = CLAMP(new_g, 0, 255);
		new_b = CLAMP(new_b, 0, 255);
		new_a = CLAMP(new_a, 0, gdAlphaMax);

		return gdTrueColorAlpha(((int)new_r), ((int)new_g), ((int)new_b), ((int)new_a));
	}

	static inline LineContribType * _gdContributionsAlloc(unsigned int line_length, unsigned int windows_size)
	{
		unsigned int u = 0;
		LineContribType *res;

		res = (LineContribType *) gdMalloc(sizeof(LineContribType));
		if (!res) {
			return NULL;
		}
		res->WindowSize = windows_size;
		res->LineLength = line_length;
		res->ContribRow = (ContributionType *) gdMalloc(line_length * sizeof(ContributionType));

		for (u = 0 ; u < line_length ; u++) {
			res->ContribRow[u].Weights = (double *) gdMalloc(windows_size * sizeof(double));
		}
		return res;
	}

	static inline void _gdContributionsFree(LineContribType * p)
	{
		unsigned int u;
		for (u = 0; u < p->LineLength; u++)  {
			gdFree(p->ContribRow[u].Weights);
		}
		gdFree(p->ContribRow);
		gdFree(p);
	}

	static inline LineContribType *_gdContributionsCalc(unsigned int line_size, unsigned int src_size, double scale_d,  const interpolation_method pFilter)
	{
		double width_d;
		double scale_f_d = 1.0;
		const double filter_width_d = DEFAULT_BOX_RADIUS;
		int windows_size;
		unsigned int u;
		LineContribType *res;

		if (scale_d < 1.0) {
			width_d = filter_width_d / scale_d;
			scale_f_d = scale_d;
		}  else {
			width_d= filter_width_d;
		}

		windows_size = 2 * (int)ceil(width_d) + 1;
		res = _gdContributionsAlloc(line_size, windows_size);

		for (u = 0; u < line_size; u++) {
			const double dCenter = (double)u / scale_d;
		/* get the significant edge points affecting the pixel */
			register int iLeft = MAX(0, (int)floor (dCenter - width_d));
			int iRight = MIN((int)ceil(dCenter + width_d), (int)src_size - 1);
			double dTotalWeight = 0.0;
			int iSrc;

		/* Cut edge points to fit in filter window in case of spill-off */
			if (iRight - iLeft + 1 > windows_size)  {
				if (iLeft < ((int)src_size - 1 / 2))  {
					iLeft++;
				} else {
					iRight--;
				}
			}

			res->ContribRow[u].Left = iLeft;
			res->ContribRow[u].Right = iRight;

			for (iSrc = iLeft; iSrc <= iRight; iSrc++) {
				dTotalWeight += (res->ContribRow[u].Weights[iSrc-iLeft] =  scale_f_d * (*pFilter)(scale_f_d * (dCenter - (double)iSrc)));
			}

			if (dTotalWeight < 0.0) {
				_gdContributionsFree(res);
				return NULL;
			}

			if (dTotalWeight > 0.0) {
				for (iSrc = iLeft; iSrc <= iRight; iSrc++) {
					res->ContribRow[u].Weights[iSrc-iLeft] /= dTotalWeight;
				}
			}
		}
		return res;
	}


	static inline void
				  _gdScaleOneAxis(gdImagePtr pSrc, gdImagePtr dst,
								  unsigned int dst_len, unsigned int row, LineContribType *contrib,
								  gdAxis axis)
	{
		unsigned int ndx;

		for (ndx = 0; ndx < dst_len; ndx++) {
			double r = 0, g = 0, b = 0, a = 0;
			const int left = contrib->ContribRow[ndx].Left;
			const int right = contrib->ContribRow[ndx].Right;
			int *dest = (axis == HORIZONTAL) ?
			&dst->tpixels[row][ndx] :
			&dst->tpixels[ndx][row];

			int i;

		/* Accumulate each channel */
			for (i = left; i <= right; i++) {
				const int left_channel = i - left;
				const int srcpx = (axis == HORIZONTAL) ?
						pSrc->tpixels[row][i] :
						pSrc->tpixels[i][row];

				r += contrib->ContribRow[ndx].Weights[left_channel]
						* (double)(gdTrueColorGetRed(srcpx));
				g += contrib->ContribRow[ndx].Weights[left_channel]
						* (double)(gdTrueColorGetGreen(srcpx));
				b += contrib->ContribRow[ndx].Weights[left_channel]
						* (double)(gdTrueColorGetBlue(srcpx));
				a += contrib->ContribRow[ndx].Weights[left_channel]
						* (double)(gdTrueColorGetAlpha(srcpx));
			}/* for */

			*dest = gdTrueColorAlpha(uchar_clamp(r, 0xFF), uchar_clamp(g, 0xFF),
					uchar_clamp(b, 0xFF),
					uchar_clamp(a, 0x7F)); /* alpha is 0..127 */
		}/* for */
	}/* _gdScaleOneAxis*/


	static inline int
				  _gdScalePass(const gdImagePtr pSrc, const unsigned int src_len,
							   const gdImagePtr pDst, const unsigned int dst_len,
							   const unsigned int num_lines,
							   const gdAxis axis)
	{
		unsigned int line_ndx;
		LineContribType * contrib;

    /* Same dim, just copy it. */
		assert(dst_len != src_len); // TODO: caller should handle this.

		contrib = _gdContributionsCalc(dst_len, src_len,
				(double)dst_len / (double)src_len,
				pSrc->interpolation);
		if (contrib == NULL) {
			return 0;
		}

	/* Scale each line */
		for (line_ndx = 0; line_ndx < num_lines; line_ndx++) {
			_gdScaleOneAxis(pSrc, pDst, dst_len, line_ndx, contrib, axis);
		}
		_gdContributionsFree (contrib);

		return 1;
	}/* _gdScalePass*/


	static gdImagePtr
	gdImageScaleTwoPass(const gdImagePtr src, const unsigned int new_width,
						const unsigned int new_height)
	{
		const unsigned int src_width = src->sx;
		const unsigned int src_height = src->sy;
		gdImagePtr tmp_im = NULL;
		gdImagePtr dst = NULL;

    /* First, handle the trivial case. */
		if (src_width == new_width && src_height == new_height) {
			return gdImageClone(src);
		}/* if */

	/* Convert to truecolor if it isn't; this code requires it. */
		if (!src->trueColor) {
			gdImagePaletteToTrueColor(src);
		}/* if */

    /* Scale horizontally unless sizes are the same. */
		if (src_width == new_width) {
			tmp_im = src;
		} else {
			tmp_im = gdImageCreateTrueColor(new_width, src_height);
			if (tmp_im == NULL) {
				return NULL;
			}
			gdImageSetInterpolationMethod(tmp_im, src->interpolation_id);

			_gdScalePass(src, src_width, tmp_im, new_width, src_height, HORIZONTAL);
		}/* if .. else*/

    /* If vertical sizes match, we're done. */
		if (src_height == new_height) {
			assert(tmp_im != src);
			return tmp_im;
		}/* if */

    /* Otherwise, we need to scale vertically. */
		dst = gdImageCreateTrueColor(new_width, new_height);
		if (dst != NULL) {
			gdImageSetInterpolationMethod(dst, src->interpolation_id);
			_gdScalePass(tmp_im, src_height, dst, new_height, new_width, VERTICAL);
		}/* if */

		if (src != tmp_im) {
			gdFree(tmp_im);
		}/* if */

		return dst;
	}/* gdImageScaleTwoPass*/


	/*
		BilinearFixed, BicubicFixed and nearest implementations are
		rewamped versions of the implementation in CBitmapEx

		http://www.codeproject.com/Articles/29121/CBitmapEx-Free-C-Bitmap-Manipulation-Class

		Integer only implementation, good to have for common usages like
		pre scale very large images before using another interpolation
		methods for the last step.
	*/
	static gdImagePtr
	gdImageScaleNearestNeighbour(gdImagePtr im, const unsigned int width, const unsigned int height)
	{
		const unsigned long new_width = MAX(1, width);
		const unsigned long new_height = MAX(1, height);
		const float dx = (float)im->sx / (float)new_width;
		const float dy = (float)im->sy / (float)new_height;
		const gdFixed f_dx = gd_ftofx(dx);
		const gdFixed f_dy = gd_ftofx(dy);

		gdImagePtr dst_img;
		unsigned long  dst_offset_x;
		unsigned long  dst_offset_y = 0;
		unsigned int i;

		dst_img = gdImageCreateTrueColor(new_width, new_height);

		if (dst_img == NULL) {
			return NULL;
		}

		for (i=0; i<new_height; i++) {
			unsigned int j;
			dst_offset_x = 0;
			if (im->trueColor) {
				for (j=0; j<new_width; j++) {
					const gdFixed f_i = gd_itofx(i);
					const gdFixed f_j = gd_itofx(j);
					const gdFixed f_a = gd_mulfx(f_i, f_dy);
					const gdFixed f_b = gd_mulfx(f_j, f_dx);
					const long m = gd_fxtoi(f_a);
					const long n = gd_fxtoi(f_b);

					dst_img->tpixels[dst_offset_y][dst_offset_x++] = im->tpixels[m][n];
				}
			} else {
				for (j=0; j<new_width; j++) {
					const gdFixed f_i = gd_itofx(i);
					const gdFixed f_j = gd_itofx(j);
					const gdFixed f_a = gd_mulfx(f_i, f_dy);
					const gdFixed f_b = gd_mulfx(f_j, f_dx);
					const long m = gd_fxtoi(f_a);
					const long n = gd_fxtoi(f_b);

					dst_img->tpixels[dst_offset_y][dst_offset_x++] = colorIndex2RGBA(im->pixels[m][n]);
				}
			}
			dst_offset_y++;
		}
		return dst_img;
	}

	static inline int getPixelOverflowColorTC(gdImagePtr im, const int x, const int y, const int color)
	{
		if (gdImageBoundsSafe(im, x, y)) {
			const int c = im->tpixels[y][x];
			if (c == im->transparent) {
				return gdTrueColorAlpha(0, 0, 0, 127);
			}
			return c;
		} else {
			register int border = 0;
			if (y < im->cy1) {
				border = im->tpixels[0][im->cx1];
				goto processborder;
			}

			if (y < im->cy1) {
				border = im->tpixels[0][im->cx1];
				goto processborder;
			}

			if (y > im->cy2) {
				if (x >= im->cx1 && x <= im->cx1) {
					border = im->tpixels[im->cy2][x];
					goto processborder;
				} else {
					return gdTrueColorAlpha(0, 0, 0, 127);
				}
			}

		/* y is bound safe at this point */
			if (x < im->cx1) {
				border = im->tpixels[y][im->cx1];
				goto processborder;
			}

			if (x > im->cx2) {
				border = im->tpixels[y][im->cx2];
			}

			processborder:
			if (border == im->transparent) {
				return gdTrueColorAlpha(0, 0, 0, 127);
			} else{
				return gdTrueColorAlpha(gdTrueColorGetRed(border), gdTrueColorGetGreen(border), gdTrueColorGetBlue(border), 127);
			}
		}
	}

	static gdImagePtr gdImageScaleBilinearPalette(gdImagePtr im, const unsigned int new_width, const unsigned int new_height)
	{
		long _width = MAX(1, new_width);
		long _height = MAX(1, new_height);
		float dx = (float)gdImageSX(im) / (float)_width;
		float dy = (float)gdImageSY(im) / (float)_height;
		gdFixed f_dx = gd_ftofx(dx);
		gdFixed f_dy = gd_ftofx(dy);
		gdFixed f_1 = gd_itofx(1);

		int dst_offset_h;
		int dst_offset_v = 0;
		long i;
		gdImagePtr new_img;
		const int transparent = im->transparent;

		new_img = gdImageCreateTrueColor(new_width, new_height);
		if (new_img == NULL) {
			return NULL;
		}
		new_img->transparent = gdTrueColorAlpha(im->red[transparent], im->green[transparent], im->blue[transparent], im->alpha[transparent]);

		for (i=0; i < _height; i++) {
			long j;
			const gdFixed f_i = gd_itofx(i);
			const gdFixed f_a = gd_mulfx(f_i, f_dy);
			register long m = gd_fxtoi(f_a);

			dst_offset_h = 0;

			for (j=0; j < _width; j++) {
			/* Update bitmap */
				gdFixed f_j = gd_itofx(j);
				gdFixed f_b = gd_mulfx(f_j, f_dx);

				const long n = gd_fxtoi(f_b);
				gdFixed f_f = f_a - gd_itofx(m);
				gdFixed f_g = f_b - gd_itofx(n);

				const gdFixed f_w1 = gd_mulfx(f_1-f_f, f_1-f_g);
				const gdFixed f_w2 = gd_mulfx(f_1-f_f, f_g);
				const gdFixed f_w3 = gd_mulfx(f_f, f_1-f_g);
				const gdFixed f_w4 = gd_mulfx(f_f, f_g);
				unsigned int pixel1;
				unsigned int pixel2;
				unsigned int pixel3;
				unsigned int pixel4;
				register gdFixed f_r1, f_r2, f_r3, f_r4,
						f_g1, f_g2, f_g3, f_g4,
						f_b1, f_b2, f_b3, f_b4,
						f_a1, f_a2, f_a3, f_a4;

			/* zero for the background color, nothig gets outside anyway */
				pixel1 = getPixelOverflowPalette(im, n, m, 0);
				pixel2 = getPixelOverflowPalette(im, n + 1, m, 0);
				pixel3 = getPixelOverflowPalette(im, n, m + 1, 0);
				pixel4 = getPixelOverflowPalette(im, n + 1, m + 1, 0);

				f_r1 = gd_itofx(gdTrueColorGetRed(pixel1));
				f_r2 = gd_itofx(gdTrueColorGetRed(pixel2));
				f_r3 = gd_itofx(gdTrueColorGetRed(pixel3));
				f_r4 = gd_itofx(gdTrueColorGetRed(pixel4));
				f_g1 = gd_itofx(gdTrueColorGetGreen(pixel1));
				f_g2 = gd_itofx(gdTrueColorGetGreen(pixel2));
				f_g3 = gd_itofx(gdTrueColorGetGreen(pixel3));
				f_g4 = gd_itofx(gdTrueColorGetGreen(pixel4));
				f_b1 = gd_itofx(gdTrueColorGetBlue(pixel1));
				f_b2 = gd_itofx(gdTrueColorGetBlue(pixel2));
				f_b3 = gd_itofx(gdTrueColorGetBlue(pixel3));
				f_b4 = gd_itofx(gdTrueColorGetBlue(pixel4));
				f_a1 = gd_itofx(gdTrueColorGetAlpha(pixel1));
				f_a2 = gd_itofx(gdTrueColorGetAlpha(pixel2));
				f_a3 = gd_itofx(gdTrueColorGetAlpha(pixel3));
				f_a4 = gd_itofx(gdTrueColorGetAlpha(pixel4));

				{
					const char red = (char) gd_fxtoi(gd_mulfx(f_w1, f_r1) + gd_mulfx(f_w2, f_r2) + gd_mulfx(f_w3, f_r3) + gd_mulfx(f_w4, f_r4));
					const char green = (char) gd_fxtoi(gd_mulfx(f_w1, f_g1) + gd_mulfx(f_w2, f_g2) + gd_mulfx(f_w3, f_g3) + gd_mulfx(f_w4, f_g4));
					const char blue = (char) gd_fxtoi(gd_mulfx(f_w1, f_b1) + gd_mulfx(f_w2, f_b2) + gd_mulfx(f_w3, f_b3) + gd_mulfx(f_w4, f_b4));
					const char alpha = (char) gd_fxtoi(gd_mulfx(f_w1, f_a1) + gd_mulfx(f_w2, f_a2) + gd_mulfx(f_w3, f_a3) + gd_mulfx(f_w4, f_a4));

					new_img->tpixels[dst_offset_v][dst_offset_h] = gdTrueColorAlpha(red, green, blue, alpha);
				}

				dst_offset_h++;
			}

			dst_offset_v++;
		}
		return new_img;
	}

	static gdImagePtr gdImageScaleBilinearTC(gdImagePtr im, const unsigned int new_width, const unsigned int new_height)
	{
		long dst_w = MAX(1, new_width);
		long dst_h = MAX(1, new_height);
		float dx = (float)gdImageSX(im) / (float)dst_w;
		float dy = (float)gdImageSY(im) / (float)dst_h;
		gdFixed f_dx = gd_ftofx(dx);
		gdFixed f_dy = gd_ftofx(dy);
		gdFixed f_1 = gd_itofx(1);

		int dst_offset_h;
		int dst_offset_v = 0;
		long i;
		gdImagePtr new_img;

		new_img = gdImageCreateTrueColor(new_width, new_height);
		if (!new_img){
			return NULL;
		}

		for (i=0; i < dst_h; i++) {
			long j;
			dst_offset_h = 0;
			for (j=0; j < dst_w; j++) {
			/* Update bitmap */
				gdFixed f_i = gd_itofx(i);
				gdFixed f_j = gd_itofx(j);
				gdFixed f_a = gd_mulfx(f_i, f_dy);
				gdFixed f_b = gd_mulfx(f_j, f_dx);
				const gdFixed m = gd_fxtoi(f_a);
				const gdFixed n = gd_fxtoi(f_b);
				gdFixed f_f = f_a - gd_itofx(m);
				gdFixed f_g = f_b - gd_itofx(n);

				const gdFixed f_w1 = gd_mulfx(f_1-f_f, f_1-f_g);
				const gdFixed f_w2 = gd_mulfx(f_1-f_f, f_g);
				const gdFixed f_w3 = gd_mulfx(f_f, f_1-f_g);
				const gdFixed f_w4 = gd_mulfx(f_f, f_g);
				unsigned int pixel1;
				unsigned int pixel2;
				unsigned int pixel3;
				unsigned int pixel4;
				register gdFixed f_r1, f_r2, f_r3, f_r4,
						f_g1, f_g2, f_g3, f_g4,
						f_b1, f_b2, f_b3, f_b4,
						f_a1, f_a2, f_a3, f_a4;
			/* 0 for bgColor, nothing gets outside anyway */
				pixel1 = getPixelOverflowTC(im, n, m, 0);
				pixel2 = getPixelOverflowTC(im, n + 1, m, 0);
				pixel3 = getPixelOverflowTC(im, n, m + 1, 0);
				pixel4 = getPixelOverflowTC(im, n + 1, m + 1, 0);

				f_r1 = gd_itofx(gdTrueColorGetRed(pixel1));
				f_r2 = gd_itofx(gdTrueColorGetRed(pixel2));
				f_r3 = gd_itofx(gdTrueColorGetRed(pixel3));
				f_r4 = gd_itofx(gdTrueColorGetRed(pixel4));
				f_g1 = gd_itofx(gdTrueColorGetGreen(pixel1));
				f_g2 = gd_itofx(gdTrueColorGetGreen(pixel2));
				f_g3 = gd_itofx(gdTrueColorGetGreen(pixel3));
				f_g4 = gd_itofx(gdTrueColorGetGreen(pixel4));
				f_b1 = gd_itofx(gdTrueColorGetBlue(pixel1));
				f_b2 = gd_itofx(gdTrueColorGetBlue(pixel2));
				f_b3 = gd_itofx(gdTrueColorGetBlue(pixel3));
				f_b4 = gd_itofx(gdTrueColorGetBlue(pixel4));
				f_a1 = gd_itofx(gdTrueColorGetAlpha(pixel1));
				f_a2 = gd_itofx(gdTrueColorGetAlpha(pixel2));
				f_a3 = gd_itofx(gdTrueColorGetAlpha(pixel3));
				f_a4 = gd_itofx(gdTrueColorGetAlpha(pixel4));
				{
					const unsigned char red   = (unsigned char) gd_fxtoi(gd_mulfx(f_w1, f_r1) + gd_mulfx(f_w2, f_r2) + gd_mulfx(f_w3, f_r3) + gd_mulfx(f_w4, f_r4));
					const unsigned char green = (unsigned char) gd_fxtoi(gd_mulfx(f_w1, f_g1) + gd_mulfx(f_w2, f_g2) + gd_mulfx(f_w3, f_g3) + gd_mulfx(f_w4, f_g4));
					const unsigned char blue  = (unsigned char) gd_fxtoi(gd_mulfx(f_w1, f_b1) + gd_mulfx(f_w2, f_b2) + gd_mulfx(f_w3, f_b3) + gd_mulfx(f_w4, f_b4));
					const unsigned char alpha = (unsigned char) gd_fxtoi(gd_mulfx(f_w1, f_a1) + gd_mulfx(f_w2, f_a2) + gd_mulfx(f_w3, f_a3) + gd_mulfx(f_w4, f_a4));

					new_img->tpixels[dst_offset_v][dst_offset_h] = gdTrueColorAlpha(red, green, blue, alpha);
				}

				dst_offset_h++;
			}

			dst_offset_v++;
		}
		return new_img;
	}

	static gdImagePtr
	gdImageScaleBilinear(gdImagePtr im, const unsigned int new_width,
						 const unsigned int new_height)
	{
		if (im->trueColor) {
			return gdImageScaleBilinearTC(im, new_width, new_height);
		} else {
			return gdImageScaleBilinearPalette(im, new_width, new_height);
		}
	}

	static gdImagePtr
	gdImageScaleBicubicFixed(gdImagePtr src, const unsigned int width,
							 const unsigned int height)
	{
		const long new_width = MAX(1, width);
		const long new_height = MAX(1, height);
		const int src_w = gdImageSX(src);
		const int src_h = gdImageSY(src);
		const gdFixed f_dx = gd_ftofx((float)src_w / (float)new_width);
		const gdFixed f_dy = gd_ftofx((float)src_h / (float)new_height);
		const gdFixed f_1 = gd_itofx(1);
		const gdFixed f_2 = gd_itofx(2);
		const gdFixed f_4 = gd_itofx(4);
		const gdFixed f_6 = gd_itofx(6);
		const gdFixed f_gamma = gd_ftofx(1.04f);
		gdImagePtr dst;

		unsigned int dst_offset_x;
		unsigned int dst_offset_y = 0;
		long i;

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (src->trueColor == 0) {
			gdImagePaletteToTrueColor(src);
		}

		dst = gdImageCreateTrueColor(new_width, new_height);
		if (!dst) {
			return NULL;
		}

		dst->saveAlphaFlag = 1;

		for (i=0; i < new_height; i++) {
			long j;
			dst_offset_x = 0;

			for (j=0; j < new_width; j++) {
				const gdFixed f_a = gd_mulfx(gd_itofx(i), f_dy);
				const gdFixed f_b = gd_mulfx(gd_itofx(j), f_dx);
				const long m = gd_fxtoi(f_a);
				const long n = gd_fxtoi(f_b);
				const gdFixed f_f = f_a - gd_itofx(m);
				const gdFixed f_g = f_b - gd_itofx(n);
				unsigned int src_offset_x[16], src_offset_y[16];
				long k;
				register gdFixed f_red = 0, f_green = 0, f_blue = 0, f_alpha = 0;
				unsigned char red, green, blue, alpha = 0;
				int *dst_row = dst->tpixels[dst_offset_y];

				if ((m < 1) || (n < 1)) {
					src_offset_x[0] = n;
					src_offset_y[0] = m;
				} else {
					src_offset_x[0] = n - 1;
					src_offset_y[0] = m;
				}

				if (m < 1) {
					src_offset_x[1] = n;
					src_offset_y[1] = m;
				} else {
					src_offset_x[1] = n;
					src_offset_y[1] = m;
				}

				if ((m < 1) || (n >= src_w - 1)) {
					src_offset_x[2] = n;
					src_offset_y[2] = m;
				} else {
					src_offset_x[2] = n + 1;
					src_offset_y[2] = m;
				}

				if ((m < 1) || (n >= src_w - 2)) {
					src_offset_x[3] = n;
					src_offset_y[3] = m;
				} else {
					src_offset_x[3] = n + 1 + 1;
					src_offset_y[3] = m;
				}

				if (n < 1) {
					src_offset_x[4] = n;
					src_offset_y[4] = m;
				} else {
					src_offset_x[4] = n - 1;
					src_offset_y[4] = m;
				}

				src_offset_x[5] = n;
				src_offset_y[5] = m;
				if (n >= src_w-1) {
					src_offset_x[6] = n;
					src_offset_y[6] = m;
				} else {
					src_offset_x[6] = n + 1;
					src_offset_y[6] = m;
				}

				if (n >= src_w - 2) {
					src_offset_x[7] = n;
					src_offset_y[7] = m;
				} else {
					src_offset_x[7] = n + 1 + 1;
					src_offset_y[7] = m;
				}

				if ((m >= src_h - 1) || (n < 1)) {
					src_offset_x[8] = n;
					src_offset_y[8] = m;
				} else {
					src_offset_x[8] = n - 1;
					src_offset_y[8] = m;
				}

				if (m >= src_h - 1) {
					src_offset_x[8] = n;
					src_offset_y[8] = m;
				} else {
					src_offset_x[9] = n;
					src_offset_y[9] = m;
				}

				if ((m >= src_h-1) || (n >= src_w-1)) {
					src_offset_x[10] = n;
					src_offset_y[10] = m;
				} else {
					src_offset_x[10] = n + 1;
					src_offset_y[10] = m;
				}

				if ((m >= src_h - 1) || (n >= src_w - 2)) {
					src_offset_x[11] = n;
					src_offset_y[11] = m;
				} else {
					src_offset_x[11] = n + 1 + 1;
					src_offset_y[11] = m;
				}

				if ((m >= src_h - 2) || (n < 1)) {
					src_offset_x[12] = n;
					src_offset_y[12] = m;
				} else {
					src_offset_x[12] = n - 1;
					src_offset_y[12] = m;
				}

				if (m >= src_h - 2) {
					src_offset_x[13] = n;
					src_offset_y[13] = m;
				} else {
					src_offset_x[13] = n;
					src_offset_y[13] = m;
				}

				if ((m >= src_h - 2) || (n >= src_w - 1)) {
					src_offset_x[14] = n;
					src_offset_y[14] = m;
				} else {
					src_offset_x[14] = n + 1;
					src_offset_y[14] = m;
				}

				if ((m >= src_h - 2) || (n >= src_w - 2)) {
					src_offset_x[15] = n;
					src_offset_y[15] = m;
				} else {
					src_offset_x[15] = n  + 1 + 1;
					src_offset_y[15] = m;
				}

				for (k = -1; k < 3; k++) {
					const gdFixed f = gd_itofx(k)-f_f;
					const gdFixed f_fm1 = f - f_1;
					const gdFixed f_fp1 = f + f_1;
					const gdFixed f_fp2 = f + f_2;
					register gdFixed f_a = 0, f_b = 0, f_d = 0, f_c = 0;
					register gdFixed f_RY;
					int l;

					if (f_fp2 > 0) f_a = gd_mulfx(f_fp2, gd_mulfx(f_fp2,f_fp2));
					if (f_fp1 > 0) f_b = gd_mulfx(f_fp1, gd_mulfx(f_fp1,f_fp1));
					if (f > 0)     f_c = gd_mulfx(f, gd_mulfx(f,f));
					if (f_fm1 > 0) f_d = gd_mulfx(f_fm1, gd_mulfx(f_fm1,f_fm1));

					f_RY = gd_divfx((f_a - gd_mulfx(f_4,f_b) + gd_mulfx(f_6,f_c) - gd_mulfx(f_4,f_d)),f_6);

					for (l = -1; l < 3; l++) {
						const gdFixed f = gd_itofx(l) - f_g;
						const gdFixed f_fm1 = f - f_1;
						const gdFixed f_fp1 = f + f_1;
						const gdFixed f_fp2 = f + f_2;
						register gdFixed f_a = 0, f_b = 0, f_c = 0, f_d = 0;
						register gdFixed f_RX, f_R, f_rs, f_gs, f_bs, f_ba;
						register int c;
						const int _k = ((k+1)*4) + (l+1);

						if (f_fp2 > 0) f_a = gd_mulfx(f_fp2,gd_mulfx(f_fp2,f_fp2));

						if (f_fp1 > 0) f_b = gd_mulfx(f_fp1,gd_mulfx(f_fp1,f_fp1));

						if (f > 0) f_c = gd_mulfx(f,gd_mulfx(f,f));

						if (f_fm1 > 0) f_d = gd_mulfx(f_fm1,gd_mulfx(f_fm1,f_fm1));

						f_RX = gd_divfx((f_a-gd_mulfx(f_4,f_b)+gd_mulfx(f_6,f_c)-gd_mulfx(f_4,f_d)),f_6);
						f_R = gd_mulfx(f_RY,f_RX);

						c = src->tpixels[*(src_offset_y + _k)][*(src_offset_x + _k)];
						f_rs = gd_itofx(gdTrueColorGetRed(c));
						f_gs = gd_itofx(gdTrueColorGetGreen(c));
						f_bs = gd_itofx(gdTrueColorGetBlue(c));
						f_ba = gd_itofx(gdTrueColorGetAlpha(c));

						f_red += gd_mulfx(f_rs,f_R);
						f_green += gd_mulfx(f_gs,f_R);
						f_blue += gd_mulfx(f_bs,f_R);
						f_alpha += gd_mulfx(f_ba,f_R);
					}
				}

				red    = (unsigned char) CLAMP(gd_fxtoi(gd_mulfx(f_red,   f_gamma)),  0, 255);
				green  = (unsigned char) CLAMP(gd_fxtoi(gd_mulfx(f_green, f_gamma)),  0, 255);
				blue   = (unsigned char) CLAMP(gd_fxtoi(gd_mulfx(f_blue,  f_gamma)),  0, 255);
				alpha  = (unsigned char) CLAMP(gd_fxtoi(gd_mulfx(f_alpha,  f_gamma)), 0, 127);

				*(dst_row + dst_offset_x) = gdTrueColorAlpha(red, green, blue, alpha);

				dst_offset_x++;
			}
			dst_offset_y++;
		}
		return dst;
	}

	BGD_DECLARE(gdImagePtr) gdImageScale(const gdImagePtr src, const unsigned int new_width, const unsigned int new_height)
	{
		gdImagePtr im_scaled = NULL;

		if (src == NULL || src->interpolation_id < 0 || src->interpolation_id > GD_METHOD_COUNT) {
			return 0;
		}

		switch (src->interpolation_id) {
		/*Special cases, optimized implementations */
			case GD_NEAREST_NEIGHBOUR:
				im_scaled = gdImageScaleNearestNeighbour(src, new_width, new_height);
				break;

			case GD_BILINEAR_FIXED:
				im_scaled = gdImageScaleBilinear(src, new_width, new_height);
				break;

			case GD_BICUBIC_FIXED:
				im_scaled = gdImageScaleBicubicFixed(src, new_width, new_height);
				break;

		/* generic */
			default:
				if (src->interpolation == NULL) {
					return NULL;
				}
				im_scaled = gdImageScaleTwoPass(src, new_width, new_height);
				break;
		}

		return im_scaled;
	}

	static gdImagePtr
	gdImageRotateNearestNeighbour(gdImagePtr src, const float degrees,
								  const int bgColor)
	{
		float _angle = ((float) (-degrees / 180.0f) * (float)M_PI);
		const int src_w  = gdImageSX(src);
		const int src_h = gdImageSY(src);
		const unsigned int new_width = (unsigned int)(abs((int)(src_w * cos(_angle))) + abs((int)(src_h * sin(_angle))) + 0.5f);
		const unsigned int new_height = (unsigned int)(abs((int)(src_w * sin(_angle))) + abs((int)(src_h * cos(_angle))) + 0.5f);
		const gdFixed f_0_5 = gd_ftofx(0.5f);
		const gdFixed f_H = gd_itofx(src_h/2);
		const gdFixed f_W = gd_itofx(src_w/2);
		const gdFixed f_cos = gd_ftofx(cos(-_angle));
		const gdFixed f_sin = gd_ftofx(sin(-_angle));

		unsigned int dst_offset_x;
		unsigned int dst_offset_y = 0;
		unsigned int i;
		gdImagePtr dst;

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (src->trueColor == 0) {
			gdImagePaletteToTrueColor(src);
		}

		dst = gdImageCreateTrueColor(new_width, new_height);
		if (!dst) {
			return NULL;
		}
		dst->saveAlphaFlag = 1;
		for (i = 0; i < new_height; i++) {
			unsigned int j;
			dst_offset_x = 0;
			for (j = 0; j < new_width; j++) {
				gdFixed f_i = gd_itofx((int)i - (int)new_height / 2);
				gdFixed f_j = gd_itofx((int)j - (int)new_width  / 2);
				gdFixed f_m = gd_mulfx(f_j,f_sin) + gd_mulfx(f_i,f_cos) + f_0_5 + f_H;
				gdFixed f_n = gd_mulfx(f_j,f_cos) - gd_mulfx(f_i,f_sin) + f_0_5 + f_W;
				long m = gd_fxtoi(f_m);
				long n = gd_fxtoi(f_n);

				if ((m > 0) && (m < src_h-1) && (n > 0) && (n < src_w-1)) {
					if (dst_offset_y < new_height) {
						dst->tpixels[dst_offset_y][dst_offset_x++] = src->tpixels[m][n];
					}
				} else {
					if (dst_offset_y < new_height) {
						dst->tpixels[dst_offset_y][dst_offset_x++] = bgColor;
					}
				}
			}
			dst_offset_y++;
		}
		return dst;
	}

	static gdImagePtr
	gdImageRotateGeneric(gdImagePtr src, const float degrees, const int bgColor)
	{
		float _angle = ((float) (-degrees / 180.0f) * (float)M_PI);
		const int src_w  = gdImageSX(src);
		const int src_h = gdImageSY(src);
		const unsigned int new_width = (unsigned int)(abs((int)(src_w * cos(_angle))) + abs((int)(src_h * sin(_angle))) + 0.5f);
		const unsigned int new_height = (unsigned int)(abs((int)(src_w * sin(_angle))) + abs((int)(src_h * cos(_angle))) + 0.5f);
		const gdFixed f_0_5 = gd_ftofx(0.5f);
		const gdFixed f_H = gd_itofx(src_h/2);
		const gdFixed f_W = gd_itofx(src_w/2);
		const gdFixed f_cos = gd_ftofx(cos(-_angle));
		const gdFixed f_sin = gd_ftofx(sin(-_angle));

		unsigned int dst_offset_x;
		unsigned int dst_offset_y = 0;
		unsigned int i;
		gdImagePtr dst;

		const gdFixed f_slop_y = f_sin;
		const gdFixed f_slop_x = f_cos;
		const gdFixed f_slop = f_slop_x > 0 && f_slop_x > 0 ?
			f_slop_x > f_slop_y ? gd_divfx(f_slop_y, f_slop_x) : gd_divfx(f_slop_x, f_slop_y)
			: 0;

		if (bgColor < 0) {
			return NULL;
		}

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (src->trueColor == 0) {
			gdImagePaletteToTrueColor(src);
		}

		dst = gdImageCreateTrueColor(new_width, new_height);
		if (!dst) {
			return NULL;
		}
		dst->saveAlphaFlag = 1;

		for (i = 0; i < new_height; i++) {
			unsigned int j;
			dst_offset_x = 0;
			for (j = 0; j < new_width; j++) {
				gdFixed f_i = gd_itofx((int)i - (int)new_height / 2);
				gdFixed f_j = gd_itofx((int)j - (int)new_width  / 2);
				gdFixed f_m = gd_mulfx(f_j,f_sin) + gd_mulfx(f_i,f_cos) + f_0_5 + f_H;
				gdFixed f_n = gd_mulfx(f_j,f_cos) - gd_mulfx(f_i,f_sin) + f_0_5 + f_W;
				long m = gd_fxtoi(f_m);
				long n = gd_fxtoi(f_n);

				if ((n <= 0) || (m <= 0) || (m >= src_h) || (n >= src_w)) {
					dst->tpixels[dst_offset_y][dst_offset_x++] = bgColor;
				} else if ((n <= 1) || (m <= 1) || (m >= src_h - 1) || (n >= src_w - 1)) {
					register int c = getPixelInterpolated(src, n, m, bgColor);
					c = c | (( gdTrueColorGetAlpha(c) + ((int)(127* gd_fxtof(f_slop)))) << 24);

					dst->tpixels[dst_offset_y][dst_offset_x++] = _color_blend(bgColor, c);
				} else {
					dst->tpixels[dst_offset_y][dst_offset_x++] = getPixelInterpolated(src, n, m, bgColor);
				}
			}
			dst_offset_y++;
		}
		return dst;
	}

	static gdImagePtr
	gdImageRotateBilinear(gdImagePtr src, const float degrees, const int bgColor)
	{
		float _angle = (float)((- degrees / 180.0f) * M_PI);
		const unsigned int src_w = gdImageSX(src);
		const unsigned int src_h = gdImageSY(src);
		unsigned int new_width = abs((int)(src_w*cos(_angle))) + abs((int)(src_h*sin(_angle) + 0.5f));
		unsigned int new_height = abs((int)(src_w*sin(_angle))) + abs((int)(src_h*cos(_angle) + 0.5f));
		const gdFixed f_0_5 = gd_ftofx(0.5f);
		const gdFixed f_H = gd_itofx(src_h/2);
		const gdFixed f_W = gd_itofx(src_w/2);
		const gdFixed f_cos = gd_ftofx(cos(-_angle));
		const gdFixed f_sin = gd_ftofx(sin(-_angle));
		const gdFixed f_1 = gd_itofx(1);
		unsigned int i;
		unsigned int dst_offset_x;
		unsigned int dst_offset_y = 0;
		unsigned int src_offset_x, src_offset_y;
		gdImagePtr dst;

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (src->trueColor == 0) {
			gdImagePaletteToTrueColor(src);
		}

		dst = gdImageCreateTrueColor(new_width, new_height);
		if (dst == NULL) {
			return NULL;
		}
		dst->saveAlphaFlag = 1;

		for (i = 0; i < new_height; i++) {
			unsigned int j;
			dst_offset_x = 0;

			for (j=0; j < new_width; j++) {
				const gdFixed f_i = gd_itofx((int)i - (int)new_height / 2);
				const gdFixed f_j = gd_itofx((int)j - (int)new_width  / 2);
				const gdFixed f_m = gd_mulfx(f_j,f_sin) + gd_mulfx(f_i,f_cos) + f_0_5 + f_H;
				const gdFixed f_n = gd_mulfx(f_j,f_cos) - gd_mulfx(f_i,f_sin) + f_0_5 + f_W;
				const unsigned int m = gd_fxtoi(f_m);
				const unsigned int n = gd_fxtoi(f_n);

				if ((m > 0) && (m < src_h - 1) && (n > 0) && (n < src_w - 1)) {
					const gdFixed f_f = f_m - gd_itofx(m);
					const gdFixed f_g = f_n - gd_itofx(n);
					const gdFixed f_w1 = gd_mulfx(f_1-f_f, f_1-f_g);
					const gdFixed f_w2 = gd_mulfx(f_1-f_f, f_g);
					const gdFixed f_w3 = gd_mulfx(f_f, f_1-f_g);
					const gdFixed f_w4 = gd_mulfx(f_f, f_g);

					if (n < src_w - 1) {
						src_offset_x = n + 1;
						src_offset_y = m;
					}

					if (m < src_h - 1) {
						src_offset_x = n;
						src_offset_y = m + 1;
					}

					if (!((n >= src_w - 1) || (m >= src_h - 1))) {
						src_offset_x = n + 1;
						src_offset_y = m + 1;
					}
					{
						const int pixel1 = src->tpixels[src_offset_y][src_offset_x];
						register int pixel2, pixel3, pixel4;

						if (src_offset_y + 1 >= src_h) {
							pixel2 = bgColor;
							pixel3 = bgColor;
							pixel4 = bgColor;
						} else if (src_offset_x + 1 >= src_w) {
							pixel2 = bgColor;
							pixel3 = bgColor;
							pixel4 = bgColor;
						} else {
							pixel2 = src->tpixels[src_offset_y][src_offset_x + 1];
							pixel3 = src->tpixels[src_offset_y + 1][src_offset_x];
							pixel4 = src->tpixels[src_offset_y + 1][src_offset_x + 1];
						}
						{
							const gdFixed f_r1 = gd_itofx(gdTrueColorGetRed(pixel1));
							const gdFixed f_r2 = gd_itofx(gdTrueColorGetRed(pixel2));
							const gdFixed f_r3 = gd_itofx(gdTrueColorGetRed(pixel3));
							const gdFixed f_r4 = gd_itofx(gdTrueColorGetRed(pixel4));
							const gdFixed f_g1 = gd_itofx(gdTrueColorGetGreen(pixel1));
							const gdFixed f_g2 = gd_itofx(gdTrueColorGetGreen(pixel2));
							const gdFixed f_g3 = gd_itofx(gdTrueColorGetGreen(pixel3));
							const gdFixed f_g4 = gd_itofx(gdTrueColorGetGreen(pixel4));
							const gdFixed f_b1 = gd_itofx(gdTrueColorGetBlue(pixel1));
							const gdFixed f_b2 = gd_itofx(gdTrueColorGetBlue(pixel2));
							const gdFixed f_b3 = gd_itofx(gdTrueColorGetBlue(pixel3));
							const gdFixed f_b4 = gd_itofx(gdTrueColorGetBlue(pixel4));
							const gdFixed f_a1 = gd_itofx(gdTrueColorGetAlpha(pixel1));
							const gdFixed f_a2 = gd_itofx(gdTrueColorGetAlpha(pixel2));
							const gdFixed f_a3 = gd_itofx(gdTrueColorGetAlpha(pixel3));
							const gdFixed f_a4 = gd_itofx(gdTrueColorGetAlpha(pixel4));
							const gdFixed f_red = gd_mulfx(f_w1, f_r1) + gd_mulfx(f_w2, f_r2) + gd_mulfx(f_w3, f_r3) + gd_mulfx(f_w4, f_r4);
							const gdFixed f_green = gd_mulfx(f_w1, f_g1) + gd_mulfx(f_w2, f_g2) + gd_mulfx(f_w3, f_g3) + gd_mulfx(f_w4, f_g4);
							const gdFixed f_blue = gd_mulfx(f_w1, f_b1) + gd_mulfx(f_w2, f_b2) + gd_mulfx(f_w3, f_b3) + gd_mulfx(f_w4, f_b4);
							const gdFixed f_alpha = gd_mulfx(f_w1, f_a1) + gd_mulfx(f_w2, f_a2) + gd_mulfx(f_w3, f_a3) + gd_mulfx(f_w4, f_a4);

							const unsigned char red   = (unsigned char) CLAMP(gd_fxtoi(f_red),   0, 255);
							const unsigned char green = (unsigned char) CLAMP(gd_fxtoi(f_green), 0, 255);
							const unsigned char blue  = (unsigned char) CLAMP(gd_fxtoi(f_blue),  0, 255);
							const unsigned char alpha = (unsigned char) CLAMP(gd_fxtoi(f_alpha), 0, 127);

							dst->tpixels[dst_offset_y][dst_offset_x++] = gdTrueColorAlpha(red, green, blue, alpha);
						}
					}
				} else {
					dst->tpixels[dst_offset_y][dst_offset_x++] = bgColor;
				}
			}
			dst_offset_y++;
		}
		return dst;
	}

	static gdImagePtr
	gdImageRotateBicubicFixed(gdImagePtr src, const float degrees,const int bgColor)
	{
		const float _angle = (float)((- degrees / 180.0f) * M_PI);
		const int src_w = gdImageSX(src);
		const int src_h = gdImageSY(src);
		const unsigned int new_width = abs((int)(src_w*cos(_angle))) + abs((int)(src_h*sin(_angle) + 0.5f));
		const unsigned int new_height = abs((int)(src_w*sin(_angle))) + abs((int)(src_h*cos(_angle) + 0.5f));
		const gdFixed f_0_5 = gd_ftofx(0.5f);
		const gdFixed f_H = gd_itofx(src_h/2);
		const gdFixed f_W = gd_itofx(src_w/2);
		const gdFixed f_cos = gd_ftofx(cos(-_angle));
		const gdFixed f_sin = gd_ftofx(sin(-_angle));
		const gdFixed f_1 = gd_itofx(1);
		const gdFixed f_2 = gd_itofx(2);
		const gdFixed f_4 = gd_itofx(4);
		const gdFixed f_6 = gd_itofx(6);
		const gdFixed f_gama = gd_ftofx(1.04f);

		unsigned int dst_offset_x;
		unsigned int dst_offset_y = 0;
		unsigned int i;
		gdImagePtr dst;

	/* impact perf a bit, but not that much. Implementation for palette
	   images can be done at a later point.
	*/
		if (src->trueColor == 0) {
			gdImagePaletteToTrueColor(src);
		}

		dst = gdImageCreateTrueColor(new_width, new_height);

		if (dst == NULL) {
			return NULL;
		}
		dst->saveAlphaFlag = 1;

		for (i=0; i < new_height; i++) {
			unsigned int j;
			dst_offset_x = 0;

			for (j=0; j < new_width; j++) {
				const gdFixed f_i = gd_itofx((int)i - (int)new_height / 2);
				const gdFixed f_j = gd_itofx((int)j - (int)new_width  / 2);
				const gdFixed f_m = gd_mulfx(f_j,f_sin) + gd_mulfx(f_i,f_cos) + f_0_5 + f_H;
				const gdFixed f_n = gd_mulfx(f_j,f_cos) - gd_mulfx(f_i,f_sin) + f_0_5 + f_W;
				const int m = gd_fxtoi(f_m);
				const int n = gd_fxtoi(f_n);

				if ((m > 0) && (m < src_h - 1) && (n > 0) && (n < src_w-1)) {
					const gdFixed f_f = f_m - gd_itofx(m);
					const gdFixed f_g = f_n - gd_itofx(n);
					unsigned int src_offset_x[16], src_offset_y[16];
					unsigned char red, green, blue, alpha;
					gdFixed f_red=0, f_green=0, f_blue=0, f_alpha=0;
					int k;

					if ((m < 1) || (n < 1)) {
						src_offset_x[0] = n;
						src_offset_y[0] = m;
					} else {
						src_offset_x[0] = n - 1;
						src_offset_y[0] = m;
					}

					if (m < 1) {
						src_offset_x[1] = n;
						src_offset_y[1] = m;
					} else {
						src_offset_x[1] = n;
						src_offset_y[1] = m ;
					}

					if ((m < 1) || (n >= src_w-1)) {
						src_offset_x[2] = - 1;
						src_offset_y[2] = - 1;
					} else {
						src_offset_x[2] = n + 1;
						src_offset_y[2] = m ;
					}

					if ((m < 1) || (n >= src_w-2)) {
						src_offset_x[3] = - 1;
						src_offset_y[3] = - 1;
					} else {
						src_offset_x[3] = n + 1 + 1;
						src_offset_y[3] = m ;
					}

					if (n < 1) {
						src_offset_x[4] = - 1;
						src_offset_y[4] = - 1;
					} else {
						src_offset_x[4] = n - 1;
						src_offset_y[4] = m;
					}

					src_offset_x[5] = n;
					src_offset_y[5] = m;
					if (n >= src_w-1) {
						src_offset_x[6] = - 1;
						src_offset_y[6] = - 1;
					} else {
						src_offset_x[6] = n + 1;
						src_offset_y[6] = m;
					}

					if (n >= src_w-2) {
						src_offset_x[7] = - 1;
						src_offset_y[7] = - 1;
					} else {
						src_offset_x[7] = n + 1 + 1;
						src_offset_y[7] = m;
					}

					if ((m >= src_h-1) || (n < 1)) {
						src_offset_x[8] = - 1;
						src_offset_y[8] = - 1;
					} else {
						src_offset_x[8] = n - 1;
						src_offset_y[8] = m;
					}

					if (m >= src_h-1) {
						src_offset_x[8] = - 1;
						src_offset_y[8] = - 1;
					} else {
						src_offset_x[9] = n;
						src_offset_y[9] = m;
					}

					if ((m >= src_h-1) || (n >= src_w-1)) {
						src_offset_x[10] = - 1;
						src_offset_y[10] = - 1;
					} else {
						src_offset_x[10] = n + 1;
						src_offset_y[10] = m;
					}

					if ((m >= src_h-1) || (n >= src_w-2)) {
						src_offset_x[11] = - 1;
						src_offset_y[11] = - 1;
					} else {
						src_offset_x[11] = n + 1 + 1;
						src_offset_y[11] = m;
					}

					if ((m >= src_h-2) || (n < 1)) {
						src_offset_x[12] = - 1;
						src_offset_y[12] = - 1;
					} else {
						src_offset_x[12] = n - 1;
						src_offset_y[12] = m;
					}

					if (m >= src_h-2) {
						src_offset_x[13] = - 1;
						src_offset_y[13] = - 1;
					} else {
						src_offset_x[13] = n;
						src_offset_y[13] = m;
					}

					if ((m >= src_h-2) || (n >= src_w - 1)) {
						src_offset_x[14] = - 1;
						src_offset_y[14] = - 1;
					} else {
						src_offset_x[14] = n + 1;
						src_offset_y[14] = m;
					}

					if ((m >= src_h-2) || (n >= src_w-2)) {
						src_offset_x[15] = - 1;
						src_offset_y[15] = - 1;
					} else {
						src_offset_x[15] = n  + 1 + 1;
						src_offset_y[15] = m;
					}

					for (k=-1; k<3; k++) {
						const gdFixed f = gd_itofx(k)-f_f;
						const gdFixed f_fm1 = f - f_1;
						const gdFixed f_fp1 = f + f_1;
						const gdFixed f_fp2 = f + f_2;
						gdFixed f_a = 0, f_b = 0,f_c = 0, f_d = 0;
						gdFixed f_RY;
						int l;

						if (f_fp2 > 0) {
							f_a = gd_mulfx(f_fp2,gd_mulfx(f_fp2,f_fp2));
						}

						if (f_fp1 > 0) {
							f_b = gd_mulfx(f_fp1,gd_mulfx(f_fp1,f_fp1));
						}

						if (f > 0) {
							f_c = gd_mulfx(f,gd_mulfx(f,f));
						}

						if (f_fm1 > 0) {
							f_d = gd_mulfx(f_fm1,gd_mulfx(f_fm1,f_fm1));
						}
						f_RY = gd_divfx((f_a-gd_mulfx(f_4,f_b)+gd_mulfx(f_6,f_c)-gd_mulfx(f_4,f_d)),f_6);

						for (l=-1;  l< 3; l++) {
							const gdFixed f = gd_itofx(l) - f_g;
							const gdFixed f_fm1 = f - f_1;
							const gdFixed f_fp1 = f + f_1;
							const gdFixed f_fp2 = f + f_2;
							gdFixed f_a = 0, f_b = 0, f_c = 0, f_d = 0;
							gdFixed f_RX, f_R;
							const int _k = ((k + 1) * 4) + (l + 1);
							register gdFixed f_rs, f_gs, f_bs, f_as;
							register int c;

							if (f_fp2 > 0) {
								f_a = gd_mulfx(f_fp2,gd_mulfx(f_fp2,f_fp2));
							}

							if (f_fp1 > 0) {
								f_b = gd_mulfx(f_fp1,gd_mulfx(f_fp1,f_fp1));
							}

							if (f > 0) {
								f_c = gd_mulfx(f,gd_mulfx(f,f));
							}

							if (f_fm1 > 0) {
								f_d = gd_mulfx(f_fm1,gd_mulfx(f_fm1,f_fm1));
							}

							f_RX = gd_divfx((f_a - gd_mulfx(f_4, f_b) + gd_mulfx(f_6, f_c) - gd_mulfx(f_4, f_d)), f_6);
							f_R = gd_mulfx(f_RY, f_RX);

							if ((src_offset_x[_k] <= 0) || (src_offset_y[_k] <= 0) || (src_offset_y[_k] >= src_h) || (src_offset_x[_k] >= src_w)) {
								c = bgColor;
							} else if ((src_offset_x[_k] <= 1) || (src_offset_y[_k] <= 1) || (src_offset_y[_k] >= (int)src_h - 1) || (src_offset_x[_k] >= (int)src_w - 1)) {
								gdFixed f_127 = gd_itofx(127);
								c = src->tpixels[src_offset_y[_k]][src_offset_x[_k]];
								c = c | (( (int) (gd_fxtof(gd_mulfx(f_R, f_127)) + 50.5f)) << 24);
								c = _color_blend(bgColor, c);
							} else {
								c = src->tpixels[src_offset_y[_k]][src_offset_x[_k]];
							}

							f_rs = gd_itofx(gdTrueColorGetRed(c));
							f_gs = gd_itofx(gdTrueColorGetGreen(c));
							f_bs = gd_itofx(gdTrueColorGetBlue(c));
							f_as = gd_itofx(gdTrueColorGetAlpha(c));

							f_red   += gd_mulfx(f_rs, f_R);
							f_green += gd_mulfx(f_gs, f_R);
							f_blue  += gd_mulfx(f_bs, f_R);
							f_alpha += gd_mulfx(f_as, f_R);
						}
					}

					red   = (unsigned char) CLAMP(gd_fxtoi(gd_mulfx(f_red, f_gama)),   0, 255);
					green = (unsigned char) CLAMP(gd_fxtoi(gd_mulfx(f_green, f_gama)), 0, 255);
					blue  = (unsigned char) CLAMP(gd_fxtoi(gd_mulfx(f_blue, f_gama)),  0, 255);
					alpha = (unsigned char) CLAMP(gd_fxtoi(gd_mulfx(f_alpha, f_gama)), 0, 127);

					dst->tpixels[dst_offset_y][dst_offset_x] =  gdTrueColorAlpha(red, green, blue, alpha);
				} else {
					dst->tpixels[dst_offset_y][dst_offset_x] =  bgColor;
				}
				dst_offset_x++;
			}

			dst_offset_y++;
		}
		return dst;
	}

	BGD_DECLARE(gdImagePtr) gdImageRotateInterpolated(const gdImagePtr src, const float angle, int bgcolor)
	{
	/* round to two decimals and keep the 100x multiplication to use it in the common square angles
	   case later. Keep the two decimal precisions so smaller rotation steps can be done, useful for
	   slow animations, f.e. */
		const int angle_rounded = fmod((int) floorf(angle * 100), 360 * 100);

		if (bgcolor < 0) {
			return NULL;
		}

	/* 0 && 90 degrees multiple rotation, 0 rotation simply clones the return image and convert it
	   to truecolor, as we must return truecolor image. */
		switch (angle_rounded) {
			case    0: {
				gdImagePtr dst = gdImageClone(src);

				if (dst == NULL) {
					return NULL;
				}
				if (dst->trueColor == 0) {
					gdImagePaletteToTrueColor(dst);
				}
				return dst;
			}

			case -2700:
			case   9000:
				return gdImageRotate90(src, 0);

			case -18000:
			case  18000:
				return gdImageRotate180(src, 0);

			case  -9000:
			case  27000:
				return gdImageRotate270(src, 0);
		}

		if (src == NULL || src->interpolation_id < 1 || src->interpolation_id > GD_METHOD_COUNT) {
			return NULL;
		}

		switch (src->interpolation_id) {
			case GD_NEAREST_NEIGHBOUR:
				return gdImageRotateNearestNeighbour(src, angle, bgcolor);
			break;

			case GD_BILINEAR_FIXED:
				return gdImageRotateBilinear(src, angle, bgcolor);
			break;

			case GD_BICUBIC_FIXED:
				return gdImageRotateBicubicFixed(src, angle, bgcolor);
			break;

			default:
				return gdImageRotateGeneric(src, angle, bgcolor);
		}
		return NULL;
	}

/**
 * Title: Affine transformation
 **/

	/**
	 * Group: Transform
	 **/

	static void gdImageClipRectangle(gdImagePtr im, gdRectPtr r)
	{
		int c1x, c1y, c2x, c2y;
		int x1,y1;

		gdImageGetClip(im, &c1x, &c1y, &c2x, &c2y);
		x1 = r->x + r->width - 1;
		y1 = r->y + r->height - 1;
		r->x = CLAMP(r->x, c1x, c2x);
		r->y = CLAMP(r->y, c1y, c2y);
		r->width = CLAMP(x1, c1x, c2x) - r->x + 1;
		r->height = CLAMP(y1, c1y, c2y) - r->y + 1;
	}

	void gdDumpRect(const char *msg, gdRectPtr r)
	{
		printf("%s (%i, %i) (%i, %i)\n", msg, r->x, r->y, r->width, r->height);
	}

	/**
	 * Function: gdTransformAffineGetImage
	 *  Applies an affine transformation to a region and return an image
	 *  containing the complete transformation.
	 *
	 * Parameters:
	 * 	dst - Pointer to a gdImagePtr to store the created image, NULL when
	 *        the creation or the transformation failed
	 *  src - Source image
	 *  src_area - rectangle defining the source region to transform
	 *  dstY - Y position in the destination image
	 *  affine - The desired affine transformation
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 */
	BGD_DECLARE(int) gdTransformAffineGetImage(gdImagePtr *dst,
			const gdImagePtr src,
	gdRectPtr src_area,
	const double affine[6])
	{
		int res;
		double m[6];
		gdRect bbox;
		gdRect area_full;

		if (src_area == NULL) {
			area_full.x = 0;
			area_full.y = 0;
			area_full.width  = gdImageSX(src);
			area_full.height = gdImageSY(src);
			src_area = &area_full;
		}

		gdTransformAffineBoundingBox(src_area, affine, &bbox);

		*dst = gdImageCreateTrueColor(bbox.width, bbox.height);
		if (*dst == NULL) {
		return GD_FALSE;
	}
		(*dst)->saveAlphaFlag = 1;

		if (!src->trueColor) {
			gdImagePaletteToTrueColor(src);
		}

	/* Translate to dst origin (0,0) */
		gdAffineTranslate(m, -bbox.x, -bbox.y);
		gdAffineConcat(m, affine, m);

		gdImageAlphaBlending(*dst, 0);

		res = gdTransformAffineCopy(*dst,
				0,0,
				src,
				src_area,
				m);

		if (res != GD_TRUE) {
			gdImageDestroy(*dst);
			dst = NULL;
			return GD_FALSE;
		} else {
			return GD_TRUE;
		}
	}

	/**
	 * Function: gdTransformAffineCopy
	 *  Applies an affine transformation to a region and copy the result
	 *  in a destination to the given position.
	 *
	 * Parameters:
	 * 	dst - Image to draw the transformed image
	 *  src - Source image
	 *  dstX - X position in the destination image
	 *  dstY - Y position in the destination image
	 *  src_area - Rectangular region to rotate in the src image
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 */
	BGD_DECLARE(int) gdTransformAffineCopy(gdImagePtr dst,
	int dst_x, int dst_y,
	const gdImagePtr src,
	gdRectPtr src_region,
	const double affine[6])
	{
		int c1x,c1y,c2x,c2y;
		int backclip = 0;
		int backup_clipx1, backup_clipy1, backup_clipx2, backup_clipy2;
		register int x, y, src_offset_x, src_offset_y;
		double inv[6];
		int *dst_p;
		gdPointF pt, src_pt;
		gdRect bbox;
		int end_x, end_y;
		gdInterpolationMethod interpolation_id_bak = GD_DEFAULT;

	/* These methods use special implementations */
		if (src->interpolation_id == GD_BILINEAR_FIXED || src->interpolation_id == GD_BICUBIC_FIXED || src->interpolation_id == GD_NEAREST_NEIGHBOUR) {
			interpolation_id_bak = src->interpolation_id;

			gdImageSetInterpolationMethod(src, GD_BICUBIC);
		}


		gdImageClipRectangle(src, src_region);

		if (src_region->x > 0 || src_region->y > 0
				|| src_region->width < gdImageSX(src)
				|| src_region->height < gdImageSY(src)) {
			backclip = 1;

			gdImageGetClip(src, &backup_clipx1, &backup_clipy1,
			&backup_clipx2, &backup_clipy2);

			gdImageSetClip(src, src_region->x, src_region->y,
					src_region->x + src_region->width - 1,
					src_region->y + src_region->height - 1);
		}

		if (!gdTransformAffineBoundingBox(src_region, affine, &bbox)) {
		if (backclip) {
			gdImageSetClip(src, backup_clipx1, backup_clipy1,
					backup_clipx2, backup_clipy2);
		}
		gdImageSetInterpolationMethod(src, interpolation_id_bak);
		return GD_FALSE;
	}

		gdImageGetClip(dst, &c1x, &c1y, &c2x, &c2y);

		end_x = bbox.width  + (int) fabs(bbox.x);
		end_y = bbox.height + (int) fabs(bbox.y);

	/* Get inverse affine to let us work with destination -> source */
		gdAffineInvert(inv, affine);

		src_offset_x =  src_region->x;
		src_offset_y =  src_region->y;

		if (dst->alphaBlendingFlag) {
			for (y = bbox.y; y <= end_y; y++) {
				pt.y = y + 0.5;
				for (x = 0; x <= end_x; x++) {
					pt.x = x + 0.5;
					gdAffineApplyToPointF(&src_pt, &pt, inv);
					gdImageSetPixel(dst, dst_x + x, dst_y + y, getPixelInterpolated(src, src_offset_x + src_pt.x, src_offset_y + src_pt.y, 0));
				}
			}
		} else {
			for (y = 0; y <= end_y; y++) {
				pt.y = y + 0.5 + bbox.y;
				if ((dst_y + y) < 0 || ((dst_y + y) > gdImageSY(dst) -1)) {
					continue;
				}
				dst_p = dst->tpixels[dst_y + y] + dst_x;

				for (x = 0; x <= end_x; x++) {
					pt.x = x + 0.5 + bbox.x;
					gdAffineApplyToPointF(&src_pt, &pt, inv);

					if ((dst_x + x) < 0 || (dst_x + x) > (gdImageSX(dst) - 1)) {
						break;
					}
					*(dst_p++) = getPixelInterpolated(src, src_offset_x + src_pt.x, src_offset_y + src_pt.y, -1);
				}
			}
		}

	/* Restore clip if required */
		if (backclip) {
			gdImageSetClip(src, backup_clipx1, backup_clipy1,
					backup_clipx2, backup_clipy2);
		}

		gdImageSetInterpolationMethod(src, interpolation_id_bak);
		return GD_TRUE;
	}

	/**
	 * Function: gdTransformAffineBoundingBox
	 *  Returns the bounding box of an affine transformation applied to a
	 *  rectangular area <gdRect>
	 *
	 * Parameters:
	 * 	src - Rectangular source area for the affine transformation
	 *  affine - the affine transformation
	 *  bbox - the resulting bounding box
	 *
	 * Returns:
	 *  GD_TRUE if the affine is rectilinear or GD_FALSE
	 */
	BGD_DECLARE(int) gdTransformAffineBoundingBox(gdRectPtr src, const double affine[6], gdRectPtr bbox)
	{
		gdPointF extent[4], min, max, point;
		int i;

		extent[0].x=0.0;
		extent[0].y=0.0;
		extent[1].x=(double) src->width;
		extent[1].y=0.0;
		extent[2].x=(double) src->width;
		extent[2].y=(double) src->height;
		extent[3].x=0.0;
		extent[3].y=(double) src->height;

		for (i=0; i < 4; i++) {
			point=extent[i];
			if (gdAffineApplyToPointF(&extent[i], &point, affine) != GD_TRUE) {
				return GD_FALSE;
			}
		}
		min=extent[0];
		max=extent[0];

		for (i=1; i < 4; i++) {
			if (min.x > extent[i].x)
				min.x=extent[i].x;
			if (min.y > extent[i].y)
				min.y=extent[i].y;
			if (max.x < extent[i].x)
				max.x=extent[i].x;
			if (max.y < extent[i].y)
				max.y=extent[i].y;
		}
		bbox->x = (int) min.x;
		bbox->y = (int) min.y;
		bbox->width  = (int) floor(max.x - min.x) - 1;
		bbox->height = (int) floor(max.y - min.y);
		return GD_TRUE;
	}

	BGD_DECLARE(int) gdImageSetInterpolationMethod(gdImagePtr im, gdInterpolationMethod id)
	{
		if (im == NULL || id < 0 || id > GD_METHOD_COUNT) {
			return 0;
		}

		switch (id) {
			case GD_DEFAULT:
				id = GD_BILINEAR_FIXED;
		/* Optimized versions */
			case GD_BILINEAR_FIXED:
			case GD_BICUBIC_FIXED:
			case GD_NEAREST_NEIGHBOUR:
			case GD_WEIGHTED4:
				im->interpolation = NULL;
				break;

		/* generic versions*/
			case GD_BELL:
				im->interpolation = filter_bell;
				break;
			case GD_BESSEL:
				im->interpolation = filter_bessel;
				break;
			case GD_BICUBIC:
				im->interpolation = filter_bicubic;
				break;
			case GD_BLACKMAN:
				im->interpolation = filter_blackman;
				break;
			case GD_BOX:
				im->interpolation = filter_box;
				break;
			case GD_BSPLINE:
				im->interpolation = filter_bspline;
				break;
			case GD_CATMULLROM:
				im->interpolation = filter_catmullrom;
				break;
			case GD_GAUSSIAN:
				im->interpolation = filter_gaussian;
				break;
			case GD_GENERALIZED_CUBIC:
				im->interpolation = filter_generalized_cubic;
				break;
			case GD_HERMITE:
				im->interpolation = filter_hermite;
				break;
			case GD_HAMMING:
				im->interpolation = filter_hamming;
				break;
			case GD_HANNING:
				im->interpolation = filter_hanning;
				break;
			case GD_MITCHELL:
				im->interpolation = filter_mitchell;
				break;
			case GD_POWER:
				im->interpolation = filter_power;
				break;
			case GD_QUADRATIC:
				im->interpolation = filter_quadratic;
				break;
			case GD_SINC:
				im->interpolation = filter_sinc;
				break;
			case GD_TRIANGLE:
				im->interpolation = filter_triangle;
				break;

			default:
				return 0;
			break;
		}
		im->interpolation_id = id;
		return 1;
	}


	/* Return the interpolation mode set in 'im'.  This is here so that
	 * the value can be read via a language or VM with an FFI but no
	 * (portable) way to extract the value from the struct. */
	BGD_DECLARE(gdInterpolationMethod) gdImageGetInterpolationMethod(gdImagePtr im)
	{
		return im->interpolation_id;
	}

	/* Convert a double to an unsigned char, rounding to the nearest
	 * integer and clamping the result between 0 and max.  The absolute
	 * value of clr must be less than the maximum value of an unsigned
	 * short. */
	static inline unsigned char
						   uchar_clamp(double clr, unsigned char max) {
		unsigned short result;

		//assert(fabs(clr) <= SHRT_MAX);

	/* Casting a negative float to an unsigned short is undefined.
	 * However, casting a float to a signed truncates toward zero and
	 * casting a negative signed value to an unsigned of the same size
	 * results in a bit-identical value (assuming twos-complement
	 * arithmetic).	 This is what we want: all legal negative values
	 * for clr will be greater than 255. */

	/* Convert and clamp. */
		result = (unsigned short)(short)(clr + 0.5);
		if (result > max) {
			result = (clr < 0) ? 0 : max;
		}/* if */

		return result;
	}/* uchar_clamp*/

/*
 * Rotate function Added on 2003/12
 * by Pierre-Alain Joye (pierre@php.net)
 **/
	#define ROTATE_DEG2RAD  3.1415926535897932384626433832795/180
	void gdImageSkewX (gdImagePtr dst, gdImagePtr src, int uRow, int iOffset, double dWeight, int clrBack, int ignoretransparent)
	{
		int i, r, g, b, a, clrBackR, clrBackG, clrBackB, clrBackA;
		FuncPtr f;

		int pxlOldLeft, pxlLeft=0, pxlSrc;

	/* Keep clrBack as color index if required */
		if (src->trueColor) {
			pxlOldLeft = clrBack;
			f = gdImageGetTrueColorPixel;
		} else {
			pxlOldLeft = clrBack;
			clrBackR = gdImageRed(src, clrBack);
			clrBackG = gdImageGreen(src, clrBack);
			clrBackB = gdImageBlue(src, clrBack);
			clrBackA = gdImageAlpha(src, clrBack);
			clrBack =  gdTrueColorAlpha(clrBackR, clrBackG, clrBackB, clrBackA);
			f = gdImageGetPixel;
		}

		for (i = 0; i < iOffset; i++) {
			gdImageSetPixel (dst, i, uRow, clrBack);
		}

		if (i < dst->sx) {
			gdImageSetPixel (dst, i, uRow, clrBack);
		}

		for (i = 0; i < src->sx; i++) {
			pxlSrc = f (src,i,uRow);

			r = (int)(gdImageRed(src,pxlSrc) * dWeight);
			g = (int)(gdImageGreen(src,pxlSrc) * dWeight);
			b = (int)(gdImageBlue(src,pxlSrc) * dWeight);
			a = (int)(gdImageAlpha(src,pxlSrc) * dWeight);

			pxlLeft = gdImageColorAllocateAlpha(src, r, g, b, a);

			if (pxlLeft == -1) {
				pxlLeft = gdImageColorClosestAlpha(src, r, g, b, a);
			}

			r = gdImageRed(src,pxlSrc) - (gdImageRed(src,pxlLeft) - gdImageRed(src,pxlOldLeft));
			g = gdImageGreen(src,pxlSrc) - (gdImageGreen(src,pxlLeft) - gdImageGreen(src,pxlOldLeft));
			b = gdImageBlue(src,pxlSrc) - (gdImageBlue(src,pxlLeft) - gdImageBlue(src,pxlOldLeft));
			a = gdImageAlpha(src,pxlSrc) - (gdImageAlpha(src,pxlLeft) - gdImageAlpha(src,pxlOldLeft));

			if (r>255) {
				r = 255;
			}

			if (g>255) {
				g = 255;
			}

			if (b>255) {
				b = 255;
			}

			if (a>127) {
				a = 127;
			}

			if (ignoretransparent && pxlSrc == dst->transparent) {
				pxlSrc = dst->transparent;
			} else {
				pxlSrc = gdImageColorAllocateAlpha(dst, r, g, b, a);

				if (pxlSrc == -1) {
					pxlSrc = gdImageColorClosestAlpha(dst, r, g, b, a);
				}
			}

			if ((i + iOffset >= 0) && (i + iOffset < dst->sx)) {
				gdImageSetPixel (dst, i+iOffset, uRow,  pxlSrc);
			}

			pxlOldLeft = pxlLeft;
		}

		i += iOffset;

		if (i < dst->sx) {
			gdImageSetPixel (dst, i, uRow, pxlLeft);
		}

		gdImageSetPixel (dst, iOffset, uRow, clrBack);

		i--;

		while (++i < dst->sx) {
			gdImageSetPixel (dst, i, uRow, clrBack);
		}
	}

	void gdImageSkewY (gdImagePtr dst, gdImagePtr src, int uCol, int iOffset, double dWeight, int clrBack, int ignoretransparent)
	{
		int i, iYPos=0, r, g, b, a;
		FuncPtr f;
		int pxlOldLeft, pxlLeft=0, pxlSrc;

		if (src->trueColor) {
			f = gdImageGetTrueColorPixel;
		} else {
			f = gdImageGetPixel;
		}

		for (i = 0; i<=iOffset; i++) {
			gdImageSetPixel (dst, uCol, i, clrBack);
		}
		r = (int)((double)gdImageRed(src,clrBack) * dWeight);
		g = (int)((double)gdImageGreen(src,clrBack) * dWeight);
		b = (int)((double)gdImageBlue(src,clrBack) * dWeight);
		a = (int)((double)gdImageAlpha(src,clrBack) * dWeight);

		pxlOldLeft = gdImageColorAllocateAlpha(dst, r, g, b, a);

		for (i = 0; i < src->sy; i++) {
			pxlSrc = f (src, uCol, i);
			iYPos = i + iOffset;

			r = (int)((double)gdImageRed(src,pxlSrc) * dWeight);
			g = (int)((double)gdImageGreen(src,pxlSrc) * dWeight);
			b = (int)((double)gdImageBlue(src,pxlSrc) * dWeight);
			a = (int)((double)gdImageAlpha(src,pxlSrc) * dWeight);

			pxlLeft = gdImageColorAllocateAlpha(src, r, g, b, a);

			if (pxlLeft == -1) {
				pxlLeft = gdImageColorClosestAlpha(src, r, g, b, a);
			}

			r = gdImageRed(src,pxlSrc) - (gdImageRed(src,pxlLeft) - gdImageRed(src,pxlOldLeft));
			g = gdImageGreen(src,pxlSrc) - (gdImageGreen(src,pxlLeft) - gdImageGreen(src,pxlOldLeft));
			b = gdImageBlue(src,pxlSrc) - (gdImageBlue(src,pxlLeft) - gdImageBlue(src,pxlOldLeft));
			a = gdImageAlpha(src,pxlSrc) - (gdImageAlpha(src,pxlLeft) - gdImageAlpha(src,pxlOldLeft));

			if (r>255) {
				r = 255;
			}

			if (g>255) {
				g = 255;
			}

			if (b>255) {
				b = 255;
			}

			if (a>127) {
				a = 127;
			}

			if (ignoretransparent && pxlSrc == dst->transparent) {
				pxlSrc = dst->transparent;
			} else {
				pxlSrc = gdImageColorAllocateAlpha(dst, r, g, b, a);

				if (pxlSrc == -1) {
					pxlSrc = gdImageColorClosestAlpha(dst, r, g, b, a);
				}
			}

			if ((iYPos >= 0) && (iYPos < dst->sy)) {
				gdImageSetPixel (dst, uCol, iYPos, pxlSrc);
			}

			pxlOldLeft = pxlLeft;
		}

		i = iYPos;
		if (i < dst->sy) {
			gdImageSetPixel (dst, uCol, i, pxlLeft);
		}

		i--;
		while (++i < dst->sy) {
			gdImageSetPixel (dst, uCol, i, clrBack);
		}
	}

	/* Rotates an image by 90 degrees (counter clockwise) */
	gdImagePtr gdImageRotate90 (gdImagePtr src, int ignoretransparent)
	{
		int uY, uX;
		int c,r,g,b,a;
		gdImagePtr dst;
		FuncPtr f;

		if (src->trueColor) {
			f = gdImageGetTrueColorPixel;
		} else {
			f = gdImageGetPixel;
		}
		dst = gdImageCreateTrueColor(src->sy, src->sx);
		if (dst != NULL) {
			int old_blendmode = dst->alphaBlendingFlag;
			dst->alphaBlendingFlag = 0;

			dst->transparent = src->transparent;

			gdImagePaletteCopy (dst, src);

			for (uY = 0; uY<src->sy; uY++) {
				for (uX = 0; uX<src->sx; uX++) {
					c = f (src, uX, uY);
					if (!src->trueColor) {
						r = gdImageRed(src,c);
						g = gdImageGreen(src,c);
						b = gdImageBlue(src,c);
						a = gdImageAlpha(src,c);
						c = gdTrueColorAlpha(r, g, b, a);
					}
					if (ignoretransparent && c == dst->transparent) {
						gdImageSetPixel(dst, uY, (dst->sy - uX - 1), dst->transparent);
					} else {
						gdImageSetPixel(dst, uY, (dst->sy - uX - 1), c);
					}
				}
			}
			dst->alphaBlendingFlag = old_blendmode;
		}

		return dst;
	}

	/* Rotates an image by 180 degrees (counter clockwise) */
	gdImagePtr gdImageRotate180 (gdImagePtr src, int ignoretransparent)
	{
		int uY, uX;
		int c,r,g,b,a;
		gdImagePtr dst;
		FuncPtr f;

		if (src->trueColor) {
			f = gdImageGetTrueColorPixel;
		} else {
			f = gdImageGetPixel;
		}
		dst = gdImageCreateTrueColor(src->sx, src->sy);

		if (dst != NULL) {
			int old_blendmode = dst->alphaBlendingFlag;
			dst->alphaBlendingFlag = 0;

			dst->transparent = src->transparent;

			gdImagePaletteCopy (dst, src);

			for (uY = 0; uY<src->sy; uY++) {
				for (uX = 0; uX<src->sx; uX++) {
					c = f (src, uX, uY);
					if (!src->trueColor) {
						r = gdImageRed(src,c);
						g = gdImageGreen(src,c);
						b = gdImageBlue(src,c);
						a = gdImageAlpha(src,c);
						c = gdTrueColorAlpha(r, g, b, a);
					}

					if (ignoretransparent && c == dst->transparent) {
						gdImageSetPixel(dst, (dst->sx - uX - 1), (dst->sy - uY - 1), dst->transparent);
					} else {
						gdImageSetPixel(dst, (dst->sx - uX - 1), (dst->sy - uY - 1), c);
					}
				}
			}
			dst->alphaBlendingFlag = old_blendmode;
		}

		return dst;
	}

	/* Rotates an image by 270 degrees (counter clockwise) */
	gdImagePtr gdImageRotate270 (gdImagePtr src, int ignoretransparent)
	{
		int uY, uX;
		int c,r,g,b,a;
		gdImagePtr dst;
		FuncPtr f;

		if (src->trueColor) {
			f = gdImageGetTrueColorPixel;
		} else {
			f = gdImageGetPixel;
		}
		dst = gdImageCreateTrueColor (src->sy, src->sx);

		if (dst != NULL) {
			int old_blendmode = dst->alphaBlendingFlag;
			dst->alphaBlendingFlag = 0;

			dst->transparent = src->transparent;

			gdImagePaletteCopy (dst, src);

			for (uY = 0; uY<src->sy; uY++) {
				for (uX = 0; uX<src->sx; uX++) {
					c = f (src, uX, uY);
					if (!src->trueColor) {
						r = gdImageRed(src,c);
						g = gdImageGreen(src,c);
						b = gdImageBlue(src,c);
						a = gdImageAlpha(src,c);
						c = gdTrueColorAlpha(r, g, b, a);
					}

					if (ignoretransparent && c == dst->transparent) {
						gdImageSetPixel(dst, (dst->sx - uY - 1), uX, dst->transparent);
					} else {
						gdImageSetPixel(dst, (dst->sx - uY - 1), uX, c);
					}
				}
			}
			dst->alphaBlendingFlag = old_blendmode;
		}

		return dst;
	}

	gdImagePtr gdImageRotate45 (gdImagePtr src, double dAngle, int clrBack, int ignoretransparent)
	{
		gdImagePtr dst1,dst2,dst3;
		double dRadAngle, dSinE, dTan, dShear;
		double dOffset;     /* Variable skew offset */
		int u, iShear, newx, newy;
		int clrBackR, clrBackG, clrBackB, clrBackA;

	/* See GEMS I for the algorithm details */
		dRadAngle = dAngle * ROTATE_DEG2RAD; /* Angle in radians */
		dSinE = sin (dRadAngle);
		dTan = tan (dRadAngle / 2.0);

		newx = (int)(src->sx + src->sy * fabs(dTan));
		newy = src->sy;

	/* 1st shear */
		dst1 = gdImageCreateTrueColor(newx, newy);
		/******* Perform 1st shear (horizontal) ******/
		if (dst1 == NULL) {
			return NULL;
		}
		#ifdef HAVE_GD_BUNDLED
		dst1->alphaBlendingFlag = gdEffectReplace;
		#else
		gdImageAlphaBlending(dst1, 0);
		#endif
		if (dAngle == 0.0) {
		/* Returns copy of src */
			gdImageCopy (dst1, src,0,0,0,0,src->sx,src->sy);
			return dst1;
		}

		gdImagePaletteCopy (dst1, src);

		if (ignoretransparent) {
			if (gdImageTrueColor(src)) {
				dst1->transparent = src->transparent;
			} else {

				dst1->transparent = gdTrueColorAlpha(gdImageRed(src, src->transparent), gdImageBlue(src, src->transparent), gdImageGreen(src, src->transparent), 127);
			}
		}

		for (u = 0; u < dst1->sy; u++) {
			if (dTan >= 0.0) {
				dShear = ((double)(u + 0.5)) * dTan;
			} else {
				dShear = ((double)(u - dst1->sy) + 0.5) * dTan;
			}

			iShear = (int)floor(dShear);
			gdImageSkewX(dst1, src, u, iShear, (dShear - iShear), clrBack, ignoretransparent);
		}

	/*
	The 1st shear may use the original clrBack as color index
	Convert it once here
	*/
		if(!src->trueColor) {
			clrBackR = gdImageRed(src, clrBack);
			clrBackG = gdImageGreen(src, clrBack);
			clrBackB = gdImageBlue(src, clrBack);
			clrBackA = gdImageAlpha(src, clrBack);
			clrBack =  gdTrueColorAlpha(clrBackR, clrBackG, clrBackB, clrBackA);
		}
	/* 2nd shear */
		newx = dst1->sx;

		if (dSinE > 0.0) {
			dOffset = (src->sx-1) * dSinE;
		} else {
			dOffset = -dSinE *  (src->sx - newx);
		}

		newy = (int) ((double) src->sx * fabs( dSinE ) + (double) src->sy * cos (dRadAngle))+1;

		dst2 = gdImageCreateTrueColor(newx, newy);
		if (dst2 == NULL) {
			gdImageDestroy(dst1);
			return NULL;
		}

		#ifdef HAVE_GD_BUNDLED
		dst2->alphaBlendingFlag = gdEffectReplace;
		#else
		gdImageAlphaBlending(dst2, 0);
		#endif

		if (ignoretransparent) {
			dst2->transparent = dst1->transparent;
		}

		for (u = 0; u < dst2->sx; u++, dOffset -= dSinE) {
			iShear = (int)floor (dOffset);
			gdImageSkewY(dst2, dst1, u, iShear, (dOffset - (double)iShear), clrBack, ignoretransparent);
		}

	/* 3rd shear */
		gdImageDestroy(dst1);

		newx = (int) ((double)src->sy * fabs (dSinE) + (double)src->sx * cos (dRadAngle)) + 1;
		newy = dst2->sy;

		dst3 = gdImageCreateTrueColor(newx, newy);
		if (dst3 == NULL) {
			gdImageDestroy(dst2);
			return NULL;
		}

		#ifdef HAVE_GD_BUNDLED
		dst3->alphaBlendingFlag = gdEffectReplace;
		#else
		gdImageAlphaBlending(dst3, 0);
		#endif

		if (ignoretransparent) {
			dst3->transparent = dst2->transparent;
		}

		if (dSinE >= 0.0) {
			dOffset = (double)(src->sx - 1) * dSinE * -dTan;
		} else {
			dOffset = dTan * ((double)(src->sx - 1) * -dSinE + (double)(1 - newy));
		}

		for (u = 0; u < dst3->sy; u++, dOffset += dTan) {
			int iShear = (int)floor(dOffset);
			gdImageSkewX(dst3, dst2, u, iShear, (dOffset - iShear), clrBack, ignoretransparent);
		}

		gdImageDestroy(dst2);

		return dst3;
	}

	gdImagePtr gdImageRotate (gdImagePtr src, double dAngle, int clrBack, int ignoretransparent)
	{
		gdImagePtr pMidImg;
		gdImagePtr rotatedImg;

		if (src == NULL) {
			return NULL;
		}

		if (!gdImageTrueColor(src) && (clrBack < 0 || clrBack>=gdImageColorsTotal(src))) {
			return NULL;
		}

		while (dAngle >= 360.0) {
			dAngle -= 360.0;
		}

		while (dAngle < 0) {
			dAngle += 360.0;
		}

		if (dAngle == 90.00) {
			return gdImageRotate90(src, ignoretransparent);
		}
		if (dAngle == 180.00) {
			return gdImageRotate180(src, ignoretransparent);
		}
		if(dAngle == 270.00) {
			return gdImageRotate270 (src, ignoretransparent);
		}

		if ((dAngle > 45.0) && (dAngle <= 135.0)) {
			pMidImg = gdImageRotate90 (src, ignoretransparent);
			dAngle -= 90.0;
		} else if ((dAngle > 135.0) && (dAngle <= 225.0)) {
			pMidImg = gdImageRotate180 (src, ignoretransparent);
			dAngle -= 180.0;
		} else if ((dAngle > 225.0) && (dAngle <= 315.0)) {
			pMidImg = gdImageRotate270 (src, ignoretransparent);
			dAngle -= 270.0;
		} else {
			return gdImageRotate45 (src, dAngle, clrBack, ignoretransparent);
		}

		if (pMidImg == NULL) {
			return NULL;
		}

		rotatedImg = gdImageRotate45 (pMidImg, dAngle, clrBack, ignoretransparent);
		gdImageDestroy(pMidImg);

		return rotatedImg;
	}
/* End Rotate function */




}
