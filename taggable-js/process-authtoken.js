

var indexURI = "http://ec2-54-235-225-226.compute-1.amazonaws.com/speechf-web";

$(window).load(function() {
	// First, parse the query string
	var params = {}, queryString = location.hash.substring(1),
	    regex = /([^&=]+)=([^&]*)/g, m;
	while (m = regex.exec(queryString)) {
	  params[decodeURIComponent(m[1])] = decodeURIComponent(m[2]);
	}
	

	console.log("The access token: " + params['access_token']);
	console.log("The browser key: " + params['state']);
	
	var isDeclined = false;
	if(params["error"]!=undefined && params["error"]=="access_denied"){
		isDeclined = true;
	}
	makeCacheTokenRequest(params['state'], params['access_token'], isDeclined);	
});

function makeCacheTokenRequest(clientKey, authToken, isDeclined){
	var posting = $.ajax({
		url: formCacheTokenURL(indexURI),
		data: "{\"clientKey\":\"" + clientKey +"\", \"authToken\":\"" + authToken + "\", \"isDeclined\":" + isDeclined + "}",
		type: "POST", 
		headers : {
			"Accept" : "*",
			"Content-Type" : "application/json"
		},
		success: function(json){
			console.log("done with caching");
			//makeFetchTokenRequest(clientKey);
		},
		complete: function() {
			window.close();
		}
	});
}

function makeFetchTokenRequest(clientKey){
	var posting = $.ajax({
		url: formFetchTokenURL(indexURI),
		type: "POST", 
		data: "{\"clientKey\":\"" + clientKey +"\"}",
		headers : {
			"Accept" : "*",
			"Content-Type" : "application/json"
		},
		success: function(json) {
			console.log("fetched client key:" + json.clientKey);
			console.log("fetched auth token:" + json.authToken);
			console.log("fetched error:" + json.isDeclined);
		},
		error: function(json){
			console.log("error while fetching cached auth token");
		}
	});		
}



function formCacheTokenURL(indexURI) {
	return indexURI + "/cacheOAuthToken";
}

function formFetchTokenURL(indexURI) {
	return indexURI + "/getOAuthToken";
}


