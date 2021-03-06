#ifdef USE_PRAGMA_IDENT_HDR
#pragma ident "@(#)ciConstant.hpp	1.11 03/12/23 16:39:26 JVM"
#endif
/*
 * Copyright 2004 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

// ciConstant
//
// This class represents a constant value.
class ciConstant VALUE_OBJ_CLASS_SPEC {
private:
  friend class ciEnv;
  friend class ciField;

  BasicType _type;
  union {
    jint      _int;
    jlong     _long;
    jint      _long_half[2];
    jfloat    _float;
    jdouble   _double;
    ciObject* _object;
  } _value;

  // Implementation of the print method.
  void print_impl();

public:

  ciConstant() {
    _type = T_ILLEGAL; _value._long = -1;
  }
  ciConstant(BasicType type, jint value) {
    assert(type != T_LONG && type != T_DOUBLE && type != T_FLOAT,
           "using the wrong ciConstant constructor");
    _type = type; _value._int = value;
  }
  ciConstant(jlong value) { 
    _type = T_LONG; _value._long = value;
  }
  ciConstant(jfloat value) { 
    _type = T_FLOAT; _value._float = value;
  }
  ciConstant(jdouble value) { 
    _type = T_DOUBLE; _value._double = value;
  }
  ciConstant(BasicType type, ciObject* p) { 
    _type = type; _value._object = p;
  }

  BasicType basic_type() const { return _type; }

  jboolean  as_boolean() {
    assert(basic_type() == T_BOOLEAN, "wrong type");
    return (jboolean)_value._int;
  }
  jchar     as_char() {
    assert(basic_type() == T_CHAR, "wrong type");
    return (jchar)_value._int;
  }
  jbyte     as_byte() {
    assert(basic_type() == T_BYTE, "wrong type");
    return (jbyte)_value._int;
  }
  jshort    as_short() {
    assert(basic_type() == T_SHORT, "wrong type");
    return (jshort)_value._int;
  }
  jint      as_int() {
    assert(basic_type() == T_BOOLEAN || basic_type() == T_CHAR  ||
           basic_type() == T_BYTE    || basic_type() == T_SHORT ||
           basic_type() == T_INT, "wrong type");
    return _value._int;
  }
  jlong     as_long() {
    assert(basic_type() == T_LONG, "wrong type");
    return _value._long;
  }
  jfloat    as_float() {
    assert(basic_type() == T_FLOAT, "wrong type");
    return _value._float;
  }
  jdouble   as_double() {
    assert(basic_type() == T_DOUBLE, "wrong type");
    return _value._double;
  }
  ciObject* as_object() const {
    assert(basic_type() == T_OBJECT || basic_type() == T_ARRAY, "wrong type");
    return _value._object;
  }

  // Debugging output
  void print();
};

