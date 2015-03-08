package info.miranda.gd;

/**
 * Group: Crop
 *
 * Constants: gdCropMode
 *  GD_CROP_DEFAULT - Default crop mode (4 corners or background)
 *  GD_CROP_TRANSPARENT - Crop using the transparent color
 *  GD_CROP_BLACK - Crop black borders
 *  GD_CROP_WHITE - Crop white borders
 *  GD_CROP_SIDES - Crop using colors of the 4 corners
 *
 * See also:
 *  <gdImageAutoCrop>
 **/
public enum GdCropMode {
	GD_CROP_DEFAULT,
	GD_CROP_TRANSPARENT,
	GD_CROP_BLACK,
	GD_CROP_WHITE,
	GD_CROP_SIDES,
	GD_CROP_THRESHOLD
}
