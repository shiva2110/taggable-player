package com.speechf.find;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Tag")
public class Tag {
	@XmlElement(required=true)
	public String keywords;
	
	@XmlElement(required=true)
	public String startTime;
	
	@XmlElement(required=true)
	public String endTime;
}
