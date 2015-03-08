package info.miranda.gd;

/*
  Group: Types

  typedef: gdFont

  typedef: gdFontPtr

  A font structure, containing the bitmaps of all characters in a
  font.  Used to declare the characteristics of a font. Text-output
  functions expect these as their second argument, following the
  <gdImagePtr> argument.  <gdFontSmall> and <gdFontGetLarge> both
  return one.

  You can provide your own font data by providing such a structure and
  the associated pixel array. You can determine the width and height
  of a single character in a font by examining the w and h members of
  the structure. If you will not be creating your own fonts, you will
  not need to concern yourself with the rest of the components of this
  structure.

  Please see the files gdfontl.c and gdfontl.h for an example of
  the proper declaration of this structure.

  > typedef struct {
  >   // # of characters in font
  >   int nchars;
  >   // First character is numbered... (usually 32 = space)
  >   int offset;
  >   // Character width and height
  >   int w;
  >   int h;
  >   // Font data; array of characters, one row after another.
  >   // Easily included in code, also easily loaded from
  >   // data files.
  >   char *data;
  > } gdFont;

  gdFontPtr is a pointer to gdFont.

*/
public class GdFont {
	/* # of characters in font */
	int nchars;
	/* First character is numbered... (usually 32 = space) */
	int offset;
	/* Character width and height */
	int w;
	int h;
	/* Font data; array of characters, one row after another.
	   Easily included in code, also easily loaded from
	   data files. */
	byte[] data;
}
