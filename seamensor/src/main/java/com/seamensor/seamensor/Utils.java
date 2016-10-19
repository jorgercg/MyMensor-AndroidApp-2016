package com.seamensor.seamensor;


import android.content.Context;
import android.os.SystemClock;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class Utils {

    public static boolean isNewFileAvailable(   AmazonS3Client s3,
                                                String localFileName,
                                                String remoteFileName,
                                                String bucketName,
                                                Context context) {
        File localFile = new File(context.getFilesDir(), localFileName );
        if (!localFile.exists()) { return true; }
        ObjectMetadata metadata = s3.getObjectMetadata(bucketName,remoteFileName);
        long remoteLastModified = metadata.getLastModified().getTime();
        if (localFile.lastModified()<remoteLastModified) {
            return true;
        }
        else {
            return false;
        }
    }

    public static TransferObserver storeRemoteFile( TransferUtility transferUtility,
                                                    String fileName,
                                                    String bucketName,
                                                    File file,
                                                    ObjectMetadata objectMetadata){

        TransferObserver observer = transferUtility.upload(
                bucketName,		/* The bucket to upload to */
                fileName,		/* The key for the uploaded object */
                file,				/* The file where the data to upload exists */
                objectMetadata			/* The ObjectMetadata associated with the object*/
        );

        return observer;
    }

    public static TransferObserver getRemoteFile( TransferUtility transferUtility,
                                                  String fileName,
                                                  String bucketName,
                                                  File file) {

        TransferObserver observer = transferUtility.download(bucketName, fileName, file);
        return observer;
    }

    public static InputStream getLocalFile( String fileName, Context context ) {
        try {
            return context.openFileInput(fileName);
        }
        catch ( FileNotFoundException exception ) {
            return null;
        }
    }

    public static Long timeNow (Boolean clockSetSuccess, Long sntpTime, Long sntpTimeReference){

        if (clockSetSuccess){
            Long now;
            now = sntpTime + SystemClock.elapsedRealtime() - sntpTimeReference;
            return now;
        }
        else
        {
            return System.currentTimeMillis();
        }

    }




}
