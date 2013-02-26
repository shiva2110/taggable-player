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

#include <stdexcept>
// for memset
#include <cstring>
#include <com/xuggle/ferry/Logger.h>
#include <com/xuggle/ferry/RefPointer.h>
#include <com/xuggle/xuggler/Global.h>
#include "com/xuggle/xuggler/VideoPicture.h"

VS_LOG_SETUP(VS_CPP_PACKAGE);

namespace com { namespace xuggle { namespace xuggler
{

  VideoPicture :: VideoPicture()
  {
    mIsComplete = false;
    mFrame = avcodec_alloc_frame();
    if (!mFrame)
      throw std::bad_alloc();
    // Set the private data pointer to point to me.
    mFrame->opaque = this;
    mFrame->width = -1;
    mFrame->height = -1;

    mFrame->format = (int) IPixelFormat::NONE;
    mTimeBase = IRational::make(1, 1000000);
  }

  VideoPicture :: ~VideoPicture()
  {
    if (mFrame)
      av_free(mFrame);
    mFrame = 0;
  }

  VideoPicture*
  VideoPicture :: make(IPixelFormat::Type format, int width, int height)
  {
    VideoPicture * retval=0;
    try {
      retval = VideoPicture::make();
      retval->mFrame->format = format;
      retval->mFrame->width = width;
      retval->mFrame->height = height;
      // default new frames to be key frames
      retval->setKeyFrame(true);
    }
    catch (std::bad_alloc &e)
    {
      VS_REF_RELEASE(retval);
      throw e;
    }
    catch (std::exception& e)
    {
      VS_LOG_DEBUG("error: %s", e.what());
      VS_REF_RELEASE(retval);
    }

    return retval;
  }
  
  VideoPicture*
  VideoPicture :: make(
      com::xuggle::ferry::IBuffer* buffer, IPixelFormat::Type format,
      int width, int height)
  {
    if (!buffer)
      return 0;
    VideoPicture *retval = 0;
    try {
      retval = make(format, width,height);
      if (!retval)
        throw std::bad_alloc();
      
      int32_t size = retval->getSize();
      if (size > 0 && size > buffer->getBufferSize())
        throw std::runtime_error("input buffer is not large enough for given picture");
      
      /** Use the buffer */
      retval->mBuffer.reset(buffer, true);
      /** Set the internal flags */
      unsigned char* bytes = (unsigned char*)buffer->getBytes(0, size);
      if (!bytes)
        throw std::runtime_error("could not access raw memory in buffer");

      (void) avpicture_fill((AVPicture*)retval->mFrame,
          bytes,
          (enum PixelFormat) format,
          width,
          height);

    }
    catch (std::bad_alloc &e)
    {
      VS_REF_RELEASE(retval);
      throw e;
    }
    catch (std::exception& e)
    {
      VS_LOG_DEBUG("error: %s", e.what());
      VS_REF_RELEASE(retval);
    }
    return retval;
  }

  void
  VideoPicture :: setData(com::xuggle::ferry::IBuffer* buffer)
  {
    if (!buffer) return;
    /** Use the buffer */
    mBuffer.reset(buffer, true);
  }
  
  bool
  VideoPicture :: copy(IVideoPicture * srcFrame)
  {
    bool result = false;
    try
    {
      if (!srcFrame)
        throw std::runtime_error("empty source frame to copy");

      if (!srcFrame->isComplete())
        throw std::runtime_error("source frame is not complete");

      VideoPicture* src = dynamic_cast<VideoPicture*>(srcFrame);
      if (!src)
        throw std::runtime_error("src frame is not of right subtype");

      // now copy the data
      allocInternalFrameBuffer();

      // get the raw buffers
      unsigned char* srcBuffer = (unsigned char*)src->mBuffer->getBytes(0, src->getSize());
      unsigned char* dstBuffer = (unsigned char*)mBuffer->getBytes(0, getSize());
      if (!srcBuffer || !dstBuffer)
        throw std::runtime_error("could not get buffer to copy");
      memcpy(dstBuffer, srcBuffer, getSize());

      this->setComplete(true,
          srcFrame->getPixelType(),
          srcFrame->getWidth(),
          srcFrame->getHeight(),
          srcFrame->getPts());
      result = true;
    }
    catch (std::exception & e)
    {
      VS_LOG_DEBUG("error: %s", e.what());
      result = false;
    }
    return result;
  }

