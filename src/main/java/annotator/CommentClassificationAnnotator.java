package annotator;

import feature.LexiconExtractor;
import feature.SentiWordNetLexicon;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.ml.CleartkAnnotator;
import org.cleartk.ml.CleartkProcessingException;
import org.cleartk.ml.Instance;
import org.cleartk.ml.feature.extractor.*;
import org.cleartk.ml.feature.function.CharacterCategoryPatternFunction;
import org.cleartk.ml.feature.function.FeatureFunctionExtractor;
import org.cleartk.ml.feature.transform.InstanceStream;
import org.cleartk.ml.feature.transform.extractor.CentroidTfidfSimilarityExtractor;
import org.cleartk.ml.feature.transform.extractor.MinMaxNormalizationExtractor;
import org.cleartk.ml.feature.transform.extractor.TfidfExtractor;
import org.cleartk.ml.feature.transform.extractor.ZeroMeanUnitStddevExtractor;
import org.cleartk.ml.libsvm.LibSvmStringOutcomeDataWriter;
import org.cleartk.token.type.Sentence;
import org.cleartk.token.type.Token;
import type.CommentAnnotation;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.apache.uima.fit.util.JCasUtil.select;

public class CommentClassificationAnnotator extends CleartkAnnotator<String> {
    public static final String PARAM_TF_IDF_URI = "tfIdfUri";
    public static final String PARAM_TF_IDF_CENTROID_SIMILARITY_URI = "tfIdfCentroidSimilarityUri";
    public static final String PARAM_ZMUS_URI = "zmusUri";
    public static final String PARAM_MINMAX_URI = "minmaxUri";
    public static final String PARAM_DIRECTORY_NAME = "modelOutputDir";
    public static final String TFIDF_EXTRACTOR_KEY = "Token";
    public static final String CENTROID_TFIDF_SIM_EXTRACTOR_KEY = "CentroidTfIdfSimilarity";
    public static final String ZMUS_EXTRACTOR_KEY = "ZMUSFeatures";
    public static final String MINMAX_EXTRACTOR_KEY = "MINMAXFeatures";
    @ConfigurationParameter(
            name = PARAM_TF_IDF_URI,
            mandatory = false,
            description = "provides a URI where the tf*idf map will be written")
    protected URI tfIdfUri;
    @ConfigurationParameter(
            name = PARAM_TF_IDF_CENTROID_SIMILARITY_URI,
            mandatory = false,
            description = "provides a URI where the tf*idf centroid data will be written")
    protected URI tfIdfCentroidSimilarityUri;
    @ConfigurationParameter(
            name = PARAM_ZMUS_URI,
            mandatory = false,
            description = "provides a URI where the Zero Mean, Unit Std Dev feature data will be written")
    protected URI zmusUri;
    @ConfigurationParameter(
            name = PARAM_MINMAX_URI,
            mandatory = false,
            description = "provides a URI where the min-max feature normalizaation data will be written")
    protected URI minmaxUri;

    @ConfigurationParameter(name = PARAM_DIRECTORY_NAME,
            mandatory = false)
    private File modelOutputDir;
    private CombinedExtractor1<CommentAnnotation> extractor;

    public static URI createTokenTfIdfDataURI(File outputDirectoryName) {
        File f = new File(outputDirectoryName, TFIDF_EXTRACTOR_KEY + "_tfidf_extractor.dat");
        return f.toURI();
    }

    public static URI createIdfCentroidSimilarityDataURI(File outputDirectoryName) {
        File f = new File(outputDirectoryName, CENTROID_TFIDF_SIM_EXTRACTOR_KEY);
        return f.toURI();
    }

    public static URI createZmusDataURI(File outputDirectoryName) {
        File f = new File(outputDirectoryName, ZMUS_EXTRACTOR_KEY + "_zmus_extractor.dat");
        return f.toURI();
    }

    public static URI createMinMaxDataURI(File outputDirectoryName) {
        File f = new File(outputDirectoryName, MINMAX_EXTRACTOR_KEY + "_minmax_extractor.dat");
        return f.toURI();
    }

