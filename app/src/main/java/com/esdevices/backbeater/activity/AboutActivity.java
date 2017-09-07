package com.esdevices.backbeater.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.esdevices.backbeater.R;
import com.esdevices.backbeater.utils.Constants;

/**
 * Created by Alina Kholcheva on 2017-06-26.
 */

public class AboutActivity extends Activity {
    
    @Bind(R.id.webView) WebView webView;
    
    @Bind(R.id.progressIndicator) ProgressBar progressIndicator;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
    
        progressIndicator.setVisibility(View.VISIBLE);
    
        
        webView.setBackgroundColor(getResources().getColor(R.color.main_color));
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressIndicator.setVisibility(View.GONE);
            }
        });
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(Constants.HELP_URL);
    }
    
    @OnClick(R.id.backButton)
    public void onBackButtonClick(View v) {
        onBackPressed();
    }
    
}

