package com.sun.corba.se.ActivationIDL;


/**
* com/sun/corba/se/ActivationIDL/EndPointInfo.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from ../../../../../../src/share/classes/com/sun/corba/se/ActivationIDL/activation.idl
* Friday, June 20, 2003 1:46:26 AM PDT
*/


// passing a struct containing endpointType and port-#s
public final class EndPointInfo implements org.omg.CORBA.portable.IDLEntity
{
  public String endpointType = null;
  public int port = (int)0;

  public EndPointInfo ()
  {
  } // ctor

  public EndPointInfo (String _endpointType, int _port)
  {
    endpointType = _endpointType;
    port = _port;
  } // ctor

} // class EndPointInfo