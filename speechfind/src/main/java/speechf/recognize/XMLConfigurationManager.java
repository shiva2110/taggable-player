package speechf.recognize;

import java.io.InputStream;

import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import speechf.main.AudioInfo;

/**
 * This class creates a recognizer based upon a base XML file configuration.
 *  It may optionally make adjustments to the base configuration to match the audio.
 * @author shiva2110
 *
 */
public class XMLConfigurationManager {

	private AudioInfo audioInfo;
	private static final String defaultConfigUrl = "/recognizer-config.xml";
	private static final String SAMPLERATE_PROPNAME = "sampleRate";
	private static final String DATASOURCE_PROPNAME = "streamDataSource";
	private static final String RECOGNIZER_PROPNAME = "recognizer";
	private String configFileUrl  = defaultConfigUrl;
	private InputStream inputStream;
	
	protected void setAudioInfo(AudioInfo audioInfo) {
		this.audioInfo = audioInfo;
	}
	protected void setConfigFileUrl(String configFileUrl) {
		this.configFileUrl = configFileUrl;
	}
	protected void setInputStream(InputStream inputStream){
		this.inputStream = inputStream;
	}
	
	protected Recognizer createRecognizer(){		
		// Get the base params from file
		ConfigurationManager cm = new ConfigurationManager(XMLConfigurationManager.class.getResource("/recognizer-config.xml"));
		
		// Adjust audio properties
		if(audioInfo!=null){
			cm.setGlobalProperty(SAMPLERATE_PROPNAME, Integer.toString(audioInfo.getSamplingRate()));
		}
		
		StreamDataSource audioDataSource = (StreamDataSource)cm.lookup(DATASOURCE_PROPNAME);
		audioDataSource.setInputStream(inputStream, "");
		
		// Adjust models
		
		// create recognizer
		Recognizer recognizer = (Recognizer) cm.lookup(RECOGNIZER_PROPNAME);
		recognizer.allocate();
		
		return recognizer;
	}
	
	
}