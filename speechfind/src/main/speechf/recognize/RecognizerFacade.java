package speechf.recognize;

import java.io.InputStream;

import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;

import speechf.main.AudioInfo;

public class RecognizerFacade {
	
	Recognizer recognizer;
	private RecognizerFacade() {
		
	}
	
	public static RecognizerFacade create(AudioInfo audioInfo, InputStream inputStream) {
		RecognizerFacade recognizerFacade = new RecognizerFacade();		
		recognizerFacade.recognizer = ConfigurationManager.createRecognizer(audioInfo, inputStream);
		return recognizerFacade;		
	}
	
	public void recognize() {
		Result result = recognizer.recognize();
		System.out.println(result.getBestFinalResultNoFiller());
	}
}
