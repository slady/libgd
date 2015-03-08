package info.miranda.gd;

/*
  Group: Types

  typedef: gdPoint

  typedef: gdPointPtr

  Represents a point in the coordinate space of the image; used by
  <gdImagePolygon>, <gdImageOpenPolygon> and <gdImageFilledPolygon>
  for polygon drawing.

  > typedef struct {
  >     int x, y;
  > } gdPoint, *gdPointPtr;

*/
public class GdPoint {
	int x, y;
}
