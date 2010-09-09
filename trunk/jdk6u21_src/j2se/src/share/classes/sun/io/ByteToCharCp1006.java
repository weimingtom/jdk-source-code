/*
 * @(#)ByteToCharCp1006.java	1.17	10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */


package sun.io;

import sun.nio.cs.ext.IBM1006;

/**
 * A table to convert to Cp1006 to Unicode
 *
 * @author  ConverterGenerator tool
 * @version >= JDK1.1.6
 */

public class ByteToCharCp1006 extends ByteToCharSingleByte {

    private final static IBM1006 nioCoder = new IBM1006();

    public String getCharacterEncoding() {
        return "Cp1006";
    }

    public ByteToCharCp1006() {
        super.byteToCharTable = nioCoder.getDecoderSingleByteMappings();
    }
}
