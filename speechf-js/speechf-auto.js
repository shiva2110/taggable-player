
var speechfErr  = "The speechf-controls class not following expected syntax. " +
"Example syntax: class='mediasrc:<source of media file> indexsrc:<http link to index>";



var progressBarHeight = 13;
var progressSliderWidth = 17;
var defaultMediaWidth = 300;
var currentSearchQuery = "";
var currentIndexTime = "";
var indexSpan =  15; //15 sec
var indexURI = "http://localhost:8080/speechf-web";
var globalPropsMap = new Object();
var resultColorMap = new Array();
resultColorMap[0] = "#FF0000";
resultColorMap[1] = "#FF1919";
resultColorMap[2] = "#FF3333";
resultColorMap[3] = "#FF6666";
resultColorMap[4] = "#FF4D4D";
resultColorMap[5] = "#FF6666";
resultColorMap[6] = "#FF8080";
resultColorMap[7] = "#FF9999";
resultColorMap[8] = "#FFB2B2";
resultColorMap[9] = "#FFCCCC";

$(window).load(function() {

	$(".speechf-controls").each(function() {
		var titleText = $(this).attr("data-props");
		//$(this).attr("title", "");
		var props = titleText.split(" ");
		if(!props.length>=3) {
			throw speechfErr;
		}

		var propsMap = new Object();

		for(i=0; i<props.length;i++) {
			if(props[i].indexOf("mediasrc:")>=0){
				var src = props[i].split("mediasrc:");
				if(src.length==2 && (src[1]!="" || src[1].length!=0)){
					propsMap["mediasrc"] = src[1];
				} else {
					throw speechfErr;
				}
			} else if(props[i].indexOf("mediaId:")>=0) {
				var src = props[i].split("mediaId:");
				if(src.length==2 && (src[1]!="" || src[1].length!=0)){
					propsMap["mediaId"] = src[1];
				} else {
					throw speechfErr;
				}
			} else if(props[i].indexOf("domain:")>=0) {
				var src = props[i].split("domain:");
				if(src.length==2 && (src[1]!="" || src[1].length!=0)){
					propsMap["domain"] = src[1];
				} else {
					throw speechfErr;
				}
			} else if(props[i].indexOf("mediatype:")>=0) {
				var mediaType = props[i].split("mediatype:");
				if(mediaType.length==2 && (mediaType[1]!="" || mediaType[1].length!=0)){
					propsMap["mediatype"] = mediaType[1];
				} else {
					throw speechfErr;
				}				
			} else if(props[i].indexOf("width:")>=0) {
				var width = props[i].split("width:");
				if(width.length==2 && (width[1]!="" || width[1].length!=0)){
					propsMap["width"] = width[1];
				} else {
					throw speechfErr;
				}				
			}
		}

		if(propsMap["mediasrc"].length==0 || propsMap["domain"].length==0 || propsMap["mediaId"].length==0 || propsMap["mediatype"].length==0) {
			throw speechfErr;
		}

		globalPropsMap[titleText] = propsMap;

		var mediaElm = createMediaElement(this, propsMap);
		var progressBar = createProgressBar(this, propsMap);
		var controlsBase = createControlsBase(this, propsMap);	
		createPlayButton(controlsBase);
		createProgressSlider(progressBar);	
		createSearchBox(controlsBase);
	});


	$(".progress-bar").mousemove(function(e){
		var mouseX = e.pageX;		
		var progressSlider = getNearbyElement(".speechf-progressSlider", $(this));
		var superParent = $(this).parents(".speechf-controls");
		var propsMap = globalPropsMap[superParent.attr("data-props")];
		var initPos = propsMap["progressSliderPos"];
		progressSlider.css("left", (mouseX-initPos));
		progressSlider.show();

	});

	$(".progress-bar").mouseover(function(e){
		var className = e.srcElement.className;
		if(className.indexOf("speechfresult")!=-1){
			var snippetClass = snippetClassNameSelector(className);
			if(snippetClass==undefined || snippetClass==null) {
				return;
			}

			// hide all other snippets
			var snippetArr  = getNearbyElement("[class|='speechfsnippet']", $(this));
			if(snippetArr!=undefined){
				if(snippetArr.length==undefined) {
					$(snippetArr).hide();
				} else {
					for(var i=0; i<snippetArr.length; i++) {
						$(snippetArr[i]).hide();
					}
				}
			}

			var snippet =  getNearbyElement(snippetClass, $(this));
			$(snippet).show();
		}
	});

	$(".progress-bar").mouseout(function(e){
		var progressSlider = getNearbyElement(".speechf-progressSlider", $(this));
		progressSlider.hide();
		
		// hide all other snippets
		var snippetArr  = getNearbyElement("[class|='speechfsnippet']", $(this));
		if(snippetArr!=undefined){
			if(snippetArr.length==undefined) {
				$(snippetArr).hide();
			} else {
				for(var i=0; i<snippetArr.length; i++) {
					$(snippetArr[i]).hide();
				}
			}
		}
		
	});

	$(".progress-bar").click(function(e){
		var mouseX = e.pageX;		
		var progressSlider = getNearbyElement(".speechf-progressSlider", $(this));
		var superParent = $(this).parents(".speechf-controls");
		var propsMap = globalPropsMap[superParent.attr("data-props")];		
		var initPos = propsMap["progressSliderPos"];
		var newPos = (mouseX-initPos);
		
		var progressBarWidth = propsMap["width"];
		var percentage = newPos/progressBarWidth;

		var mediaElement = getNearbyMediaElement($(this));	

		if(!mediaElement) {
			return;
		}

		var mediaDuration =  $(mediaElement)[0].duration;
		$(mediaElement)[0].currentTime = (percentage*mediaDuration);

		$(".speechf-searchBox").show("slow");

	});	

	$(".speechf-playbutton").click(function() {	

		//if this component is not speechf component, then return back.
		if(!isSpeechfComponent($(this))) {
			return;
		}

		var src = $(this).prop("src");
		var mediaElement = getNearbyMediaElement($(this));	

		if(!mediaElement) {
			return;
		}

		if(endsWith(src, "play.png")) {
			$(this).prop("src",  "pause.png");
			mediaElement[0].play();
		} else {
			$(this).prop("src",  "play.png");
			mediaElement[0].pause();
		}		
	});

	$(".speechf-writeButton").mousemove(function() {
		if($(this).css("background").indexOf("write3")!=-1) {
			$(this).css("background", "url(writeButton.png) no-repeat top left");
		}
	});

	$(".speechf-writeButton").mouseout(function() {
		if($(this).css("background").indexOf("writeButton")!=-1) {
			$(this).css("background", "url(write3.png) no-repeat top left");
		}
	});

	$(".speechf-writeButton").click(function() {
		// change to write clicked
		$(this).css("background", "url(writeClicked.png) no-repeat top left");

		//enable search toggle
		var searchButton =  getNearbyElement(".speechf-searchButton", $(this));
		searchButton.css("background", "url(search2.png) no-repeat center left");

		var textBox =  getNearbyElement(".speechf-searchText", $(this));

		//capture searchQuery if any
		if(textBox[0].value.length!=0 && textBox[0].value.indexOf("Type what is going on")==-1) {
			currentSearchQuery = textBox[0].value;
		} else {
			currentSearchQuery = "";
		}

		//display default text
		var mediaElement = getNearbyMediaElement($(this));	
		var currentTime = $(mediaElement)[0].currentTime;
		currentIndexTime = currentTime;
		var currentTimeMin = getMinutes(currentTime);	
		displayTextForWrite(currentTimeMin, textBox);	

		//hide search results and snippets
		var results = getNearbyElement("[class|='speechfresult']", $(this));
		var snippets = getNearbyElement("[class|='speechfsnippet']", $(this));

		for(i=0; i<results.length; i++) {
			var jObj = $(results[i]);
			jObj.hide();
		}
		for(i=0; i<snippets.length; i++) {
			var jObj = $(snippets[i]);
			jObj.hide();
		}

		// remove earlier write index time bar if any
		var writeBars = getNearbyElement(".speechf-writebar", $(this));
		for(i=0; i<writeBars.length; i++) {
			var jObj = $(writeBars[i]);
			jObj.remove();
		}

		var indexedMes = getNearbyElement(".speechf-indexedMes", $(this));
		for(i=0; i<indexedMes.length; i++) {
			var jObj = $(indexedMes[i]);
			jObj.remove();
		}

		//show write index time bar
		var percentage = currentTime/$(mediaElement)[0].duration;
		var superParent = $(this).parents(".speechf-controls");
		var propsMap = globalPropsMap[superParent.attr("data-props")];
		var progressBarWidth = propsMap["width"];
		var left = percentage * (progressBarWidth);

		percentage = indexSpan/$(mediaElement)[0].duration;			 
		var width = percentage * (progressBarWidth);

		if((left+width) > progressBarWidth) {
			width = progressBarWidth - left;
		}
		var writeBar = $("<div class='speechf-writebar' style='height:" + progressBarHeight + "; width:" + width + "; background-color:#FF0000; opacity:0.4; left:" + left + "; float:left; position:absolute'></div>");
		writeBar.appendTo(getNearbyElement(".progress-bar", $(this)));

	});

	$(".speechf-searchButton").mousemove(function() {
		if($(this).css("background").indexOf("search2")!=-1) {
			$(this).css("background", "url(searchButton.png) no-repeat top left");
		}
	});

	$(".speechf-searchButton").mouseout(function() {
		if($(this).css("background").indexOf("searchButton")!=-1) {
			$(this).css("background", "url(search2.png) no-repeat center left");
		}
	});

	$(".speechf-searchButton").click(function() {	
		if($(this).css("background").indexOf("searchButton")!=-1) {
			//change to search clicked
			$(this).css("background", "url(searchClicked.png) no-repeat top left");

			//enable write toggle
			var writeButton =  getNearbyElement(".speechf-writeButton", $(this));
			writeButton.css("background", "url(write3.png) no-repeat top left");

			//remove write bars if any
			var writeBars = getNearbyElement(".speechf-writebar", $(this));
			for(i=0; i<writeBars.length; i++) {
				var jObj = $(writeBars[i]);
				jObj.remove();
			}		

			var indexedMes = getNearbyElement(".speechf-indexedMes", $(this));
			for(i=0; i<indexedMes.length; i++) {
				var jObj = $(indexedMes[i]);
				jObj.remove();
			}

			//display default text
			var textBox =  getNearbyElement(".speechf-searchText", $(this));
			if(currentSearchQuery.length!=0) {
				textBox[0].value = currentSearchQuery;
			} else {
				displayTextForSearch(textBox);	
			}

			//show earlier search results and snippets
			var results = getNearbyElement("[class|='speechfresult']", $(this));

			for(i=0; i<results.length; i++) {
				var jObj = $(results[i]);
				jObj.show();
			}
		}
	});

	$(".progress-bar").dblclick(function() {
		var writeButton =  getNearbyElement(".speechf-writeButton", $(this));
		writeButton.click();
	});

	$(".speechf-audio").each(function(){
		addMediaEvents(this);
	});


	$(".speechf-video").each(function(){
		addMediaEvents(this);
	});


	$(".speechf-searchText").keypress(function(e) {
		if(e.which==13) { // when the keypress in "ENTER"
			var searchButton = getNearbyElement(".speechf-searchButton", $(this));
			var writeButton = getNearbyElement(".speechf-writeButton", $(this));
			var progressBar = getNearbyElement(".progress-bar", $(this));

			if(searchButton.css("background").indexOf("searchClicked")!=-1) { 

				// First remove all earlier search results and snippets
				var results = getNearbyElement("[class|='speechfresult']", $(this));
				var snippets = getNearbyElement("[class|='speechfsnippet']", $(this));

				for(i=0; i<results.length; i++) {
					var jObj = $(results[i]);
					jObj.remove();
				}
				for(i=0; i<snippets.length; i++) {
					var jObj = $(snippets[i]);
					jObj.remove();
				}

				// Search and display results
				var keywords = $(this)[0].value;
				if(keywords.length==0 || keywords==""){
					return;
				}

				var superParent = $(this).parents(".speechf-controls");
				var propsMap = globalPropsMap[superParent.attr("data-props")];

				$.support.cors = true;
				var textBox = $(this);
				var searchCall = $.ajax({
					url: formSearchURL(indexURI, propsMap["domain"], propsMap["mediaId"], keywords),
					type: "GET", 
					headers : {
						"Accept" : "application/json"
					},
					success: function(json) {
						searchSuccessCallback(json,textBox);
					},
					error: function(json){
						searchSuccessCallback(json, textBox);
					}
				});

			} else if(writeButton.css("background").indexOf("writeClicked")!=-1) {

				// prepare for indexing
				if(currentIndexTime.length==0) {
					return;
				}

				//Call the service for indexing
				//$.support.cors = true;

				var endTime =  (currentIndexTime+indexSpan);
				var keywords = $(this)[0].value;
				if(keywords.length==0 || keywords==""){
					return;
				}

				var superParent = $(this).parents(".speechf-controls");
				var propsMap = globalPropsMap[superParent.attr("data-props")];
				var posting = $.ajax({
					url: formIndexURL(indexURI, propsMap["domain"], propsMap["mediaId"]),
					data: "{\"keywords\":\"" + keywords +"\", \"startTime\":\"" +currentIndexTime + "\", \"endTime\":\"" + endTime + "\"}",
					type: "POST", 
					headers : {
						"Accept" : "*",
						"Origin" : "null",
						"User-Agent":	"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_5) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.65 Safari/537.31",
						"Content-Type" : "application/json",
						"Accept-Encoding" :	"gzip,deflate,sdch",
						"Accept-Language" :	"en-US,en;q=0.8",
						"Accept-Charset" :	"ISO-8859-1,utf-8;q=0.7,*;q=0.3"
					},
					success: indexSuccessCallback($(this), progressBar)
				});
			}	
		} else {

		}
	});



	// have search button clicked as default
	$(".speechf-searchButton").click(); 
});


