package info.miranda.gd.example;

import info.miranda.gd.GdDisposal;
import info.miranda.gd.GdImage;
import info.miranda.gd.GdImageColorType;

import java.io.File;

public class GdGifAnimTest {

	public static void main(String[] a) {
		final GdImage im = new GdImage(100, 100, GdImageColorType.PALETTE_BASED_COLOR);
		final int blue = im.colorAllocate(0, 0, 255);
		final File out = new File("gifanim.gif");
//		im.gifAnimBegin(out, 1, 10);
		GdImage last = null;
		for (int i = 0; (i <= 100); i += 2) {
			final GdImage cim = new GdImage(100, 100, GdImageColorType.PALETTE_BASED_COLOR);
			GdImage.paletteCopy(cim, im);
			cim.drawArc(50, 50, i, i, 0, 360, blue);
//			cim.gifAnimAdd(out, 0, 0, 0, 10, GdDisposal.NONE, last);
			last = cim;
		}
//		im.gifAnimEnd(out);
//		im.gifAnimClose(out);
	}

}
