/*******************************************************************************
 * Copyright (c) 2008, 2010 Xuggle Inc.  All rights reserved.
 *  
 * This file is part of Xuggle-Xuggler-Main.
 *
 * Xuggle-Xuggler-Main is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xuggle-Xuggler-Main is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Xuggle-Xuggler-Main.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

#include <cmath>
#include <cstring>

#include <com/xuggle/ferry/JNIHelper.h>
#include <com/xuggle/ferry/Logger.h>
#include <com/xuggle/ferry/Mutex.h>


#include <com/xuggle/xuggler/Global.h>
#include <com/xuggle/xuggler/FfmpegIncludes.h>
#include <com/xuggle/xuggler/Container.h>
#include <com/xuggle/xuggler/ContainerFormat.h>
#include <com/xuggle/xuggler/Codec.h>
#include <com/xuggle/xuggler/Rational.h>
#include <com/xuggle/xuggler/AudioSamples.h>
#include <com/xuggle/xuggler/VideoPicture.h>
#include <com/xuggle/xuggler/AudioResampler.h>
#include <com/xuggle/xuggler/VideoResampler.h>
#include <com/xuggle/xuggler/MediaDataWrapper.h>

/**
 * WARNING: Do not use logging in this class, and do
 * not set any static file variables to values other
 * than zero.  The class loader
 * in Java will call Global::init, and MacOS will crash
 * if during the JNI library load the method we call calls
 * back into Java:
 * http://code.google.com/p/xuggle/issues/detail?id=174
 */
namespace com { namespace xuggle { namespace xuggler
{
  using namespace com::xuggle::ferry;

  /*
   * This function will be called back by Ffmpeg anytime
   * it wants to log.  We then use it to dump
   * stuff into our own logs.
   */
  static void
  xuggler_log_callback(void* ptr, int level, const char* fmt, va_list va)
  {
    static Logger* ffmpegLogger = 0;
    AVClass* avc = ptr ? *(AVClass**)ptr : 0;

    int currentLevel = av_log_get_level();
//    fprintf(stderr, "current level: %d; log level: %d\n", level, currentLevel);
    if (level > currentLevel || currentLevel < AV_LOG_PANIC)
      // just return
      return;

    if (!ffmpegLogger)
    {
      Global::lock();
      if (!ffmpegLogger)
        ffmpegLogger = Logger::getStaticLogger( "org.ffmpeg" );
      Global::unlock();
    }

    Logger::Level logLevel;
    if (level <= AV_LOG_ERROR)
      logLevel = Logger::LEVEL_ERROR;
    else if (level <= AV_LOG_WARNING)
      logLevel = Logger::LEVEL_WARN;
    else if (level <= AV_LOG_INFO)
      logLevel = Logger::LEVEL_INFO;
    else if (level <= AV_LOG_DEBUG)
      logLevel = Logger::LEVEL_DEBUG;
    else
      logLevel = Logger::LEVEL_TRACE;

    // Revise the format string to add additional useful info
    char revisedFmt[1024];
    revisedFmt[sizeof(revisedFmt)-1] = 0;
    if (avc)
    {
      snprintf(revisedFmt, sizeof(revisedFmt), "[%s @ %p] %s",
          avc->item_name(ptr), ptr, fmt);
    }
    else
    {
      snprintf(revisedFmt, sizeof(revisedFmt), "%s", fmt);
    }
    int len = strlen(revisedFmt);
    if (len > 0 && revisedFmt[len-1] == '\n')
    {
      revisedFmt[len-1]=0;
      --len;
    }
    if (len > 0)
      // it's not useful to pass in filenames and line numbers here.
      ffmpegLogger->logVA(0, 0, logLevel, revisedFmt, va);
  }

  int
  Global :: avioInterruptCB(void*)
  {
    JNIHelper* helper = JNIHelper::getHelper();
    int retval = 0;
    if (helper) {
      retval = helper->isInterrupted();
    }
    return retval;
  }
  
