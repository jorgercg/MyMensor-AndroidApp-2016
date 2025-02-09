package com.mymensor.cognitoclient;

/**
 * This class is used to store the response of the Login call of sample Cognito
 * developer authentication.
 */
public class LoginResponse extends Response {
    private final String key;

    public LoginResponse(final int responseCode, final String responseMessage) {
        super(responseCode, responseMessage);
        this.key = null;
    }

    public LoginResponse(final String key) {
        super(200, null);
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
