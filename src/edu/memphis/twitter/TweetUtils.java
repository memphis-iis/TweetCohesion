package edu.memphis.twitter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

//Used to detect language
import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import com.cybozu.labs.langdetect.Language;

public class TweetUtils {
	
	// TweetUtils will expect the profiles to be in a directory contained in the working directory
	static String LANG_PROFILES = Paths.get("").resolve("profiles").toAbsolutePath().toString();

	private TweetUtils instance;
	
	static Map<String, Integer> langStatistic = new HashMap<String, Integer>();

	private TweetUtils() {		
	}
	
	public static void init(){
		//load language profiles
		try {
			DetectorFactory.loadProfile(LANG_PROFILES);
		} catch (LangDetectException e) {
			e.printStackTrace();
		}
	}
	

	public TweetUtils getInstance() {
		if (instance == null) {
			instance = new TweetUtils();
		}
		return instance;
	}
	
	//clean tweet content
	public static String clean(String tweetTxt) {
		String[] w = tweetTxt.trim().split(" ");
		StringBuffer sb = new StringBuffer();
		for (String word : w) {
			if (word.startsWith("@") && word.endsWith(":")) {
				continue;
			}
			if (word.startsWith("http://")) {
				continue;
			}
			if (tweetTxt.trim().startsWith("RT") && word.equalsIgnoreCase("RT")) {
				continue;
			}
			word = word.replaceAll("[^A-Za-z']", "");
			sb.append(word + " ");
		}
		return sb.toString().trim();
	}

	public static List<Tweet> getTweetsFromTxt(String filePath) {	
		init();
		List<Tweet> tweets = new ArrayList<Tweet>();
		BufferedReader br;
		try {       
			// 20120303211302Los
			br = new BufferedReader(new InputStreamReader (new FileInputStream(filePath), "UTF8"));
			Tweet tweet;
			//br = new BufferedReader(new FileReader(filePath, "UTF8"));
			String line = br.readLine();
			String dateTime;
			String text;
			while (line != null) {
				tweet = new Tweet();
				dateTime = line.substring(0, 14);
				text = line.substring(14);
				tweet.setTimeStr(dateTime);
				tweet.setText(text);				
				tweet.setLangCode(detect(text));
				tweets.add(tweet);
				line = br.readLine();
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		for(String lang : langStatistic.keySet()){
			System.out.println(lang + ":" + langStatistic.get(lang));
		}
		return tweets;
	}

	public static List<Tweet> getTweets(String filePath) {
		List<Tweet> tweets = new ArrayList<Tweet>();
		try {
			XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(filePath));
			XSSFRow row;
			XSSFCell cell;
			int rowNum = 1;
			XSSFSheet sheet = wb.getSheetAt(0);
			Tweet tweet;
			while (true) {
				tweet = new Tweet();
				row = sheet.getRow(rowNum);
				if (row == null || row.getCell(0) == null) {
					break;
				}
				cell = row.getCell(0);
				tweet.setTime(cell.getDateCellValue());

				cell = row.getCell(2);
				try {
					tweet.setText(cell.getRichStringCellValue().toString());
				} catch (Exception e) {
					System.out.println("WARNING..."
							+ tweet.getTime().toString());
					tweet.setText(cell.toString());
				}

				cell = row.getCell(3);
				tweet.setArabic(cell.getBooleanCellValue());
				cell = row.getCell(6);
				tweet.setLangCode(cell.getRichStringCellValue().toString());
				rowNum++;
				tweets.add(tweet);
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tweets;
	}
	
	public static void writeTweetsTxt(List<Tweet> tweets, String filePath) {
		try {
			// Create file
			FileWriter fstream = new FileWriter(new File(filePath), true);
			BufferedWriter out = new BufferedWriter(fstream);
			for(Tweet t : tweets){
				out.write(t.getTimeStr()+","+ t.getText());
				out.write("\n");
			}			
			out.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public static void writeTxt(List<String[]> results, String filePath) {
		try {
			// Create file
			FileWriter fstream = new FileWriter(new File(filePath), true);
			BufferedWriter out = new BufferedWriter(fstream);
			for(String[] strArr : results){
				out.write(strArr[0]+","+ strArr[1] +","+ strArr[2]+"," + strArr[3]);
				out.write("\n");
			}			
			out.close();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public static void writeTweets(List<Tweet> tweets, String filePath) {
		String path = "D:\\LSA\\t.xlsx";
		try {
			XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(filePath));
			XSSFSheet sheet = wb.getSheetAt(0);
			// Rows
			int rowNum = 1;
			for (Tweet t : tweets) {
				XSSFRow row = sheet.createRow(rowNum);
				XSSFCell cell = row.createCell(0);
				cell.setCellValue(t.getTime().toString());

				cell = row.createCell(1);
				cell.setCellValue(t.getText().toString());

				cell = row.createCell(2);
				cell.setCellValue(t.getVector().toString());

				cell = row.createCell(3);
				cell.setCellValue(t.getLangCode());
				rowNum++;
			}
			FileOutputStream fo = new FileOutputStream(path);
			wb.write(fo);
			fo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void writeResults(List<String[]> results, String filePath) {
		try {
			XSSFWorkbook wb = new XSSFWorkbook(new FileInputStream(filePath));
			XSSFSheet sheet = wb.getSheetAt(0);
			// Rows
			int rowNum = 1;
			XSSFRow row;
			XSSFCell cell;
			for (String[] strs : results) {
				row = sheet.createRow(rowNum);
				for (int i = 0; i < strs.length; i++) {
					cell = row.createCell(i);
					cell.setCellValue(strs[i]);
				}
				rowNum++;
			}
			FileOutputStream fo = new FileOutputStream(filePath);
			wb.write(fo);
			fo.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//used to detect the language of text
    public static String detect(String text) {
    	String lang = "";
    	if(text == null || text.trim().length() == 0){
    		return lang;
    	}        
		try {
			Detector detector = DetectorFactory.create();
	        detector.append(text);
	        lang = detector.detect();
		} catch (LangDetectException e) {
			
		}
		if(langStatistic.containsKey(lang)){
			langStatistic.put(lang, langStatistic.get(lang) + 1);
		} else {
			langStatistic.put(lang, 1);
		}
		return lang;
    }
    
    public static ArrayList<Language> detectLangs(String text) throws LangDetectException {
        Detector detector = DetectorFactory.create();
        detector.append(text);
        return detector.getProbabilities();
    }
}
