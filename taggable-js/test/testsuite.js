test("test something", function() {
		var expected= "http://localhost/tag?domain=test&mediaId=testId";
		equal(expected,formIndexURL("http://localhost","test", "testId"), "not equal");
});