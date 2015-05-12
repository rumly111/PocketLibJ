package com.rumly.pocketlibj;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.OperationCanceledException;
import android.text.TextPaint;
import android.util.Log;

public class PagesLoader extends AsyncTaskLoader<List<CharSequence>> {
	private String allText = "";
	private int startParagraph = 0;
	private TextPaint textPaint;
	private List<CharSequence> mPages = new LinkedList<CharSequence>();
	private Handler mHandler;
	// TODO: make these 4 fields final
	private int pageWidth = 100;
    private int pageHeight = 100;
    private float lineSpacingMultiplier = 1.0f;
    private int lineSpacingExtra = 0;
	private StringBuilder currentLine = new StringBuilder(256);
	private StringBuilder currentPage = new StringBuilder(1024);
	private int currentLineHeight;
	private int pageContentHeight;
	private int currentLineWidth;
	private int textLineHeight;
	private Long totalWords = 0L;
	private int numPagesLoaded = 0;
	
	private boolean bKeepLoading = true;
	
	private final static String TAG = "PocketLibJ";
	private final static String pageNotLoaded = "Page not loaded";
	
	public PagesLoader(Context context) {
		super(context);
	}
	
	public PagesLoader(Context context, int pageWidth, int pageHeight, 
			float lineSpacingMultiplier, int lineSpacingExtra,
			TextPaint textPaint, String allText, int startParagraph) {
		super(context);
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.lineSpacingMultiplier = lineSpacingMultiplier;
        this.lineSpacingExtra = lineSpacingExtra;
		this.textPaint = textPaint;
		this.allText = allText;
		this.startParagraph = startParagraph;
	}
	
	public void setHandler(Handler handler) {
		this.mHandler = handler;
	}
	
	public List<CharSequence> getPages() {
		return mPages;
	}
	
	private void updatePagesCount(int n) {
		if (mHandler != null) {
			Bundle data = new Bundle();
			data.putInt("num_pages", n);
			Message msg = new Message();
			msg.setData(data);
			msg.what = 0;
			
			mHandler.sendMessage(msg);
		} else {
			Log.e(TAG, "mHandler is null");
		}
	}

	@Override
	public List<CharSequence> loadInBackground() {
		Log.d(TAG, "PagesLoader.loadInBackground()");
		textLineHeight = (int) Math.ceil(textPaint.getFontMetrics(null) * lineSpacingMultiplier + lineSpacingExtra);
		String[] paragraphs = allText.split("\n", -1);

        try {
        	int i;
        	for (i = 0; i < paragraphs.length - 1 && bKeepLoading; i++) {
        		if (isLoadInBackgroundCanceled()) {
        			Log.i(TAG, "Loading pages was cancelled. Num of loaded pages: " + numPagesLoaded);
        			return mPages;
        		}
        		appendText(paragraphs[i]);
        		appendNewLine();
        	}
        	
        	if (!bKeepLoading) {
        		return mPages;
        	}

        	appendText(paragraphs[i]);
        	
        	// process last page ?
        	StringBuilder lastPage = new StringBuilder(currentPage);
        	if (pageContentHeight + currentLineHeight > pageHeight) {
        		mPages.add(lastPage);
        		lastPage = new StringBuilder();
        	}
        	lastPage.append(currentLine);        	
        	mPages.add(lastPage);
        } catch (OperationCanceledException e) {
        	if (!isLoadInBackgroundCanceled()) {
        		throw e;
        	} else {
        		Log.i(TAG, "Loading pages was cancelled. Num of loaded pages: " + numPagesLoaded);
        		return mPages;        		
        	}
        }
        
		return mPages;
	}
	
	@Override
	public void cancelLoadInBackground() {
		bKeepLoading = false;
    }
	
    private void appendText(String text) {
        String[] words = text.split(" ", -1);
        int i;
        for (i = 0; i < words.length - 1; i++) {
            appendWord(words[i] + " ");
        }
        appendWord(words[i]);
    }
    
    private void appendNewLine() {
        currentLine.append("\n");
        checkForPageEnd();
        appendLineToPage(textLineHeight);
    }
    
    private void checkForPageEnd() {
        if (pageContentHeight + currentLineHeight > pageHeight) {
            mPages.add(currentPage);
            numPagesLoaded++;
            currentPage = new StringBuilder(1024);
            pageContentHeight = 0;
            updatePagesCount(numPagesLoaded);
        }
    }
    
    private void appendWord(String appendedText) {
        int textWidth = (int) Math.ceil(textPaint.measureText(appendedText));
        if (currentLineWidth + textWidth >= pageWidth) {
            checkForPageEnd();
            appendLineToPage(textLineHeight);
        }
        appendTextToLine(appendedText, textWidth);
        totalWords += 1;
    }

    private void appendLineToPage(int textLineHeight) {
        currentPage.append(currentLine);
        pageContentHeight += currentLineHeight;

        currentLine = new StringBuilder(256);
        currentLineHeight = textLineHeight;
        currentLineWidth = 0;
    }

    private void appendTextToLine(String appendedText, int textWidth) {
        currentLineHeight = Math.max(currentLineHeight, textLineHeight);
        currentLine.append(appendedText);
        currentLineWidth += textWidth;
    }
    
 // TODO: test this
    public boolean savePages(FileOutputStream os) {
    	ObjectOutputStream out = null;
    	try {
    		out = new ObjectOutputStream(os);
    		out.writeObject(this.mPages);    		
    	} catch (IOException e) {
    		Log.e(TAG, e.getMessage());
    		return false;
    	} finally {
    		if (out != null)
    			try { out.close(); }
    			catch (IOException e) {}
    	}
    	return true;
    }
    
    // TODO: test this
    public boolean loadPages(FileInputStream is) {
    	ObjectInputStream in = null;
    	try {
    		in = new ObjectInputStream(is);
    		@SuppressWarnings("unchecked")
			List<CharSequence>nPages = (LinkedList<CharSequence>) in.readObject();
    		mPages.clear();
    		mPages.addAll(nPages);
    	} catch (IOException e) {
    		Log.e(TAG, e.getMessage());
    		return false;
    	} catch (ClassNotFoundException e) {
    		Log.e(TAG, e.getMessage());
    		return false;
    	} finally {
    		if (in != null)
    			try { in.close(); }
    			catch (IOException e) {}
    	}
    	return true;
    }
}
