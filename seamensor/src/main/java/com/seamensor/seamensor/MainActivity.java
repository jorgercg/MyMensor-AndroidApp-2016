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

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.seamensor.seamensor.cognitoclient.AwsUtil;

import java.util.List;

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

    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;

    // A List of all transfers
    private List<TransferObserver> observers;

    SharedPreferences sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mAccountManager = AccountManager.get(this);

        final Account availableAccounts[] = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);

        sharedPref = getApplicationContext().getSharedPreferences("com.mymensor.app", Context.MODE_PRIVATE);

        /**
         * Initializes the sync client. This must be call before you can use it.
         */
        CognitoSyncClientManager.init(this);

        setContentView(R.layout.activity_main);
        mainLinearLayout = (LinearLayout)findViewById(R.id.MainActivityLinearLayout);
        exitButton = (ImageButton) findViewById(R.id.exitbutton);
        appLogo = (ImageView) findViewById(R.id.mainactivity_logo);
        userLogged = (TextView) findViewById(R.id.userlogstate_message);
        logInOut = (Button) findViewById(R.id.buttonlog);
        startConfig = (Button) findViewById(R.id.buttonconfig);
        startCap = (Button) findViewById(R.id.buttoncap);

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        initData();

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
                getExistingAccountAuthToken(availableAccounts[0], Constants.AUTHTOKEN_TYPE_FULL_ACCESS);
            }
        }


        boolean showDialog = false;
        boolean invalidate = false;

        if (savedInstanceState != null) {
            showDialog = savedInstanceState.getBoolean(STATE_DIALOG);
            invalidate = savedInstanceState.getBoolean(STATE_INVALIDATE);
            if (showDialog) {
                showAccountPicker(Constants.AUTHTOKEN_TYPE_FULL_ACCESS, invalidate);
            }

        }

        Log.d(TAG, "showDialog = " + showDialog + " invalidate="+invalidate);

        if (availableAccounts.length > 1){
            showAccountPicker(Constants.AUTHTOKEN_TYPE_FULL_ACCESS, invalidate);
        }

    }

    /**
     * Gets all relevant transfers from the Transfer Service
     */
    private void initData() {
        // Use TransferUtility to get all upload transfers.
        observers = transferUtility.getTransfersWithType(TransferType.UPLOAD);
        for (TransferObserver observer : observers) {

            // Sets listeners to in progress transfers
            if (TransferState.WAITING.equals(observer.getState())
                    || TransferState.WAITING_FOR_NETWORK.equals(observer.getState())
                    || TransferState.IN_PROGRESS.equals(observer.getState())) {
                transferUtility.resume(observer.getId());
            }
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
                    editor.putString(Constants.MYM_KEY,authtoken);
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

    private void getCognitoIdAndToken(String authToken){
        // Clear the existing credentials
        CognitoSyncClientManager.credentialsProvider
                .clearCredentials();
        // Initiate user authentication against the
        // developer backend in this case the sample Cognito
        // developer authentication application.
        ((DeveloperAuthenticationProvider) CognitoSyncClientManager.credentialsProvider
                .getIdentityProvider()).login(authToken, MainActivity.this);
    }

    private void addNewAccount(String accountType, String authTokenType) {
        final AccountManagerFuture<Bundle> future = mAccountManager.addAccount(accountType, authTokenType, null, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Bundle bnd = future.getResult();
                    Log.d(TAG, "AddNewAccount Bundle is " + bnd.toString());
                    logInOut.setVisibility(View.GONE);
                    startConfig.setVisibility(View.VISIBLE);
                    startCap.setVisibility(View.VISIBLE);
                    userLogged.setText(getText(R.string.userstate_loggedin)+" "+ bnd.getString("authAccount"));
                    String mymtoken = sharedPref.getString(Constants.MYM_KEY," ");
                    Log.d(TAG, "AddNewAccount Token is " + mymtoken);
                    getCognitoIdAndToken(mymtoken);
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
