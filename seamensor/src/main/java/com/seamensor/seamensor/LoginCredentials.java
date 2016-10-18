package com.seamensor.seamensor;

/**
 * A class used to encapsulate user's credentials for showing the sample
 * developer authenticaeted feature of Amazon Cognito
 */
public class LoginCredentials {
    private String mymtoken;

    public LoginCredentials(String mymtoken) {
        this.mymtoken = mymtoken;
    }

    public String getMyMToken() {
        return mymtoken;
    }

    public void setMyMToken(String mymtoken) {
        this.mymtoken = mymtoken;
    }
}
