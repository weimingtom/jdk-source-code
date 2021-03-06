/*
 * @(#)BackBufferCapsProvider.java	1.2 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.java2d;

import java.awt.BufferCapabilities;

/**
 * Provides access to back-buffer's BufferCapabilities.
 */
public interface BackBufferCapsProvider {
    public BufferCapabilities getBackBufferCaps();
}