    List<FeatureExtractor1<CommentAnnotation>> features = new ArrayList<>();
    FeatureExtractor1<CommentAnnotation> sentimentLexicon;
    FeatureExtractor1<CommentAnnotation> sentiWordLexicon;
    FeatureExtractor1<CommentAnnotation> namedEntityExtractor;
    private FeatureExtractor1<Token> textCharPOSExtractor;
    private CleartkExtractor<Token, Token> contextExtractor;
    private CleartkExtractor<Token, Token> contextExtractor2;
    private CleartkExtractor<Token, Token> unigramFollowing;
    private CleartkExtractor<Token, Token> unigramPreceding;

    List<FeatureExtractor1<Token>> tokenFeatures = new ArrayList<>();
    private TypePathExtractor<Token> stemExtractor;
    private TypePathExtractor<Token> posExtractor;


    @SuppressWarnings("unchecked")
    @Override
    public void initialize(UimaContext context) throws ResourceInitializationException {
        super.initialize(context);
        // add feature extractors
        try {

            TfidfExtractor<String, CommentAnnotation> tfIdfExtractor = initTfIdfExtractor();

            CentroidTfidfSimilarityExtractor<String, CommentAnnotation> simExtractor = initCentroidTfIdfSimilarityExtractor();
            ZeroMeanUnitStddevExtractor<String, CommentAnnotation> zmusExtractor = initZmusExtractor();
            MinMaxNormalizationExtractor<String, CommentAnnotation> minmaxExtractor = initMinMaxExtractor();

            sentimentLexicon = new FeatureFunctionExtractor<CommentAnnotation>(new LexiconExtractor(new File("src/main/resources/sentiment/positive-negative-words.txt")));
            sentiWordLexicon = new FeatureFunctionExtractor<CommentAnnotation>(new SentiWordNetLexicon(new File("src/main/resources/sentiment/SentiWordNet_3.0.0_20130122.txt")));
            stemExtractor = new TypePathExtractor<Token>(Token.class, "stem");
            posExtractor = new TypePathExtractor<Token>(Token.class, "pos");

            // the token feature extractor: text, char pattern (uppercase, digits, etc.), and part-of-speech
            this.textCharPOSExtractor = new CombinedExtractor1<Token>(
                    new FeatureFunctionExtractor<Token>(
                            new CoveredTextExtractor<Token>(),
                            new CharacterCategoryPatternFunction<Token>(CharacterCategoryPatternFunction.PatternType.REPEATS_MERGED)),
                    new TypePathExtractor<Token>(Token.class, "pos"));

            this.contextExtractor = new CleartkExtractor<Token, Token>(
                    Token.class,
                    this.textCharPOSExtractor,
                    new CleartkExtractor.Preceding(3),
                    new CleartkExtractor.Following(3));
            this.contextExtractor2 = new CleartkExtractor<Token, Token>(
                    Token.class,
                    this.textCharPOSExtractor,
                    new CleartkExtractor.Preceding(2),
                    new CleartkExtractor.Following(2));

            this.unigramFollowing = new CleartkExtractor<Token, Token>(Token.class, new TypePathExtractor<Token>(Token.class, "pos"),
                    new CleartkExtractor.Ngram(new CleartkExtractor.Following(1), new CleartkExtractor.Focus()));
            this.unigramPreceding = new CleartkExtractor<Token, Token>(Token.class, new TypePathExtractor<Token>(Token.class, "pos"),
                    new CleartkExtractor.Ngram(new CleartkExtractor.Preceding(1), new CleartkExtractor.Focus()));
            
            CleartkExtractor<CommentAnnotation, Token> bagOfWords;
            CleartkExtractor<CommentAnnotation, Token> bagOfWords2;
            bagOfWords = new CleartkExtractor<CommentAnnotation, Token>(
                            Token.class,
                            new CoveredTextExtractor<Token>(),
                            new CleartkExtractor.Bag(new CleartkExtractor.Following(1, 2)));

                    bagOfWords2 =new CleartkExtractor<CommentAnnotation, Token>(
                            Token.class,
                            new CoveredTextExtractor<Token>(),
                            new CleartkExtractor.Bag(new CleartkExtractor.Preceding(1, 2)));

            /** Collecting all features in a CombinedExtractor1<T> **/
            this.extractor = new CombinedExtractor1<CommentAnnotation>(
                    tfIdfExtractor,
                    simExtractor,
                    zmusExtractor,
                    minmaxExtractor,
                    bagOfWords,
                    bagOfWords2,
                    sentiWordLexicon,
                    sentimentLexicon);

        } catch (IOException e) {
            throw new ResourceInitializationException(e);
        }

    }

