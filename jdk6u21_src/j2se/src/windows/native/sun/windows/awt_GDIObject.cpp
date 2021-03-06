/*
 * @(#)awt_GDIObject.cpp	1.5 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include "awt_GDIObject.h"

/**
 * These methods work around a bug in Windows where allocating
 * the max number of GDI Objects (HDC, Pen, Brush, etc.) will cause the
 * application and desktop to become unusable.  The workaround
 * ensures we never reach this maximum, by refcounting
 * HDC, Pen, and Brush objects that are active.  We increment the refcount
 * when we create these objects and decrement the
 * refcount when we release them, so that our numCurrentObjects
 * counter should always equal the number of unreleased objects.
 * We only do this for HDC, Pen, and Brush because these are the only GDI
 * objects that may grow without bound in our implementation (we cache
 * these objects per thread, so a growing number of threads may have
 * unique HDC/Pen/Brush objects per thread and might approach the maximum).
 * Also, we do not count objects allocated on a temporary basis (such as
 * the many calls to GetDC() in our code, followed quickly by ReleaseDC());
 * we only care about long-lived GDI objects that might bloat our total
 * object usage.
 */

/**
 * Default GDI Object limit for win2k and XP is 10,000
 * Set our limit much lower than that to allow a buffer for objects
 * created beyond the per-thread HDC/Brush/Pen objects we are
 * counting here, including objects created by the overall process
 * (which could include the browser, in the case of applets)
 */
#define MAX_GDI_OBJECTS 9000

// Static initialization of these globals used in AwtGDIObject
int AwtGDIObject::numCurrentObjects = 0;
// this variable will never be deleted. initialized below with SafeCreate.
CriticalSection* AwtGDIObject::objectCounterLock = NULL;
int AwtGDIObject::maxGDIObjects = GetMaxGDILimit();

/**
 * Sets up max GDI limit; we query the registry key that
 * defines this value on WindowsXP and Windows2000.
 * If we fail here, we will use the default value
 * MAX_GDI_OBJECTS as a fallback value.  This is not unreasonable -
 * it seems unlikely that many people would change this
 * registry key setting.
 * NOTE: This function is called automatically at startup to
 * set the value of maxGDIObjects; it should not be necessary to
 * call this function from anywhere else.  Think of it like a static
 * block in Java.
 */
int AwtGDIObject::GetMaxGDILimit() {
    int limit = MAX_GDI_OBJECTS;
    HKEY hKey = NULL;
    DWORD ret = RegOpenKeyEx(HKEY_LOCAL_MACHINE, 
        L"SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Windows", 0, 
        KEY_QUERY_VALUE, &hKey);
    if (ret == ERROR_SUCCESS) {
        DWORD valueLength = 4;
        DWORD regValue;
        ret = RegQueryValueEx(hKey, L"GDIProcessHandleQuota", NULL, NULL, 
            (LPBYTE)&regValue, &valueLength);
        if (ret == ERROR_SUCCESS) {
            // Set limit to 90% of the actual limit to account for other 
            // GDI objects that the process might need
            limit = (int)(regValue * .9);
        } else {
            J2dTraceLn(J2D_TRACE_WARNING, 
                "Problem with RegQueryValueEx in GetMaxGDILimit");
        }
        RegCloseKey(hKey);
    } else {
        J2dTraceLn(J2D_TRACE_WARNING, 
            "Problem with RegOpenKeyEx in GetMaxGDILimit");
    }
    return limit;
}

/**
 * Increment the object counter to indicate that we are about to
 * create a new GDI object.  If the limit has been reached, skip the
 * increment and return FALSE to indicate that an object should
 * not be allocated.
 */
BOOL AwtGDIObject::IncrementIfAvailable() {
    BOOL available;
    CriticalSection* pLock = SafeCreate(objectCounterLock);
    pLock->Enter();
    if (numCurrentObjects < maxGDIObjects) {
        available = TRUE;
        ++numCurrentObjects;
    } else {
        // First, flush the cache; we may have run out simply because 
        // we have unused colors still reserved in the cache
        GDIHashtable::flushAll();
        // Now check again to see if flushing helped.  If not, we really
        // have run out.
        if (numCurrentObjects < maxGDIObjects) {
            available = TRUE;
            ++numCurrentObjects;
        } else {
            available = FALSE;
        }
    }
    pLock->Leave();
    return available;
}

/**
 * Decrement the counter after releasing a GDI Object
 */
void AwtGDIObject::Decrement() {
    CriticalSection* pLock = SafeCreate(objectCounterLock);
    pLock->Enter();
    --numCurrentObjects;
    pLock->Leave();
}

/**
 * This utility method is called by subclasses of AwtGDIObject
 * to ensure capacity for an additional GDI object.  Failure
 * results in throwing an AWTException.
 */
BOOL AwtGDIObject::EnsureGDIObjectAvailability()
{
    if (!IncrementIfAvailable()) {
        // IncrementIfAvailable flushed the cache but still failed; must 
        // have hit the limit.  Throw an exception to indicate the problem.
        if (jvm != NULL) {
            JNIEnv* env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
	    if (env != NULL && !safe_ExceptionOccurred(env)) {
	        JNU_ThrowByName(env, "java/awt/AWTError", 
                    "Pen/Brush creation failure - " \
                    "exceeded maximum GDI resources");
            }
        }
        return FALSE;
    }
    return TRUE;
}
