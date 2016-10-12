package com.seamensor.seamensor;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amazonaws.auth.AWSAbstractCognitoDeveloperIdentityProvider;
import com.amazonaws.regions.Regions;
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

import static android.content.ContentValues.TAG;

/**
 * A class used for communicating with developer backend. This implementation
 * is meant to communicate with the Cognito Developer Authentication sample
 * service: https://github.com/awslabs/amazon-cognito-developer-authentication-sample
 */
public class DeveloperAuthenticationProvider extends AWSAbstractCognitoDeveloperIdentityProvider {

    private static final String developerProvider = Constants.AUTH_COGDEV_PROV_NAME;

    private SharedPreferences sharedPref;

    public DeveloperAuthenticationProvider(String accountId,
                                           String identityPoolId, Context context, Regions region) {
        super(accountId, identityPoolId, region);

        sharedPref = context.getSharedPreferences("MYM",Context.MODE_PRIVATE);

    }

    @Override
    public String getProviderName() {
        return developerProvider;
    }


    @Override
    public String refresh() {
        // Override the existing token
        setToken(null);
        HashMap<String, String> cognito_data = new HashMap<String, String>();
        // Get the identityId and token by making a call to your backend
        final String authToken = sharedPref.getString("mym_authToken","07cda8a18180252862884d7c748faf8bb5c0cb89"); //////////////////////////////////////////////////////////////
        Log.d("VLPauthProv:authTkn=",authToken);

        // Formulate the request and handle the response.
        final HashMap<String, String> cog_response = new HashMap<String, String>();
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.AUTH_COGDEV_SERVER,new JSONObject(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.v("Response:%n %s", response.toString(4));
                            Log.d(TAG, "Respose:"+ response.toString());
                            String userCogIdentityId = response.getString("IdentityId");
                            String userCogToken = response.getString("Token");
                            cog_response.put("cog_identityId", userCogIdentityId);
                            cog_response.put("cog_openIdToken", userCogToken);
                            cog_response.put("mym_authToken", authToken);
                            Log.d("cog_identityId", userCogIdentityId);
                            Log.d("cog_openIdToken", userCogToken);
                            Log.d("mym_authToken", authToken);
                            // Call the update method with updated identityId and token to make sure
                            // these are ready to be used from Credentials Provider.
                            if ((cog_response.get("cog_identityId")!=null)&&(cog_response.get("cog_openIdToken")!=null)){
                                Log.d("VLP auth Prov: refresh",cog_response.get("cog_identityId"));
                                Log.d("VLP auth Prov: refresh",cog_response.get("cog_openIdToken"));
                                update(cog_response.get("cog_identityId"), cog_response.get("cog_openIdToken"));
                            } else {
                                Log.d("VLP auth Prov: refresh","Refresh failed, token stil the same");
                            }

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
        return null;
    }

    // If the app has a valid identityId return it, otherwise get a valid
    // identityId from your backend.
    @Override
    public String getIdentityId() {
        // Load the identityId from the cache
        HashMap<String, String> cognito_data = new HashMap<String, String>();
        identityId = null;
        if (identityId == null) {
            int retries = 10;
            do {
                cognito_data = GetCogOpenIDTokenFromMymServer.getCognitoIdAndToken(sharedPref.getString("mym_authToken"," "));
            } while ((retries-- > 0)||(cognito_data.get("cog_identityId")!=null));
            if (cognito_data.get("cog_identityId")!=null){
                identityId = cognito_data.get("cog_identityId");
                Log.d("VLPauthProv:retrievedID",identityId);
            }else {
                Log.d("VLP auth Prov: getID","getIdentityId failed, toidentityIdken stil null");
            }
        } else {
            return identityId;
        }
        return identityId;
    }


}
