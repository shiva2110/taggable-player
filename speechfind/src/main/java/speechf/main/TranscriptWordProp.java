package speechf.main;

public enum TranscriptWordProp {
	WORD, START_TIME, END_TIME, AUDIO_HASH;
	
	public static int compareByStartTime(String st1, String st2) {
		return new Float(st1).compareTo(
				new Float(st2));
	}
}
