package speechf.main;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class TranscriptWord implements Comparable{
	Map<TranscriptWordProp, String> map =new HashMap<TranscriptWordProp, String>();
	private TranscriptWordProp defaultCompareByField = TranscriptWordProp.START_TIME ;
	
	
	public void addProp(TranscriptWordProp prop, String value) {
		this.map.put(prop, value);
	}
	
	public String getValue(TranscriptWordProp prop) {
		return this.map.get(prop);
	}
	
	public Set<TranscriptWordProp> getKeys() {
		return this.map.keySet();
	}
	
	@Override
	public int compareTo(Object o) {
		if(o instanceof TranscriptWord){
			TranscriptWord otherDoc = (TranscriptWord)o;

			if(defaultCompareByField.equals(TranscriptWordProp.START_TIME)){
				return TranscriptWordProp.compareByStartTime(map.get(defaultCompareByField), 
						otherDoc.map.get(defaultCompareByField));
			} else {
				return map.get(defaultCompareByField).compareTo(
						otherDoc.map.get(defaultCompareByField));
			}
			
		} else {
			throw  new IllegalArgumentException("Incompatible types were compared");
		}	
	}
	
	

	
	
	@Override
	public String toString() {
		return this.map.toString();
	}
}
