#ifdef USE_PRAGMA_IDENT_HDR
#pragma ident "@(#)memoryManager.cpp	1.16 04/03/31 15:34:52 JVM"
#endif
//
// Copyright 2004 Sun Microsystems, Inc.  All rights reserved.
// SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
//

# include "incls/_precompiled.incl"
# include "incls/_memoryManager.cpp.incl"

MemoryManager::MemoryManager() {
  _num_pools = 0;
  _memory_mgr_obj = NULL;
}

void MemoryManager::add_pool(MemoryPool* pool) {
  assert(_num_pools < MemoryManager::max_num_pools, "_num_pools exceeds the max");  
  if (_num_pools < MemoryManager::max_num_pools) {
    _pools[_num_pools] = pool;
    _num_pools++;
  }
  pool->add_manager(this);
}

MemoryManager* MemoryManager::get_code_cache_memory_manager() {
  return (MemoryManager*) new CodeCacheMemoryManager();
}

GCMemoryManager* MemoryManager::get_copy_memory_manager() {
  return (GCMemoryManager*) new CopyMemoryManager();
}

GCMemoryManager* MemoryManager::get_msc_memory_manager() {
  return (GCMemoryManager*) new MSCMemoryManager();
}

GCMemoryManager* MemoryManager::get_train_memory_manager() {
  return (GCMemoryManager*) new TrainMemoryManager();
}

GCMemoryManager* MemoryManager::get_parnew_memory_manager() {
  return (GCMemoryManager*) new ParNewMemoryManager();
}

GCMemoryManager* MemoryManager::get_cms_memory_manager() {
  return (GCMemoryManager*) new CMSMemoryManager();
}

GCMemoryManager* MemoryManager::get_psScavenge_memory_manager() {
  return (GCMemoryManager*) new PSScavengeMemoryManager();
}

GCMemoryManager* MemoryManager::get_psMarkSweep_memory_manager() {
  return (GCMemoryManager*) new PSMarkSweepMemoryManager();
}

instanceOop MemoryManager::get_memory_manager_instance(TRAPS) {
  // Must do an acquire so as to force ordering of subsequent
  // loads from anything _memory_mgr_obj points to or implies.
  instanceOop mgr_obj = (instanceOop)OrderAccess::load_ptr_acquire(&_memory_mgr_obj);
  if (mgr_obj == NULL) {
    // It's ok for more than one thread to execute the code up to the locked region.
    // Extra manager instances will just be gc'ed.
    klassOop k = Management::sun_management_ManagementFactory_klass(CHECK_0);
    instanceKlassHandle ik(THREAD, k);
    
    Handle mgr_name = java_lang_String::create_from_str(name(), CHECK_0);

    JavaValue result(T_OBJECT);
    JavaCallArguments args;
    args.push_oop(mgr_name);    // Argument 1

    symbolHandle method_name;
    symbolHandle signature;
    if (is_gc_memory_manager()) {
      method_name = vmSymbolHandles::createGarbageCollector_name();
      signature = vmSymbolHandles::createGarbageCollector_signature();
      args.push_oop(NULL);      // Argument 2 (for future extension) 
    } else {
      method_name = vmSymbolHandles::createMemoryManager_name();
      signature = vmSymbolHandles::createMemoryManager_signature();
    }

    // ---------------------------------------------------------------------
    // Transitioning: TO BE REMOVED after JDK support for MXBean is putback
    // ---------------------------------------------------------------------
    if (ik->find_method(method_name(), signature()) == NULL) {
      if (is_gc_memory_manager()) {
        method_name = vmSymbolHandles::createGarbageCollectorMBean_name();
        signature = vmSymbolHandles::createGarbageCollectorMBean_signature();
      } else {
        method_name = vmSymbolHandles::createMemoryManagerMBean_name();
        signature = vmSymbolHandles::createMemoryManagerMBean_signature();
      }
    }

    JavaCalls::call_static(&result,
                           ik,
                           method_name,
                           signature,
                           &args,
                           CHECK_0);

    instanceOop m = (instanceOop) result.get_jobject();
    instanceHandle mgr(THREAD, m);

    {
      // Get lock before setting _memory_mgr_obj
      // since another thread may have created the instance
      MutexLocker ml(Management_lock);

      // Check if another thread has created the management object.  We reload
      // _memory_mgr_obj here because some other thread may have initialized
      // it while we were executing the code before the lock.
      //
      // The lock has done an acquire, so the load can't float above it, but
      // we need to do a load_acquire as above.
      mgr_obj = (instanceOop)OrderAccess::load_ptr_acquire(&_memory_mgr_obj);
      if (mgr_obj != NULL) {
         return mgr_obj;
      }

      // Get the address of the object we created via call_special.
      mgr_obj = mgr();

      // Use store barrier to make sure the memory accesses associated
      // with creating the management object are visible before publishing
      // its address.  The unlock will publish the store to _memory_mgr_obj
      // because it does a release first.
      OrderAccess::release_store_ptr(&_memory_mgr_obj, mgr_obj);
    }
  }

  return mgr_obj;
}

