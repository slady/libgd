package info.miranda.gd;

import junit.framework.TestCase;
import org.junit.Test;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertEquals;

public class GdColorMapTest extends TestCase {

	@Test
	public void testLookupAliceBlue() throws Exception {
		final GdColor color = GdColorMap.lookup("AliceBlue");
		assertNotNull(color);
		assertEquals(240, color.getRed());
		assertEquals(248, color.getGreen());
		assertEquals(255, color.getBlue());
	}

	@Test
	public void testLookupNotFound() throws Exception {
		assertNull(GdColorMap.lookup("no such name"));
	}

}
