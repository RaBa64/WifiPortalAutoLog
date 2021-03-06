package org.mike.autolog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.CaptivePortal;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.mike.autolog.volley.NetworkRequest;

public class MainActivity extends AppCompatActivity {

    private WebView web;
    private Network net;
    private CaptivePortal captivePortal;
    private TextView textView;
    private static final String URL = "http://clients3.google.com/generate_204";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent intent = getIntent();
        if (ConnectivityManager.ACTION_CAPTIVE_PORTAL_SIGN_IN.equals(intent.getAction())) {
            net = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK);
            captivePortal = intent.getParcelableExtra(ConnectivityManager.EXTRA_CAPTIVE_PORTAL);
        }
        web = findViewById(R.id.webView);
        textView = findViewById(R.id.info_view);
        textView.setText(R.string.info_starting);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onStart() {
        super.onStart();
        if (net!=null && captivePortal != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.bindProcessToNetwork(net);
            final RequestQueue queue = Volley.newRequestQueue(this);

            final WebViewClient webClient = new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    textView.setText(R.string.info_portal_injecting);
                    view.evaluateJavascript("document.getElementById('user.username').value='"+ PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("pref_user_id", "") +"';" +
                            "document.getElementById('user.password').value='"+ PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("pref_password", "") +"';" +
                            "document.getElementById('aupAccepted').click();" +
                            "document.getElementById('ui_login_signon_button').click();", new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {

                        }
                    });
                    captivePortal.reportCaptivePortalDismissed();
                    finish();
                }
            };

            web.getSettings().setJavaScriptEnabled(true);
            web.setWebViewClient(webClient);


            NetworkRequest checkConnectionRequest = new NetworkRequest(Request.Method.GET, URL,
                    new Response.Listener<NetworkResponse>() {
                        @Override
                        public void onResponse(NetworkResponse response) {
                            if (response.headers.containsKey("Location")) { //some portal returns 200 instead of 302
                                web.loadUrl(response.headers.get("Location"));
                            }else {
                                textView.setText(R.string.info_no_portal);
                                captivePortal.reportCaptivePortalDismissed();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (error.networkResponse.statusCode == 302) {
                                textView.setText(R.string.info_starting);
                                web.loadUrl(error.networkResponse.headers.get("Location"));
                            } else {
                                captivePortal.reportCaptivePortalDismissed();
                                textView.setText(R.string.info_unknown_error);
                            }
                        }
                    });

            queue.add(checkConnectionRequest);
        }
    }
}

