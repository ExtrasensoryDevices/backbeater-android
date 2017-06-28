package com.esdevices.backbeater.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.bind(this);
    
        webView.setBackgroundColor(getResources().getColor(R.color.main_color));
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(Constants.HELP_URL);
    }
    
    @OnClick(R.id.backButton)
    public void onBackButtonClick(View v) {
        onBackPressed();
    }
    
    
    //private void showErrorDialog() {
    //    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    //    builder.setTitle("An error occurred");
    //    builder.setMessage("Failed to connect to the server, please try again later.");
    //    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    //        public void onClick(DialogInterface dialog, int id) {
    //            dialog.cancel();
    //        }
    //    }).show();
    //}
}

