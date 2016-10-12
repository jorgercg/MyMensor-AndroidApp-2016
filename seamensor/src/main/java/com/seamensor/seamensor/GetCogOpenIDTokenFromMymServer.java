package com.seamensor.seamensor;

import android.util.Log;

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

public class GetCogOpenIDTokenFromMymServer{

    private static final String TAG = "GetCogOpenIDTkFrMymSrv";

    public static HashMap<String, String> getCognitoIdAndToken(final String authToken){
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
        return cog_response;
    }

}
