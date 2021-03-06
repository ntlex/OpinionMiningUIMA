package webCrawlers.redditDataset;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import webCrawlers.utils.CorpusWriter;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class RedditCrawler extends WebCrawler {
    private static final String resourcesDir = System.getProperty("user.dir")+"/src/main/resources/redditDatasets/ElonMusk/";
    private final static String articleBodyClass = "div.commentarea div.usertext-body div.md";
    private final static String shortURL = "https://www.reddit.com/";


    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg"
            + "|png|mp3|mp4|zip|gz))$");

    /**
     * This method receives two parameters. The first parameter is the page
     * in which we have discovered this new url and the second parameter is
     * the new url. You should implement this function to specify whether
     * the given url should be crawled or not (based on your crawling logic).
     * In this example, we are instructing the crawler to ignore urls that
     * have css, js, git, ... extensions and to only accept urls that start
     * with "http://www.ics.uci.edu/". In this case, we didn't need the
     * referringPage parameter to make the decision.
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();

        // Ignore the url if it has an extension that matches our defined set of image extensions.
        if (FILTERS.matcher(href).matches()) {
            return false;
        }

        return href.startsWith(shortURL) && href.contains("comments");

    }

    /**
     * This function is called when a page is fetched and ready
     * to be processed by your program.
     */
    @Override
    public void visit(Page page) {
        Document document;
        Elements articleBody;

        CorpusWriter corpusWriter = new CorpusWriter();

        String url = page.getWebURL().getURL();
        System.out.println("URL: " + url);
        try {
            document = Jsoup.connect(url).userAgent("Mozilla").timeout(0).get();
            Elements threadBody = document.select("body");
            articleBody = threadBody.select(articleBodyClass).append("\\n\\n");
            String test = articleBody.text().replaceAll("\\\\n", "\n");
            Elements entry = threadBody.select("div.content div.entry.unvoted");
            Elements time = entry.select("time");

            String headerText = time.text();
            Date articleDate = extractDate(headerText);
            if(articleDate != null && articleBody.size() > 0)
                corpusWriter.writeToFile(test, articleDate, "EM_Reddit", resourcesDir);

        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    public Date extractDate(String articleHeader) {
        List<Date> articleDate;
        Date date = null;
        if(!articleHeader.equals(null)){
            articleDate = new PrettyTimeParser().parse(articleHeader);
            if(articleDate.size()!=0) {
                date = articleDate.get(articleDate.size() - 1);
            }
            else
                System.out.println("Article doesn't contain a date");
        }else
            System.out.println("Article header is empty");

        return date;
    }
}

