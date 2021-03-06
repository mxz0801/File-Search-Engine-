import cecs429.documents.DirectoryCorpus;
import cecs429.documents.Document;
import cecs429.documents.DocumentCorpus;
import cecs429.index.ClassificationIndex;
import cecs429.index.Index;
import cecs429.index.Posting;
import cecs429.text.EnglishTokenStream;
import cecs429.text.ImprovedTokenProcessor;
import cecs429.text.TokenStream;

import java.nio.file.Paths;
import java.util.*;


public class IndexBuilder {

    public static void main(String[] args) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Scanner sc = new Scanner(System.in);
        Index indexH;
        Index indexJ;
        Index indexM;
        Index indexD;

        System.out.println("Please enter the directory of the file: ");
        String directory = sc.nextLine();
        DocumentCorpus corpusH = DirectoryCorpus.loadDirectory(Paths.get(directory + "/HAMILTON"), ".json", ".txt");

        DocumentCorpus corpusJ = DirectoryCorpus.loadDirectory(Paths.get(directory + "/JAY"), ".json", ".txt");

        DocumentCorpus corpusM = DirectoryCorpus.loadDirectory(Paths.get(directory + "/MADISON"), ".json", ".txt");

        DocumentCorpus corpusD = DirectoryCorpus.loadDirectory(Paths.get(directory + "/DISPUTED"), ".json", ".txt");

        TreeSet<String> wordSets = new TreeSet<>();
        Map<String, Float> termScores;
        PriorityQueue<Map.Entry<String, Float>> pq = new PriorityQueue<>(((o1, o2) -> o2.getValue().compareTo(o1.getValue())));


        indexH = indexCorpus(corpusH, wordSets);
        indexJ = indexCorpus(corpusJ, wordSets);
        indexM = indexCorpus(corpusM, wordSets);
        indexD = indexCorpus(corpusD, null);


        Map<String, Index> categoryIndex = new HashMap<>();
        categoryIndex.put("HAMILTON", indexH);
        categoryIndex.put("JAY", indexJ);
        categoryIndex.put("MADISON", indexM);

        Map<String, DocumentCorpus> categoryCorpus = new TreeMap<>();
        categoryCorpus.put("HAMILTON", corpusH);
        categoryCorpus.put("JAY", corpusJ);
        categoryCorpus.put("MADISON", corpusM);

        termScores = scoreTerms(wordSets, categoryIndex);
        for (Map.Entry<String, Float> entry : termScores.entrySet()) {
            if (entry.getValue() <= 0) {
                entry.setValue((float) 0);
            }
            pq.offer(entry);
        }

        System.out.println("Vocabulary list:");
        System.out.println(wordSets.toString());
        System.out.println();

        Map<String, Float> Top50Term = new LinkedHashMap<>();
        for (int i = 0; i < 50; i++) {
            assert pq.peek() != null;
            String term = pq.peek().getKey();
            Float score = pq.poll().getValue();
            Top50Term.put(term, score);
            if (i == 9) { // print top10 terms
                for (Map.Entry<String, Float> entry : Top50Term.entrySet())
                    System.out.println(entry.toString());
            }
        }
        System.out.println();
        List<Map<String, Float>> termPtcScore = calculatePtc(categoryIndex, Top50Term.keySet());
        for (Document doc : corpusD.getDocuments()) {
            Set<String> vocab = new HashSet<>();
            TokenStream stream = new EnglishTokenStream(doc.getContent());
            Iterable<String> token = stream.getTokens();
            for (String t : token)
                vocab.add(t.replaceAll("\\W", "").toLowerCase());
            String name = calculateClass(vocab, categoryIndex, termPtcScore);
            System.out.println(doc.getFileTitle() + " is mostly likely to be in category " + name);
            System.out.println();
        }
        List<Float> centroidH = getCentroid(wordSets, indexH, corpusH);
        List<Float> centroidJ = getCentroid(wordSets, indexJ, corpusJ);
        List<Float> centroidM = getCentroid(wordSets, indexM, corpusM);