void MemoryManager::oops_do(OopClosure* f) {
  f->do_oop((oop*) &_memory_mgr_obj);
}

GCStatInfo::GCStatInfo(int num_pools) {
  // initialize the arrays for memory usage
  _before_gc_usage_array = (MemoryUsage*) NEW_C_HEAP_ARRAY(MemoryUsage, num_pools);
  _after_gc_usage_array  = (MemoryUsage*) NEW_C_HEAP_ARRAY(MemoryUsage, num_pools);
  size_t len = num_pools * sizeof(MemoryUsage);
  memset(_before_gc_usage_array, 0, len);
  memset(_after_gc_usage_array, 0, len);
  _usage_array_size = num_pools;
}

GCStatInfo::~GCStatInfo() {
  FREE_C_HEAP_ARRAY(MemoryUsage*, _before_gc_usage_array);
  FREE_C_HEAP_ARRAY(MemoryUsage*, _after_gc_usage_array);
}

void GCStatInfo::copy_stat(GCStatInfo* stat) {
  set_index(stat->gc_index());
  set_start_time(stat->start_time());
  set_end_time(stat->end_time());
  assert(_usage_array_size == stat->usage_array_size(), "Must have same array size");
  for (int i = 0; i < _usage_array_size; i++) {
    set_before_gc_usage(i, stat->before_gc_usage_for_pool(i));
    set_after_gc_usage(i, stat->after_gc_usage_for_pool(i));
  }
}

void GCStatInfo::set_gc_usage(int pool_index, MemoryUsage usage, bool before_gc) {
  MemoryUsage* gc_usage_array;
  if (before_gc) {
    gc_usage_array = _before_gc_usage_array;
  } else {
    gc_usage_array = _after_gc_usage_array;
  }
  gc_usage_array[pool_index] = usage;
}

GCMemoryManager::GCMemoryManager() : MemoryManager() {
  _num_collections = 0;
  _last_gc_stat = NULL;
  _num_gc_threads = 1;
}

GCMemoryManager::~GCMemoryManager() {
  delete _last_gc_stat;
}

void GCMemoryManager::initialize_gc_stat_info() {
  assert(MemoryService::num_memory_pools() > 0, "should have one or more memory pools");
  _last_gc_stat = new GCStatInfo(MemoryService::num_memory_pools());
}

void GCMemoryManager::gc_begin() {
  assert(_last_gc_stat != NULL, "Just checking");
  _accumulated_timer.start();
  _num_collections++;
  _last_gc_stat->set_index(_num_collections);
  _last_gc_stat->set_start_time(Management::timestamp());

  // Keep memory usage of all memory pools
  for (int i = 0; i < MemoryService::num_memory_pools(); i++) {
    MemoryPool* pool = MemoryService::get_memory_pool(i);
    _last_gc_stat->set_before_gc_usage(i, pool->get_memory_usage());
  }
}

void GCMemoryManager::gc_end() {
  _accumulated_timer.stop();
  _last_gc_stat->set_end_time(Management::timestamp());

  int i;
  // keep the last gc statistics for all memory pools
  for (i = 0; i < MemoryService::num_memory_pools(); i++) {
    MemoryPool* pool = MemoryService::get_memory_pool(i);
    MemoryUsage usage = pool->get_memory_usage();

    _last_gc_stat->set_after_gc_usage(i, usage);
  }

  // Set last collection usage of the memory pools managed by this collector
  for (i = 0; i < num_memory_pools(); i++) {
    MemoryPool* pool = get_memory_pool(i);
    MemoryUsage usage = pool->get_memory_usage();

    // Compare with GC usage threshold
    pool->set_last_collection_usage(usage);
    LowMemoryDetector::detect_after_gc_memory(pool);
  }
}
