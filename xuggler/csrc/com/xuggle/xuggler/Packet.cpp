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

#include <com/xuggle/ferry/Logger.h>
#include <com/xuggle/ferry/Buffer.h>
#include <com/xuggle/xuggler/Packet.h>

// for memset
#include <cstring>
#include <stdexcept>

VS_LOG_SETUP(VS_CPP_PACKAGE);

namespace com { namespace xuggle { namespace xuggler
  {

  using namespace com::xuggle::ferry;
  
  Packet :: Packet()
  {
    mPacket = (AVPacket*)av_malloc(sizeof(AVPacket));
    if (!mPacket)
      throw std::bad_alloc();
    
    if (mPacket)
    {
      // initialize because ffmpeg doesn't
      av_init_packet(mPacket);
      mPacket->data = 0;
      mPacket->size = 0;
    }
    mIsComplete = false;
  }

  Packet :: ~Packet()
  {
    if (mPacket) {
      reset();
      av_free(mPacket);
    }
    mPacket = 0;
  }

  int64_t
  Packet :: getPts()
  {
    return (mPacket ? mPacket->pts : (int64_t)-1);
  }
  
  void
  Packet :: setPts(int64_t aPts)
  {
    if (mPacket) mPacket->pts = aPts;
  }
  
  int64_t
  Packet :: getDts()
  {
    return (mPacket ? mPacket->dts : (int64_t)-1);
  }
  
  void
  Packet :: setDts(int64_t aDts)
  {
    if (mPacket) mPacket->dts = aDts;
  }
  
  int32_t
  Packet :: getSize()
  {
    return (mPacket ? mPacket->size: (int32_t)-1);
  }
  int32_t
  Packet :: getMaxSize()
  {
    return (mBuffer ? mBuffer->getBufferSize() : -1);
  }
  int32_t
  Packet :: getStreamIndex()
  {
    return (mPacket ? mPacket->stream_index: (int32_t)-1);
  }
  int32_t
  Packet :: getFlags()
  {
    return (mPacket ? mPacket->flags: (int32_t)-1);
  }
  bool
  Packet :: isKeyPacket()
  {
    return (mPacket ? mPacket->flags & AV_PKT_FLAG_KEY : false);
  }

  void
  Packet :: setKeyPacket(bool bKeyPacket)
  {
    if (mPacket)
    {
      if (bKeyPacket)
        mPacket->flags |= AV_PKT_FLAG_KEY;
      else
        mPacket->flags = 0;
    }
  }

  void
  Packet :: setFlags(int32_t flags)
  {
    if (mPacket)
      mPacket->flags = flags;
  }

  void
  Packet :: setComplete(bool complete, int32_t size)
  {
    mIsComplete = complete;
    if (mIsComplete)
    {
      if (mPacket)
        mPacket->size = size;
    }
  }
  
  void
  Packet :: setStreamIndex(int32_t streamIndex)
  {
    if (mPacket)
      mPacket->stream_index = streamIndex;
  }
  int64_t
  Packet :: getDuration()
  {
    return (mPacket ? mPacket->duration: (int64_t)-1);
  }
  
  void
  Packet :: setDuration(int64_t duration)
  {
    if (mPacket) mPacket->duration = duration;
  }
  
  int64_t
  Packet :: getPosition()
  {
    return (mPacket ? mPacket->pos: (int64_t)-1);
  }
  
  void
  Packet :: setPosition(int64_t position)
  {
    if (mPacket) mPacket->pos = position;
  }
  
  com::xuggle::ferry::IBuffer *
  Packet :: getData()
  {
    return mBuffer.get();
  }
  
  void
  Packet :: wrapAVPacket(AVPacket* pkt)
  {
    // WE ALWAYS COPY the data; Let Ffmpeg do what it wants
    // to with it's own memory.
    VS_ASSERT(mPacket, "No packet?");
    
    // Make sure we have a buffer at least as large as this packet
    // This overwrites data, which we'll patch below.
    (void) this->allocateNewPayload(pkt->size);

    // Keep a copy of this, because we're going to nuke
    // it temporarily.
    uint8_t* data_buf = mPacket->data;
    void (*orig_destruct)(struct AVPacket *) = mPacket->destruct;
    
    // copy all data members, including data and size,
    // but we'll overwrite those next.
    *mPacket = *pkt;
    // Reset the data buf.
    mPacket->data = data_buf;
    mPacket->destruct = orig_destruct;
    mPacket->size = pkt->size;

    // Copy the contents of the new packet into data.
    if (pkt->data && pkt->size)
      memcpy(mPacket->data, pkt->data, pkt->size);
    
    // And assume we're now complete.
    setComplete(true, mPacket->size);
  }

  void
  Packet :: reset()
  {
    if (mPacket) {
      av_free_packet(mPacket);
      av_init_packet(mPacket);
    }
    setComplete(false, 0);
    // Don't reset the buffer though; we can potentially reuse it.
  }