function snippetSelector(resultObj) {
	var className = resultObj.attr("class");
	var index = className.charAt(className.length-1);
	var numPattern = /[0-9]/g;
	var match = numPattern.exec(className);
	if(match==null){
		return null;
	}

	var snippetClassName = ".speechfsnippet-" + match;
	var snippet = getNearbyElement(snippetClassName, resultObj);
	if(snippet==undefined){
		return null;
	}

	return snippet;
}


function snippetClassNameSelector(resultClassName){
	var index = resultClassName.charAt(resultClassName.length-1);
	var numPattern = /[0-9]/g;
	var match = numPattern.exec(resultClassName);
	if(match==null){
		return null;
	}

	var snippetClassName = ".speechfsnippet-" + match;
	return snippetClassName;
}

function formIndexURL(indexURI, domain, mediaId) {
	return indexURI + "/tag?domain=" + domain + "&mediaId=" + mediaId;
}

function formSearchURL(indexURI, domain, mediaId, keywords) {
	return indexURI + "/search?domain=" + domain + "&mediaId=" + mediaId + "&keywords=" + keywords;
}


function searchSuccessCallback(json, textBox) {

	var searchOutput = json.searchOutput;
	var outStr = "";
	var progressBar = getNearbyElement(".progress-bar", textBox);
	var mediaElm = getNearbyMediaElement(textBox);	

	if(searchOutput.length==undefined) {
		buildSearchResult(progressBar, searchOutput, 0, mediaElm);
	} else {
		var max = 10;
		if(searchOutput.length<10) {
			max = searchOutput.length;
		}
		for(i=0;i<max; i++) {
			buildSearchResult(progressBar, searchOutput[i], i, mediaElm);			
		}		
	}
}

