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

#include <cstring>
#include <stdexcept>

#include <com/xuggle/ferry/Logger.h>
#include <com/xuggle/ferry/RefPointer.h>

#include <com/xuggle/xuggler/Global.h>
#include <com/xuggle/xuggler/Stream.h>
#include <com/xuggle/xuggler/Rational.h>
#include <com/xuggle/xuggler/StreamCoder.h>
#include <com/xuggle/xuggler/Container.h>
#include <com/xuggle/xuggler/MetaData.h>
#include <com/xuggle/xuggler/Packet.h>
#include <com/xuggle/xuggler/IIndexEntry.h>

#include "FfmpegIncludes.h"

VS_LOG_SETUP(VS_CPP_PACKAGE);

namespace com { namespace xuggle { namespace xuggler
{

  Stream :: Stream()
  {
    mStream = 0;
    mDirection = INBOUND;
    mCoder = 0;
    mContainer = 0;
    mLastDts = Global::NO_PTS;
  }

  Stream :: ~Stream()
  {
    reset();
  }

  void
  Stream :: reset()
  {
    VS_ASSERT("should be a valid object", getCurrentRefCount() >= 1);
    mMetaData.reset();
    // As of recent (March 2011) builds of FFmpeg, Stream objects
    // are cleaned up by the new avformat_free_context method in the
    // Container, so the outbound check for freeing memory is no
    // longer required.
    VS_REF_RELEASE(mCoder);
    mStream = 0;
    // We don't keep a reference to the container to avoid a ref-count loop
    // and so we don't release.
    mContainer = 0;
  }
  Stream*
  Stream :: make(Container *container, AVStream * aStream, Direction direction, AVCodec* avCodec)
  {
    // note: make will acquire this for us.
    Stream *newStream = 0;
    if (aStream) {
      try {
        newStream = make();
        newStream->mStream = aStream;
        newStream->mDirection = direction;
  
        newStream->mCoder = StreamCoder::make(
              direction == INBOUND?IStreamCoder::DECODING : IStreamCoder::ENCODING,
              aStream->codec,
              avCodec,
              newStream);
        newStream->mContainer = container;
      } catch (std::bad_alloc & e) {
        VS_REF_RELEASE(newStream);
        throw e;
      }
    }
    return newStream;
  }

  int
  Stream :: getIndex()
  {
    return (mStream ? mStream->index : -1);
  }

  int
  Stream :: getId()
  {
    return (mStream ? mStream->id : -1);
  }

  IStreamCoder *
  Stream :: getStreamCoder()
  {
    StreamCoder *retval = 0;

    // acquire a reference for the caller
    retval = mCoder;
    VS_REF_ACQUIRE(retval);

    return retval;
  }

  IRational *
  Stream :: getFrameRate()
  {
    IRational * result = 0;
    if (mStream)
    {
      result = Rational::make(&mStream->r_frame_rate);
    }
    return result;
  }

  IRational *
  Stream :: getTimeBase()
  {
    IRational * result = 0;
    if (mStream)
    {
      result = Rational::make(&mStream->time_base);
    }
    return result;
  }
  void
  Stream :: setTimeBase(IRational* src)
  {
    if (mStream && src)
    {
      mStream->time_base.den = src->getDenominator();
      mStream->time_base.num = src->getNumerator();
    }
  }
  void
  Stream :: setFrameRate(IRational* src)
  {
    if (mStream && src)
    {
      mStream->r_frame_rate.den = src->getDenominator();
      mStream->r_frame_rate.num = src->getNumerator();
    }
  }

  int64_t
  Stream :: getStartTime()
  {
    return (mStream ? mStream->start_time : Global::NO_PTS);
  }

  int64_t
  Stream :: getDuration()
  {
    return (mStream ? mStream->duration : Global::NO_PTS);
  }

  int64_t
  Stream :: getCurrentDts()
  {
    return (mStream ? mStream->cur_dts : Global::NO_PTS);
  }

  int64_t
  Stream :: getNumFrames()
  {
    return (mStream ? mStream->nb_frames : 0);
  }
  int
  Stream :: getNumIndexEntries()
  {
    return (mStream ? mStream->nb_index_entries : 0);
  }
  int
  Stream :: containerClosed(Container *)
  {
    // let the coder know we're closed.
    if (mCoder)
      mCoder->streamClosed(this);
    reset();
    return 0;
  }

