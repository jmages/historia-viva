package jujumap.juju;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import java.util.Timer;
import java.util.TimerTask;

public class WebViewAct extends Activity {

    WebView webView;
    String  url;

    @Override
    public void onCreate (Bundle bundle) {

        super.onCreate (bundle);

        Bundle b = getIntent().getExtras();

        url = b.getString("url");

        setContentView(R.layout.webview);

        webView = (WebView)findViewById(R.id.webview);

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {

            @Override
            public void run() { webView.loadUrl(url); }

        }, 400);
    }
}
