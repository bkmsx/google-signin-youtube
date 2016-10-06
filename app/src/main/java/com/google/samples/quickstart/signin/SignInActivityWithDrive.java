package com.google.samples.quickstart.signin;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelListResponse;
import com.google.api.services.youtube.model.ChannelSnippet;
import com.google.api.services.youtube.model.ChannelStatistics;

import java.io.IOException;
import java.util.List;

/**
 * Activity to demonstrate basic retrieval of the Google user's ID, email address, and basic
 * profile, which also adds a request dialog to access the user's Google Drive.
 */
public class SignInActivityWithDrive extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;

    private GoogleApiClient mGoogleApiClient;
    private TextView mStatusTextView;
    private ProgressDialog mProgressDialog;
    private String mIdToken = "";
    private String mClientId="";
    private String mAccessToken="";
    private String mAuthCode="";
    private String CLIENT_SECRET = "3bwHFE_KtOwwcFHaN5dISCj1";
    private String mResult="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Views
        mStatusTextView = (TextView) findViewById(R.id.status);

        // Button listeners
        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);
        findViewById(R.id.disconnect_button).setOnClickListener(this);
        Button button = (Button) findViewById(R.id.btn_with_drive);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getYoutubeInfo();
            }
        });
        mClientId = getString(R.string.server_client_id);

        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(YouTubeScopes.YOUTUBE))
                .requestEmail()
                .requestIdToken(mClientId)
                .requestServerAuthCode(mClientId)
                .build();
        // [END configure_signin]

        // [START build_client]
        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        // [END build_client]

        // [START customize_button]
        // Customize sign-in button. The sign-in button can be displayed in
        // multiple sizes and color schemes. It can also be contextually
        // rendered based on the requested scopes. For example. a red button may
        // be displayed when Google+ scopes are requested, but a white button
        // may be displayed when only basic profile is requested. Try adding the
        // Scopes.PLUS_LOGIN scope to the GoogleSignInOptions to see the
        // difference.
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setScopes(gso.getScopeArray());
        // [END customize_button]
    }

    @Override
    public void onStart() {
        super.onStart();

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);

        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            showProgressDialog();
            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(GoogleSignInResult googleSignInResult) {
                    hideProgressDialog();
                    handleSignInResult(googleSignInResult);
                }
            });
        }
    }

    // [START onActivityResult]
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }
    // [END onActivityResult]

    // [START handleSignInResult]
    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            mStatusTextView.setText(acct.getDisplayName()+" : "+acct.getGrantedScopes().toArray()[0].toString()+" : "+acct.getIdToken());
            mIdToken = acct.getIdToken();
            mAuthCode = acct.getServerAuthCode();
            log("Id Token: "+mIdToken);
            log("Auth Code: "+mAuthCode);
            updateUI(true);
        } else {
            // Signed out, show unauthenticated UI.
            updateUI(false);
        }
    }

    public void getYoutubeInfo(){
        new AsyncTaskGetInfo().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    private class AsyncTaskGetInfo extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(SignInActivityWithDrive.this);
            progressDialog.setMessage("Wait..");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            GoogleTokenResponse tokenResponse = null;
            try {
                tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                        new NetHttpTransport(),
                        JacksonFactory.getDefaultInstance(),
                        "https://www.googleapis.com/oauth2/v4/token",
                        mClientId,
                        CLIENT_SECRET,
                        mAuthCode,
                        "")
                        .execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (tokenResponse != null) {
                mAccessToken = tokenResponse.getAccessToken();

                log("Access Token: "+mAccessToken);
                log("Refresh Token: "+tokenResponse.getRefreshToken());
            } else {
                log("Token Response null");
            }

            GoogleCredential credential = new GoogleCredential().setAccessToken(mAccessToken);

            YouTube youTube = null;
            String channelId = "UClv3w8p-lJJYkCieeYeigbg";

            youTube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(),credential).setApplicationName("bkmsx").build();

            ChannelListResponse channelListResponse = null;
            try {
                channelListResponse = youTube.channels().list("snippet,statistics").setMine(true).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<Channel> channelList = channelListResponse.getItems();
            if (channelList.isEmpty()) {
                System.out.println("Can't find a channel with ID: " + channelId);
                return null;
            }
            Channel channel = channelList.get(0);
            ChannelSnippet snippet = channel.getSnippet();
            ChannelStatistics statistics = channel.getStatistics();
            String semicolon = " : ";
            String separate = ", ";
            mResult= snippet.getTitle()+semicolon+statistics.getSubscriberCount()+" subscribers"+semicolon+statistics.getVideoCount()+" videos"+separate+statistics.getViewCount()+" views";
            log("  - ID for searched channel: " + statistics.getSubscriberCount()+semicolon+statistics.getVideoCount());
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            Toast.makeText(SignInActivityWithDrive.this, mResult, Toast.LENGTH_LONG).show();
        }
    }

    public static void log(String msg) {
        Log.e("Log for signin app: ", msg);
    }
    // [END handleSignInResult]

    // [START signIn]
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signIn]

    // [START signOut]
    private void signOut() {
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END signOut]

    // [START revokeAccess]
    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        // [START_EXCLUDE]
                        updateUI(false);
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END revokeAccess]

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }

    private void updateUI(boolean signedIn) {
        if (signedIn) {
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.signed_out);

            findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.disconnect_button:
                revokeAccess();
                break;
        }
    }
}
