package speechf.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;

import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;
import speechf.search.SpeechFind.ScoredTranscriptWord;

public class SpeechFindTest {
	
	SpeechFind speechFind = new SpeechFind();
		
	@Test
	public void tokenize_nullReq_emptyResp() throws Exception{		
		List<String> arr = speechFind.tokenize(null);
		assertNotNull("tokenize() response must not be null", arr);
		assertEquals("For a null input tokenize must return empty", 0, arr.size());
	}
	
	@Test
	public void tokenize_onlyStopWordReq_emptyResp() throws Exception{		
		List<String> arr = speechFind.tokenize("is was a");
		assertNotNull("tokenize() response must not be null", arr);
		assertEquals("For only stop words input tokenize must return empty", 0, arr.size());
	}
	
	@Test
	public void tokenize_validReq_validResp() throws Exception{		
		List<String> arr = speechFind.tokenize("is java project was a");
		assertNotNull("tokenize() response must not be null", arr);
		assertTrue("For a valid input tokenize must return array of valid size", (arr.size()>0));
		
		int found = 0;
		for(String str: arr) {
			if(str.equals("java")) {
				found++;
			} else if(str.equals("project")) {
				found++;
			} else {
				fail("tokenize() has returned array with invalid keyword:" + str);
			}
		}
		
		if(found!=2){
			fail("tokenize() has returned array with less keywords");
		}
	}
	
	@Test
	public void scoreResults_emptyReq_emptyResp() throws Exception {
		SearchTerm term = new SearchTerm();
		term.fieldName = TranscriptWordProp.WORD;
		term.value = "java";
		
		List<ScoredTranscriptWord> list = speechFind.scoreResults(term,  new ArrayList<TranscriptWord>());
		assertNotNull("scoreResults() response must not be null", list);
		assertEquals("For empty input scoreResults() must return empty", 0, list.size());
	}
	
