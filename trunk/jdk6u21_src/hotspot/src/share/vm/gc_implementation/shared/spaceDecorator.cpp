/*
 * Copyright (c) 2002, 2008, Oracle and/or its affiliates. All rights reserved.
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

# include "incls/_precompiled.incl"
# include "incls/_spaceDecorator.cpp.incl"

// Catch-all file for utility classes

#ifndef PRODUCT

// Returns true is the location q matches the mangling
// pattern.
bool SpaceMangler::is_mangled(HeapWord* q) {
  // This test loses precision but is good enough
  return badHeapWord == (max_juint & (uintptr_t) q->value());
}


void SpaceMangler::set_top_for_allocations(HeapWord* v)  {
  if (v < end()) {
    assert(!CheckZapUnusedHeapArea || is_mangled(v),
      "The high water mark is not mangled");
  }
  _top_for_allocations = v;
}

// Mangle only the unused space that has not previously
// been mangled and that has not been allocated since being
// mangled.
void SpaceMangler::mangle_unused_area() {
  assert(ZapUnusedHeapArea, "Mangling should not be in use");
  // Mangle between top and the high water mark.  Safeguard
  // against the space changing since top_for_allocations was
  // set.
  HeapWord* mangled_end = MIN2(top_for_allocations(), end());
  if (top() < mangled_end) {
    MemRegion mangle_mr(top(), mangled_end);
    SpaceMangler::mangle_region(mangle_mr);
    // Light weight check of mangling.
    check_mangled_unused_area(end());
  }
  // Complete check of unused area which is functional when
  // DEBUG_MANGLING is defined.
  check_mangled_unused_area_complete();
}

// A complete mangle is expected in the
// exceptional case where top_for_allocations is not
// properly tracking the high water mark for mangling.
// This can be the case when to-space is being used for
// scratch space during a mark-sweep-compact.  See
// contribute_scratch() and PSMarkSweep::allocate_stacks().
void SpaceMangler::mangle_unused_area_complete() {
  assert(ZapUnusedHeapArea, "Mangling should not be in use");
  MemRegion mangle_mr(top(), end());
  SpaceMangler::mangle_region(mangle_mr);
}

// Simply mangle the MemRegion mr.
void SpaceMangler::mangle_region(MemRegion mr) {
  assert(ZapUnusedHeapArea, "Mangling should not be in use");
#ifdef ASSERT
  if(TraceZapUnusedHeapArea) {
    gclog_or_tty->print("Mangling [0x%x to 0x%x)", mr.start(), mr.end());
  }
  Copy::fill_to_words(mr.start(), mr.word_size(), badHeapWord);
  if(TraceZapUnusedHeapArea) {
    gclog_or_tty->print_cr(" done");
  }
#endif
}

// Check that top, top_for_allocations and the last
// word of the space are mangled.  In a tight memory
// situation even this light weight mangling could
// cause paging by touching the end of the space.
void  SpaceMangler::check_mangled_unused_area(HeapWord* limit) {
  if (CheckZapUnusedHeapArea) {
    // This method can be called while the spaces are
    // being reshaped so skip the test if the end of the
    // space is beyond the specified limit;
    if (end() > limit) return;

    assert(top() == end() ||
           (is_mangled(top())), "Top not mangled");
    assert((top_for_allocations() < top()) ||
           (top_for_allocations() >= end()) ||
           (is_mangled(top_for_allocations())),
           "Older unused not mangled");
    assert(top() == end() ||
           (is_mangled(end() - 1)), "End not properly mangled");
    // Only does checking when DEBUG_MANGLING is defined.
    check_mangled_unused_area_complete();
  }
}

#undef DEBUG_MANGLING
// This should only be used while debugging the mangling
// because of the high cost of checking the completeness.
void  SpaceMangler::check_mangled_unused_area_complete() {
  if (CheckZapUnusedHeapArea) {
    assert(ZapUnusedHeapArea, "Not mangling unused area");
#ifdef DEBUG_MANGLING
    HeapWord* q = top();
    HeapWord* limit = end();

    bool passed = true;
    while (q < limit) {
      if (!is_mangled(q)) {
        passed = false;
        break;
      }
      q++;
    }
    assert(passed, "Mangling is not complete");
#endif
  }
}
#undef DEBUG_MANGLING
#endif // not PRODUCT