function buildSearchResult(progressBar, searchOutput, resultIndex, mediaElm) {
	var resultSnippet = searchOutput.snippet;
	var percentage = (searchOutput.time)/($(mediaElm)[0].duration);
	var superParent = progressBar.parents(".speechf-controls");
	var propsMap = globalPropsMap[superParent.attr("data-props")];
	var progressBarWidth =  propsMap["width"];
	var resultLeftPos = percentage * (progressBarWidth);

	var resultColor = "#FF8080";
	if(resultIndex<resultColorMap.length){
		resultColor = resultColorMap[resultIndex];
	}
	var result = $("<div class='speechfresult-" + resultIndex + "' style='height:" + progressBarHeight + "; width:10px; background-color:" + resultColor + "; left:" + resultLeftPos +  "; float:left; opacity:0.8; position:absolute;'></div>");


	result.appendTo(progressBar);

	var snippetWidth = 90;
	var snippetHeight = 30;

	var snippet = $("<div class='speechfsnippet-" + resultIndex +"' style='background: url(snippet.png) no-repeat top left; border:none; position:absolute; " +
			"height:" + snippetHeight + "; font-size:x-small; font-family:Arial, Helvetica, sans-serif; z-index:2; width:" + snippetWidth + "; display:none;'>" + resultSnippet + "</div>");
	
	
	var top = progressBar.position().top -32;
	var left = result.position().left - 30;

	if((left + snippetWidth)>progressBarWidth) {
		left = left - (left + snippetWidth - progressBarWidth) - 2;
	} else if (left<0) {
		left = 2;
	}
	snippet.css("top", top);
	snippet.css("left", left);				
	progressBar.before(snippet);
}


