package speechf.indexer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;

import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;
import edu.cmu.sphinx.util.props.ConfigurationManager;

public class Sample {

	public static void main(String[] args) throws MalformedURLException, FileNotFoundException{
		ConfigurationManager cm;

		if (args.length > 0) {
			cm = new ConfigurationManager(args[0]);
		} else {
			cm = new ConfigurationManager(Sample.class.getResource("/config2.xml"));
		}
		
		 
	     
		StreamDataSource audioFileSource = (StreamDataSource)cm.lookup("streamDataSource");
		InputStream inputStream = new FileInputStream(new File("/Users/shiva2110/Downloads/male.wav"));
	     audioFileSource.setInputStream(inputStream, "");
	     
	     Recognizer recognizer = (Recognizer) cm.lookup("recognizer");
	     recognizer.allocate();
	     Result result = recognizer.recognize();
	     System.out.println(result.getBestFinalResultNoFiller());
	     
	     
	}
}
