package com.seamensor.seamensor;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.seamensor.seamensor.Constants.AUTHTOKEN_TYPE_FULL_ACCESS;


public class MainActivity extends Activity {

    private static final String TAG = "MYMMainActivity";
    private static long back_pressed;

    private static final String STATE_DIALOG = "state_dialog";
    private static final String STATE_INVALIDATE = "state_invalidate";

    LinearLayout mainLinearLayout;
    ImageButton exitButton;
    ImageView appLogo;
    Button logInOut;
    Button startConfig;
    Button startCap;
    TextView userLogged;

    private AccountManager mAccountManager;
    private AlertDialog mAlertDialog;
    private boolean mInvalidate;

    SharedPreferences sharedPref;

    public GetCredentialProviderLogged getCredentialProviderLogged;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mAccountManager = AccountManager.get(this);

        final Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

        sharedPref = getApplicationContext().getSharedPreferences("MYM", Context.MODE_PRIVATE);

        setContentView(R.layout.activity_main);
        mainLinearLayout = (LinearLayout)findViewById(R.id.MainActivityLinearLayout);
        exitButton = (ImageButton) findViewById(R.id.exitbutton);
        appLogo = (ImageView) findViewById(R.id.mainactivity_logo);
        userLogged = (TextView) findViewById(R.id.userlogstate_message);
        logInOut = (Button) findViewById(R.id.buttonlog);
        startConfig = (Button) findViewById(R.id.buttonconfig);
        startCap = (Button) findViewById(R.id.buttoncap);

        getCredentialProviderLogged = new GetCredentialProviderLogged();

        if (availableAccounts.length == 0){
            Log.d(TAG, "availableAccounts[] = " + "nada!!!!" + " Qty= 0");
        } else {
            Log.d(TAG, "availableAccounts[] = " + availableAccounts[0] + " Qty="+availableAccounts.length);
        }

        if (availableAccounts.length == 0) {
            userLogged.setText(R.string.userstate_loggedout);
            logInOut.setVisibility(View.VISIBLE);
            startConfig.setVisibility(View.GONE);
            startCap.setVisibility(View.GONE);
        } else {
            logInOut.setVisibility(View.GONE);
            startConfig.setVisibility(View.VISIBLE);
            startCap.setVisibility(View.VISIBLE);
            if (availableAccounts.length == 1){
                userLogged.setText(getText(R.string.userstate_loggedin)+" "+availableAccounts[0].name);
                getExistingAccountAuthToken(availableAccounts[0], AUTHTOKEN_TYPE_FULL_ACCESS);
            }
        }


        boolean showDialog = false;
        boolean invalidate = false;

        if (savedInstanceState != null) {
            showDialog = savedInstanceState.getBoolean(STATE_DIALOG);
            invalidate = savedInstanceState.getBoolean(STATE_INVALIDATE);
            if (showDialog) {
                showAccountPicker(AUTHTOKEN_TYPE_FULL_ACCESS, invalidate);
            }

        }

        Log.d(TAG, "showDialog = " + showDialog + " invalidate="+invalidate);

