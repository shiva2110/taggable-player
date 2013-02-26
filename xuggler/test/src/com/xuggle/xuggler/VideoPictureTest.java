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

package com.xuggle.xuggler;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.ferry.IBuffer;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IPixelFormat;

import junit.framework.TestCase;

public class VideoPictureTest extends TestCase
{
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Before
  public void setUp()
  {
    log.debug("Executing test case: {}", this.getName());
  }
  
  @Test
  public void testFrameCreationFromBuffer()
  {
    int width=100;
    int height=100;
    IPixelFormat.Type format = IPixelFormat.Type.BGR24;
    IBuffer buffer = IBuffer.make(null, width*height*3);
    // put fake data in buffer
    byte[] bytes=new byte[1];
    for(int i = 0; i < buffer.getBufferSize(); i++)
    {
      bytes[0] = (byte)i; 
      buffer.put(bytes, 0, i, 1);
    }
    IVideoPicture picture = IVideoPicture.make(buffer, format, width, height);
    assertNotNull(picture);
    assertEquals(buffer.getBufferSize(), picture.getSize());
    
    for(int i = 0; i < picture.getSize(); i++)
    {
      picture.get(i, bytes, 0, 1);
      assertEquals((byte)i, bytes[0]);
    }
  }
  @Test
  public void testFrameCreation()
  {
    IVideoPicture frame = null;
    
    frame = IVideoPicture.make(IPixelFormat.Type.RGB24, 320, 240);
    assertTrue("could not get frame", frame != null);
    assertTrue("should default to keyframe", frame.isKeyFrame());
    assertTrue("should not be complete", !frame.isComplete());
    assertEquals("wrong width", frame.getWidth(), 320);
    assertEquals("wrong height", frame.getHeight(), 240);
    assertEquals("wrong format", frame.getPixelType(), IPixelFormat.Type.RGB24);

    frame.delete();
    frame = null;
  }
  @Test
  public void testReadingFrames()
  {
    Helper h = new Helper();
    
    // CHANGE THESE IF YOU CHANGE THE INPUT FILE
    int expectedFrames = 2236;
    int expectedKeyFrames = 270;

    h.setupReadingObject(h.sampleFile);
    
    int retval = -1;
    int videoStream = -1;
    int totalFrames = 0;
    int totalKeyFrames = 0;
    for (int i = 0; i < h.mContainer.getNumStreams(); i++)
    {
      if (h.mCoders[i].getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
      {
        videoStream = i;
        // open our stream coder
        retval = h.mCoders[i].open(null, null);
        assertTrue("Could not open decoder", retval >=0);
        assertTrue("Frame should not be complete",
            !h.mFrames[i].isComplete());
        break;
      }
    }
    assertTrue("Could not find video stream", videoStream >= 0);
    
    while (h.mContainer.readNextPacket(h.mPacket) == 0)
    {
      if (h.mPacket.getStreamIndex() == videoStream)
      {
        int offset = 0;
        while (offset < h.mPacket.getSize())
        {
          retval = h.mCoders[videoStream].decodeVideo(
              h.mFrames[videoStream],
              h.mPacket,
              offset);
          assertTrue("could not decode any video", retval >0);
          offset += retval;
          if (h.mFrames[videoStream].isComplete())
          {
            totalFrames ++;
            if (h.mFrames[videoStream].isKeyFrame())
              totalKeyFrames++;
          }
        }
      } else {
        log.debug("skipping audio packet");
      }
    }
    log.debug("Total frames: {}", totalFrames);
    log.debug("Total key frames: {}", totalKeyFrames);
    assertTrue("didn't get any frames", totalFrames > 0);
    assertTrue("didn't get any key frames", totalKeyFrames > 0);
    assertTrue("didn't get any non key frames", totalKeyFrames < totalFrames);

    // this will change if you change the file.
    assertTrue("unexpected # of frames", totalFrames == expectedFrames);
    assertTrue("unexpected # of key frames", totalKeyFrames == expectedKeyFrames);
    
    h.cleanupHelper();

  }
  
  @Test
  public void testGetPictureType()
  {
    IVideoPicture pict = Utils.getBlankFrame(10, 10, 0);
    IVideoPicture.PictType defaultType = IVideoPicture.PictType.DEFAULT_TYPE;
    IVideoPicture.PictType setType = IVideoPicture.PictType.S_TYPE;
    assertEquals(defaultType, pict.getPictureType());
    pict.setPictureType(setType);
    assertEquals(setType,pict.getPictureType());
  }

  @Test
  public void testReadingFramesIntoIBuffers()
  {
    IBuffer buffer = IBuffer.make(null, 424*176*3); // should be bigger than needed
    Helper h = new Helper();
    
    // CHANGE THESE IF YOU CHANGE THE INPUT FILE
    int expectedFrames = 2236;
    int expectedKeyFrames = 270;

    h.setupReadingObject(h.sampleFile);
    
    int retval = -1;
    int videoStream = -1;
    int totalFrames = 0;
    int totalKeyFrames = 0;
    for (int i = 0; i < h.mContainer.getNumStreams(); i++)
    {
      if (h.mCoders[i].getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
      {
        videoStream = i;
        // open our stream coder
        retval = h.mCoders[i].open(null, null);
        assertTrue("Could not open decoder", retval >=0);
        assertTrue("Frame should not be complete",
            !h.mFrames[i].isComplete());
        break;
      }
    }
    assertTrue("Could not find video stream", videoStream >= 0);
    
    IVideoPicture picture = IVideoPicture.make(buffer,
        h.mCoders[videoStream].getPixelType(),
        h.mCoders[videoStream].getWidth(),
        h.mCoders[videoStream].getHeight());
    assertNotNull(picture);
    while (h.mContainer.readNextPacket(h.mPacket) == 0)
    {
      if (h.mPacket.getStreamIndex() == videoStream)
      {
        int offset = 0;
        while (offset < h.mPacket.getSize())
        {
          retval = h.mCoders[videoStream].decodeVideo(
              picture,
              h.mPacket,
              offset);
          assertTrue("could not decode any video", retval >0);
          offset += retval;
          if (picture.isComplete())
          {
            totalFrames ++;
            if (picture.isKeyFrame())
              totalKeyFrames++;
          }
        }
      } else {
        log.debug("skipping audio packet");
      }
    }
    log.debug("Total frames: {}", totalFrames);
    log.debug("Total key frames: {}", totalKeyFrames);
    assertTrue("didn't get any frames", totalFrames > 0);
    assertTrue("didn't get any key frames", totalKeyFrames > 0);
    assertTrue("didn't get any non key frames", totalKeyFrames < totalFrames);

    // this will change if you change the file.
    assertTrue("unexpected # of frames", totalFrames == expectedFrames);
    assertTrue("unexpected # of key frames", totalKeyFrames == expectedKeyFrames);
    
    h.cleanupHelper();

  }

  @Test
  public void testGetDataLineSize()
  {
    final int WIDTH = 420;
    final int HEIGHT= 360;
    final IPixelFormat.Type TYPE = IPixelFormat.Type.YUV420P;
    IVideoPicture pic = IVideoPicture.make(TYPE, WIDTH, HEIGHT);
    pic.setComplete(true, TYPE, WIDTH, HEIGHT, 0);
    assertEquals(WIDTH, pic.getDataLineSize(0));
    assertEquals(WIDTH/2, pic.getDataLineSize(1));
    assertEquals(WIDTH/2, pic.getDataLineSize(2));
    assertEquals(0, pic.getDataLineSize(3));
    assertEquals(0, pic.getDataLineSize(4));
    assertEquals(0, pic.getDataLineSize(5));
    assertEquals(0, pic.getDataLineSize(6));
    assertEquals(0, pic.getDataLineSize(7));
    assertEquals(-1, pic.getDataLineSize(8));
    assertEquals(-1, pic.getDataLineSize(-1));
  }
  
  @Test
  public void testGetDataLineSizeFromIBuffer()
  {
    final int WIDTH = 420;
    final int HEIGHT= 360;
    final IPixelFormat.Type TYPE = IPixelFormat.Type.YUV420P;
    final IBuffer buf = IBuffer.make(null, (int) (WIDTH*HEIGHT*1.5));
    IVideoPicture pic = IVideoPicture.make(buf, TYPE, WIDTH, HEIGHT);
    pic.setComplete(true, TYPE, WIDTH, HEIGHT, 0);
    assertEquals(WIDTH, pic.getDataLineSize(0));
    assertEquals(WIDTH/2, pic.getDataLineSize(1));
    assertEquals(WIDTH/2, pic.getDataLineSize(2));
    assertEquals(0, pic.getDataLineSize(3));
    assertEquals(0, pic.getDataLineSize(4));
    assertEquals(0, pic.getDataLineSize(5));
    assertEquals(0, pic.getDataLineSize(6));
    assertEquals(0, pic.getDataLineSize(7));
    assertEquals(-1, pic.getDataLineSize(8));
    assertEquals(-1, pic.getDataLineSize(-1));
  }
}
