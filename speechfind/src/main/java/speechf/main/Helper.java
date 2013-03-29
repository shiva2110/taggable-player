package speechf.main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

import speechf.index.IndexerException;
import speechf.index.IndexerFacade;

public class Helper {

	private static StandardAnalyzer standardAnalyzer = null ;
	private static final String stopWordFile = "/stop-words-english4.txt";
	
	public static synchronized StandardAnalyzer getStandardAnalyzer() throws IOException {
		if(standardAnalyzer==null){
			Set<String> stopWordsSet = readStopWords();
			standardAnalyzer = new StandardAnalyzer(Version.LUCENE_36, stopWordsSet);
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
}
