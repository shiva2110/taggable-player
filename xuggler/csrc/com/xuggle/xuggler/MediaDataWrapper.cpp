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
#include <com/xuggle/xuggler/Global.h>
#include <com/xuggle/xuggler/MediaDataWrapper.h>

namespace com { namespace xuggle { namespace xuggler {

using namespace com::xuggle::ferry;

VS_LOG_SETUP(VS_CPP_PACKAGE);

MediaDataWrapper :: MediaDataWrapper()
{
  mTimeStamp = Global::NO_PTS;
  mIsKey = true;
}

MediaDataWrapper :: ~MediaDataWrapper()
{
  mTimeBase = 0;
}

MediaDataWrapper*
MediaDataWrapper :: make(IMediaData* obj)
{
  MediaDataWrapper* retval = make();
  if (retval)
  {
    retval->wrap(obj);
  }
  return retval;
}

int64_t
MediaDataWrapper :: getTimeStamp()
{
  return mTimeStamp;
}

void
MediaDataWrapper :: setTimeStamp(int64_t aTimeStamp)
{
  mTimeStamp = aTimeStamp;
}

IRational*
MediaDataWrapper :: getTimeBase()
{
  return mTimeBase.get();
}

void
MediaDataWrapper :: setTimeBase(IRational *aBase)
{
  mTimeBase.reset(aBase, true);
}

IBuffer*
MediaDataWrapper :: getData()
{
  IBuffer* retval=0;
  // forward to wrapped object
  if (mWrapped)
  {
    retval = mWrapped->getData();
  }
  return retval;
}

int32_t
MediaDataWrapper :: getSize()
{
  int retval = -1;
  // forward to wrapped object
  if (mWrapped)
  {
    retval = mWrapped->getSize();
  }
  return retval;
}

bool
MediaDataWrapper :: isKey()
{
  return mIsKey;
}

void
MediaDataWrapper :: setKey(bool aIsKey)
{
  mIsKey = aIsKey;
}

void
MediaDataWrapper :: wrap(IMediaData*aObj)
{
  IMediaDataWrapper* wrapper=dynamic_cast<IMediaDataWrapper*>(aObj);
  // loop detection to make sure we're not wrapping ourselves.
  if (wrapper) {
    RefPointer<IMediaData> obj;
    IMediaDataWrapper *me = static_cast<IMediaDataWrapper*>(this);
    do
    {
      if (wrapper == me)
        break;
      obj = wrapper->get();
    } while (0 != (wrapper = dynamic_cast<IMediaDataWrapper*>(obj.value())));
    
    if (wrapper == me)
    {
      VS_LOG_ERROR("Attempted to wrap an object that ultimately wraps itself.  Ignoring");
      // we're wrapping ourselves
      return;
    }
  }
  mWrapped.reset(aObj, true);
  if (aObj)
  {
    setTimeStamp(aObj->getTimeStamp());
    IRational *base = aObj->getTimeBase();
    setTimeBase(base);
    VS_REF_RELEASE(base);
    setKey(aObj->isKey());
  }
  else
  {
    setTimeStamp(Global::NO_PTS);
    setTimeBase(0);
    setKey(true);
  }
}

void
MediaDataWrapper :: setData(com::xuggle::ferry::IBuffer *buffer)
{
  if (mWrapped)
    mWrapped->setData(buffer);
}

IMediaData*
MediaDataWrapper :: get()
{
  return mWrapped.get();
}

IMediaData*
MediaDataWrapper :: unwrap()
{
  RefPointer<IMediaData> unwrapped = get();
  
  // as long unwrapped points to an IMediaDataWrapper, then unwrap
  while(unwrapped && dynamic_cast<IMediaDataWrapper*>(unwrapped.value()))
    unwrapped = (dynamic_cast<IMediaDataWrapper*>(unwrapped.value()))->get();
  return unwrapped.get();
}
}}}
