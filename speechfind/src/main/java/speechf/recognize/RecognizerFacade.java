package speechf.recognize;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.CorruptIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.cmu.sphinx.decoder.search.Token;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.linguist.SearchState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeHMMState;
import edu.cmu.sphinx.linguist.lextree.LexTreeLinguist.LexTreeWordState;
import edu.cmu.sphinx.recognizer.Recognizer;
import edu.cmu.sphinx.result.Result;

import speechf.index.IndexerException;
import speechf.index.IndexerFacade;
import speechf.main.AudioInfo;
import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;

public class RecognizerFacade {
	
	Recognizer recognizer;
	List<Result> recognizerResult;
	int audioHash = -1;
	private static final Logger logger  = LoggerFactory.getLogger(RecognizerFacade.class);
	private RecognizerFacade() {
		
	}
	
	public static RecognizerFacade create(AudioInfo audioInfo, InputStream inputStream) {
		RecognizerFacade recognizerFacade = new RecognizerFacade();		
		recognizerFacade.recognizer = ConfigurationManager.createRecognizer(audioInfo, inputStream);
		return recognizerFacade;		
	}
	
	public void recognize() {
		recognizerResult = new ArrayList<Result>();
		StringBuffer timedResult = new StringBuffer();
		Result result = null;
		while((result=recognizer.recognize())!=null){
			timedResult.append(result.getTimedBestResult(true, true)).append(" ");		
			recognizerResult.add(result);
		}
		if(timedResult.length()!=0){
			audioHash = timedResult.toString().hashCode();
		}
	}
	
	public void indexWordTimeMap(IndexerFacade indexer) throws CorruptIndexException, IOException, IndexerException{
		if(recognizerResult==null || audioHash==-1){
			throw new IllegalStateException("The recognizer result is null, recognize() method should be called first");
		}
		
		for(Result result: recognizerResult){
			Token token = result.getBestToken();

			while(token!=null) {
				SearchState searchState;
				while(token!=null && !((searchState=token.getSearchState()) instanceof LexTreeWordState)){
					token = token.getPredecessor();
				}

				if(token.getWord().isFiller()) {
					token = token.getPredecessor();
					continue;
				}

				String word = token.getWord().getSpelling();
				Float endWordTime = null;
				Float startWordTime = null;

				token = token.getPredecessor();

				// Iterate until next word is found
				while(token!=null && !((searchState=token.getSearchState()) instanceof LexTreeWordState)){
					//Capture start and end time
					if((searchState instanceof LexTreeHMMState)) {

						if(token.getData()!=null){
							float time = getTime(token.getData());
							if(endWordTime==null){
								endWordTime = time;
							} else {
								startWordTime = time;
							}							
						}
					}
					token = token.getPredecessor();
				}

				TranscriptWord transcriptWord = new TranscriptWord();
				transcriptWord.addProp(TranscriptWordProp.WORD, word);
				transcriptWord.addProp(TranscriptWordProp.START_TIME, startWordTime.toString());
				transcriptWord.addProp(TranscriptWordProp.END_TIME, endWordTime.toString());
				transcriptWord.addProp(TranscriptWordProp.AUDIO_HASH, Integer.toString(audioHash));
				
				indexer.index(transcriptWord);
				logger.debug("Indexed: " + transcriptWord.toString());

		}
	}
	}
	
	private static float getTime(Data data) {
		if (data == null) {
			throw new Error("No data saved in token");
		}

		if (! (data instanceof FloatData)) {
			throw new Error("Expecting float data");
		}

		FloatData fd = (FloatData) data;
		return  ((float) fd.getFirstSampleNumber()/ fd.getSampleRate());
	}
		
	
}
