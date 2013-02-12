package speechf.mprocess;


import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AudioExtractorTest {
	
	// different values for the input stream
	
	@Test(expected=IllegalArgumentException.class)	
	public void testInitWithNull() throws IOException{
		try {
			AudioExtractor audioExtractor = new AudioExtractor("");
			audioExtractor.readAudioBytes();
		} catch(Exception e){
			System.out.println("Test case AudioExtractorTest.testInitWithNull: " + e.getClass() + ", " +  e.getMessage());
			throw e;
		}
		
	}
	
	@Test
	public void testInitmp4() throws IOException{
		
		
		try {
			AudioExtractor audioExtractor = new AudioExtractor(AudioExtractorTest.class.getResource("/video.mp4").toString());
			audioExtractor.setResample(16000);
			AudioExtractorStream audioExtractStream = new AudioExtractorStream(audioExtractor);
			
			List<Integer> audioBytes = new ArrayList<Integer>();
			int b;
			while((b=audioExtractStream.read())!=-1){
				audioBytes.add(b);
			}
			
			byte[] arr = new byte[audioBytes.size()];
			for(int i=0; i<audioBytes.size(); i++){
				arr[i] = audioBytes.get(i).byteValue();
			}
			
			audioExtractor.playbackAudio(arr);
			audioExtractStream.close();
			System.out.println("done!");
		} catch(Exception e){
			System.out.println("Test case AudioExtractorTest.testInitmp4: " + e.getClass() + ", " + e.getMessage());
			e.printStackTrace();
			throw e;
		}	
	}
	

}
