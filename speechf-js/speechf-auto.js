
var speechfErr  = "The speechf-controls class not following expected syntax. " +
	"Example syntax: class='mediasrc:<source of media file> indexsrc:<http link to index>";

var volSliderPosMap = new Object();
var progressSliderPosMap = new Object();
var volumeBarWidth = 60;
var progressBarWidth = 500;
var progressSliderWidth = 17;

$(window).load(function() {

	$(".speechf-controls").each(function() {
		var titleText = $(this).attr("title");
		var props = titleText.split(" ");
		if(props.length!=3) {
			throw speechfErr;
		}
		
		
		progressBar = createProgressBar(this);
		createProgressSlider(progressBar);
		controlsBase = createControlsBase(this);
		var mediaSrc;
		var indexSrc;
		var mediaType;
		
		for(i=0; i<props.length;i++) {
			if(props[i].indexOf("mediasrc:")>=0){
				var src = props[i].split("mediasrc:");
				if(src.length==2 && (src[1]!="" || src[1].length!=0)){
					mediaSrc = src[1];
				} else {
					throw speechfErr;
				}
			} else if(props[i].indexOf("indexsrc:")>=0) {
				var src = props[i].split("indexsrc:");
				if(src.length==2 && (src[1]!="" || src[1].length!=0)){
					indexSrc = src[1];
				} else {
					throw speechfErr;
				}
			} else if(props[i].indexOf("mediatype:")>=0) {
				var mediaType = props[i].split("mediatype:");
				if(mediaType.length==2 && (mediaType[1]!="" || mediaType[1].length!=0)){
					mediaType = mediaType[1];
				} else {
					throw speechfErr;
				}
				
			}
		}
		
		if(mediaSrc.length==0 || indexSrc.length==0 || mediaType.length==0) {
			throw speechfErr;
		}
		
		createPlayButton(controlsBase);
		createMediaElement(controlsBase, mediaType, mediaSrc);
		createVolumeElement(controlsBase);
		createSearchBox(controlsBase);
	//	createSearchButton(controlsBase);
	});
	
	$(".speechf-playbutton").click(function() {	
		
		//if this component is not speechf component, then return back.
		if(!isSpeechfComponent($(this))) {
			return;
		}
		
		var src = $(this).prop("src");
		if(endsWith(src, "play.png")) {
			$(this).prop("src",  "pause.png");
			mediaElement = getNearbyElement("audio", $(this));		
			mediaElement[0].volume = getVolume(mediaElement);
			mediaElement[0].play();
		} else {
			$(this).prop("src",  "play.png");
			mediaElement = getNearbyElement("audio", $(this));		
			mediaElement[0].pause();
		}
		
		
		
	});
	
	$(".speechf-searchButton").click(function() {	
		$.ajax(
				{url: 'http://localhost:8080/speechf-web/search/siva',
				 type: "GET",
				 headers: {"Accept" : "application/json"},
				 success: function(json){
					 alert("success:" + json.searchText);
				 },
				 error: function(json){
					 alert(json.searchText);
				 }}
		);
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
		$(this)[0].addEventListener("ended", function(){
			playButton = getNearbyElement(".speechf-playbutton", $(this))
			playButton.prop("src",  "play.png");
		});
		
		$(this)[0].addEventListener("timeupdate", function(){
			mediaDuration =  $(this)[0].duration;
			currentTime = $(this)[0].currentTime;
			percentage = currentTime/mediaDuration;	
			progressSlider = getNearbyElement(".speechf-progressSlider", $(this));			
			pos = progressSliderPosMap[progressSlider] + (percentage*progressBarWidth);	
			if(percentage>=1){
				pos = pos - progressSliderWidth;
			}
			progressSlider.css("left", pos);
		});
	});
	
	
});

function getNearbyElement(className, elm) {
	var superparent = elm.parents(".speechf-controls");
	return superparent.find(className);
}

function isSpeechfComponent(elm) {
	
	var superparent = elm.parents(".speechf-controls");
	if(typeof(superparent)=='undefined' || superparent.length==0) {
		return false;
	}
	return true;
}

function createVolumeElement(controlsBase) {
	
	controlsBase.append("<img src='speaker1.png' style='margin-top:2px; margin-left:3px; float:left;' ></img>" +	
	"<div class='speechf-volwave1' style='width:1px; height:6px; float:left; background-color:#C0C0C0; margin-top:14px; visibility:hidden;' ></div> " +
	"<div class='speechf-volwave2' style='width:1px; height:10px; float:left; background-color:#C0C0C0; margin-top:12px; margin-left:2px; visibility:hidden;' ></div>" +
	"<div class='speechf-volwave3' style='width:1px; height:13px; float:left; background-color:#C0C0C0; margin-top:10px; margin-left:2px; visibility:hidden;' ></div>" +
	"<div class='speechf-volumediv' style='float:left;  margin-left:8px;'>	" +		
		"<div class='speechf-volumebar' style='width:60px;  height:5px; float:left;background-color:#C0C0C0; margin-top:15px;'></div>" +
		"<div class='speechf-volumeslider' style='width:5px; height:20px;background-color:#C0C0C0; margin-top:6.5px;'></div>" +
	"</div>");
	
	volSlider = controlsBase.find(".speechf-volumeslider");
	pos = volSlider.offset().left;
	volSliderPosMap[volSlider]=pos;
}

function createSearchButton(controlsBase) {
	controlsBase.append("<img src='search1.png' style='float:left; margin-top:5px; border-color:white;'></img>");
}

function createSearchBox(controlsBase) {
	controlsBase.append("<div style='width:340px; margin-left:10px; margin-top:5px; float:left; background:#fff;'>" +
			"<input class='speechf-searchText' type='text' style='width:300px; height:22px; " +
			"color:#2B60DE; outline:none;" +
			"font-size:medium;border:none;background:#fff; float:left;'></input>" +
			"<input class='speechf-searchButton' type='submit' style='background: url(search1.png) no-repeat top left; border:none; width:30px;'  value=''></input>" +
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
	
	progressBar.append("<img class='speechf-progressSlider' src='progress.png' style='position:fixed;'></img>");
	progressSlider = progressBar.find(".speechf-progressSlider");
	pos = progressSlider.offset().left;
	progressSliderPosMap[progressSlider] = pos;
}

function createMediaElement(controlsBase, mediaType, src) {
	if(mediaType=="audio") {
		createAudioElement(controlsBase, src);
	} else if(mediaType=="video") {
		createVideoElement(controlsBase, src);
	} else {
		throw speechfErr;
	}
}

function createAudioElement(controlsBase, src) {
	controlsBase.append("<audio class='speechf-audio'>" +
			"<source src='" + src + "'></source>" +
			"</audio>");
}

function createVideoElement(controlsBase, src) {
	controlsBase.append("<video class='speechf-video'>" +
			"<source src='" + src + "'></source>" +
			"</video>");
}


function createControlsBase(divElm) {
	return $("<div style='height:32px; width:500px; background-color:#646060; '></div>").appendTo(divElm);
}

function createProgressBar(divElm) {
	return $("<div id='progressBar' style='height:16px; width:500px; background:rgba(192,192,192,0.5);'></div>").appendTo(divElm);
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

