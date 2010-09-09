/*
 * @(#)ByteToCharISO8859_15.java	1.9 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

/*
 * (C) Copyright IBM Corp. 1998 - All Rights Reserved
 *
 */

package sun.io;

import sun.nio.cs.ISO_8859_15;

/**
 * A table to convert ISO8859_15 to Unicode
 *
 * @author  ConverterGenerator tool
 * @version >= JDK1.1.7
 */

public class ByteToCharISO8859_15 extends ByteToCharSingleByte {

    private final static ISO_8859_15 nioCoder = new ISO_8859_15();

    public String getCharacterEncoding() {
        return "ISO8859_15";
    }

    public ByteToCharISO8859_15() {
        super.byteToCharTable = nioCoder.getDecoderSingleByteMappings();
    }
}
