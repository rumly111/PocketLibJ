package com.rumly.pocketlibj;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipInputStream;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ProgressBar;



public class ReadingActivity extends FragmentActivity
    implements ViewPager.OnPageChangeListener, 
    LoaderManager.LoaderCallbacks<List<CharSequence>> {
//	private static SQLiteDatabase db;
//	private LibraryManager libraryManager;
	private ViewPager pagesView;

	private String content = "content";
	private long id;
	private ProgressBar pbPosition;
	
	private long timeMilliSeconds;
	private long timeDiffMS;
	
	private TextPaint textPaint;
	private PagesLoader mLoader;
	private TextPagerAdapter pagesAdapter;
	
	ProgressDialog progressDialog;
	private boolean bLoadFinished = false;
	private int iWaitingForPage = -1;
	
	private String fileTag = "default";
	private boolean bUseCache = false;
	
	private final static String TAG = "PocketLibJ";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.read_screen);
		
		pagesView = (ViewPager) findViewById(R.id.pages);
		pbPosition = (ProgressBar) findViewById(R.id.pbBookPosId);
		
//		mLoader = new PagesLoader(this);

//		db = new BookDB(this).getReadableDatabase();
//		libraryManager = new LibraryManager(this);

		Intent i = getIntent();
		id = i.getLongExtra("id", 1);
		final int position = i.getIntExtra("position", 0);
		String zipFileName = i.getStringExtra("fileName");
		String bookName = i.getStringExtra("title");
		String bookAuthor = i.getStringExtra("author");
		if (bookAuthor == null || bookAuthor.length() == 0) {
			bookAuthor = "<unknown>";
		}
		String bookGenre = i.getStringExtra("genre");
		String bookCharset = "CP866";
		bUseCache = i.getBooleanExtra("useCache", false);
		fileTag = "book.save"; // i.getStringExtra("fileTag");

		if (!Charset.isSupported(bookCharset)) {
			Dialogs.showErrorDialog(this, "Error", String.format("Charset %s is not supported", bookCharset));
			return;
		}
		
		

		setTitle(String.format("'%s' by %s", bookName, bookAuthor));
		try {
			Log.i("PocketLibJ", "OPEN: " + zipFileName);
			ZipInputStream zipIs = new ZipInputStream(new FileInputStream(zipFileName));
			zipIs.getNextEntry();
			Scanner s = new Scanner(zipIs, bookCharset).useDelimiter("\\A");
			content = s.hasNext() ? s.next() : "";
			timeMilliSeconds = System.currentTimeMillis();
			content = TextPreprocess.process(content);
			timeDiffMS = System.currentTimeMillis() - timeMilliSeconds;
			Log.d("PocketLibJ", "TextProcess.process(content) time: " + timeDiffMS);

			zipIs.close();
			s.close();
		} catch (IOException e) {
			Dialogs.showErrorDialog(this, "Error", e.getMessage());
			Log.e(TAG, e.getMessage());
			endWithError(e.getMessage());
		} catch (OutOfMemoryError e) {
			Log.e(TAG, e.getMessage());
			Dialogs.showErrorDialog(this, "Error", e.getMessage());
			endWithError(e.getMessage());  // FIXME: cleanup 
		}
		
        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
//        getLoaderManager().initLoader(0, null, this);
		
		final ReadingActivity mActivity = this;
		

		// to get ViewPager width and height we have to wait global layout
        pagesView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                textPaint = new TextPaint();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mActivity);
                int textSize = sharedPref.getInt("pref_fontSize", 12);
