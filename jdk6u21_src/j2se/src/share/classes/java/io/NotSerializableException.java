/*
 * @(#)NotSerializableException.java	1.18 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package java.io;

/**
 * Thrown when an instance is required to have a Serializable interface.
 * The serialization runtime or the class of the instance can throw
 * this exception. The argument should be the name of the class.
 *
 * @author  unascribed
 * @version 1.18, 03/23/10
 * @since   JDK1.1
 */
public class NotSerializableException extends ObjectStreamException {

    private static final long serialVersionUID = 2906642554793891381L;

    /** 
     * Constructs a NotSerializableException object with message string. 
     *
     * @param classname Class of the instance being serialized/deserialized.
     */
    public NotSerializableException(String classname) {
	super(classname);
    }

    /**
     *  Constructs a NotSerializableException object.
     */
    public NotSerializableException() {
	super();
    }
}