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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.*;

import com.xuggle.ferry.JNIMemoryManager;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.ICodec.ID;

import junit.framework.TestCase;

public class ContainerFormatTest extends TestCase
{
  private final String sampleFile = "fixtures/testfile.flv";

  @Test
  public void testSetOutputFmt()
  {
    IContainerFormat fmt = IContainerFormat.make();
    
    fmt.setOutputFormat("flv", null, null);
    fmt.setOutputFormat("flv", sampleFile, null);
    fmt.setOutputFormat("flv", "file:"+sampleFile, null);

    
    fmt.setOutputFormat("NotAShortName", null, null);
    fmt.setOutputFormat("NotAShortName", "NotAURL", null);
    fmt.setOutputFormat("NotAShortName", "file:"+"NotAURL", null);
    fmt.setOutputFormat("NotAShortName", "NotAProtocol:"+"NotAURL", null);

    
    assertTrue("got to end of test without coredump.  woo hoo", true);
  }
  
  @Test
  public void testSetInputFmt()
  {
    IContainerFormat fmt = IContainerFormat.make();
    
    fmt.setInputFormat("flv");
    fmt.setInputFormat("mov");
    fmt.setInputFormat("NotAShortName");
    assertTrue("got to end of test without coredump.  woo hoo", true);
  }
  
  @Test
  public void testGetInputFlag()
  {
    IContainerFormat fmt = IContainerFormat.make();
    int retval = -1;
    int flags = fmt.getInputFlags();
    assertEquals("should be not set", flags, 0);
    retval = fmt.setInputFormat("s16be");
    assertTrue("should succeed", retval >= 0);
    boolean hasGenericIndex = fmt.getInputFlag(IContainerFormat.Flags.FLAG_GENERIC_INDEX);
    assertTrue("should have global header", hasGenericIndex);
  }
  
