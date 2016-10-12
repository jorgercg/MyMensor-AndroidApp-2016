package com.seamensor.seamensor;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;

public class AwsUtil {

    private static AmazonS3Client sS3Client;
    private static CognitoCachingCredentialsProvider sCredProvider;
    protected static CognitoCachingCredentialsProvider credentialsProvider = null;
    private static DeveloperAuthenticationProvider developerAuthenticationProvider;
    private static TransferUtility sTransferUtility;

    private static DeveloperAuthenticationProvider getDeveloperAuthenticationProvider(Context context) {
        if (developerAuthenticationProvider == null){
            developerAuthenticationProvider = new DeveloperAuthenticationProvider(
                    null,
                    Constants.COGNITO_POOL_ID,
                    context.getApplicationContext(),
                    Constants.COGNITO_POOL_ID_REGION);
        }
        return developerAuthenticationProvider;
    }

    private static CognitoCachingCredentialsProvider getCredProvider(Context context) {
        if (sCredProvider == null) {
            sCredProvider = new CognitoCachingCredentialsProvider(
                    context.getApplicationContext(),
                    getDeveloperAuthenticationProvider(context.getApplicationContext()),
                    Constants.COGNITO_POOL_ID_REGION);
        }
        Log.d("CogCach",sCredProvider.getCachedIdentityId()+" - "+sCredProvider.getIdentityId());
        return sCredProvider;
    }

    public static AmazonS3Client getS3Client(Context context) {
        if (sS3Client == null) {
            sS3Client = new AmazonS3Client(getCredProvider(context.getApplicationContext()));
        }
        return sS3Client;
    }

    public static TransferUtility getTransferUtility(Context context) {
        if (sTransferUtility == null) {
            sTransferUtility = new TransferUtility(getS3Client(context.getApplicationContext()),
                    context.getApplicationContext());
        }

        return sTransferUtility;
    }

    public static CognitoCredentialsProvider getCredentialsProvider(Context context) {
        if (credentialsProvider == null) {
            credentialsProvider = getCredProvider(context.getApplicationContext());}
        return credentialsProvider;
    }




}