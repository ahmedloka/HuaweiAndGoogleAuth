package com.h.authhuaweiandgoogle.activites;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.RestrictionsManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.snackbar.Snackbar;
import com.h.authhuaweiandgoogle.R;
import com.h.authhuaweiandgoogle.utilities.MainApplication;
import com.squareup.picasso.Picasso;
import java.util.HashMap;
import java.util.Map;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthState.AuthStateAction;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationRequest.Builder;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationService.TokenResponseCallback;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.TokenResponse;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.json.JSONException;
import org.json.JSONObject;

public class GoogleAuthActivity extends AppCompatActivity {
    private static final String AUTH_STATE = "AUTH_STATE";
    private static final String LOGIN_HINT = "login_hint";
    private static final String SHARED_PREFERENCES_NAME = "AuthStatePreference";
    private static final String USED_INTENT = "USED_INTENT";
    private AuthState mAuthState;
    private AppCompatButton mAuthorize;
    private AppCompatTextView mFamilyName;
    private AppCompatTextView mFullName;
    private AppCompatTextView mGivenName;
    private String mLoginHint;
    private MainApplication mMainApplication;
    private AppCompatButton mMakeApiCall;
    private ImageView mProfileView;
    private BroadcastReceiver mRestrictionsReceiver;
    private AppCompatButton mSignOut;

    public static class AuthorizeListener implements OnClickListener {
        private final GoogleAuthActivity googleAuthActivity;

        AuthorizeListener(@NonNull GoogleAuthActivity mainActivity) {
            this.googleAuthActivity = mainActivity;
        }

