/*
 * @(#)X11DropTargetContextPeer.java	1.5 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.awt.motif;

import java.awt.Component;
import java.awt.peer.ComponentPeer;

import sun.awt.AppContext;
import sun.awt.SunToolkit;

import sun.awt.dnd.SunDropTargetContextPeer;
import sun.awt.dnd.SunDropTargetEvent;

/**
 * The X11DropTargetContextPeer class is the class responsible for handling
 * the interaction between the XDnD/Motif DnD subsystem and Java drop targets.
 *
 * @since 1.5
 */
final class X11DropTargetContextPeer extends SunDropTargetContextPeer {

    /*
     * A key to store a peer instance for an AppContext.
     */
    private static final Object DTCP_KEY = "DropTargetContextPeer"; 

    private X11DropTargetContextPeer() {}

    public static X11DropTargetContextPeer getPeer(AppContext appContext) {
        synchronized (_globalLock) {
            X11DropTargetContextPeer peer = 
                (X11DropTargetContextPeer)appContext.get(DTCP_KEY);
            if (peer == null) {
                peer = new X11DropTargetContextPeer();
                appContext.put(DTCP_KEY, peer);
            }
            
            return peer;
        }
    }

    /*
     * Note: 
     * the method can be called on the toolkit thread while holding AWT_LOCK.
     */
    private static void postDropTargetEventToPeer(final Component component, 
                                                  final int x, final int y, 
                                                  final int dropAction,
                                                  final int actions, 
                                                  final long[] formats,
                                                  final long nativeCtxt,
                                                  final int eventID) {

        AppContext appContext = SunToolkit.targetToAppContext(component);
        X11DropTargetContextPeer peer = getPeer(appContext);

        peer.postDropTargetEvent(component, x, y, dropAction, actions, formats,
                                 nativeCtxt, eventID,
                                 !SunDropTargetContextPeer.DISPATCH_SYNC);
    }

    protected void eventProcessed(SunDropTargetEvent e, int returnValue, 
                                  boolean dispatcherDone) {
        /* If the event was not consumed, send a response to the source. */
        long ctxt = getNativeDragContext();
        if (ctxt != 0) {
            sendResponse(e.getID(), returnValue, ctxt, dispatcherDone,
                         e.isConsumed());
        }
    }

    protected void doDropDone(boolean success, int dropAction, 
                              boolean isLocal) {
	dropDone(getNativeDragContext(), success, dropAction);
    }

    protected Object getNativeData(long format) {
        return getData(getNativeDragContext(), format);
    }

    protected void processEnterMessage(SunDropTargetEvent event) {
        if (!processSunDropTargetEvent(event)) {
            super.processEnterMessage(event);
        }
    }

    protected void processExitMessage(SunDropTargetEvent event) {
        if (!processSunDropTargetEvent(event)) {
            super.processExitMessage(event);
        }
    }

    protected void processMotionMessage(SunDropTargetEvent event, 
                                        boolean operationChanged) {
        if (!processSunDropTargetEvent(event)) {
            super.processMotionMessage(event, operationChanged);
        }
    }

    protected void processDropMessage(SunDropTargetEvent event) {
        if (!processSunDropTargetEvent(event)) {
            super.processDropMessage(event);
        }
    }

    // If source is an XEmbedCanvasPeer, passes the event to it for processing and
    // return true if the event is forwarded to the XEmbed child. 
    // Otherwise, does nothing and return false.
    private boolean processSunDropTargetEvent(SunDropTargetEvent event) {
        Object source = event.getSource();

        if (source instanceof Component) {
            ComponentPeer peer = ((Component)source).getPeer();
            if (peer instanceof MEmbedCanvasPeer) {
                MEmbedCanvasPeer mEmbedCanvasPeer = (MEmbedCanvasPeer)peer;
                /* The native context is the pointer to the XClientMessageEvent
                   structure. */
                long ctxt = getNativeDragContext();

                /* If the event is not consumed, pass it to the
                   MEmbedCanvasPeer for processing. */
                if (!event.isConsumed()) {
                    // NOTE: ctxt can be zero at this point.
                    if (mEmbedCanvasPeer.processXEmbedDnDEvent(ctxt,
                                                               event.getID())) {
                        event.consume();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private native void sendResponse(int eventID, int returnValue, 
                                     long nativeCtxt, boolean dispatcherDone,
                                     boolean consumed);

    private native void dropDone(long nativeCtxt, boolean success, 
                                 int dropAction);

    private native Object getData(long nativeCtxt, long format);
}

