package speechf.main;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

public class HelperTest {
	@Test
	public void tokenize_validReq_validResp1() throws Exception {
		List<String> list = Helper.tokenize("jQuery is a fast, small, and feature-rich JavaScript library. It makes things like HTML document traversal and manipulation, event handling, animation, and Ajax much simpler with an easy-to-use API that works across a multitude of browsers. With a combination of versatility and extensibility, jQuery has changed the way that millions of people write JavaScript.", false, true);
		assertEquals("tokenizer did not work", 
				"[jquery, is, a, fast, small, and, feature, rich, javascript, library, it, makes, things, like, html, document, traversal, and, manipulation, event, handling, animation, and, ajax, much, simpler, with, an, easy, to, use, api, that, works, across, a, multitude, of, browsers, with, a, combination, of, versatility, and, extensibility, jquery, has, changed, the, way, that, millions, of, people, write, javascript]", 
				list.toString());
	}
	
	
	@Test
	public void tokenize_nullReq_emptyResp() throws Exception{		
		List<String> arr = Helper.tokenize(null, true, true);
		assertNotNull("tokenize() response must not be null", arr);
		assertEquals("For a null input tokenize must return empty", 0, arr.size());
	}
	
	@Test
	public void tokenize_onlyStopWordReq_emptyResp() throws Exception{		
		List<String> arr = Helper.tokenize("is was a", true, true);
		assertNotNull("tokenize() response must not be null", arr);
		assertEquals("For only stop words input tokenize must return empty", 0, arr.size());
	}
	
	@Test
	public void tokenize_validReq_validResp() throws Exception{		
		List<String> arr = Helper.tokenize("is java project was a", true, true);
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
	
}