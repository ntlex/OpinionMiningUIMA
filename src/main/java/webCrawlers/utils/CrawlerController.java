package webCrawlers.utils;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import webCrawlers.redditDataset.RedditCrawler;

public class CrawlerController {

    public static void main(String[] args) throws Exception {
        String crawlStorageFolder = "src/main/resources/news-store/test.txt";
        int numberOfCrawlers = 1;

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlStorageFolder);
        config.setMaxDepthOfCrawling(1);
        config.setMaxPagesToFetch(50);
        config.setPolitenessDelay(30000);
        /*
         * Instantiate the controller for this crawl.
         */
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        robotstxtConfig.setEnabled(false);
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

        /*
         * For each crawl, you need to add some seed urls. These are the first
         * URLs that are fetched and then the crawler starts following links
         * which are found in these pages
         */
        //controller.addSeed("https://www.newyorker.com/tag/elon-musk");
        controller.addSeed("https://www.reddit.com/search?q=Elon+Musk&count=22&after=t3_7lhf40");
        controller.addSeed("https://www.reddit.com/search?q=Elon+Musk&count=47&after=t3_5yl7jx");
        controller.addSeed("https://www.reddit.com/search?q=Elon+Musk&count=72&after=t3_59ybbd");
        controller.addSeed("https://www.reddit.com/search?q=Elon+Musk&count=97&after=t3_72t069");




        /*
         * Start the crawl. This is a blocking operation, meaning that your code
         * will reach the line after this only when crawling is finished.
         */
        //controller.start(NewYorkerCrawler.class, numberOfCrawlers);
        controller.start(RedditCrawler.class, numberOfCrawlers);
    }
}
