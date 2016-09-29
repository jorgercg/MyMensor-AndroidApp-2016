package com.seamensor.seamensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.text.InputFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileStatus;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.google.android.gms.common.AccountPicker;
import com.metaio.sdk.MetaioDebug;
import com.metaio.tools.io.AssetsManager;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends Activity implements LocationListener
{
	public static final String appKey = "0yrlhapf89ytpwi";
	public static final String appSecret = "neg16q87i8rpym3";
	public static final int REQUEST_LINK_TO_DBX = 0;
	public DbxAccountManager mDbxAcctMgr;
    public DbxFileSystem dbxFs;
	public final String vpsConfigFileDropbox = "vps.xml";
    public final String userDataConfigFileDropbox = "userdata.xml";
	public String activityToBeCalled = null;
    public String currentMillisString = null;
    public String descvpDropboxPath;
    public String markervpDropboxPath;
    public String vpsDropboxPath;
    public String trackingDropboxPath;
    public String vpsCheckedDropboxPath;
    public static Bitmap vpLocationDescImageFileContents;
    public static Bitmap vpMarkerImageFileContents;

    public LoadDefinitionsBeforeCallingActivity loadDefinitionsBeforeCallingActivity;

    public long[] vpConfiguredTimeSMCMillis;
    public long[] vpConfiguredTimeInUseMillis;
    public final String vpsConfiguredConfigFileSMCDropbox = "vpsconfiguredSMC.xml";
    public final String vpsConfiguredConfigFileInUseDropbox = "vpsconfigured.xml";
    public boolean reloadConfig = true; //false;

    public short qtyVps = 0;
    public short[] vpNumber;

    //Variable to store phone wifi MAC address
    public String dciWifiMAC;
    public String dciFileName;

    //Initialize the Users configuration
    public String[] userNumber = new String[20];
    public String[] userName = new String[20];
    public String[] userActivity = new String[20];

    public String seamensorAccount = null;
	
	public boolean notSeamensorAccount = false;
    public boolean seamensorAdminPresent = false;
    public boolean dataLoadSuccess = false;
    public boolean clockSetSuccess = false;

	public final String seamensorDomain = "@seamensor.com";
    public final String adminOne = "seamensor01";
    public final String adminTwo = "seamensor02";
    public final String adminThree = "seamensor03";
    public final String adminFour = "seamensor04";
    public final String adminFive = "seamensor05";

    public String gpsUTCTime = null;
    public String gpsUTCddmmyy = null;
    public long systemMillisWhenGPSTimereceived = 0;

    private static long back_pressed;

    public LocationManager lm;

    public int dciNumber;
    public int userCounter = -1;
    public int userLoggedCounter = 0;

    protected EditText pinCodeField1 = null;
    protected EditText pinCodeField2 = null;
    protected EditText pinCodeField3 = null;
    protected EditText pinCodeField4 = null;
    protected EditText pinCodeField5 = null;
    protected EditText pinCodeField6 = null;
    protected InputFilter[] filters = null;
    protected TextView topMessage = null;

    AnimationDrawable seamensorLogoAnimation;
    ImageView seamensorLogo;
    LinearLayout logoLinearLayout;
    LinearLayout keyPadLinearLayout;


    @Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
        MetaioDebug.log("onCreate(): CALLED");
        // Enable metaio SDK debug log messages based on build configuration
        MetaioDebug.enableLogging(BuildConfig.DEBUG);

        // Location Manager
        MetaioDebug.log("onCreate: Calling LocationManager");
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PERMISSION_GRANTED)
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,0,this);

        // Creating AsyncTask
        loadDefinitionsBeforeCallingActivity = new LoadDefinitionsBeforeCallingActivity();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.main);
        logoLinearLayout = (LinearLayout)findViewById(R.id.SeaMensorLogoLinearLayout1);
        logoLinearLayout.setVisibility(View.VISIBLE);
        keyPadLinearLayout = (LinearLayout)findViewById(R.id.AppUnlockLinearLayout1);
        keyPadLinearLayout.setVisibility((View.GONE));

        seamensorLogo = (ImageView) findViewById(R.id.seamensor_logo);
        seamensorLogoAnimation = (AnimationDrawable) seamensorLogo.getDrawable();
        seamensorLogo.setVisibility(View.VISIBLE);
        seamensorLogoAnimation.setVisible(true, true);
        seamensorLogoAnimation.start();

        // Retrieving SeaMensor Account information, if account does not exist then app is closed
		try
		{
            MetaioDebug.log("OnCreate: READING ACCOUNTS INFORMATION");
            AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
			Account[] list = manager.getAccounts();
			for(Account account: list)
			{
				MetaioDebug.log("OnCreate: Account: Name="+account.name+"Type="+account.type);
				if (account.type.equalsIgnoreCase("com.google"))
				{
					if (account.name.endsWith(seamensorDomain))	seamensorAccount = account.name;
                    if (account.name.startsWith(adminOne)) seamensorAdminPresent = true;
                    if (account.name.startsWith(adminTwo)) seamensorAdminPresent = true;
                    if (account.name.startsWith(adminThree)) seamensorAdminPresent = true;
                    if (account.name.startsWith(adminFour)) seamensorAdminPresent = true;
                    if (account.name.startsWith(adminFive)) seamensorAdminPresent = true;
                }
			}
			if (seamensorAccount!=null)
            {
                seamensorAccount = seamensorAccount.replace(seamensorDomain, "");
                MetaioDebug.log("OnCreate: Seamensor Account: "+seamensorAccount);
            }
            else
            {
                notSeamensorAccount = true;
            }
		}
		catch (Exception e)
		{
			e.printStackTrace();
			notSeamensorAccount = true;
			MetaioDebug.log(Log.ERROR, "OnCreate: Seamensor user not present in this device");
		}

		// Enable Dropbox
        try
        {
            MetaioDebug.log("OnCreate: STARTING DROPBOX");
            mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), appKey, appSecret);
            if (!mDbxAcctMgr.hasLinkedAccount())
            {
                MetaioDebug.log("OnCreate: If not already Linked to DROPBOX, request the connection");
                mDbxAcctMgr.startLink((Activity) this, REQUEST_LINK_TO_DBX);
            }
            else
            {
                MetaioDebug.log("OnCreate: Already Linked to DROPBOX: seamensorAdminPresent:"+seamensorAdminPresent);
                try
                {
                    dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                }
                catch (DbxException.Unauthorized unauthorized)
                {
                    unauthorized.printStackTrace();
                }
                /*
                if (!seamensorAdminPresent)
                {
                    MetaioDebug.log("OnCreate: Already Linked to DROPBOX - calling loadConfiguration()");
                    loadConfiguration();
                    MetaioDebug.log("OnCreate: Already Linked to DROPBOX - calling loadDefinitionsFromDropboxBeforeCallingSeamensor()");
                    loadDefinitionsFromDropboxBeforeCallingSeamensor();
                }
                else
                {
                    loadConfiguration();
                }
                */
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "OnCreate: Error requesting connecton to Dropbox - not linked");
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.dropboxlinkerror), Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 30);
            toast.show();
            finish();
        }

        loadConfiguration();

        activityToBeCalled = "SeaMensor"; //activityToBeCalled = "SMC";

        loadDefinitionsBeforeCallingActivity.execute();

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

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

                if (requestCode == REQUEST_LINK_TO_DBX)
                {
                    if (resultCode == Activity.RESULT_OK)
                    {
                        try
                        {
                            dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                        }
                        catch (DbxException.Unauthorized unauthorized)
                        {
                            unauthorized.printStackTrace();
                        }
                    }
                    else
                    {
                        MetaioDebug.log("onActivityResult: Link to DROPBOX FAILED");
                        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.dropboxlinkerror), Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 30);
                        toast.show();
                        finish();
                    }
                }


    }

		
	@Override
	public void onStart()
	{
		super.onStart();
        MetaioDebug.log("onStart(): CALLED");
        if (notSeamensorAccount)
		{
			showNotSeamensorAccountError();
			finish();
		}
        if ((!notSeamensorAccount)&&(!seamensorAdminPresent))
        {
            seamensorLogoAnimation.setVisible(true, true);
            seamensorLogoAnimation.start();
            if (loadDefinitionsBeforeCallingActivity.getStatus()!= AsyncTask.Status.RUNNING) loadDefinitionsBeforeCallingActivity.execute();
        }
	}


    @Override
    public void onResume()
    {
        super.onResume();

        AccountManager accountManager = AccountManager.get(this);
        accountManager.addAccount(Constants.ACCOUNT_TYPE, null, null, null, this, null, null);
    }


    @Override
    public void onPause()
    {
        super.onPause();
        MetaioDebug.log("onPause(): CALLED");
        finish();
    }

    /*
    @Override
    public void onResume()
    {
        super.onResume();
        MetaioDebug.log("onResume(): CALLED");
        if (loadDefinitionsBeforeCallingActivity.getStatus() != AsyncTask.Status.RUNNING)
        {
            MetaioDebug.log("onResume(): will call loadDefinitionsBeforeCallingActivity = " + loadDefinitionsBeforeCallingActivity.getStatus());
            new LoadDefinitionsBeforeCallingActivity().execute();
            MetaioDebug.log("onResume(): called loadDefinitionsBeforeCallingActivity = " + loadDefinitionsBeforeCallingActivity.getStatus());
        }
        else MetaioDebug.log("onResume(): loadDefinitionsBeforeCallingActivity STILL RUNNING= " + loadDefinitionsBeforeCallingActivity.getStatus());
    }
*/

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        MetaioDebug.log("onDestroy(): CALLED");
        loadDefinitionsBeforeCallingActivity.cancel(true);
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)==PERMISSION_GRANTED)
            lm.removeUpdates(this);
        MetaioDebug.log("onDestroy(): cancelled loadDefinitionsBeforeCallingActivity = " + loadDefinitionsBeforeCallingActivity.getStatus());
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

	protected void showNotSeamensorAccountError()
    {
        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_seamensor_account), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 30);
        toast.show();
    }

    protected void showNoWiFiError()
    {
        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_wifi), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 30);
        toast.show();
    }

    protected void showNoInternetTimeError()
    {
        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_inttime), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 30);
        toast.show();
    }

    public void defineVpsConfiguredFileInUse(String oldFileInUse, String newFileInUse)
    {
        String vpsConfiguredConfigNewFileContents = null;
        try
        {
            MetaioDebug.log("defineVpsConfiguredFileInUse: Reading New File:"+newFileInUse);
            //dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            DbxPath vpsConfiguredConfigNewFilePath = new DbxPath(DbxPath.ROOT,newFileInUse);
            DbxFile vpsConfiguredConfigNewFile = dbxFs.open(vpsConfiguredConfigNewFilePath);
            vpsConfiguredConfigNewFileContents=vpsConfiguredConfigNewFile.readString();
            vpsConfiguredConfigNewFile.close();
        }
        catch (Exception e)
        {
            MetaioDebug.log("defineVpsConfiguredFileInUse: Error reading New File:"+newFileInUse);
        }
        if (!(vpsConfiguredConfigNewFileContents==null))
            try
            {
                MetaioDebug.log("defineVpsConfiguredFileInUse: Overwriting Old File:"+oldFileInUse);
                //dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                DbxPath vpsConfiguredConfigOldFilePath = new DbxPath(DbxPath.ROOT,oldFileInUse);
                if (dbxFs.exists(vpsConfiguredConfigOldFilePath))
                {
                    DbxFile vpsConfiguredConfigOldFile = dbxFs.open(vpsConfiguredConfigOldFilePath);
                    vpsConfiguredConfigOldFile.writeString(vpsConfiguredConfigNewFileContents);
                    vpsConfiguredConfigOldFile.close();
                }
                else
                {
                    MetaioDebug.log("defineVpsConfiguredFileInUse: Old File DOES NOT EXIST!!!!:"+oldFileInUse);
                    dbxFs.create(vpsConfiguredConfigOldFilePath);
                    DbxFile vpsConfiguredConfigOldFile = dbxFs.open(vpsConfiguredConfigOldFilePath);
                    vpsConfiguredConfigOldFile.writeString(vpsConfiguredConfigNewFileContents);
                    vpsConfiguredConfigOldFile.close();
                }
            }
            catch (Exception e)
            {
                MetaioDebug.log("defineVpsConfiguredFileInUse: Error overwriting Old File:"+oldFileInUse);
            }
    }


    public short loadQtyVpsConfigured(String FileDropbox)
    {
        MetaioDebug.log("loadQtyVpsConfigured(): started");
        short qtyVpsConfigured = 0;
        short temp = 0;
        try
        {
            DbxPath vpsConfiguredConfigFilePath = new DbxPath(DbxPath.ROOT,FileDropbox);
            MetaioDebug.log("loadQtyVpsConfigured(): Vps Config Dropbox path = "+vpsConfiguredConfigFilePath);
            if (dbxFs.exists(vpsConfiguredConfigFilePath))
            {
                DbxFile vpsConfiguredConfigFile = dbxFs.open(vpsConfiguredConfigFilePath);
                FileInputStream fis = vpsConfiguredConfigFile.getReadStream();
                XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser myparser = xmlFactoryObject.newPullParser();
                myparser.setInput(fis, null);
                int eventType = myparser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                    } else if (eventType == XmlPullParser.START_TAG) {
                        if (myparser.getName().equalsIgnoreCase("Vp")) {
                            qtyVpsConfigured++;
                            MetaioDebug.log("loadQtyVpsConfigured() internal: qtyVpsConfigured = " + qtyVpsConfigured);
                        } else if (myparser.getName().equalsIgnoreCase("VpNumber")) {
                            eventType = myparser.next();
                            temp = Short.parseShort(myparser.getText());
                        } else if (myparser.getName().equalsIgnoreCase("Configured")) {
                            eventType = myparser.next();
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                    } else if (eventType == XmlPullParser.TEXT) {
                    }
                    eventType = myparser.next();
                }
                vpsConfiguredConfigFile.close();
                MetaioDebug.log("loadQtyVpsConfigured():Vps Configured config DROPBOX file = " + vpsConfiguredConfigFile);
                MetaioDebug.log("loadQtyVpsConfigured(): qtyVpsConfigured = " + qtyVpsConfigured);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "loadQtyVpsConfigured():Configured Vps data loading failed, see stack trace:"+FileDropbox);
        }
        return qtyVpsConfigured;
    }


    public long[] loadVpsConfigured(String FileDropbox)
    {
        long[] vpConfiguredTime = new long[qtyVps];
        MetaioDebug.log("loadVpsConfigured(): started");
        int vpListOrder = 0;
        try
        {
            // Getting a file path for vps configured config XML file
            //dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            DbxPath vpsConfiguredConfigFilePath = new DbxPath(DbxPath.ROOT,FileDropbox);
            MetaioDebug.log("loadVpsConfigured(): Vps Config Dropbox path = "+vpsConfiguredConfigFilePath);
            if (dbxFs.exists(vpsConfiguredConfigFilePath))
            {
                DbxFile vpsConfiguredConfigFile = dbxFs.open(vpsConfiguredConfigFilePath);
                FileInputStream fis = vpsConfiguredConfigFile.getReadStream();
                XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser myparser = xmlFactoryObject.newPullParser();
                myparser.setInput(fis, null);
                int eventType = myparser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_DOCUMENT) {
                        //MetaioDebug.log("loadVpsConfigured(): Start document");
                    } else if (eventType == XmlPullParser.START_TAG) {
                        //MetaioDebug.log("loadVpsConfigured(): Start tag "+myparser.getName());

                        if (myparser.getName().equalsIgnoreCase("Vp")) {
                            vpListOrder++;
                            //MetaioDebug.log("loadVpsConfigured(): VpListOrder: "+vpListOrder);
                        } else if (myparser.getName().equalsIgnoreCase("VpNumber")) {
                            eventType = myparser.next();
                            vpNumber[vpListOrder - 1] = Short.parseShort(myparser.getText());
                            //MetaioDebug.log("loadVpsConfigured(): VpNumber"+(vpListOrder-1)+": "+vpNumber[vpListOrder-1]);
                        } else if (myparser.getName().equalsIgnoreCase("Configured")) {
                            eventType = myparser.next();
                            vpConfiguredTime[vpListOrder - 1] = Long.parseLong(myparser.getText());
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        //MetaioDebug.log("loadVpsConfigured(): End tag "+myparser.getName());
                    } else if (eventType == XmlPullParser.TEXT) {
                        //MetaioDebug.log("loadVpsConfigured(): Text "+myparser.getText());
                    }
                    eventType = myparser.next();
                }
                vpsConfiguredConfigFile.close();
                MetaioDebug.log("Vps Configured config DROPBOX file = " + vpsConfiguredConfigFile);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "Configured Vps data loading failed, see stack trace:"+FileDropbox);
        }
        return vpConfiguredTime;
    }


    public void loadConfiguration()
    {
        MetaioDebug.log("loadConfiguration(): Loading Definitions from Dropbox and writing to local DCI storage");
        // Loading DCI Number from dciFileName.xml file
        try
        {
            dciNumber = 1;
            descvpDropboxPath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"dsc"+"/"+"descvp";
            markervpDropboxPath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"mrk"+"/"+"markervp";
            vpsDropboxPath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/";
            trackingDropboxPath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"trk"+"/";
            vpsCheckedDropboxPath = seamensorAccount+"/"+"chk"+"/"+dciNumber+"/";


            try
            {
                // Getting a file path for userData configuration XML file
                //dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                DbxPath userDataConfigFilePath = new DbxPath(DbxPath.ROOT,""+seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"usr"+"/"+userDataConfigFileDropbox);
                MetaioDebug.log("loadConfiguration(): userData Config Dropbox path = "+userDataConfigFilePath);
                DbxFile userDataConfigFile = dbxFs.open(userDataConfigFilePath);
                try
                {
                    FileInputStream fis = userDataConfigFile.getReadStream();
                    XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                    XmlPullParser myparser = xmlFactoryObject.newPullParser();
                    myparser.setInput(fis, null);
                    int eventType = myparser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT)
                    {
                        if(eventType == XmlPullParser.START_DOCUMENT)
                        {
                            //MetaioDebug.log("Start document");
                        }
                        else if(eventType == XmlPullParser.START_TAG)
                        {
                            //MetaioDebug.log("Start tag "+myparser.getName());
                            if(myparser.getName().equalsIgnoreCase("User"))
                            {
                                userCounter++;
                                //MetaioDebug.log("User Counter: "+userCounter);
                            }
                            else if(myparser.getName().equalsIgnoreCase("UserNumber"))
                            {
                                eventType = myparser.next();
                                userNumber[userCounter]=myparser.getText();
                            }
                            else if(myparser.getName().equalsIgnoreCase("UserName"))
                            {
                                eventType = myparser.next();
                                userName[userCounter]=myparser.getText();
                            }
                            else if(myparser.getName().equalsIgnoreCase("UserActivity"))
                            {
                                eventType = myparser.next();
                                userActivity[userCounter]=myparser.getText();
                            }
                        }
                        else if(eventType == XmlPullParser.END_TAG)
                        {
                            //MetaioDebug.log("End tag "+myparser.getName());
                        }
                        else if(eventType == XmlPullParser.TEXT)
                        {
                            //MetaioDebug.log("Text "+myparser.getText());
                        }
                        eventType = myparser.next();
                    }
                }
                finally
                {
                    userDataConfigFile.close();
                    MetaioDebug.log("loadConfiguration(): Users: "+(userCounter+1));
                    MetaioDebug.log("loadConfiguration(): UserNumbers: "+userNumber[0]+" "+userNumber[1]);
                    MetaioDebug.log("loadConfiguration(): UserNames: "+userName[0]+" "+userName[1]);
                    MetaioDebug.log("loadConfiguration(): UserActivities: "+userActivity[0]+" "+userActivity[1]);
                }

                MetaioDebug.log("loadConfiguration(): userData Config DROPBOX file = "+userDataConfigFile);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                MetaioDebug.log(Log.ERROR, "loadConfiguration(): userData loading failed, see stack trace");
            }


            // Loading qtyVps from vps.xml file
            try
            {
                //dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                DbxPath vpsFilePath = new DbxPath(DbxPath.ROOT,vpsDropboxPath+vpsConfigFileDropbox);
                MetaioDebug.log("loadConfiguration(): ####### LOADING: VPSFILE CONTENTS");
                MetaioDebug.log("loadConfiguration(): vps Dropbox path = "+vpsFilePath);
                DbxFile vpsFile = dbxFs.open(vpsFilePath);
                DbxFileStatus vpsFileStatus = vpsFile.getSyncStatus();
                try
                {
                    if (!vpsFileStatus.isLatest) vpsFile.update();
                    FileInputStream fis = vpsFile.getReadStream();
                    XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                    XmlPullParser myparser = xmlFactoryObject.newPullParser();
                    myparser.setInput(fis, null);
                    int eventType = myparser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT)
                    {
                        if(eventType == XmlPullParser.START_DOCUMENT)
                        {
                            //MetaioDebug.log("Start document");
                        }
                        else if(eventType == XmlPullParser.START_TAG)
                        {
                            //MetaioDebug.log("Start tag "+myparser.getName());
                            if(myparser.getName().equalsIgnoreCase("QtyVps"))
                            {
                                eventType = myparser.next();
                                qtyVps = Short.parseShort(myparser.getText());
                            }
                        }
                        else if(eventType == XmlPullParser.END_TAG)
                        {
                            //MetaioDebug.log("End tag "+myparser.getName());
                        }
                        else if(eventType == XmlPullParser.TEXT)
                        {
                            //MetaioDebug.log("Text "+myparser.getText());
                        }
                        eventType = myparser.next();
                    }
                }
                finally
                {
                    vpsFile.close();
                    MetaioDebug.log("loadConfiguration(): QtyVps: "+qtyVps);
                    vpNumber = new short[qtyVps];
                    vpConfiguredTimeSMCMillis = new long[qtyVps];
                    vpConfiguredTimeInUseMillis = new long[qtyVps];
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                MetaioDebug.log(Log.ERROR, "loadConfiguration(): vpsFile loading failed, see stack trace");
            }



            short qtyVpsSMC = loadQtyVpsConfigured(vpsCheckedDropboxPath+vpsConfiguredConfigFileSMCDropbox);
            short qtyVpsSeaMensor = loadQtyVpsConfigured(vpsCheckedDropboxPath+vpsConfiguredConfigFileInUseDropbox);

            if (qtyVpsSMC==qtyVpsSeaMensor)
            {
                vpConfiguredTimeSMCMillis = loadVpsConfigured(vpsCheckedDropboxPath+vpsConfiguredConfigFileSMCDropbox);
                vpConfiguredTimeInUseMillis = loadVpsConfigured(vpsCheckedDropboxPath+vpsConfiguredConfigFileInUseDropbox);
                defineVpsConfiguredFileInUse(vpsCheckedDropboxPath+vpsConfiguredConfigFileInUseDropbox,vpsCheckedDropboxPath+vpsConfiguredConfigFileSMCDropbox);
            }
            else
            {
                defineVpsConfiguredFileInUse(vpsCheckedDropboxPath+vpsConfiguredConfigFileInUseDropbox,vpsCheckedDropboxPath+vpsConfiguredConfigFileSMCDropbox);
                vpConfiguredTimeSMCMillis = loadVpsConfigured(vpsCheckedDropboxPath+vpsConfiguredConfigFileSMCDropbox);
                vpConfiguredTimeInUseMillis = loadVpsConfigured(vpsCheckedDropboxPath+vpsConfiguredConfigFileInUseDropbox);
                reloadConfig = true;
            }



        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "loadConfiguration(): Error loadConfiguration()");
        }

    }



    public class LoadDefinitionsBeforeCallingActivity extends AsyncTask<Void, String, Void>
    {
            @Override
            protected void onPreExecute()
            {
                MetaioDebug.log("loadDefinitionsBeforeCallingActivity: onPreExecute()");
                clockSetSuccess = false;
            }

            @Override
            protected void onProgressUpdate(String... progress)
            {
                TextView message = (TextView) findViewById(R.id.bottom_message);
                message.setText(progress[0]);
            }

            @Override
            protected Void doInBackground(Void... params)
            {
                /*
                //  Trying to set SystemClock with info from currentmillis.com
                //
                //
                */
                MetaioDebug.log("loadDefinitionsBeforeCallingActivity: doInBackground()");
                MetaioDebug.log("loadDefinitionsBeforeCallingActivity: checking if internet is available");
                boolean isInternetAvailable = false;
                boolean isCurrentMillisComAvailable = false;
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null)
                {
                    try {
                        HttpURLConnection urlc = (HttpURLConnection) (new URL("http://www.google.com").openConnection());
                        urlc.setRequestProperty("User-Agent", "Test");
                        urlc.setRequestProperty("Connection", "close");
                        urlc.setConnectTimeout(5000);
                        urlc.connect();
                        if (urlc.getResponseCode() == 200) isInternetAvailable=true;
                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity: isInternetAvailable="+isInternetAvailable);
                    } catch (IOException e) {
                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity: Error checking internet connection:"+e.getLocalizedMessage());
                    }
                }
                else
                {
                    MetaioDebug.log("loadDefinitionsBeforeCallingActivity: No network available!");
                }
                long now = 0;
                /*
                if (isInternetAvailable)
                {
                    try {
                        HttpURLConnection urlc = (HttpURLConnection) (new URL("http://currentmillis.com/api/millis-since-unix-epoch.php").openConnection());
                        urlc.setConnectTimeout(5000);
                        urlc.connect();
                        if (urlc.getResponseCode() == 200) isCurrentMillisComAvailable = true;
                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity: isCurrentMillisComAvailable=" + isCurrentMillisComAvailable);
                    } catch (IOException e) {
                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity: Error checking isCurrentMillisComAvailable connection:" + e.getLocalizedMessage());
                    }
                }
                else
                {
                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity: No isCurrentMillisComAvailable available!");
                }
                */

                if (isInternetAvailable)
                {
                    Long loopStart = System.currentTimeMillis();
                    MetaioDebug.log("loadDefinitionsBeforeCallingActivity: Calling SNTP");
                    SntpClient sntpClient = new SntpClient();
                    do
                    {
                        if (isCancelled())
                        {
                            Log.i("AsyncTask", "loadDefinitionsBeforeCallingActivity: cancelled");
                            break;
                        }
                        if (sntpClient.requestTime("pool.ntp.org", 5000))
                        {
                            now = sntpClient.getNtpTime() + SystemClock.elapsedRealtime() - sntpClient.getNtpTimeReference();
                            Log.i("SNTP","SNTP Present Time ="+now);
                            Log.i("SNTP","System Present Time ="+System.currentTimeMillis());
                        }
                        if (now!=0) MetaioDebug.log("loadDefinitionsBeforeCallingActivity: ntp:now="+now);

                    } while ((now==0)&&((System.currentTimeMillis()-loopStart)<20000));
                    MetaioDebug.log("loadDefinitionsBeforeCallingActivity: ending the loop querying pool.ntp.org for 20 seconds max:"+(System.currentTimeMillis()-loopStart)+" millis:"+now);
                    if (now!=0)
                    {
                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity: System.currentTimeMillis() before setTime="+System.currentTimeMillis());
                        clockSetSuccess = true;
                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity: System.currentTimeMillis() AFTER setTime="+System.currentTimeMillis());
                    }
                }
                /*
                //  If we could not get time from pool.ntp.org we will try to pry it from GPS signal.
                //
                //
                */
                if (!clockSetSuccess)
                {
                    MetaioDebug.log("loadDefinitionsBeforeCallingActivity: starting to retrieve time from GPS signal");
                    if (1==1)//(seamensorAdminPresent)
                    {
                        publishProgress(getString(R.string.admin_present_waiving_gps_time));
                        clockSetSuccess=true;
                    }
                    else
                    {
                        publishProgress(getString(R.string.waiting_for_gps_time));
                        long millisRetrievedFromGPS = 0;
                        boolean wait = true;
                        while (wait)
                        {
                            if (isCancelled())
                            {
                                Log.i("AsyncTask","loadDefinitionsBeforeCallingActivity: cancelled");
                                break;
                            }
                            if ((gpsUTCTime != null) && (gpsUTCddmmyy != null))
                            {
                                String gpsCapturedTimeStamp = gpsUTCddmmyy + " " + gpsUTCTime;
                                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyy HHmmss.SS");
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                Date date = null;
                                try {
                                    date = sdf.parse(gpsCapturedTimeStamp);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                millisRetrievedFromGPS = date.getTime();
                                long currentMillis = millisRetrievedFromGPS + (System.currentTimeMillis() - systemMillisWhenGPSTimereceived);
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity: new time will be set to currentMillis = " + currentMillis);
                                clockSetSuccess = true;
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity: new time is set to currentMillis = " + currentMillis);
                                currentMillisString = Long.toString(currentMillis);
                                wait = false;
                            }
                        }
                    }
                }
                /*
                //  Loading Data from Dropbox and Writing to Local Storage
                //
                //
                */
                if (clockSetSuccess)
                {
                    try
                    {
                        //MetaioDebug.log("####### LOADING: dbxFs ");
                        //dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                        // Loading Markers from Dropbox and writing to local storage.
                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity: ####### LOADING: MARKERFILES CONTENTS");
                        publishProgress(getString(R.string.waiting_to_load_assets));
                        for (int i = 0; i < qtyVps; i++)
                        {
                            if (isCancelled())
                            {
                                Log.i("AsyncTask", "loadDefinitionsBeforeCallingActivity: cancelled");
                                break;
                            }
                            boolean newVpMarkerImageFileInDropbox = false;
                            try
                            {
                                File markerFile = new File(getFilesDir(), "markervp" + (i + 1) + ".jpg");
                                DbxPath vpMarkerImageFilePath = new DbxPath(DbxPath.ROOT, markervpDropboxPath + (i + 1) + ".jpg");
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:vpMarkerImageFilePath Dropbox: " + markervpDropboxPath + (i + 1) + ".jpg");
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:vpConfiguredTimeSMCMillis[i]: " + vpConfiguredTimeSMCMillis[i]);
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:vpConfiguredTimeInUseMillis[i]: " + vpConfiguredTimeInUseMillis[i]);
                                DbxFile vpMarkerImageFileDropbox = dbxFs.open(vpMarkerImageFilePath);
                                DbxFileStatus vpMarkerImageFileDropboxStatus = vpMarkerImageFileDropbox.getSyncStatus();
                                if ((!vpMarkerImageFileDropboxStatus.isLatest) || (markerFile.length() == 0) || (vpConfiguredTimeSMCMillis[i] > vpConfiguredTimeInUseMillis[i]) || reloadConfig)
                                {
                                    vpMarkerImageFileDropbox.update();
                                    vpMarkerImageFileContents = BitmapFactory.decodeStream(vpMarkerImageFileDropbox.getReadStream());
                                    newVpMarkerImageFileInDropbox = true;
                                }
                                vpMarkerImageFileDropbox.close();
                            }
                            catch (Exception e)
                            {
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:EXCEPTION with vpMarkerImageFilePath: " + markervpDropboxPath + (i + 1) + ".jpg");
                                e.printStackTrace();
                            }
                            try
                            {
                                File markerFile = new File(getFilesDir(), "markervp" + (i + 1) + ".jpg");
                                if ((!markerFile.exists()) || (newVpMarkerImageFileInDropbox))
                                {
                                    MetaioDebug.log("loadDefinitionsBeforeCallingActivity:WRITING TO LOCAL STORAGE: vpMarkerImageFilePath LOCAL: Exists:" + markerFile.exists() + "Path: " + getFilesDir() + "/" + "markervp" + (i + 1) + ".jpg");
                                    FileOutputStream fos = new FileOutputStream(markerFile);
                                    try
                                    {
                                        vpMarkerImageFileContents.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                                    }
                                    catch (Exception Ex)
                                    {
                                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity:Error compressing : " + Ex.getMessage());
                                    }
                                    fos.close();
                                    MetaioDebug.log("loadDefinitionsBeforeCallingActivity:markerFile LOCAL: " + markerFile.getAbsolutePath());
                                }
                            }
                            catch (FileNotFoundException e)
                            {
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:File not found: " + e.getMessage());
                            }
                            catch (Exception e)
                            {
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:Error accessing file: " + e.getMessage());
                            }
                        }

                        publishProgress(getString(R.string.still_loading_assets));
                        // Loading Vp Location Description Images from Dropbox and writing to local storage.
                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity:####### LOADING: VPDESCFILES CONTENTS");
                        for (int i = 0; i < qtyVps; i++)
                        {
                            if (isCancelled())
                            {
                                Log.i("AsyncTask", "loadDefinitionsBeforeCallingActivity: cancelled");
                                break;
                            }
                            boolean newVpLocationDescImageFileInDropbox = false;
                            try
                            {
                                File descvpFile = new File(getFilesDir(), "descvp" + (i + 1) + ".png");
                                DbxPath vpLocationDescImageFilePath = new DbxPath(DbxPath.ROOT, descvpDropboxPath + (i + 1) + ".png");
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:vpLocationDescImageFilePath DROPBOX: " + descvpDropboxPath + (i + 1) + ".png");
                                DbxFile vpLocationDescImageFileDropbox = dbxFs.open(vpLocationDescImageFilePath);
                                DbxFileStatus vpLocationDescImageFileDropboxStatus = vpLocationDescImageFileDropbox.getSyncStatus();
                                if ((!vpLocationDescImageFileDropboxStatus.isLatest) || (descvpFile.length() == 0) || (vpConfiguredTimeSMCMillis[i] > vpConfiguredTimeInUseMillis[i]) || reloadConfig)
                                {
                                    vpLocationDescImageFileDropbox.update();
                                    vpLocationDescImageFileContents = BitmapFactory.decodeStream(vpLocationDescImageFileDropbox.getReadStream());
                                    newVpLocationDescImageFileInDropbox = true;
                                }
                                vpLocationDescImageFileDropbox.close();
                            }
                            catch (Exception e)
                            {
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:EXCEPTION with vpLocationDescImageFilePath: " + descvpDropboxPath + (i + 1) + ".png");
                                e.printStackTrace();
                            }
                            try
                            {
                                File descvpFile = new File(getFilesDir(), "descvp" + (i + 1) + ".png");
                                //MetaioDebug.log("descvpFile: "+"descvp" + (i + 1) + ".png ="+ descvpFile.exists());
                                //MetaioDebug.log("descvpFile: "+"descvp" + (i + 1) + ".png ="+ descvpFile.getPath());
                                //MetaioDebug.log("descvpFile: "+"descvp" + (i + 1) + ".png ="+ descvpFile.getAbsolutePath());
                                //MetaioDebug.log("descvpFile: "+"descvp" + (i + 1) + ".png ="+ descvpFile.getCanonicalPath());
                                if ((!descvpFile.exists()) || (newVpLocationDescImageFileInDropbox))
                                {
                                    MetaioDebug.log("loadDefinitionsBeforeCallingActivity:WRITING TO LOCAL STORAGE: vpLocationDescImageFilePath local: " + getFilesDir() + "/" + "descvp" + (i + 1) + ".png");
                                    FileOutputStream fos = new FileOutputStream(descvpFile);
                                    try
                                    {
                                        vpLocationDescImageFileContents.compress(Bitmap.CompressFormat.PNG, 95, fos);
                                    }
                                    catch (Exception Ex)
                                    {
                                        MetaioDebug.log("loadDefinitionsBeforeCallingActivity:loadDefinitionsBeforeCallingActivity:Error compressing : " + Ex.getMessage());
                                    }
                                    fos.close();
                                }
                            }
                            catch (FileNotFoundException e)
                            {
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:File not found: " + e.getMessage());
                            }
                            catch (IOException e)
                            {
                                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:Error accessing file: " + e.getMessage());
                            }
                        }


                        publishProgress(getString(R.string.almost_finished_loading_assets));
                        // Loading Metaio Assets
                        try
                        {
                            MetaioDebug.log("loadDefinitionsBeforeCallingActivity:####### LOADING: METAIO ASSETS");
                            AssetsManager.extractAllAssets(getApplicationContext(), true);
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                            MetaioDebug.log(Log.ERROR, "loadDefinitionsBeforeCallingActivity:AssetsManager.extractAllAssets failed, see stack trace");
                        }

                        dataLoadSuccess = true;
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        MetaioDebug.log(Log.ERROR, "loadDefinitionsBeforeCallingActivity:Error loadDefinitionsFromDropboxBeforeCallingSeamensor() in BACKGROUND");
                    }
                    publishProgress(getString(R.string.load_assets_finished));
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                super.onPostExecute(result);
                MetaioDebug.log("loadDefinitionsBeforeCallingActivity: onPostExecute()");
                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:####### LOADING: onPostExecute: callingARVewactivity: clockSetSuccess=" + clockSetSuccess);
                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:####### LOADING: onPostExecute: callingARVewactivity: dataLoadSuccess=" + dataLoadSuccess);
                MetaioDebug.log("loadDefinitionsBeforeCallingActivity:####### LOADING: onPostExecute: callingARVewactivity: activityToBeCalled="+activityToBeCalled);
                if ((dataLoadSuccess)&&(clockSetSuccess))
                {
                    TextView message = (TextView) findViewById(R.id.bottom_message);
                    message.setText(getString(R.string.load_assets_finished));
                    if (activityToBeCalled.equalsIgnoreCase("SMC"))
                    {
                        try
                        {
                            Intent intent = new Intent(getApplicationContext(), smc.class);
                            intent.putExtra("seamensoraccount", seamensorAccount);
                            intent.putExtra("usernumber", userNumber[userLoggedCounter]);
                            intent.putExtra("username", userName[userLoggedCounter]);
                            intent.putExtra("dcinumber", dciNumber);
                            intent.putExtra("QtyVps", qtyVps);
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
                            Intent intent = new Intent(getApplicationContext(), seamensor.class);
                            intent.putExtra("seamensoraccount", seamensorAccount);
                            intent.putExtra("dcinumber", dciNumber);
                            intent.putExtra("QtyVps", qtyVps);
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
                else
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