//                textPaint.setTextSize(getResources().getDimension(R.dimen.text_size));
                DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
                float textSizeF = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, displayMetrics);
                textPaint.setTextSize(textSizeF);
                pbPosition.setMax(2);

                mLoader = new PagesLoader(mActivity, pagesView.getWidth(), pagesView.getHeight(), 1, 0,
                		textPaint, content, position);
                pagesAdapter = new TextPagerAdapter(getSupportFragmentManager(), mLoader.getPages());
                pagesAdapter.attachProgressBar(pbPosition);                
                mActivity.getLoaderManager().initLoader(0, null, mActivity);


                pagesView.setAdapter(pagesAdapter);
                if (position > 0) {
                	gotoPage(position);
//                	for (pageNum = 0; pageSplitter.getPageWords().get(pageNum) < position; pageNum++) { }
//                	Log.d("PocketLibJ", String.format("position = %d | page = %d", position, pageNum));
                }
                pagesView.setOnPageChangeListener(mActivity);
                pagesView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
        
	}
	
	private final Handler handler = new Handler() {

		public void handleMessage(Message msg) {
			if (msg.what == 0) {
				int numPages = msg.getData().getInt("num_pages");
				if (numPages % 20 == 0)
					Log.i(TAG, "Received page count:" + numPages);
				pagesAdapter.updatePageCount(numPages);
//				pbPosition.setMax(numPages - 1);
				if (iWaitingForPage != -1 && numPages >= iWaitingForPage) {
					pagesView.setCurrentItem(iWaitingForPage);
					iWaitingForPage = -1;
					progressDialog.dismiss();
				}
			}
		}
	};
	
	private void endWithError(String message) {
		Intent i = new Intent();
		setResult(RESULT_CANCELED, i);
		i.putExtra("message", message);
		finish();
	}
	
	@Override
	public void onBackPressed() {
		Intent i = new Intent();
		if (pagesView == null) {
			setResult(RESULT_CANCELED, i);
		} else {
			mLoader.cancelLoad();
//			mLoader.cancelLoadInBackground();
			i.putExtra("id", id);
			i.putExtra("position", pagesView.getCurrentItem());
//			if (pageSplitter.getPageWords().size() == pagesView.getCurrentItem()) {
//				i.putExtra("finished", false);
//			}
			setResult(RESULT_OK, i);
		}
		finish();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.reading_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.mGoToPageId:
        	AlertDialog.Builder ad = new AlertDialog.Builder(this);
        	ad.setTitle("Input");
        	ad.setMessage("Enter page number");

        	final EditText input = new EditText(this);
        	input.setInputType(InputType.TYPE_CLASS_NUMBER);
        	ad.setView(input);

        	ad.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dlg, int which) {
        			gotoPage(Integer.valueOf(input.getText().toString()));
        		}
        	});

        	ad.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dlg, int which) {
        			dlg.cancel();
        		}
        	});

        	ad.show();

            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private boolean gotoPage(int n) 
    {
    	if (n > pagesAdapter.getCount()) {
    		if (bLoadFinished) {
    			Log.e(TAG, "Cannot go to page " + n);
    			return false;
    		} else {
    			Log.v(TAG, "Go to page " + n);
    			progressDialog = ProgressDialog.show(ReadingActivity.this, "Loading", "Please wait", true, false); 
    			iWaitingForPage = n;    			
    		}
    	} else {
    		pagesView.setCurrentItem(n);
    		pbPosition.setProgress(n);
    	}
    	return true;
    }

	@Override
	public void onPageScrollStateChanged(int arg0) {

	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {

	}

	@Override
	public void onPageSelected(int page) {
//		pbPosition.setVisibility(ProgressBar.VISIBLE);
		pbPosition.setProgress(page);
	}
	
	public void addText(View v) {
		Log.d("PocketLibJ", "addText()");
	}

	@Override
	public Loader<List<CharSequence>> onCreateLoader(int id, Bundle args) {
		Log.d(TAG, "ReadingActivity.onCreateLoader()");
//		PagesLoader loader = new PagesLoader(this);
		mLoader.setHandler(handler);
		if (bUseCache) {
			FileInputStream cachedBook = null;
			try {
				cachedBook = openFileInput(fileTag);
				if (!mLoader.loadPages(cachedBook)) {
					Log.e(TAG, "Failed to load cached book.");
					mLoader.forceLoad();
				} else {
					Log.i(TAG, "Successfully loaded from file " + fileTag);
					pagesAdapter.updatePageCount(mLoader.getPages().size());
//					pbPosition.setMax(mLoader.getPages().size());
				}
			} catch (IOException e) {
				Log.v(TAG, "Cached book open failed. Forcing load from zip");
				mLoader.forceLoad();
			} finally {
				if (cachedBook != null) try { cachedBook.close(); }
				catch (IOException e) {}
			}			
		} else {
			if (mLoader.getPages().size() == 0) {
				mLoader.forceLoad();
			}
		}
		return mLoader;
	}

	@Override
	public void onLoadFinished(Loader<List<CharSequence>> loader,
			List<CharSequence> data) {
		Log.d(TAG, "ReadingActivity.onLoadFinished()");
		pagesAdapter.updatePageCount(data.size());
		pbPosition.setMax(data.size() - 1);
		bLoadFinished = true;
		if (bUseCache) {
			FileOutputStream cachedBook = null;
			try {
				cachedBook = openFileOutput(fileTag, Context.MODE_PRIVATE);
				if (!mLoader.savePages(cachedBook)) {
					Log.e(TAG, "Saving book to file " + fileTag + " failed");
				} else {
					Log.i(TAG, "Successfully cached to file " + fileTag);
				}
			} catch (IOException e) {
				Log.e(TAG, "Saving book to file " + fileTag + " failed: " + e.getMessage());
			} finally {
				if (cachedBook != null) try { cachedBook.close(); }
				catch (IOException e) {}
			}			
		}
	}

	@Override
	public void onLoaderReset(Loader<List<CharSequence>> loader) {
		Log.d(TAG, "ReadingActivity.onLoaderReset()");
	}
	
}
