

test("test something", function() {
		var expected= "http://localhost/tag?domain=test&mediaId=testId";
		equal(expected,formIndexURL("http://localhost","test", "testId"), "not equal");
});

// test if side-effects are happening when a mouse down and up event occurs on tagabl-canvas
test("tagabl canvas mouse down -> mouse up event", function(){
	var $fixture = $( "#qunit-fixture" );
	$fixture.append("<canvas class='tagabl-canvas' width=100 height=100 style='position:absolute; z-index:3;top:0; left:0'></canvas>");
	$(window).load();
	
	//mouse down
	var event = $.Event("mousedown", {which:1, pageX:50, pageY:50});
	$(".tagabl-canvas").trigger(event);	
	equal(cornerA.x, 50, "not equal");
	equal(cornerA.y, 50, "not equal");
	
	//mouse up
	event = $.Event("mouseup", {which:1, pageX:50, pageY:50});
	$(".tagabl-canvas").trigger(event);	
	equal(leftButtonDown, false);	
	
});

test("tagable canvas mouse down->mouse move->move up event", function(){
	var $fixture = $( "#qunit-fixture" );
	$fixture.append("<canvas class='tagabl-canvas' width=100 height=100 style='position:absolute; z-index:3;top:0; left:0'></canvas>");
	$(window).load();
	
	//mouse down
	var event = $.Event("mousedown", {which:1, pageX:50, pageY:50});
	$(".tagabl-canvas").trigger(event);	
	
	//mousemove
	event = $.Event("mousemove", {pageX:100, pageY:100});
	$(".tagabl-canvas").trigger(event);	
	
	//mouseup
	event = $.Event("mouseup", {which:1, pageX:50, pageY:50});
	$(".tagabl-canvas").trigger(event);	
	
	equal(prevRect.width, 50, "not equal");
	equal(prevRect.height, 50, "not equal");
	
});

test("test indexContentSurroundings when only the frames before the current frame are similar", function(){
	var $fixture = $( "#qunit-fixture" );
	$fixture.append("<video class='tagabl-video' width=500 height=100><source src='/Users/shiva2110/Downloads/UnrollingParams.mp4'></source></video>");
	$(window).load();
	
	//prepare stubs and set expectations
	var stub = sinon.stub(window, "callComparePixels");  //stub that replaces the original function
	stub.returns("left"); //make stub return a value

	//call function under test
	var mediaElement = $(".tagabl-video");
	mediaElement[0].load();
	indexContentSurroundings(mediaElement, 100, 0.5, 0.5, 0.3, 0.2);
	
	equal(stub.callCount, 10, "not equal");
	stub.restore();
	
});

