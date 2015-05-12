package com.rumly.pocketlibj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class BookDB extends SQLiteOpenHelper
{
    private static final String DB_NAME = "library.db";
    private static final String SQL_FILE_NAME_ZIP = "books.sql.zip";
    private static final String SQL_TABLE_CREATE = "CREATE TABLE books("
    		+ "_id integer primary key autoincrement, "
    		+ "AUTHOR text[30], AUTHOR_L text[30], "
    		+ "NAME text[100] NOT NULL, NAME_L text[100] NOT NULL, "
    		+ "SUBJ text[10] NOT NULL, LENGTH integer NOT NULL, " 
    		+ "FILENAME text[50] NOT NULL, POSITION integer DEFAULT 0, "
    		+ "STARTED integer DEFAULT 0, FINISHED integer DEFAULT 0, "
    		+ "STARRED integer DEFAULT 0, DOWNLOADED integer DEFAULT 0, "
    		+ "CHARSET text[10] DEFAULT 'IBM866')";
    private static final int DB_VERSION = 1;
    private Context mContext;
    
    public BookDB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.mContext = context;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
    	db.execSQL(SQL_TABLE_CREATE);
    	db.beginTransaction();
    	try {
    		ZipInputStream zipIs = new ZipInputStream(mContext.getAssets().open(SQL_FILE_NAME_ZIP));
    		zipIs.getNextEntry();
    		BufferedReader reader = new BufferedReader(new InputStreamReader(zipIs));
    		String sql = "";
    		while ((sql = reader.readLine()) != null) {
    			db.execSQL(sql);
    		}
    		db.setTransactionSuccessful();
    	} catch (IOException e) {
    		Log.e("PocketLibJ", "Error creating DB: " + e.toString());
    	} finally {
    		db.endTransaction();
    	}
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	      Log.w("PocketLibJ",
	          "Upgrading database from version " + oldVersion + " to "
	          + newVersion + ", which will destroy all old data");
	      db.execSQL("DROP TABLE IF EXISTS books");
	      onCreate(db);
    }
}
