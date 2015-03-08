#ifdef __cplusplus
extern "C" {
#endif

#ifndef GDHELPERS_H
#define GDHELPERS_H 1

	/* sys/types.h is needed for size_t on Sparc-SunOS-4.1 */
#ifndef _WIN32_WCE
#include <sys/types.h>
#else
#include <stdlib.h>
#endif /* _WIN32_WCE */

	/* TBB: strtok_r is not universal; provide an implementation of it. */

	char * gd_strtok_r (char *s, char *sep, char **state);

	/* The extended version of gdReallocEx will free *ptr if the
	 * realloc fails */
	void *gdReallocEx (void *ptr, size_t size);

	/* Returns nonzero if multiplying the two quantities will
		result in integer overflow. Also returns nonzero if
		either quantity is negative. By Phil Knirsch based on
		netpbm fixes by Alan Cox. */

	int overflow2(int a, int b);

#define DPCM2DPI(dpcm) (unsigned int)((dpcm)*2.54 + 0.5)
#define DPM2DPI(dpm)   (unsigned int)((dpm)*0.0254 + 0.5)
#define DPI2DPCM(dpi)  (unsigned int)((dpi)/2.54 + 0.5)
#define DPI2DPM(dpi)   (unsigned int)((dpi)/0.0254 + 0.5)

#endif /* GDHELPERS_H */

#ifdef __cplusplus
}
#endif
