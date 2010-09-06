/* 
 *  @OSF_COPYRIGHT@
 *  COPYRIGHT NOTICE
 *  Copyright (c) 1990, 1991, 1992, 1993 Open Software Foundation, Inc.
 *  ALL RIGHTS RESERVED (MOTIF). See the file named COPYRIGHT.MOTIF for
 *  the full copyright text.
*/ 
/* 
 * HISTORY
*/ 
/*   $XConsortium: ScrolledW.h /main/11 1995/07/13 17:56:18 drk $ */
/*
*  (c) Copyright 1987, 1988, 1989, 1990, 1991, 1992 HEWLETT-PACKARD COMPANY */
#ifndef _XmScrolledWindow_h
#define _XmScrolledWindow_h

#include <Xm/Xm.h>

#ifdef __cplusplus
extern "C" {
#endif


#ifndef XmIsScrolledWindow
#define XmIsScrolledWindow(w)	XtIsSubclass(w, xmScrolledWindowWidgetClass)
#endif /* XmIsScrolledWindow */


externalref WidgetClass xmScrolledWindowWidgetClass;

typedef struct _XmScrolledWindowClassRec * XmScrolledWindowWidgetClass;
typedef struct _XmScrolledWindowRec      * XmScrolledWindowWidget;


/********    Public Function Declarations    ********/

extern void XmScrolledWindowSetAreas( 
                        Widget w,
                        Widget hscroll,
                        Widget vscroll,
                        Widget wregion) ;
extern Widget XmCreateScrolledWindow( 
                        Widget parent,
                        char *name,
                        ArgList args,
                        Cardinal argCount) ;
extern void XmScrollVisible(
			Widget      	scrw,
			Widget          wid,
			Dimension       hor_margin, 
			Dimension       ver_margin) ;

/********    End Public Function Declarations    ********/


#ifdef __cplusplus
}  /* Close scope of 'extern "C"' declaration which encloses file. */
#endif

#endif /* _XmScrolledWindow_h */
/* DON'T ADD ANYTHING AFTER THIS #endif */
