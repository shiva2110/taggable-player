package speechf.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

import speechf.index.IndexerException;
import speechf.index.IndexerFacade;

public class Helper {

	private static StandardAnalyzer standardAnalyzer = null ;
	private static final String stopWordFile = "/stop-words-english4.txt";
	
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
		
			BufferedReader br = new BufferedReader(new FileReader(Helper.class.getResource(stopWordFile).getPath()));
			String line;
			Set<String> stopWordsSet = new HashSet<String>();
			while((line=br.readLine()) != null) {
				stopWordsSet.add(line.toLowerCase().trim());
			}
			br.close();

			return stopWordsSet;
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
			
			return list;			
	}
}
