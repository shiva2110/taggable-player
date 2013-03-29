package speechf.index;

public class IndexerException extends Exception{
	
	public IndexerException(String message) {
		super(message);
	}
	
	public IndexerException(Exception e){
		super(e);
	}

}