  com::xuggle::ferry::IBuffer*
  VideoPicture :: getData()
  {
    com::xuggle::ferry::IBuffer *retval = 0;
    try {
      if (getSize() > 0) {
        if (!mBuffer || mBuffer->getBufferSize() < getSize())
        {
          allocInternalFrameBuffer();
        }
        retval = mBuffer.get();
        if (!retval) {
          throw std::bad_alloc();
        }
      }
    } catch (std::bad_alloc &e) {
      VS_REF_RELEASE(retval);
      throw e;
    } catch (std::exception & e)
    {
      VS_LOG_DEBUG("Error: %s", e.what());
      VS_REF_RELEASE(retval);
    }
    return retval;
  }

  void
  VideoPicture :: fillAVFrame(AVFrame *frame)
  {
    if (!mBuffer || mBuffer->getBufferSize() < getSize())
      allocInternalFrameBuffer();
    unsigned char* buffer = (unsigned char*)mBuffer->getBytes(0, getSize());
    // This is an inherently unsafe operation; it copies over all the bits in the AVFrame
    memcpy(frame, mFrame, sizeof(AVFrame));
    //*frame = *mFrame;
    // and then relies on avpicture_fill to overwrite any areas in frame that
    // are pointed to the wrong place.
    avpicture_fill((AVPicture*)frame, buffer, (enum PixelFormat) frame->format,
        frame->width, frame->height);
    frame->quality = getQuality();
    frame->type = FF_BUFFER_TYPE_USER;
  }

  void
  VideoPicture :: copyAVFrame(AVFrame* frame, IPixelFormat::Type pixel,
      int32_t width, int32_t height)
  {
    try
    {
      // Need to copy the contents of frame->data to our
      // internal buffer.
      VS_ASSERT(frame, "no frame?");
      VS_ASSERT(frame->data[0], "no data in frame");
      // resize the frame to the AVFrame
      mFrame->width = width;
      mFrame->height = height;
      mFrame->format = (int)pixel;

      int bufSize = getSize();
      if (bufSize <= 0)
        throw std::runtime_error("invalid size for frame");

      if (!mBuffer || mBuffer->getBufferSize() < bufSize)
        // reuse buffers if we can.
        allocInternalFrameBuffer();

      uint8_t* buffer = (uint8_t*)mBuffer->getBytes(0, bufSize);
      if (!buffer)
        throw std::runtime_error("really?  no buffer");

      if (frame->data[0])
      {
        // Make sure the frame isn't already using our buffer
        if(buffer != frame->data[0])
        {
          avpicture_fill((AVPicture*)mFrame, buffer,
              (enum PixelFormat) pixel, width, height);
          av_picture_copy((AVPicture*)mFrame, (AVPicture*)frame,
              (PixelFormat)frame->format, frame->width, frame->height);
        }
        mFrame->key_frame = frame->key_frame;
      }
      else
      {
        throw std::runtime_error("no data in frame to copy");
      }
    }
    catch (std::exception & e)
    {
      VS_LOG_DEBUG("error: %s", e.what());
    }
  }

  AVFrame*
  VideoPicture :: getAVFrame()
  {
    if (!mBuffer || mBuffer->getBufferSize() < getSize())
    {
      // reuse buffers if we can.
      allocInternalFrameBuffer();
    }
    return mFrame;
  }
  
  int
  VideoPicture :: getDataLineSize(int lineNo)
  {
    int retval = -1;
    if (getAVFrame()
        && lineNo >= 0
        && (unsigned int) lineNo < (sizeof(mFrame->linesize)/sizeof(mFrame->linesize[0])))
      retval = mFrame->linesize[lineNo];
    return retval;
  }

  bool
  VideoPicture :: isKeyFrame()
  {
    return (mFrame ? mFrame->key_frame : false);
  }

  void
  VideoPicture :: setKeyFrame(bool aIsKey)
  {
    if (mFrame)
      mFrame->key_frame = aIsKey;
  }
  
