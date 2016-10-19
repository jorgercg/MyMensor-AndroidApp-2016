package com.seamensor.seamensor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;
import com.seamensor.seamensor.cognitoclient.AwsUtil;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class LoaderActivity extends Activity implements LocationListener
{
    private static final String TAG = "LoaderActvty";

	public final String vpsConfigFileLocal = "vps.xml";
    public final String vpsCheckedConfigFileDropbox = "vpschecked.xml";
    public final String trackingConfigFileName = "TrackingDataMarkerless.xml";
    public final String idMarkersTrackingConfigFileName = "TrckMarkers.xml";
	public String activityToBeCalled = null;
    public String descvpRemotePath;
    public String markervpRemotePath;
    public String vpsRemotePath;
    public String trackingRemotePath;
    public String vpsCheckedRemotePath;

    public BackgroundLoader backgroundLoader;

    public long sntpReference;
    public long sntpTime;

    public short qtyVps = 0;
    public short[] vpNumber;

    public String seamensorAccount = null;

    public boolean clockSetSuccess = false;

    private static long back_pressed;

    public LocationManager lm;

    public int dciNumber;

    AnimationDrawable seamensorLogoAnimation;
    ImageView seamensorLogo;
    LinearLayout logoLinearLayout;

    SharedPreferences sharedPref;

    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;

    @Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
        activityToBeCalled = getIntent().getExtras().get("activitytobecalled").toString();
        Log.d(TAG,"onCreate(): CALLED");
        // Enable metaio SDK debug log messages based on build configuration
        MetaioDebug.enableLogging(BuildConfig.DEBUG);

        sharedPref = this.getSharedPreferences("com.mymensor.app",Context.MODE_PRIVATE);

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        // Location Manager
        Log.d(TAG,"onCreate: Calling LocationManager");
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PERMISSION_GRANTED)
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);

        // Creating AsyncTask
        backgroundLoader = new BackgroundLoader();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_loader);
        logoLinearLayout = (LinearLayout)findViewById(R.id.SeaMensorLogoLinearLayout1);
        logoLinearLayout.setVisibility(View.VISIBLE);

        seamensorLogo = (ImageView) findViewById(R.id.seamensor_logo);
        seamensorLogoAnimation = (AnimationDrawable) seamensorLogo.getDrawable();
        seamensorLogo.setVisibility(View.VISIBLE);
        seamensorLogoAnimation.setVisible(true, true);
        seamensorLogoAnimation.start();

        // Retrieving SeaMensor Account information, if account does not exist then app is closed
        seamensorAccount = "magellan.victoria";
        Log.d(TAG,"OnCreate: Seamensor Account: "+seamensorAccount);

        backgroundLoader.execute();

    } // End onCreate

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

	@Override
	public void onStart()
	{
		super.onStart();
        Log.d(TAG,"onStart(): CALLED");
        seamensorLogoAnimation.setVisible(true, true);
        seamensorLogoAnimation.start();
	}


    @Override
    public void onResume()
    {
        super.onResume();


    }


    @Override
    public void onPause()
    {
        super.onPause();
        Log.d(TAG,"onPause(): CALLED");
        finish();
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG,"onDestroy(): CALLED");
        backgroundLoader.cancel(true);
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PERMISSION_GRANTED)
            lm.removeUpdates(this);
        Log.d(TAG,"onDestroy(): cancelled backgroundLoader = " + backgroundLoader.getStatus());
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


    public class BackgroundLoader extends AsyncTask<Void, String, Void> {
        @Override
        protected void onPreExecute() {
            Log.d(TAG, "backgroundLoader: onPreExecute()");
            clockSetSuccess = false;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            TextView message = (TextView) findViewById(R.id.bottom_message);
            message.setText(progress[0]);

        }

        @Override
        protected Void doInBackground(Void... params) {
            long now = 0;
            Long loopStart = System.currentTimeMillis();
            Log.d(TAG, "backgroundLoader: Calling SNTP");
            SntpClient sntpClient = new SntpClient();
            do {
                if (isCancelled()) {
                    Log.i("AsyncTask", "backgroundLoader: cancelled");
                    break;
                }
                if (sntpClient.requestTime("pool.ntp.org", 5000)) {
                    sntpTime = sntpClient.getNtpTime();
                    sntpReference = sntpClient.getNtpTimeReference();
                    now = sntpTime + SystemClock.elapsedRealtime() - sntpReference;
                    Log.i("SNTP", "SNTP Present Time =" + now);
                    Log.i("SNTP", "System Present Time =" + System.currentTimeMillis());
                    clockSetSuccess = true;
                }
                if (now != 0)
                    Log.d(TAG, "backgroundLoader: ntp:now=" + now);

            } while ((now == 0) && ((System.currentTimeMillis() - loopStart) < 10000));
            Log.d(TAG, "backgroundLoader: ending the loop querying pool.ntp.org for 10 seconds max:" + (System.currentTimeMillis() - loopStart) + " millis:" + now);
            if (clockSetSuccess) {
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() before setTime=" + System.currentTimeMillis());
                clockSetSuccess = true;
                Log.d(TAG, "backgroundLoader: System.currentTimeMillis() AFTER setTime=" + Utils.timeNow(clockSetSuccess, sntpTime, sntpReference));
            }

            /*
            *********************************************************************************************************************
             */
            Log.d(TAG, "loadConfiguration(): Loading Definitions from Dropbox and writing to local DCI storage");
            // Loading DCI Number from dciFileName.xml file

            dciNumber = 1;
            descvpRemotePath = seamensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/" + "dsc" + "/";
            markervpRemotePath = seamensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/" + "mrk" + "/";
            vpsRemotePath = seamensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "vps" + "/";
            trackingRemotePath = seamensorAccount + "/" + "cfg" + "/" + dciNumber + "/" + "trk" + "/";
            vpsCheckedRemotePath = seamensorAccount + "/" + "chk" + "/" + dciNumber + "/";

            File vpsFile = new File(getApplicationContext().getFilesDir(),vpsConfigFileLocal);

            if (Utils.isNewFileAvailable(  s3Client,
                    vpsConfigFileLocal,
                    (vpsRemotePath + vpsConfigFileLocal),
                    Constants.BUCKET_NAME,
                    getApplicationContext())) {
                Log.d(TAG,"vpsFile isNewFileAvailable= TRUE");
                TransferObserver observer = Utils.getRemoteFile(transferUtility, (vpsRemotePath + vpsConfigFileLocal), Constants.BUCKET_NAME, vpsFile);
                observer.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state.equals(TransferState.COMPLETED)) {
                            Log.d(TAG,"TransferListener="+state.toString());
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal>0){
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                        }

                        //Display percentage transfered to user
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e(TAG, "loadConfiguration(): vpsFile loading failed, see stack trace");
                        finish();
                    }

                });
            } else {
                Log.d(TAG,"vpsFile isNewFileAvailable= FALSE");

            }

            /*
            *********************************************************************************************************************
             */
            File idTrackingFile = new File(getApplicationContext().getFilesDir(),idMarkersTrackingConfigFileName);

            if (Utils.isNewFileAvailable(  s3Client,
                    idMarkersTrackingConfigFileName,
                    (trackingRemotePath + idMarkersTrackingConfigFileName),
                    Constants.BUCKET_NAME,
                    getApplicationContext())) {
                Log.d(TAG,"idTrackingFile isNewFileAvailable= TRUE");
                TransferObserver observer = Utils.getRemoteFile(transferUtility, (trackingRemotePath + idMarkersTrackingConfigFileName), Constants.BUCKET_NAME, idTrackingFile);
                observer.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state.equals(TransferState.COMPLETED)) {
                            Log.d(TAG,"TransferListener="+state.toString());
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal>0){
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                        }

                        //Display percentage transfered to user
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e(TAG, "loadConfiguration(): vpsFile loading failed, see stack trace");
                        finish();
                    }

                });
            } else {
                Log.d(TAG,"idTrackingFile isNewFileAvailable= FALSE");
            }
            /*
            *********************************************************************************************************************
             */

            File imageTrackingFile = new File(getApplicationContext().getFilesDir(),trackingConfigFileName);

            if (Utils.isNewFileAvailable(  s3Client,
                    trackingConfigFileName,
                    (trackingRemotePath + trackingConfigFileName),
                    Constants.BUCKET_NAME,
                    getApplicationContext())) {
                Log.d(TAG,"imageTrackingFile isNewFileAvailable= TRUE");
                TransferObserver observer = Utils.getRemoteFile(transferUtility, (trackingRemotePath + trackingConfigFileName), Constants.BUCKET_NAME, imageTrackingFile);
                observer.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state.equals(TransferState.COMPLETED)) {
                            Log.d(TAG,"TransferListener="+state.toString());
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal>0){
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                        }

                        //Display percentage transfered to user
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e(TAG, "loadConfiguration(): vpsFile loading failed, see stack trace");
                        finish();
                    }

                });
            } else {
                Log.d(TAG,"imageTrackingFile isNewFileAvailable= FALSE");
            }

           /*
            *********************************************************************************************************************
            */

            File vpsCheckedFile = new File(getApplicationContext().getFilesDir(),vpsCheckedConfigFileDropbox);

            if (Utils.isNewFileAvailable(  s3Client,
                    vpsCheckedConfigFileDropbox,
                    (vpsCheckedRemotePath + vpsCheckedConfigFileDropbox),
                    Constants.BUCKET_NAME,
                    getApplicationContext())) {
                Log.d(TAG,"vpsCheckedFile isNewFileAvailable= TRUE");
                TransferObserver observer = Utils.getRemoteFile(transferUtility, (vpsCheckedRemotePath + vpsCheckedConfigFileDropbox), Constants.BUCKET_NAME, vpsCheckedFile);
                observer.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        if (state.equals(TransferState.COMPLETED)) {
                            Log.d(TAG,"TransferListener="+state.toString());
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        if (bytesTotal>0){
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                        }

                        //Display percentage transfered to user
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Log.e(TAG, "loadConfiguration(): vpsFile loading failed, see stack trace");
                        finish();
                    }

                });
            } else {
                Log.d(TAG,"vpsCheckedFile isNewFileAvailable= FALSE");
            }
           /*
            *********************************************************************************************************************
            */

            Boolean configFilesOK = false;

            do {
                File vpsFileCHK = new File(getApplicationContext().getFilesDir(),vpsConfigFileLocal);
                File idTrackingFileCHK = new File(getApplicationContext().getFilesDir(),idMarkersTrackingConfigFileName);
                File imageTrackingFileCHK = new File(getApplicationContext().getFilesDir(),trackingConfigFileName);
                File vpsCheckedFileCHK = new File(getApplicationContext().getFilesDir(),vpsCheckedConfigFileDropbox);
                configFilesOK = ((vpsFileCHK.exists())&&(idTrackingFileCHK.exists())&&(imageTrackingFileCHK.exists())&&(vpsCheckedFileCHK.exists()));
            } while (configFilesOK==false);

            Log.d(TAG,"Loading Config Files: configFilesOK="+configFilesOK);

            publishProgress(getString(R.string.still_loading_assets));

            /*
            *********************************************************************************************************************
            */

            try
            {
                try
                {
                    Log.d(TAG,"loadQtyVpsFromVpsFile: File="+vpsConfigFileLocal);
                    InputStream fis = Utils.getLocalFile(vpsConfigFileLocal, getApplicationContext());
                    XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                    XmlPullParser myparser = xmlFactoryObject.newPullParser();
                    myparser.setInput(fis, null);
                    int eventType = myparser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT)
                    {
                        if(eventType == XmlPullParser.START_DOCUMENT)
                        {
                            //Log.d(TAG,"Start document");
                        }
                        else if(eventType == XmlPullParser.START_TAG)
                        {
                            //Log.d(TAG,"Start tag "+myparser.getName());
                            if(myparser.getName().equalsIgnoreCase("QtyVps"))
                            {
                                eventType = myparser.next();
                                qtyVps = Short.parseShort(myparser.getText());
                            }
                        }
                        else if(eventType == XmlPullParser.END_TAG)
                        {
                            //Log.d(TAG,"End tag "+myparser.getName());
                        }
                        else if(eventType == XmlPullParser.TEXT)
                        {
                            //Log.d(TAG,"Text "+myparser.getText());
                        }
                        eventType = myparser.next();
                    }
                    fis.close();
                }
                finally
                {
                    Log.d(TAG,"loadConfiguration(): QtyVps: "+qtyVps);
                    vpNumber = new short[qtyVps];
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, "loadConfiguration(): vpsFile loading failed, see stack trace");
            }

            /*
            *********************************************************************************************************************
            */

            try
            {
                // Loading Markers from Dropbox and writing to local storage.

                //message.setText(getString(R.string.waiting_to_load_assets));
                for (int i = 0; i < qtyVps; i++)
                {
                    Log.d(TAG,"loadFinalDefinitions: ####### LOADING: MARKERFILES CONTENTS i="+i);
                    //TextView message = (TextView) findViewById(R.id.bottom_message);
                    File markerFile = new File(getApplicationContext().getFilesDir(), "markervp" + (i + 1) + ".jpg");
                    Log.d(TAG,"loadFinalDefinitions: vpMarkerImageFilePath Dropbox: " + markervpRemotePath+ "markervp" + (i + 1) + ".jpg");
                    if (Utils.isNewFileAvailable(  s3Client,
                            "markervp" + (i + 1) + ".jpg",
                            (markervpRemotePath+ "markervp" + (i + 1) + ".jpg"),
                            Constants.BUCKET_NAME,
                            getApplicationContext())) {
                        Log.d(TAG,"markerFile loadFinalDefinitions: isNewFileAvailable= TRUE");
                        final TransferObserver observer = Utils.getRemoteFile(transferUtility, (markervpRemotePath+ "markervp" + (i + 1) + ".jpg"), Constants.BUCKET_NAME, markerFile);
                        observer.setTransferListener(new TransferListener() {

                            @Override
                            public void onStateChanged(int id, TransferState state) {
                                if (state.equals(TransferState.COMPLETED)) {
                                    Log.d(TAG,"loadFinalDefinitions: TransferListener Marker="+observer.getKey()+" State="+state.toString());
                                }
                            }

                            @Override
                            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                if (bytesTotal>0){
                                    int percentage = (int) (bytesCurrent / bytesTotal * 100);
                                }

                                //Display percentage transfered to user
                            }

                            @Override
                            public void onError(int id, Exception ex) {
                                Log.e(TAG, "loadFinalDefinitions: Marker loading failed, see stack trace");
                                finish();
                            }

                        });
                    } else {
                        Log.d(TAG,"markerFile loadFinalDefinitions: "+ "markervp" + (i + 1) + ".jpg" +" isNewFileAvailable= FALSE");
                    }
                }
                //message.setText(getString(R.string.still_loading_assets));


                // Loading Vp Location Description Images from Dropbox and writing to local storage.

                for (int j = 0; j < qtyVps; j++)
                {
                    Log.d(TAG,"loadFinalDefinitions:####### LOADING: VPDESCFILES CONTENTS j="+j);
                    File descvpFile = new File(getApplicationContext().getFilesDir(), "descvp" + (j + 1) + ".png");
                    Log.d(TAG,"loadFinalDefinitions: vpLocationDescImageFilePath Dropbox: " + descvpRemotePath+ "descvp" + (j + 1) + ".png");
                    if (Utils.isNewFileAvailable(  s3Client,
                            "descvp" + (j + 1) + ".png",
                            (markervpRemotePath+ "descvp" + (j + 1) + ".png"),
                            Constants.BUCKET_NAME,
                            getApplicationContext())) {
                        Log.d(TAG,"descvpFile loadFinalDefinitions: isNewFileAvailable= TRUE");
                        final TransferObserver observer = Utils.getRemoteFile(transferUtility, (descvpRemotePath+ "descvp" + (j + 1) + ".png"), Constants.BUCKET_NAME, descvpFile);
                        observer.setTransferListener(new TransferListener() {

                            @Override
                            public void onStateChanged(int id, TransferState state) {
                                if (state.equals(TransferState.COMPLETED)) {
                                    Log.d(TAG,"loadFinalDefinitions: TransferListener Descvp="+observer.getKey()+" State="+state.toString());
                                }
                            }

                            @Override
                            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                                if (bytesTotal>0){
                                    int percentage = (int) (bytesCurrent / bytesTotal * 100);

                                }

                                //Display percentage transfered to user
                            }

                            @Override
                            public void onError(int id, Exception ex) {
                                Log.e(TAG, "loadFinalDefinitions: Descvp loading failed, see stack trace");
                                finish();
                            }

                        });
                    } else {
                        Log.d(TAG,"descvpFile loadFinalDefinitions: "+ "descvp" + (j + 1) + ".jpg"+" isNewFileAvailable= FALSE");
                    }
                }
                //message.setText(getString(R.string.almost_finished_loading_assets));
                // Loading Metaio Assets
                try
                {
                    Log.d(TAG,"loadFinalDefinitions: backgroundLoader:####### LOADING: METAIO ASSETS");
                    AssetsManager.extractAllAssets(getApplicationContext(), true);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    Log.e(TAG, "loadFinalDefinitions:AssetsManager.extractAllAssets failed, see stack trace");
                }


            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.e(TAG, "loadFinalDefinitions:Error loadFinalDefinitions()");
            }

            int product = 0;

            do {
                for (int k = 0; k < qtyVps; k++){
                    try{
                        File descvpFileCHK = new File(getApplicationContext().getFilesDir(), "descvp" + (k + 1) + ".png");
                        //Log.d(TAG,"descvpFileCHK created ="+descvpFileCHK.getName());
                        File markerFileCHK = new File(getApplicationContext().getFilesDir(), "markervp" + (k + 1) + ".jpg");
                        //Log.d(TAG,"markerFileCHK created ="+markerFileCHK.getName());
                        if (descvpFileCHK.exists()) {  Log.d(TAG,"descvpFileCHK.exists()="+descvpFileCHK.getName()); }
                        if (markerFileCHK.exists()) {  Log.d(TAG,"markerFileCHK.exists()="+markerFileCHK.getName()); }
                        if ((descvpFileCHK.exists())&&(markerFileCHK.exists())) {
                            if (k == 0) {
                                product = Math.abs(1);
                            } else {
                                product *= Math.abs(1);
                            }
                        } else {
                            if (k == 0) {
                                product = Math.abs(0);
                            } else {
                                product *= Math.abs(0);
                            }
                        }
                    } catch (Exception e) {
                        Log.e (TAG, "Image Files Checking Failed:"+e.toString());
                    }

                }

            } while (product==0);
            Log.d(TAG,"FINALLY!!!!!!! Loading Config Files: imageFilesOK="+product);
            publishProgress(getString(R.string.load_assets_finished));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            callingActivities();
        }
    }


    public void callingActivities(){

        Log.d(TAG,"callingActivities");
        Log.d(TAG,"callingActivities:####### LOADING: onPostExecute: callingARVewactivity: clockSetSuccess=" + clockSetSuccess);
        Log.d(TAG,"callingActivities:####### LOADING: onPostExecute: callingARVewactivity: activityToBeCalled="+activityToBeCalled);
        TextView message = (TextView) findViewById(R.id.bottom_message);
        if (activityToBeCalled.equalsIgnoreCase("SMC"))
        {
            try
            {
                Intent intent = new Intent(getApplicationContext(), ConfigActivity.class);
                intent.putExtra("seamensoraccount", seamensorAccount);
                intent.putExtra("dcinumber", dciNumber);
                intent.putExtra("QtyVps", qtyVps);
                intent.putExtra("sntpTime", sntpTime);
                intent.putExtra("sntpReference", sntpReference);
                intent.putExtra("clockSetSuccess", clockSetSuccess);
                startActivity(intent);
            }
            catch (Exception e)
            {
                Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            }
            finally
            {
                finish();
            }
        }
        if (activityToBeCalled.equalsIgnoreCase("SeaMensor"))
        {
            try
            {
                Intent intent = new Intent(getApplicationContext(), ImageCapActivity.class);
                intent.putExtra("seamensoraccount", seamensorAccount);
                intent.putExtra("dcinumber", dciNumber);
                intent.putExtra("QtyVps", qtyVps);
                intent.putExtra("sntpTime", sntpTime);
                intent.putExtra("sntpReference", sntpReference);
                intent.putExtra("clockSetSuccess", clockSetSuccess);
                startActivity(intent);
            }
            catch (Exception e)
            {
                Toast toast = Toast.makeText(getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER | Gravity.CENTER_HORIZONTAL, 0, 30);
                toast.show();
            }
            finally
            {
                finish();
            }
        }

    }


    public class SntpClient
    {
        private static final String TAG = "SntpClient";

        private static final int REFERENCE_TIME_OFFSET = 16;
        private static final int ORIGINATE_TIME_OFFSET = 24;
        private static final int RECEIVE_TIME_OFFSET = 32;
        private static final int TRANSMIT_TIME_OFFSET = 40;
        private static final int NTP_PACKET_SIZE = 48;

        private static final int NTP_PORT = 123;
        private static final int NTP_MODE_CLIENT = 3;
        private static final int NTP_VERSION = 3;

        // Number of seconds between Jan 1, 1900 and Jan 1, 1970
        // 70 years plus 17 leap days
        private static final long OFFSET_1900_TO_1970 = ((365L * 70L) + 17L) * 24L * 60L * 60L;

        // system time computed from NTP server response
        private long mNtpTime;

        // value of SystemClock.elapsedRealtime() corresponding to mNtpTime
        private long mNtpTimeReference;

        // round trip time in milliseconds
        private long mRoundTripTime;

        /**
         * Sends an SNTP request to the given host and processes the response.
         *
         * @param host host name of the server.
         * @param timeout network timeout in milliseconds.
         * @return true if the transaction was successful.
         */
        public boolean requestTime(String host, int timeout) {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setSoTimeout(timeout);
                InetAddress address = InetAddress.getByName(host);
                byte[] buffer = new byte[NTP_PACKET_SIZE];
                DatagramPacket request = new DatagramPacket(buffer, buffer.length, address, NTP_PORT);

                // set mode = 3 (client) and version = 3
                // mode is in low 3 bits of first byte
                // version is in bits 3-5 of first byte
                buffer[0] = NTP_MODE_CLIENT | (NTP_VERSION << 3);

                // get current time and write it to the request packet
                long requestTime = System.currentTimeMillis();
                long requestTicks = SystemClock.elapsedRealtime();
                writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime);

                socket.send(request);

                // read the response
                DatagramPacket response = new DatagramPacket(buffer, buffer.length);
                socket.receive(response);
                long responseTicks = SystemClock.elapsedRealtime();
                long responseTime = requestTime + (responseTicks - requestTicks);

                // extract the results
                long originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET);
                long receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET);
                long transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET);
                long roundTripTime = responseTicks - requestTicks - (transmitTime - receiveTime);
                // receiveTime = originateTime + transit + skew
                // responseTime = transmitTime + transit - skew
                // clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime))/2
                //             = ((originateTime + transit + skew - originateTime) +
                //                (transmitTime - (transmitTime + transit - skew)))/2
                //             = ((transit + skew) + (transmitTime - transmitTime - transit + skew))/2
                //             = (transit + skew - transit + skew)/2
                //             = (2 * skew)/2 = skew
                long clockOffset = ((receiveTime - originateTime) + (transmitTime - responseTime))/2;
                // if (false) Log.d(TAG, "round trip: " + roundTripTime + " ms");
                // if (false) Log.d(TAG, "clock offset: " + clockOffset + " ms");

                // save our results - use the times on this side of the network latency
                // (response rather than request time)
                mNtpTime = responseTime + clockOffset;
                mNtpTimeReference = responseTicks;
                mRoundTripTime = roundTripTime;
            } catch (Exception e) {
                if (false) Log.d(TAG, "request time failed: " + e);
                return false;
            } finally {
                if (socket != null) {
                    socket.close();
                }
            }

            return true;
        }

        /**
         * Returns the time computed from the NTP transaction.
         *
         * @return time value computed from NTP server response.
         */
        public long getNtpTime() {
            return mNtpTime;
        }

        /**
         * Returns the reference clock value (value of SystemClock.elapsedRealtime())
         * corresponding to the NTP time.
         *
         * @return reference clock corresponding to the NTP time.
         */
        public long getNtpTimeReference() {
            return mNtpTimeReference;
        }

        /**
         * Returns the round trip time of the NTP transaction
         *
         * @return round trip time in milliseconds.
         */
        public long getRoundTripTime() {
            return mRoundTripTime;
        }

        /**
         * Reads an unsigned 32 bit big endian number from the given offset in the buffer.
         */
        private long read32(byte[] buffer, int offset) {
            byte b0 = buffer[offset];
            byte b1 = buffer[offset+1];
            byte b2 = buffer[offset+2];
            byte b3 = buffer[offset+3];

            // convert signed bytes to unsigned values
            int i0 = ((b0 & 0x80) == 0x80 ? (b0 & 0x7F) + 0x80 : b0);
            int i1 = ((b1 & 0x80) == 0x80 ? (b1 & 0x7F) + 0x80 : b1);
            int i2 = ((b2 & 0x80) == 0x80 ? (b2 & 0x7F) + 0x80 : b2);
            int i3 = ((b3 & 0x80) == 0x80 ? (b3 & 0x7F) + 0x80 : b3);

            return ((long)i0 << 24) + ((long)i1 << 16) + ((long)i2 << 8) + (long)i3;
        }

        /**
         * Reads the NTP time stamp at the given offset in the buffer and returns
         * it as a system time (milliseconds since January 1, 1970).
         */
        private long readTimeStamp(byte[] buffer, int offset) {
            long seconds = read32(buffer, offset);
            long fraction = read32(buffer, offset + 4);
            return ((seconds - OFFSET_1900_TO_1970) * 1000) + ((fraction * 1000L) / 0x100000000L);
        }

        /**
         * Writes system time (milliseconds since January 1, 1970) as an NTP time stamp
         * at the given offset in the buffer.
         */
        private void writeTimeStamp(byte[] buffer, int offset, long time) {
            long seconds = time / 1000L;
            long milliseconds = time - seconds * 1000L;
            seconds += OFFSET_1900_TO_1970;

            // write seconds in big endian format
            buffer[offset++] = (byte)(seconds >> 24);
            buffer[offset++] = (byte)(seconds >> 16);
            buffer[offset++] = (byte)(seconds >> 8);
            buffer[offset++] = (byte)(seconds >> 0);

            long fraction = milliseconds * 0x100000000L / 1000L;
            // write fraction in big endian format
            buffer[offset++] = (byte)(fraction >> 24);
            buffer[offset++] = (byte)(fraction >> 16);
            buffer[offset++] = (byte)(fraction >> 8);
            // low order bits should be random data
            buffer[offset++] = (byte)(Math.random() * 255.0);
        }
    }
}



