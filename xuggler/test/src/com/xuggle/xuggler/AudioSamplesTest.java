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
import com.xuggle.xuggler.Global;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;

import junit.framework.TestCase;

public class AudioSamplesTest extends TestCase
{
  private final Logger log = LoggerFactory.getLogger(this.getClass());

  @Before
  public void setUp()
  {
    log.debug("Executing test case: {}", this.getName());
  }
  
  @Test
  public void testReadingSamples()
  {
    Helper h = new Helper();
    
    h.setupReadingObject(h.sampleFile);
    
    int retval = -1;
    int audioStream = -1;
    int totalSamples = 0;
    for (int i = 0; i < h.mContainer.getNumStreams(); i++)
    {
      if (h.mCoders[i].getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO)
      {
        audioStream = i;
        // open our stream coder
        retval = h.mCoders[i].open(null, null);
        assertTrue("Could not open decoder", retval >=0);

        assertTrue("unexpected samples inbuffer",
            h.mSamples[i].getNumSamples() == 0);
        break;
      }
    }
    assertTrue("Could not find audio stream", audioStream >= 0);
    
    while (h.mContainer.readNextPacket(h.mPacket) == 0)
    {
      if (h.mPacket.getStreamIndex() == audioStream)
      {
        int offset = 0;
        while (offset < h.mPacket.getSize())
        {
          retval = h.mCoders[audioStream].decodeAudio(
              h.mSamples[audioStream],
              h.mPacket,
              offset);
          assertTrue("could not decode any audio", retval >0);
          offset += retval;
          assertTrue("did not write any samples",
              h.mSamples[audioStream].getNumSamples() > 0);
          log.debug("Decoded {} samples",
              h.mSamples[audioStream].getNumSamples());
          totalSamples += h.mSamples[audioStream].getNumSamples();
        }
      } else {
        log.debug("skipping video packet");
      }
    }
    h.mCoders[audioStream].close();
    log.debug("Total audio samples: {}", totalSamples);
    assertTrue("didn't get any audio", totalSamples > 0);
    // this will change if you change the file.
    assertTrue("unexpected # of samples", totalSamples == 3291264);
  }
  
  @Test
  public void testReadingSamplesIntoIBuffer()
  {
    IBuffer buffer = IBuffer.make(null, 192000*2);
    Helper h = new Helper();
    
    h.setupReadingObject(h.sampleFile);
    
    int retval = -1;
    int audioStream = -1;
    int totalSamples = 0;
    for (int i = 0; i < h.mContainer.getNumStreams(); i++)
    {
      if (h.mCoders[i].getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO)
      {
        audioStream = i;
        // open our stream coder
        retval = h.mCoders[i].open(null, null);
        assertTrue("Could not open decoder", retval >=0);

        assertTrue("unexpected samples inbuffer",
            h.mSamples[i].getNumSamples() == 0);
        break;
      }
    }
    assertTrue("Could not find audio stream", audioStream >= 0);
    
    IAudioSamples samples = IAudioSamples.make(buffer,
        h.mCoders[audioStream].getChannels(),
        h.mCoders[audioStream].getSampleFormat());
    assertNotNull(samples);
    while (h.mContainer.readNextPacket(h.mPacket) == 0)
    {
      if (h.mPacket.getStreamIndex() == audioStream)
      {
        int offset = 0;
        while (offset < h.mPacket.getSize())
        {
          retval = h.mCoders[audioStream].decodeAudio(
              samples,
              h.mPacket,
              offset);
          assertTrue("could not decode any audio", retval >0);
          offset += retval;
          assertTrue("did not write any samples",
              samples.getNumSamples() > 0);
          log.debug("Decoded {} samples",
              samples.getNumSamples());
          totalSamples += samples.getNumSamples();
        }
      } else {
        log.debug("skipping video packet");
      }
    }
    h.mCoders[audioStream].close();
    log.debug("Total audio samples: {}", totalSamples);
    assertTrue("didn't get any audio", totalSamples > 0);
    // this will change if you change the file.
    assertTrue("unexpected # of samples", totalSamples == 3291264);
  }
  

  @Test
  public void testGetNextPts()
  {
    int sampleRate = 440;
    int channels=1;
    
    IAudioSamples samples = IAudioSamples.make(sampleRate, 1);
    assertNotNull(samples);

    samples.setComplete(true, sampleRate, sampleRate, channels, IAudioSamples.Format.FMT_S16, 0);
    assertTrue(samples.isComplete());
    assertEquals(0, samples.getPts());
    assertEquals(Global.DEFAULT_PTS_PER_SECOND, samples.getNextPts());
  }
  
  @Test
  public void testSetPts()
  {
    int sampleRate = 440;
    int channels=1;
    
    IAudioSamples samples = IAudioSamples.make(sampleRate, 1);
    assertNotNull(samples);

    samples.setComplete(true, sampleRate, sampleRate, channels, IAudioSamples.Format.FMT_S16, 0);
    assertTrue(samples.isComplete());
    assertEquals(0, samples.getPts());
    assertEquals(Global.DEFAULT_PTS_PER_SECOND, samples.getNextPts());
    
    samples.setPts(Global.DEFAULT_PTS_PER_SECOND);
    assertEquals(Global.DEFAULT_PTS_PER_SECOND, samples.getPts());
    assertEquals(2*Global.DEFAULT_PTS_PER_SECOND, samples.getNextPts());
    
    
  }

  @Test
  public void testGetBufferType()
  {
    int sampleRate = 440;
    int channels=1;
    
    IAudioSamples samples = IAudioSamples.make(sampleRate, 1);
    assertNotNull(samples);

    samples.setComplete(true, sampleRate, sampleRate,
        channels, IAudioSamples.Format.FMT_S16, 0);
    assertTrue(samples.isComplete());
    
    IBuffer buffer;
    buffer = samples.getData();
    assertEquals(IBuffer.Type.IBUFFER_SINT16, buffer.getType());

    samples.setComplete(true, sampleRate, sampleRate,
        channels, IAudioSamples.Format.FMT_S32, 0);
    assertTrue(samples.isComplete());
    buffer = samples.getData();
    assertEquals(IBuffer.Type.IBUFFER_SINT32, buffer.getType());
 }

}
