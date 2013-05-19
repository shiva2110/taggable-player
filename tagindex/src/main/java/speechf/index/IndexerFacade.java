package speechf.index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import speechf.main.Helper;
import speechf.main.TranscriptWord;
import speechf.main.TranscriptWordProp;

public class IndexerFacade {

	private static IndexWriter FSIndexWriter;
	private static final String FSdir = "/projects/workrepo/speech-F/data/index/";

	@SuppressWarnings("deprecation")
	private static synchronized IndexWriter getFSIndexWriter() throws IndexerException {
		try {
			if (FSIndexWriter == null) {			
				FSIndexWriter = new IndexWriter(
						FSDirectory.open(new File(FSdir)),
								Helper.getStandardAnalyzer(true, false),
								IndexWriter.MaxFieldLength.UNLIMITED);
			}
		} catch (Exception e) {
			throw new IndexerException(e);
		}

		return FSIndexWriter;
	}

	public void index(TranscriptWord word) throws IndexerException {
		
		try {
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
			
			
			if(word.getValue(TranscriptWordProp.MEDIA_ID)!=null) {
				doc.add(new Field(TranscriptWordProp.MEDIA_ID.toString(), word.getValue(TranscriptWordProp.MEDIA_ID), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}


			IndexerFacade.getFSIndexWriter().addDocument(doc);
		} catch(CorruptIndexException e) {
			throw new IndexerException(e);
		} catch(IOException e){
			throw new IndexerException(e);
		}
		
	}

	public void close() throws CorruptIndexException, IOException, IndexerException {
		IndexerFacade.getFSIndexWriter().commit();
		IndexerFacade.getFSIndexWriter().close();
	}
}
