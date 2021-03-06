//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.client;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.microsoft.identity.msal.R;

/**
 * Custom tab requires the device to have a browser with custom tab support, chrome with version >= 45 comes with the
 * support and is available on all devices with API version >= 16 . The sdk use chrome custom tab, and before launching
 * chrome custom tab, we need to check if chrome package is in the device. If it is, it's safe to launch the chrome
 * custom tab; Otherwise the sdk will launch chrome.
 * AuthenticationActivity will be responsible for checking if it's safe to launch chrome custom tab, if not, will
 * go with chrome browser, if chrome is not installed, we throw error back.
 */
public final class AuthenticationActivity extends Activity
{
    private WebView webView;

    private static final String TAG = AuthenticationActivity.class.getSimpleName();

    private String mRequestUrl;
    private int mRequestId;
    private UiEvent.Builder mUiEventBuilder;
    private String mTelemetryRequestId;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_authentication);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setVisibility(View.INVISIBLE);

        if (savedInstanceState != null)
        {
            Logger.verbose(TAG, null, "AuthenticationActivity is re-created after killed by the os.");
            mTelemetryRequestId = savedInstanceState.getString(Constants.TELEMETRY_REQUEST_ID);
            mUiEventBuilder = new UiEvent.Builder();
            return;
        }

        final Intent data = getIntent();
        if (data == null)
        {
            sendError("Received null data intent from caller");
            return;
        }

        mRequestUrl = data.getStringExtra(Constants.REQUEST_URL_KEY);
        mRequestId = data.getIntExtra(Constants.REQUEST_ID, 0);
        if (MsalUtils.isEmpty(mRequestUrl))
        {
            sendError("Request url is not set on the intent");
            return;
        }

        mTelemetryRequestId = data.getStringExtra(Constants.TELEMETRY_REQUEST_ID);
        mUiEventBuilder = new UiEvent.Builder();
        Telemetry.getInstance().startEvent(mTelemetryRequestId, mUiEventBuilder.getEventName());

        mRequestUrl = this.getIntent().getStringExtra(Constants.REQUEST_URL_KEY);

        Logger.infoPII(TAG, null, "Request to launch is: " + mRequestUrl);

        webView.setWebChromeClient(new WebChromeClient()
        {
            public void onProgressChanged(WebView view, int progress)
            {
                if (!view.getUrl().startsWith("msal") && progress == 100)
                {
                    webView.setVisibility(View.VISIBLE);
                }
            }
        });


        webView.setWebViewClient(new WebViewClient()
        {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon)
            {
                view.setVisibility(View.INVISIBLE);
                if (!TextUtils.isEmpty(url) && url.startsWith("msal"))
                {
                    final Intent intent = new Intent(getApplicationContext(), BrowserTabActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                    intent.addCategory(Intent.CATEGORY_BROWSABLE);
                    intent.setDataAndNormalize(Uri.parse(url));
                    startActivity(intent);
                }
                else
                {
                    super.onPageStarted(view, url, favicon);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                if (url.contains("access_denied"))
                {
                    finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(final WebView view, String url)
            {
                super.onPageFinished(view, url);
            }

        });
        webView.loadUrl(mRequestUrl);
    }

    /**
     * OnNewIntent will be called before onResume.
     */
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        Logger.info(TAG, null, "onNewIntent is called, received redirect from system webview.");

        final String url = intent.getStringExtra(Constants.CUSTOM_TAB_REDIRECT);
        final Intent resultIntent = new Intent();
        resultIntent.putExtra(Constants.AUTHORIZATION_FINAL_URL, url);
        returnToCaller(Constants.UIResponse.AUTH_CODE_COMPLETE, resultIntent);
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState)
    {
        super.onSaveInstanceState(outState);

        outState.putString(Constants.REQUEST_URL_KEY, mRequestUrl);
        outState.putString(Constants.TELEMETRY_REQUEST_ID, mTelemetryRequestId);
    }

    /**
     * Return the error back to caller.
     *
     * @param resultCode The result code to return back.
     * @param data       {@link Intent} contains the detailed result.
     */
    private void returnToCaller(final int resultCode, final Intent data)
    {
        Logger.info(TAG, null, "Return to caller with resultCode: " + resultCode + "; requestId: " + mRequestId);
        data.putExtra(Constants.REQUEST_ID, mRequestId);

        if (null != mUiEventBuilder)
        {
            Telemetry.getInstance().stopEvent(mTelemetryRequestId, mUiEventBuilder);
        }

        setResult(resultCode, data);
        this.finish();
    }

    /**
     * Send error back to caller with the error description.
     *
     * @param errorDescription The error description to send back.
     */
    private void sendError(final String errorDescription)
    {
        Logger.info(TAG, null, "Sending error back to the caller, errorCode: " + MsalClientException.UNRESOLVABLE_INTENT + "; errorDescription"
                + errorDescription);
        final Intent errorIntent = new Intent();
        errorIntent.putExtra(Constants.UIResponse.ERROR_CODE, MsalClientException.UNRESOLVABLE_INTENT);
        errorIntent.putExtra(Constants.UIResponse.ERROR_DESCRIPTION, errorDescription);
        returnToCaller(Constants.UIResponse.AUTH_CODE_ERROR, errorIntent);
    }
}
