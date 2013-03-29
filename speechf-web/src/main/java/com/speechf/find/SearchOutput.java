package com.speechf.find;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="searchOutput")
public class SearchOutput {
	
		@XmlElement(required=true)
		public 	String time;
		
		@XmlElement(required=true)
		public  String snippet;
		
		@XmlElement(required=true)
		public  double score;
		
		public SearchOutput() {
			
		}
		
		public SearchOutput(String time, String snippet, double score) {
			this.time = time;
			this.snippet = snippet;
			this.score = score;
		}
}
