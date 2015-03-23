#ifdef __cplusplus
extern "C" {
#endif


#ifndef GD_H
#define GD_H 1

#ifdef __cplusplus
	extern "C"
	{
#endif

/* stdio is needed for file I/O. */
#include <stdio.h>
#include <stdarg.h>
#include "gd_io.h"


/* define struct with name and func ptr and add it to gdImageStruct gdInterpolationMethod interpolation; */

/* Functions to manipulate images. */

/* Creates an image from various file types. These functions
   return a palette or truecolor image based on the
   nature of the file being loaded. Truecolor PNG
   stays truecolor; palette PNG stays palette-based;
   JPEG is always truecolor. */
BGD_DECLARE(gdImagePtr) gdImageCreateFromPng (FILE * fd);
BGD_DECLARE(gdImagePtr) gdImageCreateFromPngCtx (gdIOCtxPtr in);
BGD_DECLARE(gdImagePtr) gdImageCreateFromPngPtr (int size, void *data);

/* These read the first frame only */
BGD_DECLARE(gdImagePtr) gdImageCreateFromGif (FILE * fd);
BGD_DECLARE(gdImagePtr) gdImageCreateFromGifCtx (gdIOCtxPtr in);
BGD_DECLARE(gdImagePtr) gdImageCreateFromGifPtr (int size, void *data);
BGD_DECLARE(gdImagePtr) gdImageCreateFromWBMP (FILE * inFile);
BGD_DECLARE(gdImagePtr) gdImageCreateFromWBMPCtx (gdIOCtx * infile);
BGD_DECLARE(gdImagePtr) gdImageCreateFromWBMPPtr (int size, void *data);
BGD_DECLARE(gdImagePtr) gdImageCreateFromJpeg (FILE * infile);
BGD_DECLARE(gdImagePtr) gdImageCreateFromJpegEx (FILE * infile, int ignore_warning);
BGD_DECLARE(gdImagePtr) gdImageCreateFromJpegCtx (gdIOCtx * infile);
BGD_DECLARE(gdImagePtr) gdImageCreateFromJpegCtxEx (gdIOCtx * infile, int ignore_warning);
BGD_DECLARE(gdImagePtr) gdImageCreateFromJpegPtr (int size, void *data);
BGD_DECLARE(gdImagePtr) gdImageCreateFromJpegPtrEx (int size, void *data, int ignore_warning);
BGD_DECLARE(gdImagePtr) gdImageCreateFromWebp (FILE * inFile);
BGD_DECLARE(gdImagePtr) gdImageCreateFromWebpPtr (int size, void *data);
BGD_DECLARE(gdImagePtr) gdImageCreateFromWebpCtx (gdIOCtx * infile);

BGD_DECLARE(gdImagePtr) gdImageCreateFromTiff(FILE *inFile);
BGD_DECLARE(gdImagePtr) gdImageCreateFromTiffCtx(gdIOCtx *infile);
BGD_DECLARE(gdImagePtr) gdImageCreateFromTiffPtr(int size, void *data);

BGD_DECLARE(gdImagePtr) gdImageCreateFromTga( FILE * fp );
BGD_DECLARE(gdImagePtr) gdImageCreateFromTgaCtx(gdIOCtx* ctx);
BGD_DECLARE(gdImagePtr) gdImageCreateFromTgaPtr(int size, void *data);

BGD_DECLARE(gdImagePtr) gdImageCreateFromBmp (FILE * inFile);
BGD_DECLARE(gdImagePtr) gdImageCreateFromBmpPtr (int size, void *data);
BGD_DECLARE(gdImagePtr) gdImageCreateFromBmpCtx (gdIOCtxPtr infile);
BGD_DECLARE(gdImagePtr) gdImageCreateFromFile(const char *filename);


/*
  Group: Types

  typedef: gdSource

  typedef: gdSourcePtr

    *Note:* This interface is *obsolete* and kept only for
    *compatibility.  Use <gdIOCtx> instead.

    Represents a source from which a PNG can be read. Programmers who
    do not wish to read PNGs from a file can provide their own
    alternate input mechanism, using the <gdImageCreateFromPngSource>
    function. See the documentation of that function for an example of
    the proper use of this type.

    > typedef struct {
    >         int (*source) (void *context, char *buffer, int len);
    >         void *context;
    > } gdSource, *gdSourcePtr;

    The source function must return -1 on error, otherwise the number
    of bytes fetched. 0 is EOF, not an error!

   'context' will be passed to your source function.

*/
typedef struct {
	int (*source) (void *context, char *buffer, int len);
	void *context;
}
gdSource, *gdSourcePtr;

/* Deprecated in favor of gdImageCreateFromPngCtx */
BGD_DECLARE(gdImagePtr) gdImageCreateFromPngSource (gdSourcePtr in);

BGD_DECLARE(gdImagePtr) gdImageCreateFromGd (FILE * in);
BGD_DECLARE(gdImagePtr) gdImageCreateFromGdCtx (gdIOCtxPtr in);
BGD_DECLARE(gdImagePtr) gdImageCreateFromGdPtr (int size, void *data);

BGD_DECLARE(gdImagePtr) gdImageCreateFromGd2 (FILE * in);
BGD_DECLARE(gdImagePtr) gdImageCreateFromGd2Ctx (gdIOCtxPtr in);
BGD_DECLARE(gdImagePtr) gdImageCreateFromGd2Ptr (int size, void *data);

BGD_DECLARE(gdImagePtr) gdImageCreateFromGd2Part (FILE * in, int srcx, int srcy, int w,
						  int h);
BGD_DECLARE(gdImagePtr) gdImageCreateFromGd2PartCtx (gdIOCtxPtr in, int srcx, int srcy,
						     int w, int h);
BGD_DECLARE(gdImagePtr) gdImageCreateFromGd2PartPtr (int size, void *data, int srcx, int srcy,
						     int w, int h);
/* 2.0.10: prototype was missing */
BGD_DECLARE(gdImagePtr) gdImageCreateFromXbm (FILE * in);
BGD_DECLARE(void) gdImageXbmCtx(gdImagePtr image, char* file_name, int fg, gdIOCtx * out);

/* NOTE: filename, not FILE */
BGD_DECLARE(gdImagePtr) gdImageCreateFromXpm (char *filename);



/* FreeType 2 text output with hook to extra flags */

BGD_DECLARE(void) gdImageAABlend (gdImagePtr im);

/* 2.0.16: for thread-safe use of gdImageStringFT and friends,
   call this before allowing any thread to call gdImageStringFT.
   Otherwise it is invoked by the first thread to invoke
   gdImageStringFT, with a very small but real risk of a race condition.
   Return 0 on success, nonzero on failure to initialize freetype. */
BGD_DECLARE(int) gdFontCacheSetup (void);

/* Optional: clean up after application is done using fonts in
   gdImageStringFT(). */
BGD_DECLARE(void) gdFontCacheShutdown (void);
/* 2.0.20: for backwards compatibility. A few applications did start calling
   this function when it first appeared although it was never documented.
   Simply invokes gdFontCacheShutdown. */
BGD_DECLARE(void) gdFreeFontCache (void);

/* Calls gdImageStringFT. Provided for backwards compatibility only. */
BGD_DECLARE(char *) gdImageStringTTF (gdImage * im, int *brect, int fg, char *fontlist,
                                      double ptsize, double angle, int x, int y,
                                      char *string);

/* FreeType 2 text output */
BGD_DECLARE(char *) gdImageStringFT (gdImage * im, int *brect, int fg, char *fontlist,
                                     double ptsize, double angle, int x, int y,
                                     char *string);


/*
  Group: Types

  typedef: gdFTStringExtra

  typedef: gdFTStringExtraPtr

  A structure and associated pointer type used to pass additional
  parameters to the <gdImageStringFTEx> function. See
  <gdImageStringFTEx> for the structure definition.

  Thanks to Wez Furlong.
*/

/* 2.0.5: provides an extensible way to pass additional parameters.
   Thanks to Wez Furlong, sorry for the delay. */
typedef struct {
	int flags;		/* Logical OR of gdFTEX_ values */
	double linespacing;	/* fine tune line spacing for '\n' */
	int charmap;		/* TBB: 2.0.12: may be gdFTEX_Unicode,
				   gdFTEX_Shift_JIS, gdFTEX_Big5,
				   or gdFTEX_Adobe_Custom;
				   when not specified, maps are searched
				   for in the above order. */
	int hdpi;                /* if (flags & gdFTEX_RESOLUTION) */
	int vdpi;		 /* if (flags & gdFTEX_RESOLUTION) */
	char *xshow;             /* if (flags & gdFTEX_XSHOW)
				    then, on return, xshow is a malloc'ed
				    string containing xshow position data for
				    the last string.
				    
				    NB. The caller is responsible for gdFree'ing
				    the xshow string.
				 */
	char *fontpath;	         /* if (flags & gdFTEX_RETURNFONTPATHNAME)
				    then, on return, fontpath is a malloc'ed
				    string containing the actual font file path name
				    used, which can be interesting when fontconfig
				    is in use.
				    
				    The caller is responsible for gdFree'ing the
				    fontpath string.
				 */

}
gdFTStringExtra, *gdFTStringExtraPtr;

#define gdFTEX_LINESPACE 1
#define gdFTEX_CHARMAP 2
#define gdFTEX_RESOLUTION 4
#define gdFTEX_DISABLE_KERNING 8
#define gdFTEX_XSHOW 16
/* The default unless gdFTUseFontConfig(1); has been called:
   fontlist is a full or partial font file pathname or list thereof
   (i.e. just like before 2.0.29) */
#define gdFTEX_FONTPATHNAME 32
/* Necessary to use fontconfig patterns instead of font pathnames
   as the fontlist argument, unless gdFTUseFontConfig(1); has
   been called. New in 2.0.29 */
#define gdFTEX_FONTCONFIG 64
/* Sometimes interesting when fontconfig is used: the fontpath
   element of the structure above will contain a gdMalloc'd string
   copy of the actual font file pathname used, if this flag is set
   when the call is made */
#define gdFTEX_RETURNFONTPATHNAME 128

/* If flag is nonzero, the fontlist parameter to gdImageStringFT
   and gdImageStringFTEx shall be assumed to be a fontconfig font pattern
   if fontconfig was compiled into gd. This function returns zero
   if fontconfig is not available, nonzero otherwise. */
BGD_DECLARE(int) gdFTUseFontConfig(int flag);

/* These are NOT flags; set one in 'charmap' if you set the
   gdFTEX_CHARMAP bit in 'flags'. */
#define gdFTEX_Unicode 0
#define gdFTEX_Shift_JIS 1
#define gdFTEX_Big5 2
#define gdFTEX_Adobe_Custom 3

BGD_DECLARE(char *) gdImageStringFTEx (gdImage * im, int *brect, int fg, char *fontlist,
                                       double ptsize, double angle, int x, int y,
                                       char *string, gdFTStringExtraPtr strex);

/* Converts a truecolor image to a palette-based image,
   using a high-quality two-pass quantization routine
   which attempts to preserve alpha channel information
   as well as R/G/B color information when creating
   a palette. If ditherFlag is set, the image will be
   dithered to approximate colors better, at the expense
   of some obvious "speckling." colorsWanted can be
   anything up to 256. If the original source image
   includes photographic information or anything that
   came out of a JPEG, 256 is strongly recommended.
   
   Better yet, don't use these function -- write real
   truecolor PNGs and JPEGs. The disk space gain of
   conversion to palette is not great (for small images
   it can be negative) and the quality loss is ugly.
   
   DIFFERENCES: gdImageCreatePaletteFromTrueColor creates and
   returns a new image. gdImageTrueColorToPalette modifies
   an existing image, and the truecolor pixels are discarded.
   
   gdImageTrueColorToPalette() returns TRUE on success, FALSE on failure.
*/

BGD_DECLARE(gdImagePtr) gdImageCreatePaletteFromTrueColor (gdImagePtr im, int ditherFlag,
							   int colorsWanted);

BGD_DECLARE(int) gdImageTrueColorToPalette (gdImagePtr im, int ditherFlag,
					    int colorsWanted);

/* An attempt at getting the results of gdImageTrueColorToPalette to
 * look a bit more like the original (im1 is the original and im2 is
 * the palette version */

BGD_DECLARE(int) gdImageColorMatch(gdImagePtr im1, gdImagePtr im2);

/* Selects quantization method used for subsequent gdImageTrueColorToPalette calls.
   See gdPaletteQuantizationMethod enum (e.g. GD_QUANT_NEUQUANT, GD_QUANT_LIQ).
   Speed is from 1 (highest quality) to 10 (fastest).
   Speed 0 selects method-specific default (recommended).

   Returns FALSE if the given method is invalid or not available.
*/
BGD_DECLARE(int) gdImageTrueColorToPaletteSetMethod (gdImagePtr im, int method, int speed);

/*
  Chooses quality range that subsequent call to gdImageTrueColorToPalette will aim for.
  Min and max quality is in range 1-100 (1 = ugly, 100 = perfect). Max must be higher than min.
  If palette cannot represent image with at least min_quality, then image will remain true-color.
  If palette can represent image with quality better than max_quality, then lower number of colors will be used.
  This function has effect only when GD_QUANT_LIQ method has been selected and the source image is true-color.
*/
BGD_DECLARE(void) gdImageTrueColorToPaletteSetQuality (gdImagePtr im, int min_quality, int max_quality);

BGD_DECLARE(void) gdImageGif (gdImagePtr im, FILE * out);
BGD_DECLARE(void) gdImagePng (gdImagePtr im, FILE * out);
BGD_DECLARE(void) gdImagePngCtx (gdImagePtr im, gdIOCtx * out);
BGD_DECLARE(void) gdImageGifCtx (gdImagePtr im, gdIOCtx * out);
BGD_DECLARE(void) gdImageTiff(gdImagePtr im, FILE *outFile);
BGD_DECLARE(void *) gdImageTiffPtr(gdImagePtr im, int *size);
BGD_DECLARE(void) gdImageTiffCtx(gdImagePtr image, gdIOCtx *out);

BGD_DECLARE(void *) gdImageBmpPtr(gdImagePtr im, int *size, int compression);
BGD_DECLARE(void) gdImageBmp(gdImagePtr im, FILE *outFile, int compression);
BGD_DECLARE(void) gdImageBmpCtx(gdImagePtr im, gdIOCtxPtr out, int compression);

/* 2.0.12: Compression level: 0-9 or -1, where 0 is NO COMPRESSION at all,
   1 is FASTEST but produces larger files, 9 provides the best
   compression (smallest files) but takes a long time to compress, and
   -1 selects the default compiled into the zlib library. */
BGD_DECLARE(void) gdImagePngEx (gdImagePtr im, FILE * out, int level);
BGD_DECLARE(void) gdImagePngCtxEx (gdImagePtr im, gdIOCtx * out, int level);

BGD_DECLARE(void) gdImageWBMP (gdImagePtr image, int fg, FILE * out);
BGD_DECLARE(void) gdImageWBMPCtx (gdImagePtr image, int fg, gdIOCtx * out);

BGD_DECLARE(int) gdImageFile(gdImagePtr im, const char *filename);
BGD_DECLARE(int) gdSupportsFileType(const char *filename, int writing);


/* Guaranteed to correctly free memory returned by the gdImage*Ptr
   functions */
BGD_DECLARE(void) gdFree (void *m);

/* Best to free this memory with gdFree(), not free() */
BGD_DECLARE(void *) gdImageWBMPPtr (gdImagePtr im, int *size, int fg);

/* 100 is highest quality (there is always a little loss with JPEG).
   0 is lowest. 10 is about the lowest useful setting. */
BGD_DECLARE(void) gdImageJpeg (gdImagePtr im, FILE * out, int quality);
BGD_DECLARE(void) gdImageJpegCtx (gdImagePtr im, gdIOCtx * out, int quality);

/* Best to free this memory with gdFree(), not free() */
BGD_DECLARE(void *) gdImageJpegPtr (gdImagePtr im, int *size, int quality);

BGD_DECLARE(void) gdImageWebpEx (gdImagePtr im, FILE * outFile, int quantization);
BGD_DECLARE(void) gdImageWebp (gdImagePtr im, FILE * outFile);
BGD_DECLARE(void *) gdImageWebpPtr (gdImagePtr im, int *size);
BGD_DECLARE(void *) gdImageWebpPtrEx (gdImagePtr im, int *size, int quantization);
BGD_DECLARE(void) gdImageWebpCtx (gdImagePtr im, gdIOCtx * outfile, int quantization);

BGD_DECLARE(void) gdImageGifAnimBegin(gdImagePtr im, FILE *outFile, int GlobalCM, int Loops);
BGD_DECLARE(void) gdImageGifAnimAdd(gdImagePtr im, FILE *outFile, int LocalCM, int LeftOfs, int TopOfs, int Delay, int Disposal, gdImagePtr previm);
BGD_DECLARE(void) gdImageGifAnimEnd(FILE *outFile);
BGD_DECLARE(void) gdImageGifAnimBeginCtx(gdImagePtr im, gdIOCtx *out, int GlobalCM, int Loops);
BGD_DECLARE(void) gdImageGifAnimAddCtx(gdImagePtr im, gdIOCtx *out, int LocalCM, int LeftOfs, int TopOfs, int Delay, int Disposal, gdImagePtr previm);
BGD_DECLARE(void) gdImageGifAnimEndCtx(gdIOCtx *out);
BGD_DECLARE(void *) gdImageGifAnimBeginPtr(gdImagePtr im, int *size, int GlobalCM, int Loops);
BGD_DECLARE(void *) gdImageGifAnimAddPtr(gdImagePtr im, int *size, int LocalCM, int LeftOfs, int TopOfs, int Delay, int Disposal, gdImagePtr previm);
BGD_DECLARE(void *) gdImageGifAnimEndPtr(int *size);



/*
  Group: Types

  typedef: gdSink

  typedef: gdSinkPtr

    *Note:* This interface is *obsolete* and kept only for
    *compatibility.  Use <gdIOCtx> instead.

    Represents a "sink" (destination) to which a PNG can be
    written. Programmers who do not wish to write PNGs to a file can
    provide their own alternate output mechanism, using the
    <gdImagePngToSink> function. See the documentation of that
    function for an example of the proper use of this type.

    > typedef struct {
    >     int (*sink) (void *context, char *buffer, int len);
    >     void *context;
    > } gdSink, *gdSinkPtr;

    The _sink_ function must return -1 on error, otherwise the number of
    bytes written, which must be equal to len.

    _context_ will be passed to your sink function.

*/

typedef struct {
	int (*sink) (void *context, const char *buffer, int len);
	void *context;
}
gdSink, *gdSinkPtr;

BGD_DECLARE(void) gdImagePngToSink (gdImagePtr im, gdSinkPtr out);

BGD_DECLARE(void) gdImageGd (gdImagePtr im, FILE * out);
BGD_DECLARE(void) gdImageGd2 (gdImagePtr im, FILE * out, int cs, int fmt);

/* Best to free this memory with gdFree(), not free() */
BGD_DECLARE(void *) gdImageGifPtr (gdImagePtr im, int *size);

/* Best to free this memory with gdFree(), not free() */
BGD_DECLARE(void *) gdImagePngPtr (gdImagePtr im, int *size);
BGD_DECLARE(void *) gdImagePngPtrEx (gdImagePtr im, int *size, int level);

/* Best to free this memory with gdFree(), not free() */
BGD_DECLARE(void *) gdImageGdPtr (gdImagePtr im, int *size);

/* Best to free this memory with gdFree(), not free() */
BGD_DECLARE(void *) gdImageGd2Ptr (gdImagePtr im, int cs, int fmt, int *size);

BGD_DECLARE(gdImagePtr) gdImageClone (gdImagePtr src);

BGD_DECLARE(gdImagePtr) gdImageNeuQuant(gdImagePtr im, const int max_color, int sample_factor);

enum gdPixelateMode {
	GD_PIXELATE_UPPERLEFT,
	GD_PIXELATE_AVERAGE
};

BGD_DECLARE(int) gdImagePixelate(gdImagePtr im, int block_size, const unsigned int mode);

typedef struct {
	int sub;
	int plus;
	unsigned int num_colors;
	int *colors;
	unsigned int seed;
} gdScatter, *gdScatterPtr;

BGD_DECLARE(int) gdImageScatter(gdImagePtr im, int sub, int plus);
BGD_DECLARE(int) gdImageScatterColor(gdImagePtr im, int sub, int plus, int colors[], unsigned int num_colors);
BGD_DECLARE(int) gdImageScatterEx(gdImagePtr im, gdScatterPtr s);
BGD_DECLARE(int) gdImageSmooth(gdImagePtr im, float weight);
BGD_DECLARE(int) gdImageMeanRemoval(gdImagePtr im);
BGD_DECLARE(int) gdImageEmboss(gdImagePtr im);
BGD_DECLARE(int) gdImageGaussianBlur(gdImagePtr im);
BGD_DECLARE(int) gdImageEdgeDetectQuick(gdImagePtr src);
BGD_DECLARE(int) gdImageSelectiveBlur( gdImagePtr src);
BGD_DECLARE(int) gdImageConvolution(gdImagePtr src, float filter[3][3], float filter_div, float offset);
BGD_DECLARE(int) gdImageColor(gdImagePtr src, const int red, const int green, const int blue, const int alpha);
BGD_DECLARE(int) gdImageContrast(gdImagePtr src, double contrast);
BGD_DECLARE(int) gdImageBrightness(gdImagePtr src, int brightness);
BGD_DECLARE(int) gdImageGrayScale(gdImagePtr src);
BGD_DECLARE(int) gdImageNegate(gdImagePtr src);

BGD_DECLARE(gdImagePtr) gdImageCopyGaussianBlurred(gdImagePtr src, int radius,
                                                   double sigma);


/* Macros to access information about images. */

/* Returns nonzero if the image is a truecolor image,
   zero for a palette image. */
#define gdImageTrueColor(im) ((im)->trueColor)

#define gdImageGetTransparent(im) ((im)->transparent)
#define gdImageGetInterlaced(im) ((im)->interlace)


#define gdImageResolutionX(im) (im)->res_x
#define gdImageResolutionY(im) (im)->res_y

/* I/O Support routines. */

BGD_DECLARE(gdIOCtx *) gdNewFileCtx (FILE *);
/* If data is null, size is ignored and an initial data buffer is
   allocated automatically. NOTE: this function assumes gd has the right
   to free or reallocate "data" at will! Also note that gd will free
   "data" when the IO context is freed. If data is not null, it must point
   to memory allocated with gdMalloc, or by a call to gdImage[something]Ptr.
   If not, see gdNewDynamicCtxEx for an alternative. */
BGD_DECLARE(gdIOCtx *) gdNewDynamicCtx (int size, void *data);
/* 2.0.21: if freeFlag is nonzero, gd will free and/or reallocate "data" as
   needed as described above. If freeFlag is zero, gd will never free
   or reallocate "data", which means that the context should only be used
   for *reading* an image from a memory buffer, or writing an image to a
   memory buffer which is already large enough. If the memory buffer is
   not large enough and an image write is attempted, the write operation
   will fail. Those wishing to write an image to a buffer in memory have
   a much simpler alternative in the gdImage[something]Ptr functions. */
BGD_DECLARE(gdIOCtx *) gdNewDynamicCtxEx (int size, void *data, int freeFlag);
BGD_DECLARE(gdIOCtx *) gdNewSSCtx (gdSourcePtr in, gdSinkPtr out);
BGD_DECLARE(void *) gdDPExtractData (struct gdIOCtx *ctx, int *size);

#define GD2_CHUNKSIZE           128
#define GD2_CHUNKSIZE_MIN	64
#define GD2_CHUNKSIZE_MAX       4096

#define GD2_VERS                2
#define GD2_ID                  "gd2"

#define GD2_FMT_RAW             1
#define GD2_FMT_COMPRESSED      2

#define GD_FLIP_HORINZONTAL 1
#define GD_FLIP_VERTICAL 2
#define GD_FLIP_BOTH 3

BGD_DECLARE(gdImagePtr) gdImageCrop(gdImagePtr src, const gdRect *crop);
BGD_DECLARE(gdImagePtr) gdImageCropAuto(gdImagePtr im, const unsigned int mode);
BGD_DECLARE(gdImagePtr) gdImageCropThreshold(gdImagePtr im, const unsigned int color, const float threshold);

BGD_DECLARE(int) gdImageSetInterpolationMethod(gdImagePtr im, gdInterpolationMethod id);
BGD_DECLARE(gdInterpolationMethod) gdImageGetInterpolationMethod(gdImagePtr im);

BGD_DECLARE(gdImagePtr) gdImageScale(const gdImagePtr src, const unsigned int new_width, const unsigned int new_height);

BGD_DECLARE(gdImagePtr) gdImageRotateInterpolated(const gdImagePtr src, const float angle, int bgcolor);

BGD_DECLARE(int) gdAffineApplyToPointF (gdPointFPtr dst, const gdPointFPtr src, const double affine[6]);
BGD_DECLARE(int) gdAffineInvert (double dst[6], const double src[6]);
BGD_DECLARE(int) gdAffineFlip (double dst_affine[6], const double src_affine[6], const int flip_h, const int flip_v);
BGD_DECLARE(int) gdAffineConcat (double dst[6], const double m1[6], const double m2[6]);

BGD_DECLARE(int) gdAffineIdentity (double dst[6]);
BGD_DECLARE(int) gdAffineScale (double dst[6], const double scale_x, const double scale_y);
BGD_DECLARE(int) gdAffineRotate (double dst[6], const double angle);
BGD_DECLARE(int) gdAffineShearHorizontal (double dst[6], const double angle);
BGD_DECLARE(int) gdAffineShearVertical(double dst[6], const double angle);
BGD_DECLARE(int) gdAffineTranslate (double dst[6], const double offset_x, const double offset_y);
BGD_DECLARE(double) gdAffineExpansion (const double src[6]);
BGD_DECLARE(int) gdAffineRectilinear (const double src[6]);
BGD_DECLARE(int) gdAffineEqual (const double matrix1[6], const double matrix2[6]);
BGD_DECLARE(int) gdTransformAffineGetImage(gdImagePtr *dst, const gdImagePtr src, gdRectPtr src_area, const double affine[6]);
BGD_DECLARE(int) gdTransformAffineCopy(gdImagePtr dst, int dst_x, int dst_y, const gdImagePtr src, gdRectPtr src_region, const double affine[6]);
/*
gdTransformAffineCopy(gdImagePtr dst, int x0, int y0, int x1, int y1,
		      const gdImagePtr src, int src_width, int src_height,
		      const double affine[6]);
*/
BGD_DECLARE(int) gdTransformAffineBoundingBox(gdRectPtr src, const double affine[6], gdRectPtr bbox);




/* Version information functions */
BGD_DECLARE(int) gdMajorVersion(void);
BGD_DECLARE(int) gdMinorVersion(void);
BGD_DECLARE(int) gdReleaseVersion(void);
BGD_DECLARE(const char *) gdExtraVersion(void);
BGD_DECLARE(const char *) gdVersionString(void);


#ifdef __cplusplus
}
#endif

/* newfangled special effects */
#include "gdfx.h"

#endif				/* GD_H */

#ifdef __cplusplus
}
#endif
