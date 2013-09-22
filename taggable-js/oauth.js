
var OAuthStates = {"DECLINED":"declined", "ACCEPTED":"accepted", "WAITING":"waiting"};



function requestUserID(OAuthProp){
	OAuthProp["OAuthState"] = OAuthStates.WAITING;
	
	//make a post request to google open auth
	var stateID = Math.random();
	window.open(formOAuthURL(stateID));
	console.log("continuing to fetch auth token from cache server");
	OAuthProp["OAuthCallCounter"] = 0;
	OAuthProp["OAuthIntervalId"] = setInterval(function(){getOpenAuthID(stateID, OAuthProp);}, 10000);
}


function getOpenAuthID(stateID, OAuthProp){
	OAuthProp["OAuthCallCounter"] =  OAuthProp["OAuthCallCounter"] + 1;
	if(OAuthProp["OAuthCallCounter"]>=30){
		console.log("stopping fetch oauth calls");
		stopOAuthIdCalls(OAuthProp);
		return;
	}
	
	console.log("making fetch oauth call");
	var posting = $.ajax({
		url: formFetchTokenURL(),
		type: "POST", 
		data: "{\"clientKey\":\"" + stateID +"\"}",
		headers : {
			"Accept" : "*",
			"Content-Type" : "application/json"
		},
		success: function(json) {
			console.log("got proper response while fetching cached auth token");
			if(json!=null){
				stopOAuthIdCalls(OAuthProp);
				if(json.isDeclined){
					OAuthProp["OAuthState"] = OAuthStates.DECLINED;
					console.log("declined");
				} else {
					OAuthProp["OAuthState"] = OAuthStates.ACCEPTED;
					getGoogleId(json.authToken);
				}			
			}
		},
		error: function(json){
			console.log("error while fetching cached auth token");
			stopOAuthIdCalls(OAuthProp);
		}
	});	
}

function getGoogleId(accessToken){
	var posting = $.ajax({
		url: formGoogleIdReqURL(accessToken),
		type: "GET",
		headers : {
			"Accept" : "*"
		},
		success: function(json) {
			console.log("got proper response while fetching google id");
			if(json!=null){
				console.log("google id:" + json.email);	
				postIdFetchCallBack(json.email);
			}
		},
		error: function(json){
			console.log("error while fetching google id");
		}
	});	
}

//stop making further calls
function stopOAuthIdCalls(OAuthProp){
	var intervalId;
	if((intervalId=OAuthProp["OAuthIntervalId"])!=undefined){
		window.clearInterval(intervalId);
	}
}

function postIdFetchCallBack(userId){
	//set propsMap["userId"]
	globalPropsMap["userId"] = userId;
	var superParents = $(document).find(".taggable-container");
	
	if(globalPropsMap["sessionId"]!=undefined){
		//update all indexes with sessionId: replace sessionId with userId.
		
		for(var i=0; i<superParents.length; i++){
			var superParent = $(superParents[i]);
			var propsMap = globalPropsMap[superParent.attr("id")];
			
			var contentToUpdate  =  "{\"userId\":\"" + globalPropsMap["sessionId"] + "\"}";
			
			var newContent =  "{\"userId\":\"" + globalPropsMap["userId"] + "\"}";
			
			var posting = $.ajax({
				url: formUpdateIndexURL(indexURI, propsMap.domain, propsMap.mediasrc),
				data: "[" + contentToUpdate + "," + newContent + "]",
				type: "POST", 
				headers : {
					"Accept" : "*",
					"Content-Type" : "application/json"
				}
			});
		}
		
	}
	
	//call getAllIndexTags and getAllContentTags	
	for(var i=0; i<superParents.length; i++){
		var superParent = $(superParents[i]);
		var propsMap = globalPropsMap[superParent.attr("id")];
		if(propsMap.indexTagState == undefined){
			loadIndexTags(superParent.find(".controls-base"), propsMap); //TBD - superParent is not accessible from here.
			propsMap.indexTagState = "loaded";
		}
		getArchivedContentTags(propsMap);
	}
}

function getComputedUserId(){
	if(globalPropsMap["userId"]!=undefined){
		return globalPropsMap["userId"];
	}
	
	if(globalPropsMap["sessionId"]==undefined){
		globalPropsMap["sessionId"] = Math.random().toString();
	}
	
	return globalPropsMap["sessionId"];
}

function formGoogleIdReqURL(accessToken){
	return "https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + accessToken;
}



function formOAuthURL(state){
	return "https://accounts.google.com/o/oauth2/auth?" +
		"scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.email+https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile&" +
		"state=" + state + "&" + 
		"redirect_uri=" + formOAuthRedirectURL() + "&" +
		"response_type=token&" +
		"client_id=71663558295.apps.googleusercontent.com";
}

function formOAuthRedirectURL(){
	return "http%3A%2F%2Fec2-54-235-225-226.compute-1.amazonaws.com%2Foauth2callback";
}

function formFetchTokenURL(){
	return indexURI + "/getOAuthToken";
}