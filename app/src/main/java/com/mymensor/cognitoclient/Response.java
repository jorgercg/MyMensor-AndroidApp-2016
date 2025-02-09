package com.mymensor.cognitoclient;

/**
 * A class for storing the response from sample Cognito developer
 * authentication.
 */
public class Response {
    public static final Response SUCCESSFUL = new Response(200, "OK");

    private final int responseCode;
    private final String responseMessage;

    public Response(final int responseCode, final String responseMessage) {
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
    }

    public boolean requestWasSuccessful() {
        return this.getResponseCode() == 200;
    }

    public int getResponseCode() {
        return this.responseCode;
    }

    public String getResponseMessage() {
        return this.responseMessage;
    }
}
