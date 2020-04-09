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
import java.util.regex.Pattern;

import static org.apache.uima.fit.util.JCasUtil.select;

public class AnalyzeFeatures extends JCasAnnotator_ImplBase {
    public static final String PARAM_INPUT_FILE = "InputFile";
    /**
     * To make this class general, the path to the feature that is used
     * for the evaluation the tokenValuePath has to be set to the feature
     * e.g. for the pos value: pos/PosValue is used
     * (works only for token: de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token)
     */
    @ConfigurationParameter(name = PARAM_INPUT_FILE, mandatory = true)
    private String inputFile;
    Logger logger = UIMAFramework.getLogger(AnalyzeFeatures.class);

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
        // TODO Auto-generated method stub
        try {
            logger.log(Level.INFO, "Start analyzing results");
            String line;
            String[] splitLine;
            BufferedReader reader = new BufferedReader(
                    new FileReader(inputFile));
            int correct = 0;
            int wrong = 0;
            int tokenCount = 0;
            int commentCount = 0;
            int truePositive = 0;
            int falsePositive = 0;
            int trueNegative = 0;
            int falseNegative = 0;

            String[] labels = {"negative", "positive"};

            boolean writeToEval = true;
            PrintWriter writer = new PrintWriter(new FileOutputStream("src/main/resources/res/twitter_evalRes.txt", false));
            Pattern p = Pattern.compile(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*(?![^\\\"]*\\\"))");

            for (CommentAnnotation comment : select(jCas, CommentAnnotation.class)) {
                if (comment.getGoldValue() == null) {
                    commentCount++;
                    line = reader.readLine();
                    splitLine = p.split(line);
                    String trueValue = splitLine[1];
                    //logger.log(Level.INFO, comment.getCoveredText() + " " + comment.getPredictValue() + " " + comment.getGoldValue());
                    String classifiedValue = comment.getPredictValue();

                    if (classifiedValue.equals("negative") && labels[Integer.parseInt(trueValue)].equals("negative"))
                        trueNegative++;
                    else if (classifiedValue.equals("negative") && labels[Integer.parseInt(trueValue)].equals("positive"))
                        falseNegative++;
                    else if (classifiedValue.equals("positive")&& labels[Integer.parseInt(trueValue)].equals("positive"))
                        truePositive++;
                    else if(classifiedValue.equals("positive") && labels[Integer.parseInt(trueValue)].equals("negative"))
                        falsePositive++;

                    if (splitLine[3].equals(comment.getCoveredText())) {
                        if (labels[Integer.parseInt(trueValue)].equals(classifiedValue)) {
                            correct++;
                            logger.log(Level.INFO, "Correct tag: " + labels[Integer.parseInt(trueValue)] + ", predicted tag: " + classifiedValue);
                        } else {
                            wrong++;
                        }
                        tokenCount++;
                    } else {
                        logger.log(
                                Level.WARNING,
                                "Token of predicting file does not match to text ("
                                        + splitLine[3] + "!="
                                        + comment.getCoveredText() + ")");
                    }
                    // write to new file here:
                    if (writeToEval) {
                        writer.println(line + " " + classifiedValue);
                    }
                }
            }
            reader.close();
            if (writeToEval) {
                writer.close();
            }

            float precision = (float) 100 * truePositive / (truePositive + falsePositive);
            float recall = (float) 100 * truePositive / (truePositive + falseNegative);
            float fscore = (float) 2 * ((precision * recall )/ (precision + recall));

            logger.log(Level.INFO, "Correct: " + correct);
            logger.log(Level.INFO, "Error: " + wrong);
            logger.log(Level.INFO, "Total: " + tokenCount);
            logger.log(Level.INFO, "Total comments " + commentCount);
            logger.log(Level.INFO, "Precision " + precision);
            logger.log(Level.INFO, "Recall " + recall);
            logger.log(Level.INFO, "F1 " + fscore);

            //logger.log(Level.INFO, "Total of positive comments " + posCount);
            //logger.log(Level.INFO, "Total of negative comments " + negCount);


            /*logger.log(Level.INFO, "Correct rate: " + correct / tokenCount);
            logger.log(Level.INFO, "Error rate: " + wrong / tokenCount);*/


        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, e.getMessage());
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        }

    }

}