function indexSuccessCallback(textBox, progressBar) {
	//after indexing, remove the text
	var currentTimeMin = getMinutes(currentIndexTime);	
	displayTextForWrite(currentTimeMin, textBox);	

	//after indexing, show successful indexed message
	var writeBars = getNearbyElement(".speechf-writebar", textBox);
	var snippetWidth = 50;
	var snippetHeight = 10;
	var snippet = $("<div class='speechf-indexedMes' style='background: url(snippet.png) no-repeat top left; border:none; position:absolute; " +
			"height:" + snippetHeight + "; font-size:x-small; font-family:Arial, Helvetica, sans-serif; z-index:2; width:" + snippetWidth + "'>Indexed!</div>");
	var top = progressBar.position().top - 10;
	var left = $(writeBars[0]).position().left + 10;
	var superParent = progressBar.parents(".speechf-controls");
	var propsMap = globalPropsMap[superParent.attr("data-props")];
	var progressBarWidth = propsMap["width"];
	if((left + snippetWidth)>progressBarWidth) {
		left = left - (left + snippetWidth - progressBarWidth) - 2;
	} else if (left<0) {
		left = 2;
	}
	snippet.css("top", top);
	snippet.css("left", left);				
	progressBar.before(snippet);	
	snippet.fadeIn(2000);
	snippet.fadeOut(2000);		
}

