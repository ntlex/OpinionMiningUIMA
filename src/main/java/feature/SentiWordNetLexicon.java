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

public class SentiWordNetLexicon implements FeatureExtractor1<CommentAnnotation> {

    private final List<String> posLexicon = new ArrayList<>();
    private final List<String> negLexicons = new ArrayList<>();

    public SentiWordNetLexicon(File wordList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(wordList));) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] split = line.split("\t");
                double positiveScore = Double.parseDouble(split[2]);
                double negativeScore = Double.parseDouble(split[3]);
                String[] wordCleanup = split[4].split("#");
                String word = wordCleanup[0];
                addWordToLexicon(word, negativeScore, positiveScore);

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
                    // if (!negWordsFound.contains(word))
                     //  negWordsFound.add(word);
                    negCount++;
                } else if (posLexicon.contains(word)) {
                     // if (!posWordsFound.contains(word))
                      //  posWordsFound.add(word);
                    posCount++;
                }

            }

        }

        //Feature negativeWords = new Feature("negativeWords ", negWordsFound);
        //Feature positiveWords = new Feature("positiveWords ", posWordsFound);
        Feature positiveFeature = new Feature("positive " + posCount);
        Feature negativeFeature = new Feature("negative " + negCount);

        //System.out.println("Count of positive words "+posCount+ " and negative words "+negCount+" contained in this comments" );

        //customFeatures.add(negativeWords);
        //customFeatures.add(positiveWords);
        customFeatures.add(positiveFeature);
        customFeatures.add(negativeFeature);

        return customFeatures;

    }

    private void addWordToLexicon(String word, double negativeScore, double positiveScore) {

        if (negativeScore > 0)
            this.negLexicons.add(word);
        else if (positiveScore > 0)
            this.posLexicon.add(word);
    }
}
