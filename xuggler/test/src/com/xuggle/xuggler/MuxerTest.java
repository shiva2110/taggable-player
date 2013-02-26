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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xuggle.test_utils.NameAwareTestClassRunner;

@RunWith(NameAwareTestClassRunner.class)
public class MuxerTest
{
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private String mTestName;

  @Before
  public void setUp() throws Exception
  {
    mTestName = NameAwareTestClassRunner.getTestMethodName();
    log.debug("-----START----- {}", mTestName);
  }

  @After
  public void tearDown() throws Exception
  {
    log.debug("----- END ----- {}", mTestName);
  }

  @Test
  public void testMuxing()
  {
    final IContainer input = IContainer.make();
    
    final int numCopies=4;
    final IContainer[] outputs=new IContainer[numCopies];
    for(int i = 0; i < outputs.length; i++)
    {
      outputs[i] = IContainer.make();
      outputs[i].open(this.getClass().getName()+"_"+mTestName+"_"+i+".flv",
          IContainer.Type.WRITE,
          null);
    }
    
    // open the input file
    assertTrue("could not open file",
        input.open("fixtures/testfile.flv", IContainer.Type.READ,null) >= 0);
    int numStreams = input.getNumStreams();
    for(int i = 0; i < numStreams; i++)
    {
      final IStream stream = input.getStream(i);
      final IStreamCoder coder = stream.getStreamCoder();
      assertTrue(coder.open(null, null) >= 0);
      for(int j = 0; j < outputs.length; j++)
      {
        outputs[j].addNewStream(coder.getCodec());
        IStreamCoder newCoder = IStreamCoder.make(IStreamCoder.Direction.ENCODING, coder);
        assertTrue(outputs[j].getStream(i).setStreamCoder(newCoder) >= 0);
        assertTrue("could not open copied coder", newCoder.open(null, null) >= 0);
      }
    }
    // write the output headers
    for(int i = 0; i < outputs.length; i++)
    {
      assertTrue(outputs[i].writeHeader() >= 0);
    }
    int numPktsRead=0;
    int numPktsWritten=0;
    for(;;)
    {
      final IPacket pkt = IPacket.make();
      if (input.readNextPacket(pkt) < 0)
        break;
      ++numPktsRead;
      
      for(int i=0; i < outputs.length; i++)
      {
        assertTrue(outputs[i].writePacket(pkt, false) >= 0);
        ++numPktsWritten;
      }
      
    }
    assertEquals(numPktsRead, numPktsWritten/outputs.length);
    assertEquals(7950, numPktsRead);
    
    // write the output trailers
    for(int i = 0; i < outputs.length; i++)
    {
      assertTrue(outputs[i].writeTrailer() >= 0);
      int streams = outputs[i].getNumStreams();
      for(int j = 0; j < streams; j++)
      {
        assertTrue(outputs[i].getStream(j).getStreamCoder().close() >= 0);
      }
      assertTrue(outputs[i].close() >= 0);
    }
    
  }
}