  @Test
  public void testGetOutputFlag()
  {
    IContainerFormat fmt = IContainerFormat.make();
    
    int retval = -1;
    int flags = fmt.getOutputFlags();
    assertEquals("should be not set", flags, 0);
    retval = fmt.setOutputFormat("mov", null, null);
    assertTrue("should succeed", retval >= 0);
    boolean hasGlobalHeader = fmt.getOutputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER);
    assertTrue("should have global header", hasGlobalHeader);
  }
  
  @Test
  public void testSetInputFlag()
  {
    IContainerFormat fmt = IContainerFormat.make();
    int retval = -1;
    int flags = fmt.getInputFlags();
    assertEquals("should be not set", flags, 0);
    retval = fmt.setInputFormat("s16be");
    assertTrue("should succeed", retval >= 0);
    boolean hasGlobalHeader = fmt.getInputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER);
    assertTrue("should not have global header", !hasGlobalHeader);
    fmt.setInputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER, true);
    hasGlobalHeader = fmt.getInputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER);
    assertTrue("should have global header", hasGlobalHeader);

    fmt.setInputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER, false);
    hasGlobalHeader = fmt.getInputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER);
    assertTrue("should not have global header", !hasGlobalHeader);
  }
  
  @Test
  public void testSetOutputFlag()
  {
    IContainerFormat fmt = IContainerFormat.make();
    
    int retval = -1;
    int flags = fmt.getOutputFlags();
    assertEquals("should be not set", flags, 0);
    retval = fmt.setOutputFormat("mov", null, null);
    assertTrue("should succeed", retval >= 0);
    boolean hasGlobalHeader = fmt.getOutputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER);
    assertTrue("should have global header", hasGlobalHeader);
    
    fmt.setOutputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER, false);
    hasGlobalHeader = fmt.getOutputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER);
    assertTrue("should not have global header", !hasGlobalHeader);

    fmt.setOutputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER, true);
    hasGlobalHeader = fmt.getOutputFlag(IContainerFormat.Flags.FLAG_GLOBALHEADER);
    assertTrue("should have global header", hasGlobalHeader);
    

  }
  
  @Test
  public void testGetOutputCodecsSupported()
  {
    IContainerFormat fmt = IContainerFormat.make();
    
    int retval = -1;
    int flags = fmt.getOutputFlags();
    assertEquals("should be not set", flags, 0);
    retval = fmt.setOutputFormat("mov", null, null);
    assertTrue("should succeed", retval >= 0);
    
    List<ICodec.ID> codecs = fmt.getOutputCodecsSupported();
    assertNotNull(codecs);
//    for(ICodec.ID id : codecs)
//      System.out.println("Codec: "+id);

    assertTrue("should get at least one codec", codecs.size() > 1);
    assertTrue("should have MP3",
        codecs.contains(ICodec.ID.CODEC_ID_MP3));
    assertTrue("Should contain H263",
        codecs.contains(ICodec.ID.CODEC_ID_H263));
  }
  
  @Test
  public void testGetInputFormats()
  {
    Collection<IContainerFormat> installed =
      IContainerFormat.getInstalledInputFormats();
    assertTrue(installed.size() > 0);
    for(IContainerFormat fmt : installed)
    {
      assertNotNull(fmt);
      assertTrue(fmt.isInput());
      assertTrue(fmt.getInputFormatShortName().length() > 0);
    }
  }
  @Test
  public void testGetOutputFormats()
  {
    Collection<IContainerFormat> installed =
      IContainerFormat.getInstalledOutputFormats();
    assertTrue(installed.size() > 0);
    for(IContainerFormat fmt : installed)
    {
      assertNotNull(fmt);
      assertTrue(fmt.isOutput());
      assertTrue(fmt.getOutputFormatShortName().length() > 0);
    }
  }
  
  @Test
  public void testEstablishOutputCodecId()
  {
    JNIMemoryManager.getMgr().flush();
    IContainerFormat fmt = IContainerFormat.make();
    fmt.setOutputFormat("flv", null, null);
    assertEquals(ICodec.ID.CODEC_ID_FLV1,
        fmt.establishOutputCodecId(ICodec.Type.CODEC_TYPE_VIDEO));
    assertEquals(ICodec.ID.CODEC_ID_MP3,
        fmt.establishOutputCodecId(ICodec.Type.CODEC_TYPE_AUDIO));
    
    fmt.setOutputFormat("mp4", null, null);
    assertEquals(ICodec.ID.CODEC_ID_H264,
        fmt.establishOutputCodecId(ICodec.Type.CODEC_TYPE_VIDEO));
    assertEquals(ICodec.ID.CODEC_ID_AAC,
        fmt.establishOutputCodecId(ICodec.Type.CODEC_TYPE_AUDIO));

    fmt.setOutputFormat("3gp", null, null);
    assertEquals(ICodec.ID.CODEC_ID_H263,
        fmt.establishOutputCodecId(ICodec.Type.CODEC_TYPE_VIDEO));
    assertEquals(ICodec.ID.CODEC_ID_AMR_NB,
        fmt.establishOutputCodecId(ICodec.Type.CODEC_TYPE_AUDIO));

    fmt.delete();
    assertEquals(0, JNIMemoryManager.getMgr().getNumPinnedObjects());
  }
  
  @Test
  public void testEstablishOutputCodecIdFailOnMismatchedArgs()
  {
    JNIMemoryManager.getMgr().flush();
    IContainerFormat fmt = IContainerFormat.make();
    fmt.setOutputFormat("flv", null, null);
    try {
      fmt.establishOutputCodecId(ICodec.Type.CODEC_TYPE_VIDEO,
          ICodec.ID.CODEC_ID_MP3);
      fail("should not get here");
    } catch (IllegalArgumentException e) {}
    fmt.delete();
    assertEquals(0, JNIMemoryManager.getMgr().getNumPinnedObjects());
  }
  @Test
  public void testEstablishOutputCodecIdFailOnInputFormat()
  {
    JNIMemoryManager.getMgr().flush();
    IContainerFormat fmt = IContainerFormat.make();
    fmt.setInputFormat("flv");
    try {
      fmt.establishOutputCodecId(ICodec.Type.CODEC_TYPE_VIDEO);
      fail("should not get here");
    } catch (IllegalArgumentException e) {}
    fmt.delete();
    assertEquals(0, JNIMemoryManager.getMgr().getNumPinnedObjects());
  }
  
  @Test
  public void testIssue200()
  {
    IContainerFormat format = IContainerFormat.make();
    format.setOutputFormat("flv", null, null);

    List<ID> codecs = format.getOutputCodecsSupported();
    // now let's make sure there are no dups
    Set<ID> uniqueCodecs = new HashSet<ID>();
    for(ID id : codecs) {
      assertFalse("id not unique: "+id, uniqueCodecs.contains(id));
      uniqueCodecs.add(id);
    }
  }
  /**
   * Make sure the first video codec returned for FLV is FLV1.
   */
  @Test
  public void testIssue201()
  {
    IContainerFormat format = IContainerFormat.make();
    format.setOutputFormat("flv", null, null);

    List<ID> codecs = format.getOutputCodecsSupported();
    for(ID id : codecs) {
      ICodec codec = ICodec.findEncodingCodec(id);
      assertNotNull(codec);
      if (codec.getType() == ICodec.Type.CODEC_TYPE_VIDEO) {
        assertEquals(ICodec.ID.CODEC_ID_FLV1, id);
        // and the test is now finished.
        break;
      }
    }
  }
  
  /**
   * This test is not really a test -- it's used to show all enums
   * and how they map to codecs
   */
  @Test
  public void testShowEnumsWithoutNativeCode()
  {
    ICodec.ID ids[] = ICodec.ID.values();
    for (ICodec.ID id : ids)
    {
      System.out.println("ID: " + id + "; Value: " + id.swigValue());
    }
  }


  
}