    private TfidfExtractor<String, CommentAnnotation> initTfIdfExtractor() throws IOException {
        CleartkExtractor<CommentAnnotation, Token> countsExtractor = new CleartkExtractor<CommentAnnotation, Token>(
                Token.class,
                new CoveredTextExtractor<Token>(),
                new CleartkExtractor.Count(new CleartkExtractor.Covered()));

        TfidfExtractor<String, CommentAnnotation> tfIdfExtractor = new TfidfExtractor<String, CommentAnnotation>(
                CommentClassificationAnnotator.TFIDF_EXTRACTOR_KEY,
                countsExtractor);

        if (this.tfIdfUri != null) {
            tfIdfExtractor.load(this.tfIdfUri);
        }
        return tfIdfExtractor;
    }


    private CentroidTfidfSimilarityExtractor<String, CommentAnnotation> initCentroidTfIdfSimilarityExtractor()
            throws IOException {
        CleartkExtractor<CommentAnnotation, Token> countsExtractor = new CleartkExtractor<CommentAnnotation, Token>(
                Token.class,
                new CoveredTextExtractor<Token>(),
                new CleartkExtractor.Count(new CleartkExtractor.Covered()));

        CentroidTfidfSimilarityExtractor<String, CommentAnnotation> simExtractor = new CentroidTfidfSimilarityExtractor<String, CommentAnnotation>(
                CommentClassificationAnnotator.CENTROID_TFIDF_SIM_EXTRACTOR_KEY,
                countsExtractor);

        if (this.tfIdfCentroidSimilarityUri != null) {
            simExtractor.load(this.tfIdfCentroidSimilarityUri);
        }
        return simExtractor;
    }

    private ZeroMeanUnitStddevExtractor<String, CommentAnnotation> initZmusExtractor()
            throws IOException {
        CombinedExtractor1<CommentAnnotation> featuresToNormalizeExtractor = new CombinedExtractor1<CommentAnnotation>(
                new CountAnnotationExtractor<CommentAnnotation>(Sentence.class),
                new CountAnnotationExtractor<CommentAnnotation>(Token.class));

        ZeroMeanUnitStddevExtractor<String, CommentAnnotation> zmusExtractor = new ZeroMeanUnitStddevExtractor<String, CommentAnnotation>(
                ZMUS_EXTRACTOR_KEY,
                featuresToNormalizeExtractor);

        if (this.zmusUri != null) {
            zmusExtractor.load(this.zmusUri);
        }

        return zmusExtractor;
    }

    private MinMaxNormalizationExtractor<String, CommentAnnotation> initMinMaxExtractor()
            throws IOException {
        CombinedExtractor1<CommentAnnotation> featuresToNormalizeExtractor = new CombinedExtractor1<CommentAnnotation>(
                new CountAnnotationExtractor<CommentAnnotation>(Sentence.class),
                new CountAnnotationExtractor<CommentAnnotation>(Token.class));

        MinMaxNormalizationExtractor<String, CommentAnnotation> minmaxExtractor = new MinMaxNormalizationExtractor<String, CommentAnnotation>(
                MINMAX_EXTRACTOR_KEY,
                featuresToNormalizeExtractor);

        if (this.minmaxUri != null) {
            minmaxExtractor.load(this.minmaxUri);
        }

        return minmaxExtractor;
    }

    /**
     * Recursively going through all annotated comments.
     * During training we write each comment instance in the modelOutputDir
     * alone with its gold value. The written instances are then
     * read by the collectFeatures method to transform and train the data.
     **/

