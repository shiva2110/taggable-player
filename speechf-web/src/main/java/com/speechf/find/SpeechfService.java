package com.speechf.find;

import java.io.IOException;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import speechf.index.IndexerException;
import speechf.index.IndexerFacade;
import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;

@Path("/")
public class SpeechfService {

	@POST
	@Path("/tag")
	@Consumes({"application/xml", "application/json"})
	public Response indexTag(Tag tag, 
			@QueryParam("sourceDomain") String domain,
			@QueryParam("mediaId") String mediaId){
		
		// form the unique id of the media to represent the index.
		mediaId = new StringBuffer("domain=").append(domain).append("&mediaId=").append(mediaId).toString();
		
		IndexerFacade indexer = new IndexerFacade();
		TranscriptWord transcriptWord = new TranscriptWord();
		transcriptWord.addProp(TranscriptWordProp.MEDIA_ID, mediaId);
		transcriptWord.addProp(TranscriptWordProp.WORD, tag.keywords);
		transcriptWord.addProp(TranscriptWordProp.START_TIME, tag.startTime);
		transcriptWord.addProp(TranscriptWordProp.END_TIME, tag.endTime);
		
		try {
			indexer.index(transcriptWord);
		} catch(IndexerException e){
			e.printStackTrace();
		} 
		
		return Response.ok().build();
	}
	
	
	@GET
	@Path("/search")
	@Produces({"application/xml", "application/json"})
	public Response search(@QueryParam("keywords") String keywords,
			@QueryParam("sourceDomain") String domain,
			@QueryParam("mediaId") String mediaId) {
		
		System.out.println("search keywords:" + keywords);
		
		SearchOutput[] searchOutputs = new SearchOutput[4];
		searchOutputs[0] = new SearchOutput("3", "you think you're white, you are black", 1.00);
		searchOutputs[1] = new SearchOutput("12", "black americans", 0.90);
		searchOutputs[2] = new SearchOutput("16", "black woman", 0.85);
		searchOutputs[3] = new SearchOutput("24", "native americans", 0.40);
		
		return Response.ok(searchOutputs).header("Access-Control-Allow-Origin", "*").build();
	}
}
