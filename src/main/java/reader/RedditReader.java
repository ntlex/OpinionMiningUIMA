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

public class RedditReader extends JCasAnnotator_ImplBase {

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
        Sentence reddit = null;
        int idx = 0;
        Token token = null;
        CommentAnnotation Comment;
        boolean initReddit = false;
        StringBuffer docText = new StringBuffer();

        StringBuffer redditSb = new StringBuffer();


        for (String line : tokens) {

            // new reddit comment if there's a new empty line
            if (line.trim().isEmpty()) {
                if (reddit != null && token != null) {
                    terminateRedditComment(reddit, token, docText);
                    docText.append("\n");
                    idx++;
                }
                // init new reddit comment with the next recognized token
                initReddit = true;
                redditSb = new StringBuffer();
            } else {
                String[] tag = line.split("\n");

                String reddit_comment = tag[0];

                docText.append(reddit_comment);
                redditSb.append(reddit_comment + " ");


                token = new Token(docView, idx, idx + reddit_comment.length());
                Comment = new CommentAnnotation(docView, idx, idx + reddit_comment.length());

                docText.append(" ");
                idx++;

                // start new reddit comment
                if (initReddit) {
                    reddit = new Sentence(docView);
                    reddit.setBegin(token.getBegin());
                    initReddit = false;
                }
                // increment actual index of text
                idx += reddit_comment.length();

                Comment.addToIndexes();
                //token.addToIndexes();

                logger.log(Level.FINE,
                        "Token: [" + docText.substring(token.getBegin(), token.getEnd()) + "]"
                                + token.getBegin() + "\t" + token.getEnd());
                logger.log(
                        Level.FINE,
                        "Reddit Comment: ["
                                + docText.substring(Comment.getBegin(),
                                Comment.getEnd()) + "]" + Comment.getBegin()
                                + "\t" + Comment.getEnd());


            }
        }
        if (reddit != null && token != null) {
            terminateRedditComment(reddit, token, docText);
        }

        docView.setSofaDataString(docText.toString(), "text/plain");
    }


    private void terminateRedditComment(Sentence reddit, Token token, StringBuffer docText) {
        reddit.setEnd(token.getEnd());
        reddit.addToIndexes();
        logger.log(Level.FINE,
                "Reddit:[" + docText.substring(reddit.getBegin(), reddit.getEnd()) + "]\t"
                        + reddit.getBegin() + "\t" + reddit.getEnd());
    }
}
