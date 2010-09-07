/*
 * @(#)file      JDMAclBlock.java
 * @(#)author    Sun Microsystems, Inc.
 * @(#)version   4.9
 * @(#)date      04/10/19
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 */


/* Generated By:JJTree: Do not edit this line. JDMAclBlock.java */

package com.sun.jmx.snmp.IPAcl;

import java.util.Hashtable;

/** 
 * @version     4.9     12/19/03 
 * @author      Sun Microsystems, Inc. 
 */ 
class JDMAclBlock extends SimpleNode {
  JDMAclBlock(int id) {
    super(id);
  }

  JDMAclBlock(Parser p, int id) {
    super(p, id);
  }

  public static Node jjtCreate(int id) {
      return new JDMAclBlock(id);
  }

  public static Node jjtCreate(Parser p, int id) {
      return new JDMAclBlock(p, id);
  }
  
  /**
   * Do no need to go through this part of the tree for
   * building TrapEntry.
   */
   public void buildTrapEntries(Hashtable dest) {}

  /**
   * Do no need to go through this part of the tree for
   * building InformEntry.
   */
   public void buildInformEntries(Hashtable dest) {}
}