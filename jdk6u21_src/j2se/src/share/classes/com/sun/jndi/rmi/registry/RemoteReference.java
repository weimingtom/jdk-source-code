/*
 * @(#)RemoteReference.java	1.8 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.sun.jndi.rmi.registry;


import java.rmi.*;

import javax.naming.*;


/**
 * The RemoteReference interface wraps a Reference in a Remote wrapper.
 *
 * @author Scott Seligman
 * @version 1.8 10/03/23
 */


public interface RemoteReference extends Remote {

        Reference getReference() throws NamingException, RemoteException;
}
