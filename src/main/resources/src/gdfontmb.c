


#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include "gdfontmb.h"

char gdFontMediumBoldData[]
gdFont gdFontMediumBoldRep = {
	gdFontMediumBoldData
};

BGD_EXPORT_DATA_PROT gdFontPtr gdFontMediumBold = &gdFontMediumBoldRep;

BGD_DECLARE(gdFontPtr)
gdFontGet (void)
{
	return gdFontMediumBold;
}

/* This file has not been truncated. */