        public void onClick(View view) {
            Builder builder = new Builder(new AuthorizationServiceConfiguration(Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"), Uri.parse("https://www.googleapis.com/oauth2/v4/token")), "511828570984-fuprh0cm7665emlne3rnf9pk34kkn86s.apps.googleusercontent.com", AuthorizationRequest.RESPONSE_TYPE_CODE, Uri.parse("com.google.codelabs.appauth:/oauth2callback"));
            builder.setScopes("profile");
            if (this.googleAuthActivity.getLoginHint() != null) {
                Map loginHintMap = new HashMap();
                loginHintMap.put(GoogleAuthActivity.LOGIN_HINT, this.googleAuthActivity.getLoginHint());
                builder.setAdditionalParameters(loginHintMap);
                Log.i(MainApplication.LOG_TAG, String.format("login_hint: %s", this.googleAuthActivity.getLoginHint()));
                Log.i(MainApplication.LOG_TAG, String.format("login_hint: %s", this.googleAuthActivity.getLoginHint()));
            }
            AuthorizationRequest request = builder.build();
            new AuthorizationService(view.getContext()).performAuthorizationRequest(request, PendingIntent.getActivity(view.getContext(), request.hashCode(), new Intent("com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE"), 0));
        }
    }

    public static class MakeApiCallListener implements OnClickListener {
        /* access modifiers changed from: private */
        AuthState mAuthState;
        /* access modifiers changed from: private */
        AuthorizationService mAuthorizationService;
        /* access modifiers changed from: private */
        final GoogleAuthActivity googleAuthActivity;

        MakeApiCallListener(@NonNull GoogleAuthActivity mainActivity, @NonNull AuthState authState, @NonNull AuthorizationService authorizationService) {
            this.googleAuthActivity = mainActivity;
            this.mAuthState = authState;
            this.mAuthorizationService = authorizationService;
            this.mAuthState.performActionWithFreshTokens(this.mAuthorizationService, new AuthStateAction() {
                public void execute(String accessToken, String idToken, AuthorizationException ex) {
                    if (ex == null) {
                        Log.i(MainApplication.LOG_TAG, String.format("TODO: make an API call with [Access Token: %s, ID Token: %s]", accessToken, idToken));
                        MakeApiCallListener.this.mAuthState.performActionWithFreshTokens(MakeApiCallListener.this.mAuthorizationService, new AuthStateAction() {
                            public void execute(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                                MakeApiCallListener.this.fetchAccountData().execute(accessToken);
                            }
                        });
                    }
                }
            });
        }

        /* access modifiers changed from: private */
        AsyncTask<String, Void, JSONObject> fetchAccountData() {
            return new AsyncTask<String, Void, JSONObject>() {
                /* access modifiers changed from: protected */
                public JSONObject doInBackground(String... tokens) {
                    try {
                        String jsonBody = new OkHttpClient().newCall(new Request.Builder().url("https://www.googleapis.com/oauth2/v3/userinfo").addHeader("Authorization", String.format("Bearer %s", tokens[0])).build()).execute().body().string();
                        Log.i(MainApplication.LOG_TAG, String.format("User Info Response %s", jsonBody));
                        return new JSONObject(jsonBody);
                    } catch (Exception exception) {
                        Log.w(MainApplication.LOG_TAG, exception);
                        return null;
                    }
                }

                /* access modifiers changed from: protected */
                public void onPostExecute(JSONObject userInfo) {
                    String message;
                    if (userInfo != null) {
                        String fullName = userInfo.optString("name", null);
                        String givenName = userInfo.optString("given_name", null);
                        String familyName = userInfo.optString("family_name", null);
                        String imageUrl = userInfo.optString("picture", null);
                        if (!TextUtils.isEmpty(imageUrl)) {
                            Picasso.with(MakeApiCallListener.this.googleAuthActivity).load(imageUrl).placeholder(R.drawable.ic_account_circle_black_48dp).
                                    into(MakeApiCallListener.this.googleAuthActivity.mProfileView);
                        }
                        if (!TextUtils.isEmpty(fullName)) {
                            MakeApiCallListener.this.googleAuthActivity.mFullName.setText(fullName);
                        }
                        if (!TextUtils.isEmpty(givenName)) {
                            MakeApiCallListener.this.googleAuthActivity.mGivenName.setText(givenName);
                        }
                        if (!TextUtils.isEmpty(familyName)) {
                            MakeApiCallListener.this.googleAuthActivity.mFamilyName.setText(familyName);
                        }
                        if (userInfo.has(AuthorizationException.PARAM_ERROR)) {
                            message = String.format("%s [%s]",
                                    MakeApiCallListener.this.googleAuthActivity
                                            .getString(R.string.request_failed), userInfo.optString(AuthorizationException.PARAM_ERROR_DESCRIPTION, "No description"));
                        } else {
                            message = MakeApiCallListener.this.googleAuthActivity.getString(R.string.request_complete);
                        }
                        Snackbar.make(MakeApiCallListener.this.googleAuthActivity.mProfileView, message, Snackbar.LENGTH_SHORT).show();
                    }
                }
            };
        }

        public void onClick(View view) {
        }
    }

    public static class SignOutListener implements OnClickListener {
        private final GoogleAuthActivity googleAuthActivity;

        public SignOutListener(@NonNull GoogleAuthActivity googleAuthActivity) {
            this.googleAuthActivity = googleAuthActivity;
        }

        public void onClick(View view) {
            GoogleAuthActivity mainActivity = this.googleAuthActivity;
            mainActivity.mAuthState = null;
            mainActivity.clearAuthState();
            this.googleAuthActivity.enablePostAuthorizationFlows();
            Toast.makeText(mainActivity, "Sign Out SUCCESS", Toast.LENGTH_SHORT).show();
        }
    }

    /* access modifiers changed from: protected */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_auth);
        this.mMainApplication = (MainApplication) getApplication();
        this.mAuthorize = findViewById(R.id.authorize);
        this.mMakeApiCall = findViewById(R.id.makeApiCall);
        this.mSignOut = findViewById(R.id.signOut);
        this.mGivenName = findViewById(R.id.givenName);
        this.mFamilyName = findViewById(R.id.familyName);
        this.mFullName = findViewById(R.id.fullName);
        this.mProfileView = findViewById(R.id.profileImage);
        enablePostAuthorizationFlows();
        this.mAuthorize.setOnClickListener(new AuthorizeListener(this));
        getAppRestrictions();
    }

    public String getLoginHint() {
        return this.mLoginHint;
    }

    /* access modifiers changed from: private */
    public void enablePostAuthorizationFlows() {
        this.mAuthState = restoreAuthState();
        AuthState authState = this.mAuthState;
        if (authState == null || !authState.isAuthorized()) {
            this.mMakeApiCall.setVisibility(View.GONE);
            this.mSignOut.setVisibility(View.VISIBLE);
            return;
        }
        if (this.mMakeApiCall.getVisibility() == View.GONE) {
            this.mMakeApiCall.setVisibility(View.VISIBLE);
            this.mMakeApiCall.setOnClickListener(new MakeApiCallListener(this, this.mAuthState, new AuthorizationService(this)));
        }
        if (this.mSignOut.getVisibility() == View.GONE) {
            this.mSignOut.setVisibility(View.VISIBLE);
            this.mSignOut.setOnClickListener(new SignOutListener(this));
        }
    }

