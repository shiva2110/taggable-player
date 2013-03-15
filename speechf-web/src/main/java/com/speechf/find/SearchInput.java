package com.speechf.find;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("restriction")
@XmlRootElement(name="SearchInput")
public class SearchInput {

	@XmlElement(required=true)
	public String searchText;
}
