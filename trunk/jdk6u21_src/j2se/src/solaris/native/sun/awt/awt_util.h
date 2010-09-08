/*
 * @(#)awt_util.h	1.67 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef _AWT_UTIL_H_
#define _AWT_UTIL_H_

#ifndef HEADLESS
#ifndef XAWT
#include <Xm/VendorSEP.h>
#include <Xm/VendorSP.h>
#endif
#include "gdefs.h"

typedef struct ConvertEventTimeAndModifiers {
    jlong when;
    jint modifiers;
} ConvertEventTimeAndModifiers;

Boolean awt_util_focusIsOnMenu(Display *display);
int32_t awt_util_sendButtonClick(Display *display, Window window);

Widget awt_util_createWarningWindow(Widget parent, char *warning);
void awt_util_show(Widget w);
void awt_util_hide(Widget w);
void awt_util_enable(Widget w);
void awt_util_disable(Widget w);
void awt_util_reshape(Widget w, jint x, jint y, jint wd, jint ht);
void awt_util_mapChildren(Widget w, void (*func)(Widget,void *),
			  int32_t applyToSelf, void *data);
int32_t awt_util_setCursor(Widget w, Cursor c);
void awt_util_convertEventTimeAndModifiers
    (XEvent *event, ConvertEventTimeAndModifiers *output);
Widget awt_WidgetAtXY(Widget root, Position x, Position y);
char *awt_util_makeWMMenuItem(char *target, Atom protocol);
Cardinal awt_util_insertCallback(Widget w);
void awt_util_consumeAllXEvents(Widget widget);
void awt_util_cleanupBeforeDestroyWidget(Widget widget);
void awt_util_debug_init();
Time awt_util_getCurrentServerTime();
jlong awt_util_nowMillisUTC();
jlong awt_util_nowMillisUTC_offset(Time server_offset);
void awt_util_do_wheel_scroll(Widget scrolled_window, jint scrollType,
                                jint scrollAmt, jint wheelAmt);
Widget awt_util_get_scrollbar_to_scroll(Widget window);


typedef struct _EmbeddedFrame {
    Widget embeddedFrame;
    Window frameContainer;
    jobject javaRef;
    Boolean eventSelectedPreviously;
    struct _EmbeddedFrame * next;
    struct _EmbeddedFrame * prev;
} EmbeddedFrame;

void awt_util_addEmbeddedFrame(Widget embeddedFrame, jobject javaRef);
void awt_util_delEmbeddedFrame(Widget embeddedFrame);
Boolean awt_util_processEventForEmbeddedFrame(XEvent *ev);

#define WITH_XERROR_HANDLER(f) do {		\
    XSync(awt_display, False);			\
    xerror_code = Success;			\
    xerror_saved_handler = XSetErrorHandler(f);	\
} while (0)

/* Convenience macro for handlers to use */
#define XERROR_SAVE(err) do {			\
    xerror_code = (err)->error_code;		\
} while (0)

#define RESTORE_XERROR_HANDLER do {		\
    XSync(awt_display, False);			\
    XSetErrorHandler(xerror_saved_handler);	\
} while (0)

#define EXEC_WITH_XERROR_HANDLER(f, code) do {	\
    WITH_XERROR_HANDLER(f);			\
    do {					\
	code;					\
    } while (0);				\
    RESTORE_XERROR_HANDLER;			\
} while (0)

/*
 * Since X reports protocol errors asynchronously, we often need to
 * install an error handler that acts like a callback.  While that
 * specialized handler is installed we save original handler here.
 */
extern XErrorHandler xerror_saved_handler;

/*
 * A place for error handler to report the error code.
 */
extern unsigned char xerror_code;

extern int xerror_ignore_bad_window(Display *dpy, XErrorEvent *err);

#endif /* !HEADLESS */

#ifndef INTERSECTS
#define INTERSECTS(r1_x1,r1_x2,r1_y1,r1_y2,r2_x1,r2_x2,r2_y1,r2_y2) \
!((r2_x2 <= r1_x1) ||\
  (r2_y2 <= r1_y1) ||\
  (r2_x1 >= r1_x2) ||\
  (r2_y1 >= r1_y2))
#endif

#ifndef MIN
#define MIN(a,b) ((a) < (b) ? (a) : (b))
#endif
#ifndef MAX
#define MAX(a,b) ((a) > (b) ? (a) : (b))
#endif

