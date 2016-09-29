package com.seamensor.seamensor;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import static android.accounts.AccountManager.KEY_BOOLEAN_RESULT;

public class MymAccAuthenticator extends AbstractAccountAuthenticator {

    private final Context mContext;

    public MymAccAuthenticator(Context context)
    {
        super(context);
        this.mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        Log.d("MymAccAuthenticator:", "Called-addAccount");
        final Intent intent = new Intent(mContext, MymAccAuthenticatorActivity.class);
        intent.putExtra(MymAccAuthenticatorActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(MymAccAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(MymAccAuthenticatorActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);

        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        Log.d("MymAccAuthenticator:", "Called-confirmCred");
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d("MymAccAuthenticator:", "Called-getAuthTokn");
        return null;
    }


    @Override
    public String getAuthTokenLabel(String authTokenType) {
        Log.d("MymAccAuthenticator:", "Called-getAuthLbl");
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
        Log.d("MymAccAuthenticator:", "Called-updateCred");
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        Log.d("MymAccAuthenticator:", "Called-hasFeatures");
        final Bundle result = new Bundle();
        result.putBoolean(KEY_BOOLEAN_RESULT, false);
        return result;
    }
}



