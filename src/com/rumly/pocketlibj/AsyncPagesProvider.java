package com.rumly.pocketlibj;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.graphics.Typeface;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.StyleSpan;
import android.util.Log;

class AsyncPagesProvider  extends FragmentPagerAdapter {
	private final int pageWidth;
    private final int pageHeight;
    private final float lineSpacingMultiplier;
    private final int lineSpacingExtra;
    
    private final List<CharSequence> pages = new ArrayList<CharSequence>();
    private int numPagesLoaded = 0;
    private boolean bAllPagesLoaded = false;
        
    private final List<Long> pageWords = new ArrayList<Long>();
    
    
    private final Activity mActivity;
    private final String allText;
    private final TextPaint textPaint;

    public AsyncPagesProvider(
    		FragmentManager fm,
    		Activity activity, String allText,
    		TextPaint textPaint,
    		int pageWidth, int pageHeight,  
    		float lineSpacingMultiplier, int lineSpacingExtra) {
    	super(fm);
    	this.mActivity = activity;
    	this.allText = allText;
    	this.textPaint = textPaint;
        this.pageWidth = pageWidth;
        this.pageHeight = pageHeight;
        this.lineSpacingMultiplier = lineSpacingMultiplier;
        this.lineSpacingExtra = lineSpacingExtra;
    }
    
    // NOTE: maye Result should be List<CharSequence> ?
//    private class PageSplitterTask extends AsyncTask<String, Long, Integer> {
    Runnable pageSplitterTask = new Runnable() {
    	
    	private SpannableStringBuilder currentLine = new SpannableStringBuilder();
    	private SpannableStringBuilder currentPage = new SpannableStringBuilder();
    	private int currentLineHeight;
    	private int pageContentHeight;
    	private int currentLineWidth;
    	private int textLineHeight;
    	private Long totalWords = 0L;
		
		@Override
		public void run() {

			textLineHeight = (int) Math.ceil(textPaint.getFontMetrics(null) * lineSpacingMultiplier + lineSpacingExtra);
			String[] paragraphs = allText.split("\n", -1);
			
	        int i;
	        for (i = 0; i < paragraphs.length - 1; i++) {
	            appendText(paragraphs[i]);
	            appendNewLine();
	        }
	        appendText(paragraphs[i]);
	        pageWords.add(totalWords);
	        
	        SpannableStringBuilder lastPage = new SpannableStringBuilder(currentPage);
	        if (pageContentHeight + currentLineHeight > pageHeight) {
	            pages.add(lastPage);
	            lastPage = new SpannableStringBuilder();
	        }
	        lastPage.append(currentLine);
	        pages.add(lastPage);
			
		}
		
		private void updateUI() {
			mActivity.runOnUiThread(new Runnable() { 
                public void run() {
                    notifyDataSetChanged();
                }
            }); 
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
	            pages.add(currentPage);
	            numPagesLoaded++;
	            currentPage = new SpannableStringBuilder();
	            pageContentHeight = 0;
	            pageWords.add(totalWords);
//	            notifyDataSetChanged();
	            updateUI();
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

	        currentLine = new SpannableStringBuilder();
	        currentLineHeight = textLineHeight;
	        currentLineWidth = 0;
	    }

	    private void appendTextToLine(String appendedText, int textWidth) {
	        currentLineHeight = Math.max(currentLineHeight, textLineHeight);
	        currentLine.append(renderToSpannable(appendedText, textPaint));
	        currentLineWidth += textWidth;
	    }
	    
	    private SpannableString renderToSpannable(String text, TextPaint textPaint) {
	        SpannableString spannable = new SpannableString(text);

	        if (textPaint.isFakeBoldText()) {
	            spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, spannable.length(), 0);
	        }
	        return spannable;
	    }	
		
    }; // runnable
    
    private CharSequence getPage(int n) {
    	boolean bKeepGoing = true;
    	CharSequence p = "";
    	while (bKeepGoing) {
    		try {
    			p = pages.get(n);
    			bKeepGoing = false;
    		} catch (IndexOutOfBoundsException e) {
    			try {
    				Thread.sleep(100);
    			} catch (InterruptedException er) {
    				bKeepGoing = false;
    			}
    		}
    	}
    	return p;
    }
    
    @Override
    public Fragment getItem(int i) {
    	Log.d("PocketLibJ", "TextPagerAdapter.getItem(" + i + ")");
        return PageFragment.newInstance(getPage(i));
    }
    
    @Override
    public int getCount() {
    	return numPagesLoaded;
    }
    
    public void genPages() {
    	new Thread(pageSplitterTask).start();
    }
    
    // TODO: test this
    public boolean savePages(String fileName) {
    	ObjectOutputStream out = null;
    	try {
    		out = new ObjectOutputStream(new FileOutputStream(fileName));
    		out.writeObject(this.pages);    		
    	} catch (IOException e) {
    		Log.e("PocketLibJ", e.getMessage());
    		return false;
    	} finally {
    		if (out != null)
    			try { out.close(); }
    			catch (IOException e) {}
    	}
    	return true;
    }
    
    // TODO: test this
    public boolean loadPages(String fileName) {
    	ObjectInputStream in = null;
    	try {
    		in = new ObjectInputStream(new FileInputStream(fileName));
    		List<CharSequence>nPages = (ArrayList<CharSequence>) in.readObject();
    		pages.clear();
    		pages.addAll(nPages);
    	} catch (IOException e) {
    		Log.e("PocketLibJ", e.getMessage());
    		return false;
    	} catch (ClassNotFoundException e) {
    		Log.e("PocketLibJ", e.getMessage());
    		return false;
    	} finally {
    		if (in != null)
    			try { in.close(); }
    			catch (IOException e) {}
    	}
    	return true;
    }
}
