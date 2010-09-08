/*
 * @(#)XDropTargetProtocolListener.java	1.4 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt.X11;

interface XDropTargetProtocolListener {
    void handleDropTargetNotification(XWindow xwindow, int x, int y, 
                                      int dropAction, int actions, 
                                      long[] formats, long nativeCtxt, 
                                      int eventID);
}
