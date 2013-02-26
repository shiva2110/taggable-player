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

#include <com/xuggle/ferry/RefPointer.h>
#include <com/xuggle/ferry/Logger.h>
#include <com/xuggle/ferry/IBuffer.h>
#include <com/xuggle/xuggler/IVideoPicture.h>
#include <com/xuggle/xuggler/IPixelFormat.h>
#include <com/xuggle/xuggler/PixelFormat.h>

VS_LOG_SETUP(VS_CPP_PACKAGE);

namespace com { namespace xuggle { namespace xuggler
{
  using namespace com::xuggle::ferry;
  
  IPixelFormat :: IPixelFormat()
  {
  
  }
  
  IPixelFormat :: ~IPixelFormat()
  {
    
  }
  
  unsigned char
  IPixelFormat :: getYUV420PPixel(IVideoPicture *frame, int x, int y, YUVColorComponent c)
  {
    unsigned char result = 0;
    
    int offset = getYUV420PPixelOffset(frame, x, y, c);
    RefPointer<IBuffer> buffer = frame->getData();
    unsigned char * bytes = (unsigned char*)buffer->getBytes(0, offset+1);
    
    if (!bytes)
      throw std::runtime_error("could not find bytes in frame");
    result = bytes[offset];
    return result;
  }

  void
  IPixelFormat :: setYUV420PPixel(IVideoPicture *frame, int x, int y, YUVColorComponent c, unsigned char value)
  {
    int offset = getYUV420PPixelOffset(frame, x, y, c);
    RefPointer<IBuffer> buffer = frame->getData();
    unsigned char * bytes = (unsigned char*)buffer->getBytes(0, offset+1);
    
    if (!bytes)
    {
      VS_LOG_DEBUG("Could not find buffer of length: %d", offset+1);
      throw std::runtime_error("could not find bytes in frame");
    }
    bytes[offset] = value;
  }

  int
  IPixelFormat :: getYUV420PPixelOffset(IVideoPicture *frame, int x, int y, YUVColorComponent c)
  {
    if (!frame)
      throw std::runtime_error("no frame");
    
    int width = frame->getWidth();
    if (x < 0 || x >= width)
      throw std::runtime_error("x value invalid for input frame");
    
    int height = frame->getHeight();
    if (y < 0 || y >= height)
      throw std::runtime_error("y value invalid for input frame");
    
    if (frame->getPixelType() != YUV420P)
      throw std::runtime_error("pixel type of input frame is incorrect");

    int offset = PixelFormat::getFastYUV420PPixelOffset(frame->getWidth(), frame->getHeight(), x, y, c);
    VS_LOG_TRACE("w: %d; h: %d; x: %d; y: %d; c: %d; offset: %d",
        frame->getWidth(), frame->getHeight(), x, y, c, offset);
    return offset;
    
  }

}}}
