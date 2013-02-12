package speechf.main;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import speechf.mprocess.AudioExtractorStream;
import speechf.mprocess.AudioExtractorTest;
import speechf.recognize.RecognizerFacade;


public class controller {
	
	public static void main(String[] args) throws FileNotFoundException{

		AudioInfo audioInfo = new AudioInfo();
		audioInfo.setSamplingRate(16000);
		
		InputStream inputStream = new FileInputStream("/Users/shiva2110/Downloads/news.wav");
		RecognizerFacade recognizerFacade = RecognizerFacade.create(audioInfo, inputStream);
		recognizerFacade.recognize();
		
	}
}
