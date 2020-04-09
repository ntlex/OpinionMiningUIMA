package reader;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import type.CommentAnnotation;

import java.util.regex.Pattern;

public class TweetReader extends JCasAnnotator_ImplBase {

    public static final String CONLL_VIEW = "ConnlView";
    private Logger logger = null;

    @Override
    public void initialize(UimaContext context)
            throws ResourceInitializationException {
        super.initialize(context);
        logger = context.getLogger();
    }

    @Override
    public void process(JCas jcas)
            throws AnalysisEngineProcessException {
        JCas docView;
        String tbText;
        Pattern p = Pattern.compile(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*(?![^\\\"]*\\\"))");
        try {
            docView = jcas.getView(CAS.NAME_DEFAULT_SOFA);
            tbText = jcas.getView(CONLL_VIEW).getDocumentText();
        } catch (CASException e) {
            throw new AnalysisEngineProcessException(e);
        }
        // a new sentence always starts with a new line
        if (tbText.charAt(0) != '\n') {
            tbText = "\n" + tbText;
        }

        String[] tokens = tbText.split("(\r\n|\n)");
        Sentence sentence = null;
        int idx = 0;
        Token token = null;
        CommentAnnotation Comment;
        String SentimentLabel;
        boolean initSentence = false;
        StringBuffer docText = new StringBuffer();

        StringBuffer sentenceSb = new StringBuffer();


        for (String line : tokens) {

            // new sentence if there's a new line
            if (line.equals("")) {
                if (sentence != null && token != null) {
                    terminateSentence(sentence, token, docText);
                    docText.append("\n");
                    idx++;
                }
                // init new sentence with the next recognized token
                initSentence = true;
                sentenceSb = new StringBuffer();
            } else {
                String[] tag = p.split(line);
                String startsWith = tag[0];
                if (!startsWith.contains("ItemID")) {
                    String tweet = tag[3];
                    SentimentLabel = tag[1];

                    if (SentimentLabel.equals("1"))
                        SentimentLabel = "positive";
                    else SentimentLabel = "negative";

                    docText.append(tweet);
                    sentenceSb.append(tweet + " ");


                    token = new Token(docView, idx, idx + tweet.length());
                    Comment = new CommentAnnotation(docView, idx, idx + tweet.length());

                    docText.append(" ");
                    idx++;

                    // start new sentence
                    if (initSentence) {
                        sentence = new Sentence(docView);
                        sentence.setBegin(token.getBegin());
                        initSentence = false;
                    }
                    // increment actual index of text
                    idx += tweet.length();
                    Comment.setGoldValue(SentimentLabel);


                    logger.log(Level.INFO, Comment.getCoveredText());
                    Comment.addToIndexes();
                    //token.addToIndexes();

                    logger.log(Level.FINE,
                            "Token: [" + docText.substring(token.getBegin(), token.getEnd()) + "]"
                                    + token.getBegin() + "\t" + token.getEnd());
                    logger.log(
                            Level.FINE,
                            "TwitterBody: ["
                                    + docText.substring(Comment.getBegin(),
                                    Comment.getEnd()) + "]" + Comment.getBegin()
                                    + "\t" + Comment.getEnd());
                }

            }
        }
        if (sentence != null && token != null) {
            terminateSentence(sentence, token, docText);
        }

        docView.setSofaDataString(docText.toString(), "text/plain");
    }


    private void terminateSentence(Sentence sentence, Token token, StringBuffer docText) {
        sentence.setEnd(token.getEnd());
        sentence.addToIndexes();
        logger.log(Level.FINE,
                "Sentence:[" + docText.substring(sentence.getBegin(), sentence.getEnd()) + "]\t"
                        + sentence.getBegin() + "\t" + sentence.getEnd());
    }
}
