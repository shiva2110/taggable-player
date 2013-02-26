package speechf.index;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryParser.ParseException;
import org.junit.Test;

import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;
import speechf.search.SearchResult;
import speechf.search.SearchTerm;
import speechf.search.SpeechFind;

public class IndexerTest {
	
	@Test
	public void testIndex() throws CorruptIndexException, IOException, ParseException {
		IndexerFacade indexer = new IndexerFacade();
		TranscriptWord transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "hello");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.00");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.05");
		transcriptWord.addProp(TranscriptWordProp.AUDIO_HASH, "333");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "world");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.06");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.10");
		transcriptWord.addProp(TranscriptWordProp.AUDIO_HASH, "333");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "I can");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.11");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.14");
		transcriptWord.addProp(TranscriptWordProp.AUDIO_HASH, "333");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "find what you said");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.15");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.19");
		transcriptWord.addProp(TranscriptWordProp.AUDIO_HASH, "333");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "not what");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.20");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.21");
		transcriptWord.addProp(TranscriptWordProp.AUDIO_HASH, "333");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "you sang");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.22");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.27");
		transcriptWord.addProp(TranscriptWordProp.AUDIO_HASH, "333");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "Please speek");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.31");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.34");
		transcriptWord.addProp(TranscriptWordProp.AUDIO_HASH, "333");
		
		indexer.index(transcriptWord);
		
		indexer.close();
		
		
	}
}
