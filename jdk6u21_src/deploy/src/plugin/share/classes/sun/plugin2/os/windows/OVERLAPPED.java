/* !---- DO NOT EDIT: This file autogenerated by com\sun\gluegen\JavaEmitter.java on Sun Feb 03 07:50:40 PST 2008 ----! */


package sun.plugin2.os.windows;

import java.nio.*;

import sun.plugin2.gluegen.runtime.*;

import java.nio.*;

public abstract class OVERLAPPED {
  StructAccessor accessor;

  public static int size() {
    if (CPU.is32Bit()) {
      return OVERLAPPED32.size();
    } else {
      return OVERLAPPED64.size();
    }
  }

  public static OVERLAPPED create() {
    return create(BufferFactory.newDirectByteBuffer(size()));
  }

  public static OVERLAPPED create(ByteBuffer buf) {
    if (CPU.is32Bit()) {
      return new OVERLAPPED32(buf);
    } else {
      return new OVERLAPPED64(buf);
    }
  }

  OVERLAPPED(ByteBuffer buf) {
    accessor = new StructAccessor(buf);
  }

  public ByteBuffer getBuffer() {
    return accessor.getBuffer();
  }
}