	@Test
	public void scoreResults_nullTerm_emptyResp() throws Exception {			
		List<TranscriptWord> inputList = new ArrayList<TranscriptWord>();
		TranscriptWord t1 = new TranscriptWord();
		t1.addProp(TranscriptWordProp.WORD, "java project");
		t1.addProp(TranscriptWordProp.START_TIME, "0.00");
		t1.addProp(TranscriptWordProp.END_TIME, "0.15");
		
		inputList.add(t1);
		List<ScoredTranscriptWord> list = speechFind.scoreResults(null,  inputList);
		assertNotNull("scoreResults() response must not be null", list);
		assertEquals("For null search term input scoreResults() must return empty", 1, inputList.size());
	}
	
	
	@Test
	public void scoreResults_validReq_validResp() throws Exception{		
		TranscriptWord t1 = new TranscriptWord();
		t1.addProp(TranscriptWordProp.WORD, "java project");
		t1.addProp(TranscriptWordProp.START_TIME, "0.00");
		t1.addProp(TranscriptWordProp.END_TIME, "0.15");
		
		TranscriptWord t3 = new TranscriptWord();
		t3.addProp(TranscriptWordProp.WORD, "java speechf project");
		t3.addProp(TranscriptWordProp.START_TIME, "0.16");
		t3.addProp(TranscriptWordProp.END_TIME, "0.20");
		
		TranscriptWord t2 = new TranscriptWord();
		t2.addProp(TranscriptWordProp.WORD, "java speechf");
		t2.addProp(TranscriptWordProp.START_TIME, "0.07");
		t2.addProp(TranscriptWordProp.END_TIME, "0.15");
		
		TranscriptWord t7 = new TranscriptWord();
		t7.addProp(TranscriptWordProp.WORD, "speechf project");
		t7.addProp(TranscriptWordProp.START_TIME, "0.21");
		t7.addProp(TranscriptWordProp.END_TIME, "0.24");
		
		TranscriptWord t4 = new TranscriptWord();
		t4.addProp(TranscriptWordProp.WORD, "java speechf");
		t4.addProp(TranscriptWordProp.START_TIME, "0.21");
		t4.addProp(TranscriptWordProp.END_TIME, "0.24");
		
		TranscriptWord t5 = new TranscriptWord();
		t5.addProp(TranscriptWordProp.WORD, "java speechf");
		t5.addProp(TranscriptWordProp.START_TIME, "0.27");
		t5.addProp(TranscriptWordProp.END_TIME, "0.30");
		
		TranscriptWord t6 = new TranscriptWord();
		t6.addProp(TranscriptWordProp.WORD, "java speechf");
		t6.addProp(TranscriptWordProp.START_TIME, "0.32");
		t6.addProp(TranscriptWordProp.END_TIME, "0.35");
		
		
		List<TranscriptWord> inputList = new ArrayList<TranscriptWord>();
		inputList.add(t1);
		inputList.add(t2);
		inputList.add(t3);
		inputList.add(t4);
		inputList.add(t5);
		inputList.add(t6);
		inputList.add(t7);
		
		SearchTerm searchTerm = new SearchTerm();
		searchTerm.fieldName = TranscriptWordProp.WORD;
		searchTerm.value = "java";
		
		List<ScoredTranscriptWord> resultList = speechFind.scoreResults(searchTerm,  inputList);
		assertNotNull("scoreResults() response must not be null", resultList);
		assertEquals("scoreResults() should return same sized response as request", 7, resultList.size());
		
		// results containing searchTerm and with overlapping time range should be at the top
		if(!resultList.get(0).transcriptWord.
				getValue(TranscriptWordProp.END_TIME).equals("0.15"))  {
			fail("scoreResults() scoring is incorrect");
		}
		
		if(!resultList.get(1).transcriptWord.                                                                                                
				getValue(TranscriptWordProp.END_TIME).equals("0.15"))  {
			fail("scoreResults() scoring is incorrect");
		}
		
		//results containing no search term should be at the bottom.
		if(!resultList.get(6).transcriptWord.
				getValue(TranscriptWordProp.WORD).equals("speechf project"))  {
			fail("scoreResults() scoring is incorrect");
		}
		
	}
	
	
	@Test
	public void scoreResults_validMultiTermSearch_validResp() throws Exception {		
		TranscriptWord t1 = new TranscriptWord();
		t1.addProp(TranscriptWordProp.WORD, "java project");
		t1.addProp(TranscriptWordProp.START_TIME, "0.00");
		t1.addProp(TranscriptWordProp.END_TIME, "0.06");
		
		TranscriptWord t3 = new TranscriptWord();
		t3.addProp(TranscriptWordProp.WORD, "java speechf project");
		t3.addProp(TranscriptWordProp.START_TIME, "0.16");
		t3.addProp(TranscriptWordProp.END_TIME, "0.20");
		
		TranscriptWord t2 = new TranscriptWord();
		t2.addProp(TranscriptWordProp.WORD, "java speechf");
		t2.addProp(TranscriptWordProp.START_TIME, "0.07");
		t2.addProp(TranscriptWordProp.END_TIME, "0.15");
		
		TranscriptWord t8 = new TranscriptWord();
		t8.addProp(TranscriptWordProp.WORD, "jquery script");
		t8.addProp(TranscriptWordProp.START_TIME, "0.39");
		t8.addProp(TranscriptWordProp.END_TIME, "0.43");
		
		TranscriptWord t7 = new TranscriptWord();
		t7.addProp(TranscriptWordProp.WORD, "speechf project");
		t7.addProp(TranscriptWordProp.START_TIME, "0.21");
		t7.addProp(TranscriptWordProp.END_TIME, "0.24");
		
		TranscriptWord t4 = new TranscriptWord();
		t4.addProp(TranscriptWordProp.WORD, "java speechf");
		t4.addProp(TranscriptWordProp.START_TIME, "0.21");
		t4.addProp(TranscriptWordProp.END_TIME, "0.24");
		
		TranscriptWord t5 = new TranscriptWord();
		t5.addProp(TranscriptWordProp.WORD, "java speechf");
		t5.addProp(TranscriptWordProp.START_TIME, "0.27");
		t5.addProp(TranscriptWordProp.END_TIME, "0.30");
		
		TranscriptWord t6 = new TranscriptWord();
		t6.addProp(TranscriptWordProp.WORD, "java speechf");
		t6.addProp(TranscriptWordProp.START_TIME, "0.32");
		t6.addProp(TranscriptWordProp.END_TIME, "0.35");
		
		
		
		
		List<TranscriptWord> inputList = new ArrayList<TranscriptWord>();
		inputList.add(t1);
		inputList.add(t2);
		inputList.add(t3);
		inputList.add(t4);
		inputList.add(t5);
		inputList.add(t6);
		inputList.add(t7);
		inputList.add(t8);
		
		SearchTerm searchTerm = new SearchTerm();
		searchTerm.fieldName = TranscriptWordProp.WORD;
		searchTerm.value = "java speechf";
		
		List<ScoredTranscriptWord> resultList = speechFind.scoreResults(searchTerm,  inputList);
		assertNotNull("scoreResults() response must not be null", resultList);
		assertEquals("scoreResults() should return same sized response as request", 8, resultList.size());
		
		// results containing all the searchTerms should be at the top
		
		String t = resultList.get(0).transcriptWord.
				getValue(TranscriptWordProp.WORD);
		if(!t.contains("java") || !t.contains("speechf"))  {
			fail("scoreResults() scoring is incorrect");
		}
		
		t = resultList.get(1).transcriptWord.
				getValue(TranscriptWordProp.WORD);
		if(!t.contains("java") || !t.contains("speechf"))  {
			fail("scoreResults() scoring is incorrect");
		}
	
		t = resultList.get(2).transcriptWord.
				getValue(TranscriptWordProp.WORD);
		if(!t.contains("java") || !t.contains("speechf"))  {
			fail("scoreResults() scoring is incorrect");
		}
		
		t = resultList.get(3).transcriptWord.
				getValue(TranscriptWordProp.WORD);
		if(!t.contains("java") || !t.contains("speechf"))  {
			fail("scoreResults() scoring is incorrect");
		}
		
		t = resultList.get(4).transcriptWord.
				getValue(TranscriptWordProp.WORD);
		if(!t.contains("java") || !t.contains("speechf"))  {
			fail("scoreResults() scoring is incorrect");
		}
		
		//result containing no search terms should be at the bottom.
		t = resultList.get(7).transcriptWord.
				getValue(TranscriptWordProp.WORD);
		if(!t.contains("jquery") || !t.contains("script"))  {
			fail("scoreResults() scoring is incorrect");
		}
	}
	
