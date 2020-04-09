package webCrawlers.twitterCrawler;

import org.joda.time.DateTime;
import twitter4j.*;
import webCrawlers.utils.CorpusWriter;
import java.util.List;

public class TweetsCrawler {
    private static final String resourcesDir = System.getProperty("user.dir")+"/src/main/resources/tweets/";

    public static void main(String[] args){

        Twitter twitter = new TwitterFactory().getInstance();
        DateTime lastWeek = new DateTime().minusDays(7);
        CorpusWriter corpusWriter = new CorpusWriter();

        try {
            Query query = new Query("#elonmusk"+"+exclude:retweets");
            query.setSince(lastWeek.toLocalDate().toString());
            QueryResult result;
            do{
                result = twitter.search(query);
                List<Status> tweets = result.getTweets();
                for (Status tweet : tweets) {
                    if(tweet.getLang().equals("en")){
                        corpusWriter.writeToFile(tweet.getText(), tweet.getCreatedAt(), "tweets", resourcesDir);
                        System.out.println("TWEET " + tweet.getText());
                    }
                }
            }while ((query = result.nextQuery()) != null);
            System.exit(0);

        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to search tweets: " + te.getMessage());
            System.exit(-1);
    }
    }


}
