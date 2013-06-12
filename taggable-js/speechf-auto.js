
var speechfErr  = "The taggable-container class not following expected syntax.";



var progressBarHeight = 13;
var progressSliderWidth = 17;
var defaultMediaWidth = "300";
var currentSearchQuery = "";
var currentIndexTime = "";
var indexSpan =  15; //15 sec
var indexURI = "http://ec2-54-235-225-226.compute-1.amazonaws.com/speechf-web";
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
var mediaIndexKey = 0;
var playimg = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAIAAAC0Ujn1AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAADkSURBVEhL7ZYxDoQgEEW9qY2NjbUNjTU3oDaxoeUM1lhRegA6o2DjTkJi4WZxAI1bSD158+f/IZDlt53sNnL+og/ePmTIMAyMseicfaq3bVuWRSlV13VEgxM00OEYYzjnRVEENUChgW6t1Vo3TYOnY9FO/rqufd+XZYlpEIZ29HmeMfEGo518TLyR6D3erut+mZOEBu3jOF6Mdo63bevJM0Y1QKWUVVX59yQMDQ5M00QpvXj5wAQhBP5OolSDAxAXIQQjdq85QWPiitkQcBYTVww6aPzv4odemVd1ogP/8VlIHOID8G2+0cUDzd0AAAAASUVORK5CYII=";
var pauseimg = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAeCAIAAAC0Ujn1AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAACOSURBVEhL7VbbCcAgEHNN8YVoHcQJ3MI1XKLztPfjYcUfQfshl99wAUMuJzPbwLYpG5LuvD3JEGvt9UX7WOcckt57rfUwZmNDYDjnfFdwznEYhIBFKsYopZyTLqU8FZ10CAGplBJJkyGUENg3Wpm2YqD5IBWIloJ6EkK0rFJqop6W3OKTrgwZssSB//4hL+9Rj3PoevgiAAAAAElFTkSuQmCC";
var fullscreenimg = "data:image/png;base64,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";
var minscreenimg = "data:image/png;base64,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";
var writeHighlightImg = "data:image/png;base64,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";
var writeButtonImg = "data:image/png;base64,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";
var writeClickedImg = "data:image/png;base64,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";

	
$(window).load(function() {

	$("[data-player='taggable']").each(function() {
		var propsMap = new Object();
		
		//check if this element is either audio or video
		if(this.tagName!="VIDEO" && this.tagName!="AUDIO") {
			return;
		}
		
		var sourceElement = $(this).find("source");
		if(sourceElement.length==0) {
			return;
		}
		
		propsMap["mediasrc"] = $(sourceElement[0]).attr("src");
		propsMap["domain"] = document.domain;
		propsMap["width"] = $(this).attr("width");
		propsMap["height"] = $(this).attr("height");
		propsMap["mediaIndex"] = mediaIndexKey;
		

		var superParent = $("<div class='taggable-container' id=" + mediaIndexKey + "/>");
		
		//adjust super parent's position
		superParent.css("left", $(this).css("left"));
		superParent.css("top", $(this).css("top"));
		superParent.css("position", $(this).css("position"));
		propsMap["left"] = $(this).css("left");
		propsMap["top"] = $(this).css("top");
		propsMap["position"] = $(this).css("position");
		
		var mediaObj = $(this).clone();
		var mediaElm = createMediaElement(superParent, mediaObj, propsMap);
		var progressBar = createProgressBar(superParent, propsMap);
		var controlsBase = createControlsBase(superParent, propsMap);	
		createPlayButton(controlsBase);
		createSearchBox(controlsBase, propsMap);
		if(this.tagName=="VIDEO"){
			createScreenAdjust(controlsBase);
		}

		globalPropsMap[mediaIndexKey] = propsMap;
		mediaIndexKey++;
		// In case the media element is broken, disable all controls.
		if(mediaElm==undefined) {
			var disablerLeft = progressBar.css("left");
			var disablerTop = progressBar.css("top");
			var disablerWidth = controlsBase.css("width");
			var disablerHeight = controlsBase.css("height");
			var disabler = $("<div style='background-color:grey; left:"+ disablerLeft + "; top:" + disablerTop + "; width:" + disablerWidth + "; height:" + disablerHeight +"; position:absolute; z-index:3; opacity:0.5'></div>");
			disabler.appendTo(controlsBase);
		}
		
		$(this).replaceWith(superParent);

	});

	$(".speechf-fullscreen").click(function() {

		if($(this).attr("title")=="fullscreen") {
			fullscreen($(this));
		} else if($(this).attr("title")=="minscreen") {
			minscreen($(this));
		}

	});

	$(".progress-bar").mousemove(function(e){
		var mouseX = e.pageX;		
		var progressSlider = getNearbyElement(".speechf-progressSlider", $(this));
		var initPos = $(this).offset().left;
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
		var superParent = $(this).parents(".taggable-container");
		var initPos = $(this).offset().left;
		var newPos = (mouseX-initPos);		
		var progressBarWidth = superParent.css("width");
		progressBarWidth = progressBarWidth.replace("px", "");
		var percentage = newPos/progressBarWidth;
		var mediaElement = getNearbyMediaElement($(this));	

		if(!mediaElement) {
			return;
		}

		var mediaDuration =  $(mediaElement)[0].duration;
		$(mediaElement)[0].currentTime = (percentage*mediaDuration);

		$(".speechf-searchBox").show("slow");
		
		//move write bar when progress bar is clicked explicitly
		var writeButton =  getNearbyElement(".speechf-writeButton", $(this));
		if(writeButton.attr("title").indexOf("writeClicked")!=-1) {
			writeButton.click();
		}

	});	

	$(".speechf-playbutton").click(function() {	

		//if this component is not speechf component, then return back.
		if(!isSpeechfComponent($(this))) {
			return;
		}

		var title = $(this).prop("title");
		var mediaElement = getNearbyMediaElement($(this));	

		if(!mediaElement) {
			return;
		}

		if(title.indexOf("play")!=-1) {
			$(this).prop("src",  pauseimg);
			$(this).prop("title", "pause");
			mediaElement[0].play();
		} else {
			$(this).prop("src",  playimg);
			$(this).prop("title", "play");
			mediaElement[0].pause();
		}		
	});

	$(".speechf-writeButton").mousemove(function() {
		if($(this).attr("title").indexOf("writeButton")!=-1) {
			$(this).css("background", "url(" + writeHighlightImg +") no-repeat top left");
			$(this).attr("title", "writeHighlight");
		}
	});

	$(".speechf-writeButton").mouseout(function() {
		if($(this).attr("title").indexOf("writeHighlight")!=-1) {
			$(this).css("background", "url(" + writeButtonImg + ") no-repeat top left");
			$(this).attr("title", "writeButton");
		}
	});

	$(".speechf-writeButton").click(function() {
		// change to write clicked
		$(this).css("background", "url(" + writeClickedImg + ") no-repeat top left");
		$(this).attr("title", "writeClicked");

		//enable search toggle
		var searchButton =  getNearbyElement(".speechf-searchButton", $(this));
		searchButton.css("background", "url(search2.png) no-repeat center left");

		var textBox =  getNearbyElement(".speechf-searchText", $(this));

		//capture searchQuery if any
		if(textBox[0].value.length!=0 && textBox[0].value.indexOf("Tag at")==-1) {
			currentSearchQuery = textBox[0].value;
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
		var superParent = $(this).parents(".taggable-container");
		//var propsMap = globalPropsMap[superParent.attr("data-props")];
		var progressBarWidth = superParent.css("width");
		progressBarWidth = progressBarWidth.replace("px", "");
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
			//change to search clicked
			$(this).css("background", "url(searchClicked.png) no-repeat top left");

			//enable write toggle
			var writeButton =  getNearbyElement(".speechf-writeButton", $(this));
			writeButton.css("background", "url(" + writeButtonImg +") no-repeat top left");
			writeButton.attr("title", "writeButton");

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
			if(currentSearchQuery.length!=0 && currentSearchQuery.indexOf("Tag at")==-1) {
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
	});

	$(".taggable-audio").each(function(){
		addMediaEvents(this);
	});


	$(".taggable-video").each(function(){
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

				var superParent = $(this).parents(".taggable-container");
				var propsMap = globalPropsMap[superParent.attr("id")];

				$.support.cors = true;
				var textBox = $(this);
				var searchCall = $.ajax({
					url: formSearchURL(indexURI, propsMap["domain"], propsMap["mediasrc"], keywords),
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

			} else if(writeButton.attr("title").indexOf("writeClicked")!=-1) {

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

				var superParent = $(this).parents(".taggable-container");
				var propsMap = globalPropsMap[superParent.attr("id")];
				var posting = $.ajax({
					url: formIndexURL(indexURI, propsMap["domain"], propsMap["mediasrc"]),
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
					success: indexSuccessCallback($(this), progressBar, keywords)
				});
			}	
		} else { //escape
		}
	});

	$(document).bind("webkitfullscreenchange", function(e) {
		if(document.webkitIsFullScreen==false) {
			var superParent = $(e.srcElement);
			if(superParent==undefined || superParent[0].className.indexOf("taggable-container")==-1) {
				return;
			}
			
			var minscreenButton  = superParent.find(".speechf-fullscreen");
			minscreen(minscreenButton);
		}
	});



	// have search button clicked as default
	$(".speechf-searchButton").click(); 
});


function fullscreen(fullscreenButton) {
	var mediaElement = getNearbyMediaElement(fullscreenButton);	
	if(mediaElement.attr("class")!="taggable-video") {
		return;
	}	

	if(fullscreenButton.attr("title")=="fullscreen") {
		if($.isFunction(mediaElement[0].webkitEnterFullscreen)){

			var screenH = screen.height;
			var screenW = screen.width;

			//adjust super parent
			var superParent = fullscreenButton.parents(".taggable-container");				
			superParent.css("width", screenW);
			superParent.css("height", screenH-50);
			superParent.css("left", 0);
			superParent.css("top", 50);
			superParent.css("background-color", "black");

			//adjust media element
			mediaElement.css("width", screenW);
			mediaElement.css("height", screenH-100);

			// adjust controls-base
			var controlsBase = getNearbyElement(".controls-base", fullscreenButton);
			controlsBase.css("width", screenW);
			arrangeControlsBase(controlsBase, true);

			//adjust border-width and widh of progressBar
			var progressBar = getNearbyElement(".progress-bar", fullscreenButton);
			var borderWidth = progressBar.css("border-left-width");
			borderWidth = borderWidth.replace("px", "");
			var propsMap = globalPropsMap[superParent.attr("id")];
			var width = propsMap["width"].replace("px", "");
			var percentage = borderWidth/width;
			var adjustedBorderWidth = percentage * (screenW);
			progressBar.css("border-left-width", adjustedBorderWidth);
			progressBar.css("width" , (screenW-adjustedBorderWidth));

			//adjust write bars if any
			adjustWriteBar(superParent, true);				

			//adjust search results if any
			adjustSearchResults(superParent, true);

			//make fullscreen request
			document.webkitCancelFullScreen(); 
			superParent[0].webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
			fullscreenButton.attr("src" , minscreenimg);
			fullscreenButton.attr("title" , "minscreen");
			
		}
	}
}

function minscreen(minscreenButton) {
	var mediaElement = getNearbyMediaElement(minscreenButton);	
	if(mediaElement.attr("class")!="taggable-video") {
		return;
	}

	if(minscreenButton.attr("title")=="minscreen") {
		if($.isFunction(mediaElement[0].webkitEnterFullscreen)){
			var superParent = minscreenButton.parents(".taggable-container");
			var propsMap = globalPropsMap[superParent.attr("id")];

			//adjust controlsBase
			var controlsBase = getNearbyElement(".controls-base", minscreenButton);
			controlsBase.css("width", propsMap["width"]);				
			arrangeControlsBase(controlsBase, false);

			//adjust border-width and widh of progressBar
			var progressBar = getNearbyElement(".progress-bar", minscreenButton);
			var borderWidth = progressBar.css("border-left-width");
			borderWidth = borderWidth.replace("px", "");
			var percentage = borderWidth/(screen.width);
			var width = propsMap["width"].replace("px", "");				
			var adjustedBorderWidth = percentage * (width);
			progressBar.css("border-left-width", adjustedBorderWidth);
			progressBar.css("width" , (width-adjustedBorderWidth));

			//adjust mediaElement
			mediaElement.css("width", propsMap["width"]);
			mediaElement.css("height", "auto");

			//adjust super parent
			superParent.css("width", propsMap["width"]);
			superParent.css("height", "auto");
			superParent.css("left", propsMap["left"]);
			superParent.css("top", propsMap["top"]); 

			//exit fullscreen
			document.webkitCancelFullScreen(); 

			//adjust write bars if any
			adjustWriteBar(superParent, false);	

			//adjust search results if any
			adjustSearchResults(superParent, false);

			minscreenButton.attr("src" , fullscreenimg);
			minscreenButton.attr("title" , "fullscreen");
		}
	}
}

function arrangeControlsBase(controlsBase, fullscreenMode) {

	if(fullscreenMode) {
		var maxWidth = controlsBase.css("width");
		maxWidth = maxWidth.replace("px", "");
		var center = (maxWidth/2);
		var searchTextWidth = 0.30 * (maxWidth);
		var controls = controlsBase.children();

		var adjustedSearchBoxWidth; 
		for(i=0; i<controls.length; i++) {
			if(controls[i].className=="speechf-searchBox"){
				var searchBoxElems = $(controls[i]).children();
				for(j=0; j<searchBoxElems.length; j++) {
					if(searchBoxElems[j].className == "speechf-searchText"){
						$(searchBoxElems[j]).css("width", searchTextWidth);
					}					
				}
				adjustedSearchBoxWidth = $(controls[i]).css("width");
			}
		}

		for(i=0; i<controls.length; i++) {
			if(controls[i].className=="speechf-playbutton"){
				adjustedSearchBoxWidth = adjustedSearchBoxWidth.replace("px", "");
				$(controls[i]).css("margin-left", (center-(adjustedSearchBoxWidth/2)));
			}
		}
	} else {
		var maxWidth = controlsBase.css("width");
		maxWidth = maxWidth.replace("px", "");
		var searchTextWidth = maxWidth - 170;

		var controls = controlsBase.children();

		for(i=0; i<controls.length; i++) {
			if(controls[i].className=="speechf-searchBox"){
				var searchBoxElems = $(controls[i]).children();
				for(j=0; j<searchBoxElems.length; j++) {
					if(searchBoxElems[j].className == "speechf-searchText"){
						$(searchBoxElems[j]).css("width", searchTextWidth);
					}
				}
			} else if(controls[i].className=="speechf-playbutton"){
				$(controls[i]).css("margin-left", 0);
			}
		}
	}

}

/**
 * This function should be called to re-adjust write bar in case of fullscreen mode toggle.
 * @param superParent
 * @param fullscreenMode
 */
function adjustWriteBar(superParent, fullscreenMode) {

	// remove earlier write index time bar if any
	var writeBars = superParent.find(".speechf-writebar");
	var writeBar;

	// find the one write bar to operate on
	if(writeBars==undefined) {
		return;
	}	
	if(writeBars.length==undefined) {
		writeBar = writeBars;
	} else if(writeBars.length >= 1) {
		writeBar = $(writeBars[0]);
	}

	if(writeBar == undefined) {
		return;
	}

	//initialize required vars
	var propsMap = globalPropsMap[superParent.attr("id")];
	var currentleftPos = writeBar.css("left");
	currentleftPos = currentleftPos.replace("px", "");
	var mediaElement = findMediaElement(superParent);
	var adjustedleftPos;
	var adjustedWidth;
	var adjustedMaxWidth;
	var currentPercentage;
	// calculate adjusted pos and width
	if(fullscreenMode) {
		var width = propsMap["width"];
		width = width.replace("px", "");
		currentPercentage = currentleftPos/width;
		adjustedMaxWidth = screen.width;
	} else {
		currentPercentage = currentleftPos/(screen.width);
		adjustedMaxWidth = propsMap["width"];
		adjustedMaxWidth = adjustedMaxWidth.replace("px", "");
	}	

	adjustedleftPos = currentPercentage * (adjustedMaxWidth);
	var widthPercentage = indexSpan/$(mediaElement)[0].duration;	
	adjustedWidth = widthPercentage * (adjustedMaxWidth);
	if((adjustedleftPos+adjustedWidth) > adjustedMaxWidth) {
		adjustedWidth = adjustedMaxWidth - adjustedleftPos;
	}

	// create new write bar
	writeBar.remove();
	var writeBarHTML = $("<div class='speechf-writebar' style='height:" + progressBarHeight + "; width:" + adjustedWidth + "; background-color:#FF0000; opacity:0.4; left:" + adjustedleftPos + "; float:left; position:absolute'></div>");
	writeBarHTML.appendTo(superParent.find(".progress-bar"));

}


function adjustSearchResults(superParent, fullscreenMode) {

	var results = superParent.find("[class|='speechfresult']");
	var snippets = superParent.find("[class|='speechfsnippet']");
	var progressBar = superParent.find(".progress-bar");
	var propsMap = globalPropsMap[superParent.attr("id")];

	for(i=0; i<results.length; i++) {
		var jObj = $(results[i]);
		var left = jObj.css("left");
		left = left.replace("px", "");
		var adjustedLeftPos;
		var maxWidth;
		if(fullscreenMode) {
			var curWidth = propsMap["width"];
			curWidth = curWidth.replace("px", "");
			var percentage = left/curWidth;
			maxWidth = screen.width;
			adjustedLeftPos = percentage * (maxWidth);
		} else {
			var percentage = left/(screen.width);
			maxWidth = propsMap["width"];
			maxWidth = maxWidth.replace("px", "");
			adjustedLeftPos = percentage * (maxWidth);
		}
		jObj.css("left", adjustedLeftPos);

		//adjust snippet pos
		var snippet = snippetSelector(jObj);
		if(snippet==undefined || snippet==null) {
			continue;
		}
		var snippetTop = progressBar.position().top -65;
		var snippetLeft = jObj.position().left - 30;
		var snippetWidth = 90;

		maxWidth = parseInt(maxWidth);
		if((snippetLeft + snippetWidth)>maxWidth) {
			snippetLeft = snippetLeft - (snippetLeft + snippetWidth - maxWidth) - 2;
		} else if (snippetLeft<0) {
			snippetLeft = 2;
		}
		snippet.css("top", snippetTop);
		snippet.css("left", snippetLeft);	
	}
}

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
	var superParent = progressBar.parents(".taggable-container");
	var propsMap = globalPropsMap[superParent.attr("id")];
	var progressBarWidth =  superParent.css("width"); //progress-bar css width is not reliable cuz of border width changes during media play
	progressBarWidth = progressBarWidth.replace("px", "");
	var resultLeftPos = percentage * (progressBarWidth);

	var resultColor = "#FF8080";
	if(resultIndex<resultColorMap.length){
		resultColor = resultColorMap[resultIndex];
	}
	var result = $("<div class='speechfresult-" + resultIndex + "' style='height:" + progressBarHeight + "; width:10px; background-color:" + resultColor + "; left:" + resultLeftPos +  "; float:left; opacity:0.8; position:absolute;'></div>");


	result.appendTo(progressBar);

	var snippetWidth = 105;
	var snippetHeight = 60;

	var snippet = $("<div class='speechfsnippet-" + resultIndex +"' style='background-image:url(snippetbg.png); border:none; position:absolute;" +
			"height:" + snippetHeight + "; font-size:11; font-weight:500; color:white; font-family:Lucida Console, Monaco5, monospace; z-index:2; width:" + snippetWidth + "; text-align:center; display:none;'>" +
					"<div style='top:10%; left:5%; position:absolute; text-align:center;'>" + resultSnippet + "</div></div>");


	var top = progressBar.position().top -65;
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


function indexSuccessCallback(textBox, progressBar, keywords) {
	//after indexing, remove the text
	var currentTimeMin = getMinutes(currentIndexTime);	
	//displayTextForWrite(currentTimeMin, textBox);	

	//after indexing, show successful indexed message
	var writeBars = getNearbyElement(".speechf-writebar", textBox);
	var snippetWidth = 150;
	var snippetHeight = 60;
	keywords = keywords.substring(0,10) + "...";
	var snippet = $("<div class='speechf-indexedMes' style='background: url(Tagged.png) no-repeat top left; border:none; position:absolute; " +
			"height:" + snippetHeight + "; color:white; font-size:11; font-family:Lucida Console, Monaco5, monospace; z-index:2; width:" + snippetWidth + "'> " +
			"<div style='top:5; left:5; position:absolute'>Indexed <br />'" + keywords + "'!</div></div>");
	var top = progressBar.position().top - 50;
	var left = $(writeBars[0]).position().left - 30;
	var superParent = progressBar.parents(".taggable-container");
	var propsMap = globalPropsMap[superParent.attr("id")];
	var progressBarWidth =  superParent.css("width"); //progress-bar css width is not reliable cuz of border width changes during media play
	progressBarWidth = progressBarWidth.replace("px", "");
	if((left + snippetWidth)>progressBarWidth) {
		left = left - (left + snippetWidth - progressBarWidth) - 2;
	} else if (left<0) {
		left = 2;
	}
	snippet.css("top", top);
	snippet.css("left", left);				
	progressBar.before(snippet);	
	snippet.fadeIn(2000);
	snippet.delay(3000).fadeOut(2000, "swing");		
}

function displayTextForSearch(textBox) {
	textBox[0].value = "Enter your search terms here";
	textBox.focus();
	textBox.select();
}

function displayTextForWrite(currentTime, textBox) {	
	textBox[0].value = "Tag at " + currentTime + "m";
	textBox.focus();
	textBox.select();
}

function getMinutes(sec) {
	sec = sec/60;
	sec = sec.toFixed(2);
	return sec;
}

function getWriteIconBg(){
	return "url(" + writeButtonImg + ") no-repeat top left";
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
		var controlsBase = getNearbyElement(".controls-base", $(elm));	
		var progressBar = getNearbyElement(".progress-bar", $(elm));	
		var progressBarOrigWidth = controlsBase.css("width");
		progressBarOrigWidth = progressBarOrigWidth.replace("px", "");
		var borderWidth = (percentage*progressBarOrigWidth);
		progressBar.css("border-left-width" , borderWidth);
		progressBar.css("border-left-style", "solid");
		progressBar.css("border-left-color", "#FCFCFC");
		progressBar.css("width" , (progressBarOrigWidth-borderWidth));	
		
		//move write bar if required
		var writeButton =  getNearbyElement(".speechf-writeButton", $(elm));
		if(writeButton.attr("title").indexOf("writeClicked")!=-1) {
			if(Math.abs(currentTime-currentIndexTime)>=indexSpan){
				writeButton.click();
			}
		}
		
	});
}

