/*
 * @(#)AnnotationTypeDoc.java	1.5 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

package com.sun.javadoc;


/**
 * Represents an annotation type.
 * 
 * @author Scott Seligman
 * @version 1.5 10/03/23
 * @since 1.5
 */
public interface AnnotationTypeDoc extends ClassDoc {

    /**
     * Returns the elements of this annotation type.
     * Returns an empty array if there are none.
     *
     * @return the elements of this annotation type.
     */
    AnnotationTypeElementDoc[] elements();
}
