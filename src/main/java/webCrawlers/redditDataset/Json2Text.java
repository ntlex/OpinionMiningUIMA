package webCrawlers.redditDataset;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import webCrawlers.utils.CorpusWriter;

public class Json2Text {
    private static final String resourcesDir = System.getProperty("user.dir")+"/src/main/resources/redditDatasets/";

    private static final String filePath = System.getProperty("user.dir")+"/src/main/resources/redditDatasets/json/";

    public static void main(String[] args) {
        CorpusWriter corpusWriter = new CorpusWriter();
        File jsonDir = new File(filePath);
        File[] directoryList = jsonDir.listFiles();

        try {
            if(directoryList!=null){
                for(File file: directoryList){
                    FileReader reader = new FileReader(file);

                    JSONParser jsonParser = new JSONParser();
                    JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

                    // get a String from the JSON object

                    JSONArray comments= (JSONArray) jsonObject.get("reddits");

                    Iterator i = comments.iterator();

                    System.out.println("Extracting reddit comments from "+file + " to text.");

                    // take each value from the json array separately
                    while (i.hasNext()) {
                        JSONObject innerObj = (JSONObject) i.next();
                        Long date = (Long) innerObj.get("created_utc");
                        Date commentDate = new Date(Long.parseLong(String.valueOf(date))* 1000);
                        String commentBody = (String) innerObj.get("body");

                        corpusWriter.writeToFile(commentBody,commentDate , "reddit", resourcesDir);
                    }
                }
            }


        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (ParseException ex) {
            ex.printStackTrace();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

    }

}