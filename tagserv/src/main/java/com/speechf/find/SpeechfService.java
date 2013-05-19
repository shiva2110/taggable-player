package com.speechf.find;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.lucene.index.CorruptIndexException;

import speechf.index.IndexerException;
import speechf.index.IndexerFacade;
import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;
import speechf.search.SearchTerm;
import speechf.search.SpeechFind;
import speechf.search.SpeechFind.ScoredTranscriptWord;

@Path("/")
public class SpeechfService {

	@POST
	@Path("/tag")
	@Consumes({"application/xml", "application/json"})
	public Response indexTag(Tag tag, 
			@QueryParam("domain") String domain,
			@QueryParam("mediaId") String mediaId){
		
		// form the unique id of the media to represent the index.
		mediaId = new StringBuffer("domain=").append(domain).append("&mediaId=").append(mediaId).toString();
		
		IndexerFacade indexer = new IndexerFacade();
		TranscriptWord transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, mediaId);
		System.out.println(mediaId);
		transcriptWord.addProp(TranscriptWordProp.WORD, tag.keywords);
		System.out.println(tag.keywords);
		transcriptWord.addProp(TranscriptWordProp.START_TIME, tag.startTime);
		System.out.println(tag.startTime);
		transcriptWord.addProp(TranscriptWordProp.END_TIME, tag.endTime);
		System.out.println(tag.endTime);
		
		try {
			indexer.index(transcriptWord);
			indexer.commit();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return Response.ok().build();
	}
	
	
	@GET
	@Path("/search")
	@Produces({"application/xml", "application/json"})
	public Response search(@QueryParam("keywords") String keywords,
			@QueryParam("domain") String domain,
			@QueryParam("mediaId") String mediaId){
		
		System.out.println("keywords:" + keywords + "; domain:" + domain + "; mediaId:"+ mediaId);
		SpeechFind speechFind = new SpeechFind();
		SearchTerm searchQ = new SearchTerm();
		searchQ.fieldName = TranscriptWordProp.WORD;
		searchQ.value = keywords;
		
		SearchTerm filterQ = new SearchTerm();
		filterQ.fieldName = TranscriptWordProp.MEDIA_ID;
		filterQ.value = new StringBuffer("domain=").append(domain).append("&mediaId=").append(mediaId).toString();
		
		SearchOutput[] searchOutputs = null;
		try {
			List<ScoredTranscriptWord> resultList = speechFind.find(searchQ, filterQ);
			System.out.println("result length:" +  resultList.size());
			searchOutputs = new SearchOutput[resultList.size()];
			for(int i=0; i<resultList.size(); i++) {
				ScoredTranscriptWord result = resultList.get(i);
				searchOutputs[i] = new SearchOutput(result.getStartTime(), result.getSnippet(), result.getScore());
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return Response.ok(searchOutputs).build();
		//return Response.ok(searchOutputs).header("Access-Control-Allow-Origin", "*").build();
	}
}
