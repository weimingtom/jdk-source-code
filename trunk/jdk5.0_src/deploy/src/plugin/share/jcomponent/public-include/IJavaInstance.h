/*
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#if !defined(__IJavaInstance__)
#define __IJavaInstance__

#include "IEgo.h"

interface IJavaInstance : public IEgo {

    virtual void start()=0;
    virtual void stop()=0;
    virtual void destroy()=0;
    virtual void window(int, int, int, int, int)=0;
    virtual void javascriptReply(const char *)=0;
    virtual void docbase(const char *)=0;

};
#endif