	@Test
	public void  pruneResults_prunableReq_validResp() {
		List<SpeechFind.ScoredTranscriptWord> reqList = new ArrayList<SpeechFind.ScoredTranscriptWord>();
		
		TranscriptWord word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.01");
		word.addProp(TranscriptWordProp.WORD, "java is a coffee");
		SpeechFind.ScoredTranscriptWord scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.88);
		reqList.add(scoredWord);
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.11");
		word.addProp(TranscriptWordProp.WORD, "coffee bean");
		scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.72);
		reqList.add(scoredWord);
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.23");
		word.addProp(TranscriptWordProp.WORD, "java bean");
		scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.71);
		reqList.add(scoredWord);
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.04");
		word.addProp(TranscriptWordProp.WORD, "java a coffee");
		scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.56);
		reqList.add(scoredWord);		
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.21");
		word.addProp(TranscriptWordProp.WORD, "bean dip on java");
		scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.45);
		reqList.add(scoredWord);
		
		
		SpeechFind speechFind = new SpeechFind();
		List<SpeechFind.ScoredTranscriptWord> prunedList = speechFind.pruneResults(reqList);
		
		assertNotNull("pruneResults() response must not be null", prunedList);
		assertEquals("pruneResults() should return correct sized response", 2, prunedList.size());
		
		//test whether the order remains intact.
		SpeechFind.ScoredTranscriptWord prevWord = prunedList.get(0);
		for(int i=1; i<prunedList.size(); i++) {
			if(prunedList.get(i).compare(prunedList.get(i), prevWord) < 0) {
				fail("order is not maintained after pruning");
			}
			prevWord = prunedList.get(i);
		}
		
		//test whether the pruning worked
		for(int i=0; i<prunedList.size(); i++)  {
			for(int j=(i+1); j<prunedList.size(); j++) {
				
				if(isClose(prunedList.get(i).transcriptWord, 
						prunedList.get(j).transcriptWord,
						speechFind.getClosenessWindow())) {
					fail("pruning did not work properly");
				}
			}
		}
	}
	
	@Test
	public void  pruneResults_nonPrunableReq_validResp() {
		List<SpeechFind.ScoredTranscriptWord> reqList = new ArrayList<SpeechFind.ScoredTranscriptWord>();
		
		TranscriptWord word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.01");
		word.addProp(TranscriptWordProp.WORD, "java is a coffee");
		SpeechFind.ScoredTranscriptWord scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.88);
		reqList.add(scoredWord);
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.12");
		word.addProp(TranscriptWordProp.WORD, "coffee bean");
		scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.72);
		reqList.add(scoredWord);
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.23");
		word.addProp(TranscriptWordProp.WORD, "java bean");
		scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.71);
		reqList.add(scoredWord);
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.56");
		word.addProp(TranscriptWordProp.WORD, "java a coffee");
		scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.56);
		reqList.add(scoredWord);		
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.START_TIME, "0.43");
		word.addProp(TranscriptWordProp.WORD, "bean dip on java");
		scoredWord = new SpeechFind().new ScoredTranscriptWord(word, 0.45);
		reqList.add(scoredWord);
		
		
		SpeechFind speechFind = new SpeechFind();
		List<SpeechFind.ScoredTranscriptWord> prunedList = speechFind.pruneResults(reqList);
		
		assertNotNull("pruneResults() response must not be null", prunedList);
		assertEquals("pruneResults() should return correct sized response", 5, prunedList.size());
		
		//test whether the order remains intact.
		SpeechFind.ScoredTranscriptWord prevWord = prunedList.get(0);
		for(int i=1; i<prunedList.size(); i++) {
			if(prunedList.get(i).compare(prunedList.get(i), prevWord) < 0) {
				fail("order is not maintained after pruning");
			}
			prevWord = prunedList.get(i);
		}
		
	}
	
	@Test
	public void getSnippet_noSearchTermReq_validResp() {
		SpeechFind speechFind = new SpeechFind();
		SearchTerm searchQ = new SearchTerm();
		searchQ.fieldName = TranscriptWordProp.WORD;
		searchQ.value = "dance project";
		
		String result = "jQuery is a fast, small, and feature-rich JavaScript library. It makes things like HTML document traversal and manipulation, event handling, animation, and Ajax much simpler with an easy-to-use API that works across a multitude of browsers. With a combination of versatility and extensibility, jQuery has changed the way that millions of people write JavaScript.";
		String snippet = speechFind.getSnippet(searchQ, result, 50, 30);
		
		assertNotNull("getSnippet() response must not be null", snippet);
		assertTrue("getSnippet() response must be less than 50 chars", (snippet.length()<50));
		
	}
	
	@Test
	public void getSnippet_validReq_allSearchTermsResp() {
		SpeechFind speechFind = new SpeechFind();
		SearchTerm searchQ = new SearchTerm();
		searchQ.fieldName = TranscriptWordProp.WORD;
		searchQ.value = "jquery javascript";
		
		String result = "jQuery is a fast, small, and feature-rich JavaScript library. It makes things like HTML document traversal and manipulation, event handling, animation, and Ajax much simpler with an easy-to-use API that works across a multitude of browsers. With a combination of versatility and extensibility, jQuery has changed the way that millions of people write JavaScript.";
		String snippet = speechFind.getSnippet(searchQ, result, 50, 30);
		
		assertNotNull("getSnippet() response must not be null", snippet);
		
		snippet = snippet.toLowerCase();
		
		//check if snippet contains all search terms
		if(!snippet.contains("jquery") || !snippet.contains("javascript")) {
			fail("snippet does not contains all search terms: " + snippet);
		}
		
		
		searchQ = new SearchTerm();
		searchQ.fieldName = TranscriptWordProp.WORD;
		searchQ.value = "jquery javascript HTML";
		
		snippet = speechFind.getSnippet(searchQ, result, 50, 30);
		
		snippet = snippet.toLowerCase();
		
		//check if snippet contains all search terms
		if(!snippet.contains("jquery") || !snippet.contains("javascript") || !snippet.contains("html")) {
			fail("snippet does not contains all search terms: " + snippet);
		}
	}
	
	
	@Test
	public void getSnippet_validReq_maxCharsResp() {
		SpeechFind speechFind = new SpeechFind();
		SearchTerm searchQ = new SearchTerm();
		searchQ.fieldName = TranscriptWordProp.WORD;
		searchQ.value = "jquery javascript";
		
		String result = "jQuery is a fast, small, and feature-rich JavaScript library. It makes things like HTML document traversal and manipulation, event handling, animation, and Ajax much simpler with an easy-to-use API that works across a multitude of browsers. With a combination of versatility and extensibility, jQuery has changed the way that millions of people write JavaScript.";
		String snippet = speechFind.getSnippet(searchQ, result, 50, 30);
		
		assertNotNull("getSnippet() response must not be null", snippet);
		
		snippet = snippet.toLowerCase();
		
		//check if max chars is met
		String temp = snippet.replaceAll("jquery", "");
		temp = temp.replaceAll("javascript", "");
		temp = temp.replaceAll("\\.\\.", "");
		assertTrue("getSnippet() response must be less than 50 chars", (temp.length()<=50));
		
		
		searchQ = new SearchTerm();
		searchQ.fieldName = TranscriptWordProp.WORD;
		searchQ.value = "jquery javascript HTML";		
		snippet = speechFind.getSnippet(searchQ, result, 50, 30);		
		snippet = snippet.toLowerCase();
		
		temp = snippet.replaceAll("jquery", "");
		temp = temp.replaceAll("javascript", "");
		temp = temp.replaceAll("html", "");
		temp = temp.replaceAll("\\.\\.", "");
		assertTrue("getSnippet() response must be less than 50 chars", (temp.length()<=50));
	
	}
	
	
	@Test
	public void getSnippet_validReq_closeWordsResp() {
		SpeechFind speechFind = new SpeechFind();
		SearchTerm searchQ = new SearchTerm();
		searchQ.fieldName = TranscriptWordProp.WORD;
		searchQ.value = "jquery javascript";
		
		String result = "jQuery is a fast, small, and feature-rich JavaScript library. It makes things like HTML document traversal and manipulation, event handling, animation, and Ajax much simpler with an easy-to-use API that works across a multitude of browsers. With a combination of versatility and extensibility, jQuery has changed the way that millions of people write JavaScript.";
		String snippet = speechFind.getSnippet(searchQ, result, 50, 30);
		
		assertNotNull("getSnippet() response must not be null", snippet);
		
		snippet = snippet.toLowerCase();
		
		//check if snippet contains shortest connection
		if(!snippet.contains("jquery is") || !snippet.contains("feature-rich javascript library") ) {
			fail("snippet does not contain the shortest connection");
		}
		
		// check if the shortest connection is separated by not more than 30 chars
		int stIndex = snippet.indexOf("jquery");
		int enIndex = snippet.indexOf("javascript");		
		while(stIndex!=snippet.length() && snippet.charAt(stIndex)!=' ') {
			stIndex++;
		}		
		enIndex =  enIndex-1;		
		String betweenString = snippet.substring(stIndex, enIndex);
		betweenString.replaceAll("\\.\\.", "");		
		assertTrue("The shortest connection should not be separated by more than 30 chars", (betweenString.length()<=30));
		
		
		searchQ = new SearchTerm();
		searchQ.fieldName = TranscriptWordProp.WORD;
		searchQ.value = "jquery javascript HTML";		
		snippet = speechFind.getSnippet(searchQ, result, 50, 30);		
		snippet = snippet.toLowerCase();
		
		//check if snippet contains shortest connection
		if(!snippet.contains("jquery is") || !snippet.contains("feature-rich javascript library") || !(snippet.contains("like html document")) ) {
				fail("snippet does not contain the shortest connection");
		}
		
		stIndex = snippet.indexOf("jquery");
		enIndex = snippet.indexOf("html");		
		while(stIndex!=snippet.length() && snippet.charAt(stIndex)!=' ') {
			stIndex++;
		}		
		enIndex =  enIndex-1;		
		betweenString = snippet.substring(stIndex, enIndex);
		betweenString.replaceAll("\\.\\.", "");		
		assertTrue("The shortest connection should not be separated by more than 30 chars", (betweenString.length()<=30));
	
	}
	
	private boolean isClose(TranscriptWord word1, TranscriptWord word2, double closenessWindow) {
		double startTime1 = new Double(word1.getValue(TranscriptWordProp.START_TIME));
		double startTime2 =  new Double(word2.getValue(TranscriptWordProp.START_TIME));
		if( (startTime1<=startTime2 && (startTime1 + closenessWindow)>=startTime2) || 
				(startTime1>startTime2  && (startTime2 + closenessWindow) >= startTime1) ) {
			return true;
		}
		return false;
	}
	
	
}
