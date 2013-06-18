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
  }//end of uglify
  
});

grunt.loadNpmTasks('grunt-contrib-uglify');
grunt.registerTask('default', 'running default grunt tasks', function(){
		grunt.task.run(['uglify']);
		//add custom tasks or function calls here:
	}
);

};



