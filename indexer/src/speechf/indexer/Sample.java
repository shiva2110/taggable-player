package speechf.indexer;

import edu.cmu.sphinx.recognizer.Recognizer;
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
	}
}
