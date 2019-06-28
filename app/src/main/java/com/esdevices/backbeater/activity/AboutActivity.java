package com.esdevices.backbeater.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

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

    @Bind(R.id.toolbar) Toolbar toolbar;
    @Bind(R.id.goBackButton) TextView goBackButton;
    @Bind(R.id.goForwardButton) TextView goForwardButton;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
    
        progressIndicator.setVisibility(View.VISIBLE);
        toolbar.setVisibility(View.INVISIBLE);

        goBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoBack()) {
                    webView.goBack();
                }
            }
        });

        goForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (webView.canGoForward()) {
                    webView.goForward();
                }
            }
        });
        
        webView.setBackgroundColor(getResources().getColor(R.color.main_color));
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressIndicator.setVisibility(View.GONE);
                toolbar.setVisibility(View.VISIBLE);
                if (webView.canGoBack())
                    goBackButton.setTextColor(0xFFFFFFFF);
                else
                    goBackButton.setTextColor(0xFF808080);

                if (webView.canGoForward())
                    goForwardButton.setTextColor(0xFFFFFFFF);
                else
                    goForwardButton.setTextColor(0xFF808080);
            }
        });
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setJavaScriptEnabled(true);

        int v = getIntent().getIntExtra("url", 0);
        if (v == 1)
            webView.loadUrl(Constants.BUY_SENSOR_URL);
        else
            webView.loadUrl(Constants.HELP_URL);
    }
    
    @OnClick(R.id.backButton)
    public void onBackButtonClick(View v) {
        onBackPressed();
    }

}

