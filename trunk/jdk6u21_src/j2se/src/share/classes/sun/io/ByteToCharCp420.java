/*
 * @(#)ByteToCharCp420.java	1.18	10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */


package sun.io;

import sun.nio.cs.ext.IBM420;

/**
 * A table to convert to Cp420 to Unicode
 *
 * @author  ConverterGenerator tool
 * @version >= JDK1.1.6
 */

public class ByteToCharCp420 extends ByteToCharSingleByte {

    private final static IBM420 nioCoder = new IBM420();

    public String getCharacterEncoding() {
        return "Cp420";
    }

    public ByteToCharCp420() {
        super.byteToCharTable = nioCoder.getDecoderSingleByteMappings();
    }
}
