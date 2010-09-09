/*
 * @(#)ClipboardOwner.java	1.11 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.awt.datatransfer;

/**
 * Defines the interface for classes that will provide data to
 * a clipboard. An instance of this interface becomes the owner
 * of the contents of a clipboard (clipboard owner) if it is
 * passed as an argument to
 * {@link java.awt.datatransfer.Clipboard#setContents} method of
 * the clipboard and this method returns successfully. 
 * The instance remains the clipboard owner until another application
 * or another object within this application asserts ownership
 * of this clipboard.
 *
 * @see java.awt.datatransfer.Clipboard
 * 
 * @version 	1.11, 12/19/03
 * @author	Amy Fowler
 */

public interface ClipboardOwner {

    /**
     * Notifies this object that it is no longer the clipboard owner.
     * This method will be called when another application or another
     * object within this application asserts ownership of the clipboard.
     *
     * @param clipboard the clipboard that is no longer owned
     * @param contents the contents which this owner had placed on the clipboard
     */
    public void lostOwnership(Clipboard clipboard, Transferable contents);

}