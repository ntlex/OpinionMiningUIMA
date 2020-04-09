package reader;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import com.rometools.utils.Integers;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import type.CommentAnnotation;

import java.util.regex.Pattern;

public class TwitterReader extends JCasAnnotator_ImplBase {

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
        StringBuffer docText = new StringBuffer();
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

        String[] tweets = tbText.split("(\r\n|\n)");
        
        int idx = 0;
        Token token = null;
        CommentAnnotation comment = null;
        String sentimentLabel;
        Sentence sentence = null;

        for (String line : tweets) {
            // new sentence if there's a new line
            String[] tag = p.split(line);
            String startsWith = tag[0];
            if((!startsWith.contains("ItemID")) && (tag.length == 4)){
                String tweet = tag[3];
                sentimentLabel = tag[1];
                
                docText.append(tweet);
                docText.append(" ");

                if(sentimentLabel.equals("1"))
                    sentimentLabel = "positive";
                else sentimentLabel = "negative";
                
                comment = new CommentAnnotation(docView, idx, idx + tweet.length());
                sentence = new Sentence(docView, idx, idx + tweet.length());
                
                String[] words = tweet.split("\\s+");
                int current = 0;
                int position = 0;
                int tokenStart = 0;
                int tokenEnd = 0;
                
                for (String word : words){
                	do{
                		position = tweet.indexOf(word, current);
                	} while (position < current);
                	current = position + 1;
                	tokenStart = idx + position;
                	tokenEnd = tokenStart + word.length();
                	token = new Token(docView, tokenStart, tokenEnd);
                	token.addToIndexes();

                	/*logger.log(Level.INFO, String.valueOf(token.getBegin()));
                	logger.log(Level.INFO, String.valueOf(token.getEnd()));

                	 logger.log(Level.INFO,
                             "Token: [" + docText.substring(token.getBegin(), token.getEnd()) + "]"
                                     + token.getBegin() + "\t" + token.getEnd());*/
                }

                
                idx++;

                // increment actual index of text
                idx += tweet.length();
                comment.setGoldValue(sentimentLabel);


                //logger.log(Level.INFO, comment.getCoveredText());
                comment.addToIndexes();
                sentence.addToIndexes();
                
                /*logger.log(
                        Level.INFO,
                        "TwitterBody: ["
                                + docText.substring(comment.getBegin(), comment.getEnd()) + "]" + comment.getBegin()
                                + "\t" + comment.getEnd());*/
            }
        }

        docView.setSofaDataString(docText.toString(), "text/plain");
    }
}