  static int xuggler_lockmgr_cb(void** ctx, enum AVLockOp op)
  {
    if (!ctx)
      return 1;
    
    int retval=0;
    com::xuggle::ferry::Mutex* mutex=
      static_cast<com::xuggle::ferry::Mutex*>(*ctx);
    switch(op)
    {
      case AV_LOCK_CREATE:
        mutex = com::xuggle::ferry::Mutex::make();
        *ctx = mutex;
        retval = !!mutex;
        break;
      case AV_LOCK_DESTROY:
        if (mutex) mutex->release();
        *ctx = 0;
        break;
      case AV_LOCK_OBTAIN:
        if (mutex) mutex->lock();
        break;
      case AV_LOCK_RELEASE:
        if (mutex) mutex->unlock();
        break;
    }
    return retval;
  }
  
  Global* Global :: sGlobal = 0;

  void
  Global :: init()
  {
    if (!sGlobal)
    {
      av_lockmgr_register(xuggler_lockmgr_cb);
      av_log_set_callback(xuggler_log_callback);
      av_log_set_level(AV_LOG_ERROR); // Only log errors by default
      av_register_all();
      avformat_network_init();
      // and set up filter support
      avfilter_register_all();

      // and set up the device library for webcam support
      avdevice_register_all();

      // turn down logging
      sGlobal = new Global();
    }
  }
  void
  Global :: deinit()
  {
    avformat_network_deinit();
  }

  void
  Global :: destroyStaticGlobal(JavaVM*vm,void*closure)
  {
    Global *val = (Global*)closure;
    if (!vm && val) {
      av_lockmgr_register(0);
      delete val;
    }
  }

  Global :: Global()
  {
    com::xuggle::ferry::JNIHelper::sRegisterTerminationCallback(
        Global::destroyStaticGlobal,
        this);
    mLock = com::xuggle::ferry::Mutex::make();
  }

  Global :: ~Global()
  {
    if (mLock)
      mLock->release();
  }
  
  void
  Global :: lock()
  {
    Global::init();
    if (sGlobal && sGlobal->mLock)
      sGlobal->mLock->lock();
  }

  void
  Global :: unlock()
  {
    Global::init();
    if (sGlobal && sGlobal->mLock)
      sGlobal->mLock->unlock();
  }

  int64_t
  Global :: getVersion()
  {
    return ((int64_t)getVersionMajor())<<48 | ((int64_t)getVersionMinor())<<32 | (int64_t)getVersionRevision();
  }
  int32_t
  Global :: getVersionMajor()
  {
    Global::init();
    return VS_LIB_MAJOR_VERSION;
  }
  int32_t
  Global:: getVersionMinor()
  {
    Global::init();
    return VS_LIB_MINOR_VERSION;
  }
  int32_t
  Global :: getVersionRevision()
  {
    Global::init();
    return VS_REVISION;
  }
  
  const char *
  Global :: getVersionStr()
  {
    Global::init();
    return PACKAGE_VERSION "."  VS_STRINGIFY(VS_REVISION);
  }
  int32_t
  Global :: getAVFormatVersion()
  {
    Global::init();
    return LIBAVFORMAT_VERSION_INT;
  }
  const char *
  Global :: getAVFormatVersionStr()
  {
    Global::init();
    return AV_STRINGIFY(LIBAVFORMAT_VERSION);
  }
  int
  Global :: getAVCodecVersion()
  {
    Global::init();
    return LIBAVCODEC_VERSION_INT;
  }
  const char *
  Global :: getAVCodecVersionStr()
  {
    Global::init();
    return AV_STRINGIFY(LIBAVCODEC_VERSION);
  }

  void
  Global :: setFFmpegLoggingLevel(int32_t level)
  {
    Global::init();
    if (level < AV_LOG_PANIC)
      av_log_set_level(AV_LOG_QUIET);
    else if (level < AV_LOG_FATAL)
      av_log_set_level(AV_LOG_PANIC);
    else if (level < AV_LOG_ERROR)
      av_log_set_level(AV_LOG_FATAL);
    else if (level < AV_LOG_WARNING)
      av_log_set_level(AV_LOG_ERROR);
    else if (level < AV_LOG_INFO)
      av_log_set_level(AV_LOG_WARNING);
    else if (level < AV_LOG_VERBOSE)
      av_log_set_level(AV_LOG_INFO);
    else if (level < AV_LOG_DEBUG)
      av_log_set_level(AV_LOG_VERBOSE);
    else
      av_log_set_level(AV_LOG_DEBUG);
//    fprintf(stderr, "FFmpeg logging level = %d\n", av_log_get_level());
  }
}}}
