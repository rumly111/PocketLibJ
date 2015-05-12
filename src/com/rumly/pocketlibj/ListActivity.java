package com.rumly.pocketlibj;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ListActivity extends Activity
{
	private ListView listView;
	private static SimpleCursorAdapter adapter;
	private static SQLiteDatabase db;
	private SharedPreferences sharedPref;
	
	final static String baseSQL = "select _id,AUTHOR,NAME,SUBJ,STARRED,DOWNLOADED,STARTED,FINISHED from books";

	public ProgressDialog pDialog;

	private Map<String,String> sqlConstraints;
	private LibraryManager libraryManager;
	private Context mContext;
	
	private final static String TAG = "PocketLibJ";

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_screen);
		db = new BookDB(this).getReadableDatabase();
		libraryManager = new LibraryManager(this);
		sqlConstraints = new HashMap<String, String>();
		sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		this.mContext = this;
		setupUI();
	}
	
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.book_menu, menu);
    }
	
	@Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId())
        {
        case R.id.mOpenWvId:
            Log.d(TAG, "Open in internal viewer");
            openInternal(info.id);
            return true;
        case R.id.mOpenExtId:
        	Log.d(TAG, "Open in external viewer");
        	openExternal(info.id);
        	return true;            
        case R.id.mToggleStarredId:
        	db.execSQL("update books set STARRED = 1 - STARRED where _id = ?", new Object[] { info.id });
        	Cursor c = db.rawQuery(buildSqlQuery(baseSQL, sqlConstraints), null);
        	adapter.swapCursor(c);
        	return true;
        case R.id.mDownloadId:
        	downloadBook(info.id);
        	return true;
        default:
            return super.onContextItemSelected(item);
        }
    }

	private void setupUI()
	{
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Downloading file. Please wait...");
        pDialog.setIndeterminate(false);
        pDialog.setMax(100);
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pDialog.setCancelable(false);
		
		EditText search = (EditText) findViewById(R.id.edSearchId);
		listView = (ListView) findViewById(R.id.listId);
		listView.setDividerHeight(3);
		listView.setTextFilterEnabled(true);
		search.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) { }

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

			@Override
			public void afterTextChanged(Editable s) {
				adapter.getFilter().filter(s.toString());
			}
		});

		Intent i = getIntent();
		String list = i.getStringExtra("list");
		if (list.equals("books"))
		{
			buildBooksList(i);
		}
		else if (list.equals("authors"))
		{
			buildAuthorsList();
		}
		else if (list.equals("genres"))
		{
			buildGenresList();
		}
		else
		{
			Log.e("LibraryActivity", "Unknown list type " + list);
		}
	}
	
	private String buildSqlQuery(String base, Map<String,String> constraints) {
		if (constraints.isEmpty()) {
			return base;
		}
		StringBuilder sql = new StringBuilder();
		sql.append(base);
		sql.append(" where ");
		boolean first = true;
		for (String c : constraints.values()) {
			if (first)
				first = false;
			else
				sql.append(" and ");
			sql.append(c);
		}
		return sql.toString();
	}
	
	private void buildBooksList(Intent i) {
		Log.d(TAG, "Building books list");
		String winTitle = i.getStringExtra("title");
		if (winTitle != null) {
			setTitle(winTitle);
		} else {
			setTitle(getString(R.string.books));
		}

//		final String sql = "select _id,AUTHOR,NAME,SUBJ,STARRED from books";
		final String sql = baseSQL;
		String sql_constraint = i.getStringExtra("constraint");
		if (sql_constraint != null)
		{
			sqlConstraints.put("base", sql_constraint);
		}
		Cursor cursor = db.rawQuery(buildSqlQuery(sql, sqlConstraints), null);

		adapter = new SimpleCursorAdapter(this, R.layout.row_book, cursor,
				new String[] {"NAME", "AUTHOR", "SUBJ", "STARRED", "DOWNLOADED"},
				new int[] {R.id.tvBookId, R.id.tvAuthorId, R.id.tvGenreId, R.id.imgStarId, R.id.imgDLId},
				0);

		adapter.setFilterQueryProvider(new FilterQueryProvider() {
			@Override
			public Cursor runQuery(CharSequence constraint) {
				if (constraint.length() > 0) {
					String new_constraint = "NAME_L like '%" + constraint.toString().toLowerCase(Locale.getDefault()) + "%'";
					sqlConstraints.put("search", new_constraint);
				} else {
					sqlConstraints.put("search", "1");
				}
				return db.rawQuery(buildSqlQuery(sql, sqlConstraints), null);
			}

		});
		
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (columnIndex == 1) {
					adapter.setViewText((TextView) view, 
							cursor.getString(columnIndex).isEmpty() ? getString(R.string.unknown_author) : cursor.getString(columnIndex));
					return true;
				}
				if (columnIndex == 4) {
					if (cursor.getInt(columnIndex) == 1) {
						((ImageView) view).setImageResource(R.drawable.star);							
					} else {
						((ImageView) view).setImageResource(android.R.color.transparent);
					}
					return true;
				}
				if (columnIndex == 5) {
					if (cursor.getInt(columnIndex) == 1) {
						((ImageView) view).setImageResource(R.drawable.dl);							
					} else {
						((ImageView) view).setImageResource(android.R.color.transparent);
					}
					return true;
				}
				return false;
			}
		});
		
		listView.setAdapter(adapter);
		
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				openBook(id);
			}
		});
		
		registerForContextMenu(listView);
	}
	
	private void buildAuthorsList() {
		Log.d(TAG, "Building authors list");
		setTitle(getString(R.string.authors));

		db = new BookDB(this).getReadableDatabase();
		String sql = "select _id,AUTHOR,COUNT() from books GROUP BY AUTHOR";
		Cursor cursor = db.rawQuery(sql, null);

		adapter = new SimpleCursorAdapter(this,
				R.layout.row_author, cursor,
				new String[] {"AUTHOR", "COUNT()"},
				new int[] {R.id.tvAuthNameId, R.id.tvAuthNBooksId},
				0);

		adapter.setFilterQueryProvider(new FilterQueryProvider() {

			@Override
			public Cursor runQuery(CharSequence constraint) {
				String sql = "select _id,AUTHOR,COUNT() from books";
				if (constraint.length() > 0) {
					sql = sql + " where AUTHOR_L like '%" + constraint.toString().toLowerCase(Locale.getDefault()) + "%'";
				}
				sql += " GROUP BY AUTHOR";
				return db.rawQuery(sql, null);
			}

		});
		
		adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			
			@Override
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				if (columnIndex == 1) {
					adapter.setViewText((TextView) view, 
							cursor.getString(columnIndex).isEmpty() ? getString(R.string.unknown_author) : cursor.getString(columnIndex));
					return true;
				}
				return false;
			}
		});

		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				String author = ((TextView) view.findViewById(R.id.tvAuthNameId)).getText().toString();
				Intent switchTo = new Intent(parent.getContext(), ListActivity.class);
				switchTo.putExtra("list", "books");
				switchTo.putExtra("constraint", String.format("AUTHOR = '%s'", author));
				switchTo.putExtra("title", "Books of " + author);
				startActivity(switchTo);
			}
		});
	}
	
	private void buildGenresList() {
		Log.d(TAG, "Building genres list");
		setTitle(getString(R.string.genres));

		db = new BookDB(this).getReadableDatabase();
		String sql = "select _id,SUBJ,COUNT() from books GROUP BY SUBJ";
		Cursor cursor = db.rawQuery(sql, null);

		adapter = new SimpleCursorAdapter(this,
				R.layout.row_genre, cursor,
				new String[] {"SUBJ", "COUNT()"},
				new int[] {R.id.tvGenreNameId, R.id.tvGenreNBooksId},
				0);

		adapter.setFilterQueryProvider(new FilterQueryProvider() {

			@Override
			public Cursor runQuery(CharSequence constraint) {
				String sql = "select _id,SUBJ,COUNT() from books";
				if (constraint.length() > 0) {
					sql = sql + " where SUBJ like '%" + constraint.toString() + "%'";
				}
				sql += " GROUP BY SUBJ";
				return db.rawQuery(sql, null);
			}
		});

		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String genre = ((TextView) view.findViewById(R.id.tvGenreNameId)).getText().toString();
				Intent switchTo = new Intent(parent.getContext(), ListActivity.class);
				switchTo.putExtra("list", "books");
				switchTo.putExtra("constraint", String.format("SUBJ = '%s'", genre));
				switchTo.putExtra("title", "Books by genre " + genre);
				startActivity(switchTo);
			}
		});
	}
	
	private void openBook(long id) {
		String viewer = sharedPref.getString("pref_viewer", "external");
		if (viewer.equals("internal")) {
			openInternal(id);
		} else if (viewer.equals("external")) {
			openExternal(id);
		} else {
			Log.e(TAG, "Unknown viewer type:" + viewer);
		}
	}
	
	private void openInternal(long id) {
		LibraryManager.BookInfo bookInfo = libraryManager.getBookInfo(id);
		if (bookInfo == null || !libraryManager.bookExists(id)) {
			Toast.makeText(this, R.string.book_not_found, Toast.LENGTH_SHORT).show();
			return;
		}
		SharedPreferences.Editor editor = sharedPref.edit();
		editor.putLong("reading", id);
		editor.commit();
		db.execSQL("update books set STARTED = 1 where _id = ?", new Object[] { id });
		Intent switchTo = new Intent(this, ReadingActivity.class);
		switchTo.putExtra("id", id);
		switchTo.putExtra("fileName", bookInfo.fileName);
		switchTo.putExtra("fileTag", bookInfo.fileTag);
		switchTo.putExtra("title", bookInfo.title);
		switchTo.putExtra("author", bookInfo.author);
		switchTo.putExtra("genre", bookInfo.genre);
		switchTo.putExtra("position", bookInfo.position);
		startActivityForResult(switchTo, 1);
	}
	
	private void openExternal(long id) {
		LibraryManager.BookInfo bookInfo = libraryManager.getBookInfo(id);
		if (bookInfo == null || !libraryManager.bookExists(id)) {
			Toast.makeText(this, R.string.book_not_found, Toast.LENGTH_SHORT).show();
			return;
		}
		db.execSQL("update books set STARTED = 1 where _id = ?", new Object[] { id });
		Intent intent = new Intent();
		File fileToOpen = new File(bookInfo.fileName);
		intent.setAction(Intent.ACTION_VIEW);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.setDataAndType(Uri.fromFile(fileToOpen), "application/zip");
		try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.ext_app_not_found, Toast.LENGTH_SHORT).show();
        }
	}
		
	private class DownloadFileFromURL extends AsyncTask<String, Integer, Boolean>{
		
		private String errorMsg = "";
		private Integer id;
    	
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog.setProgress(0);
            pDialog.show();
        }
        
        @Override
        protected Boolean doInBackground(String... args) {
            final URL url;
            final URLConnection conection;
            id = Integer.valueOf(args[2]);
            InputStream input = null;
            OutputStream output = null;
            try {
            	url = new URL(args[0]);
            	conection = url.openConnection();
                conection.connect();

                input = new BufferedInputStream(url.openStream(), 8192);
                output = new FileOutputStream(args[1]);

                int count;
                int lenghtOfFile = conection.getContentLength();
                byte data[] = new byte[1024];
                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress((int) (total * 100 / lenghtOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
            } catch (IOException e) {
                Log.e("Error: ", e.getMessage());
                errorMsg = e.getLocalizedMessage();
                return Boolean.valueOf(false);
	        } finally {
	        	if (input != null) try { input.close(); } catch (IOException e) {}
	        	if (output != null) try { output.close(); } catch (IOException e) {}
	        }
            return Boolean.valueOf(true);
        }

        @Override
		protected void onProgressUpdate(Integer... values) {
        	pDialog.setProgress(values[0]);
		}

        @Override
        protected void onPostExecute(Boolean result) {
        	pDialog.dismiss();
            if (result) {
            	db.execSQL("update books set DOWNLOADED = 1 where _id = ?", new Object[] { id });
            	Cursor c = db.rawQuery(buildSqlQuery(baseSQL, sqlConstraints), null);
            	adapter.swapCursor(c);
            } else {
            	Toast.makeText(mContext, errorMsg, Toast.LENGTH_SHORT).show();
            }
        }
    }
	
	private void downloadBook(long id) {
		LibraryManager.BookInfo info = libraryManager.getBookInfo(id);
    	if (info == null) {
    		Toast.makeText(this, R.string.book_not_found, Toast.LENGTH_SHORT).show();
    		return;
    	} else {
    		String url = libraryManager.getRemoteUrl(info);
        	File directory = new File(libraryManager.getFileDir(info.genre));
        	directory.mkdirs();
        	new DownloadFileFromURL().execute(url, info.fileName, "" + info.id);
    	}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				int position = data.getIntExtra("position", 1);
				long id = data.getLongExtra("id", 1);
				boolean finished = data.getBooleanExtra("finished", false);
				Toast.makeText(mContext, String.format("Position: %d", position), Toast.LENGTH_SHORT).show();
				Log.v(TAG, String.format("Received position: %d", position));
				db.execSQL("update books set POSITION = ? where _id = ?", new Object[] { position, id });
				if (finished) {
					db.execSQL("update books set STARTED = 0, FINISHED = 1 where _id = ?", 
							new Object[] { id });
				}
			} else if (resultCode == RESULT_CANCELED) {
				String errMessage = data.getStringExtra("message");
				Toast.makeText(mContext, errMessage, Toast.LENGTH_SHORT).show();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);			
		}
	}
	
	
}
