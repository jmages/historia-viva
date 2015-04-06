package jujumap.jjmap;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

public class WebViewAct extends Activity {

    WebView webView;
    String  url_o;
    String  url_e;

    @Override
    public void onCreate (Bundle bundle) {

        super.onCreate (bundle);

        Bundle b = getIntent().getExtras();

        url_o = b.getString("url");

        Log.d("url_o", url_o);

        url_e = (url_o);

        Log.d("url_e", url_e);

        setContentView(R.layout.webview);

        webView = (WebView)findViewById(R.id.webview);

        //webView.setWebViewClient(new WebViewClient());

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                webView.loadUrl(url_e);
            }

        }, 400);
    }
}