        if (availableAccounts.length > 1){
            showAccountPicker(AUTHTOKEN_TYPE_FULL_ACCESS, invalidate);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAlertDialog != null && mAlertDialog.isShowing()) {
            outState.putBoolean(STATE_DIALOG, true);
            outState.putBoolean(STATE_INVALIDATE, mInvalidate);
        }
    }

    private void showAccountPicker(final String authTokenType, final boolean invalidate) {
        mInvalidate = invalidate;
        final Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

        if (availableAccounts.length == 0) {
            Toast.makeText(this, "No accounts", Toast.LENGTH_SHORT).show();
        } else {
            String name[] = new String[availableAccounts.length];
            for (int i = 0; i < availableAccounts.length; i++) {
                name[i] = availableAccounts[i].name;
            }

            // Account picker
            mAlertDialog = new AlertDialog.Builder(this).setTitle("Pick Account").setAdapter(new ArrayAdapter<String>(getBaseContext(), android.R.layout.simple_list_item_1, name), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (invalidate)
                        invalidateAuthToken(availableAccounts[which], authTokenType);
                    else
                        getExistingAccountAuthToken(availableAccounts[which], authTokenType);
                    userLogged.setText(getText(R.string.userstate_loggedin)+" "+availableAccounts[which].name);
                }
            }).create();
            mAlertDialog.show();
        }
    }

    private void invalidateAuthToken(final Account account, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, authTokenType, null, this, null,null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();
                    final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                    mAccountManager.invalidateAuthToken(account.type, authtoken);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /* Method to get existing mym_authToken from Account manager and fetch Cognito Credentials from Cognito */
    private void getExistingAccountAuthToken(Account account, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.getAuthToken(account, authTokenType, null, this, null, null);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bundle bnd = future.getResult();
                    final String authtoken = bnd.getString(AccountManager.KEY_AUTHTOKEN);
                    final String userName = bnd.getString(AccountManager.KEY_ACCOUNT_NAME);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putString("mym_authToken",authtoken);
                    editor.commit();
                    Log.d(TAG, "GetToken Bundle is " + bnd);
                    logInOut.setVisibility(View.GONE);
                    startConfig.setVisibility(View.VISIBLE);
                    startCap.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Token is " + authtoken);
                    getCognitoIdAndToken(authtoken);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void getCognitoIdAndToken(final String authToken){
        // Formulate the request and handle the response.
        final Bundle cog_response = new Bundle();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.AUTH_COGDEV_SERVER,new JSONObject(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.v("Response:%n %s", response.toString(4));
                            Log.d(TAG, "Respose:"+ response.toString());
                            String userCogIdentityId = response.getString("IdentityId");
                            String userCogToken = response.getString("Token");
                            cog_response.putString("cog_identityId", userCogIdentityId);
                            cog_response.putString("cog_openIdToken", userCogToken);
                            cog_response.putString("mym_authToken", authToken);
                            Log.d("cog_identityId", userCogIdentityId);
                            Log.d("cog_openIdToken", userCogToken);
                            Log.d("mym_authToken", authToken);
                            getCredentialProviderLogged.execute(cog_response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "Respose:"+ error.toString());
                VolleyLog.e("Main Activity: Error COG AUTH TOKEN: ", error.getMessage());
            }

        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization", "Token "+authToken);
                return headers;
            }
        };
        // Add the request to the RequestQueue.
        VolleyHelper.getInstance().addToRequestQueue(jsonObjectRequest);
    }

    public class GetCredentialProviderLogged extends AsyncTask<Bundle, Void, Void> {
        @Override
        protected Void doInBackground(cognito_data... params) {
            if ((("cog_identityId") != null) && (cognito_data.get("cog_openIdToken") != null)) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("cog_identityId", cognito_data.get("cog_identityId"));
                editor.putString("cog_openIdToken", cognito_data.get("cog_openIdToken"));
                editor.putString("mym_authToken", cognito_data.get("mym_authToken"));
                editor.commit();
                DeveloperAuthenticationProvider developerProvider = new DeveloperAuthenticationProvider(null, Constants.COGNITO_POOL_ID, getApplicationContext(), Constants.COGNITO_POOL_ID_REGION);
                CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(getApplicationContext(), developerProvider, Constants.COGNITO_POOL_ID_REGION);
                HashMap<String, String> logins = new HashMap<String, String>();
                logins.put(developerProvider.getProviderName(), "jrcgonc@gmail.com");
                credentialsProvider.setLogins(logins);
                credentialsProvider.refresh();
            } else {
                Log.d(TAG, "getCredentialProviderLogged FAILED");
            }
        }
    }


    private void addNewAccount(String accountType, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.addAccount(accountType, authTokenType, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bnd = future.getResult();
                    Log.d(TAG, "AddNewAccount Bundle is " + bnd);
                    logInOut.setVisibility(View.GONE);
                    startConfig.setVisibility(View.VISIBLE);
                    startCap.setVisibility(View.VISIBLE);
                    userLogged.setText(getText(R.string.userstate_loggedin)+" "+ bnd.getString("authAccount"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, null);
    }

    @Override
    public void onBackPressed()
    {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), getString(R.string.double_bck_exit), Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    public void onButtonClick(View v) {
        if (v.getId() == R.id.exitbutton) {
            if (back_pressed + 2000 > System.currentTimeMillis()) {
                Log.d(TAG, "Closing");
                finish();
            } else
                Toast.makeText(getBaseContext(), getString(R.string.double_bck_exit), Toast.LENGTH_SHORT).show();
            back_pressed = System.currentTimeMillis();
        }
        if (v.getId() == R.id.buttonlog) {
            Log.d(TAG, "Calling method to add a new account");
            addNewAccount(Constants.ACCOUNT_TYPE, Constants.AUTHTOKEN_TYPE_FULL_ACCESS);
        }
        if (v.getId() == R.id.buttonconfig) {
            Log.d(TAG, "Calling the SeaMensor Configuration Activity");
            Intent launch_intent = new Intent(this,LoaderActivity.class);
            launch_intent.putExtra("activitytobecalled", "SMC");
            startActivity(launch_intent);
            finish();
        }
        if (v.getId() == R.id.buttoncap) {
            Log.d(TAG, "Calling the SeaMensor Capture Activity");
            Intent launch_intent = new Intent(this,LoaderActivity.class);
            launch_intent.putExtra("activitytobecalled", "seamensor");
            startActivity(launch_intent);
            finish();
        }
    }

}
