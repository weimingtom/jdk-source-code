/*
 * Copyright (c) 2000, 2007, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

class VMRegImpl;
typedef VMRegImpl* VMReg;

// Use Register as shortcut
class RegisterImpl;
typedef RegisterImpl* Register;


// The implementation of integer registers for the ia32 architecture
inline Register as_Register(int encoding) {
  return (Register)(intptr_t) encoding;
}

class RegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
#ifndef AMD64
    number_of_registers      = 8,
    number_of_byte_registers = 4
#else
    number_of_registers      = 16,
    number_of_byte_registers = 16
#endif // AMD64
  };

  // derived registers, offsets, and addresses
  Register successor() const                          { return as_Register(encoding() + 1); }

  // construction
  inline friend Register as_Register(int encoding);

  VMReg as_VMReg();

  // accessors
  int   encoding() const                         { assert(is_valid(), "invalid register"); return (intptr_t)this; }
  bool  is_valid() const                         { return 0 <= (intptr_t)this && (intptr_t)this < number_of_registers; }
  bool  has_byte_register() const                { return 0 <= (intptr_t)this && (intptr_t)this < number_of_byte_registers; }
  const char* name() const;
};

// The integer registers of the ia32/amd64 architecture

CONSTANT_REGISTER_DECLARATION(Register, noreg, (-1));


CONSTANT_REGISTER_DECLARATION(Register, rax,    (0));
CONSTANT_REGISTER_DECLARATION(Register, rcx,    (1));
CONSTANT_REGISTER_DECLARATION(Register, rdx,    (2));
CONSTANT_REGISTER_DECLARATION(Register, rbx,    (3));
CONSTANT_REGISTER_DECLARATION(Register, rsp,    (4));
CONSTANT_REGISTER_DECLARATION(Register, rbp,    (5));
CONSTANT_REGISTER_DECLARATION(Register, rsi,    (6));
CONSTANT_REGISTER_DECLARATION(Register, rdi,    (7));
#ifdef AMD64
CONSTANT_REGISTER_DECLARATION(Register, r8,     (8));
CONSTANT_REGISTER_DECLARATION(Register, r9,     (9));
CONSTANT_REGISTER_DECLARATION(Register, r10,   (10));
CONSTANT_REGISTER_DECLARATION(Register, r11,   (11));
CONSTANT_REGISTER_DECLARATION(Register, r12,   (12));
CONSTANT_REGISTER_DECLARATION(Register, r13,   (13));
CONSTANT_REGISTER_DECLARATION(Register, r14,   (14));
CONSTANT_REGISTER_DECLARATION(Register, r15,   (15));
#endif // AMD64

// Use FloatRegister as shortcut
class FloatRegisterImpl;
typedef FloatRegisterImpl* FloatRegister;

inline FloatRegister as_FloatRegister(int encoding) {
  return (FloatRegister)(intptr_t) encoding;
}

// The implementation of floating point registers for the ia32 architecture
class FloatRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
    number_of_registers = 8
  };

  // construction
  inline friend FloatRegister as_FloatRegister(int encoding);

  VMReg as_VMReg();

  // derived registers, offsets, and addresses
  FloatRegister successor() const                          { return as_FloatRegister(encoding() + 1); }

  // accessors
  int   encoding() const                          { assert(is_valid(), "invalid register"); return (intptr_t)this; }
  bool  is_valid() const                          { return 0 <= (intptr_t)this && (intptr_t)this < number_of_registers; }
  const char* name() const;

};

// Use XMMRegister as shortcut
class XMMRegisterImpl;
typedef XMMRegisterImpl* XMMRegister;

// Use MMXRegister as shortcut
class MMXRegisterImpl;
typedef MMXRegisterImpl* MMXRegister;

inline XMMRegister as_XMMRegister(int encoding) {
  return (XMMRegister)(intptr_t)encoding;
}

inline MMXRegister as_MMXRegister(int encoding) {
  return (MMXRegister)(intptr_t)encoding;
}

// The implementation of XMM registers for the IA32 architecture
class XMMRegisterImpl: public AbstractRegisterImpl {
 public:
  enum {
#ifndef AMD64
    number_of_registers = 8
#else
    number_of_registers = 16
#endif // AMD64
  };

  // construction
  friend XMMRegister as_XMMRegister(int encoding);

  VMReg as_VMReg();

  // derived registers, offsets, and addresses
  XMMRegister successor() const                          { return as_XMMRegister(encoding() + 1); }

  // accessors
  int   encoding() const                          { assert(is_valid(), "invalid register"); return (intptr_t)this; }
  bool  is_valid() const                          { return 0 <= (intptr_t)this && (intptr_t)this < number_of_registers; }
  const char* name() const;
};


// The XMM registers, for P3 and up chips
CONSTANT_REGISTER_DECLARATION(XMMRegister, xnoreg , (-1));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm0 , ( 0));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm1 , ( 1));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm2 , ( 2));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm3 , ( 3));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm4 , ( 4));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm5 , ( 5));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm6 , ( 6));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm7 , ( 7));
#ifdef AMD64
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm8,      (8));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm9,      (9));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm10,    (10));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm11,    (11));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm12,    (12));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm13,    (13));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm14,    (14));
CONSTANT_REGISTER_DECLARATION(XMMRegister, xmm15,    (15));
#endif // AMD64

// Only used by the 32bit stubGenerator. These can't be described by vmreg and hence
// can't be described in oopMaps and therefore can't be used by the compilers (at least
// were deopt might wan't to see them).

// The MMX registers, for P3 and up chips
CONSTANT_REGISTER_DECLARATION(MMXRegister, mnoreg , (-1));
CONSTANT_REGISTER_DECLARATION(MMXRegister, mmx0 , ( 0));
CONSTANT_REGISTER_DECLARATION(MMXRegister, mmx1 , ( 1));
CONSTANT_REGISTER_DECLARATION(MMXRegister, mmx2 , ( 2));
CONSTANT_REGISTER_DECLARATION(MMXRegister, mmx3 , ( 3));
CONSTANT_REGISTER_DECLARATION(MMXRegister, mmx4 , ( 4));
CONSTANT_REGISTER_DECLARATION(MMXRegister, mmx5 , ( 5));
CONSTANT_REGISTER_DECLARATION(MMXRegister, mmx6 , ( 6));
CONSTANT_REGISTER_DECLARATION(MMXRegister, mmx7 , ( 7));


// Need to know the total number of registers of all sorts for SharedInfo.
// Define a class that exports it.
class ConcreteRegisterImpl : public AbstractRegisterImpl {
 public:
  enum {
  // A big enough number for C2: all the registers plus flags
  // This number must be large enough to cover REG_COUNT (defined by c2) registers.
  // There is no requirement that any ordering here matches any ordering c2 gives
  // it's optoregs.

    number_of_registers =      RegisterImpl::number_of_registers +
#ifdef AMD64
                               RegisterImpl::number_of_registers +  // "H" half of a 64bit register
#endif // AMD64
                           2 * FloatRegisterImpl::number_of_registers +
                           2 * XMMRegisterImpl::number_of_registers +
                           1 // eflags
  };

  static const int max_gpr;
  static const int max_fpr;
  static const int max_xmm;

};
