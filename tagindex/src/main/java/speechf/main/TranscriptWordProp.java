package speechf.main;

public enum TranscriptWordProp {
	WORD, //word
	FMTWORD, //formatted word (eg., stemming)
	START_TIME, // start time of the indexed word
	END_TIME, // end time of the indexed word
	MEDIA_ID,  // unique id of the media stream
	AUDIO_HASH;
	
	
	public static int compareByStartTime(String st1, String st2) {
		return new Float(st1).compareTo(
				new Float(st2));
	}
}
