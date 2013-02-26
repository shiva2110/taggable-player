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
#include <com/xuggle/xuggler/IBuffer.h>
#include <com/xuggle/xuggler/Buffer.h>
#include "BufferTest.h"

VS_LOG_SETUP(VS_CPP_PACKAGE);

using namespace VS_CPP_NAMESPACE;

void
BufferTest :: setUp()
{
  buffer = 0;
}

void
BufferTest :: tearDown()
{
  buffer = 0;
}

void
BufferTest :: testCreationAndDestruction()
{
  long bufSize = 10;
  buffer = Buffer::make(0, bufSize);
  VS_TUT_ENSURE("no buffer", buffer);

  VS_TUT_ENSURE_EQUALS("wrong size", buffer->getBufferSize(),
      bufSize);

}

void
BufferTest :: testReadingAndWriting()
{
  long bufSize = 10;
  buffer = Buffer::make(0, bufSize);
  VS_TUT_ENSURE("no buffer", buffer);
  int i = 0;
  unsigned char * buf = (unsigned char*)buffer->getBytes(0, bufSize);
  VS_TUT_ENSURE("no allocation", buf);

  for (i = 0; i < bufSize; i++)
  {
    // This tests that we can write to the buffer
    // area without a memory read error.
    // This test only potentially shows up as errors in a memory
    // tool like valgrind.
    buf[i] = i;
  }

  // now read; if we didn't initialize correctly, this
  // shows up as an error in valgrind
  for (i = 0; i < bufSize; i++)
  {
    // This tests that we can write to the buffer
    // area without a memory read error.
    // This test only potentially shows up as errors in a memory
    // tool like valgrind.
    VS_TUT_ENSURE_EQUALS("but we just wrote here?",
        buf[i],
        i);
  }    
}

void
BufferTest :: testWrapping()
{
  long bufSize = 10;
  unsigned char*raw = new unsigned char[bufSize];
  bool freeCalled = false;
  Buffer* wrappingBuffer=0;
  VS_TUT_ENSURE("could allocate buffer", raw);

  wrappingBuffer = Buffer::make(0, raw, bufSize, freeBuffer, &freeCalled);
  VS_TUT_ENSURE("no buffer", wrappingBuffer);

  // release the buffer
  VS_REF_RELEASE(wrappingBuffer);

  VS_TUT_ENSURE("closure not executed", freeCalled);   

  freeCalled = false;
  // This time no free buffer
  raw = new unsigned char[bufSize];

  wrappingBuffer = Buffer::make(0, raw, bufSize, 0, &freeCalled);
  VS_TUT_ENSURE("no buffer", wrappingBuffer);

  // release the buffer
  VS_REF_RELEASE(wrappingBuffer);

  VS_TUT_ENSURE("closure executed?", !freeCalled);

  // and clean up
  delete [] raw;

}
