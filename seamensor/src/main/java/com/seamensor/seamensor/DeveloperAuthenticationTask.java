package com.seamensor.seamensor;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.seamensor.seamensor.cognitoclient.Response;
import com.seamensor.seamensor.DeveloperAuthenticationProvider;
import com.seamensor.seamensor.LoginCredentials;

/**
 * A class which performs the task of authentication the user. For the sample it
 * validates a set of username and possword against the sample Cognito developer
 * authentication application
 */
public class DeveloperAuthenticationTask extends
        AsyncTask<LoginCredentials, Void, Void> {

    // The user name or the developer user identifier you will pass to the
    // Amazon Cognito in the GetOpenIdTokenForDeveloperIdentity API
    private String mymToken;

    private boolean isSuccessful;

    private final Context context;

    public DeveloperAuthenticationTask(Context context) {
        this.context = context;
    }

    @Override
    protected Void doInBackground(LoginCredentials... params) {

        Response response = DeveloperAuthenticationProvider
                .getDevAuthClientInstance()
                .login(params[0].getMyMToken());
        isSuccessful = response.requestWasSuccessful();
        mymToken = params[0].getMyMToken();

        if (isSuccessful) {
            CognitoSyncClientManager
                    .addLogins(
                            ((DeveloperAuthenticationProvider) CognitoSyncClientManager.credentialsProvider
                                    .getIdentityProvider()).getProviderName(),
                            mymToken);
            // Always remember to call refresh after updating the logins map
            ((DeveloperAuthenticationProvider) CognitoSyncClientManager.credentialsProvider
                    .getIdentityProvider()).refresh();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if (isSuccessful) {
            new AlertDialog.Builder(context).setTitle("Login OK")
                    .setMessage("Success!!").show();
        }
        if (!isSuccessful) {
            new AlertDialog.Builder(context).setTitle("Login error")
                    .setMessage("Credentials not accepted!!").show();
        }
    }
}