package com.deffe.max.chatgoo;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class WebFragment extends Fragment
{
    public WebFragment()
    {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_web, container, false);

        String url = null;

        if (getArguments() != null)
        {

            url = getArguments().getString("url");
        }

        WebView webView = view.findViewById(R.id.webview);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setJavaScriptEnabled(true);

        webView.loadUrl(url);

        return view;
    }
}
