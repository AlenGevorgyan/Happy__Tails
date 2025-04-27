package com.app.happytails.utils.Fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.app.happytails.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OAuthWebViewFragment extends DialogFragment {

    private static final String ARG_AUTH_URL = "auth_url";
    private static final String REDIRECT_URI = "https://happytails.page.link/UkMX"; // Replace with your actual Redirect URI
    private static final String BACKEND_TOKEN_EXCHANGE_URL = "https://us-central1-rational-photon-380817.cloudfunctions.net/exchangePatreonCode"; // Replace with your Cloud Function URL

    private WebView webView;
    private final OkHttpClient httpClient = new OkHttpClient();
    private FirebaseAuth firebaseAuth;

    public static OAuthWebViewFragment newInstance(String authUrl) {
        OAuthWebViewFragment fragment = new OAuthWebViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_AUTH_URL, authUrl);
        Log.d("WebView Fragment", "Creating new instance with URL: " + authUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patreon_web_view, container, false);
        webView = view.findViewById(R.id.webView);

        // WebView settings (as before)
        webView.getSettings().setJavaScriptEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d("WebView", "Loading URL: " + url);

                if (url.startsWith(REDIRECT_URI)) {
                    Uri uri = Uri.parse(url);
                    String code = uri.getQueryParameter("code");
                    Log.d("OAuthRedirect", "OAuth code received: " + code);

                    if (code != null) {
                        sendCodeToServer(code);
                        dismiss();
                        return true;
                    } else if (uri.getQueryParameter("error") != null) {
                        // Handle error (as before)
                        dismiss();
                        return true;
                    } else {
                        // Handle no code (as before)
                        dismiss();
                        return true;
                    }
                }
                return false;
            }
        });

        String authUrl = getArguments() != null ? getArguments().getString(ARG_AUTH_URL) : "";
        webView.loadUrl(authUrl);

        return view;
    }

    private void sendCodeToServer(String code) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not authenticated.", Toast.LENGTH_SHORT).show();
            return;
        }
        String uid = currentUser.getUid();

        RequestBody requestBody = new FormBody.Builder()
                .add("code", code)
                .add("redirect_uri", REDIRECT_URI)
                .add("firebaseUid", uid) // Send the Firebase UID
                .build();

        Request request = new Request.Builder()
                .url(BACKEND_TOKEN_EXCHANGE_URL)
                .post(requestBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error communicating with the backend.", Toast.LENGTH_LONG).show());
                    Log.e("BackendComm", "Error sending code to backend: " + e.getMessage());
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                final String responseData = response.body().string();
                Log.d("BackendComm", "Backend response: " + responseData);

                if (response.isSuccessful()) {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Patreon authentication successful.", Toast.LENGTH_SHORT).show());
                        if (getParentFragment() instanceof DogProfile) {
                            ((DogProfile) getParentFragment()).requireActivity().runOnUiThread(() ->
                                    ((DogProfile) getParentFragment()).onPatreonAuthSuccess());
                        }
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No body";
                    Log.e("BackendComm", "Backend error during token exchange: " + response.code() + " - " + errorBody);
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Backend error during token exchange.", Toast.LENGTH_LONG).show());
                    }
                }
            }
        });
    }
}