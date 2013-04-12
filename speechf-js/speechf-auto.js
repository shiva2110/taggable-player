
var speechfErr  = "The speechf-controls class not following expected syntax. " +
	"Example syntax: class='mediasrc:<source of media file> indexsrc:<http link to index>";

var volSliderPosMap = new Object();
var progressSliderPosMap = new Object();
var volumeBarWidth = 60;
var progressBarWidth = 500;
var progressSliderWidth = 17;
var defaultMediaWidth = 300;

$(window).load(function() {

	$(".speechf-controls").each(function() {
		var titleText = $(this).attr("title");
		 $(this).attr("title", "");
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
			} else if(props[i].indexOf("indexsrc:")>=0) {
				var src = props[i].split("indexsrc:");
				if(src.length==2 && (src[1]!="" || src[1].length!=0)){
					propsMap["indexsrc"] = src[1];
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
		
		if(propsMap["mediasrc"].length==0 || propsMap["indexsrc"].length==0 || propsMap["mediatype"].length==0) {
			throw speechfErr;
		}
		
		mediaElm = createMediaElement(this, propsMap);
		progressBar = createProgressBar(this, propsMap);
		controlsBase = createControlsBase(this, propsMap);	
		createPlayButton(controlsBase);
		createProgressSlider(progressBar);	

		//controlsBase = createControlsBase(this);		
		//createPlayButton(controlsBase);		
	//	createVolumeElement(controlsBase);
	//	drawVolCurve1(controlsBase);
	//	drawVolCurve2(controlsBase);
		createSearchBox(controlsBase);
	//	createSearchButton(controlsBase);
	});
	
/*	$(".speechf-controls").hover(function() {
		$(".progress-bar").show("slow");
		$(".controls-base").show("slow");
		$(".speechf-playbutton").show("slow");
	}, function() {
		$(".progress-bar").hide("slow");
		$(".controls-base").hide("slow"); 
		$(".speechf-playbutton").hide("slow");
	});*/
	
	
	$(".progress-bar").mousemove(function(e){
		var mouseX = e.pageX;		
		progressSlider = getNearbyElement(".speechf-progressSlider", $(this));
		var initPos = progressSliderPosMap[progressSlider]
		progressSlider.css("left", (mouseX-initPos));
		progressSlider.show();
	});
	
	$(".progress-bar").mouseout(function(e){
		progressSlider = getNearbyElement(".speechf-progressSlider", $(this));
		progressSlider.hide();
	});
	
	$(".progress-bar").click(function(e){
		var mouseX = e.pageX;		
		progressSlider = getNearbyElement(".speechf-progressSlider", $(this));
		var initPos = progressSliderPosMap[progressSlider]
		var newPos = (mouseX-initPos);
		var percentage = newPos/progressBarWidth;
		
		var mediaElement = getNearbyMediaElement($(this));	
		
		if(!mediaElement) {
			return;
		}
		
		mediaDuration =  $(mediaElement)[0].duration;
		$(mediaElement)[0].currentTime = (percentage*mediaDuration);
		
		$(".speechf-searchBox").show("slow");
	
	});
	
	$(".progress-bar").dblclick(function() {
		$(".speechf-searchButton").css("background", getWriteIconBg());
		var mediaElement = getNearbyMediaElement($(this));	
		var textBox =  getNearbyElement(".speechf-searchText", $(this));
		displayTextForWrite(mediaElement, textBox);		
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
	
	$(".speechf-searchButton").click(function() {	
		
		if($(this).css("background").indexOf("search")!=-1) {
			$(this).css("background", getWriteIconBg());
			var mediaElement = getNearbyMediaElement($(this));	
			var textBox =  getNearbyElement(".speechf-searchText", $(this));
			displayTextForWrite(mediaElement, textBox);	
		} else {
			$(this).css("background", getSearchIconBg());
			var textBox =  getNearbyElement(".speechf-searchText", $(this));
			displayTextForSearch(textBox);	
		}	
		
		/*$.ajax(
				{url: 'http://localhost:8080/speechf-web/search/siva',
				 type: "GET",
				 headers: {"Accept" : "application/json"},
				 success: function(json){
					 SearchOutput = json.searchOutput;
					 var outStr = "";
					 for(i=0;i<SearchOutput.length; i++) {
						 outStr = outStr + SearchOutput[i].time + ",";
					 }
					 alert("success:" + outStr);
				 },
				 error: function(json){
					 alert(json.searchText);
				 }}
		);*/
	});
	
	$(".speechf-volumeslider").draggable({axis:"x"}, {containment:"parent"});
	
	$(".speechf-volumeslider").bind("drag", function(event, ui) {
		//if this component is not speechf component, then return back.
		if(!isSpeechfComponent($(this))) {
			return;
		}
		
		pos = $(this).offset().left -  volSliderPosMap[$(this)];
		percentage = (pos*100)/volumeBarWidth;
		
		var volwav3 = getNearbyElement(".speechf-volwave3", $(this));
		var volwav2 = getNearbyElement(".speechf-volwave2", $(this));
		var volwav1 = getNearbyElement(".speechf-volwave1", $(this));
		
		if(percentage > 80) {
			volwav3.css("visibility", "visible");
			volwav2.css("visibility", "visible");
			volwav1.css("visibility", "visible");
		} else if(percentage > 40 && percentage < 80) {
			volwav3.css("visibility", "hidden");
			volwav2.css("visibility", "visible");
			volwav1.css("visibility", "visible");
		} else if(percentage > 10 && percentage < 40) {
			volwav3.css("visibility", "hidden");
			volwav2.css("visibility", "hidden");
			volwav1.css("visibility", "visible");
		} else if(percentage < 10) {
			volwav3.css("visibility", "hidden");
			volwav2.css("visibility", "hidden");
			volwav1.css("visibility", "hidden");
		}

		mediaElement = getNearbyElement("audio", $(this));
		mediaElement[0].volume = (percentage/100);
	});
	
	$(".speechf-audio").each(function(){
		addMediaEvents(this);
		});

	
	$(".speechf-video").each(function(){
		addMediaEvents(this);
		});
	
	
	$(".speechf-searchText").keypress(function(e) {
		if(e.which==13) {
			var searchButton = getNearbyElement(".speechf-searchButton", $(this));
			if(searchButton.css("background").indexOf("search")!=-1) {
				//You need to call the REST service here instead of this code:
				var result = $("<div style='height:16px; width:10px; background-color:red; left:200; float:left; position:absolute' title='tron legacy we are coming'></div>");
				result.appendTo(getNearbyElement(".progress-bar", $(this)));
			}		
		}
	});

		
});


function displayTextForSearch(textBox) {
	textBox[0].value = "Enter your search terms here";
	textBox.focus();
	textBox.select();
}

function displayTextForWrite(mediaElement, textBox) {
	var currentTime = $(mediaElement)[0].currentTime;
	currentTime = getMinutes(currentTime);		
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
		playButton = getNearbyElement(".speechf-playbutton", $(elm))
		playButton.prop("src",  "play.png");
	});
	
	$(elm)[0].addEventListener("timeupdate", function(){
		mediaDuration =  $(elm)[0].duration;
		currentTime = $(elm)[0].currentTime;
		percentage = currentTime/mediaDuration;	
		progressBar = getNearbyElement(".progress-bar", $(elm));			

		var borderWidth = (percentage*progressBarWidth);
		progressBar.css("border-left-width" , borderWidth);
		progressBar.css("border-left-style", "solid");
		progressBar.css("border-left-color", "#0099CC");

		progressBar.css("width" , (progressBarWidth-borderWidth));	
	});
}

function getNearbyElement(className, elm) {
	var superparent = elm.parents(".speechf-controls");
	return superparent.find(className);
}

function getNearbyMediaElement(elm) {
	mediaElement = getNearbyElement("audio", elm);	
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

function createVolumeElement(controlsBase) {
	
	/*volImage = $("<img class='speechf-speaker' src='speaker1.png' style='margin-top:28px; margin-left:10px; float:left; zoom:0.20; position:absolute;' ></img>");
	volCurve1 = $("<canvas class='speechf-volcurve1' style='margin-top:6px; position:absolute; margin-left:11px;' width=25 height=20></canvas>");
	volCurve2 = $("<canvas class='speechf-volcurve2' style='margin-top:2px; position:absolute; margin-left:16px;' width=25 height=20></canvas>");
	controlsBase.append(volImage);	
	controlsBase.append(volCurve1);	
	controlsBase.append(volCurve2);	
	
	"<div class='speechf-volumediv' style='float:left;  margin-left:25px; position:absolute;'>	" +		
		"<div class='speechf-volumebar' style='width:60px;  height:5px; float:left;background-color:#C0C0C0; margin-top:15px;'></div>" +
		"<div class='speechf-volumeslider' style='width:5px; height:20px;background-color:#C0C0C0; margin-top:6.5px;'></div>" +
	"</div>"*/
	
	/*volSlider = controlsBase.find(".speechf-volumeslider");
	pos = volSlider.offset().left;
	volSliderPosMap[volSlider]=pos;*/
}

function drawVolCurve1(controlsBase) {

	volCurve1 = controlsBase.find(".speechf-volcurve1");
	var context = volCurve1[0].getContext('2d');
	
	context.beginPath();

	initLeft = 1;
	initTop = 3;	
	midLeft = 20;
	midTop =  1;	
	endLeft = 10;
	endTop = 10;
	
    context.moveTo(initLeft, initTop);
    context.quadraticCurveTo(midLeft, midTop, endLeft, endTop);    
    context.lineWidth = 1;
    context.strokeStyle = 'silver';
    context.stroke();	
}

function drawVolCurve2(controlsBase) {

	volCurve1 = controlsBase.find(".speechf-volcurve2");
	var context = volCurve1[0].getContext('2d');
	
	context.beginPath();

	initLeft = 1;
	initTop = 3;	
	midLeft = 20;
	midTop =  1;	
	endLeft = 12;
	endTop = 10;
	
    context.moveTo(initLeft, initTop);
    context.quadraticCurveTo(midLeft, midTop, endLeft, endTop);    
    context.lineWidth = 1;
    context.strokeStyle = 'silver';
    context.stroke();	
}

function createSearchButton(controlsBase) {
	controlsBase.append("<img src='search1.png' style='float:left; margin-top:5px; border-color:white;'></img>");
}

function createSearchBox(controlsBase) {
	controlsBase.append("<div class='speechf-searchBox' style='width:385px; margin-left:10px; margin-top:4px; margin-left:10px; float:left; background:#fff; '>" +
			"<input class='speechf-searchText' type='text' style='width:340px; height:25px;" +
			"outline:none; font-size:x-small;border:none;background:#fff; float:left; '></input>" +
			"<img src='searchboxDivider.png' style='float:left; border:none; width:5; height:15; margin-top:7px'></img>" +
			"<input class='speechf-searchButton' type='submit' style='background: url(search2.png) no-repeat center left; border:none; width:35; height:25; margin-left:3px'  value=''></input>" +
	"</div>");
}

function createPlayButton(controlsBase) {
	
	controlsBase.append("<input type='image' " +
			"src='play.png' " +
			"class='speechf-playbutton'" +
			"style='float:left;  margin-top:1px;'>" +
			"</input>");
}

function createProgressSlider(progressBar) {
	
	progressSlider = 
		$("<div class='speechf-progressSlider' style='height:14px; border-style:solid; border-width:1px; border-color:#002E3D; position:absolute; float:left;'></div>")
	progressBar.append(progressSlider);
	pos = progressSlider.offset().left;
	progressSliderPosMap[progressSlider] = pos;
}

function createSearchResultPointer(progressBar, timePercentage, score) {
	color = chooseColor(score);
	resultPointer = $("<div class='speechf-resultPointer' style='width:3px; height:16px; background-color:" + color + "; position:fixed;'></div>");
	progressBar.append(resultPointer);
	
	progressSlider =  progressBar.find(".speechf-progressSlider");
	pos = progressSliderPosMap[progressSlider] + (timePercentage*progressBarWidth);
	resultPointer.css("left", pos);
}

function createSnippetBubble() {
	$("<div ></div>")
}

function chooseColor(score) {
	switch(score){
	case 9:
		return "#E0FFFF";
	case 8:
		return "#BDEDFF";
	case 7:
		return "#ADDFFF";
	case 6:
		return "#5CB3FF";
	case 5:
		return "#56A5EC";
	case 4:
		return "#488AC7";
	case 3:
		return "#306EFF";
	case 2:
		return "#2554C7";
	case 1:
		return "#151B8D";
	}
	
	
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
	var controlsBase = $("<div class='controls-base' style='height:34px; background-color:#646060; '>"  +
			"</div>");
	if(propsMap.hasOwnProperty("width")) {
		controlsBase.css("width", propsMap["width"]);
	} else {
		controlsBase.css("width", defaultMediaWidth);
	}
	
	return controlsBase.appendTo(divElm);
}

function createProgressBar(baseElm,propsMap) {
	var progressBar = $("<div class='progress-bar' style='height:16px; background-color:rgba(192,192,192,0.5); '>" +
			"</div>");
	if(propsMap.hasOwnProperty("width")) {
		progressBar.css("width", propsMap["width"]);
		progressBarWidth = propsMap["width"];
	} else {
		progressBar.css("width", defaultMediaWidth);
		progressBarWidth = defaultMediaWidth;
	}
	

	progressBar.appendTo(baseElm);
	return progressBar;
}





function endsWith(line, pattern)  {
	var index = line.length - pattern.length;
	if(index>=0 && line.lastIndexOf(pattern)==index) {
		return true;
	} else {
		return false;
	}
}

function getVolume(elm) {
	volumeSlider = getNearbyElement(".speechf-volumeslider",elm);
	pos = volumeSlider.offset().left -  volSliderPosMap[volumeSlider];
	percentage = (pos*100)/volumeBarWidth;
	return (percentage/100);
}

