package cecs429.index;

import java.util.*;
/**
 *	Implements Positional Inverted Index 
 */

public class ClassificationIndex implements Index {
	private Map<String,ArrayList<Posting>> mIndex= new HashMap<>();
	private List<String> mVocabulary = new ArrayList<>();
	private Map<String, Integer> freq = new HashMap<>();
	private Map<Integer,Float> LdMap = new HashMap<>();
	private Map<Integer, Map<String,Integer>> WdtMap = new HashMap<>();
	private int DocCount = 0;
	private int mTokens = 0;

	/**
	 * Associates the given documentId and position with the given term in the index.
	 */
	public void addTerm(String term, int documentId, int position) {
		ArrayList<Posting> postList = new ArrayList<>();
		ArrayList<Integer> positionList = new ArrayList<>();
		positionList.add(position);
		Posting post = new Posting(documentId, positionList);
		postList.add(post);
		if(freq.containsKey(term)){
			Integer frequency = freq.get(term);
			freq.put(term,frequency+1);
		}else{
			freq.put(term,1);
		}
		if(mIndex.get(term)==null) {
			mVocabulary.add(term);
			mIndex.put(term, postList);
		}
		else if(mIndex.get(term).get(mIndex.get(term).size() - 1).getDocumentId() != documentId)
			mIndex.get(term).add(post);
		else if(mIndex.get(term).get(mIndex.get(term).size() - 1).getDocumentId() == documentId)
			mIndex.get(term).get(mIndex.get(term).size() - 1).getPosition().add(position);
	}

	public void addLd(int docId, float weight){
		LdMap.put(docId,weight);
	}

	@Override
	public float getLd(int docId){
		return LdMap.get(docId);
	}

	@Override
	public Map<String, Integer> getWdtMap(int docId) {
		return WdtMap.get(docId);
	}

	@Override
	public List<Posting> getPostings(String term) {
		ArrayList<Posting> p = new ArrayList<>();
		if(mIndex.get(term) == null)
			return  p;
		else
			return mIndex.get(term);
	}

	@Override
	public List<Posting> getPostings(String term, boolean check) {
		return null;
	}

	public List<String> getVocabulary() {
		Collections.sort(mVocabulary);
		return mVocabulary;
	}

	public void setDocCount(int count){
		DocCount = count;
	}
	@Override
	public int getDocCount() {
		return DocCount;
	}

	public void setTokens(int tokens){
		mTokens = tokens;
	}
	@Override
	public int getTokens(){
		return mTokens;
	}

	@Override
	public Integer getFrequency(String term) {
		return freq.get(term);
	}

	public void addWdtMap(int id, HashMap<String, Integer> docVocabFreq) {
		WdtMap.put(id,docVocabFreq);
	}

}
