package speechf.recognize;

import java.io.InputStream;

import speechf.main.AudioInfo;
import edu.cmu.sphinx.recognizer.Recognizer;

/**
 * This is the class which should be directly used by RecognizerFacade class to configure itself
 *  and prepare itself before performing recognition.
 *  
 * This class uses the audioInfo supplied and by using other knowledge about the 
 *  incoming audio stream to decide and choose the best way to configure the recognizer.
 * 
 * @author shiva2110
 *
 */
public class ConfigurationManager {

	/**
	 * Chooses the best way to create and configure the recognizer.
	 * @param audioInfo
	 * @param inputStream
	 * @return
	 */
	public static Recognizer createRecognizer(AudioInfo audioInfo, InputStream inputStream) {
		
		XMLConfigurationManager configMgr = new XMLConfigurationManager();
		configMgr.setAudioInfo(audioInfo);
		configMgr.setInputStream(inputStream);
		
		return configMgr.createRecognizer();
	}
}