    @SuppressWarnings("unchecked")
    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {

        for (CommentAnnotation comment : select(jCas, CommentAnnotation.class)) {

            int commentBeginning = comment.getBegin();
            int commentEnd = comment.getEnd();
            Instance<String> instance = new Instance<String>();

            instance.addAll(this.extractor.extract(jCas, comment));
            //System.err.println("Comment: " + comment.getCoveredText());
            List<Token> tokens = JCasUtil.selectCovered(jCas, Token.class, comment);

            for (Token token : tokens) {
                instance.addAll(this.textCharPOSExtractor.extract(jCas, token));
                instance.addAll(this.contextExtractor.extract(jCas, token));
                //instance.addAll(this.stemExtractor.extract(jCas, token));
                //instance.addAll(this.posExtractor.extract(jCas, token));
                instance.addAll(this.contextExtractor2.extract(jCas, token));
                //instance.addAll(this.unigramFollowing.extract(jCas, token));
                //instance.addAll(this.unigramPreceding.extract(jCas, token));

            }

            if (this.isTraining()) {
                instance.setOutcome(comment.getGoldValue());
                this.dataWriter.write(instance);

            } else {
                String result = this.classifier.classify(instance.getFeatures());
                CommentAnnotation cmt = new CommentAnnotation(jCas, commentBeginning, commentEnd);
                cmt.setPredictValue(result);
                cmt.addToIndexes();

            }
        }
        if (this.isTraining()) {

            try {
                this.collectFeatures(this.modelOutputDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Transform features and write training data
     * In this phase, the normalization statistics are computed and the raw
     * features are transformed into normalized features.
     * Then the adjusted values are written with a DataWriter (libsvm in this case)
     * for training
     **/
    public void collectFeatures(File outputDirectory) throws IOException, CleartkProcessingException {

        Iterable<Instance<String>> instances = InstanceStream.loadFromDirectory(outputDirectory);

        System.err.println("Collection feature normalization statistics");

        /** Collect TF*IDF stats for computing tf*idf values on extracted tokens **/
        URI tfIdfDataURI = CommentClassificationAnnotator.createTokenTfIdfDataURI(outputDirectory);
        TfidfExtractor<String, CommentAnnotation> extractor = new TfidfExtractor<String, CommentAnnotation>(
                CommentClassificationAnnotator.TFIDF_EXTRACTOR_KEY);
        extractor.train(instances);
        extractor.save(tfIdfDataURI);


        /** Collect TF*IDF Centroid stats for computing similarity to corpus centroid **/
        URI tfIdfCentroidSimDataURI = CommentClassificationAnnotator.createIdfCentroidSimilarityDataURI(outputDirectory);
        CentroidTfidfSimilarityExtractor<String, CommentAnnotation> simExtractor = new CentroidTfidfSimilarityExtractor<String, CommentAnnotation>(
                CommentClassificationAnnotator.CENTROID_TFIDF_SIM_EXTRACTOR_KEY);
        simExtractor.train(instances);
        simExtractor.save(tfIdfCentroidSimDataURI);

        /** Collect ZMUS stats for feature normalization **/
        URI zmusDataURI = CommentClassificationAnnotator.createZmusDataURI(outputDirectory);
        ZeroMeanUnitStddevExtractor<String, CommentAnnotation> zmusExtractor = new ZeroMeanUnitStddevExtractor<String, CommentAnnotation>(
                CommentClassificationAnnotator.ZMUS_EXTRACTOR_KEY);
        zmusExtractor.train(instances);
        zmusExtractor.save(zmusDataURI);

        /** Collect MinMax stats for feature normalization **/
        URI minmaxDataURI = CommentClassificationAnnotator.createMinMaxDataURI(outputDirectory);
        MinMaxNormalizationExtractor<String, CommentAnnotation> minmaxExtractor = new MinMaxNormalizationExtractor<String, CommentAnnotation>(
                CommentClassificationAnnotator.MINMAX_EXTRACTOR_KEY);
        minmaxExtractor.train(instances);
        minmaxExtractor.save(minmaxDataURI);

        /** Rerun training data writer pipeline, to transform the extracted instances -- an alternative,
         * more costly approach would be to reinitialize the DocumentClassificationAnnotator above with
         * the URIs for the feature extractor.
         * In this example, we now write in the libsvm format **/

        System.err.println("Write out model training data");
        LibSvmStringOutcomeDataWriter dataWriter = new LibSvmStringOutcomeDataWriter(outputDirectory);
        for (Instance<String> instance : instances) {
            instance = extractor.transform(instance);
            instance = simExtractor.transform(instance);
            instance = zmusExtractor.transform(instance);
            instance = minmaxExtractor.transform(instance);
            dataWriter.write(instance);
        }
        dataWriter.finish();
    }
}


