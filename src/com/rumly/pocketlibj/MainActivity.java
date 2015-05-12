package com.rumly.pocketlibj;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity
{
	private final static String TAG = "PocketLibJ";
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_screen);
        setTitle(R.string.start_screen);
    }

    public void onClick(View view)
    {
        Intent switchTo = new Intent(this, ListActivity.class);
        switch (view.getId())
        {
        case R.id.btnBooksId:
            switchTo.putExtra("list", "books");
            break;
        case R.id.btnAuthorsId:
            switchTo.putExtra("list", "authors");
            break;
        case R.id.btnGenresId:
            switchTo.putExtra("list", "genres");
            break;
        case R.id.btnStarredId:
            switchTo.putExtra("list", "books");
            switchTo.putExtra("constraint", "STARRED = 1");
            switchTo.putExtra("title", getResources().getString(R.string.starred_books));
            break;
        }
        startActivity(switchTo);
    }
    
    public void showSettings(View view) {
    	Intent settings = new Intent(this, SettingsActivity.class);
    	if (view.getId() == R.id.btnSettingsId) {
    		startActivity(settings);
    	}
    }
    
    public void openReading(View view) {
    	SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    	LibraryManager lm = new LibraryManager(this);
    	long id = sharedPref.getLong("reading", -1);
    	if (id > 0) {
    		LibraryManager.BookInfo info = lm.getBookInfo(id);
    		if (info == null || !lm.bookExists(info)) {
    			Toast.makeText(this, R.string.book_not_found, Toast.LENGTH_SHORT).show();
    			return;
    		}
    		Intent switchTo = new Intent(this, ReadingActivity.class);
    		switchTo.putExtra("id", id);
    		switchTo.putExtra("fileName", info.fileName);
    		switchTo.putExtra("fileTag", info.fileTag);
    		switchTo.putExtra("title", info.title);
    		switchTo.putExtra("author", info.author);
    		switchTo.putExtra("genre", info.genre);
    		switchTo.putExtra("position", info.position);
    		switchTo.putExtra("useCache", false);
    		startActivityForResult(switchTo, 1);
    	}
    }
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == RESULT_OK) {
				int position = data.getIntExtra("position", 0);
				long id = data.getLongExtra("id", -1);
				boolean finished = data.getBooleanExtra("finished", false);
				Toast.makeText(this, String.format("Position: %d", position), Toast.LENGTH_SHORT).show();
				Log.v(TAG, String.format("Received position: %d", position));
				SQLiteDatabase db = new BookDB(this).getReadableDatabase();
				db.execSQL("update books set POSITION = ? where _id = ?", new Object[] { position, id });
				if (finished) {
					db.execSQL("update books set STARTED = 0, FINISHED = 1 where _id = ?", 
							new Object[] { id });
				}
				db.close();
			} else if (resultCode == RESULT_CANCELED) {
				String errMessage = data.getStringExtra("message");
				Toast.makeText(this, errMessage, Toast.LENGTH_SHORT).show();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);			
		}
	}
}
