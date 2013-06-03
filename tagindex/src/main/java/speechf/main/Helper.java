package speechf.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import speechf.index.IndexerException;
import speechf.index.IndexerFacade;
import speechf.search.SearcherFacade;

public class Helper {

	private static StandardAnalyzer standardAnalyzer = null ;
	private static final String stopWordFile = "/stop-words-english4.txt";
	private static Stemmer stemmer;
	private static final Logger logger = LoggerFactory.getLogger(Helper.class);
	
	public static synchronized StandardAnalyzer getStandardAnalyzerWithStopWords() throws IOException {
		if(standardAnalyzer==null){
			Set<String> stopWordsSet = readStopWords();
			standardAnalyzer = new StandardAnalyzer(Version.LUCENE_36, stopWordsSet);
		}
		return standardAnalyzer;
	}
	

	public static synchronized StandardAnalyzer getStandardAnalyzer(boolean withStopWords, boolean reset) throws IOException {
		if(standardAnalyzer==null || reset){
			if(withStopWords) {
				Set<String> stopWordsSet = readStopWords();
				standardAnalyzer = new StandardAnalyzer(Version.LUCENE_36, stopWordsSet);
			} else {
				standardAnalyzer = new StandardAnalyzer(Version.LUCENE_36, new HashSet<>());
			}			
		}
		return standardAnalyzer;
	}
	
	private static Set<String> readStopWords() throws IOException {
		InputStream in = Helper.class.getResourceAsStream(stopWordFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String line;
		Set<String> stopWordsSet = new HashSet<String>();
		while((line=br.readLine()) != null) {
			stopWordsSet.add(line.toLowerCase().trim());
		}
		br.close();

		return stopWordsSet;
			
	}
	
	private static synchronized void initializeStemmer() {
		if(stemmer==null) {
			stemmer = new Stemmer();
		}
	}
	
	public static String stem(String word) {
		if(stemmer==null) {
			initializeStemmer();
		}
		stemmer.add(word.toCharArray(), word.length());
		stemmer.stem();
		return stemmer.toString();		
	}
	
	public static String stemKeywords(String words) {
		StringBuffer buffer = new StringBuffer();
		String fmtKeyword = null;
		try {
			List<String> keywordList = 	Helper.tokenize(words, true, false);
			for(String k: keywordList) {
				logger.debug("keyword before stemming: " +  k);
				String stemmedWord = Helper.stem(k);
				logger.debug("keyword after stemming: " +  stemmedWord);
				buffer.append(stemmedWord).append(" ");
			}
			fmtKeyword = buffer.toString().trim();
		} catch(IOException e) {
			//Log
			e.printStackTrace();
			logger.debug("Exception at Helper.stemKeywords()" + e.getMessage());
		}
		
		if(fmtKeyword==null || fmtKeyword.length()==0 || fmtKeyword=="") {
			fmtKeyword = words;
		}
		return fmtKeyword;
	}
	
	
	/**
	 * returns an array of keywords, representing analyzed and tokenized words.
	 * @param keywords
	 * @return
	 * @throws IOException 
	 * @throws IndexerException 
	 */
	public static List<String> tokenize(String keywords, boolean withStopWords, boolean analyzerReset) throws IOException {
		
			List<String> list = new ArrayList<String>();
			if(keywords==null) {
				return list;
			}	
			
			Analyzer analyzer = Helper.getStandardAnalyzer(withStopWords, analyzerReset);
			 
			TokenStream tokenStream = analyzer.tokenStream("", new StringReader(keywords));
			
			do {
				String term = tokenStream.getAttribute(CharTermAttribute.class).toString();
				if(term.equals("")) {
					continue;
				}
				list.add(term);

			}while(tokenStream.incrementToken());	
			logger.debug("tokenized list length: " + list.size());
			return list;			
			
	}
}
