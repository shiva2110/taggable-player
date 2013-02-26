package speechf.index;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;

public class IndexerFacade {
	
	private static IndexWriter FSIndexWriter;
	private static final String FSdir = "/projects/workrepo/speech-F/data/index/";

	@SuppressWarnings("deprecation")
	private static synchronized IndexWriter getFSIndexWriter() {

		try {
			if (FSIndexWriter == null) {
				FSIndexWriter = new IndexWriter(
						FSDirectory.open(new File(FSdir)),
						new StandardAnalyzer(Version.LUCENE_36,
								new HashSet<String>()),
						IndexWriter.MaxFieldLength.UNLIMITED);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return FSIndexWriter;
	}
	
	public void index(TranscriptWord word) throws CorruptIndexException, IOException {
		Document doc = new Document();
		
		if(word.getValue(TranscriptWordProp.WORD)!=null) {
			doc.add(new Field(TranscriptWordProp.WORD.toString(), word.getValue(TranscriptWordProp.WORD), Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
		}
		
		if(word.getValue(TranscriptWordProp.START_TIME)!=null) {
			NumericField f = new NumericField(TranscriptWordProp.START_TIME.toString(),  Field.Store.YES,  true).
					setFloatValue(new Float(word.getValue(TranscriptWordProp.START_TIME)));
			
			doc.add(f);
		}
		
		if(word.getValue(TranscriptWordProp.END_TIME)!=null) {
			NumericField f = new NumericField(TranscriptWordProp.END_TIME.toString(),  Field.Store.YES,  true).
					setFloatValue(new Float(word.getValue(TranscriptWordProp.END_TIME)));
			doc.add(f);
		}
		
		if(word.getValue(TranscriptWordProp.AUDIO_HASH)!=null) {
			doc.add(new Field(TranscriptWordProp.AUDIO_HASH.toString(), word.getValue(TranscriptWordProp.AUDIO_HASH), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
		}
		
		
		IndexerFacade.getFSIndexWriter().addDocument(doc);
	}
	
	public void close() throws CorruptIndexException, IOException {
		IndexerFacade.getFSIndexWriter().commit();
		IndexerFacade.getFSIndexWriter().close();
	}
}
