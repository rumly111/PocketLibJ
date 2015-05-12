package com.rumly.pocketlibj;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;

public class LibraryManager {
	private static SQLiteDatabase db;
	private Cursor c;
	private Activity mActivity;
	
	private final static String LIBRARY_ROOT = "/mnt/sdcard/books/";
	private final static String LIBRARY_SERVER = "http://10.0.2.2:8080/";
    private final static Map<String,String> genresMap;
    static {
    	genresMap = new HashMap<String, String>();
    	genresMap.put("анекдоты", "anecdots");
    	genresMap.put("боевик", "action");
    	genresMap.put("гадание", "talers");
    	genresMap.put("детектив", "detect");
    	genresMap.put("детская", "children");
    	genresMap.put("документ", "document");
    	genresMap.put("дом", "home");
    	genresMap.put("драма", "drama");
    	genresMap.put("жен. роман", "lover");
    	genresMap.put("журнал", "journal");
    	genresMap.put("закон. акт", "acts");
    	genresMap.put("история", "history");
    	genresMap.put("классика", "classics");
    	genresMap.put("криминал", "criminal");
    	genresMap.put("лирика", "lyric");
    	genresMap.put("медицина", "medic");
    	genresMap.put("мемуары", "memor");
    	genresMap.put("н-фантаст.", "sfiction");
    	genresMap.put("наука", "science");
    	genresMap.put("песни", "songs");
    	genresMap.put("политика", "politics");
    	genresMap.put("приключен", "adventur");
    	genresMap.put("психология", "psyhol");
    	genresMap.put("религия", "religion");
    	genresMap.put("секс-учеба", "sex-tech");
    	genresMap.put("сказка", "tales");
    	genresMap.put("словарь", "diction");
    	genresMap.put("спорт", "sport");
    	genresMap.put("стихи", "poems");
    	genresMap.put("триллер", "thriller");
    	genresMap.put("учеба", "teacher");
    	genresMap.put("философия", "phylos");
    	genresMap.put("фэнтази", "fantasy");
    	genresMap.put("эзотерика", "exoteric");
    	genresMap.put("экономика", "economy");
    	genresMap.put("энциклоп", "encicl");
    	genresMap.put("эротика", "erotic");
    	genresMap.put("юмор", "humor");
    	genresMap.put("юмор прог.", "humor_pr");    	
    }
    private SharedPreferences sharedPref;
    
    public Map<String,String> getGenresMap() {
    	return genresMap;
    }

    public LibraryManager(Activity activity) {
    	this.mActivity = activity;
    	sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
    	db = new BookDB(activity).getReadableDatabase();
    }
    
    public boolean bookExists(BookInfo info) {
    	return (new File(info.fileName)).exists();
    }
    
    public boolean bookExists(long id) {
    	BookInfo info = getBookInfo(id);
    	if (info == null) {
    		return false;
    	} else {
    		return bookExists(info);
    	}
    }
    
    public String getFileName(String fileTag, String bookGenre) {
    	return sharedPref.getString("library_root", LIBRARY_ROOT) 
    			+ genresMap.get(bookGenre) + "/" + fileTag + ".zip";
    }
    
    public String getFileDir(String bookGenre) {
    	return sharedPref.getString("library_root", LIBRARY_ROOT) 
    			+ genresMap.get(bookGenre);
    }
    
    public String getRemoteUrl(BookInfo info) {
    	String libraryServer = sharedPref.getString("pref_server", LIBRARY_SERVER);
    	return libraryServer + genresMap.get(info.genre) + "/" + info.fileTag + ".zip";
    }
    
    public String getRemoteUrl(long id) {
    	BookInfo info = getBookInfo(id);
    	if (info == null) {
    		return null;
    	} else {
    		return getRemoteUrl(info);    		
    	}
    }
    
    public BookInfo getBookInfo(long id) {
    	String sql = "select AUTHOR,NAME,SUBJ,FILENAME,STARRED,DOWNLOADED,POSITION from books where _id = ?";
    	c = db.rawQuery(sql, new String[] { String.valueOf(id) });
    	if (c.moveToFirst()) {
    		String author = c.getString(0);
    		String title = c.getString(1);
    		String genre = c.getString(2);
    		String fileTag = c.getString(3);
    		String fileName = getFileName(fileTag, genre);
    		boolean starred = c.getInt(4) == 1;
    		boolean downloaded = c.getInt(5) == 1;
    		int position = c.getInt(6);
    		return new BookInfo(id, author, title, genre, fileTag, fileName, starred, downloaded, position);
    	} else {
    		return null;
    	}
    }
    
    public class BookInfo {
    	public final long id;
    	public final String author;
    	public final String title;
    	public final String genre;
    	public final String fileTag;
    	public final String fileName;
    	public final boolean starred;
    	public final boolean downloaded;
    	public final int position;
    	
    	public BookInfo(long id, String author, String title, String genre,
    			String fileTag, String fileName, boolean starred, 
    			boolean downloaded, int position) {
    		this.id = id;
    		this.author = author;
    		this.title = title;
    		this.genre = genre;
    		this.fileTag = fileTag;
    		this.fileName = fileName;
    		this.starred = starred;
    		this.downloaded = downloaded;
    		this.position = position;
    	}
    }
    
    
}
