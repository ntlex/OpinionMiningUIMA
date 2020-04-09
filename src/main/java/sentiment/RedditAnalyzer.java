package sentiment;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import type.CommentAnnotation;

import java.io.*;

import static org.apache.uima.fit.util.JCasUtil.select;

public class RedditAnalyzer extends JCasAnnotator_ImplBase {
    Logger logger = UIMAFramework.getLogger(RedditAnalyzer.class);

    public static final String PARAM_OUTPUT_DIR = "InputFile";

    @ConfigurationParameter(name = PARAM_OUTPUT_DIR, mandatory = true)
    private String outputDir;

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        int commentCount = 0;
        int posCount = 0;
        int negCount = 0;
        try {
            String filenamePositive = outputDir + "/Elon_positive_comments.txt";
            String filenameNegative = outputDir + "/Elon_negative_comments.txt";

            BufferedWriter positiveWriter = new BufferedWriter(new FileWriter(filenamePositive, true));
            BufferedWriter negativeWriter = new BufferedWriter(new FileWriter(filenameNegative, true));

            for (CommentAnnotation comment : select(jCas, CommentAnnotation.class)) {
                String classifiedValue = comment.getPredictValue();

                if (comment.getPredictValue() != null) {
                    commentCount++;
                    if (classifiedValue.equals("negative")) {
                        negativeWriter.write(comment.getCoveredText());
                        negCount++;
                    } else if (classifiedValue.equals("positive")) {
                        positiveWriter.write(comment.getCoveredText());
                        posCount++;
                    }

                }

            }
            positiveWriter.close();
            negativeWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        float posPercentage = (float) posCount / commentCount * 100;
        float negPercentage = (float) negCount / commentCount * 100;


        logger.log(Level.INFO, "Total comments " + commentCount);
        logger.log(Level.INFO, "Total of positive comments " + posCount);
        logger.log(Level.INFO, "Total of negative comments " + negCount);
        logger.log(Level.INFO, "Percentage of positive comments " + posPercentage + "%");
        logger.log(Level.INFO, "Percentage of negative comments " + negPercentage + "%");

    }

}
