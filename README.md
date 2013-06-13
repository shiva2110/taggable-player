Taggable
========
Taggable is a HTML5 media player that allows you to search and tag media content. "Taggable" media player with searching and tagging feature can be created instantly with simple HTML5 snippets. 

Project Page
------
http://shiva2110.github.io/taggable-player

Features
------
#### Tagging
![ScreenShot](https://raw.github.com/shiva2110/taggable-player/master/readme-files/features-tag.png)

#### Search
![ScreenShot](https://raw.github.com/shiva2110/taggable-player/master/readme-files/features-search.png)

How to setup
------
#### Downloads
Download <b>distrib/tagplyr-0.1.min.js</b> and <b>distrib/jquery-1.8.3.min.js</b> from the git repo.

#### HTML syntax
Your video can be embedded into taggable-player using simple HTML5 snippet as shown below:
<br />
<b>In the head:</b>
```
<script src="jquery-1.8.3.min.js"></script>
<script src="tagplyr-0.1.min.js"></script>
```
	
<b>In the body:</b>
```
<video data-player="taggable">
	<source src="html5lecture.webm"></source>	
</video>
```

Note the custom HTML attribute <b>data-player="taggable"</b> that enables embedding your video into taggable-player, instead of the default HTML5 video player.

