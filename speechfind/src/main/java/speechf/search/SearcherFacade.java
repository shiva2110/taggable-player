package speechf.search;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;



public class SearcherFacade {
	
	
	private static final String FSdir = "/projects/workrepo/speech-F/data/index/";
	private static final Logger logger = LoggerFactory.getLogger(SearcherFacade.class);
	

	public List<TranscriptWord> booleanSearch(SearchTerm searchTerm, SearchTerm filterTerm) throws IOException, ParseException {
		Directory directory = FSDirectory.open(new File(FSdir));
		
		String searchBooleanTerm = addAnd(searchTerm.value);
		
		
		IndexReader reader = IndexReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);
		QueryParser queryParser = new QueryParser(Version.LUCENE_36, searchTerm.fieldName.toString() , new StandardAnalyzer(Version.LUCENE_36, new HashSet<String>()));
		Query q = queryParser.parse(searchBooleanTerm);
		
		Filter filter = null;
		if(filterTerm!=null) {
			String filterBooleanTerm = addAnd(filterTerm.value);
			Query filterQ = new TermQuery(new Term(filterTerm.fieldName.toString(), filterBooleanTerm));
			filter = new QueryWrapperFilter(filterQ);
		}
		
		
		TopDocs docs = searcher.search(q, filter, Integer.MAX_VALUE);
		searcher.close();
		directory.close();
		
		ScoreDoc[] scoreDocs = docs.scoreDocs;
		List<TranscriptWord> transcriptWords = convertToTranscriptWordList(reader, scoreDocs);
		reader.close();
		return transcriptWords;
	}
	
	
	public List<TranscriptWord> rangeSearch(SearchTerm rangeStartTerm, SearchTerm rangeEndTerm, SearchTerm filterTerm) throws IOException, ParseException {
		
		if(!rangeStartTerm.fieldName.equals(rangeEndTerm.fieldName)) {
			throw new IllegalArgumentException("Range search is allowed only on the same fields!");
		}		
	
		
		Directory directory = FSDirectory.open(new File(FSdir));	
		
		IndexReader reader = IndexReader.open(directory);
		IndexSearcher searcher = new IndexSearcher(reader);		
		Query q = NumericRangeQuery.newFloatRange(rangeStartTerm.fieldName.toString(), 
				new Float(rangeStartTerm.value), 
				new Float(rangeEndTerm.value), true, true);
		
		Filter filter = null;
		if(filterTerm!=null) {
			String filterBooleanTerm = addAnd(filterTerm.value);
			Query filterQ = new TermQuery(new Term(filterTerm.fieldName.toString(), filterBooleanTerm));
			filter = new QueryWrapperFilter(filterQ);
		}
		
		
		TopDocs docs = searcher.search(q, filter, Integer.MAX_VALUE);
		searcher.close();
		directory.close();
		
		ScoreDoc[] scoreDocs = docs.scoreDocs;
		List<TranscriptWord> transcriptWords = convertToTranscriptWordList(reader, scoreDocs);
		reader.close();
		return transcriptWords;
	}
	
	private List<TranscriptWord> convertToTranscriptWordList(IndexReader reader, ScoreDoc[] scoreDocs) throws CorruptIndexException, IOException {
		List<TranscriptWord> transcriptWords = new ArrayList<TranscriptWord>();
		for(ScoreDoc scoreDoc : scoreDocs) {
			TranscriptWord transcriptWord = new TranscriptWord();
			Document doc = reader.document(scoreDoc.doc);
			List<Fieldable> fields = doc.getFields();
			for(Fieldable field: fields) {
				TranscriptWordProp  propKey = null;
				try {
					propKey = TranscriptWordProp.valueOf(field.name());
				} catch(Exception e) {
					logger.debug("transcript word propkey unidentified :" + field.name());					
				}
				if(propKey!=null) {
					transcriptWord.addProp(TranscriptWordProp.valueOf(field.name()), field.stringValue());
				}				
			}
	
			transcriptWords.add(transcriptWord);
		}
		return transcriptWords;
	}
	
	public String addAnd(String term){
		StringBuffer sb = new StringBuffer();		
		String[] terms = term.split(" ");		
		for(int i=0; i<terms.length-1; i++) {
			sb.append(terms[i]).append(" AND ");
		}		
		sb.append(terms[terms.length-1]);
		return sb.toString();
	}

	
}
