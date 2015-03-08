package info.miranda.gd;

public enum GdPaletteQuantizationMethod {
	GD_QUANT_DEFAULT,
	GD_QUANT_JQUANT,  /* libjpeg's old median cut. Fast, but only uses 16-bit color. */
	GD_QUANT_NEUQUANT, /* neuquant - approximation using kohonen neural network. */
	GD_QUANT_LIQ /* combination of algorithms used in libimagequant/pngquant2 aiming for highest quality at cost of speed */
}