  int32_t
  Stream :: acquire()
  {
    int retval = 0;
    retval = RefCounted::acquire();
    VS_LOG_TRACE("Acquired %p: %d", this, retval);
    return retval;
  }

  int32_t
  Stream :: release()
  {
    int retval = 0;
    retval = RefCounted::release();
    VS_LOG_TRACE("Released %p: %d", this, retval);
    return retval;
  }

  IRational*
  Stream :: getSampleAspectRatio()
  {
    IRational* retval = 0;
    if (mStream)
    {
      retval = IRational::make(
          mStream->sample_aspect_ratio.num,
          mStream->sample_aspect_ratio.den);
    }
    return retval;
  }

  void
  Stream :: setSampleAspectRatio(IRational* aNewValue)
  {
    if (aNewValue && mStream)
    {
      mStream->sample_aspect_ratio.num =
        aNewValue->getNumerator();
      mStream->sample_aspect_ratio.den =
        aNewValue->getDenominator();
    }
    return;
  }

  const char*
  Stream :: getLanguage()
  {
    const char*retval = 0;
    com::xuggle::ferry::RefPointer<IMetaData> metaData = getMetaData();
    if (metaData) {
      retval = metaData->getValue("language", IMetaData::METADATA_NONE);
    }
    return retval;
  }

  void
  Stream :: setLanguage(const char* aNewValue)
  {
    com::xuggle::ferry::RefPointer<IMetaData> metaData = getMetaData();
    if (metaData) {
      metaData->setValue("language", aNewValue);
    }
    return;
  }

  IContainer*
  Stream :: getContainer()
  {
    // add ref for caller
    VS_REF_ACQUIRE(mContainer);
    return mContainer;
  }

  int32_t
  Stream :: setStreamCoder(IStreamCoder *coder)
  {
    return setStreamCoder(coder, true);
  }
  int32_t
  Stream :: setStreamCoder(IStreamCoder *aCoder, bool assumeOnlyStream)
  {
    int32_t retval = -1;
    try
    {
      if (mCoder && mCoder->isOpen())
        throw std::runtime_error("cannot call setStreamCoder when current coder is open");
      
      if (!aCoder)
        throw std::runtime_error("cannot set to a null stream coder");

      StreamCoder *coder = dynamic_cast<StreamCoder*>(aCoder);
      if (!coder)
        throw std::runtime_error("IStreamCoder is not of expected underlying C++ type");

      // Close the old stream coder
      if (mCoder)
      {
        mCoder->streamClosed(this);
      }

      if (coder->setStream(this, assumeOnlyStream) < 0)
        throw std::runtime_error("IStreamCoder doesn't like this stream");

      VS_REF_RELEASE(mCoder);
      mCoder = coder;
      VS_REF_ACQUIRE(mCoder);
      retval = 0;
    }
    catch (std::exception & e)
    {
      VS_LOG_ERROR("Error: %s", e.what());
      retval = -1;
    }
    return retval;
  }
  
  IStream::ParseType
  Stream :: getParseType()
  {
    if (mStream) {
      return (IStream::ParseType)mStream->need_parsing;
    } else {
      return IStream::PARSE_NONE;
    }
  }
  
  void
  Stream :: setParseType(IStream::ParseType type)
  {
    if (mStream) {
      mStream->need_parsing = (enum AVStreamParseType)type;
    }
  }
  
  IMetaData*
  Stream :: getMetaData()
  {
    if (!mMetaData && mStream)
    {
      if (mDirection == IStream::OUTBOUND)
        mMetaData = MetaData::make(&mStream->metadata);
      else
        // make a read-only copy so when libav deletes the
        // input version we don't delete our copy
        mMetaData = MetaData::make(mStream->metadata);
    }
    return mMetaData.get();
  }
  
  void
  Stream :: setMetaData(IMetaData * copy)
  {
    MetaData* data = dynamic_cast<MetaData*>(getMetaData());
    if (data) {
      data->copy(copy);
      // release for the get above
      data->release();
    }
    return;
  }