  Packet*
  Packet :: make (int32_t payloadSize)
  {
    Packet *retval= 0;
    try {
      retval = Packet::make();
      if (retval->allocateNewPayload(payloadSize) < 0)
      {
        throw std::bad_alloc();
      }
    }
    catch (std::bad_alloc & e)
    {
      VS_REF_RELEASE(retval);
      throw e;
    }

    return retval;
  }
  
  Packet*
  Packet :: make (com::xuggle::ferry::IBuffer* buffer)
  {
    Packet *retval= 0;
    retval = Packet::make();
    if (retval)
    {
      retval->wrapBuffer(buffer);
    }
    return retval;
  }
  
  Packet*
  Packet :: make (Packet *packet, bool copyData)
  {
    Packet* retval=0;
    com::xuggle::ferry::IBuffer *buffer=0;
    IRational* timeBase = 0;
    try
    {
      if (!packet)
        throw std::runtime_error("need packet to copy");

      if (copyData)
      {
        int32_t numBytes = packet->getSize();
        retval = make(numBytes);
        if (!retval || !retval->mPacket || !retval->mPacket->data)
          throw std::bad_alloc();
        if (numBytes > 0 && packet->mPacket->data)
          memcpy(retval->mPacket->data, packet->mPacket->data,
              numBytes);
      } else {
        buffer=packet->getData();
        retval = make(buffer);
        if (!retval)
          throw std::bad_alloc();
      }
      // Keep a copy of this, because we're going to nuke
      // it temporarily.
      uint8_t* data_buf = retval->mPacket->data;
      void (*orig_destruct)(struct AVPacket *) = retval->mPacket->destruct;

      // copy all data members, including data and size,
      // but we'll overwrite those next.
      *(retval->mPacket) = *(packet->mPacket);

      retval->mPacket->data = data_buf;
      retval->mPacket->destruct = orig_destruct;

      // separate here to catch addRef()
      timeBase = packet->getTimeBase();
      retval->setTimeBase(timeBase);

      retval->setComplete(retval->mPacket->size > 0,
          retval->mPacket->size);
    }
    catch (std::exception &e)
    {
      VS_REF_RELEASE(retval);
    }
    VS_REF_RELEASE(buffer);
    VS_REF_RELEASE(timeBase);
    
    return retval;
  }
  
  int32_t
  Packet :: allocateNewPayload(int32_t payloadSize)
  {
    int32_t retval = -1;
    reset();
    uint8_t* payload = 0;

    // Some FFMPEG encoders will read past the end of a
    // buffer, so you need to allocate extra; yuck.
    if (!mBuffer || mBuffer->getBufferSize() < payloadSize)
    {
      // buffer isn't big enough; we need to make a new one.
      payload = (uint8_t*) av_malloc(payloadSize+FF_INPUT_BUFFER_PADDING_SIZE);
      if (!payload)
        throw std::bad_alloc();
      
      // we don't use the JVM for packets because Ffmpeg is REAL squirly about that
      mBuffer = Buffer::make(0, payload,
          payloadSize,
          Packet::freeAVBuffer, 0);
      if (!mBuffer) {
        av_free(payload);
        throw std::bad_alloc();
      }
      // and memset the padding area.
      memset(payload + payloadSize,
          0,
          FF_INPUT_BUFFER_PADDING_SIZE);
    } else {
      payload = (uint8_t*)mBuffer->getBytes(0, payloadSize);
    }
    VS_ASSERT(mPacket, "Should already have a packet");
    VS_ASSERT(mBuffer, "Should have allocated a buffer");
    VS_ASSERT(payload, "Should have allocated a payload");
    if (mBuffer && mPacket)
    {
      mPacket->data = payload;

      // And start out at zero.
      mPacket->size = 0;
      this->setComplete(false, 0);

      retval = 0;
    }
    return retval;
  }

  void
  Packet :: setData(IBuffer* buffer)
  {
    wrapBuffer(buffer);
  }
  
  void
  Packet :: wrapBuffer(IBuffer *buffer)
  {
    if (buffer != mBuffer.value())
    {
      reset();
      // and acquire this buffer.
      mBuffer.reset(buffer, true);
    }
    if (mBuffer)
    {
      // and patch up our AVPacket
      VS_ASSERT(mPacket, "No AVPacket");
      if (mPacket)
      {
        mPacket->size = mBuffer->getBufferSize();
        mPacket->data = (uint8_t*)mBuffer->getBytes(0, mPacket->size);
        // And assume we're now complete.
        setComplete(true, mPacket->size);
      }
    }
  }
  bool
  Packet :: isComplete()
  {
    return mIsComplete && mPacket->data;
  }
  
  void
  Packet :: freeAVBuffer(void * buf, void * closure)
  {
    // We know that FFMPEG allocated this with av_malloc, but
    // that might change in future versions; so this is
    // inherently somewhat dangerous.
    (void) closure;
    av_free(buf);
  }

  int64_t
  Packet :: getConvergenceDuration()
  {
    return (mPacket ? mPacket->convergence_duration : -1);
  }
  
  void
  Packet :: setConvergenceDuration(int64_t duration)
  {
    if (mPacket) mPacket->convergence_duration = duration;
  }
  }}}
