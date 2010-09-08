/*
 * @(#)SocketDispatcher.c	1.20 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#include <windows.h>
#include <winsock2.h>
#include <ctype.h>
#include "jni.h"
#include "jni_util.h"
#include "jvm.h"
#include "jlong.h"
#include "sun_nio_ch_SocketDispatcher.h"
#include "nio.h"
#include "nio_util.h"


/**************************************************************
 * SocketDispatcher.c
 */

JNIEXPORT jint JNICALL
Java_sun_nio_ch_SocketDispatcher_read0(JNIEnv *env, jclass clazz, jobject fdo,
                                      jlong address, jint len)
{
    /* set up */
    int i = 0;
    DWORD read = 0;
    DWORD flags = 0;
    jint fd = fdval(env, fdo);
    WSABUF buf;

    /* destination buffer and size */
    buf.buf = (char *)address;
    buf.len = (u_long)len;
    
    /* read into the buffers */
    i = WSARecv((SOCKET)fd, /* Socket */
            &buf,           /* pointers to the buffers */
            (DWORD)1,       /* number of buffers to process */
            &read,          /* receives number of bytes read */
            &flags,         /* no flags */
            0,              /* no overlapped sockets */
            0);             /* no completion routine */

    if (i == SOCKET_ERROR) {
        int theErr = (jint)WSAGetLastError();
        if (theErr == WSAEWOULDBLOCK) {
            return IOS_UNAVAILABLE;
        }
        JNU_ThrowIOExceptionWithLastError(env, "Read failed");
        return IOS_THROWN;
    }

    return convertReturnVal(env, (jint)read, JNI_TRUE);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_SocketDispatcher_readv0(JNIEnv *env, jclass clazz, jobject fdo,
                                       jlong address, jint len)
{
    /* set up */
    int i = 0;
    DWORD read = 0;
    DWORD flags = 0;
    jint fd = fdval(env, fdo);
    struct iovec *iovp = (struct iovec *)address;
    WSABUF *bufs = malloc(len * sizeof(WSABUF));

    if (bufs == 0) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return IOS_THROWN;
    }

    if ((isNT() == JNI_FALSE) && (len > 16)) {
        len = 16;
    }

    /* copy iovec into WSABUF */
    for(i=0; i<len; i++) {
        bufs[i].buf = (char *)iovp[i].iov_base;
        bufs[i].len = (u_long)iovp[i].iov_len;
    }
    
    /* read into the buffers */
    i = WSARecv((SOCKET)fd, /* Socket */
            bufs,           /* pointers to the buffers */
            (DWORD)len,     /* number of buffers to process */
            &read,          /* receives number of bytes read */
            &flags,         /* no flags */
            0,              /* no overlapped sockets */
            0);             /* no completion routine */

    /* clean up */
    free(bufs);

    if (i != 0) {
        int theErr = (jint)WSAGetLastError();
        if (theErr == WSAEWOULDBLOCK) {
            return IOS_UNAVAILABLE;
        }
        JNU_ThrowIOExceptionWithLastError(env, "Vector read failed");
        return IOS_THROWN;
    }

    return convertLongReturnVal(env, (jlong)read, JNI_TRUE);
}

JNIEXPORT jint JNICALL
Java_sun_nio_ch_SocketDispatcher_write0(JNIEnv *env, jclass clazz, jobject fdo,
                                       jlong address, jint len)
{
    /* set up */
    int i = 0;
    DWORD written = 0;
    jint fd = fdval(env, fdo);
    WSABUF buf;

    /* copy iovec into WSABUF */
    buf.buf = (char *)address;
    buf.len = (u_long)len;
    
    /* read into the buffers */
    i = WSASend((SOCKET)fd, /* Socket */
            &buf,           /* pointers to the buffers */
            (DWORD)1,       /* number of buffers to process */
            &written,       /* receives number of bytes written */
            0,              /* no flags */
            0,              /* no overlapped sockets */
            0);             /* no completion routine */

    if (i == SOCKET_ERROR) {
        int theErr = (jint)WSAGetLastError();
        if (theErr == WSAEWOULDBLOCK) {
            return IOS_UNAVAILABLE;
        }
        JNU_ThrowIOExceptionWithLastError(env, "Write failed");
        return IOS_THROWN;
    }

    return convertReturnVal(env, (jint)written, JNI_FALSE);
}

JNIEXPORT jlong JNICALL
Java_sun_nio_ch_SocketDispatcher_writev0(JNIEnv *env, jclass clazz,
                                         jobject fdo, jlong address, jint len)
{
    /* set up */
    int i = 0;
    DWORD written = 0;
    jint fd = fdval(env, fdo);
    struct iovec *iovp = (struct iovec *)address;
    WSABUF *bufs = malloc(len * sizeof(WSABUF));

    if (bufs == 0) {
        JNU_ThrowOutOfMemoryError(env, 0);
        return IOS_THROWN;
    }

    if ((isNT() == JNI_FALSE) && (len > 16)) {
        len = 16;
    }

    /* copy iovec into WSABUF */
    for(i=0; i<len; i++) {
        bufs[i].buf = (char *)iovp[i].iov_base;
        bufs[i].len = (u_long)iovp[i].iov_len;
    }
    
    /* read into the buffers */
    i = WSASend((SOCKET)fd, /* Socket */
            bufs,           /* pointers to the buffers */
            (DWORD)len,     /* number of buffers to process */
            &written,       /* receives number of bytes written */
            0,              /* no flags */
            0,              /* no overlapped sockets */
            0);             /* no completion routine */

    /* clean up */
    free(bufs);

    if (i != 0) {
        int theErr = (jint)WSAGetLastError();
        if (theErr == WSAEWOULDBLOCK) {
            return IOS_UNAVAILABLE;
        }
        JNU_ThrowIOExceptionWithLastError(env, "Vector write failed");
        return IOS_THROWN;
    }

    return convertLongReturnVal(env, (jlong)written, JNI_FALSE);
}

JNIEXPORT void JNICALL
Java_sun_nio_ch_SocketDispatcher_close0(JNIEnv *env, jclass clazz,
                                         jobject fdo)
{
    jint fd = fdval(env, fdo);
    struct linger l;
    int len = sizeof(l);

    if (fd != -1) {
        int result = 0;
        if (getsockopt(fd, SOL_SOCKET, SO_LINGER, (char *)&l, &len) == 0) {
            if (l.l_onoff == 0) {
                WSASendDisconnect(fd, NULL);
            }
        }
        result = closesocket(fd);
        if (result == SOCKET_ERROR) {
            JNU_ThrowIOExceptionWithLastError(env, "Socket close failed");
        }
    }
}
