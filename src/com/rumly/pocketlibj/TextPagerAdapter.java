package com.rumly.pocketlibj;

import java.util.List;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.bluejamesbond.text.DocumentView;
import com.bluejamesbond.text.IDocumentLayout;

class PageFragment extends Fragment {
    private final static String PAGE_TEXT = "PAGE_TEXT";
    private SharedPreferences sharedPref;

    public static PageFragment newInstance(CharSequence pageText) {
        PageFragment frag = new PageFragment();
        Bundle args = new Bundle();
        args.putCharSequence(PAGE_TEXT, pageText);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CharSequence text = getArguments().getCharSequence(PAGE_TEXT);
        DocumentView docView = (DocumentView) inflater.inflate(R.layout.page, container, false);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int textSize = sharedPref.getInt("pref_fontSize", 12);
        IDocumentLayout layout = docView.getLayout();
        IDocumentLayout.LayoutParams layoutParams = layout.getLayoutParams();
        layoutParams.setTextSize(textSize);

        docView.setText(text);
        return docView;
    }
}

public class TextPagerAdapter extends FragmentPagerAdapter {
    private final List<CharSequence> pageTexts;
    private int pageCount = 0;
    private ProgressBar pb = null;

    public TextPagerAdapter(FragmentManager fm, List<CharSequence> pageTexts) {
        super(fm);
        this.pageTexts = pageTexts;
        this.pageCount = pageTexts.size();
    }
    
    public void attachProgressBar(ProgressBar pb) {
    	this.pb = pb;
    }

    @Override
    public Fragment getItem(int i) {
    	Log.d("PocketLibJ", "TextPagerAdapter.getItem(" + i + ")");
        return PageFragment.newInstance(pageTexts.get(i));
    }

    @Override
    public int getCount() {
//        return pageTexts.size();
    	return pageCount;
    }
    
    public void updatePageCount(int n) {
    	this.pageCount = n;
    	if (pb != null)
    		pb.setMax(n - 1);
    	notifyDataSetChanged();
    }
}