struct DPos {
    int32_t x;
    int32_t y;
    int32_t mapped;
    void *data;
    void *peer;
    int32_t echoC;
};

extern jobject awtJNI_GetCurrentThread(JNIEnv *env);
extern void awtJNI_ThreadYield(JNIEnv *env);

#ifndef HEADLESS
extern Widget prevWidget;
#endif /* !HEADLESS */

/*
 * Functions for accessing fields by name and signature
 */

JNIEXPORT jobject JNICALL
JNU_GetObjectField(JNIEnv *env, jobject self, const char *name,
		   const char *sig);

JNIEXPORT jboolean JNICALL
JNU_SetObjectField(JNIEnv *env, jobject self, const char *name,
		   const char *sig, jobject val);

JNIEXPORT jlong JNICALL
JNU_GetLongField(JNIEnv *env, jobject self, const char *name);

JNIEXPORT jint JNICALL
JNU_GetIntField(JNIEnv *env, jobject self, const char *name);

JNIEXPORT jboolean JNICALL
JNU_SetIntField(JNIEnv *env, jobject self, const char *name, jint val);

JNIEXPORT jboolean JNICALL
JNU_SetLongField(JNIEnv *env, jobject self, const char *name, jlong val);

JNIEXPORT jboolean JNICALL
JNU_GetBooleanField(JNIEnv *env, jobject self, const char *name);

JNIEXPORT jboolean JNICALL
JNU_SetBooleanField(JNIEnv *env, jobject self, const char *name, jboolean val);

JNIEXPORT jint JNICALL
JNU_GetCharField(JNIEnv *env, jobject self, const char *name);

#ifndef HEADLESS
#ifdef __solaris__
extern Widget awt_util_getXICStatusAreaWindow(Widget w);
#else
int32_t awt_util_getIMStatusHeight(Widget vw);
XVaNestedList awt_util_getXICStatusAreaList(Widget w);
Widget awt_util_getXICStatusAreaWindow(Widget w);
#endif




#ifdef __linux__
typedef struct _XmImRefRec {
  Cardinal      num_refs;       /* Number of referencing widgets. */
  Cardinal      max_refs;       /* Maximum length of refs array. */
  Widget*       refs;           /* Array of referencing widgets. */
  XtPointer     **callbacks;
} XmImRefRec, *XmImRefInfo;

typedef struct _PreeditBufferRec {
  unsigned short length;
  wchar_t        *text;
  XIMFeedback    *feedback;
  int32_t            caret;
  XIMCaretStyle  style;
} PreeditBufferRec, *PreeditBuffer;

typedef struct _XmImXICRec {
  struct _XmImXICRec *next;     /* Links all have the same XIM. */
  XIC           xic;            /* The XIC. */
  Window        focus_window;   /* Cached information about the XIC. */
  XIMStyle      input_style;    /* ...ditto... */
  int32_t           status_width;   /* ...ditto... */
  int32_t           preedit_width;  /* ...ditto... */
  int32_t           sp_height;      /* ...ditto... */
  Boolean       has_focus;      /* Does this XIC have keyboard focus. */
  Boolean       anonymous;      /* Do we have exclusive rights to this XIC. */
  XmImRefRec    widget_refs;    /* Widgets referencing this XIC. */
  struct _XmImXICRec **source; /* Original source of shared XICs. */
  PreeditBuffer preedit_buffer;
} XmImXICRec, *XmImXICInfo;

typedef struct _XmImShellRec {
  /* per-Shell fields. */
  Widget        im_widget;      /* Dummy widget to make intrinsics behave. */
  Widget        current_widget; /* Widget whose visual we're matching. */

  /* per <Shell,XIM> fields. */
  XmImXICInfo   shell_xic;      /* For PER_SHELL sharing policy. */
  XmImXICInfo   iclist;         /* All known XICs for this <XIM,Shell>. */
} XmImShellRec, *XmImShellInfo;

typedef struct {
  /* per-Display fields. */
  XContext      current_xics;   /* Map widget -> current XmImXICInfo. */

  /* per-XIM fields. */
  XIM           xim;            /* The XIM. */
  XIMStyles     *styles;        /* XNQueryInputStyle result. */
  XmImRefRec    shell_refs;     /* Shells referencing this XIM. */
} XmImDisplayRec, *XmImDisplayInfo;

#endif
#endif /* !HEADLESS */
#endif           /* _AWT_UTIL_H_ */

