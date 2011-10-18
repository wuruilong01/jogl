/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <unistd.h>
#include <AppKit/AppKit.h>

#include "NativewindowCommon.h"
#include "jogamp_nativewindow_macosx_OSXUtil.h"

static const char * const ClazzNameRunnable = "java/lang/Runnable";
static jmethodID runnableRunID = NULL;

static const char * const ClazzNamePoint = "javax/media/nativewindow/util/Point";
static const char * const ClazzAnyCstrName = "<init>";
static const char * const ClazzNamePointCstrSignature = "(II)V";
static jclass pointClz = NULL;
static jmethodID pointCstr = NULL;

static int _initialized=0;

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_macosx_OSXUtil_initIDs0(JNIEnv *env, jclass _unused) {
    if(0==_initialized) {
        jclass c;
        c = (*env)->FindClass(env, ClazzNamePoint);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't find %s", ClazzNamePoint);
        }
        pointClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==pointClz) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't use %s", ClazzNamePoint);
        }
        pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        if(NULL==pointCstr) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't fetch %s.%s %s",
                ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        }

        c = (*env)->FindClass(env, ClazzNameRunnable);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't find %s", ClazzNameRunnable);
        }
        runnableRunID = (*env)->GetMethodID(env, c, "run", "()V");
        if(NULL==runnableRunID) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't fetch %s.run()V", ClazzNameRunnable);
        }
        _initialized=1;
    }
    return JNI_TRUE;
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    getLocationOnScreenImpl0
 * Signature: (JII)Ljavax/media/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_GetLocationOnScreen0
  (JNIEnv *env, jclass unused, jlong winOrView, jint src_x, jint src_y)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    /**
     * return location in 0/0 top-left space,
     * OSX is 0/0 bottom-left space naturally
     */
    NSRect r;
    int dest_x=-1;
    int dest_y=-1;

    NSObject *nsObj = (NSObject*) ((intptr_t) winOrView);
    NSWindow* win = NULL;
    NSView* view = NULL;

    if( [nsObj isKindOfClass:[NSWindow class]] ) {
        win = (NSWindow*) nsObj;
        view = [win contentView];
    } else if( nsObj != NULL && [nsObj isKindOfClass:[NSView class]] ) {
        view = (NSView*) nsObj;
        win = [view window];
    } else {
        NativewindowCommon_throwNewRuntimeException(env, "neither win not view %p\n", nsObj);
    }
    NSScreen* screen = [win screen];
    NSRect screenRect = [screen frame];

    NSRect viewFrame = [view frame];

    r.origin.x = src_x;
    r.origin.y = viewFrame.size.height - src_y; // y-flip for 0/0 top-left
    r.size.width = 0;
    r.size.height = 0;
    // NSRect rS = [win convertRectToScreen: r]; // 10.7
    NSPoint oS = [win convertBaseToScreen: r.origin];
    dest_x = (int) oS.x;
    dest_y = (int) screenRect.origin.y + screenRect.size.height - oS.y;

    jobject res = (*env)->NewObject(env, pointClz, pointCstr, (jint)dest_x, (jint)dest_y);

    [pool release];

    return res;
}

@interface MainRunnable : NSObject

{
    JavaVM *jvmHandle;
    int jvmVersion;
    jobject runnableObj;
}

- (id) initWithRunnable: (jobject)runnable jvmHandle: (JavaVM*)jvm jvmVersion: (int)jvmVers;
- (void) jRun;

@end

@implementation MainRunnable

- (id) initWithRunnable: (jobject)runnable jvmHandle: (JavaVM*)jvm jvmVersion: (int)jvmVers
{
    jvmHandle = jvm;
    jvmVersion = jvmVers;
    runnableObj = runnable;
    return [super init];
}

- (void) jRun
{
    int shallBeDetached = 0;
    JNIEnv* env = NativewindowCommon_GetJNIEnv(jvmHandle, jvmVersion, &shallBeDetached);
    if(NULL!=env) {
        (*env)->CallVoidMethod(env, runnableObj, runnableRunID);

        if (shallBeDetached) {
            (*jvmHandle)->DetachCurrentThread(jvmHandle);
        }
    }
}

@end


/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    RunOnMainThread0
 * Signature: (ZLjava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_RunOnMainThread0
  (JNIEnv *env, jclass unused, jboolean jwait, jobject runnable)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    if ( NO == [NSThread isMainThread] ) {
        jobject runnableGlob = (*env)->NewGlobalRef(env, runnable);

        BOOL wait = (JNI_TRUE == jwait) ? YES : NO;
        JavaVM *jvmHandle = NULL;
        int jvmVersion = 0;

        if(0 != (*env)->GetJavaVM(env, &jvmHandle)) {
            jvmHandle = NULL;
        } else {
            jvmVersion = (*env)->GetVersion(env);
        }

        MainRunnable * mr = [[MainRunnable alloc] initWithRunnable: runnableGlob jvmHandle: jvmHandle jvmVersion: jvmVersion];
        [mr performSelectorOnMainThread:@selector(jRun) withObject:nil waitUntilDone:wait];
        [mr release];

        (*env)->DeleteGlobalRef(env, runnableGlob);
    } else {
        (*env)->CallVoidMethod(env, runnable, runnableRunID);
    }

    [pool release];
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    RunOnMainThread0
 * Signature: (ZLjava/lang/Runnable;)V
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_IsMainThread0
  (JNIEnv *env, jclass unused)
{
    return ( [NSThread isMainThread] == YES ) ? JNI_TRUE : JNI_FALSE ;
}