package speechf.indexer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFileFormat.Type;

public class Test {

	public static void main(String[] args) throws MalformedURLException, UnsupportedAudioFileException, IOException{
		
		File file = new File("/Users/shiva2110/Downloads/male.wav");
		Type[] types = AudioSystem.getAudioFileTypes();

		for(Type type: types){
			System.out.println(type.toString());
		}
		AudioSystem.getAudioInputStream(file.toURI().toURL());
	}
}