        List<List<Float>> centroidD = new ArrayList<>();
        for (int d = 0; d < corpusD.getCorpusSize(); d++) {
            List<Float> centroid = getDocCentroid(wordSets, indexD, corpusD.getDocument(d));
            centroidD.add(centroid);
        }
        System.out.println("--------------------------------------------------------");
        List<Float> centroid52 = getDocCentroid(wordSets, indexD, corpusD.getDocument(3));
        System.out.println("the first 30 components (alphabetically) of the normalized vector for the document 52");
        for (int i = 0; i < 30; i++) {
            System.out.println(centroid52.get(i));
        }
        System.out.println("----------------------------------------------------------------");
        printRocchio(centroidH,centroidM,centroidJ,centroidD,corpusD);
    }

    private static List<Float> getDocCentroid(TreeSet<String> wordSets, Index index, Document document) {
        List<Float> result = new ArrayList<>();
        for (String s : wordSets) {
            if (index.getPostings(s) == null) {
                result.add(0.0F);
            } else {
                float totalVd = 0.0F;
                float Ld = index.getLd(document.getId());
                Map<String, Integer> WdtMap = index.getWdtMap(document.getId());
                Integer freqs = WdtMap.get(s);
                if (freqs != null) {
                    float Wdt = (float) (1 + Math.log(freqs));
                    totalVd = (Wdt / Ld);
                }
                result.add(totalVd);
            }
        }
        return result;
    }

    private static List<Float> getCentroid(TreeSet<String> wordSets, Index index, DocumentCorpus corpus) {
        List<Float> result = new ArrayList<>();
        for (String s : wordSets) {
            if (index.getPostings(s) == null) {
                result.add(0.0F);
            } else {
                float totalVd = 0.0F;
                for (Document sDocument : corpus.getDocuments()) {
                    float Ld = index.getLd(sDocument.getId());
                    Map<String, Integer> WdtMap = index.getWdtMap(sDocument.getId());
                    Integer freqs = WdtMap.get(s);
                    if (freqs != null) {
                        float Wdt = (float) (1 + Math.log(freqs));
                        totalVd = (Wdt / Ld) + totalVd;
                    }
                }
                result.add(totalVd / corpus.getCorpusSize());
            }
        }
        return result;
    }

    private static Map<String, Float> scoreTerms(TreeSet<String> wordSets, Map<String, Index> categoryIndex) {
        Map<String, Float> scoreMap = new HashMap<>();
        for (Index category : categoryIndex.values()) {
            for (String word : wordSets) {
                float N11;
                float N1x = 0;
                float Nx1;
                float N10;
                float Nx0;
                float N01;
                float N0x;
                float N00;
                float N = 0;

                Nx1 = category.getDocCount();

                List<Integer> categoryID = new ArrayList<>();

                for (Posting p : category.getPostings(word)) {
                    categoryID.add(p.getDocumentId());
                }
                N11 = categoryID.size();

                // calucate the total number of the categories ABC contains term word
                for (Index cate : categoryIndex.values()) {
                    for (Posting p : cate.getPostings(word)) {
                        N1x = N1x + 1;
                    }
                    N = cate.getDocCount() + N;
                }

                N10 = N1x - N11;
                N01 = Nx1 - N11;

                N00 = N - N10 - N01 - N11;
                Nx0 = N10 + N00;
                N0x = N01 + N00;

                float Iscore = (N11 / N) * log2((N * N11) / (N1x * Nx1)) + (N10 / N) * (log2((N * N10) / (N1x * Nx0))) + (N01 / N) * (log2((N * N01) / (N0x * Nx1)))
                        + (N00 / N) * (log2((N * N00) / (N0x * Nx0)));
                if (Float.isNaN(Iscore)) {
                    Iscore = 0;
                }
                if (scoreMap.containsKey(word)) {
                    float s = scoreMap.get(word);
                    scoreMap.put(word, Math.max(s, Iscore));
                } else {
                    scoreMap.put(word, Iscore);
                }
            }
        }

        return scoreMap;
    }

    private static float log2(float num) {
        return (float) (Math.log(num) / Math.log(2));
    }

    private static List<Map<String, Float>> calculatePtc(Map<String, Index> categoryList, Set<String> top50Terms) {
        List<Map<String, Float>> result = new ArrayList<>();
        for (Index index : categoryList.values()) {
            Map<String, Float> termScore = new HashMap<>();
            float score;
            int indexToken = 0;
            for(String sFreq: top50Terms){
                if(index.getFrequency(sFreq) !=null ){
                    indexToken+=index.getFrequency(sFreq);
                }
            }
            for (String s : top50Terms) {
                int termCount = 0;
                for (Posting p : index.getPostings(s)) {
                    termCount = termCount + p.getPosition().size();
                }

                score = (float) (termCount + 1) / (indexToken + 50);
                termScore.put(s, score);
            }
            result.add(termScore);
        }
        return result;
    }

    private static String calculateClass(Set<String> testSetVocab, Map<String, Index> categoryIndex, List<Map<String, Float>> termPtcScore) {
        Map<String, Float> resultMap = new HashMap<>();

        int docAllClass = 0;
        for (Index index : categoryIndex.values()) {
            docAllClass += index.getDocCount();
        }
        int categoryId = 0;
        for (Map.Entry<String, Index> entry : categoryIndex.entrySet()) {
            float totalTermScore = (float) 0;
            float result;
            for (String s : testSetVocab) {
                if (termPtcScore.get(categoryId).containsKey(s)) {
                    totalTermScore += (float) Math.log(termPtcScore.get(categoryId).get(s));
                }
            }
            result = (float) Math.log(((float) categoryIndex.get(entry.getKey()).getDocCount() / docAllClass)) + totalTermScore;
            resultMap.put(entry.getKey(), result);
            categoryId++;
        }
        float maximum = -Float.MAX_VALUE;
        String category = null;
        for (Map.Entry<String, Float> entry : resultMap.entrySet()) {
            System.out.println("Bayesian classification score for " +entry.getKey() + " is " + entry.getValue());
            if (entry.getValue() > maximum) {
                maximum = entry.getValue();
                category = entry.getKey();
            }
        }
        return category;
    }

    private static Index indexCorpus(DocumentCorpus corpus, TreeSet<String> wordSets) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        ImprovedTokenProcessor processor = new ImprovedTokenProcessor();
        ClassificationIndex index = new ClassificationIndex();
        Set<String> totalTokens = new HashSet<>();
        for (Document sDocument : corpus.getDocuments()) {
            HashMap<String, Integer> docVocabFreq = new HashMap<>();
            TokenStream stream = new EnglishTokenStream(sDocument.getContent());
            Iterable<String> token = stream.getTokens();
            int position = 1;
            for (String t : token) {
                totalTokens.add(t);
                t = t.replaceAll("\\W", "").toLowerCase();
                if (t.length() == 0)
                    continue;
                String newT = getStem(t);
                if (docVocabFreq.containsKey(newT)) {
                    Integer buff = docVocabFreq.get(newT);
                    buff++;
                    docVocabFreq.put(newT, buff);
                } else {
                    docVocabFreq.put(newT, 1);
                }
                List<String> word = processor.processToken(newT);
                if (word.size() > 0) {
                    for (String s : word) {
                        if (wordSets != null) {
                            wordSets.add(s);
                        }
                        index.addTerm(s, sDocument.getId(), position);
                    }
                    position++;
                }
            }
            float Ld = 0;
            for (float ld : docVocabFreq.values()) {
                float wdt = (float) (1 + Math.log(ld));
                Ld += Math.pow(wdt, 2);
            }
            Ld = (float) Math.sqrt(Ld);
            index.addWdtMap(sDocument.getId(), docVocabFreq);
            index.addLd(sDocument.getId(), Ld);
        }
        index.setDocCount(corpus.getCorpusSize());
        index.setTokens(totalTokens.size());
        return index;
    }

    public static String getStem(String input) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
        ImprovedTokenProcessor processor2 = new ImprovedTokenProcessor();
        return processor2.stem(input);
    }

    private static void printRocchio(List<Float> centroidH, List<Float> centroidM, List<Float> centroidJ, List<List<Float>> centroidD, DocumentCorpus corpusD) {
        float tempH = 0, tempM = 0, tempJ = 0;
        for (int d = 0; d < centroidD.size(); d++) {

            List<Float> centroid = centroidD.get(d);
            for (int i = 0; i < centroid.size(); i++) {
                tempH += (float) Math.pow((centroid.get(i) - centroidH.get(i)), 2);
                tempM += (float) Math.pow((centroid.get(i) - centroidM.get(i)), 2);
                tempJ += (float) Math.pow((centroid.get(i) - centroidJ.get(i)), 2);
            }
            tempH = (float) Math.sqrt(tempH);
            tempM = (float) Math.sqrt(tempM);
            tempJ = (float) Math.sqrt(tempJ);
            System.out.println("Dist to /hamilton for doc " + corpusD.getDocument(d).getFileTitle() + " is " + tempH);
            System.out.println("Dist to /madison  for doc " + corpusD.getDocument(d).getFileTitle() + " is " + tempM);
            System.out.println("Dist to /jay for doc " + corpusD.getDocument(d).getFileTitle() + " is " + tempJ);
            float min = Math.min(Math.min(tempH, tempM), tempJ);
            if (min == tempH) {
                System.out.println("Low distance for " + corpusD.getDocument(d).getFileTitle() + " is hamilton");
            } else if (min == tempM) {
                System.out.println("Low distance for " + corpusD.getDocument(d).getFileTitle() + " is madison ");
            } else {
                System.out.println("Low distance for " + corpusD.getDocument(d).getFileTitle() + " is jay");
            }
            System.out.println();
        }
    }

}
