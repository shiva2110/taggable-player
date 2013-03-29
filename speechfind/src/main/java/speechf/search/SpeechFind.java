package speechf.search;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.util.Version;

import speechf.index.IndexerException;
import speechf.index.IndexerFacade;
import speechf.main.Helper;
import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;


/**
 * Class for speech find algorithm
 * @author shiva2110
 *
 */
public class SpeechFind {
	
	private static final double closenessWindow = 0.10;
	
	public double getClosenessWindow() {
		return closenessWindow;
	}
	
	public class ScoredTranscriptWord implements Comparator{
		double score;
		TranscriptWord transcriptWord;
		public ScoredTranscriptWord(TranscriptWord transcriptWord, double score) {
			this.transcriptWord = transcriptWord;
			this.score = score;
		}
		
		@Override
		public int compare(Object o1, Object o2) {
			if(!o1.getClass().equals(ScoredTranscriptWord.class)  || 
					!o2.getClass().equals(ScoredTranscriptWord.class)) {
				throw new IllegalArgumentException();
			}
			
			ScoredTranscriptWord obj1 = (ScoredTranscriptWord)o1;
			ScoredTranscriptWord obj2 = (ScoredTranscriptWord)o2;
			
			if(obj1.score < obj2.score) {
				return 1;
			} else if(obj1.score > obj2.score) {
				return -1;
			} else {
				return 0;
			}
		}
	}
	
	
	public List<ScoredTranscriptWord> find(SearchTerm searchQ, SearchTerm filterQ) {
		
		try {
			//Call SearcherFacade.search to get results.
			SearcherFacade searcherFacade = new SearcherFacade();
			List<TranscriptWord> transcriptWordList = searcherFacade.vectorSearch(searchQ, filterQ);
			
			// Call scoreResults() to score results.
			List<ScoredTranscriptWord> scoredTranscriptList = scoreResults(searchQ, transcriptWordList);
			
			// Call pruneResults()
			scoredTranscriptList = pruneResults(scoredTranscriptList);
			
			return scoredTranscriptList;
		} catch(Exception e) {
			
		}
		
		
		return null;
	}
	
	public String getSnippet(SearchTerm searchQ, String result, int maxChars, int maxConnectionChars) {
		return null;
	}
	
	public List<ScoredTranscriptWord> pruneResults(List<ScoredTranscriptWord> transcriptWordList) {
		
		Set<Integer> prunableSet = new HashSet<Integer>();
		for(int i=0;i<transcriptWordList.size(); i++) {
			if(prunableSet.contains(i)) {
				continue;
			}
			for(int j=(i+1); j<transcriptWordList.size(); j++) {
				if(prunableSet.contains(j)) {
					continue;
				}
				
				if( isClose(transcriptWordList.get(i).transcriptWord,
						transcriptWordList.get(j).transcriptWord)  ) {
					prunableSet.add(j);
				}
			}
		}
		
		List<ScoredTranscriptWord> prunedList = new ArrayList<ScoredTranscriptWord>(); 
		for(int i=0;i<transcriptWordList.size(); i++) {
			if(!prunableSet.contains(i)) {
				prunedList.add(transcriptWordList.get(i));
			}			
		}
		
		
		return prunedList;
	}
	
