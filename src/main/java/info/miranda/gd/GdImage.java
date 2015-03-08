package info.miranda.gd;

/*
   Group: Types

   typedef: gdImage

   typedef: gdImagePtr

   The data structure in which gd stores images. <gdImageCreate>,
   <gdImageCreateTrueColor> and the various image file-loading functions
   return a pointer to this type, and the other functions expect to
   receive a pointer to this type as their first argument.

   *gdImagePtr* is a pointer to *gdImage*.

   (Previous versions of this library encouraged directly manipulating
   the contents ofthe struct but we are attempting to move away from
   this practice so the fields are no longer documented here.  If you
   need to poke at the internals of this struct, feel free to look at
   *gd.h*.)
*/
public class GdImage {
	/* Palette-based image pixels */
	byte[][] pixels;
	int sx;
	int sy;
	/* These are valid in palette images only. See also
	   'alpha', which appears later in the structure to
	   preserve binary backwards compatibility */
	int colorsTotal;
	int[] red = new int[GdUtils.MAX_COLORS];
	int[] green = new int[GdUtils.MAX_COLORS];
	int[] blue = new int[GdUtils.MAX_COLORS];
	int[] open = new int[GdUtils.MAX_COLORS];
	/* For backwards compatibility, this is set to the
	   first palette entry with 100% transparency,
	   and is also set and reset by the
	   gdImageColorTransparent function. Newer
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
	int trueColor;
	int[][] tpixels;
	/* Should alpha channel be copied, or applied, each time a
	   pixel is drawn? This applies to truecolor images only.
	   No attempt is made to alpha-blend in palette images,
	   even if semitransparent palette entries exist.
	   To do that, build your image as a truecolor image,
	   then quantize down to 8 bits. */
	int alphaBlendingFlag;
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
	  gdImageSetClip */
	int cx1;
	int cy1;
	int cx2;
	int cy2;

	/* 2.1.0: allows to specify resolution in dpi */
	int res_x;
	int res_y;

	/* Selects quantization method, see gdImageTrueColorToPaletteSetMethod() and gdPaletteQuantizationMethod enum. */
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
	/*
	interpolation_method interpolation;
	*/


	/* 2.0.12: this now checks the clipping rectangle */
	private boolean gdImageBoundsSafeMacro(final int x, final int y) {
		return (!(((y < cy1) || (y > cy2)) || ((x < cx1) || (x > cx2))));
	}

	/*
		Function: gdImageCreate
	
		  gdImageCreate is called to create palette-based images, with no
		  more than 256 colors. The image must eventually be destroyed using
		  gdImageDestroy().
	
		Parameters:
	
			sx - The image width.
			sy - The image height.
	
		Returns:
	
			A pointer to the new image or NULL if an error occurred.
	
		Example:
	
			>   gdImagePtr im;
			>   im = gdImageCreate(64, 64);
			>   // ... Use the image ...
			>   gdImageDestroy(im);
	
		See Also:
	
			<gdImageCreateTrueColor>        
	
	 */
	public GdImage(final int sx, final int sy) {
		pixels = new byte[sy][sx];
		this.sx = sx;
		this.sy = sy;
		colorsTotal = 0;
		transparent = (-1);
		interlace = 0;
		thick = 1;
		AA = 0;
		for (int i = 0; (i < GdUtils.MAX_COLORS); i++) {
			open[i] = 1;
		};
		trueColor = 0;
		cx1 = 0;
		cy1 = 0;
		cx2 = sx - 1;
		cy2 = sy - 1;
		res_x = GdUtils.GD_RESOLUTION;
		res_y = GdUtils.GD_RESOLUTION;
//		interpolation = NULL;
		interpolation_id = GdInterpolationMethod.GD_BILINEAR_FIXED;
	}




	/*
		Function: gdImageCreateTrueColor
	
		  <gdImageCreateTrueColor> is called to create truecolor images,
		  with an essentially unlimited number of colors. Invoke
		  <gdImageCreateTrueColor> with the x and y dimensions of the
		  desired image. <gdImageCreateTrueColor> returns a <gdImagePtr>
		  to the new image, or NULL if unable to allocate the image. The
		  image must eventually be destroyed using <gdImageDestroy>().
	
		  Truecolor images are always filled with black at creation
		  time. There is no concept of a "background" color index.
	
		Parameters:
	
			sx - The image width.
			sy - The image height.
	
		Returns:
	
			A pointer to the new image or NULL if an error occurred.
	
		Example:
	
			> gdImagePtr im;
			> im = gdImageCreateTrueColor(64, 64);
			> // ... Use the image ...
			> gdImageDestroy(im);
	
		See Also:
	
			<gdImageCreateTrueColor>        
	
	*/
	public GdImage(final int sx, final int sy, final boolean truecolor) {
		pixels = new byte[sy][sx];
		this.sx = sx;
		this.sy = sy;
		transparent = (-1);
		interlace = 0;
		trueColor = 1;
	/* 2.0.2: alpha blending is now on by default, and saving of alpha is
	   off by default. This allows font antialiasing to work as expected
	   on the first try in JPEGs -- quite important -- and also allows
	   for smaller PNGs when saving of alpha channel is not really
	   desired, which it usually isn't! */
		saveAlphaFlag = 0;
		alphaBlendingFlag = 1;
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
	}

}
