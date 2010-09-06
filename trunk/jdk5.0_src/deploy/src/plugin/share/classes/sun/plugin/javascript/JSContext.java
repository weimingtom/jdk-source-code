/*
 * @(#)JSContext.java	1.7 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.plugin.javascript;

/** 
 * <p> JSContext is an interface that the AppletContext object must implement 
 * in order to support netscape.javascript.JSObject in Java Plug-in. The idea 
 * is that netscape.javascript.JSObject.getWindow is called to obtain a 
 * JSObject which represents the Window object in the document object model.
 * Since the AppletContext is provided by the Java Plug-in, it also contains 
 * information about the corresponding native instance handle info. 
 * The implementor of this interface is therefore required to return the right
 * implementation of the JSObject for the current browser
 * </p>
 */
public interface JSContext {

    /** 
     * <p> Return the JSObject implementation for this applet window
     * </p>
     *
     * @param JSObject implementation
     */
    netscape.javascript.JSObject getJSObject();
}
