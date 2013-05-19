package speechf.search;

import java.util.ArrayList;
import java.util.List;
import speechf.main.TranscriptWord;
public class Cluster {
	public Float mean;	
	public int totalMembers;
	List<TranscriptWord> members = new ArrayList<TranscriptWord>();
	
	public void addMember(TranscriptWord member) {
		members.add(member);
	}
	
	public List<TranscriptWord> getMembers() {
		return members;
	}
	
}
