package speechf.search;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.queryParser.ParseException;
import org.junit.Test;

public class SpeechFindTest {
	
	@Test
	public void testFind() throws IOException, ParseException {
		SpeechFind finder = new SpeechFind();			
		List<SearchResult> searchResults = finder.find("hello find speek", "444");
		
		for(SearchResult result: searchResults) {
			System.out.println("---");
			System.out.println("snippet: " + result.snippet);
			System.out.println("score: " + result.score);
		}
	}
	
	@Test
	public void testFind_noFilter() throws IOException, ParseException {
		SpeechFind finder = new SpeechFind();			
		List<SearchResult> searchResults = finder.find("hello find speek", null);
		
		for(SearchResult result: searchResults) {
			System.out.println("---");
			System.out.println("snippet: " + result.snippet);
			System.out.println("score: " + result.score);
		}
	}
	
	@Test
	public void testFind1() throws IOException, ParseException {
		SpeechFind finder = new SpeechFind();			
		List<SearchResult> searchResults = finder.find("black woman jaywalking", "-762350663");
		
		for(SearchResult result: searchResults) {
			System.out.println("---");
			System.out.println("snippet: " + result.snippet);
			System.out.println("score: " + result.score);
		}
	}
}
