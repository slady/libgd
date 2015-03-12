package info.miranda.gd;

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
	private boolean boundsSafeMacro(final int x, final int y) {
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
		if (boundsSafeMacro(x, y)) {
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
			return GdUtils.gdTrueColorAlpha(red[p], green[p], blue[p],
					(transparent == p) ? GdUtils.ALPHA_TRANSPARENT : alpha[p]);
		} else {
			return p;
		}
	}

	/* This function accepts truecolor pixel values only. The
     source color is composited with the destination color
     based on the alpha channel value of the source color.
     The resulting color is opaque. */

/* Thanks to Frank Warmerdam for this superior implementation
	of gdAlphaBlend(), which merges alpha in the
	destination color much better. */

	private int gdAlphaBlend(final int dst, final int src) {
		int src_alpha = GdUtils.gdTrueColorGetAlpha(src);
		int dst_alpha, alpha, red, green, blue;
		int src_weight, dst_weight, tot_weight;

	/* -------------------------------------------------------------------- */
	/*      Simple cases we want to handle fast.                            */
	/* -------------------------------------------------------------------- */
		if (src_alpha == GdUtils.ALPHA_OPAQUE)
			return src;

		dst_alpha = GdUtils.gdTrueColorGetAlpha(dst);
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

		red = (GdUtils.gdTrueColorGetRed(src) * src_weight
				+ GdUtils.gdTrueColorGetRed(dst) * dst_weight) / tot_weight;
		green = (GdUtils.gdTrueColorGetGreen(src) * src_weight
				+ GdUtils.gdTrueColorGetGreen(dst) * dst_weight) / tot_weight;
		blue = (GdUtils.gdTrueColorGetBlue(src) * src_weight
				+ GdUtils.gdTrueColorGetBlue(dst) * dst_weight) / tot_weight;

	/* -------------------------------------------------------------------- */
	/*      Return merged result.                                           */
	/* -------------------------------------------------------------------- */
		return ((alpha << 24) + (red << 16) + (green << 8) + blue);
	}

	private int gdLayerOverlay(final int dst, final int src) {
		int a1, a2;
		a1 = GdUtils.ALPHA_MAX - GdUtils.gdTrueColorGetAlpha(dst);
		a2 = GdUtils.ALPHA_MAX - GdUtils.gdTrueColorGetAlpha(src);
		return (((GdUtils.ALPHA_MAX - a1 * a2 / GdUtils.ALPHA_MAX) << 24) +
				(gdAlphaOverlayColor(GdUtils.gdTrueColorGetRed(src),
						GdUtils.gdTrueColorGetRed(dst), GdUtils.RED_MAX) << 16) +
				(gdAlphaOverlayColor(GdUtils.gdTrueColorGetGreen(src),
						GdUtils.gdTrueColorGetGreen(dst), GdUtils.GREEN_MAX) << 8) +
				(gdAlphaOverlayColor(GdUtils.gdTrueColorGetBlue(src),
						GdUtils.gdTrueColorGetBlue(dst), GdUtils.BLUE_MAX)));
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
		a1 = GdUtils.ALPHA_MAX - GdUtils.gdTrueColorGetAlpha(src);
		a2 = GdUtils.ALPHA_MAX - GdUtils.gdTrueColorGetAlpha(dst);

		r1 = GdUtils.RED_MAX - (a1 * (GdUtils.RED_MAX - GdUtils.gdTrueColorGetRed(src))) / GdUtils.ALPHA_MAX;
		r2 = GdUtils.RED_MAX - (a2 * (GdUtils.RED_MAX - GdUtils.gdTrueColorGetRed(dst))) / GdUtils.ALPHA_MAX;
		g1 = GdUtils.GREEN_MAX - (a1 * (GdUtils.GREEN_MAX - GdUtils.gdTrueColorGetGreen(src))) / GdUtils.ALPHA_MAX;
		g2 = GdUtils.GREEN_MAX - (a2 * (GdUtils.GREEN_MAX - GdUtils.gdTrueColorGetGreen(dst))) / GdUtils.ALPHA_MAX;
		b1 = GdUtils.BLUE_MAX - (a1 * (GdUtils.BLUE_MAX - GdUtils.gdTrueColorGetBlue(src))) / GdUtils.ALPHA_MAX;
		b2 = GdUtils.BLUE_MAX - (a2 * (GdUtils.BLUE_MAX - GdUtils.gdTrueColorGetBlue(dst))) / GdUtils.ALPHA_MAX ;

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
				if (boundsSafeMacro(x, y)) {
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
											GdUtils.gdTrueColorGetRed(p),
											GdUtils.gdTrueColorGetGreen(p),
											GdUtils.gdTrueColorGetBlue(p),
											GdUtils.gdTrueColorGetAlpha(p)));
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
					p = GdUtils.gdTrueColorAlpha(tile.red[p], tile.green[p], tile.blue[p], tile.alpha[p]);
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
									GdUtils.gdTrueColorGetRed(p),
									GdUtils.gdTrueColorGetGreen(p),
									GdUtils.gdTrueColorGetBlue(p),
									GdUtils.gdTrueColorGetAlpha(p)));
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

	private int colorResolve(int r, int g, int b) {
		return colorResolveAlpha(r, g, b, GdUtils.ALPHA_OPAQUE);
	}

	private int colorResolveAlpha(int r, int g, int b, int a) {
		int c;
		int ct = -1;
		int op = -1;
		long rd, gd, bd, ad, dist;
		long mindist = 4 * 255 * 255;	/* init to max poss dist */
		if (trueColor) {
			return GdUtils.gdTrueColorAlpha(r, g, b, a);
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
		if (!boundsSafeMacro(x, y)) {
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
		dr = GdUtils.gdTrueColorGetRed(color);
		dg = GdUtils.gdTrueColorGetGreen(color);
		db = GdUtils.gdTrueColorGetBlue(color);

		r = GdUtils.gdTrueColorGetRed(p);
		g = GdUtils.gdTrueColorGetGreen(p);
		b = GdUtils.gdTrueColorGetBlue(p);

		dr = blendColor(t, r, dr);
		dg = blendColor(t, g, dg);
		db = blendColor(t, b, db);
		tpixels[y][x] = GdUtils.gdTrueColorAlpha(dr, dg, db, GdUtils.ALPHA_OPAQUE);
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

	/* Line thickness (defaults to 1). Affects lines, ellipses,
	   rectangles, polygons and so forth. */
	public void setThickness(int thickness) {
		thick = thickness;
	}

	public int getRed(final int c) {
		return trueColor ? GdUtils.gdTrueColorGetRed(c) : red[(c)];
	}

	public int getGreen(final int c) {
		return trueColor ? GdUtils.gdTrueColorGetGreen(c) : green[(c)];
	}

	public int getBlue(final int c) {
		return trueColor ? GdUtils.gdTrueColorGetBlue(c) : blue[(c)];
	}

	public int getAlpha(final int c) {
		return trueColor ? GdUtils.gdTrueColorGetAlpha(c) : alpha[(c)];
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
					dst_row[x] = GdUtils.gdTrueColorAlpha(0, 0, 0, 127);
				} else {
					dst_row[x] = GdUtils.gdTrueColorAlpha(red[c], green[c], blue[c], alpha[c]);
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
			return GdUtils.gdTrueColorAlpha(r, g, b, a);
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

}
