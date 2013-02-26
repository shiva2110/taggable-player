/**
 * 
 */
package com.xuggle.xuggler;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

import com.xuggle.xuggler.IStreamCoder.Direction;

/**
 * @author aclarke
 *
 */
public class ConfigurationTest
{

  /**
   * Test method for {@link Configuration#configure(Properties, IConfigurable)}.
   */
  @Test
  public void testConfigure1() throws FileNotFoundException, IOException
  {
    final String TEST_FILE = "fixtures/"+this.getClass().getName()+".properties";

    Properties props = new Properties();
    props.load(new FileInputStream(TEST_FILE));
    IStreamCoder coder = IStreamCoder.make(Direction.ENCODING, ICodec.findEncodingCodec(ICodec.ID.CODEC_ID_MP3));
    final long defaultValue = coder.getPropertyAsLong("subq");
    assertEquals(""+defaultValue, coder.getPropertyAsString("subq"));
    Configuration.printOption(System.out, coder, coder.getPropertyMetaData("subq"));
    int retval = Configuration.configure(props, coder);
    assertTrue("should succeed", retval >= 0);
    assertEquals("7", coder.getPropertyAsString("subq"));
    assertNotSame(defaultValue, coder.getPropertyAsLong("subq"));
    Configuration.printOption(System.out, coder, coder.getPropertyMetaData("subq"));
  }

  @Test
  public void testConfigure2() throws FileNotFoundException, IOException
  {
    final String TEST_FILE = "fixtures/"+this.getClass().getName()+".properties";

    IStreamCoder coder = IStreamCoder.make(Direction.ENCODING, ICodec.findEncodingCodec(ICodec.ID.CODEC_ID_H264));
    final long defaultValue = coder.getPropertyAsLong("subq");
    assertEquals(""+defaultValue, coder.getPropertyAsString("subq"));
    Configuration.printOption(System.out, coder, coder.getPropertyMetaData("subq"));
    int retval = Configuration.configure(TEST_FILE, coder);
    assertTrue("should succeed", retval >= 0);
    assertEquals("7", coder.getPropertyAsString("subq"));
    assertNotSame(defaultValue, coder.getPropertyAsLong("subq"));
    Configuration.printOption(System.out, coder, coder.getPropertyMetaData("subq"));
  }


}
