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


#endif /* GDHELPERS_H */

#ifdef __cplusplus
}
#endif
