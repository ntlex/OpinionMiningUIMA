package sentiment;

import annotator.CommentClassificationAnnotator;
import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.testing.util.HideOutput;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.cleartk.ml.feature.transform.InstanceDataWriter;
import org.cleartk.ml.jar.DefaultDataWriterFactory;
import org.cleartk.ml.jar.DirectoryDataWriterFactory;
import org.cleartk.ml.jar.GenericJarClassifierFactory;
import org.cleartk.ml.jar.JarClassifierBuilder;
import org.cleartk.opennlp.tools.SentenceAnnotator;
import org.cleartk.snowball.DefaultSnowballStemmer;
import org.cleartk.token.tokenizer.TokenAnnotator;
import org.cleartk.util.cr.FilesCollectionReader;
import reader.RedditReader;
import reader.TweetReader;
import reader.TwitterReader;

import java.io.File;
import java.io.IOException;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;

public class ExecuteSentiment {

    public static void writeModel(File saTrain, String modelDirectory, String language)
            throws ResourceInitializationException, UIMAException, IOException {
        System.err.println("Step 1: Extracting features and writing raw instances data");
        runPipeline(
                FilesCollectionReader.getCollectionReaderWithSuffixes(saTrain.getAbsolutePath(),
                        TweetReader.CONLL_VIEW, saTrain.getName()),
                createEngine(TwitterReader.class),
                createEngine(AnalysisEngineFactory.createEngineDescription(
                        SentenceAnnotator.getDescription(),
                        TokenAnnotator.getDescription()
                        )),
                createEngine(AnalysisEngineFactory.createEngineDescription(
                        CommentClassificationAnnotator.class,
                        CommentClassificationAnnotator.PARAM_IS_TRAINING, true,
                        CommentClassificationAnnotator.PARAM_DIRECTORY_NAME, modelDirectory,
                        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME,
                        InstanceDataWriter.class.getName(),
                        DirectoryDataWriterFactory.PARAM_OUTPUT_DIRECTORY,
                        modelDirectory))

        );
    }

    public static void trainModel(File modelDirectory, String[] arguments) throws Exception {
        /** Stage 3: Train and write model
         * Now that the features have been extracted and normalized, we can proceed
         *in running machine learning to train and package a model **/

        System.err.println("Train model and write model.jar file.");
        HideOutput hider = new HideOutput();
        JarClassifierBuilder.trainAndPackage(modelDirectory, arguments);
        hider.restoreOutput();
    }

    public static void classifyTestFile(File modelDirectory, File saTest, String language, File reddit)
            throws ResourceInitializationException, UIMAException, IOException {

        runPipeline(
                FilesCollectionReader.getCollectionReaderWithSuffixes(saTest.getAbsolutePath(),
                        RedditReader.CONLL_VIEW, saTest.getName()),
                createEngine(RedditReader.class),
                //createEngine(TwitterReader.class),
                createEngine(AnalysisEngineFactory.createEngineDescription(
                        SentenceAnnotator.getDescription(),
                        TokenAnnotator.getDescription()
                       )),
                createEngine(AnalysisEngineFactory.createEngineDescription(
                        CommentClassificationAnnotator.class,
                        CommentClassificationAnnotator.PARAM_IS_TRAINING, false,
                        CommentClassificationAnnotator.PARAM_DIRECTORY_NAME, modelDirectory,
                        CommentClassificationAnnotator.PARAM_TF_IDF_URI,
                        CommentClassificationAnnotator.createTokenTfIdfDataURI(modelDirectory),
                        CommentClassificationAnnotator.PARAM_TF_IDF_CENTROID_SIMILARITY_URI,
                        CommentClassificationAnnotator.createIdfCentroidSimilarityDataURI(modelDirectory),
                        CommentClassificationAnnotator.PARAM_MINMAX_URI,
                        CommentClassificationAnnotator.createMinMaxDataURI(modelDirectory),
                        CommentClassificationAnnotator.PARAM_ZMUS_URI,
                        CommentClassificationAnnotator.createZmusDataURI(modelDirectory),
                        GenericJarClassifierFactory.PARAM_CLASSIFIER_JAR_PATH, modelDirectory + "/model.jar")
                ),
                createEngine(RedditAnalyzer.class, RedditAnalyzer.PARAM_OUTPUT_DIR, reddit.getAbsolutePath()));
                //createEngine(AnalyzeFeatures.class, AnalyzeFeatures.PARAM_INPUT_FILE, saTest.getAbsolutePath()));

    }

    public static void main(String[] args) throws Exception {

        String[] trainingArguments = new String[]{"-t", "0"};
        long start = System.currentTimeMillis();

        String modelDirectory = "src/test/resources/model/";
        File modelDir = new File("src/test/resources/model/");
        String redditModelDirectory = "src/test/resources/redditDatasets/results/";
        File redditModelDir = new File("src/test/resources/redditDatasets/results/");
        String language = "English";
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/Elon_small.txt");
        File sentimentTrain = new File("src/main/resources/twitterDataset/Twitter_SA_Dataset_small.csv");
        //File sentimentTrain = new File("src/main/resources/twitterDataset/Twitter_SA_Dataset_small.csv");
        //File sentimentTest = new File("src/main/resources/twitterDataset/twitter_Dataset_test_2.csv");
        //File sentimentTest = new File("src/main/resources/twitterDataset/twitter_small_2.csv");
        //File sentimentTrain = new File("src/main/resources/twitterDataset/Twitter_SA_Dataset_small.csv");

        new File(modelDirectory).mkdirs();
        new File(redditModelDirectory).mkdirs();

        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_01-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_02-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_03-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_04-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_05-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_06-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_07-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_08-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_09-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_10-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_11-2017.txt");
        File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_12-2017.txt");
        //File sentimentTest = new File("src/main/resources/redditDatasets/ElonMusk/EM_Reddit_01-2018.txt");

        //writeModel(sentimentTrain, modelDirectory, language);
        //trainModel(modelDir, trainingArguments);
        //classifyTestFile(modelDir, sentimentTest, language, redditModelDir);
        classifyTestFile(modelDir, sentimentTest, language, redditModelDir);

        long now = System.currentTimeMillis();
        UIMAFramework.getLogger().log(Level.INFO, "Time: " + (now - start) + "ms");
    }

}
