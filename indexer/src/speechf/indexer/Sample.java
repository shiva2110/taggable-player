package speechf.indexer;

import java.io.File;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class Sample {

	public static void main(String[] args){
		ConfigurationManager cm;

		if (args.length > 0) {
			cm = new ConfigurationManager(args[0]);
		} else {
			cm = new ConfigurationManager(Sample.class.getResource("/config.xml"));
		}
		
		 Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
	     recognizer.allocate();
	     
	     AudioFileDataSource audioFileSource = (AudioFileDataSource)cm.lookup("audioFileDataSource");
	     File file = new File("/Users/shiva2110/Documents/voice.wav");
	     audioFileSource.setAudioFile(file, "");
	     
	     
	     Result result = recognizer.recognize();
	     System.out.println(result.getBestFinalResultNoFiller());
	     
	     
	}
}
