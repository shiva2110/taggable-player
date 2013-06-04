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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private static final double closenessWindow = 0.04;
	private static final Logger logger = LoggerFactory.getLogger(SearcherFacade.class);
	
	public double getClosenessWindow() {
		return closenessWindow;
	}
	
	public class ScoredTranscriptWord implements Comparator{
		double score;
		TranscriptWord transcriptWord;
		String snippet;
		public ScoredTranscriptWord(TranscriptWord transcriptWord, double score) {
			this.transcriptWord = transcriptWord;
			this.score = score;
		}
		
		public void setSnippet(String snippet) {
			this.snippet = snippet;
		}
		
		public String getSnippet() {
			return this.snippet;
		}
		
		public String getStartTime() {
			return this.transcriptWord.getValue(TranscriptWordProp.START_TIME);
		}
		
		public double getScore() {
			return this.score;
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
	
	
	public List<ScoredTranscriptWord> find(SearchTerm searchQ, SearchTerm filterQ) throws SearchException {
		
		try {
			//Call SearcherFacade.search to get results.
			SearcherFacade searcherFacade = new SearcherFacade();
			String fmtKeyword = Helper.stemKeywords(searchQ.value);
			
			SearchTerm FmtSearchQ = new SearchTerm();
			FmtSearchQ.value = fmtKeyword;
			FmtSearchQ.fieldName = TranscriptWordProp.FMTWORD; //formatted
			logger.debug("formatted search keyword: " + FmtSearchQ.value);
			
			List<TranscriptWord> transcriptWordList = searcherFacade.vectorSearch(FmtSearchQ, filterQ);
			logger.debug("after vector search: " + transcriptWordList.size());
			
			// Call scoreResults() to score results.
			List<ScoredTranscriptWord> scoredTranscriptList = scoreResults(FmtSearchQ, transcriptWordList);
			logger.debug("after score results: " + scoredTranscriptList.size());
			
			// Call pruneResults()
			scoredTranscriptList = pruneResults(scoredTranscriptList);
			logger.debug("after prune results: " + scoredTranscriptList.size());
			
			// get snippet
			for(ScoredTranscriptWord resultObj: scoredTranscriptList) {
				String snippet = getSnippet(FmtSearchQ, resultObj.transcriptWord.getValue(TranscriptWordProp.WORD));
				resultObj.setSnippet(snippet);
			}			
			
			return scoredTranscriptList;
		} catch(IOException | ParseException | SearchException e) {
			throw new SearchException("Exception in find() " + e.getMessage());
		}
	
	}
	
	/**
	 * Takes a formatted (stemmed) search query 'searchQ', and actual result as indexed 'result' as input.
	 * Returns snippet extracted from 'result'
	 * 
	 * @param searchQ
	 * @param result
	 * @return
	 * @throws IOException
	 * @throws SearchException
	 */
	public String getSnippet(SearchTerm searchQ, String result) throws IOException, SearchException {
		
			if(result==null || searchQ==null || searchQ.value==null) {
				return null;
			}
			
			searchQ.value = searchQ.value.toLowerCase();
			String resultLow = result.toLowerCase();
			List<String> searchTermsQ = Helper.tokenize(searchQ.value, true, false);
			
			List<String> resultTerms = Helper.tokenize(resultLow, true, false);
			HashMap<String, List<String>> stemmedWordMap = Helper.stemKeywordList(resultTerms);
			
			System.out.println(stemmedWordMap.toString());
			List<String> searchTerms = new ArrayList<String>();
			for(String term: searchTermsQ) {
				term = term.toLowerCase();
				if(stemmedWordMap.containsKey(term)) {
					List<String> list = stemmedWordMap.get(term);
					searchTerms.addAll(list);
				}
			}
			
			if(searchTerms.size()==0) {
				throw new SearchException("Snippet could not be derived as there are no search terms in the result");
			}
			
			Map<String, List<Integer>> tokenIndexMap = new HashMap<String, List<Integer>>();
			for(String term: searchTerms) {
				List<Integer> list = getAllIndex(term, resultLow);
				if(list.size()>0) {
					tokenIndexMap.put(term, list);
				}
				
			}
			
			Map<String, Integer> curIndexMap = new HashMap<String, Integer>();
			
			
			for(String term: tokenIndexMap.keySet()) {
				curIndexMap.put(term, tokenIndexMap.get(term).get(0));
			}
			
			int minDist = Integer.MAX_VALUE;
			Map<String, Integer> bestIndexMap = new HashMap<String, Integer>();
			
			while(true) {
				// get min
				String minStr = getMinKey(curIndexMap);
				
				// get max
				String maxStr = getMaxKey(curIndexMap);
				
				// get distance between min and max
				int endIndex =  curIndexMap.get(maxStr) + maxStr.length(); //adjust end index to offset for end of maxStr
				int dist = endIndex - curIndexMap.get(minStr);
				
				//update minDist and best index
				if(dist<minDist) {
					minDist = dist;
					bestIndexMap.putAll(curIndexMap);
				}
				
				//update curIndex of minStr by +1, if index out of bounds break
				int curIndex = curIndexMap.get(minStr);
				List<Integer> list = tokenIndexMap.get(minStr);
				int indexOfCurIndex = list.indexOf(curIndex);
				if(indexOfCurIndex == list.size()-1) {
					break;
				}
				curIndex = list.get(indexOfCurIndex + 1);
				curIndexMap.put(minStr, curIndex);
			}
			
			String minStr = getMinKey(bestIndexMap);
			String maxStr = getMaxKey(bestIndexMap);
			
			//adjust maxIndex to end of term
			int index = bestIndexMap.get(maxStr);
			index = index + maxStr.length();			
			String snippet = result.substring(bestIndexMap.get(minStr), index);
			
			//get surrounding terms
			int numberOfSurroundingTerms = 0;
			if(bestIndexMap.get(minStr) == bestIndexMap.get(maxStr)) {
				numberOfSurroundingTerms=2;
			} else {
				numberOfSurroundingTerms=1;
			}
			
			String prefixTerm = getPrevTo(minStr, bestIndexMap.get(minStr), result, numberOfSurroundingTerms);
			String suffixTerm = getNextTo(maxStr, bestIndexMap.get(maxStr), result, numberOfSurroundingTerms);
			
			snippet = new StringBuffer(prefixTerm).append(snippet).append(suffixTerm).toString();
			return snippet;
	}
	
	public String getPrevTo(String token, int tokenStartIndex, String originalText, int numberOfPrevTerms) throws IOException {
		
		token = token.toLowerCase();
		String originalTextLo = originalText.toLowerCase();
		
		//validate the arguments
		//check size
		if(tokenStartIndex<0 || tokenStartIndex>=originalTextLo.length()) {
			return "";
		}
		
		//if the token and tokenStartIndex dont match return empty string
		String rest = originalTextLo.substring(tokenStartIndex) ;
		if(!rest.startsWith(token)) {
			return "";
		}
		
		StringBuffer sb = new StringBuffer("");
		int index = tokenStartIndex;
		index--;
			
		for(int i=0; i<numberOfPrevTerms; i++) {
			while(index>=0) {
				char ch = originalText.charAt(index);	
				String chStr = Character.toString(ch);
				if(chStr.matches("[A-Za-z0-9\\-]")) {
					break;
				}
				sb = new StringBuffer(chStr).append(sb);
				index--;	
			}
			
			while(index>=0) {
				char ch = originalText.charAt(index);	
				String chStr = Character.toString(ch);
				if(!chStr.matches("[A-Za-z0-9\\-]")) {
					break;
				}
				sb = new StringBuffer(chStr).append(sb);
				index--;	
			}
		}
		
		
		return sb.toString();		
	}
	
	public String getNextTo(String token, int tokenStartIndex, String originalText, int numberOfNextTerms) throws IOException {
		
		token = token.toLowerCase();
		String originalTextLo = originalText.toLowerCase();
			
		//validate the arguments
				//check size
				if(tokenStartIndex<0 || tokenStartIndex>=originalTextLo.length()) {
					return "";
				}
				
				//if the token and tokenStartIndex dont match return empty string
				String rest = originalTextLo.substring(tokenStartIndex) ;
				if(!rest.startsWith(token)) {
					return "";
				}
				
				StringBuffer sb = new StringBuffer("");
				int index = tokenStartIndex;
				index++;
				
				//pass the token
				while(index<originalText.length()) {
					char ch = originalText.charAt(index);	
					String chStr = Character.toString(ch);
					if(!chStr.matches("[A-Za-z0-9\\-]")) {
						break;
					}
					index++;	
				}
				
				for(int i=0; i<numberOfNextTerms; i++){
					//pass the delimiter after token
					while(index<originalText.length()) {
						char ch = originalText.charAt(index);	
						String chStr = Character.toString(ch);
						if(chStr.matches("[A-Za-z0-9\\-]")) {
							break;
						}
						sb.append(chStr);
						index++;	
					}
					
					//pass the next term to token
					while(index<originalText.length()) {
						char ch = originalText.charAt(index);	
						String chStr = Character.toString(ch);
						if(!chStr.matches("[A-Za-z0-9\\-]")) {
							break;
						}
						sb.append(chStr);
						index++;	
					}
				}
				
				
				return sb.toString();	
	}
	
	private String getMinKey(Map<String, Integer> map) {
		int min = Integer.MAX_VALUE;
		String minStr = null;
		for(String str: map.keySet()) {
			if(map.get(str)<min) {
				min = map.get(str);
				minStr = str;
			}
		}
		
		return minStr;
	}
	
	private String getMaxKey(Map<String, Integer> map) {
		int max = Integer.MIN_VALUE;
		String maxStr = null;
		for(String str: map.keySet()) {
			if(map.get(str)>max) {
				max = map.get(str);
				maxStr = str;
			}
		}
		
		return maxStr;
	}
	
	
	public List<Integer> getAllIndex(String token, String sentence) {
		
		List<Integer> indexList = new ArrayList<Integer>();
		int index = 0;
		while(index<sentence.length() && index!=-1) {
			index = sentence.indexOf(token, index);
			if(index!=-1) {
				indexList.add(index);
				index = index + token.length();				
			}
		}
		
		return indexList;
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
		
		List<String> searchTerms = Helper.tokenize(searchQ.value, true, false);
		
		for(int i=0; i<scoredList.size(); i++) {
			List<String> containingTerms = getContainingTerms(scoredList.get(i).transcriptWord.getValue(TranscriptWordProp.FMTWORD), searchTerms);
			
			for(String containingTerm: containingTerms) {
				double similarResults = 1; //
				double similarResultsWithSameWindow = 1;
				
				for(int j=0; j<scoredList.size(); j++) {
					if(i!=j) {
						if(scoredList.get(j).transcriptWord.getValue(TranscriptWordProp.FMTWORD).contains(containingTerm)) {
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
		
		str = str.toLowerCase();
		List<String> containingTerms = new ArrayList<String>();
		for(String s: terms) {
			s=s.toLowerCase();
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
	
	
	
	
}
