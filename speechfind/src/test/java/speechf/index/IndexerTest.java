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
	public void testIndex() throws CorruptIndexException, IOException, ParseException, IndexerException {
	/*	IndexerFacade indexer = new IndexerFacade();
		TranscriptWord transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "jQuery is a fast, small, and feature-rich JavaScript library.");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.00");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.10");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "jQuery is a JavaScript library");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.00");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.10");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "jQuery is small rich JavaScript library");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.00");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.10");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "It makes things like HTML document traversal and manipulation simple");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.11");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.16");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "HTML document traversal");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.11");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.16");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "HTML document traversal, event handling, animation and Ajax");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.11");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.16");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "It makes things like HTML document traversal and manipulation, event handling, animation, and Ajax much simpler");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.11");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.16");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "easy-to-use API that works across a multitude of browsers.");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.17");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.23");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "easy-to-use API that works across browsers.");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.17");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.23");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "easy-to-use API for all browsers.");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.17");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.23");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "combination of versatility and extensibility");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.19");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.23");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "With a combination of versatility and extensibility, jQuery has changed the way that millions of people write JavaScript.");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.24");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.30");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "versatility and extensibility of jQuery");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.24");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.30");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.WORD, "versatility and extensibility of jQuery changed millions of people writing JavaScript");
		transcriptWord.addProp(TranscriptWordProp.START_TIME, "0.24");
		transcriptWord.addProp(TranscriptWordProp.END_TIME, "0.30");
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=video about jquery");
		
		indexer.index(transcriptWord);
		
		indexer.close();*/
		
		
	}
}