  int32_t
  Stream :: stampOutputPacket(IPacket* packet)
  {
    if (!packet)
      return -1;

//    VS_LOG_DEBUG("input:  duration: %lld; dts: %lld; pts: %lld;",
//        packet->getDuration(), packet->getDts(), packet->getPts());

    // Always just reset this; cheaper than checking if it's
    // already set
    packet->setStreamIndex(this->getIndex());
    
    com::xuggle::ferry::RefPointer<IRational> thisBase = getTimeBase();
    com::xuggle::ferry::RefPointer<IRational> packetBase = packet->getTimeBase();
    if (!thisBase || !packetBase)
      return -1;
    if (thisBase->compareTo(packetBase.value()) == 0) {
//      VS_LOG_DEBUG("Same timebase: %d/%d vs %d/%d",
//          thisBase->getNumerator(), thisBase->getDenominator(),
//          packetBase->getNumerator(), packetBase->getDenominator());
      // it's already got the right time values
      return 0;
    }
    
    int64_t duration = packet->getDuration();
    int64_t dts = packet->getDts();
    int64_t pts = packet->getPts();
    
    if (duration >= 0)
      duration = thisBase->rescale(duration, packetBase.value(),
          IRational::ROUND_DOWN);
    
    if (pts != Global::NO_PTS)
    {
      pts = thisBase->rescale(pts, packetBase.value(), IRational::ROUND_DOWN);
    }
    if (dts != Global::NO_PTS)
    {
      dts = thisBase->rescale(dts, packetBase.value(), IRational::ROUND_DOWN);
      if (mLastDts != Global::NO_PTS && dts == mLastDts)
      {
        // adjust for rounding; we never want to insert a frame that
        // is not monotonically increasing.  Note we only do this if
        // we're off by one; that's because we ROUND_DOWN and then assume
        // that can be off by at most one.  If we're off by more than one
        // then it's really an error on the person muxing to this stream.
        dts = mLastDts+1;
        // and round up pts
        if (pts != Global::NO_PTS)
          ++pts;
        // and if after all that adjusting, pts is less than dts
        // let dts win.
        if (pts == Global::NO_PTS || pts < dts)
          pts = dts;
      }
      mLastDts = dts;
    }
    
//    VS_LOG_DEBUG("output: duration: %lld; dts: %lld; pts: %lld;",
//        duration, dts, pts);
    packet->setDuration(duration);
    packet->setPts(pts);
    packet->setDts(dts);
    packet->setTimeBase(thisBase.value());
//    VS_LOG_DEBUG("Reset timebase: %d/%d",
//        thisBase->getNumerator(), thisBase->getDenominator());
    return 0;
  }

  IIndexEntry*
  Stream :: findTimeStampEntryInIndex(int64_t wantedTimeStamp, int32_t flags)
  {
    int32_t index = findTimeStampPositionInIndex(wantedTimeStamp, flags);
    // getIndexEntry will check for a negative index and return null if so
    return getIndexEntry(index);
  }
  int32_t
  Stream :: findTimeStampPositionInIndex(int64_t wantedTimeStamp,
      int32_t flags)
  {
    int retval = -1;
    if (mStream) {
      retval = av_index_search_timestamp(
          mStream,
          wantedTimeStamp,
          flags);
    }
    return retval;
  }

  IIndexEntry*
  Stream :: getIndexEntry(int32_t index)
  {
    IIndexEntry* retval = 0;
    if (mStream->index_entries
        && index >= 0
        && index < mStream->nb_index_entries)
    {
      AVIndexEntry* entry = &(mStream->index_entries[index]);
      if (entry) {
        retval = IIndexEntry::make(
            entry->pos,
            entry->timestamp,
            entry->flags,
            entry->size,
            entry->min_distance
        );
      }
    }
    return retval;
  }
  int32_t
  Stream :: addIndexEntry(IIndexEntry* entry)
  {
    if (!entry)
      return -1;
    if (!mStream)
      return -1;
    return av_add_index_entry(mStream,
        entry->getPosition(),
        entry->getTimeStamp(),
        entry->getSize(),
        entry->getMinDistance(),
        entry->getFlags());
  }

  void
  Stream :: setId(int32_t aId)
  {
    if (!mStream)
      return;
    mStream->id = aId;
  }
}}}
