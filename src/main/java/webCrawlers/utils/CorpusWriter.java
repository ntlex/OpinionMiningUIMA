package webCrawlers.utils;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CorpusWriter {

    public void writeToFile(String articleContent, Date articlePubDate, String type, String resourcesDir) {

        String filenameDate = getDate(articlePubDate, type);
        String filename = resourcesDir + type + "_" + filenameDate + ".txt";
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(filename, true));
            writer.write(articleContent);
            System.out.println("Content written in file" + filename);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (Exception ex) {
                System.out.println("Error in closing the BufferedWriter" + ex);
            }
        }
    }

    public String getDate(Date date, String type) {

        String filenameDate = null;
        SimpleDateFormat getMonth = new SimpleDateFormat("MM");
        SimpleDateFormat getYear = new SimpleDateFormat("YYYY");

        if (type.equals("news") || type.equals("reddit")) {
            filenameDate = getMonth.format(date)+ "-" + getYear.format(date);
        } else if (type.equals("tweets")) {
            filenameDate = getYear.format(date);
        } else if(type.equals("EM_Reddit")){
            filenameDate = getMonth.format(date)+ "-" + getYear.format(date);
        }
        return filenameDate;
    }
    //"cat "+ filename+" | perl -lane \"print if /\\S/\" > test_txt.txt";
}