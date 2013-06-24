module.exports = function(grunt) {

// Project configuration.
grunt.initConfig({
  pkg: grunt.file.readJSON('package.json'),
  uglify: {
    options: {
      banner: '/*! <%= pkg.name %> - v<%= pkg.version %> - ' +
        '<%= grunt.template.today("yyyy-mm-dd") %> */'
    },
    my_target: {
      files: {
        '../distrib/tagplyr.min.js': ['../taggable-js/taggable-player.js']
      }
    }
  },//end of uglify
  jshint: {
	default: {
		options: {
			eqeqeq:false
		},
		src:['../taggable-js/taggable-player.js']
	}
  }//end of jshint
  
});



grunt.loadNpmTasks('grunt-contrib-uglify');
grunt.loadNpmTasks('grunt-contrib-jshint');

grunt.registerTask('default', 'running default grunt tasks', function(){
		grunt.task.run(['jshint:default']);
		grunt.task.run(['uglify']);
	
		//add custom tasks or function calls here:
		var done = this.async();
		replace_js_in_file("../distrib/htmllecture.html", /tagplyr((?!\.min\.js).)*\.min\.js/g , "tagplyr.min.js", done);
		done = this.async();
		replace_js_in_file("../README.md", /tagplyr((?!\.min\.js).)*\.min\.js/g , "tagplyr.min.js", done);	
	}
);

};

function replace_js_in_file(fileName, regex, replacement, done){
	console.log("Inside function replace_js_in_file()");
	var fs = require('fs');
	console.log("Calling async function readFile");
	fs.readFile(fileName, 'utf8', function (err,data) {
  	if (err) {
    		console.log(err);
		done(false);
  	}
	
  	var result = data.replace(regex, replacement);
  	fs.writeFile(fileName, result, 'utf8', function (err) {
     			if (err) {
					console.log(err);
					done(false);
  				}
			});
	console.log("done with async function readFile");
	done();
	});
	console.log("Gone past async function, done will be called when done!");
}