	private boolean isClose(TranscriptWord word1, TranscriptWord word2) {
		double startTime1 = new Double(word1.getValue(TranscriptWordProp.START_TIME));
		double startTime2 =  new Double(word2.getValue(TranscriptWordProp.START_TIME));
		if( (startTime1<=startTime2 && (startTime1 + closenessWindow)>=startTime2) || 
				(startTime1>startTime2  && (startTime2 + closenessWindow) >= startTime1) ) {
			return true;
		}
		return false;
	}
	
	
	/**
	 * Applies scoring algorithm to score input results and returns sorted scored results
	 * 
	 * This method scores based on popularity of input search terms -
	 * 		1. A result is scored high if many other results have common search terms in the same time range. (similarResultsWithSameWindow)
	 * 			- This indicates agreement/popularity that the search terms are indeed present in that time range.
	 * 		2. A result is scored low if many other results have common search terms in different time range. (similarResults)
	 * 			- This indicates that the search term is just too common and occurs everywhere. Can be related to IDF.
	 * @param transcriptWordList
	 * @return
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	public List<ScoredTranscriptWord> scoreResults(SearchTerm searchQ, List<TranscriptWord> transcriptWordList) throws IOException{
		List<ScoredTranscriptWord> scoredList = new ArrayList<ScoredTranscriptWord>();
		
		for(TranscriptWord wordObj: transcriptWordList) {
			scoredList.add(new ScoredTranscriptWord(wordObj, 0));
		}
		
		if(searchQ == null){
			return scoredList;
		}
		
		List<String> searchTerms = tokenize(searchQ.value);
		
		for(int i=0; i<scoredList.size(); i++) {
			List<String> containingTerms = getContainingTerms(scoredList.get(i).transcriptWord.getValue(TranscriptWordProp.WORD), searchTerms);
			
			for(String containingTerm: containingTerms) {
				double similarResults = 1; //
				double similarResultsWithSameWindow = 1;
				
				for(int j=0; j<scoredList.size(); j++) {
					if(i!=j) {
						if(scoredList.get(j).transcriptWord.getValue(TranscriptWordProp.WORD).contains(containingTerm)) {
							similarResults++;
							
							if(sameWindow(scoredList.get(i), scoredList.get(j))) {
								similarResultsWithSameWindow ++;
							}
						}
					}
				}
				
				scoredList.get(i).score = scoredList.get(i).score + (similarResultsWithSameWindow/similarResults);
			}			
		}
		
		
		Collections.sort(scoredList, new ScoredTranscriptWord(null, 0));
		return scoredList;
	}
	
	private boolean sameWindow(ScoredTranscriptWord w1, ScoredTranscriptWord w2) {
		if(new Double(w2.transcriptWord.getValue(TranscriptWordProp.START_TIME)) < 
				new Double(w1.transcriptWord.getValue(TranscriptWordProp.END_TIME))) {
			
			if(new Double(w2.transcriptWord.getValue(TranscriptWordProp.END_TIME)) >=
				new Double(w1.transcriptWord.getValue(TranscriptWordProp.END_TIME))) {
				return true;
			}
		}
		
		return false;
	}
	
	private List<String> getContainingTerms(String str, List<String> terms) {
		
		List<String> containingTerms = new ArrayList<String>();
		for(String s: terms) {
			if(str.contains(s)) {
				containingTerms.add(s);
			}
		}
		
		return containingTerms;
	}
	
	private boolean containsAll(String str, List<String> terms) {
		List<String> containingTerms = new ArrayList<String>();
		for(String s: terms) {
			if(str.contains(s)) {
				containingTerms.add(s);
			}
		}
		
		if(containingTerms.size() == terms.size()){
			return true;
		} 
		return false;
	}
	
	
	/**
	 * returns an array of keywords, representing analyzed and tokenized words.
	 * @param keywords
	 * @return
	 * @throws IOException 
	 * @throws IndexerException 
	 */
	public List<String> tokenize(String keywords) throws IOException {
		
			List<String> list = new ArrayList<String>();
			if(keywords==null) {
				return list;
			}	
			
			Analyzer analyzer = Helper.getStandardAnalyzer();
			TokenStream tokenStream = analyzer.tokenStream("", new StringReader(keywords));
			
			do {
				String term = tokenStream.getAttribute(CharTermAttribute.class).toString();
				if(term.equals("")) {
					continue;
				}
				list.add(term);
				System.out.println(term);
			}while(tokenStream.incrementToken());	
			
			return list;			
	}
	
}