function displayTextForSearch(textBox) {
	textBox[0].value = "Enter your search terms here";
	textBox.focus();
	textBox.select();
}

function displayTextForWrite(currentTime, textBox) {	
	textBox[0].value = "Type what is going on at " + currentTime + "m";
	textBox.focus();
	textBox.select();
}

function getMinutes(sec) {
	sec = sec/60;
	sec = sec.toFixed(2);
	return sec;
}

function getWriteIconBg(){
	return "url(write3.png) no-repeat top left";
}

function getSearchIconBg() {
	return "url(search2.png) no-repeat center left";
}

function addMediaEvents(elm) {
	$(elm)[0].addEventListener("ended", function(){
		var playButton = getNearbyElement(".speechf-playbutton", $(elm))
		playButton.prop("src",  "play.png");
	});

	$(elm)[0].addEventListener("timeupdate", function(){
		var mediaDuration =  $(elm)[0].duration;
		var currentTime = $(elm)[0].currentTime;
		var percentage = currentTime/mediaDuration;	
		var progressBar = getNearbyElement(".progress-bar", $(elm));			
		var superParent = $(this).parents(".speechf-controls");
		var propsMap = globalPropsMap[superParent.attr("data-props")];
		var progressBarWidth = propsMap["width"];
		var borderWidth = (percentage*progressBarWidth);
		progressBar.css("border-left-width" , borderWidth);
		progressBar.css("border-left-style", "solid");
		progressBar.css("border-left-color", "#FCFCFC");

		progressBar.css("width" , (progressBarWidth-borderWidth));	
	});
}

function getNearbyElement(className, elm) {
	var superparent = elm.parents(".speechf-controls");
	return superparent.find(className);
}

function getNearbyMediaElement(elm) {
	var mediaElement = getNearbyElement("audio", elm);	
	if(mediaElement.length==0) {
		mediaElement = getNearbyElement("video", elm);	
	}

	if(mediaElement.length==0) {
		return;
	}

	return mediaElement;
}


