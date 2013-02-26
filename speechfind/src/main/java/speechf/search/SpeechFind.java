package speechf.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.queryParser.ParseException;

import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;

public class SpeechFind {

	
	private static final Float snippetWindow = new Float(0.40);
	
	public List<SearchResult> find(String searchTerms, String audioHash) throws IOException, ParseException {
		String[] terms = searchTerms.split(" ");
		SearcherFacade searcherFacade = new SearcherFacade();
		List<TranscriptWord> resultDocs = new ArrayList<TranscriptWord>();

		for(String term: terms) {
			SearchTerm searchTerm = new SearchTerm();
			searchTerm.fieldName = TranscriptWordProp.WORD;
			searchTerm.value = term;

			SearchTerm filterTerm = null;
			if(audioHash!=null){
				filterTerm = new SearchTerm();
				filterTerm.fieldName = TranscriptWordProp.AUDIO_HASH;
				filterTerm.value = audioHash;
			}
			
			List<TranscriptWord> docs = searcherFacade.booleanSearch(searchTerm, filterTerm);
			resultDocs.addAll(docs);
		}

		TimeSpacedKmeans kmeans = new TimeSpacedKmeans();
		List<Cluster> clusters = kmeans.cluster(resultDocs);
		return getSearchResult(clusters, terms, audioHash);
	}
	
	
	public List<SearchResult> getSearchResult(List<Cluster> clusters, String[] terms , String audioHash) throws IOException, ParseException{
		
		Set<String> searchTerms = new HashSet<String>();
		for(String term: terms){
			searchTerms.add(term);
		}
		
		SearcherFacade searcherFacade = new SearcherFacade();
		List<SearchResult> searchResults = new ArrayList<SearchResult>();
		for(Cluster cluster: clusters) {
			Set<String> uniqueTerms = new HashSet<String>();
			Float minStartTime = Float.MAX_VALUE;
			Float maxEndTime = Float.MIN_VALUE;
			
			for(TranscriptWord doc: cluster.getMembers()) {
				// track terms found in the cluster for scoring
				if(searchTerms.contains(doc.getValue(TranscriptWordProp.WORD))) {
					uniqueTerms.add(doc.getValue(TranscriptWordProp.WORD));
				}
				
				//track range of time for the cluster for snippet selection
				Float startTime = new Float(doc.getValue(TranscriptWordProp.START_TIME));
				if(startTime < minStartTime) {
					minStartTime = startTime;
				}
				
				Float endTime = new Float(doc.getValue(TranscriptWordProp.END_TIME));
				if(endTime > maxEndTime) {
					maxEndTime = endTime;
				}
			}
			
			minStartTime = minStartTime - snippetWindow;
			maxEndTime = maxEndTime + snippetWindow;
			
			SearchTerm rangeStartTerm = new SearchTerm();
			rangeStartTerm.fieldName = TranscriptWordProp.START_TIME;
			rangeStartTerm.value = minStartTime.toString();
			
			SearchTerm rangeEndTerm = new SearchTerm();
			rangeEndTerm.fieldName = TranscriptWordProp.START_TIME;
			rangeEndTerm.value = maxEndTime.toString();
			
			SearchTerm filterTerm = null;
			if(audioHash!=null){
				filterTerm = new SearchTerm();
				filterTerm.fieldName = TranscriptWordProp.AUDIO_HASH;
				filterTerm.value = audioHash;
			}
			
			List<TranscriptWord> resultDocs = searcherFacade.rangeSearch(rangeStartTerm, rangeEndTerm, filterTerm);
			Collections.sort(resultDocs);
			StringBuffer sb = new StringBuffer();
			for(TranscriptWord resultDoc: resultDocs) {
				sb.append(resultDoc.getValue(TranscriptWordProp.WORD)).append(" ");
			}
			
			SearchResult result = new SearchResult();
			result.snippet = sb.toString(); 
			result.score = new Double(uniqueTerms.size()) / new Double(searchTerms.size());			
			searchResults.add(result);
		}
		
		return searchResults;
	}

	
}
