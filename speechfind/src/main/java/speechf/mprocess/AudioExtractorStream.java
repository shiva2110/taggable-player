package speechf.mprocess;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class AudioExtractorStream extends InputStream{

	private AudioExtractor audioExtractor;
	
	List<Byte> buffer;
	int bufferIndex;
	public int totalBytesRead;
	boolean endOfFile = false;
	
	public AudioExtractorStream(AudioExtractor audioExtractor) {
		this.audioExtractor = audioExtractor;
	}
	
	@Override
	public int read() throws IOException {
	
		if(buffer==null || bufferIndex>=buffer.size()) {
			buffer = audioExtractor.readAudioBytes();
			if(buffer==null) {
				endOfFile = true;
				System.out.println("end of file");
				//audioExtractor.seekToStart();
				return -1;
			}
			//System.out.println("buffer is not null");
			bufferIndex=0;
		}
		
		//System.out.println("buffer is not null");
		byte b = buffer.get(bufferIndex);
		bufferIndex++;
		totalBytesRead++;
		
		// return internal 2's complement representation
		// for storing in case of negative byte in the data.
		// A negative number will be returned only in case of end of file.
		int i = (b & (0xff));
		return  i; 
	}
	
	

}
