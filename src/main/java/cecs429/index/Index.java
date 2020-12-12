package cecs429.index;

import java.io.IOException;
import java.util.List;

/**
 * An Index can retrieve postings for a term from a data structure associating terms and the documents
 * that contain them.
 */
public interface Index {
	/**
	 * Retrieves a list of Postings of documents that contain the given term.
	 */
	List<Posting> getPostings(String term);

	List<Posting> getPostings(String term, boolean check);
	
	/**
	 * A (sorted) list of all terms in the index vocabulary.
	 */
	List<String> getVocabulary();

	Integer getFrequency(String term);
	int getDocNum();
}