function getNearbyElement(className, elm) {
	var superparent = elm.parents(".taggable-container");
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

function findMediaElement(parent) {
	var mediaElement = parent.find("audio");	
	if(mediaElement.length==0) {
		mediaElement = parent.find("video");
	}
	if(mediaElement.length==0) {
		return;
	}

	return mediaElement;
}


function isSpeechfComponent(elm) {

	var superparent = elm.parents(".taggable-container");
	if(typeof(superparent)=='undefined' || superparent.length==0) {
		return false;
	}
	return true;
}


function createSearchButton(controlsBase) {
	controlsBase.append("<img src='search1.png' style='float:left; margin-top:5px; border-color:white;'></img>");
}

function createSearchBox(controlsBase, propsMap) {
	var progressBarWidth = propsMap["width"];
	var width = progressBarWidth-170;
	controlsBase.append("<div class='speechf-searchBox' style='margin-left:10px; margin-top:4px; margin-left:10px; float:left; background:#fff; '>" +
			"<input class='speechf-searchText' type='text' style='width:" + width + "; height:27px;" +
			"outline:none; font-family:Lucida Console, Monaco5, monospace; font-size:small;border:none;background:#fff; float:left; '></input>" +
			"<img src='searchboxDivider.png' style='float:left; border:none;  height:15; margin-top:7px'></img>" +
			"<input class='speechf-searchButton' type='submit' style='background: url(search2.png) no-repeat center left; border:none; width:25; height:27; margin-left:3px; float:left;'  value=''></input>" +
			"<input class='speechf-writeButton' type='submit' title='writeButton' style='background: url(" + writeButtonImg + ") no-repeat top left; border:none; width:35; height:27; margin-left:6px;'  value=''></input>" +

	"</div>");
}

function createScreenAdjust(controlsBase) {

	controlsBase.append("<input type='image' " +
			"src=" + fullscreenimg + " " +
			"title=fullscreen " +
			"class='speechf-fullscreen'" +
			"style='float:left;  margin-top:4px; margin-left:8px;'>" +
	"</input>");
}

function createPlayButton(controlsBase) {

	controlsBase.append("<input type='image' " +
			"title=play " +
			"src=" + playimg + " " +
			"class='speechf-playbutton'" +
			"style='float:left;  margin-top:3px;'>" +
	"</input>");
}

function createProgressSlider(progressBar, propsMap) {

	var progressSlider = 
		$("<div class='speechf-progressSlider' style='height:14px; border-style:solid; border-width:1px; border-color:#002E3D; position:absolute; float:left;'></div>")
		progressBar.append(progressSlider);
	var pos = progressSlider.offset().left;
	propsMap["progressSliderPos"] = pos;
}

function createSnippetBubble() {
	$("<div ></div>")
}

function createMediaElement(superParent, mediaObj, propsMap) {

	if(propsMap.hasOwnProperty("width") && propsMap["width"]!=undefined) {
		mediaObj.attr('width', propsMap["width"]);
	} else {
		propsMap["width"] = defaultMediaWidth;
		mediaObj.attr('width', defaultMediaWidth);
	}
	
	mediaObj.css("left", "");
	mediaObj.css("top", "");
	mediaObj.css("position", "");
	if(mediaObj[0].tagName=="AUDIO") {
		mediaObj.attr("class", "taggable-audio");
	} else {
		mediaObj.attr("class", "taggable-video");
	}
	
	
	mediaObj.appendTo(superParent);
	propsMap["mediaHeight"] = mediaObj.css("height");

	if(!supportedFormat(propsMap["mediasrc"]) || !supportedBrowser()) {
		
		var brokenVideo;
		if(!supportedBrowser()) {
			brokenVideo = $("<div style='background-color:black; color:grey; text-align:center; font-size:x-small; font-family:Arial, Helvetica, sans-serif;'>" +
			"Sorry :( , Unsupported browser detected. <br/> The SPEECHF HTML5 Media player currently supports only Chrome.</div>");
		} else if(!supportedFormat(src)){
			brokenVideo = $("<div style='background-color:black; color:grey; text-align:center; font-size:x-small; font-family:Arial, Helvetica, sans-serif;'>" +
			"Sorry :( , This HTML5 media player does not supports your media file. <br/> The supported media formats are ogg, mp4 and webm.</div>");
		}
		
		brokenVideo.css("width", propsMap["width"]);
		brokenVideo.css("height", propsMap["mediaHeight"]);
		mediaObj.remove();
		brokenVideo.appendTo(superParent);

		var height = propsMap["mediaHeight"].replace("px", "");
		var brokenMesTop = brokenVideo.offset().top + (height/2);		
		var width = propsMap["width"].replace("px", "");
		var brokenMesLeft = brokenVideo.offset().left + (width/2);		
		var brokenMes = $("<div style='font-size:x-small; font-family:Arial, Helvetica, sans-serif; left:" + brokenMesLeft + "; top:" + brokenMesTop + " color:grey;'>Sorry :\ , This HTML5 media player does not supports this media file.</div>");
		brokenVideo.innerHTML = "<div style='background-color:black; color:grey;'>hello</div>"
			return;
	}

	return mediaObj;
}

function supportedBrowser() {
	if($.browser.webkit) {
		return true;
	}
	return false;
}

function supportedFormat(src) {
	if(src.indexOf(".mp4")!=-1 || src.indexOf(".m4a")!=-1 || src.indexOf(".m4b")!=-1 || src.indexOf(".m4v")!=-1
			|| src.indexOf(".ogg")!=-1 || src.indexOf(".oga")!=-1 || src.indexOf(".ogv")!=-1 || src.indexOf(".ogx")!=-1
			|| src.indexOf(".webm")!=-1) {
		return true;
	}

	return false;
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
