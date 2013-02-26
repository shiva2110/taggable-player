package speechf.main;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import speechf.index.IndexerFacade;
import speechf.mprocess.AudioExtractor;
import speechf.mprocess.AudioExtractorStream;
import speechf.mprocess.AudioExtractorTest;
import speechf.recognize.RecognizerFacade;


public class controller {
	
	private static final Logger logger  = LoggerFactory.getLogger(controller.class);
	
	public static void main(String[] args) throws IOException{
		
	
		AudioExtractor audioExtractor = new AudioExtractor("file://projects/workrepo/speech-F/sampledata/news.wav");
	/*	AudioInfo audioInfo = new AudioInfo();
		audioInfo.setSamplingRate(16000);
		audioInfo.setChannels(1);
		audioExtractor.setResample(audioInfo);*/
		AudioExtractorStream audioStream = new AudioExtractorStream(audioExtractor);		
		RecognizerFacade recognizerFacade = RecognizerFacade.create(audioExtractor.getAudioInfo(), audioStream);
		logger.debug("===============================================");
		logger.debug("Recognition started!");
		recognizerFacade.recognize();
		logger.debug("===============================================");
		logger.debug("Indexing started!");
		IndexerFacade indexer = new IndexerFacade();
		recognizerFacade.indexWordTimeMap(indexer);
		indexer.close();
		logger.debug("===============================================");
		logger.debug("Indexing complete!");
		
	}
}
