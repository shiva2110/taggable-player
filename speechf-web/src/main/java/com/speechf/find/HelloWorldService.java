package com.speechf.find;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/search")
public class HelloWorldService {

	@GET
	@Path("/{param}")
	@Produces({"application/xml", "application/json"})
	public Response getMsg(@PathParam("param") String msg) {
		SearchInput searchInput = new SearchInput();
		searchInput.searchText = msg;
		
		System.out.println(msg);
		return Response.ok(searchInput).header("Access-Control-Allow-Origin", "*").build();
	}
	
}