  int64_t
  VideoPicture :: getPts()
  {
    return (mFrame ? mFrame->pts : -1);
  }

  void
  VideoPicture :: setPts(int64_t value)
  {
    if (mFrame)
      mFrame->pts = value;
  }

  int
  VideoPicture :: getQuality()
  {
    return (mFrame ? mFrame->quality : FF_LAMBDA_MAX);
  }

  void
  VideoPicture :: setQuality(int newQuality)
  {
    if (newQuality < 0 || newQuality > FF_LAMBDA_MAX)
      newQuality = FF_LAMBDA_MAX;
    if (mFrame)
      mFrame->quality = newQuality;
  }

  void
  VideoPicture :: setComplete(
      bool aIsComplete,
      IPixelFormat::Type format,
      int width,
      int height,
      int64_t pts
  )
  {
    try {
      mIsComplete = aIsComplete;

      if (mIsComplete)
      {
        setPts(pts);
      }

      if (!mFrame)
        throw std::runtime_error("no AVFrame allocated");
      if (format != IPixelFormat::NONE && mFrame->format != (int)IPixelFormat::NONE && (int)format != mFrame->format)
        throw std::runtime_error("pixel formats don't match");
      if (width > 0 && mFrame->width >0 && width != mFrame->width)
        throw std::runtime_error("width does not match");
      if (height > 0 && mFrame->height > 0 && height != mFrame->height)
        throw std::runtime_error("height does not match");
    }
    catch (std::exception& e)
    {
      VS_LOG_DEBUG("error: %s", e.what());
    }
  }

  int32_t
  VideoPicture :: getSize()
  {
    int retval = -1;
    if (mFrame->width > 0 && mFrame->height > 0)
      retval = avpicture_get_size((PixelFormat)mFrame->format, mFrame->width, mFrame->height);
    return retval;
  }

  void
  VideoPicture :: allocInternalFrameBuffer()
  {
    int bufSize = getSize();
    if (bufSize <= 0)
      throw std::runtime_error("invalid size for frame");

    // reuse buffers if we can.
    if (!mBuffer || mBuffer->getBufferSize() < bufSize)
    {
      // Now, it turns out some accelerated assembly functions will
      // read at least a word past the end of an image buffer, so
      // we make space for that to happen.
      // I arbitrarily choose the sizeof a long-long (64 bit).

      // note that if the user has passed in their own buffer that is
      // the right 'size' but doesnt have this padding, we let it through anyway.
      int extraBytes=sizeof(int64_t);

      // Make our copy buffer.
      mBuffer = com::xuggle::ferry::IBuffer::make(this, bufSize+extraBytes);
      if (!mBuffer) {
        throw std::bad_alloc();
      }

      // Now, to further work around issues, I added the extra 8-bytes,
      // and now I'm going to zero just those 8-bytes out.  I don't
      // zero-out the whole buffer because I want Valgrind to detect
      // if it's not written to first.  But I know this overrun
      // issue exists in the MMX conversions in SWScale for some libraries,
      // so I'm going to fake it out here.
      {
        unsigned char * buf =
          ((unsigned char*)mBuffer->getBytes(0, bufSize+extraBytes));

        memset(buf+bufSize, 0, extraBytes);
      }
    }
    uint8_t* buffer = (uint8_t*)mBuffer->getBytes(0, bufSize);
    if (!buffer)
      throw std::bad_alloc();

    int imageSize = avpicture_fill((AVPicture*)mFrame,
        buffer,
        (enum PixelFormat) mFrame->format,
        mFrame->width,
        mFrame->height);
    if (imageSize != bufSize)
      throw std::runtime_error("could not fill picture");

    mFrame->type = FF_BUFFER_TYPE_USER;
    VS_ASSERT(mFrame->data[0] != 0, "Empty buffer");
  }

  IVideoPicture::PictType
  VideoPicture :: getPictureType()
  {
    IVideoPicture::PictType retval = IVideoPicture::DEFAULT_TYPE;
    if (mFrame)
      retval = (PictType) mFrame->pict_type;
    return retval;
  }
  
  void
  VideoPicture :: setPictureType(IVideoPicture::PictType type)
  {
    if (mFrame)
      mFrame->pict_type = (enum AVPictureType) type;
  }
}}}
