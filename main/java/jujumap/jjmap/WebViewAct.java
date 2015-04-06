package jujumap.jjmap;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Timer;
import java.util.TimerTask;

public class WebViewAct extends Activity {

    WebView webView = null;
    String url     = "";

    @Override
    public void onCreate (Bundle bundle) {

        super.onCreate (bundle);

        Bundle b = getIntent().getExtras();

        url     = b.getString("url");

        setContentView(R.layout.webview);

        webView = (WebView)findViewById(R.id.webview);
        webView.setWebViewClient(new WebViewClient());


        Timer timer = new Timer();

        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                webView.loadUrl(url);
            }

        }, 400);

    }
}
