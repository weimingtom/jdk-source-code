/*
 * Copyright (c) 1997, 2006, Oracle and/or its affiliates. All rights reserved.
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

#include "incls/_precompiled.incl"
#include "incls/_icBuffer.cpp.incl"


DEF_STUB_INTERFACE(ICStub);

StubQueue* InlineCacheBuffer::_buffer    = NULL;
ICStub*    InlineCacheBuffer::_next_stub = NULL;


void ICStub::finalize() {
  if (!is_empty()) {
    ResourceMark rm;
    CompiledIC *ic = CompiledIC_at(ic_site());
    assert(CodeCache::find_nmethod(ic->instruction_address()) != NULL, "inline cache in non-nmethod?");

    assert(this == ICStub_from_destination_address(ic->stub_address()), "wrong owner of ic buffer");
    ic->set_cached_oop(cached_oop());
    ic->set_ic_destination(destination());
  }
}


address ICStub::destination() const {
  return InlineCacheBuffer::ic_buffer_entry_point(code_begin());
}

oop ICStub::cached_oop() const {
  return InlineCacheBuffer::ic_buffer_cached_oop(code_begin());
}


void ICStub::set_stub(CompiledIC *ic, oop cached_value, address dest_addr) {
  // We cannot store a pointer to the 'ic' object, since it is resource allocated. Instead we
  // store the location of the inline cache. Then we have enough information recreate the CompiledIC
  // object when we need to remove the stub.
  _ic_site = ic->instruction_address();

  // Assemble new stub
  InlineCacheBuffer::assemble_ic_buffer_code(code_begin(), cached_value, dest_addr);
  assert(destination() == dest_addr,   "can recover destination");
  assert(cached_oop() == cached_value, "can recover destination");
}


void ICStub::clear() {
  _ic_site = NULL;
}


#ifndef PRODUCT
// anybody calling to this stub will trap

void ICStub::verify() {
}

void ICStub::print() {
  tty->print_cr("ICStub: site: " INTPTR_FORMAT, _ic_site);
}
#endif

//-----------------------------------------------------------------------------------------------
// Implementation of InlineCacheBuffer

void InlineCacheBuffer::init_next_stub() {
  ICStub* ic_stub = (ICStub*)buffer()->request_committed (ic_stub_code_size());
  assert (ic_stub != NULL, "no room for a single stub");
  set_next_stub(ic_stub);
}

void InlineCacheBuffer::initialize() {
  if (_buffer != NULL) return; // already initialized
  _buffer = new StubQueue(new ICStubInterface, 10*K, InlineCacheBuffer_lock, "InlineCacheBuffer");
  assert (_buffer != NULL, "cannot allocate InlineCacheBuffer");
  init_next_stub();
}


ICStub* InlineCacheBuffer::new_ic_stub() {
  while (true) {
    ICStub* ic_stub = (ICStub*)buffer()->request_committed(ic_stub_code_size());
    if (ic_stub != NULL) {
      return ic_stub;
    }
    // we ran out of inline cache buffer space; must enter safepoint.
    // We do this by forcing a safepoint
    EXCEPTION_MARK;

    VM_ForceSafepoint vfs;
    VMThread::execute(&vfs);
    // We could potential get an async. exception at this point.
    // In that case we will rethrow it to ourselvs.
    if (HAS_PENDING_EXCEPTION) {
      oop exception = PENDING_EXCEPTION;
      CLEAR_PENDING_EXCEPTION;
      Thread::send_async_exception(JavaThread::current()->threadObj(), exception);
    }
  }
  ShouldNotReachHere();
  return NULL;
}


void InlineCacheBuffer::update_inline_caches() {
  if (buffer()->number_of_stubs() > 1) {
    if (TraceICBuffer) {
      tty->print_cr("[updating inline caches with %d stubs]", buffer()->number_of_stubs());
    }
    buffer()->remove_all();
    init_next_stub();
  }
}


bool InlineCacheBuffer::contains(address instruction_address) {
  return buffer()->contains(instruction_address);
}


bool InlineCacheBuffer::is_empty() {
  return buffer()->number_of_stubs() == 1;    // always has sentinel
}


void InlineCacheBuffer_init() {
  InlineCacheBuffer::initialize();
}


void InlineCacheBuffer::create_transition_stub(CompiledIC *ic, oop cached_oop, address entry) {
  assert(!SafepointSynchronize::is_at_safepoint(), "should not be called during a safepoint");
  assert (CompiledIC_lock->is_locked(), "");
  assert(cached_oop == NULL || cached_oop->is_perm(), "must belong to perm. space");
  if (TraceICBuffer) { tty->print_cr("  create transition stub for " INTPTR_FORMAT, ic->instruction_address()); }

  // If an transition stub is already associate with the inline cache, then we remove the association.
  if (ic->is_in_transition_state()) {
    ICStub* old_stub = ICStub_from_destination_address(ic->stub_address());
    old_stub->clear();
  }

  // allocate and initialize new "out-of-line" inline-cache
  ICStub* ic_stub = get_next_stub();
  ic_stub->set_stub(ic, cached_oop, entry);

  // Update inline cache in nmethod to point to new "out-of-line" allocated inline cache
  ic->set_ic_destination(ic_stub->code_begin());

  set_next_stub(new_ic_stub()); // can cause safepoint synchronization
}


address InlineCacheBuffer::ic_destination_for(CompiledIC *ic) {
  ICStub* stub = ICStub_from_destination_address(ic->stub_address());
  return stub->destination();
}


oop InlineCacheBuffer::cached_oop_for(CompiledIC *ic) {
  ICStub* stub = ICStub_from_destination_address(ic->stub_address());
  return stub->cached_oop();
}
