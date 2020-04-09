package feature;

import org.apache.uima.jcas.JCas;
import org.cleartk.ml.Feature;
import org.cleartk.ml.feature.extractor.CleartkExtractorException;
import org.cleartk.ml.feature.extractor.FeatureExtractor1;
import type.CommentAnnotation;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LexiconExtractor implements FeatureExtractor1<CommentAnnotation> {

    private final List<String> posLexicon = new ArrayList<>();
    private final List<String> negLexicons = new ArrayList<>();

    public LexiconExtractor(File wordList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(wordList));) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] split = line.split(" ");
                String label = split[2];
                String word = split[0];
                addWordToLexicon(label, word);

            }

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public List<Feature> extract(JCas view, CommentAnnotation focusAnnotation) throws CleartkExtractorException {
        List<Feature> customFeatures = new ArrayList();
        List<String> commentTokens = Arrays.asList(focusAnnotation.getCoveredText().split(" "));
        //List<String> negWordsFound = new ArrayList<>();
        //List<String> posWordsFound = new ArrayList<>();

        int posCount = 0;
        int negCount = 0;
        for (String word : commentTokens) {
            if (!word.equals("")) {
                if (negLexicons.contains(word)) {
                    //if (!negWordsFound.contains(word))
                     // negWordsFound.add(word);
                    negCount++;
                } else if (posLexicon.contains(word)) {
                    //if (!posWordsFound.contains(word))
                     // posWordsFound.add(word);
                    posCount++;
                }

            }
        }

       // Feature negativeWords = new Feature("negativeWords ", negWordsFound);
       // Feature positiveWords = new Feature("positiveWords ", posWordsFound);
        Feature positiveFeature = new Feature("positive " + posCount);
        Feature negativeFeature = new Feature("negative " + negCount);
        Feature ratio = new Feature("ratio " + (negCount/(posCount + 1)));

        //System.out.println("Count of positive words "+posCount+ " and negative words "+negCount+" contained in this comments" );
       // customFeatures.add(negativeWords);
       // customFeatures.add(positiveWords);
        customFeatures.add(positiveFeature);
        customFeatures.add(negativeFeature);
        customFeatures.add(ratio);

        return customFeatures;

    }

    private void addWordToLexicon(String label, String word) {

        if (label.equals("negative"))
            this.negLexicons.add(word);
        else if (label.equals("positive"))
            this.posLexicon.add(word);
    }
}