    private void handleAuthorizationResponse(@NonNull Intent intent) {
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        final AuthState authState = new AuthState(response, AuthorizationException.fromIntent(intent));
        if (response != null) {
            Log.i(MainApplication.LOG_TAG, String.format("Handled Authorization Response %s ", authState.toJsonString()));
            MainApplication mainApplication = this.mMainApplication;
            StringBuilder sb = new StringBuilder();
            sb.append("SUCCESS");
            sb.append(authState.toJsonString());
            Toast.makeText(mainApplication, sb.toString(), Toast.LENGTH_SHORT).show();
            new AuthorizationService(this).performTokenRequest(response.createTokenExchangeRequest(), new TokenResponseCallback() {
                public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                    if (exception != null) {
                        Log.w(MainApplication.LOG_TAG, "Token Exchange failed", exception);
                    } else if (tokenResponse != null) {
                        authState.update(tokenResponse, exception);
                        GoogleAuthActivity.this.persistAuthState(authState);
                        Log.i(MainApplication.LOG_TAG, String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                    }
                }
            });
            return;
        }
        Toast.makeText(this.mMainApplication, "FAiled", Toast.LENGTH_LONG).show();
    }

    /* access modifiers changed from: private */
    public void persistAuthState(@NonNull AuthState authState) {
        getSharedPreferences(SHARED_PREFERENCES_NAME, 0).edit().putString(AUTH_STATE,
                authState.toJsonString()).commit();
        enablePostAuthorizationFlows();
    }

    /* access modifiers changed from: private */
    public void clearAuthState() {
        getSharedPreferences(SHARED_PREFERENCES_NAME, 0).edit().remove(AUTH_STATE).apply();
    }

    @Nullable
    private AuthState restoreAuthState() {
        String jsonString = getSharedPreferences(SHARED_PREFERENCES_NAME, 0).getString(AUTH_STATE, null);
        if (!TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.fromJson(jsonString);
            } catch (JSONException e) {
            }
        }
        return null;
    }

    /* access modifiers changed from: protected */
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkIntent(intent);
    }

    private void checkIntent(@Nullable Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            // do nothing
            if ("com.google.codelabs.appauth.HANDLE_AUTHORIZATION_RESPONSE".equals(action)) {
                if (!intent.hasExtra(USED_INTENT)) {
                    handleAuthorizationResponse(intent);
                    intent.putExtra(USED_INTENT, true);
                }
            }
        }
    }

    /* access modifiers changed from: protected */
    public void onStart() {
        super.onStart();
        checkIntent(getIntent());
        registerRestrictionsReceiver();
    }

    /* access modifiers changed from: protected */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onResume() {
        super.onResume();
        getAppRestrictions();
        registerRestrictionsReceiver();
    }

    /* access modifiers changed from: protected */
    public void onStop() {
        super.onStop();
        unregisterReceiver(this.mRestrictionsReceiver);
    }

    /* access modifiers changed from: private */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void getAppRestrictions() {
        Bundle appRestrictions = ((RestrictionsManager) getSystemService(Context.RESTRICTIONS_SERVICE)).getApplicationRestrictions();
        if (appRestrictions.isEmpty()) {
            return;
        }
        if (!appRestrictions.getBoolean("restrictions_pending")) {
            this.mLoginHint = appRestrictions.getString(LOGIN_HINT);
            return;
        }
        Toast.makeText(this, R.string.restrictions_pending_block_user, Toast.LENGTH_LONG).show();
        finish();
    }

    private void registerRestrictionsReceiver() {
        IntentFilter restrictionsFilter = new IntentFilter
                ("android.intent.action.APPLICATION_RESTRICTIONS_CHANGED");
        this.mRestrictionsReceiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            public void onReceive(Context context, Intent intent) {
                GoogleAuthActivity.this.getAppRestrictions();
            }
        };
        registerReceiver(this.mRestrictionsReceiver, restrictionsFilter);
    }
}
