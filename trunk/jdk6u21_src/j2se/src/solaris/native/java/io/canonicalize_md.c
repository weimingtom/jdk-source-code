/*
 * @(#)canonicalize_md.c	1.39 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * Pathname canonicalization for Unix file systems
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <errno.h>
#include <limits.h>
#include <alloca.h>


/* Note: The comments in this file use the terminology
         defined in the java.io.File class */


/* Check the given name sequence to see if it can be further collapsed.
   Return zero if not, otherwise return the number of names in the sequence. */

static int
collapsible(char *names)
{
    char *p = names;
    int dots = 0, n = 0;

    while (*p) {
	if ((p[0] == '.') && ((p[1] == '\0')
			      || (p[1] == '/')
			      || ((p[1] == '.') && ((p[2] == '\0')
						    || (p[2] == '/'))))) {
	    dots = 1;
	}
	n++;
	while (*p) {
	    if (*p == '/') {
		p++;
		break;
	    }
	    p++;
	}
    }
    return (dots ? n : 0);
}


/* Split the names in the given name sequence,
   replacing slashes with nulls and filling in the given index array */

static void
splitNames(char *names, char **ix)
{
    char *p = names;
    int i = 0;

    while (*p) {
	ix[i++] = p++;
	while (*p) {
	    if (*p == '/') {
		*p++ = '\0';
		break;
	    }
	    p++;
	}
    }
}


/* Join the names in the given name sequence, ignoring names whose index
   entries have been cleared and replacing nulls with slashes as needed */

static void
joinNames(char *names, int nc, char **ix)
{
    int i;
    char *p;

    for (i = 0, p = names; i < nc; i++) {
	if (!ix[i]) continue;
	if (i > 0) {
	    p[-1] = '/';
	}
	if (p == ix[i]) {
	    p += strlen(p) + 1;
	} else {
	    char *q = ix[i];
	    while ((*p++ = *q++));
	}
    }
    *p = '\0';
}


/* Collapse "." and ".." names in the given path wherever possible.
   A "." name may always be eliminated; a ".." name may be eliminated if it
   follows a name that is neither "." nor "..".  This is a syntactic operation
   that performs no filesystem queries, so it should only be used to cleanup
   after invoking the realpath() procedure. */

static void
collapse(char *path)
{
    char *names = (path[0] == '/') ? path + 1 : path; /* Preserve first '/' */
    int nc;
    char **ix;
    int i, j;
    char *p, *q;

    nc = collapsible(names);
    if (nc < 2) return;		/* Nothing to do */
    ix = (char **)alloca(nc * sizeof(char *));
    splitNames(names, ix);

    for (i = 0; i < nc; i++) {
	int dots = 0;

	/* Find next occurrence of "." or ".." */
	do {
	    char *p = ix[i];
	    if (p[0] == '.') {
		if (p[1] == '\0') {
		    dots = 1;
		    break;
		}
		if ((p[1] == '.') && (p[2] == '\0')) {
		    dots = 2;
		    break;
		}
	    }
	    i++;
	} while (i < nc);
	if (i >= nc) break;

	/* At this point i is the index of either a "." or a "..", so take the
	   appropriate action and then continue the outer loop */
	if (dots == 1) {
	    /* Remove this instance of "." */
	    ix[i] = 0;
	}
	else {
	    /* If there is a preceding name, remove both that name and this
	       instance of ".."; otherwise, leave the ".." as is */
	    for (j = i - 1; j >= 0; j--) {
		if (ix[j]) break;
	    }
	    if (j < 0) continue;
	    ix[j] = 0;
	    ix[i] = 0;
	}
	/* i will be incremented at the top of the loop */
    }

    joinNames(names, nc, ix);
}


/* Convert a pathname to canonical form.  The input path is assumed to contain
   no duplicate slashes.  On Solaris we can use realpath() to do most of the
   work, though once that's done we still must collapse any remaining "." and
   ".." names by hand. */

int
canonicalize(char *original, char *resolved, int len)
{
    if (len < PATH_MAX) {
	errno = EINVAL;
	return -1;
    }

    if (strlen(original) > PATH_MAX) {
	errno = ENAMETOOLONG;
	return -1;
    }

    /* First try realpath() on the entire path */
    if (realpath(original, resolved)) {
	/* That worked, so return it */
	collapse(resolved);
	return 0;
    }
    else {
	/* Something's bogus in the original path, so remove names from the end
	   until either some subpath works or we run out of names */
	char *p, *end, *r = NULL;
	char path[PATH_MAX + 1];

	strncpy(path, original, sizeof(path));
	if (path[PATH_MAX] != '\0') {
	    errno = ENAMETOOLONG;
	    return -1;
	}
	end = path + strlen(path);

	for (p = end; p > path;) {

	    /* Skip last element */
	    while ((--p > path) && (*p != '/'));
	    if (p == path) break;

	    /* Try realpath() on this subpath */
	    *p = '\0';
	    r = realpath(path, resolved);
	    *p = (p == end) ? '\0' : '/';

	    if (r != NULL) {
		/* The subpath has a canonical path */
		break;
	    }
	    else if (errno == ENOENT || errno == ENOTDIR || errno == EACCES) {
		/* If the lookup of a particular subpath fails because the file
		   does not exist, because it is of the wrong type, or because
		   access is denied, then remove its last name and try again.
		   Other I/O problems cause an error return. */
		continue;
	    }
	    else {
		return -1;
	    }
	}

	if (r != NULL) {
	    /* Append unresolved subpath to resolved subpath */
	    int rn = strlen(r);
	    if (rn + strlen(p) >= len) {
		/* Buffer overflow */
		errno = ENAMETOOLONG;
		return -1;
	    }
	    if ((rn > 0) && (r[rn - 1] == '/') && (*p == '/')) {
		/* Avoid duplicate slashes */
		p++;
	    }
	    strcpy(r + rn, p);
	    collapse(r);
	    return 0;
	}
	else {
	    /* Nothing resolved, so just return the original path */
	    strcpy(resolved, path);
	    collapse(resolved);
	    return 0;
	}
    }

}