function isSpeechfComponent(elm) {

	var superparent = elm.parents(".speechf-controls");
	if(typeof(superparent)=='undefined' || superparent.length==0) {
		return false;
	}
	return true;
}


function createSearchButton(controlsBase) {
	controlsBase.append("<img src='search1.png' style='float:left; margin-top:5px; border-color:white;'></img>");
}

function createSearchBox(controlsBase) {
	var superParent =controlsBase.parents(".speechf-controls");
	var propsMap = globalPropsMap[superParent.attr("data-props")];
	var progressBarWidth = propsMap["width"];
	var width = progressBarWidth-170;
	controlsBase.append("<div class='speechf-searchBox' style='margin-left:10px; margin-top:4px; margin-left:10px; float:left; background:#fff; '>" +
			"<input class='speechf-searchText' type='text' style='width:" + width + "; height:27px;" +
			"outline:none; font-size:x-small;border:none;background:#fff; float:left; '></input>" +
			"<img src='searchboxDivider.png' style='float:left; border:none;  height:15; margin-top:7px'></img>" +
			"<input class='speechf-searchButton' type='submit' style='background: url(search2.png) no-repeat center left; border:none; width:25; height:27; margin-left:3px; float:left;'  value=''></input>" +
			"<input class='speechf-writeButton' type='submit' style='background: url(write3.png) no-repeat top left; border:none; width:35; height:27; margin-left:6px;'  value=''></input>" +

	"</div>");
}

function createPlayButton(controlsBase) {

	controlsBase.append("<input type='image' " +
			"src='play.png' " +
			"class='speechf-playbutton'" +
			"style='float:left;  margin-top:3px;'>" +
	"</input>");
}

function createProgressSlider(progressBar) {

	var progressSlider = 
		$("<div class='speechf-progressSlider' style='height:14px; border-style:solid; border-width:1px; border-color:#002E3D; position:absolute; float:left;'></div>")
		progressBar.append(progressSlider);
	var pos = progressSlider.offset().left;
	var superParent = progressBar.parents(".speechf-controls");
	var propsMap = globalPropsMap[superParent.attr("data-props")];
	propsMap["progressSliderPos"] = pos;
}

function createSnippetBubble() {
	$("<div ></div>")
}

function createMediaElement(baseElm, propsMap) {
	var src = propsMap["mediasrc"];
	var mediaObj;
	if(propsMap["mediatype"]=="audio") {
		mediaObj = $("<audio class='speechf-audio'>" +
				"<source src='" + src + "'></source>" +
		"</audio>");
	} else if(propsMap["mediatype"]=="video") {
		mediaObj =  $("<video class='speechf-video'>" +
				"<source src='" + src + "'></source>" +
		"</video>");
	} else {
		throw speechfErr;
	}

	if(propsMap.hasOwnProperty("width")) {
		mediaObj.attr('width', propsMap["width"]);
	} else {
		propsMap["width"] = defaultMediaWidth;
		mediaObj.attr('width', defaultMediaWidth);
	}	

	mediaObj.appendTo(baseElm);
	return mediaObj;
}




function createControlsBase(divElm, propsMap) {
	var controlsBase = $("<div class='controls-base' style='height:35px; background-color:#646060; '>"  +
	"</div>");
	if(propsMap.hasOwnProperty("width")) {
		controlsBase.css("width", propsMap["width"]);
	} else {
		controlsBase.css("width", defaultMediaWidth);
	}

	return controlsBase.appendTo(divElm);
}

function createProgressBar(baseElm,propsMap) {
	var progressBar = $("<div class='progress-bar' style='height:" + progressBarHeight + "; background-color:#B8B8B8;'>" +
	"</div>");
	if(propsMap.hasOwnProperty("width")) {
		progressBar.css("width", propsMap["width"]);
	} else {
		progressBar.css("width", defaultMediaWidth);
	}


	
	return progressBar.appendTo(baseElm);
}





function endsWith(line, pattern)  {
	var index = line.length - pattern.length;
	if(index>=0 && line.lastIndexOf(pattern)==index) {
		return true;
	} else {
		return false;
	}
}
