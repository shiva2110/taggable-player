package speechf.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import speechf.index.IndexerFacade;
import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;

public class SearcherFacadeTest {

	SearcherFacade searcherFacade = new SearcherFacade();
	
	@Test
	public void booleanSearch_nullReq_emptyResp() throws Exception{
		List<TranscriptWord> list = searcherFacade.booleanSearch(null, null);
		assertNotNull("booleanSearch() response must not be null", list);
		assertEquals("booleanSearch() must return empty if input search is null", 0, list.size());
	}
	
	@Test
	public void booleanSearch_validReqNullFilter_validResp() throws Exception{
		SearchTerm searchTerm = new SearchTerm();
		searchTerm.fieldName = TranscriptWordProp.WORD;
		searchTerm.value = "java project";
		
		List<TranscriptWord> list = searcherFacade.booleanSearch(searchTerm, null);
		
		int resultCountExpected = 3;
		assertNotNull("booleanSearch() response must not be null", list);
		assertTrue("booleanSearch() must return empty if input search is null", (list.size()==resultCountExpected));
		
		for(TranscriptWord wordObj: list){
			String term = wordObj.getValue(TranscriptWordProp.WORD);
			if(!term.contains("java") || !term.contains("project")) {
				fail("findAll() returned invalid result. The search terms were 'java project'. The returned result: " + term);
			}
		}
	}
	
	@Test
	public void booleanSearch_validReq_validResp() throws Exception{
		SearchTerm searchTerm = new SearchTerm();
		searchTerm.fieldName = TranscriptWordProp.WORD;
		searchTerm.value = "java project";
		
		SearchTerm filterTerm = new SearchTerm();
		filterTerm.fieldName = TranscriptWordProp.MEDIA_ID;
		filterTerm.value = new StringBuffer("domain=").append("youtube.com").append("&mediaId=").append("abcde").toString();
		
		int resultCountExpected = 2;
		List<TranscriptWord> list = searcherFacade.booleanSearch(searchTerm, filterTerm);
		assertNotNull("booleanSearch() response must not be null", list);
		assertEquals("booleanSearch() must return empty if input search is null", resultCountExpected,list.size());
		
		for(TranscriptWord wordObj: list){
			String mediaId = wordObj.getValue(TranscriptWordProp.MEDIA_ID);
			if(!mediaId.equals(filterTerm.value)) {
				fail("findAll() did not filter the results properly. The filter term was" + filterTerm.value + "The returned result contains: " + mediaId);
			}
			
			String word = wordObj.getValue(TranscriptWordProp.WORD);
			if(!word.contains("java") || !word.contains("project")) {
				fail("booleanSearch() did not return all results");
			}
			
		}
	}
	
	
	@BeforeClass
	public static void index() throws Exception {
	/*	IndexerFacade indexerFacade = new IndexerFacade();
		
		TranscriptWord word = new TranscriptWord();
		word.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=abcde");
		word.addProp(TranscriptWordProp.WORD, "This is a project based out of java");		
		indexerFacade.index(word);
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=ecghuy");
		word.addProp(TranscriptWordProp.WORD, "This project uses java, jquery, html5 etc.");		
		indexerFacade.index(word);
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=abcde");
		word.addProp(TranscriptWordProp.WORD, "java and rest services based project");		
		indexerFacade.index(word);
		
		word = new TranscriptWord();
		word.addProp(TranscriptWordProp.MEDIA_ID, "domain=youtube.com&mediaId=abcde");
		word.addProp(TranscriptWordProp.WORD, "java is also a coffee");		
		indexerFacade.index(word);
		
		indexerFacade.close(); */
	}
	
	
	@Test
	public void vectorSearch_validReq_validResp() throws Exception {
		SearchTerm searchTerm = new SearchTerm();
		searchTerm.fieldName = TranscriptWordProp.WORD;
		searchTerm.value = "java project";
		
		SearchTerm filterTerm = new SearchTerm();
		filterTerm.fieldName = TranscriptWordProp.MEDIA_ID;
		filterTerm.value = new StringBuffer("domain=").append("youtube.com").append("&mediaId=").append("abcde").toString();
		
		SearcherFacade searcherFacade = new SearcherFacade();
		List<TranscriptWord> result = searcherFacade.vectorSearch(searchTerm, filterTerm);
		
		int resultCountExpected = 3;
		assertNotNull("vectorSearch() response must not be null", result);
		assertTrue("vectorSearch() did not return all results", (result.size()==resultCountExpected));
		
		for(TranscriptWord word: result) {
			
			if(!word.getValue(TranscriptWordProp.MEDIA_ID).equals(filterTerm.value)) {
				fail("filtering did not work");
			}
			
			if(!word.getValue(TranscriptWordProp.WORD).contains("java")  &&  
					!word.getValue(TranscriptWordProp.WORD).contains("project")) {
				fail("Invalid search response returned");
			}
		}
	}
	
	@Test
	public void vectorSearch_nullReq_emptyResp() throws Exception{
		List<TranscriptWord> list = searcherFacade.vectorSearch(null, null);
		assertNotNull("vectorSearch() response must not be null", list);
		assertEquals("vectorSearch() must return empty if input search is null", 0, list.size());
	}
	
	@Test
	public void vectorSearch_validReqNullFilter_validResp() throws Exception{
		SearchTerm searchTerm = new SearchTerm();
		searchTerm.fieldName = TranscriptWordProp.WORD;
		searchTerm.value = "java project";
		
		List<TranscriptWord> list = searcherFacade.vectorSearch(searchTerm, null);
		
		int resultCountExpected = 4;
		assertNotNull("vectorSearch() response must not be null", list);
		assertTrue("vectorSearch() did not return all results", (list.size()==resultCountExpected));
		
		for(TranscriptWord wordObj: list){
			String term = wordObj.getValue(TranscriptWordProp.WORD);
			if(!term.contains("java") && !term.contains("project")) {
				fail("findAll() returned invalid result. The search terms were 'java project'. The returned result: " + term);
			}
		}
	}
	
	@Test
	public void vectorSearch_unavailableFilter_emptyResp() throws Exception {
		SearchTerm searchTerm = new SearchTerm();
		searchTerm.fieldName = TranscriptWordProp.WORD;
		searchTerm.value = "java project";
		
		SearchTerm filterTerm = new SearchTerm();
		filterTerm.fieldName = TranscriptWordProp.MEDIA_ID;
		filterTerm.value = new StringBuffer("domain=").append("bestbuy.com").append("&mediaId=").append("abcde").toString();
		
		SearcherFacade searcherFacade = new SearcherFacade();
		List<TranscriptWord> result = searcherFacade.vectorSearch(searchTerm, filterTerm);
		
		assertEquals("vectorSearch() must return empty if input filter is unavailable", 0, result.size());
		
	}
	
	
}
