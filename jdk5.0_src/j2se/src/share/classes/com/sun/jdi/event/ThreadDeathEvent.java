/*
 * @(#)ThreadDeathEvent.java	1.13 03/12/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.jdi.event;

import com.sun.jdi.*;

/**
 * Notification of a completed thread in the target VM. The 
 * notification is generated by the dying thread before it terminates.
 * Because of this timing, it is possible 
 * for {@link VirtualMachine#allThreads} to return this thread 
 * after this event is received. 
 * <p>
 * Note that this event gives no information
 * about the lifetime of the thread object. It may or may not be collected
 * soon depending on what references exist in the target VM.
 *
 * @see EventQueue
 * @see VirtualMachine
 * @see ThreadReference
 *
 * @author Robert Field
 * @since  1.3
 */
public interface ThreadDeathEvent extends Event {
    /**
     * Returns the thread which is terminating.
     *
     * @return a {@link ThreadReference} which mirrors the event's thread in 
     * the target VM.
     */
    public ThreadReference thread();
}
    
