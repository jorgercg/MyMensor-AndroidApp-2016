package com.seamensor.seamensor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.location.Location;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileStatus;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.CameraVector;
import com.metaio.sdk.jni.ETRACKING_STATE;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKAndroid;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.ImageStruct;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector2di;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxAccountManager;

public class seamensor extends ARViewActivity implements
        OnItemClickListener,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
	private IGeometry mSeaMensorCube;
	private IGeometry mVpChecked;
	private MetaioSDKCallbackHandler mSDKCallback;
	private TrackingValues mTrackingValues;
    private Rotation rotation;
	private Vector3d cameraToCOS;
	private Vector3d cameraEulerAngle;
    private float trackingQuality;
    private float modelScale = 10;

    public int timesSetMrklsTrkIsCalled = 0;

    public static final boolean debugMode = false;

	public static final String appKey = "0yrlhapf89ytpwi";
	public static final String appSecret = "neg16q87i8rpym3";
	public static final int REQUEST_LINK_TO_DBX = 0;
    public boolean inPosition = false;
    public boolean inRotation = false;
	public boolean vpPhotoAccepted = false;
	public boolean vpPhotoRejected = false;
	public boolean vpPhotoRequestInProgress = false;
	public boolean lastVpPhotoRejected = false;
    public DbxAccountManager mDbxAcctMgr;
    public final int idMarkerStdSize = 20;
    public final String trackingConfigFileName = "TrackingDataMarkerless.xml";
    public final String idMarkersTrackingConfigFileName = "TrckMarkers.xml";
    public final short standardMarkerlessMarkerWidth = 500;
    public final short standardMarkerlessMarkerHeigth = 500;
    public final String seaMensorMarker = "seamensormarker.jpg";
    public final short seaMensorMarkerWidthWhenIdIs20mm = 46;
    public final short seaMensorMarkerHeigthWhenIdIs20mm = 46;
    public final short seaMensorMarkerWidthWhenIdIs100mm = 134;
    public final short seaMensorMarkerHeigthWhenIdIs100mm = 134;
    public String markerlesstrackingConfigFileContents;
    public String idMarkersTrackingConfigFileContents;
    public String trkLocalFilePath;
    public String geometry3dConfigFile = "CuboVP.obj";
	public String geometrySecondary3dConfigFile = "vpchecked.obj";
	public String cameraCalibrationFileName = "cameracalibration.xml";
	public final String vpsConfigFileDropbox = "vps.xml";
	public final String vpsCheckedConfigFileDropbox = "vpschecked.xml";

    public String camCalDropboxPath;
    public String descvpDropboxPath;
    public String markervpDropboxPath;
    public String trackingDropboxPath;
    public String vpsDropboxPath;
    public String vpsCheckedDropboxPath;
    public String capDropboxPath;

    public boolean[] vpChecked;
    public long[] photoTakenTimeMillis;
    public long[] vpNextCaptureMillis;
    public boolean resultMarkerlessTrk = false;
    public boolean resultSpecialTrk = false;
    public boolean idTrackingLoaded = false;
    public boolean calledIdTrackingLoad = false;
    public boolean superTrackingSingleLoaded = false;
    public boolean calledSuperTrackingSingleLoad = false;
    public boolean waitingForMarkerlessTrackingConfigurationToLoad = false;
	public boolean doubleCheckingProcedureStarted=false;
	public boolean doubleCheckingProcedureFinalized=false;
	public boolean inFocus = false;
	public boolean imageViewTransparent = true;
    public boolean torchModeOn = false;
	public boolean showingVpLocationPhoto = false;
    public long millisWhenEtsWasInitialized=0;
    public long elapsedMillisSinceEtsWasInitialized=0;
    public boolean etsInitializedAndNotFound=false;

	public static float tolerancePosition;
	public static float toleranceRotation;
	
	public short shipId;
	public short qtyVps = 0;
    public short markerWidth = 500;
    public short markerHeight = 500;
	public String frequencyUnit;
	public int frequencyValue;
	public int im;
	
    private static final long INTERVAL = 1000 * 60 * 60;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mCurrentLocation;
    String mLastUpdateTime;

	public int coordinateSystemTrackedInPoseI;
	
	public int initialCoordinateSystemTrackedInPoseI;
	public int disambiguatedCoordinateSystemTrackedInPoseI;
    public int lastVpSelectedByUser;
	public int photoSelected = 0;
	public boolean[] vpIsAmbiguous;
    public boolean[] vpFlashTorchIsOn;
    public boolean[] vpIsSuperSingle;
    public boolean[] vpSuperIdIs20mm;
    public boolean[] vpSuperIdIs100mm;
    public int[] vpSuperMarkerId;
	public boolean vpIsDisambiguated = false;
	public boolean waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
	
	public short[] vpNumber;
    public String[] vpLocationDesText;
	public int[] vpXCameraDistance;
	public int[] vpYCameraDistance;
	public int[] vpZCameraDistance;
	public int[] vpXCameraRotation;
	public int[] vpYCameraRotation;
	public int[] vpZCameraRotation;
    public short[] vpMarkerlessMarkerWidth;
    public short[] vpMarkerlessMarkerHeigth;
	public String[] vpFrequencyUnit;
	public long[] vpFrequencyValue;
	public static Bitmap vpLocationDescImageFileContents;
	public static Bitmap selectedVpPhotoImageFileContents;
	public String[] locPhotoToExif;
    public String[] currentLocationToPhotoToExif;
	
	public String seamensorAccount;
    public int dciNumber;
	//public String currentMillisStartAppString;
    //public Long localCurrentMillisStartApp;
	
	private static long back_pressed;
	
	ListView listView;
	TouchImageView imageView;
	ImageView targetImageView;
	ImageView radarScanImageView;
    ImageView mProgress;
	ImageView swayRightImageView;
	ImageView swayLeftImageView;
	ImageView heaveUpImageView;
	ImageView heaveDownImageView;
	ImageView surgeImageView;
	ImageView yawImageView;
	ImageView pitchImageView;
	ImageView rollCImageView;
	ImageView roll01ImageView;
	ImageView roll02ImageView;
	ImageView roll03ImageView;
	ImageView roll04ImageView;
	ImageView roll05ImageView;
	ImageView roll06ImageView;
	ImageView roll07ImageView;
	ImageView roll08ImageView;
	ImageView roll09ImageView;
	ImageView roll10ImageView;
	ImageView roll11ImageView;
	ImageView roll12ImageView;
	ImageView roll13ImageView;
	ImageView roll14ImageView;
	ImageView roll15ImageView;
	
	Button okButton;
	Button alphaToggleButton;
    Button flashTorchVpToggleButton;
	Button showVpCapturesButton;
	Button showPreviousVpCaptureButton;
	Button showNextVpCaptureButton;
	ImageButton exitButton;
	Button acceptVpPhotoButton;
	Button rejectVpPhotoButton;
	TextView vpLocationDesTextView;
	TextView vpIdNumber;
    TextView xPosView;
    TextView yPosView;
    TextView zPosView;
    TextView xRotView;
    TextView yRotView;
    TextView zRotView;
    TextView trkQualityView;
	TextView isVpPhotoOkTextView;

	Animation rotationRadarScan;
    Animation rotationMProgress;

	public Drawable drawableTargetWhite;
	public Drawable drawableTargetGreen;
	
	public Drawable drawableSwayRight;
	public Drawable drawableSwayLeft;
	
	public Drawable drawableHeaveUp;
	public Drawable drawableHeaveDown;
	
	public Drawable drawableSurgeA1;
	public Drawable drawableSurgeA2;
	public Drawable drawableSurgeA3;
	public Drawable drawableSurgeB1;
	public Drawable drawableSurgeB2;
	public Drawable drawableSurgeB3;
	
	public Drawable drawableYawC;
	public Drawable drawableYawL1;
	public Drawable drawableYawL2;
	public Drawable drawableYawL3;
	public Drawable drawableYawL4;
	public Drawable drawableYawR1;
	public Drawable drawableYawR2;
	public Drawable drawableYawR3;
	public Drawable drawableYawR4;
	
	public Drawable drawablePitchC;
	public Drawable drawablePitchH1;
	public Drawable drawablePitchH2;
	public Drawable drawablePitchH3;
	public Drawable drawablePitchH4;
	public Drawable drawablePitchL1;
	public Drawable drawablePitchL2;
	public Drawable drawablePitchL3;
	public Drawable drawablePitchL4;
	
	public Drawable drawableRollC;
	public Drawable drawableRoll01;
	public Drawable drawableRoll02;
	public Drawable drawableRoll03;
	public Drawable drawableRoll04;
	public Drawable drawableRoll05;
	public Drawable drawableRoll06;
	public Drawable drawableRoll07;
	public Drawable drawableRoll08;
	public Drawable drawableRoll09;
	public Drawable drawableRoll10;
	public Drawable drawableRoll11;
	public Drawable drawableRoll12;
	public Drawable drawableRoll13;
	public Drawable drawableRoll14;
	public Drawable drawableRoll15;

	private TransferUtility transferUtility;

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		// Retrieve SeaMensor configuration info
		seamensorAccount = getIntent().getExtras().get("seamensoraccount").toString();
		dciNumber = Integer.parseInt(getIntent().getExtras().get("dcinumber").toString());
		qtyVps = Short.parseShort(getIntent().getExtras().get("QtyVps").toString());
        /*
		try
		{
			currentMillisStartAppString = getIntent().getExtras().get("currentmillis").toString();
		}
		catch (Exception e) 
    	{
			currentMillisStartAppString ="0000";
        }
        try
        {
            localCurrentMillisStartApp = ((Long) getIntent().getExtras().get("localcurrentmillis"));
        }
        catch (Exception e)
        {
            localCurrentMillisStartApp = (localCurrentMillisStartApp * 0);
        }
        */


        //markerlesstrackingConfigFileContents = getIntent().getExtras().getString("MarkerlessTrkCfg");
        //idMarkersTrackingConfigFileContents = getIntent().getExtras().getString("IdMarkerTrkCfg");


		MetaioDebug.log("SeaMensor onCreate:Package: "+this.getPackageName());
		MetaioDebug.log("SeaMensor onCreate:SeaMensor Account: "+seamensorAccount);
		MetaioDebug.log("SeaMensor onCreate:Qty Vps: "+qtyVps);
		//MetaioDebug.log("SeaMensor onCreate:Current Millis since Epoch: "+currentMillisStartAppString);

        // Fused Location Provider
        MetaioDebug.log("SeaMensor onCreate: Setting up Fused Location Provider");

        if (!isGooglePlayServicesAvailable())
        {
            MetaioDebug.log("SeaMensor onCreate: isGooglePlayServicesAvailable()=false");
            //finish();
        }
        else MetaioDebug.log("SeaMensor onCreate: isGooglePlayServicesAvailable()=true");


        createLocationRequest();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


		// Enable Dropbox
		mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), appKey, appSecret);
		if (!mDbxAcctMgr.hasLinkedAccount()) 
		{
			MetaioDebug.log("SeaMensor onCreate: Linking to DROPBOX");
			mDbxAcctMgr.startLink((Activity)this, REQUEST_LINK_TO_DBX);		
		}
		else
		{
			MetaioDebug.log("SeaMensor onCreate: Linked to DROPBOX ALREADY");
		}

        camCalDropboxPath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"cam"+"/";
        descvpDropboxPath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"dsc"+"/"+"descvp";
        markervpDropboxPath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"mrk"+"/"+"markervp";
        trackingDropboxPath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"trk"+"/";
        vpsDropboxPath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/";
        vpsCheckedDropboxPath = seamensorAccount+"/"+"chk"+"/"+dciNumber+"/";
        capDropboxPath = seamensorAccount+"/"+"cap"+"/";

		mSDKCallback = new MetaioSDKCallbackHandler();

		// Load VPs data
        loadConfigurationFile();
		loadVpsChecked();
		verifyVpsChecked();
		
		try 
		{
			drawableTargetWhite = getResources().getDrawable(R.drawable.targetwhite);
			drawableTargetGreen = getResources().getDrawable(R.drawable.targetgreen);
			drawableSwayRight = getResources().getDrawable(R.drawable.sway_right);
			drawableSwayLeft = getResources().getDrawable(R.drawable.sway_left);
			drawableHeaveUp = getResources().getDrawable(R.drawable.heave_up);
			drawableHeaveDown = getResources().getDrawable(R.drawable.heave_down);
			drawableSurgeA1 = getResources().getDrawable(R.drawable.surge_a1);
			drawableSurgeA2 = getResources().getDrawable(R.drawable.surge_a2);
			drawableSurgeA3 = getResources().getDrawable(R.drawable.surge_a3);
			drawableSurgeB1 = getResources().getDrawable(R.drawable.surge_b1);
			drawableSurgeB2 = getResources().getDrawable(R.drawable.surge_b2);
			drawableSurgeB3 = getResources().getDrawable(R.drawable.surge_b3);
			drawableYawC = getResources().getDrawable(R.drawable.yaw_c);
			drawableYawL1 = getResources().getDrawable(R.drawable.yaw_l1);
			drawableYawL2 = getResources().getDrawable(R.drawable.yaw_l2);
			drawableYawL3 = getResources().getDrawable(R.drawable.yaw_l3);
			drawableYawL4 = getResources().getDrawable(R.drawable.yaw_l4);
			drawableYawR1 = getResources().getDrawable(R.drawable.yaw_r1);
			drawableYawR2 = getResources().getDrawable(R.drawable.yaw_r2);
			drawableYawR3 = getResources().getDrawable(R.drawable.yaw_r3);
			drawableYawR4 = getResources().getDrawable(R.drawable.yaw_r4);
			drawablePitchC = getResources().getDrawable(R.drawable.pitch_c);
			drawablePitchH1 = getResources().getDrawable(R.drawable.pitch_h1);
			drawablePitchH2 = getResources().getDrawable(R.drawable.pitch_h2);
			drawablePitchH3 = getResources().getDrawable(R.drawable.pitch_h3);
			drawablePitchH4 = getResources().getDrawable(R.drawable.pitch_h4);
			drawablePitchL1 = getResources().getDrawable(R.drawable.pitch_l1);
			drawablePitchL2 = getResources().getDrawable(R.drawable.pitch_l2);
			drawablePitchL3 = getResources().getDrawable(R.drawable.pitch_l3);
			drawablePitchL4 = getResources().getDrawable(R.drawable.pitch_l4);
			drawableRollC = getResources().getDrawable(R.drawable.roll_c);
			drawableRoll01 = getResources().getDrawable(R.drawable.roll_01);
			drawableRoll02 = getResources().getDrawable(R.drawable.roll_02);
			drawableRoll03 = getResources().getDrawable(R.drawable.roll_03);
			drawableRoll04 = getResources().getDrawable(R.drawable.roll_04);
			drawableRoll05 = getResources().getDrawable(R.drawable.roll_05);
			drawableRoll06 = getResources().getDrawable(R.drawable.roll_06);
			drawableRoll07 = getResources().getDrawable(R.drawable.roll_07);
			drawableRoll08 = getResources().getDrawable(R.drawable.roll_08);
			drawableRoll09 = getResources().getDrawable(R.drawable.roll_09);
			drawableRoll10 = getResources().getDrawable(R.drawable.roll_10);
			drawableRoll11 = getResources().getDrawable(R.drawable.roll_11);
			drawableRoll12 = getResources().getDrawable(R.drawable.roll_12);
			drawableRoll13 = getResources().getDrawable(R.drawable.roll_13);
			drawableRoll14 = getResources().getDrawable(R.drawable.roll_14);
			drawableRoll15 = getResources().getDrawable(R.drawable.roll_15);
		}
			catch (Exception e)
		{    
			e.printStackTrace();
		}
	}

    protected void createLocationRequest()
    {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == status) {
            return true;
        } else {
            GooglePlayServicesUtil.getErrorDialog(status, this, 0).show();
            return false;
        }
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        MetaioDebug.log("ARViewActivity.onConnected SeaMensor - isConnected=" + mGoogleApiClient.isConnected());
        startLocationUpdates();
    }

    protected void startLocationUpdates()
    {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        MetaioDebug.log("ARViewActivity.startLocationUpdates: Location update started");
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        MetaioDebug.log("ARViewActivity.stopLocationUpdates Location update stopped .......................");
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        MetaioDebug.log("ARViewActivity.onConnectionFailed SeaMensor - Connection failed: " + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location)
    {
        MetaioDebug.log("ARViewActivity.onLocationChanged - CALLED");
        mCurrentLocation = location;
		mLastUpdateTime = Long.toString(System.currentTimeMillis());
        //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        if (null != mCurrentLocation)
        {
            String lat = String.valueOf(mCurrentLocation.getLatitude());
            String lng = String.valueOf(mCurrentLocation.getLongitude());
            MetaioDebug.log("At Time:mLastUpdateTime=" + mLastUpdateTime + " Lat:" + lat + " Lon:" + lng + " Accuracy:" + mCurrentLocation.getAccuracy() + " Provider:" + mCurrentLocation.getProvider());
        }
        else
        {
            MetaioDebug.log("ARViewActivity.onLocationChanged - location is null");
        }
    }

    /*
    public void callLocationSystem()
    {
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener()
        {
            public void onLocationChanged(Location location)
            {
                //accuracy = location.getAccuracy();
                currentLocationToPhotoToExif = getGPSToExif(location);
                MetaioDebug.log("onLocationChanged: location received");

            }

            public void onStatusChanged(String provider, int status, Bundle extras)
            {

            }

            public void onProviderEnabled(String provider)
            {

            }

            public void onProviderDisabled(String provider)
            {

            }
        };

        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,0,0,locationListener);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);

        lastKnowLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        Criteria criteria = new Criteria();
        provider = lm.getBestProvider(criteria, false);

    }
    */


	@Override
	protected void onStart() 
	{
		super.onStart();
		/*
        long currentMillis = System.currentTimeMillis();
		if (currentMillis < Long.parseLong(currentMillisStartAppString))
		{
			//MetaioDebug.log("SeaMensor onStart:Device clock with problems: please reset time");
			//Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.device_clock_problem), Toast.LENGTH_LONG);
	        //toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 30);
	        //toast.show();
            Long presentLocalCurrentMillis = System.currentTimeMillis();
            presentLocalCurrentMillis = Long.parseLong(currentMillisStartAppString)+presentLocalCurrentMillis-localCurrentMillisStartApp;
            setTime(presentLocalCurrentMillis);
            //finish();
		}
        */

        mGoogleApiClient.connect();

		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
		            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
		            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
		            | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
		            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		String[] novaLista = new String[qtyVps];
		for (int i=0; i<qtyVps; i++)
		{
			novaLista[i] = getString(R.string.vp_name)+vpNumber[i];
		}
		listView = (ListView) mGUIView.findViewById(R.id.vp_list);
		MetaioDebug.log("Listview: "+listView);
		listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, novaLista));
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		listView.setOnItemClickListener(this);
		listView.setVisibility(View.VISIBLE);

		vpLocationDesTextView = (TextView) mGUIView.findViewById(R.id.textView1);
		vpIdNumber = (TextView) mGUIView.findViewById(R.id.textView2);
		imageView = (TouchImageView) mGUIView.findViewById(R.id.imageView1);
		targetImageView = (ImageView) mGUIView.findViewById(R.id.imageViewTarget);
		radarScanImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRadarScan);
        mProgress = (ImageView) mGUIView.findViewById(R.id.waitingTrkLoading);
        swayRightImageView = (ImageView) mGUIView.findViewById(R.id.imageViewSwayRight);
		swayLeftImageView = (ImageView) mGUIView.findViewById(R.id.imageViewSwayLeft);
		heaveUpImageView = (ImageView) mGUIView.findViewById(R.id.imageViewHeaveUp);
		heaveDownImageView = (ImageView) mGUIView.findViewById(R.id.imageViewHeaveDown);
		surgeImageView = (ImageView) mGUIView.findViewById(R.id.imageViewSurge);
		exitButton = (ImageButton) mGUIView.findViewById(R.id.button1);
		okButton = (Button) mGUIView.findViewById(R.id.button2);
		alphaToggleButton = (Button) mGUIView.findViewById(R.id.buttonAlphaToggle);
        flashTorchVpToggleButton = (Button) mGUIView.findViewById(R.id.buttonFlashTorchVpToggle);
		showVpCapturesButton = (Button) mGUIView.findViewById(R.id.buttonShowVpCaptures);
		showPreviousVpCaptureButton = (Button) mGUIView.findViewById(R.id.buttonShowPreviousVpCapture);
		showNextVpCaptureButton = (Button) mGUIView.findViewById(R.id.buttonShowNextVpCapture);
		acceptVpPhotoButton = (Button) mGUIView.findViewById(R.id.buttonAcceptVpPhoto);
		rejectVpPhotoButton = (Button) mGUIView.findViewById(R.id.buttonRejectVpPhoto);

		yawImageView = (ImageView) mGUIView.findViewById(R.id.imageViewYaw);
		pitchImageView = (ImageView) mGUIView.findViewById(R.id.imageViewPitch);
		
		rollCImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRollCenter);
		roll01ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll01);
		roll02ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll02);
		roll03ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll03);
		roll04ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll04);
		roll05ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll05);
		roll06ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll06);
		roll07ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll07);
		roll08ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll08);
		roll09ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll09);
		roll10ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll10);
		roll11ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll11);
		roll12ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll12);
		roll13ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll13);
		roll14ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll14);
		roll15ImageView = (ImageView) mGUIView.findViewById(R.id.imageViewRoll15);
		
		exitButton.setVisibility(View.VISIBLE);

        xPosView = (TextView) mGUIView.findViewById(R.id.xPosView);
        yPosView = (TextView) mGUIView.findViewById(R.id.yPosView);
        zPosView = (TextView) mGUIView.findViewById(R.id.zPosView);
        xRotView = (TextView) mGUIView.findViewById(R.id.xRotView);
        yRotView = (TextView) mGUIView.findViewById(R.id.yRotView);
        zRotView = (TextView) mGUIView.findViewById(R.id.zRotView);
        trkQualityView = (TextView) mGUIView.findViewById(R.id.trkQualityView);
		isVpPhotoOkTextView = (TextView) mGUIView.findViewById(R.id.textViewIsPhotoOK);

        rotationRadarScan = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
		radarScanImageView.setVisibility(View.VISIBLE);
		radarScanImageView.startAnimation(rotationRadarScan);

        rotationMProgress = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        mProgress.setVisibility(View.GONE);
        mProgress.startAnimation(rotationMProgress);
	}


	@Override
	public void onBackPressed()
	{
		if (back_pressed + 2000 > System.currentTimeMillis())
        {
            super.onBackPressed();
        }
		else
			runOnUiThread(new Runnable()
			{
				@Override
				public void run() 
				{	
					Toast.makeText(getBaseContext(), getString(R.string.double_bck_exit), Toast.LENGTH_SHORT).show();
				}
			});
		back_pressed = System.currentTimeMillis();
	}
	
	
	@Override
	protected void startCamera()
	{
		super.startCamera();
		final CameraVector cameras = metaioSDK.getCameraList();
		if (cameras.size() > 0)
		{
			com.metaio.sdk.jni.Camera camera = cameras.get(0);
			camera.setResolution(new Vector2di (1280,720));
			metaioSDK.startCamera(camera);
			MetaioDebug.log("CameraSeamensor: Name: "+camera);
			MetaioDebug.log("CameraSeamensor: Res: "+camera.getResolution());
			MetaioDebug.log("CameraSeamensor: FPS: " + camera.getFps());
		}
		else
		{
			MetaioDebug.log(Log.WARN, "No camera found on the device!");
		}
	}

	 /*
     * Parameters:
        adapter - The AdapterView where the click happened.
        view - The view within the AdapterView that was clicked
        position - The position of the view in the adapter.
        id - The row id of the item that was clicked.
     */
	@Override
	public void onItemClick(AdapterView<?> adapter, View view, final int position, long id) 
	{
		MetaioDebug.log("Adapter: " + adapter);
		MetaioDebug.log("View: "+view);
		MetaioDebug.log("Position: "+position);
		MetaioDebug.log("Row id: "+id);
		vpLocationDescImageFileContents = null;
        lastVpSelectedByUser = position;
		// File path of VP Location Picture Image
		try
		{
			File descvpFile = new File(AssetsManager.getAbsolutePath()+"/", "descvp" + (position + 1) + ".png");
            FileInputStream fis = new FileInputStream(descvpFile);
			vpLocationDescImageFileContents = BitmapFactory.decodeStream(fis);
			fis.close();
    	}
		catch (Exception e)
		{
			e.printStackTrace();
			MetaioDebug.log(Log.ERROR, "vpLocationDescImageFile failed, see stack trace");
		}
		runOnUiThread(new Runnable()
		{
			@Override
			public void run() 
			{	
				showingVpLocationPhoto = true;
				// TURNING OFF RADAR SCAN
				radarScanImageView.clearAnimation();
				radarScanImageView.setVisibility(View.GONE);
				// Setting the correct listview set position
				listView.setItemChecked(position, vpChecked[position]);
                // Show last captured date and what is the frequency
                String lastTimeAcquiredAndNextOne = "";
                String formattedNextDate="";
                if (photoTakenTimeMillis[position]>0)
                {
                    Date lastDate = new Date(photoTakenTimeMillis[position]);
                    Date nextDate = new Date(vpNextCaptureMillis[position]);
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ssZ");
					sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String formattedLastDate = sdf.format(lastDate);
                    formattedNextDate = sdf.format(nextDate);
                    lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
                                                 formattedLastDate+"  "+
                                                 getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
                                                 formattedNextDate;
                }
                else
                {
                    lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
                                                 getString(R.string.date_vp_touched_not_acquired)+"  "+
                                                 getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
                                                 getString(R.string.date_vp_touched_first_acquisition);
                }
				// VP Location Description TextView
                MetaioDebug.log("vpLocationDesText["+position+"]:"+vpLocationDesText[position]+lastTimeAcquiredAndNextOne);
				vpLocationDesTextView.setText(vpLocationDesText[position] + "\n" + lastTimeAcquiredAndNextOne);
				vpLocationDesTextView.setVisibility(View.VISIBLE);
				// VP Location # TextView
				String vpId = Integer.toString(vpNumber[position]);
				vpId = getString(R.string.vp_name)+vpId;
				vpIdNumber.setText(vpId);
				vpIdNumber.setVisibility(View.VISIBLE);
				// VP Location Picture ImageView
				if (!(vpLocationDescImageFileContents==null))
				{
					imageView.setImageBitmap(vpLocationDescImageFileContents);
					imageView.setVisibility(View.VISIBLE);
					imageView.resetZoom();
					if (imageViewTransparent)
					{
						imageView.setImageAlpha(128);
					}
					else
						imageView.setImageAlpha(255);
				}
				// Dismiss Location Description Buttons
				okButton.setVisibility(View.VISIBLE);
				alphaToggleButton.setVisibility(View.VISIBLE);
				if (imageViewTransparent) alphaToggleButton.setBackgroundResource(R.drawable.custom_button_option_selected);
				if (!imageViewTransparent) alphaToggleButton.setBackgroundResource(R.drawable.custom_button_option_notselected);
                flashTorchVpToggleButton.setVisibility(View.VISIBLE);
                if (vpFlashTorchIsOn[position])
                {
                    flashTorchVpToggleButton.setTextColor(Color.BLACK);
                    flashTorchVpToggleButton.setBackgroundResource(R.drawable.custom_button_option_notselected);
                }
                if (!vpFlashTorchIsOn[position])
                {
                    flashTorchVpToggleButton.setTextColor(Color.GRAY);
                    flashTorchVpToggleButton.setBackgroundResource(R.drawable.custom_button_option_notselected);
                }
				showVpCapturesButton.setVisibility(View.VISIBLE);
				exitButton.setVisibility(View.GONE);
				listView.setVisibility(View.GONE);
			}
		});
			
	}
			
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        if (requestCode == REQUEST_LINK_TO_DBX) {
            if (resultCode == Activity.RESULT_OK) 
            	{
    			MetaioDebug.log("Link to DROPBOX OK");
                } 
            else
            	{
            	MetaioDebug.log("Link to DROPBOX FAILED");
            	}
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

	@Override
	protected void onResume() 
	{
		super.onResume();
		MetaioDebug.log("ARViewActivity.onResume SeaMensor");
        if (mGoogleApiClient.isConnected()) startLocationUpdates();
		setVpsChecked();
        runOnUiThread(new Runnable() {
			@Override
			public void run() {
				{
					MetaioDebug.log("onResume: Turning off Waiting Circle");
					mProgress.clearAnimation();
					mProgress.setVisibility(View.GONE);
				}

			}
		});
    }

    @Override
    protected void onRestart()
    {
        super.onRestart();
        MetaioDebug.log("ARViewActivity.onRestart SeaMensor");
        //setVpsChecked();
        runOnUiThread(new Runnable() {
			@Override
			public void run() {
				{
					MetaioDebug.log("onRestart: Turning off Waiting Circle");
					mProgress.clearAnimation();
					mProgress.setVisibility(View.GONE);
				}

			}
		});
    }

    @Override
    protected void onPause()
	{
        super.onPause();
		MetaioDebug.log("ARViewActivity.onPause SeaMensor");
        stopLocationUpdates();
		new SaveVpsChecked().execute();
		finish();
	}

    @Override
    public void onStop()
    {
        super.onStop();
        MetaioDebug.log("ARViewActivity.onStop SeaMensor");
        mGoogleApiClient.disconnect();
        MetaioDebug.log("ARViewActivity.onStop SeaMensor: mGoogleApiClient.isConnected()=" + mGoogleApiClient.isConnected());
    }

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		MetaioDebug.log("ARViewActivity.onDestroy SeaMensor");
		mSDKCallback.delete();
		mSDKCallback = null;
	}



	@Override
	protected int getGUILayout() 
	{
		// Attaching layout to the activity
		return R.layout.seamensor; 
	}

	public class SaveVpsChecked extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params) {
			// Saving vpChecked state.
			try {
				// Getting a file path for vps checked config XML file
				DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
				if (!dbxFs.exists(new DbxPath(DbxPath.ROOT, vpsCheckedDropboxPath + vpsCheckedConfigFileDropbox))) {
					DbxFile vpsCheckedConfigFile = dbxFs.create(new DbxPath(DbxPath.ROOT, vpsCheckedDropboxPath + vpsCheckedConfigFileDropbox));
					try {
						XmlSerializer xmlSerializer = Xml.newSerializer();
						StringWriter writer = new StringWriter();
						xmlSerializer.setOutput(writer);
						xmlSerializer.startDocument("UTF-8", true);
						xmlSerializer.text("\n");
						xmlSerializer.startTag("", "VpsChecked");
						xmlSerializer.text("\n");
						for (int i = 0; i < qtyVps; i++) {
							xmlSerializer.text("\t");
							xmlSerializer.startTag("", "Vp");
							xmlSerializer.text("\n");
							xmlSerializer.text("\t");
							xmlSerializer.text("\t");
							xmlSerializer.startTag("", "VpNumber");
							xmlSerializer.text(Short.toString(vpNumber[i]));
							xmlSerializer.endTag("", "VpNumber");
							xmlSerializer.text("\n");
							xmlSerializer.text("\t");
							xmlSerializer.text("\t");
							xmlSerializer.startTag("", "Checked");
							xmlSerializer.text(Boolean.toString(vpChecked[i]));
							xmlSerializer.endTag("", "Checked");
							xmlSerializer.text("\n");
							xmlSerializer.text("\t");
							xmlSerializer.text("\t");
							xmlSerializer.startTag("", "PhotoTakenTimeMillis");
							xmlSerializer.text(Long.toString(photoTakenTimeMillis[i]));
							xmlSerializer.endTag("", "PhotoTakenTimeMillis");
							xmlSerializer.text("\n");
							xmlSerializer.text("\t");
							xmlSerializer.endTag("", "Vp");
							xmlSerializer.text("\n");
						}
						xmlSerializer.endTag("", "VpsChecked");
						xmlSerializer.endDocument();
						vpsCheckedConfigFile.writeString(writer.toString());
					} finally {
						vpsCheckedConfigFile.close();
					}
				} else {
					dbxFs.delete(new DbxPath(DbxPath.ROOT, vpsCheckedDropboxPath + vpsCheckedConfigFileDropbox));
					DbxFile vpsCheckedConfigFile = dbxFs.create(new DbxPath(DbxPath.ROOT, vpsCheckedDropboxPath + vpsCheckedConfigFileDropbox));
					try {
						MetaioDebug.log("Deleting and saving a new vpsCheckedConfigFile to Dropbox");
						XmlSerializer xmlSerializer = Xml.newSerializer();
						StringWriter writer = new StringWriter();
						xmlSerializer.setOutput(writer);
						xmlSerializer.startDocument("UTF-8", true);
						xmlSerializer.text("\n");
						xmlSerializer.startTag("", "VpsChecked");
						xmlSerializer.text("\n");
						for (int i = 0; i < qtyVps; i++) {
							xmlSerializer.text("\t");
							xmlSerializer.startTag("", "Vp");
							xmlSerializer.text("\n");
							xmlSerializer.text("\t");
							xmlSerializer.text("\t");
							xmlSerializer.startTag("", "VpNumber");
							xmlSerializer.text(Short.toString(vpNumber[i]));
							xmlSerializer.endTag("", "VpNumber");
							xmlSerializer.text("\n");
							xmlSerializer.text("\t");
							xmlSerializer.text("\t");
							xmlSerializer.startTag("", "Checked");
							xmlSerializer.text(Boolean.toString(vpChecked[i]));
							xmlSerializer.endTag("", "Checked");
							xmlSerializer.text("\n");
							xmlSerializer.text("\t");
							xmlSerializer.text("\t");
							xmlSerializer.startTag("", "PhotoTakenTimeMillis");
							xmlSerializer.text(Long.toString(photoTakenTimeMillis[i]));
							xmlSerializer.endTag("", "PhotoTakenTimeMillis");
							xmlSerializer.text("\n");
							xmlSerializer.text("\t");
							xmlSerializer.endTag("", "Vp");
							xmlSerializer.text("\n");
						}
						xmlSerializer.endTag("", "VpsChecked");
						xmlSerializer.endDocument();
						vpsCheckedConfigFile.writeString(writer.toString());
					} finally {
						vpsCheckedConfigFile.close();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				MetaioDebug.log(Log.ERROR, "Vps checked state data saving to Dropbox failed, see stack trace");
			}
			return null;
		}
	}


	public void setVpsChecked() 
	{
		//MetaioDebug.log("setVpsChecked: qtyVps="+qtyVps);
		try
		{
			// set the checked state of the vp items
			runOnUiThread(new Runnable()
			{
				@Override
				public void run() 
				{
					for (int i=0; i<(qtyVps); i++)
					{
                        //MetaioDebug.log("setVpsChecked: vpChecked["+i+"]="+vpChecked[i]);
                        if (listView != null)
						{
							listView.setItemChecked(i, vpChecked[i]);
						}
						else MetaioDebug.log("setVpsChecked: listView="+listView);
					}
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
			MetaioDebug.log(Log.ERROR, "setVpsChecked failed, see stack trace");	
		}
	}


    public String[] getGPSToExif(Location l)
    {
        String[] gpsString = new String[7];
		boolean usingLastLocation = false;
        try
        {
            /*
            try
            {
                l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Log.e("GPSTOEXIF","Error getLastKnownLocation(lm.GPS_PROVIDER)");
                MetaioDebug.log("Error getLastKnownLocation(lm.GPS_PROVIDER)");
            }
            */
            if (l==null)
            {
                if (mGoogleApiClient.isConnected())
                {
                    l=LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
					if (l != null) usingLastLocation = true;
                }
            }
            double[] gps = new double[2];
            if (l != null)
            {
                gps[0] = l.getLatitude();
                gps[1] = l.getLongitude();

                if (gps[0]<0)
                {
                    gpsString[1]="S";
                    gps[0]=(-1)*gps[0];
                }
                else
                {
                    gpsString[1]="N";
                }
                if (gps[1]<0)
                {
                    gpsString[3]="W";
                    gps[1]=(-1)*gps[1];
                }
                else
                {
                    gpsString[3]="E";
                }
                long latDegInteger = (long) (gps[0] - (gps[0] % 1));
                long latMinInteger = (long) ((60*(gps[0]-latDegInteger))-((60*(gps[0]-latDegInteger)) % 1));
                long latSecInteger = (long) (((60*(gps[0]-latDegInteger)) % 1)*60*1000);
                gpsString[0]=""+latDegInteger+"/1,"+latMinInteger+"/1,"+latSecInteger+"/1000";

                long lonDegInteger = (long) (gps[1] - (gps[1] % 1));
                long lonMinInteger = (long) ((60*(gps[1]-lonDegInteger))-((60*(gps[1]-lonDegInteger)) % 1));
                long lonSecInteger = (long) (((60*(gps[1]-lonDegInteger)) % 1)*60*1000);
                gpsString[2]=""+lonDegInteger+"/1,"+lonMinInteger+"/1,"+lonSecInteger+"/1000";
				gpsString[0]= Double.toString(gps[0]);
				gpsString[1]= Double.toString(gps[1]);
				gpsString[4]=Float.toString(l.getAccuracy());
				gpsString[5]=mLastUpdateTime;
				gpsString[6]=l.getProvider();
				if (usingLastLocation) gpsString[6]="getLastLocation";
                MetaioDebug.log("getGPSToExif: LAT:"+gps[0]+" "+(gps[0] % 1)+" "+gpsString[0]+gpsString[1]+" LON:"+gps[1]+" "+gpsString[2]+gpsString[3]);
            }
            else
            {
                gpsString[0] = " ";
                gpsString[1] = " ";
                gpsString[2] = " ";
                gpsString[3] = " ";
				gpsString[4] = " ";
				gpsString[5] = " ";
				gpsString[6] = " ";
            }
			for (int index = 0; index<7; index++)
			{
				if (gpsString[index]==null) gpsString[index]=" ";
				MetaioDebug.log("getGPSToExif: gpsString[index]="+gpsString[index]);
			}

        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "getGPSToExif: failed, see stack trace");
        }
        return gpsString;
    }


	private void checkPositionToTarget(TrackingValuesVector poses, final int i)
	{
		mTrackingValues = metaioSDK.getTrackingValues(poses.get(i).getCoordinateSystemID());
        trackingQuality = mTrackingValues.getQuality();
		if (mTrackingValues.isTrackingState())
		{
			cameraToCOS = mTrackingValues.getTranslation();
			Rotation cameraRotation = mTrackingValues.getRotation();
			cameraEulerAngle = cameraRotation.getEulerAngleDegrees();
			final float posX = cameraToCOS.getX()*(-1);
			final float posY = cameraToCOS.getY()*(-1);
			final float posZ = cameraToCOS.getZ();
			final float rotX = cameraEulerAngle.getX();
			final float rotY = cameraEulerAngle.getY();
			final float rotZ = cameraEulerAngle.getZ();

            if (debugMode)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        xPosView.setText("pX="+Float.toString(Math.round(posX)));
                        xPosView.setVisibility(View.VISIBLE);
                        yPosView.setText("pY="+Float.toString(Math.round(posY)));
                        yPosView.setVisibility(View.VISIBLE);
                        zPosView.setText("pZ="+Float.toString(Math.round(posZ)));
                        zPosView.setVisibility(View.VISIBLE);
                        xRotView.setText("rX="+Float.toString(Math.round(rotX)));
                        xRotView.setVisibility(View.VISIBLE);
                        yRotView.setText("rY="+Float.toString(Math.round(rotY)));
                        yRotView.setVisibility(View.VISIBLE);
                        zRotView.setText("rZ="+Float.toString(Math.round(rotZ)));
                        zRotView.setVisibility(View.VISIBLE);
                        trkQualityView.setText("qL="+Float.toString(Math.round(trackingQuality)));
                        trkQualityView.setVisibility(View.VISIBLE);
                    }
                });

            }

            if (!vpIsSuperSingle[coordinateSystemTrackedInPoseI - 1])
            {
                inPosition = (  (Math.abs(posX - 0) <= tolerancePosition) &&
                                        (Math.abs(posY - 0) <= tolerancePosition) &&
                                        (Math.abs(posZ - vpZCameraDistance[coordinateSystemTrackedInPoseI - 1]) <= tolerancePosition));
                inRotation = (  (Math.abs(rotX - 0) <= toleranceRotation) &&
                                        (Math.abs(rotY - 0) <= toleranceRotation) &&
                                        (Math.abs(rotZ - 0) <= toleranceRotation));
            }
            else
            {
                inPosition = ((Math.abs(posX - vpXCameraDistance[coordinateSystemTrackedInPoseI - 1]) <= (tolerancePosition)) &&
                        (Math.abs(posY - vpYCameraDistance[coordinateSystemTrackedInPoseI - 1]) <= (tolerancePosition)) &&
                        (Math.abs(posZ - vpZCameraDistance[coordinateSystemTrackedInPoseI - 1]) <= (tolerancePosition)));
                /*
                inRotation = (  (Math.abs(rotX - 0) <= (toleranceRotation*2)) &&
                                (Math.abs(rotY - 0) <= (toleranceRotation*2)) &&
                                (Math.abs(rotZ - 0) <= (toleranceRotation*2)));
                */
                inRotation = ((Math.abs(rotX - vpXCameraRotation[coordinateSystemTrackedInPoseI - 1]) <= (toleranceRotation)) &&
                        (Math.abs(rotY - vpYCameraRotation[coordinateSystemTrackedInPoseI - 1]) <= (toleranceRotation)) &&
                        (Math.abs(rotZ - vpZCameraRotation[coordinateSystemTrackedInPoseI - 1]) <= (toleranceRotation)));
            }
			// Setting attitude indicators
			// Sway indicator
			if (posX>(vpXCameraDistance[coordinateSystemTrackedInPoseI-1]+tolerancePosition))
			{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					swayLeftImageView.setVisibility(View.VISIBLE);
					swayRightImageView.setVisibility(View.GONE);
					swayLeftImageView.setImageDrawable(drawableSwayLeft);
				}
				});
			}
			else
				if (posX<(vpXCameraDistance[coordinateSystemTrackedInPoseI-1]-tolerancePosition))
				{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						swayRightImageView.setVisibility(View.VISIBLE);
						swayLeftImageView.setVisibility(View.GONE);
						swayRightImageView.setImageDrawable(drawableSwayRight);
					}
					});
				}
				else
				{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						swayRightImageView.setVisibility(View.GONE);
						swayLeftImageView.setVisibility(View.GONE);
					}
					});
				}
			// Heave indicator
			if (posY>(vpYCameraDistance[coordinateSystemTrackedInPoseI-1]+tolerancePosition))
			{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					heaveDownImageView.setVisibility(View.VISIBLE);
					heaveUpImageView.setVisibility(View.GONE);
					heaveDownImageView.setImageDrawable(drawableHeaveDown);
				}
				});
			}
			else
				if (posY<(vpYCameraDistance[coordinateSystemTrackedInPoseI-1]-tolerancePosition))
				{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						heaveUpImageView.setVisibility(View.VISIBLE);
						heaveDownImageView.setVisibility(View.GONE);
						heaveUpImageView.setImageDrawable(drawableHeaveUp);
					}
					});
				}
				else
				{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						heaveUpImageView.setVisibility(View.GONE);
						heaveDownImageView.setVisibility(View.GONE);
					}
					});
				}
			// Surge indicator
			// Surge indicator - Range 1
			if ((posZ<(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]-tolerancePosition))&&
				(posZ>(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]-(2*tolerancePosition))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					surgeImageView.setVisibility(View.VISIBLE);
					surgeImageView.setImageDrawable(drawableSurgeB1);
				}
				});
			}
			else
				if ((posZ>(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]+tolerancePosition))&&
					(posZ<(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]+(2*tolerancePosition))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					surgeImageView.setVisibility(View.VISIBLE);
					surgeImageView.setImageDrawable(drawableSurgeA1);
				}
				});
				}
			
			// Surge indicator - Range 2			
			if ((posZ<(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]-(2*tolerancePosition)))&&
				(posZ>(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]-(3*tolerancePosition))))
					{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						surgeImageView.setVisibility(View.VISIBLE);
						surgeImageView.setImageDrawable(drawableSurgeB2);
					}
					});
				}
				else
					if ((posZ>(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]+(2*tolerancePosition)))&&
						(posZ<(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]+(3*tolerancePosition))))
					{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						surgeImageView.setVisibility(View.VISIBLE);
						surgeImageView.setImageDrawable(drawableSurgeA2);
					}
					});
					}

			// Surge indicator - Range 3			
			if (posZ<(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]-(3*tolerancePosition)))
					{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						surgeImageView.setVisibility(View.VISIBLE);
						surgeImageView.setImageDrawable(drawableSurgeB3);
					}
					});
				}
				else
					if (posZ>(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]+(3*tolerancePosition)))
					{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						surgeImageView.setVisibility(View.VISIBLE);
						surgeImageView.setImageDrawable(drawableSurgeA3);
					}
					});
					}
			
			// Surge indicator - Center			
			if ((posZ>=(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]-tolerancePosition))&&
				(posZ<=(vpZCameraDistance[coordinateSystemTrackedInPoseI-1]+tolerancePosition)))
					{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						surgeImageView.setVisibility(View.GONE);

					}
					});
				}

			// Yaw indicator
			// Yaw indicator - Center
			if ((rotY>=(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]-toleranceRotation))&&
				(rotY<=(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]+toleranceRotation)))
					{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						yawImageView.setVisibility(View.GONE);

					}
					});
				}
			// Yaw indicator - Range 1
			if ((rotY<(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]-toleranceRotation))&&
				(rotY>(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]-(2*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					yawImageView.setVisibility(View.VISIBLE);
					yawImageView.setImageDrawable(drawableYawL1);
				}
				});
			}
			else
				if ((rotY>(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]+toleranceRotation))&&
					(rotY<(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]+(2*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					yawImageView.setVisibility(View.VISIBLE);
					yawImageView.setImageDrawable(drawableYawR1);
				}
				});
				}
			// Yaw indicator - Range 2
			if ((rotY<(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]-(2*toleranceRotation)))&&
				(rotY>(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]-(3*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					yawImageView.setVisibility(View.VISIBLE);
					yawImageView.setImageDrawable(drawableYawL2);
				}
				});
			}
			else
				if ((rotY>(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]+(2*toleranceRotation)))&&
					(rotY<(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]+(3*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					yawImageView.setVisibility(View.VISIBLE);
					yawImageView.setImageDrawable(drawableYawR2);
				}
				});
				}

			// Yaw indicator - Range 3
			if ((rotY<(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]-(3*toleranceRotation)))&&
				(rotY>(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]-(4*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					yawImageView.setVisibility(View.VISIBLE);
					yawImageView.setImageDrawable(drawableYawL3);
				}
				});
			}
			else
				if ((rotY>(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]+(3*toleranceRotation)))&&
					(rotY<(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]+(4*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					yawImageView.setVisibility(View.VISIBLE);
					yawImageView.setImageDrawable(drawableYawR3);
				}
				});
				}
			// Yaw indicator - Range 4
			if (rotY<(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]-(4*toleranceRotation)))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					yawImageView.setVisibility(View.VISIBLE);
					yawImageView.setImageDrawable(drawableYawL4);
				}
				});
			}
			else
				if (rotY>(vpYCameraRotation[coordinateSystemTrackedInPoseI-1]+(4*toleranceRotation)))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					yawImageView.setVisibility(View.VISIBLE);
					yawImageView.setImageDrawable(drawableYawR4);
				}
				});
				}
			
			// Pitch indicator
			// Pitch indicator - Center
			if ((rotX>=(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]-toleranceRotation))&&
				(rotX<=(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]+toleranceRotation)))
					{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						pitchImageView.setVisibility(View.GONE);

					}
					});
				}
			// Pitch indicator - Range 1
			if ((rotX<(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]-toleranceRotation))&&
				(rotX>(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]-(2*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					pitchImageView.setVisibility(View.VISIBLE);
					pitchImageView.setImageDrawable(drawablePitchH1);
				}
				});
			}
			else
				if ((rotX>(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]+toleranceRotation))&&
					(rotX<(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]+(2*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					pitchImageView.setVisibility(View.VISIBLE);
					pitchImageView.setImageDrawable(drawablePitchL1);
				}
				});
				}
			// Pitch indicator - Range 2
			if ((rotX<(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]-(2*toleranceRotation)))&&
				(rotX>(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]-(3*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					pitchImageView.setVisibility(View.VISIBLE);
					pitchImageView.setImageDrawable(drawablePitchH2);
				}
				});
			}
			else
				if ((rotX>(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]+(2*toleranceRotation)))&&
					(rotX<(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]+(3*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					pitchImageView.setVisibility(View.VISIBLE);
					pitchImageView.setImageDrawable(drawablePitchL2);
				}
				});
				}

			// Pitch indicator - Range 3
			if ((rotX<(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]-(3*toleranceRotation)))&&
				(rotX>(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]-(4*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					pitchImageView.setVisibility(View.VISIBLE);
					pitchImageView.setImageDrawable(drawablePitchH3);
				}
				});
			}
			else
				if ((rotX>(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]+(3*toleranceRotation)))&&
					(rotX<(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]+(4*toleranceRotation))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					pitchImageView.setVisibility(View.VISIBLE);
					pitchImageView.setImageDrawable(drawablePitchL3);
				}
				});
				}
			// Pitch indicator - Range 4
			if (rotX<(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]-(4*toleranceRotation)))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					pitchImageView.setVisibility(View.VISIBLE);
					pitchImageView.setImageDrawable(drawablePitchH4);
				}
				});
			}
			else
				if (rotX>(vpXCameraRotation[coordinateSystemTrackedInPoseI-1]+(4*toleranceRotation)))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					pitchImageView.setVisibility(View.VISIBLE);
					pitchImageView.setImageDrawable(drawablePitchL4);
				}
				});
				}
			
			// Roll indicator
			// Roll indicator - Center
			if ((rotZ>=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]-toleranceRotation))&&
				(rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+toleranceRotation)))
					{
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
						rollCImageView.setVisibility(View.GONE);
						roll01ImageView.setVisibility(View.GONE);
						roll02ImageView.setVisibility(View.GONE);
						roll03ImageView.setVisibility(View.GONE);
						roll04ImageView.setVisibility(View.GONE);
						roll05ImageView.setVisibility(View.GONE);
						roll06ImageView.setVisibility(View.GONE);
						roll07ImageView.setVisibility(View.GONE);
						roll08ImageView.setVisibility(View.GONE);
						roll09ImageView.setVisibility(View.GONE);
						roll10ImageView.setVisibility(View.GONE);
						roll11ImageView.setVisibility(View.GONE);
						roll12ImageView.setVisibility(View.GONE);
						roll13ImageView.setVisibility(View.GONE);
						roll14ImageView.setVisibility(View.GONE);
						roll15ImageView.setVisibility(View.GONE);
					}
					});
				}
			// Roll indicator - Range 1
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+toleranceRotation))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(32.5f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll01ImageView.setVisibility(View.VISIBLE);
					roll01ImageView.setImageDrawable(drawableRoll01);
					
					rollCImageView.setVisibility(View.GONE);
					//roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 2
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(32.5f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(55f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll02ImageView.setVisibility(View.VISIBLE);
					roll02ImageView.setImageDrawable(drawableRoll02);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					//roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 3
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(55f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(77.5f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll03ImageView.setVisibility(View.VISIBLE);
					roll03ImageView.setImageDrawable(drawableRoll03);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					//roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 4
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(77.5f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(100f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll04ImageView.setVisibility(View.VISIBLE);
					roll04ImageView.setImageDrawable(drawableRoll04);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					//roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 5
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(100f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(122.5f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll05ImageView.setVisibility(View.VISIBLE);
					roll05ImageView.setImageDrawable(drawableRoll05);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					//roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 6
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(122.5f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(145f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll06ImageView.setVisibility(View.VISIBLE);
					roll06ImageView.setImageDrawable(drawableRoll06);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					//roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 7
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(145f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(167.5f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll07ImageView.setVisibility(View.VISIBLE);
					roll07ImageView.setImageDrawable(drawableRoll07);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					//roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 8
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(167.5f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(180f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll08ImageView.setVisibility(View.VISIBLE);
					roll08ImageView.setImageDrawable(drawableRoll08);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					//roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 9
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-180f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-147.5f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll09ImageView.setVisibility(View.VISIBLE);
					roll09ImageView.setImageDrawable(drawableRoll09);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					//roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 10
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-147.5f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-125f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll10ImageView.setVisibility(View.VISIBLE);
					roll10ImageView.setImageDrawable(drawableRoll10);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					//roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 11
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-125f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-102.5f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll11ImageView.setVisibility(View.VISIBLE);
					roll11ImageView.setImageDrawable(drawableRoll11);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					//roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 12
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-102.5f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-80f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll12ImageView.setVisibility(View.VISIBLE);
					roll12ImageView.setImageDrawable(drawableRoll12);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					//roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 13
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-80f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-57.5f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll13ImageView.setVisibility(View.VISIBLE);
					roll13ImageView.setImageDrawable(drawableRoll13);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					//roll13ImageView.setVisibility(View.GONE);
					roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 14
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-57.5f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-35f))))
				{
				runOnUiThread(new Runnable()
				{
				@Override
				public void run() 
				{
					roll14ImageView.setVisibility(View.VISIBLE);
					roll14ImageView.setImageDrawable(drawableRoll14);
					
					rollCImageView.setVisibility(View.GONE);
					roll01ImageView.setVisibility(View.GONE);
					roll02ImageView.setVisibility(View.GONE);
					roll03ImageView.setVisibility(View.GONE);
					roll04ImageView.setVisibility(View.GONE);
					roll05ImageView.setVisibility(View.GONE);
					roll06ImageView.setVisibility(View.GONE);
					roll07ImageView.setVisibility(View.GONE);
					roll08ImageView.setVisibility(View.GONE);
					roll09ImageView.setVisibility(View.GONE);
					roll10ImageView.setVisibility(View.GONE);
					roll11ImageView.setVisibility(View.GONE);
					roll12ImageView.setVisibility(View.GONE);
					roll13ImageView.setVisibility(View.GONE);
					//roll14ImageView.setVisibility(View.GONE);
					roll15ImageView.setVisibility(View.GONE);
				}
				});
				}
			// Roll indicator - Range 15
			if ((rotZ>(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-35f)))&&
			   (rotZ<=(vpZCameraRotation[coordinateSystemTrackedInPoseI-1]+(-10f))))
				{
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						roll15ImageView.setVisibility(View.VISIBLE);
						roll15ImageView.setImageDrawable(drawableRoll15);

						rollCImageView.setVisibility(View.GONE);
						roll01ImageView.setVisibility(View.GONE);
						roll02ImageView.setVisibility(View.GONE);
						roll03ImageView.setVisibility(View.GONE);
						roll04ImageView.setVisibility(View.GONE);
						roll05ImageView.setVisibility(View.GONE);
						roll06ImageView.setVisibility(View.GONE);
						roll07ImageView.setVisibility(View.GONE);
						roll08ImageView.setVisibility(View.GONE);
						roll09ImageView.setVisibility(View.GONE);
						roll10ImageView.setVisibility(View.GONE);
						roll11ImageView.setVisibility(View.GONE);
						roll12ImageView.setVisibility(View.GONE);
						roll13ImageView.setVisibility(View.GONE);
						roll14ImageView.setVisibility(View.GONE);
						//roll15ImageView.setVisibility(View.GONE);
					}
				});
				}

			if ((inPosition) && (inRotation) && (!waitingForMarkerlessTrackingConfigurationToLoad) && (!vpPhotoRequestInProgress))
			{
				MetaioDebug.log("checkPositionToTarget: Camera inPosition && inRotation: i="+i+"?="+((!vpIsAmbiguous[coordinateSystemTrackedInPoseI-1]) || ((vpIsAmbiguous[coordinateSystemTrackedInPoseI-1])&&(vpIsDisambiguated)&&(doubleCheckingProcedureFinalized))));
				if ((vpIsAmbiguous[coordinateSystemTrackedInPoseI-1]) && (!doubleCheckingProcedureFinalized))
					{
						if (metaioSDK.setTrackingConfiguration(idMarkersTrackingConfigFileContents, false))
						{
							MetaioDebug.log("checkPositionToTarget: ambiguous VP detected - In Double Checking Procedure - init Disambiguation procedure - Tracking with ID Marker");
							doubleCheckingProcedureStarted = true;
						}
					}

				if ((!vpIsAmbiguous[coordinateSystemTrackedInPoseI-1]) || ((vpIsAmbiguous[coordinateSystemTrackedInPoseI-1])&&(vpIsDisambiguated)&&(doubleCheckingProcedureFinalized)))
					{
						inFocus = false;
						inFocus = true;
						runOnUiThread(new Runnable()
							{
							@Override
							public void run() 
							{
								// turning the target to green
								targetImageView.setImageDrawable(drawableTargetGreen);
							}
							});
						Camera camera = IMetaioSDKAndroid.getCamera(this);
				        Camera.Parameters params = camera.getParameters();
                        String initialFocusMode = params.getFocusMode();
                        params.setSceneMode("steadyphoto");
                        params.setFocusMode("continuous-video");
                        //params.setSceneMode("sports");
                        camera.setParameters(params);
                        AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {
							  @Override
							  public void onAutoFocus(boolean success, Camera camera) 
							  	{
								  inFocus = true;
							  	}
							};
				        camera.autoFocus(autoFocusCallback);
                        camera.setParameters(params);

						if (inFocus)
						{
							vpPhotoRequestInProgress = true;
							MetaioDebug.log("checkPositionToTarget: vpPhotoRequestInProgress = "+vpPhotoRequestInProgress);
							metaioSDK.requestCameraImage();
							MetaioDebug.log("inFocus = "+inFocus);
							inFocus = false;
                            float focus[] = new float[3];
                            IMetaioSDKAndroid.getCamera(this).getParameters().getFocusDistances(focus);
                            if (camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH))
                            {
                                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                camera.setParameters(params);
                                torchModeOn = false;
                            }
                            params.setFocusMode(initialFocusMode);
                            camera.setParameters(params);
                            //MetaioDebug.log("Depois da Espera: Pos = "+cameraToCOS+"  "+cameraToCOS.norm());

                            MetaioDebug.log("checkPositionToTarget: FOCUS_DISTANCE_NEAR_INDEX: "+focus[0]+"m");
                            MetaioDebug.log("checkPositionToTarget: FOCUS_DISTANCE_OPTIMAL_INDEX: "+focus[1]+"m");
                            MetaioDebug.log("checkPositionToTarget: FOCUS_DISTANCE_FAR_INDEX: "+focus[2]+"m");
                            MetaioDebug.log("checkPositionToTarget: Focal Length: "+IMetaioSDKAndroid.getCamera(this).getParameters().getFocalLength()+"mm");
                            MetaioDebug.log("checkPositionToTarget: DIF Pos:X="+(Math.abs(posX-vpXCameraDistance[coordinateSystemTrackedInPoseI-1]))+
                                    " Y="+(Math.abs(posY-vpYCameraDistance[coordinateSystemTrackedInPoseI-1]))+
                                    " Z="+(Math.abs(posZ-vpZCameraDistance[coordinateSystemTrackedInPoseI-1]))+
                                        " Rot:X="+((Math.abs(rotX-vpXCameraRotation[coordinateSystemTrackedInPoseI-1]))+" Y="+
                                        (Math.abs(rotY-vpYCameraRotation[coordinateSystemTrackedInPoseI-1]))+" Z="+
                                        (Math.abs(rotZ-vpZCameraRotation[coordinateSystemTrackedInPoseI-1]))));
                            MetaioDebug.log("checkPositionToTarget: Pos:z="+ posZ);

                            runOnUiThread(new Runnable()
                            {

                                @Override
                                public void run()
                                {
                                    vpChecked[(coordinateSystemTrackedInPoseI-1)] = true;
                                    setVpsChecked();
                                    mSeaMensorCube.setVisible(false);
                                    mVpChecked.setVisible(true);
                                    // TURNING OFF THE ATTITUDE INDICATOR SYSTEM
                                    targetImageView.setVisibility(View.GONE);
                                    swayRightImageView.setVisibility(View.GONE);
                                    swayLeftImageView.setVisibility(View.GONE);
                                    heaveUpImageView.setVisibility(View.GONE);
                                    heaveDownImageView.setVisibility(View.GONE);
                                    surgeImageView.setVisibility(View.GONE);
                                    yawImageView.setVisibility(View.GONE);
                                    pitchImageView.setVisibility(View.GONE);
                                    rollCImageView.setVisibility(View.GONE);
                                    roll01ImageView.setVisibility(View.GONE);
                                    roll02ImageView.setVisibility(View.GONE);
                                    roll03ImageView.setVisibility(View.GONE);
                                    roll04ImageView.setVisibility(View.GONE);
                                    roll05ImageView.setVisibility(View.GONE);
                                    roll06ImageView.setVisibility(View.GONE);
                                    roll07ImageView.setVisibility(View.GONE);
                                    roll08ImageView.setVisibility(View.GONE);
                                    roll09ImageView.setVisibility(View.GONE);
                                    roll10ImageView.setVisibility(View.GONE);
                                    roll11ImageView.setVisibility(View.GONE);
                                    roll12ImageView.setVisibility(View.GONE);
                                    roll13ImageView.setVisibility(View.GONE);
                                    roll14ImageView.setVisibility(View.GONE);
                                    roll15ImageView.setVisibility(View.GONE);
                                    if (radarScanImageView.isShown())
                                    {
                                        radarScanImageView.clearAnimation();
                                        radarScanImageView.setVisibility(View.GONE);
                                    }
                                    MetaioDebug.log("checkPositionToTarget: ON UI THREAD: mVpChecked.setVisible(true)");
                                    if (imageView.isShown())
                                    {
                                        showingVpLocationPhoto = false;
                                        vpLocationDesTextView.setVisibility(View.GONE);
                                        vpIdNumber.setVisibility(View.GONE);
                                        imageView.setVisibility(View.GONE);
                                        okButton.setVisibility(View.GONE);
                                        alphaToggleButton.setVisibility(View.GONE);
                                        flashTorchVpToggleButton.setVisibility(View.GONE);
                                        exitButton.setVisibility(View.VISIBLE);
                                        listView.setVisibility(View.VISIBLE);
                                    }
                                }
                            });

                        }

					}

			}
		}
		return;
	}
		
	@Override
	public void onDrawFrame() 
	{
		super.onDrawFrame();
		if (metaioSDK != null)
		{
			//MetaioDebug.log("onDrawFrame: isBackgroundProcessingEnabled="+metaioSDK.isBackgroundProcessingEnabled());
			verifyVpsChecked();
            if (etsInitializedAndNotFound)
            {
                if (!radarScanImageView.isShown())
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            radarScanImageView.setVisibility(View.VISIBLE);
                            radarScanImageView.startAnimation(rotationRadarScan);
                            // TURNING OFF THE ATTITUDE INDICATOR SYSTEM
                            targetImageView.setVisibility(View.GONE);
                            swayRightImageView.setVisibility(View.GONE);
                            swayLeftImageView.setVisibility(View.GONE);
                            heaveUpImageView.setVisibility(View.GONE);
                            heaveDownImageView.setVisibility(View.GONE);
                            surgeImageView.setVisibility(View.GONE);
                            yawImageView.setVisibility(View.GONE);
                            pitchImageView.setVisibility(View.GONE);
                            rollCImageView.setVisibility(View.GONE);
                            roll01ImageView.setVisibility(View.GONE);
                            roll02ImageView.setVisibility(View.GONE);
                            roll03ImageView.setVisibility(View.GONE);
                            roll04ImageView.setVisibility(View.GONE);
                            roll05ImageView.setVisibility(View.GONE);
                            roll06ImageView.setVisibility(View.GONE);
                            roll07ImageView.setVisibility(View.GONE);
                            roll08ImageView.setVisibility(View.GONE);
                            roll09ImageView.setVisibility(View.GONE);
                            roll10ImageView.setVisibility(View.GONE);
                            roll11ImageView.setVisibility(View.GONE);
                            roll12ImageView.setVisibility(View.GONE);
                            roll13ImageView.setVisibility(View.GONE);
                            roll14ImageView.setVisibility(View.GONE);
                            roll15ImageView.setVisibility(View.GONE);
                        }
                    });
                }
                elapsedMillisSinceEtsWasInitialized = System.currentTimeMillis() - millisWhenEtsWasInitialized;
                //MetaioDebug.log("oTE: *** oDF: "+elapsedMillisSinceEtsWasInitialized);
                if (elapsedMillisSinceEtsWasInitialized>2000)
                {
                    MetaioDebug.log("oTE: *** oDF:onDrawFrame: etsInitializedAndNotFound = "+etsInitializedAndNotFound+" elapsed millis ="+elapsedMillisSinceEtsWasInitialized+" CALLING setMarkerlessTrackingConfiguration(markerlessTrackingConfigFileContents)");
                    etsInitializedAndNotFound = false;
                    setMarkerlessTrackingConfiguration(markerlesstrackingConfigFileContents);
                }
            }
            Camera.Parameters params = null;
			Camera camera = IMetaioSDKAndroid.getCamera(this);
			if (camera != null)
			{
				params = camera.getParameters();
			}
            // get all detected poses/targets
			TrackingValuesVector poses = metaioSDK.getTrackingValues();
			//if we have detected one, verify if it is in the ambiguous list and then attach our 3d model to this coordinate system Id

			if (poses.size() != 0)
            {
                for (im = 0; im < poses.size(); im++)
                {
                    final TrackingValues tv = poses.get(im);
                    if (tv.isTrackingState())
                    {
                        if ((!vpIsAmbiguous[coordinateSystemTrackedInPoseI - 1]) || ((vpIsAmbiguous[coordinateSystemTrackedInPoseI - 1]) && (vpIsDisambiguated)) || (waitingToCaptureVpAfterDisambiguationProcedureSuccessful))
                        {
                            if (camera != null)
							{
								if ((vpFlashTorchIsOn[coordinateSystemTrackedInPoseI - 1]) && (camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)))
								{
									params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
									camera.setParameters(params);
								}
								if ((!vpFlashTorchIsOn[coordinateSystemTrackedInPoseI - 1]) && (camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)))
								{
									params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
									camera.setParameters(params);
									torchModeOn = false;
								}
							}
                            if (!vpChecked[coordinateSystemTrackedInPoseI - 1])
                            {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // TURNING OFF RADAR SCAN
                                        radarScanImageView.clearAnimation();
                                        radarScanImageView.setVisibility(View.GONE);
                                        // lighting up the crosshair to aim at the seamensor cube
                                        targetImageView.setImageDrawable(drawableTargetWhite);
                                        targetImageView.setVisibility(View.VISIBLE);
                                    }
                                });
                                if (mVpChecked.isVisible()) mVpChecked.setVisible(false);
                                if (vpIsSuperSingle[coordinateSystemTrackedInPoseI-1])
                                {
                                    if (vpSuperIdIs20mm[coordinateSystemTrackedInPoseI-1]) modelScale=4f;
                                    if (vpSuperIdIs100mm[coordinateSystemTrackedInPoseI-1]) modelScale=8f;
                                }
                                else
                                {
                                    modelScale=50f;
                                }
                                //MetaioDebug.log("OnDrawFrame: vpIsSuperSingle["+(coordinateSystemTrackedInPoseI-1)+"]="+vpIsSuperSingle[coordinateSystemTrackedInPoseI-1]+" 20mm="+vpSuperIdIs20mm[coordinateSystemTrackedInPoseI-1]+" 100mm="+vpSuperIdIs100mm[coordinateSystemTrackedInPoseI-1]+" - modelScale= "+modelScale);
                                mSeaMensorCube.setScale(new Vector3d(modelScale, modelScale, modelScale), false);
                                mSeaMensorCube.setTranslation(new Vector3d(0f, 0f, 0f), false);
                                mSeaMensorCube.setVisible(true);
                                mSeaMensorCube.setCoordinateSystemID(poses.get(im).getCoordinateSystemID());
                                if (!waitingForMarkerlessTrackingConfigurationToLoad) checkPositionToTarget(poses, im);
                            }
                            else
                            {
                                mVpChecked.setTranslation(new Vector3d(0f, 0f, 0f), false);
                                mVpChecked.setVisible(true);
                                mVpChecked.setCoordinateSystemID(poses.get(im).getCoordinateSystemID());
                                //MetaioDebug.log("INSIDE FIRST IF CLAUSE - OnDrawFrame POSES LOST Poses.size = "+poses.size()+" mVpChecked.setVisible(true)");
                                // TURNING OFF THE ATTITUDE INDICATOR SYSTEM
                                targetImageView.setVisibility(View.GONE);
                                swayRightImageView.setVisibility(View.GONE);
                                swayLeftImageView.setVisibility(View.GONE);
                                heaveUpImageView.setVisibility(View.GONE);
                                heaveDownImageView.setVisibility(View.GONE);
                                surgeImageView.setVisibility(View.GONE);
                                yawImageView.setVisibility(View.GONE);
                                pitchImageView.setVisibility(View.GONE);
                                rollCImageView.setVisibility(View.GONE);
                                roll01ImageView.setVisibility(View.GONE);
                                roll02ImageView.setVisibility(View.GONE);
                                roll03ImageView.setVisibility(View.GONE);
                                roll04ImageView.setVisibility(View.GONE);
                                roll05ImageView.setVisibility(View.GONE);
                                roll06ImageView.setVisibility(View.GONE);
                                roll07ImageView.setVisibility(View.GONE);
                                roll08ImageView.setVisibility(View.GONE);
                                roll09ImageView.setVisibility(View.GONE);
                                roll10ImageView.setVisibility(View.GONE);
                                roll11ImageView.setVisibility(View.GONE);
                                roll12ImageView.setVisibility(View.GONE);
                                roll13ImageView.setVisibility(View.GONE);
                                roll14ImageView.setVisibility(View.GONE);
                                roll15ImageView.setVisibility(View.GONE);
                            }
                        }
                    }
                         /*
                    else
                    {
                        if (notTracking)
                        {
                            MetaioDebug.log("onDrawFrame - NOT TRACKING -  ");
                            if ((camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) && (!torchModeOn))
                            {
                                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                camera.setParameters(params);
                            }

                            //MetaioDebug.log("OnDrawFrame: POSES LOST Poses.size=" + poses.size() + " Turning HUD OFF");

                            if (targetImageView.isShown())
                            {
                                //MetaioDebug.log("OnDrawFrame POSES LOST Poses.size = "+poses.size()+" Turning HUD OFF");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        // TURNING OFF THE ATTITUDE INDICATOR SYSTEM
                                        targetImageView.setVisibility(View.GONE);
                                        swayRightImageView.setVisibility(View.GONE);
                                        swayLeftImageView.setVisibility(View.GONE);
                                        heaveUpImageView.setVisibility(View.GONE);
                                        heaveDownImageView.setVisibility(View.GONE);
                                        surgeImageView.setVisibility(View.GONE);
                                        yawImageView.setVisibility(View.GONE);
                                        pitchImageView.setVisibility(View.GONE);
                                        rollCImageView.setVisibility(View.GONE);
                                        roll01ImageView.setVisibility(View.GONE);
                                        roll02ImageView.setVisibility(View.GONE);
                                        roll03ImageView.setVisibility(View.GONE);
                                        roll04ImageView.setVisibility(View.GONE);
                                        roll05ImageView.setVisibility(View.GONE);
                                        roll06ImageView.setVisibility(View.GONE);
                                        roll07ImageView.setVisibility(View.GONE);
                                        roll08ImageView.setVisibility(View.GONE);
                                        roll09ImageView.setVisibility(View.GONE);
                                        roll10ImageView.setVisibility(View.GONE);
                                        roll11ImageView.setVisibility(View.GONE);
                                        roll12ImageView.setVisibility(View.GONE);
                                        roll13ImageView.setVisibility(View.GONE);
                                        roll14ImageView.setVisibility(View.GONE);
                                        roll15ImageView.setVisibility(View.GONE);
                                        // TURNING ON THE RADAR SCAN
                                        if (!showingVpLocationPhoto) {
                                            radarScanImageView.setVisibility(View.VISIBLE);
                                            radarScanImageView.startAnimation(rotationRadarScan);
                                        }
                                    }
                                });
                            }



                        }

                    }
                     */
                }
            }
		}
	}	

	public void onButtonClick(View v) 
	{
		if (v.getId() == R.id.button1)
			{
            if (back_pressed + 2000 > System.currentTimeMillis())
            {
                MetaioDebug.log("Closing ARViewactivity");
                finish();
            }
            else
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(getBaseContext(), getString(R.string.button_exit), Toast.LENGTH_SHORT).show();
                    }
                });
            back_pressed = System.currentTimeMillis();
			}
		if (v.getId() == R.id.button2)
			{
			MetaioDebug.log("Closing VPx location photo");
			showingVpLocationPhoto = false;
			vpLocationDesTextView.setVisibility(View.GONE);
			vpIdNumber.setVisibility(View.GONE);
			imageView.setVisibility(View.GONE);
			okButton.setVisibility(View.GONE);
			alphaToggleButton.setVisibility(View.GONE);
            flashTorchVpToggleButton.setVisibility(View.GONE);
			showPreviousVpCaptureButton.setVisibility(View.GONE);
			showNextVpCaptureButton.setVisibility(View.GONE);
			showVpCapturesButton.setVisibility(View.GONE);
			exitButton.setVisibility(View.VISIBLE);
			listView.setVisibility(View.VISIBLE);
			// TURNING ON RADAR SCAN
			radarScanImageView.setVisibility(View.VISIBLE);
            radarScanImageView.startAnimation(rotationRadarScan);
            // TURNING OFF THE ATTITUDE INDICATOR SYSTEM
            targetImageView.setVisibility(View.GONE);
            swayRightImageView.setVisibility(View.GONE);
            swayLeftImageView.setVisibility(View.GONE);
            heaveUpImageView.setVisibility(View.GONE);
            heaveDownImageView.setVisibility(View.GONE);
            surgeImageView.setVisibility(View.GONE);
            yawImageView.setVisibility(View.GONE);
            pitchImageView.setVisibility(View.GONE);
            rollCImageView.setVisibility(View.GONE);
            roll01ImageView.setVisibility(View.GONE);
            roll02ImageView.setVisibility(View.GONE);
            roll03ImageView.setVisibility(View.GONE);
            roll04ImageView.setVisibility(View.GONE);
            roll05ImageView.setVisibility(View.GONE);
            roll06ImageView.setVisibility(View.GONE);
            roll07ImageView.setVisibility(View.GONE);
            roll08ImageView.setVisibility(View.GONE);
            roll09ImageView.setVisibility(View.GONE);
            roll10ImageView.setVisibility(View.GONE);
            roll11ImageView.setVisibility(View.GONE);
            roll12ImageView.setVisibility(View.GONE);
            roll13ImageView.setVisibility(View.GONE);
            roll14ImageView.setVisibility(View.GONE);
            roll15ImageView.setVisibility(View.GONE);
			}
		if (v.getId() == R.id.button3)
			{
			MetaioDebug.log("Show Program Version");
			runOnUiThread(new Runnable()
			{
				@Override
				public void run() 
				{
					String message = getString(R.string.app_version_seamensor);
					Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			});
			}
		if (v.getId() == R.id.buttonAlphaToggle)
			{
			MetaioDebug.log("Toggling imageView Transparency");
			if (imageViewTransparent)
			{
				imageViewTransparent = false;
				imageView.setImageAlpha(255);
			}
			else
			{
				imageViewTransparent = true;
				imageView.setImageAlpha(128);
			}
			if (imageViewTransparent) alphaToggleButton.setBackgroundResource(R.drawable.custom_button_option_selected);
			if (!imageViewTransparent) alphaToggleButton.setBackgroundResource(R.drawable.custom_button_option_notselected);
			}
        if (v.getId() == R.id.buttonFlashTorchVpToggle)
        {
            MetaioDebug.log("Toggling flash mode");
            Camera camera = IMetaioSDKAndroid.getCamera(this);
            Camera.Parameters params = camera.getParameters();
            MetaioDebug.log("vpFlashTorchIsOn[lastVpSelectedByUser])="+vpFlashTorchIsOn[lastVpSelectedByUser]+"  torchModeOn ="+torchModeOn);
            if ((vpFlashTorchIsOn[lastVpSelectedByUser])&&(camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)))
            {
                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(params);
                torchModeOn = true;
                flashTorchVpToggleButton.setBackgroundResource(R.drawable.custom_button_option_selected);
            }
            else
            {
                if ((vpFlashTorchIsOn[lastVpSelectedByUser]) && (camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)))
                {
                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(params);
                    torchModeOn = false;
                    flashTorchVpToggleButton.setBackgroundResource(R.drawable.custom_button_option_notselected);
                }
            }
        }
		if (v.getId()==R.id.buttonAcceptVpPhoto)
		{
			vpPhotoAccepted = true;
			MetaioDebug.log("vpPhotoAccepted BUTTON PRESSED: vpPhotoAccepted:"+vpPhotoAccepted+" vpPhotoRejected:"+vpPhotoRejected);
		}
		if (v.getId()==R.id.buttonRejectVpPhoto)
		{
			vpPhotoRejected = true;
			MetaioDebug.log("vpPhotoRejected BUTTON PRESSED: vpPhotoAccepted:"+vpPhotoAccepted+" vpPhotoRejected:"+vpPhotoRejected);
		}
		if (v.getId()==R.id.buttonShowVpCaptures)
		{
			alphaToggleButton.setVisibility(View.GONE);
			flashTorchVpToggleButton.setVisibility(View.GONE);
			showVpCapturesButton.setVisibility(View.GONE);
			showPreviousVpCaptureButton.setVisibility(View.VISIBLE);
			showNextVpCaptureButton.setVisibility(View.VISIBLE);
			imageView.resetZoom();
			if (imageViewTransparent)
			{
				imageView.setImageAlpha(255);
				imageViewTransparent=false;
			}
			photoSelected = -1;
			showVpCaptures(lastVpSelectedByUser + 1);
		}
		if (v.getId()==R.id.buttonShowPreviousVpCapture)
		{
			photoSelected++;
			showVpCaptures(lastVpSelectedByUser+1);
		}
		if (v.getId()==R.id.buttonShowNextVpCapture)
		{
			photoSelected--;
			showVpCaptures(lastVpSelectedByUser+1);
		}

	}
	
	public void showVpCaptures(int vpSelected)
	{
		final int position = vpSelected-1;
		String vpPhotoFileName=" ";
		List<DbxFileInfo> entries = new ArrayList<DbxFileInfo>();
		int numOfEntries = 0;
		try
		{
			DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
			if (dbxFs != null)
			{
				try
				{
					MetaioDebug.log("showVpCaptures: dropbox folder="+DbxPath.ROOT+capDropboxPath+vpSelected);
					entries = dbxFs.listFolder(new DbxPath(DbxPath.ROOT, capDropboxPath + vpSelected ));
				} catch (DbxException e) {
					e.printStackTrace();
				}
				if (!entries.isEmpty())
				{
					numOfEntries = entries.size();
					if (photoSelected==-1) photoSelected = numOfEntries - 1;
					if (photoSelected<0) photoSelected = 0;
					if (photoSelected > (numOfEntries-1)) photoSelected = 0; //(numOfEntries-1);
					DbxFile selectedVpPhotoInDropbox = dbxFs.open(entries.get(photoSelected).path);
					try {
						vpPhotoFileName = selectedVpPhotoInDropbox.getInfo().path.getName();
						selectedVpPhotoImageFileContents = BitmapFactory.decodeStream(selectedVpPhotoInDropbox.getReadStream());
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
					selectedVpPhotoInDropbox.close();
					StringBuilder sb = new StringBuilder(vpPhotoFileName);
					final String filename = Pattern.compile(".jpg").matcher(sb).replaceAll("");
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (!(selectedVpPhotoImageFileContents==null))
							{
								imageView.setImageBitmap(selectedVpPhotoImageFileContents);
								imageView.setVisibility(View.VISIBLE);
								imageView.resetZoom();
								if (imageViewTransparent) imageView.setImageAlpha(255);
								MetaioDebug.log("showVpCaptures: filename="+filename);
								String lastTimeAcquired = "";
								Date lastDate = new Date(Long.parseLong(filename));
								SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ssZ");
								sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
								String formattedLastDate = sdf.format(lastDate);
								lastTimeAcquired = getString(R.string.date_vp_capture_shown) + ": " +formattedLastDate;
								vpLocationDesTextView.setText(vpLocationDesText[lastVpSelectedByUser] + "\n" + lastTimeAcquired);
								vpLocationDesTextView.setVisibility(View.VISIBLE);

							}
						}
					});
				}
				else
				{
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							String message = getString(R.string.no_photo_captured_in_this_vp);
							Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
							toast.setGravity(Gravity.CENTER, 0, 0);
							toast.show();
							String lastTimeAcquiredAndNextOne = "";
							String formattedNextDate="";
							if (photoTakenTimeMillis[position]>0)
							{
								Date lastDate = new Date(photoTakenTimeMillis[position]);
								Date nextDate = new Date(vpNextCaptureMillis[position]);
								SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ssZ");
								sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
								String formattedLastDate = sdf.format(lastDate);
								formattedNextDate = sdf.format(nextDate);
								lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
										formattedLastDate+"  "+
										getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
										formattedNextDate;
							}
							else
							{
								lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
										getString(R.string.date_vp_touched_not_acquired)+"  "+
										getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
										getString(R.string.date_vp_touched_first_acquisition);
							}
							vpLocationDesTextView.setText(vpLocationDesText[position] + "\n" + lastTimeAcquiredAndNextOne);
						}
					});
				}
			}
		}
		catch (DbxException e)
		{
			e.printStackTrace();
		}

	}


	public void loadConfigurationFile()
	{
		coordinateSystemTrackedInPoseI = 1;
        vpLocationDesText = new String[qtyVps];
        vpXCameraDistance = new int[qtyVps];
        vpYCameraDistance = new int[qtyVps];
        vpZCameraDistance = new int[qtyVps];
        vpXCameraRotation = new int[qtyVps];
        vpYCameraRotation = new int[qtyVps];
        vpZCameraRotation = new int[qtyVps];
        vpMarkerlessMarkerWidth = new short[qtyVps];
        vpMarkerlessMarkerHeigth = new short[qtyVps];
        vpNumber = new short[qtyVps];
        vpFrequencyUnit = new String[qtyVps];
        vpFrequencyValue = new long[qtyVps];
        vpChecked = new boolean[qtyVps];
        vpIsAmbiguous = new boolean[qtyVps];
        vpFlashTorchIsOn = new boolean[qtyVps];
        vpIsSuperSingle = new boolean[qtyVps];
        vpSuperIdIs20mm = new boolean[qtyVps];
        vpSuperIdIs100mm = new boolean[qtyVps];
        vpSuperMarkerId = new int[qtyVps];
        photoTakenTimeMillis = new long[qtyVps];
        vpNextCaptureMillis = new long[qtyVps];

		MetaioDebug.log("loadConfigurationFile() started");
		
		for (int i=0; i<qtyVps; i++)
		{
			vpFrequencyUnit[i] = "";
			vpFrequencyValue[i] = 0;
		}
				
		// Load Initialization Values from file
        short vpListOrder = 0;

		try
		{
			// Getting a file path for vps configuration XML file
			DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
			DbxPath vpsConfigFilePath = new DbxPath(DbxPath.ROOT,vpsDropboxPath+vpsConfigFileDropbox);
			MetaioDebug.log("Vps Config Dropbox path = "+vpsConfigFilePath);
			DbxFile vpsConfigFile = dbxFs.open(vpsConfigFilePath);
			try 
				{
				FileInputStream fis = vpsConfigFile.getReadStream();
				XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
				XmlPullParser myparser = xmlFactoryObject.newPullParser();
				myparser.setInput(fis, null);
				int eventType = myparser.getEventType();
		        while (eventType != XmlPullParser.END_DOCUMENT) 
			        {
			        	if(eventType == XmlPullParser.START_DOCUMENT) 
			        	{
			        		MetaioDebug.log("Start document VPs Configuration File");
			        	} 
			        	else if(eventType == XmlPullParser.START_TAG) 
			        	{
			        		MetaioDebug.log("Start tag "+myparser.getName());
			        		if(myparser.getName().equalsIgnoreCase("Parameters"))
			        		{
			        			MetaioDebug.log("Parameters:");
			        		}
			        		else if(myparser.getName().equalsIgnoreCase("ShipId"))
		        			{ 
			        			eventType = myparser.next();
			        			shipId= Short.parseShort(myparser.getText());
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("FrequencyUnit"))
		        			{ 
			        			eventType = myparser.next();
			        			frequencyUnit = myparser.getText();
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("FrequencyValue"))
		        			{ 
			        			eventType = myparser.next();
			        			frequencyValue = Integer.parseInt(myparser.getText());
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("QtyVps"))
		        			{ 
			        			eventType = myparser.next();
			        			qtyVps = Short.parseShort(myparser.getText());
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("TolerancePosition"))
		        			{ 
			        			eventType = myparser.next();
			        			tolerancePosition = Float.parseFloat(myparser.getText());
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("ToleranceRotation"))
		        			{ 
			        			eventType = myparser.next();
			        			toleranceRotation = Float.parseFloat(myparser.getText());
		        			}
                            else if(myparser.getName().equalsIgnoreCase("Vp"))
                            {
                                vpListOrder++;
                                //MetaioDebug.log("VpListOrder: "+vpListOrder);
                            }
                            else if(myparser.getName().equalsIgnoreCase("VpNumber"))
		        			{
                                eventType = myparser.next();
                                vpNumber[vpListOrder-1] = Short.parseShort(myparser.getText());
			        			//MetaioDebug.log("VpNumber"+(vpListOrder-1)+": "+vpNumber[vpListOrder-1]);
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("VpXCameraDistance"))
		        			{ 
			        			eventType = myparser.next();
			        			vpXCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
			        			//MetaioDebug.log("vpXCameraDistance["+(vpListOrder-1)+"]"+vpXCameraDistance[vpListOrder-1]);
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("VpYCameraDistance"))
		        			{ 
			        			eventType = myparser.next();
			        			vpYCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
			        			//MetaioDebug.log("vpYCameraDistance["+(vpListOrder-1)+"]"+vpYCameraDistance[vpListOrder-1]);
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("VpZCameraDistance"))
		        			{ 
			        			eventType = myparser.next();
			        			vpZCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
			        			//MetaioDebug.log("vpZCameraDistance["+(vpListOrder-1)+"]"+vpZCameraDistance[vpListOrder-1]);
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("VpXCameraRotation"))
		        			{ 
			        			eventType = myparser.next();
			        			vpXCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
			        			//MetaioDebug.log("vpXCameraRotation["+(vpListOrder-1)+"]"+vpXCameraRotation[vpListOrder-1]);
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("VpYCameraRotation"))
		        			{ 
			        			eventType = myparser.next();
			        			vpYCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
			        			//MetaioDebug.log("vpYCameraRotation["+(vpListOrder-1)+"]"+vpYCameraRotation[vpListOrder-1]);
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("VpZCameraRotation"))
		        			{ 
			        			eventType = myparser.next();
			        			vpZCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
			        			//MetaioDebug.log("vpZCameraRotation["+(vpListOrder-1)+"]"+vpZCameraRotation[vpListOrder-1]);
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("VpLocDescription"))
		        			{ 
			        			eventType = myparser.next();
			        			vpLocationDesText[vpListOrder-1] = myparser.getText();
		        			}
                            else if(myparser.getName().equalsIgnoreCase("VpMarkerlessMarkerWidth"))
                            {
                                eventType = myparser.next();
                                vpMarkerlessMarkerWidth[vpListOrder-1] = Short.parseShort(myparser.getText());
                            }
                            else if(myparser.getName().equalsIgnoreCase("VpMarkerlessMarkerHeigth"))
                            {
                                eventType = myparser.next();
                                vpMarkerlessMarkerHeigth[vpListOrder-1] = Short.parseShort(myparser.getText());
                            }
                            else if(myparser.getName().equalsIgnoreCase("VpIsAmbiguous"))
		        			{ 
			        			eventType = myparser.next();
			        			vpIsAmbiguous[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
		        			}
                            else if(myparser.getName().equalsIgnoreCase("VpFlashTorchIsOn"))
                            {
                                eventType = myparser.next();
                                vpFlashTorchIsOn[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
                            }
                            else if(myparser.getName().equalsIgnoreCase("VpIsSuperSingle"))
                            {
                                eventType = myparser.next();
                                vpIsSuperSingle[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
                            }
                            else if(myparser.getName().equalsIgnoreCase("VpSuperIdIs20mm"))
                            {
                                eventType = myparser.next();
                                vpSuperIdIs20mm[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
                            }
                            else if(myparser.getName().equalsIgnoreCase("vpSuperIdIs100mm"))
                            {
                                eventType = myparser.next();
                                vpSuperIdIs100mm[vpListOrder-1] = Boolean.parseBoolean(myparser.getText());
                            }
                            else if(myparser.getName().equalsIgnoreCase("VpSuperMarkerId"))
                            {
                                eventType = myparser.next();
                                vpSuperMarkerId[vpListOrder-1] = Integer.parseInt(myparser.getText());
                            }
			        		else if(myparser.getName().equalsIgnoreCase("VpFrequencyUnit"))
		        			{ 
			        			eventType = myparser.next();
			        			vpFrequencyUnit[vpListOrder-1] = myparser.getText();
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("VpFrequencyValue"))
		        			{ 
			        			eventType = myparser.next();
			        			vpFrequencyValue[vpListOrder-1] = Long.parseLong(myparser.getText());
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
				vpsConfigFile.close();
	    		}
			
			MetaioDebug.log("Vps Config DROPBOX file = "+vpsConfigFile);
						
		}       
		catch (Exception e)
		{
			e.printStackTrace();
			MetaioDebug.log(Log.ERROR, "Vps data loading failed, see stack trace");
		}


		
		for (int i=0; i<qtyVps; i++)
		{
            //MetaioDebug.log("vpX["+(i+1)+"]="+vpXCameraDistance[i]+" - vpY["+(i+1)+"]="+vpYCameraDistance[i]+" - vpZ["+(i+1)+"]="+vpZCameraDistance[i]);
            vpChecked[i] = false;
			if (vpFrequencyUnit[i]=="")
				{
					vpFrequencyUnit[i]=frequencyUnit;
				}
			if (vpFrequencyValue[i]==0)
				{
					vpFrequencyValue[i]=frequencyValue;
				}
		}
	}
	
	
	public void verifyVpsChecked()
	{
		boolean change = false;
		long presentMillis = System.currentTimeMillis();
		long presentHour = presentMillis/(1000*60*60);
        long presentDay = presentMillis/(1000*60*60*24);
        long presentWeek = presentDay/7;
        long presentMonth = presentWeek/(52/12);
        		
		for (int i=0; i<qtyVps; i++)
		{
			//MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
			if (vpChecked[i])
			{
				if (vpFrequencyUnit[i].equalsIgnoreCase("millis"))
				{
					//MetaioDebug.log("Present Millis since Epoch: "+presentMillis);
					if ((presentMillis-(photoTakenTimeMillis[i]))>(vpFrequencyValue[i]))
						{
						vpChecked[i] = false;
						change = true;
						}
					//MetaioDebug.log("Photo Millis since Epoch: "+(photoTakenTimeMillis[i]));
					//MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
				}
				if (vpFrequencyUnit[i].equalsIgnoreCase("hour"))
				{
					//MetaioDebug.log("Present Hour since Epoch: "+presentHour);
					if ((presentHour-(photoTakenTimeMillis[i]/(1000*60*60)))>(vpFrequencyValue[i]))
						{
						vpChecked[i] = false;
						change = true;
						}
					//MetaioDebug.log("Photo Hour since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60)));
					//MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
				}
				if (vpFrequencyUnit[i].equalsIgnoreCase("day"))
				{
					//MetaioDebug.log("Present Day since Epoch: "+presentDay);
					if ((presentDay-(photoTakenTimeMillis[i]/(1000*60*60*24)))>(vpFrequencyValue[i]))
						{
						vpChecked[i] = false;
						change = true;
						}
					//MetaioDebug.log("Photo Day since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60*24)));
					//MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
				}
				if (vpFrequencyUnit[i].equalsIgnoreCase("week"))
				{
					//MetaioDebug.log("Present Week since Epoch: "+presentWeek);
					if ((presentWeek-(photoTakenTimeMillis[i]/(1000*60*60*24*7)))>(vpFrequencyValue[i]))
						{
						vpChecked[i] = false;
						change=true;
						}
					//MetaioDebug.log("Photo Week since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60*24*7)));
					//MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
				}
				if (vpFrequencyUnit[i].equalsIgnoreCase("month"))
				{
					//MetaioDebug.log("Present Month since Epoch: "+presentMonth);
					if ((presentMonth-(photoTakenTimeMillis[i]/(1000*60*60*24*7*(52/12))))>(vpFrequencyValue[i]))
						{
						vpChecked[i] = false;
						change = true;
						}
					//MetaioDebug.log("Photo Month since Epoch: "+(photoTakenTimeMillis[i]/(1000*60*60*24*7*(52/12))));
					//MetaioDebug.log("vpchecked: "+i+" :"+vpChecked[i]);
				}
				if (change) setVpsChecked();
			}

            if (vpFrequencyUnit[i].equalsIgnoreCase("millis"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + vpFrequencyValue[i];
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("hour"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i]*60*60*1000);
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("day"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i]*24*60*60*1000);
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("week"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i]*7*24*60*60*1000);
            }
            if (vpFrequencyUnit[i].equalsIgnoreCase("month"))
            {
                vpNextCaptureMillis[i] = photoTakenTimeMillis[i] + (vpFrequencyValue[i]*(52/12)*7*24*60*60*1000);
            }
		}
	}
	
	
	public void loadVpsChecked()
	{
		MetaioDebug.log("loadVpsChecked() started ");
		int vpListOrder = 0;
		try
		{
			// Getting a file path for vps checked config XML file
			DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            DbxPath vpsCheckedConfigFilePath = new DbxPath(DbxPath.ROOT,vpsCheckedDropboxPath+vpsCheckedConfigFileDropbox);
			MetaioDebug.log("Vps Config Dropbox path = "+vpsCheckedConfigFilePath);
			DbxFile vpsCheckedConfigFile=null;
			try
			{
				vpsCheckedConfigFile = dbxFs.open(vpsCheckedConfigFilePath);
			}
			catch (DbxException.AlreadyOpen dbe)
			{
				MetaioDebug.log(Log.ERROR, "loadVpsChecked: Checked Vps data loading failed, vpsCheckedConfigFile ALREADY OPEN: "+dbe.getMessage());
			}
			try
			{
				FileInputStream fis = vpsCheckedConfigFile.getReadStream();
				XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
				XmlPullParser myparser = xmlFactoryObject.newPullParser();
				myparser.setInput(fis, null);
				int eventType = myparser.getEventType();
		        while (eventType != XmlPullParser.END_DOCUMENT) 
			        {
			        	if(eventType == XmlPullParser.START_DOCUMENT) 
			        	{
			        		MetaioDebug.log("Start document: vpschecked.xml");
			        	} 
			        	else if(eventType == XmlPullParser.START_TAG) 
			        	{
			        		MetaioDebug.log("Start tag "+myparser.getName());

                            if(myparser.getName().equalsIgnoreCase("Vp"))
                            {
                                vpListOrder++;
                                MetaioDebug.log("VpListOrder: "+vpListOrder);
                            }
                            else if(myparser.getName().equalsIgnoreCase("VpNumber"))
                            {
                                eventType = myparser.next();
                                vpNumber[vpListOrder - 1] = Short.parseShort(myparser.getText());
                                MetaioDebug.log("VpNumber["+(vpListOrder-1)+": "+vpNumber[vpListOrder-1]);
                            }
        	        		else if(myparser.getName().equalsIgnoreCase("Checked"))
		        			{ 
			        			eventType = myparser.next();
			        			vpChecked[vpListOrder-1]= Boolean.parseBoolean(myparser.getText());
								MetaioDebug.log("VpChecked["+(vpListOrder-1)+": "+vpChecked[vpListOrder-1]);
		        			}
			        		else if(myparser.getName().equalsIgnoreCase("PhotoTakenTimeMillis"))
		        			{ 
			        			eventType = myparser.next();
			        			photoTakenTimeMillis[vpListOrder-1] = Long.parseLong(myparser.getText());
		        			}
			        		
			        	} 
			        	else if(eventType == XmlPullParser.END_TAG) 
			        	{
			        		MetaioDebug.log("End tag "+myparser.getName());
			        	} 
			        	else if(eventType == XmlPullParser.TEXT) 
			        	{
			        		MetaioDebug.log("Text "+myparser.getText());
			        	}
			        	eventType = myparser.next();		
			        }
			}
			finally 
			{
				vpsCheckedConfigFile.close();
	    	}
			MetaioDebug.log("Vps Config DROPBOX file = "+vpsCheckedConfigFile);
		}       
		catch (Exception e)
		{
			e.printStackTrace();
			MetaioDebug.log(Log.ERROR, "Checked Vps data loading failed, see stack trace:"+e.getMessage());
		}

	}

    @Override
	protected void loadContents() 
	{
		MetaioDebug.log("loadContents() started");
		try
		{
            // Loading SeaMensorCube and VpChecked 3d Geometries.
            AssetsManager.extractAllAssets(getApplicationContext(),true);
            rotation = new Rotation(new Vector3d(0f,0f,0f));
            String seaMensorCubeModel = AssetsManager.getAssetPath(getApplicationContext(), geometry3dConfigFile);
            String vpCheckedModel = AssetsManager.getAssetPath(getApplicationContext(), geometrySecondary3dConfigFile);
            MetaioDebug.log("Models : "+ AssetsManager.getAbsolutePath());
            MetaioDebug.log("Model loaded: "+getApplicationContext()+"  " + seaMensorCubeModel);
            MetaioDebug.log("Model loaded: "+getApplicationContext()+"  " + vpCheckedModel);
            // Starting Dropbox filesystem
            DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
			// defining the localFilePath to be used for all assets downloaded from DROPBOX in the MainActivity
			String localFilePath = AssetsManager.getAbsolutePath()+"/";
            trkLocalFilePath = localFilePath;

            try
            {
                MetaioDebug.log("####### LOADING: METAIO TRACKING");
                // Getting a file path for tracking configuration XML file
                DbxPath trackingConfigFilePathDropbox = new DbxPath(DbxPath.ROOT, trackingDropboxPath + trackingConfigFileName);
                // using the localFilePath to define the path to the trackingConfigFile
                String trackingConfigFilePath = localFilePath + trackingConfigFileName;
                MetaioDebug.log("Tracking Config Dropbox path = " + trackingConfigFilePathDropbox);
                // Loading the tracking configuration from DROPBOX
                DbxFile trackingConfigFile = dbxFs.open(trackingConfigFilePathDropbox);
                DbxFileStatus trackingConfigFileStatus = trackingConfigFile.getSyncStatus();
                try
                {
                    if (!trackingConfigFileStatus.isLatest) trackingConfigFile.update();
                    markerlesstrackingConfigFileContents = trackingConfigFile.readString();
                }
                finally
                {
                    trackingConfigFile.close();
                }

                markerlesstrackingConfigFileContents = markerlesstrackingConfigFileContents.replace("markervp",localFilePath+"markervp");
                markerlesstrackingConfigFileContents = markerlesstrackingConfigFileContents.replace(seaMensorMarker,localFilePath+seaMensorMarker);

                // Loading ID Markers Tracking config file used for disambiguation
                // Getting a file path for tracking configuration XML file
                DbxPath idTrackingConfigFilePathDropbox = new DbxPath(DbxPath.ROOT,trackingDropboxPath+idMarkersTrackingConfigFileName);
                // using the localFilePath to define the path to the trackingConfigFile
                String idMarkersTrackingConfigFilePath = localFilePath+idMarkersTrackingConfigFileName;
                MetaioDebug.log("ID Markers Tracking Config Dropbox path = "+idTrackingConfigFilePathDropbox);
                // Loading the tracking configuration from DROPBOX
                DbxFile idTrackingConfigFile = dbxFs.open(idTrackingConfigFilePathDropbox);
                DbxFileStatus idTrackingConfigFileStatus = idTrackingConfigFile.getSyncStatus();
                try
                {
                    if (!idTrackingConfigFileStatus.isLatest) idTrackingConfigFile.update();
                    idMarkersTrackingConfigFileContents = idTrackingConfigFile.readString();
                }
                finally
                {
                    idTrackingConfigFile.close();
                }

            }
            catch (Exception e)
            {
                MetaioDebug.log(Log.ERROR, "Error loading tracking files from Dropbox and writing to LOCAL storage");
            }

            setMarkerlessTrackingConfiguration(markerlesstrackingConfigFileContents);


            // Continuing to Load 3d Geometries.
			if (seaMensorCubeModel != null) 
			{
				mSeaMensorCube = metaioSDK.createGeometry(seaMensorCubeModel);
				if (mSeaMensorCube != null) 
				{
					// Set geometry properties
					// Set scale
					mSeaMensorCube.setScale(new Vector3d(10f, 10f, 10f));
					// Set translation
					mSeaMensorCube.setTranslation(new Vector3d(0f, 0f, 0f));
					// Set rotation
					rotation = mSeaMensorCube.getRotation();
					rotation.setFromEulerAngleDegrees(new Vector3d(0f,0f,0f));
					mSeaMensorCube.setRotation(rotation,false);
					MetaioDebug.log("Loaded geometry (name):"+mSeaMensorCube);
					MetaioDebug.log("Loaded geometry (translation):"+mSeaMensorCube.getTranslation());
					MetaioDebug.log("Loaded geometry (rotation):"+mSeaMensorCube.getRotation().getEulerAngleDegrees());
					mSeaMensorCube.setVisible(false);
				}				
				else
					MetaioDebug.log(Log.ERROR, "Error loading geometry: "+seaMensorCubeModel);
			}
			if (vpCheckedModel != null) 
			{
				mVpChecked = metaioSDK.createGeometry(vpCheckedModel);
				if (mVpChecked != null) 
				{
					// Set geometry properties
					// Set scale
					mVpChecked.setScale(new Vector3d(60f, 60f, 60f));
					// Set translation
					mVpChecked.setTranslation(new Vector3d(0f, 0f, 0f));
					// Set rotation
					com.metaio.sdk.jni.Rotation rot = mVpChecked.getRotation();
					rot.setFromEulerAngleDegrees(new Vector3d(0f,0f,0f));
					mVpChecked.setRotation(rot,false);
					MetaioDebug.log("Loaded geometry (name):"+mVpChecked);
					MetaioDebug.log("Loaded geometry (translation):"+mVpChecked.getTranslation());
					MetaioDebug.log("Loaded geometry (rotation):"+mVpChecked.getRotation().getEulerAngleDegrees());
					mVpChecked.setVisible(false);
				}				
				else
					MetaioDebug.log(Log.ERROR, "Error loading geometry: "+mVpChecked);
			}
        }
		catch (Exception e)
		{
			e.printStackTrace();
			MetaioDebug.log(Log.ERROR, "loadContents failed, see stack trace");
		}
	}


    public void setMarkerlessTrackingConfiguration(final String trkContents)
    {
        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected void onPreExecute()
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                       //if (waitingForMarkerlessTrackingConfigurationToLoad)
                       {
                           MetaioDebug.log("BEFORE STARTING setMarkerlessTrackingConfiguration IN BACKGROUND - Lighting Waiting Circle");
                           mProgress.setVisibility(View.VISIBLE);
                           mProgress.startAnimation(rotationMProgress);
                       }
                    }
                });
            }

            @Override
            protected Void doInBackground(Void... params)
            {
                MetaioDebug.log("STARTING setMarkerlessTrackingConfiguration IN BACKGROUND");
                resultMarkerlessTrk = metaioSDK.setTrackingConfiguration(trkContents,false);
                MetaioDebug.log("RESULT setMarkerlessTrackingConfiguration IN BACKGROUND:"+resultMarkerlessTrk);
                timesSetMrklsTrkIsCalled++;
                MetaioDebug.log("setMarkerlessTrackingConfiguration: *** timesSetMrklsTrkIsCalled:"+timesSetMrklsTrkIsCalled);
                return null;
            }

            @Override
            protected void onPostExecute(Void result)
            {
                MetaioDebug.log("Tracking data loaded: " + resultMarkerlessTrk);
                MetaioDebug.log("# of Def COS: " +metaioSDK.getNumberOfDefinedCoordinateSystems());
                MetaioDebug.log("FINISHING setMarkerlessTrackingConfiguration IN BACKGROUND");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //if (waitingForMarkerlessTrackingConfigurationToLoad)
                        {
                            MetaioDebug.log("FINISHING setMarkerlessTrackingConfiguration IN BACKGROUND - Turning off Waiting Circle");
                            mProgress.clearAnimation();
                            mProgress.setVisibility(View.GONE);
                            MetaioDebug.log("FINISHING setMarkerlessTrackingConfiguration IN BACKGROUND - mProgress.isShown():" + mProgress.isShown());
							// TURNING OFF TARGET
							targetImageView.setImageDrawable(drawableTargetWhite);
							targetImageView.setVisibility(View.GONE);
							// TURNING ON RADAR SCAN
							radarScanImageView.setVisibility(View.VISIBLE);
							radarScanImageView.startAnimation(rotationRadarScan);
                            waitingForMarkerlessTrackingConfigurationToLoad = false;
                        }

                    }
                });

            }

        }.execute();
    }


    public boolean setSpecialDisambiguationTrackingConfiguration(int vpIndex, int width, int height, String markerFilePath)
    {
        boolean trackingConfigIsSet = false;
        MetaioDebug.log("Marker File Path="+markerFilePath);
        String trkcfg ="<?xml version=\"1.0\"?>"+
                "<TrackingData>"+
                "<Sensors>"+
                "<Sensor Type=\"FeatureBasedSensorSource\" Subtype=\"Fast\">"+
                "<SensorID>FeatureTracking"+vpIndex+"</SensorID>"+
                "<Parameters>"+
                "<FeatureDescriptorAlignment>regular</FeatureDescriptorAlignment>"+
                "<MaxObjectsToDetectPerFrame>1</MaxObjectsToDetectPerFrame>"+
                "<MaxObjectsToTrackInParallel>1</MaxObjectsToTrackInParallel>"+
                "<SimilarityThreshold>0.7</SimilarityThreshold>"+
                "</Parameters>"+
                "<SensorCOS>"+
                "<SensorCosID>Patch"+vpIndex+"</SensorCosID>"+
                "<Parameters>"+
                "<ReferenceImage WidthMM=\""+width+"\" HeigthMM=\""+height+"\">"+markerFilePath+"</ReferenceImage>"+
                "<SimilarityThreshold>0.7</SimilarityThreshold>"+
                "</Parameters>"+
                "</SensorCOS>"+
                "</Sensor>"+
                "</Sensors>"+
                "<Connections>"+
                "<COS>"+
                "<Name>MarkerlessCOS"+vpIndex+"</Name>"+
                "<Fuser Type=\"SmoothingFuser\">"+
                "<Parameters>"+
                "<KeepPoseForNumberOfFrames>0</KeepPoseForNumberOfFrames>"+
                "<GravityAssistance></GravityAssistance>"+
                "<AlphaTranslation>0.9</AlphaTranslation>"+
                "<GammaTranslation>0.9</GammaTranslation>"+
                "<AlphaRotation>0.8</AlphaRotation>"+
                "<GammaRotation>0.8</GammaRotation>"+
                "<ContinueLostTrackingWithOrientationSensor>false</ContinueLostTrackingWithOrientationSensor>"+
                "</Parameters>"+
                "</Fuser>"+
                "<SensorSource>"+
                "<SensorID>FeatureTracking"+vpIndex+"</SensorID>"+
                "<SensorCosID>Patch"+vpIndex+"</SensorCosID>"+
                "<HandEyeCalibration>"+
                "<TranslationOffset>"+
                "<X>0</X>"+
                "<Y>0</Y>"+
                "<Z>0</Z>"+
                "</TranslationOffset>"+
                "<RotationOffset>"+
                "<X>0</X>"+
                "<Y>0</Y>"+
                "<Z>0</Z>"+
                "<W>1</W>"+
                "</RotationOffset>"+
                "</HandEyeCalibration>"+
                "<COSOffset>"+
                "<TranslationOffset>"+
                "<X>0</X>"+
                "<Y>0</Y>"+
                "<Z>0</Z>"+
                "</TranslationOffset>"+
                "<RotationOffset>"+
                "<X>0</X>"+
                "<Y>0</Y>"+
                "<Z>0</Z>"+
                "<W>1</W>"+
                "</RotationOffset>"+
                "</COSOffset>"+
                "</SensorSource>"+
                "</COS>"+
                "</Connections>"+
                "</TrackingData>";

        if (metaioSDK.setTrackingConfiguration(trkcfg, false))
        {
            MetaioDebug.log("Disambiguation - New Tracking Config Loaded: vpIndex="+vpIndex);
            trackingConfigIsSet = true;
            return trackingConfigIsSet;
        }
        else
        {
            MetaioDebug.log(Log.ERROR,"Failed to load Marker for Markerless Traking Config for Disambiguation: vpIndex="+vpIndex);
            return false;
        }
    }


    public boolean setSuperSLAMExtendedTrackingConfiguration(int vpIndex, int width, int height, String markerFilePath)
    {
        boolean trackingConfigIsSet = false;
        MetaioDebug.log("SLAM Extended Markerless Marker File Path="+markerFilePath);
        String trkcfg = "<?xml version=\"1.0\"?>"+
                        "<TrackingData>"+
                            "<Sensors>"+
                                "<Sensor Version=\"2\" Type=\"SLAMSensorSource\">"+
                                    "<SensorID>SLAM</SensorID>"+
                                    "<Parameters>"+
                                        "<MaxObjectsToDetectPerFrame>5</MaxObjectsToDetectPerFrame>"+
                                        "<MaxObjectsToTrackInParallel>1</MaxObjectsToTrackInParallel>"+
                                    "</Parameters>"+
                                    "<SensorCOS>"+
                                        "<SensorCosID>world</SensorCosID>"+
                                        "<Parameters>"+
                                            "<Initialization type=\"image\">"+
                                                "<ReferenceImage WidthMM=\""+width+"\" HeigthMM=\""+height+"\">"+markerFilePath+"</ReferenceImage>"+
                                                "<SimilarityThreshold>0.7</SimilarityThreshold>"+
                                            "</Initialization>"+
                                            "<Learning Enabled=\"true\">"+
                                                "<MinNumberOfObservations>3</MinNumberOfObservations>"+
                                                "<MinTriangulationAngle>5.0</MinTriangulationAngle>"+
                                            "</Learning>"+
                                            "<Tracking>"+
                                                "<AlignZAxisWithGravity>false</AlignZAxisWithGravity>"+
                                            "</Tracking>"+
                                        "</Parameters>"+
                                    "</SensorCOS>"+
                                "</Sensor>"+
                            "</Sensors>"+
                            "<Connections>"+
                                "<COS>"+
                                    "<Name>COS"+vpIndex+"</Name>"+
                                    "<Fuser Type=\"SmoothingFuser\">"+
                                        "<Parameters>"+
                                            "<AlphaRotation>0.8</AlphaRotation>"+
                                            "<AlphaTranslation>1</AlphaTranslation>"+
                                            "<GammaRotation>0.8</GammaRotation>"+
                                            "<GammaTranslation>1</GammaTranslation>"+
                                            "<KeepPoseForNumberOfFrames>60</KeepPoseForNumberOfFrames>"+
                                        "</Parameters>"+
                                    "</Fuser>"+
                                    "<SensorSource>"+
                                        "<SensorID>SLAM</SensorID>"+
                                        "<SensorCosID>world</SensorCosID>"+
                                        "<HandEyeCalibration>"+
                                            "<TranslationOffset>"+
                                                "<X>0.0</X>"+
                                                "<Y>0.0</Y>"+
                                                "<Z>0.0</Z>"+
                                            "</TranslationOffset>"+
                                            "<RotationOffset>"+
                                                "<X>0.0</X>"+
                                                "<Y>0.0</Y>"+
                                                "<Z>0.0</Z>"+
                                                "<W>1.0</W>"+
                                            "</RotationOffset>"+
                                        "</HandEyeCalibration>"+
                                        "<COSOffset>"+
                                            "<TranslationOffset>"+
                                                "<X>0.5</X>"+
                                                "<Y>0.5</Y>"+
                                                "<Z>0.0</Z>"+
                                            "</TranslationOffset>"+
                                            "<RotationOffset>"+
                                                "<X>0.0</X>"+
                                                "<Y>0.0</Y>"+
                                                "<Z>0.0</Z>"+
                                                "<W>1.0</W>"+
                                            "</RotationOffset>"+
                                        "</COSOffset>"+
                                    "</SensorSource>"+
                                "</COS>"+
                            "</Connections>"+
                        "</TrackingData>";

        if (metaioSDK.setTrackingConfiguration(trkcfg, false))
        {
            MetaioDebug.log("Disambiguation - Super SLAM Extended Tracking Config Loaded: vpIndex="+vpIndex);
            trackingConfigIsSet = true;
            return trackingConfigIsSet;
        }
        else
        {
            MetaioDebug.log(Log.ERROR,"Failed to load Super SLAM Extended Tracking Config: vpIndex="+vpIndex);
            return false;
        }
    }


    @Override
	protected void onGeometryTouched(IGeometry geometry) 
	{
		if (metaioSDK != null)
		{
			final TrackingValuesVector poses = metaioSDK.getTrackingValues();
			//if we have detected one, attach our 3d model to this coordinate system Id
			if (poses.size() != 0)
				for (im=0; im<poses.size(); im++)
				{
				if (geometry.getCoordinateSystemID()==poses.get(im).getCoordinateSystemID())
					{
					MetaioDebug.log("onGeometryTouched: Touched CoordinateSystemID = "+coordinateSystemTrackedInPoseI);
                    Date currentDate = new Date(System.currentTimeMillis());
                    SimpleDateFormat sdfext = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ssZ");
					sdfext.setTimeZone(TimeZone.getTimeZone("UTC"));
                    MetaioDebug.log("onGeometryTouched: Current Time = "+sdfext.format(currentDate));
					runOnUiThread(new Runnable()
					{
					@Override
					public void run() 
					{
                        // Show last captured date and what is the frequency
                        String lastTimeAcquiredAndNextOne = "";
                        String formattedNextDate="";
                        if (photoTakenTimeMillis[coordinateSystemTrackedInPoseI-1]>0)
                        {
                            Date lastDate = new Date(photoTakenTimeMillis[coordinateSystemTrackedInPoseI-1]);
                            Date nextDate = new Date(vpNextCaptureMillis[coordinateSystemTrackedInPoseI-1]);
                            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ssZ");
							sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                            String formattedLastDate = sdf.format(lastDate);
                            formattedNextDate = sdf.format(nextDate);
                            lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
                                    formattedLastDate+"  "+
                                    getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
                                    formattedNextDate;
                        }
                        else
                        {
                            lastTimeAcquiredAndNextOne = getString(R.string.date_vp_touched_last_acquired) + ": " +
                                    getString(R.string.date_vp_touched_not_acquired)+"  "+
                                    getString(R.string.date_vp_touched_free_to_be_acquired)+ ": "+
                                    getString(R.string.date_vp_touched_first_acquisition);
                        }
                        String message = getString(R.string.vp_touched)+(vpNumber[coordinateSystemTrackedInPoseI-1])+"\n"+vpLocationDesText[coordinateSystemTrackedInPoseI-1]
                                        +"\n"+lastTimeAcquiredAndNextOne;
						Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
					}
					});
					}
				}
		}
	}


	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler() 
	{
		return mSDKCallback;
	}

    final class MetaioSDKCallbackHandler extends IMetaioSDKCallback
	{

		@Override
		public void onSDKReady() 
		{
			MetaioDebug.log("The SDK is ready");
			// show GUI after SDK is ready
			runOnUiThread(new Runnable() 
				{
				@Override
				public void run() 
				{
					mGUIView.setVisibility(View.VISIBLE);
					setVpsChecked();
				}
				});
			
			
			// Load camera calibration			
			try
			{
				DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
				DbxPath cameraCalibrationFilePath = new DbxPath(DbxPath.ROOT,camCalDropboxPath+cameraCalibrationFileName);
				MetaioDebug.log("Camera Calibration path = "+cameraCalibrationFilePath);
				String cameraCalibration;
				DbxFile cameraCalibrationFile = dbxFs.open(cameraCalibrationFilePath);
				try {
					cameraCalibration = cameraCalibrationFile.readString();
				} finally {
					cameraCalibrationFile.close();
				}
				MetaioDebug.log("Camera Calibration DROPBOX file = "+cameraCalibrationFile);
				boolean result = metaioSDK.setCameraParameters(cameraCalibration); 
				MetaioDebug.log("Camera Calibration data loaded: " + result);
			}
			catch (Exception e)
			{
                MetaioDebug.log("Camera Calibration data loading failed");
			}	
			
		}
		
		@Override
		public void onAnimationEnd(IGeometry geometry, String animationName) 
		{
			MetaioDebug.log("animation ended" + animationName);
		}
		

		private  File getOutputMediaFile(){
		    File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
		            + "/Android/data/"
		            + getApplicationContext().getPackageName()
		            + "/Files"); 

		    // Create the storage directory if it does not exist
		    if (! mediaStorageDir.exists()){
		        if (! mediaStorageDir.mkdirs()){
		            return null;
		        }
		    } 
		    // Create a media file name
		    File mediaFile;
		    String mImageName="MI_tempo.jpg";
		    mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);  
		    return mediaFile;
		} 
		
		
		@Override
		public void onNewCameraFrame(ImageStruct cameraFrame)
		{
			BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
			bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
			Bitmap bitmapImage = null;
			File pictureFile = getOutputMediaFile();
			MetaioDebug.log("onNewCameraFrame: a new camera frame image is delivered " + cameraFrame.getTimestamp());
			long momentoLong = System.currentTimeMillis();
			photoTakenTimeMillis[coordinateSystemTrackedInPoseI - 1] = momentoLong;

			String momento = String.valueOf(momentoLong);
			MetaioDebug.log("onNewCameraFrame: a new camera frame image is delivered " + momento);
			if ((vpIsAmbiguous[coordinateSystemTrackedInPoseI - 1]) && (vpIsDisambiguated))
				waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
			if (doubleCheckingProcedureFinalized) {
				doubleCheckingProcedureStarted = false;
				doubleCheckingProcedureFinalized = false;
			}
			;
			if (cameraFrame != null) {
				bitmapImage = cameraFrame.getBitmap();
				final int width = bitmapImage.getWidth();
				final int height = bitmapImage.getHeight();
				MetaioDebug.log("onNewCameraFrame: Camera frame width: " + width + " height: " + height);
				locPhotoToExif = getGPSToExif(mCurrentLocation);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						targetImageView.setImageDrawable(drawableTargetWhite);
						targetImageView.setVisibility(View.GONE);
					}
				});
				if (pictureFile == null) {
					MetaioDebug.log("onNewCameraFrame: Error creating media file, check storage permissions: ");// e.getMessage());
					return;
				}
			}

			if (bitmapImage != null)
			{
				MetaioDebug.log("onNewCameraFrame: metaioSDK.setTrackingConfiguration(\"DUMMY\")");
				metaioSDK.setTrackingConfiguration("DUMMY");
				if ((!vpPhotoAccepted) && (!vpPhotoRejected))
				{
					final Bitmap tmpBitmapImage = bitmapImage;
					runOnUiThread(new Runnable() {
					@Override
					public void run() {
						imageView.setImageBitmap(tmpBitmapImage);
						imageView.resetZoom();
						imageView.setVisibility(View.VISIBLE);
						if (imageViewTransparent) imageView.setImageAlpha(255);
						isVpPhotoOkTextView.setVisibility(View.VISIBLE);
						acceptVpPhotoButton.setVisibility(View.VISIBLE);
						rejectVpPhotoButton.setVisibility(View.VISIBLE);
						listView.setVisibility(View.GONE);
									}
				});
				}

				do
				{
					// Waiting for user response
					// MetaioDebug.log("vpPhotoAccepted:"+vpPhotoAccepted+" vpPhotoRejected:"+vpPhotoRejected);
				} while ((!vpPhotoAccepted)&&(!vpPhotoRejected));

				MetaioDebug.log("onNewCameraFrame: LOOP ENDED: vpPhotoAccepted:"+vpPhotoAccepted+" vpPhotoRejected:"+vpPhotoRejected);

				if (vpPhotoAccepted)
				{
					MetaioDebug.log("onNewCameraFrame: vpPhotoAccepted!!!!");
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (imageViewTransparent) imageView.setImageAlpha(128);
							imageView.setVisibility(View.GONE);
							isVpPhotoOkTextView.setVisibility(View.GONE);
							acceptVpPhotoButton.setVisibility(View.GONE);
							rejectVpPhotoButton.setVisibility(View.GONE);
							listView.setVisibility(View.VISIBLE);
						}
					});

					setVpsChecked();
					new SaveVpsChecked().execute();

					try
					{
						FileOutputStream fos = new FileOutputStream(pictureFile);
						bitmapImage.compress(Bitmap.CompressFormat.JPEG, 95, fos);
						fos.close();
						ExifInterface locPhotoTags = new ExifInterface(pictureFile.getAbsolutePath());
						locPhotoTags.setAttribute("GPSLatitude", locPhotoToExif[0]);
						MetaioDebug.log("onNewCameraFrame: GPSLatitude:" + locPhotoToExif[0] + " " + locPhotoToExif[1]);
						locPhotoTags.setAttribute("GPSLatitudeRef", locPhotoToExif[1]);
						locPhotoTags.setAttribute("GPSLongitude", locPhotoToExif[2]);
						MetaioDebug.log("onNewCameraFrame: GPSLongitude:" + locPhotoToExif[2] + " " + locPhotoToExif[3]);
						locPhotoTags.setAttribute("GPSLongitudeRef", locPhotoToExif[3]);
						locPhotoTags.setAttribute("GPSAltitude", locPhotoToExif[4]);
						locPhotoTags.setAttribute("Make", locPhotoToExif[5]);
						locPhotoTags.setAttribute("Model", locPhotoToExif[6]);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
						sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
						String formattedDateTime = sdf.format(photoTakenTimeMillis[coordinateSystemTrackedInPoseI - 1]);
						locPhotoTags.setAttribute("DateTime", formattedDateTime);
						locPhotoTags.saveAttributes();
						DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
						if (!dbxFs.exists(new DbxPath(DbxPath.ROOT,capDropboxPath+vpNumber[coordinateSystemTrackedInPoseI-1]+"/"+momento+".jpg")))
						{
							DbxFile bitMap = dbxFs.create(new DbxPath(DbxPath.ROOT,capDropboxPath+vpNumber[coordinateSystemTrackedInPoseI-1]+"/"+momento+".jpg"));
							MetaioDebug.log("onNewCameraFrame: Saving a new camera frame image to: "+capDropboxPath+vpNumber[coordinateSystemTrackedInPoseI-1]+"/"+momento+".jpg");
							bitMap.writeFromExistingFile(pictureFile, false);
							bitMap.close();
							//pictureFile.delete();
							if (resultSpecialTrk)
							{
								waitingForMarkerlessTrackingConfigurationToLoad = true;
								MetaioDebug.log("onNewCameraFrame: vpPhotoAccepted >>>>> calling setMarkerlessTrackingConfiguration");
								setMarkerlessTrackingConfiguration(markerlesstrackingConfigFileContents);
								resultSpecialTrk = false;
								vpIsDisambiguated = false;
							}
						}
						else
						{
							dbxFs.delete(new DbxPath(DbxPath.ROOT,capDropboxPath+vpNumber[coordinateSystemTrackedInPoseI-1]+"/"+momento+".jpg"));
							MetaioDebug.log("onNewCameraFrame: Deleting and saving a new camera frame image to: "+capDropboxPath+vpNumber[coordinateSystemTrackedInPoseI-1]+"/"+momento+".jpg");
							DbxFile bitMap = dbxFs.create(new DbxPath(DbxPath.ROOT,capDropboxPath+vpNumber[coordinateSystemTrackedInPoseI-1]+"/"+momento+".jpg"));
							bitMap.writeFromExistingFile(pictureFile, false);
							bitMap.close();
							//pictureFile.delete();
							if (resultSpecialTrk)
							{
								waitingForMarkerlessTrackingConfigurationToLoad = true;
								MetaioDebug.log("onNewCameraFrame: vpPhotoAccepted >>>>> calling setMarkerlessTrackingConfiguration");
								setMarkerlessTrackingConfiguration(markerlesstrackingConfigFileContents);
								resultSpecialTrk = false;
								vpIsDisambiguated = false;
							}
						}
					}
					catch (Exception e)
					{
						vpChecked[coordinateSystemTrackedInPoseI - 1] = false;
						setVpsChecked();
						new SaveVpsChecked().execute();
						//waitingToCaptureVpAfterDisambiguationProcedureSuccessful =true;
						e.printStackTrace();
					}

					try {
						ObjectMetadata myObjectMetadata = new ObjectMetadata();
						//create a map to store user metadata
						Map<String, String> userMetadata = new HashMap<String,String>();
						userMetadata.put("GPSLatitude", locPhotoToExif[0]);
						userMetadata.put("GPSLongitude", locPhotoToExif[1]);
						userMetadata.put("VP", ""+(coordinateSystemTrackedInPoseI));
						userMetadata.put("seamensorAccount", seamensorAccount);
						userMetadata.put("Precisioninm", locPhotoToExif[4]);
						userMetadata.put("LocationMillis", locPhotoToExif[5]);
						userMetadata.put("LocationMethod", locPhotoToExif[6]);
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
						sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
						String formattedDateTime = sdf.format(photoTakenTimeMillis[coordinateSystemTrackedInPoseI - 1]);
						userMetadata.put("DateTime", formattedDateTime);
						//call setUserMetadata on our ObjectMetadata object, passing it our map
						myObjectMetadata.setUserMetadata(userMetadata);
						//uploading the objects
						transferUtility = AwsUtil.getTransferUtility(getApplicationContext());
						File fileToUpload = pictureFile;
						TransferObserver observer = transferUtility.upload(
								Constants.BUCKET_NAME,		/* The bucket to upload to */
								"cap/"+seamensorAccount+"/"+momento+".jpg",		/* The key for the uploaded object */
								fileToUpload,				/* The file where the data to upload exists */
								myObjectMetadata			/* The ObjectMetadata associated with the object*/
						);
						Log.d("Cognito",observer.getState().toString());
						Log.d("Cognito",observer.getAbsoluteFilePath());
						Log.d("Cognito",observer.getBucket());
						Log.d("Cognito",observer.getKey());
					}
					catch (Exception e)
					{

					}

					vpPhotoAccepted = false;
					vpPhotoRequestInProgress = false;
					MetaioDebug.log("onNewCameraFrame: vpPhotoAccepted: vpPhotoRequestInProgress = "+vpPhotoRequestInProgress);
					waitingForMarkerlessTrackingConfigurationToLoad = true;
					MetaioDebug.log("onNewCameraFrame: vpPhotoAccepted >>>>> calling setMarkerlessTrackingConfiguration >>>> SECOND TIME!!!!!");
					setMarkerlessTrackingConfiguration(markerlesstrackingConfigFileContents);
				}

				if (vpPhotoRejected)
				{
					MetaioDebug.log("onNewCameraFrame: vpPhotoRejected!!!!");
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							if (imageViewTransparent) imageView.setImageAlpha(128);
							imageView.setVisibility(View.GONE);
							acceptVpPhotoButton.setVisibility(View.GONE);
							rejectVpPhotoButton.setVisibility(View.GONE);
							isVpPhotoOkTextView.setVisibility(View.GONE);
							listView.setVisibility(View.VISIBLE);
							// TURNING ON RADAR SCAN
							radarScanImageView.setVisibility(View.VISIBLE);
							radarScanImageView.startAnimation(rotationRadarScan);
						}
					});
					vpChecked[coordinateSystemTrackedInPoseI - 1] = false;
					//waitingToCaptureVpAfterDisambiguationProcedureSuccessful=true;
					setVpsChecked();
					new SaveVpsChecked().execute();
					if (resultSpecialTrk)
					{
						waitingForMarkerlessTrackingConfigurationToLoad = true;
						setMarkerlessTrackingConfiguration(markerlesstrackingConfigFileContents);
						resultSpecialTrk = false;
						vpIsDisambiguated = false;
					}
					lastVpPhotoRejected = true;
					vpPhotoRejected = false;
					vpPhotoRequestInProgress = false;
					MetaioDebug.log("onNewCameraFrame: vpPhotoRejected >>>>> calling setMarkerlessTrackingConfiguration");
					MetaioDebug.log("onNewCameraFrame: vpPhotoRejected: vpPhotoRequestInProgress = "+vpPhotoRequestInProgress);
				}
			}
		}

		@Override
		public void onScreenshotImage(ImageStruct image)
		{
			MetaioDebug.log("screenshot image is received" + image.getTimestamp());
		}
		
		@Override
		public void onTrackingEvent(TrackingValuesVector trackingValues)
		{
            // BLOCK 0
            MetaioDebug.log("oTE: BLOCK 0:************** onTrackingEvent called");
            //for (int i=0; i<trackingValues.size(); i++)      //if (trackingValues.size()>0)
            for (int i=0; i<trackingValues.size(); i++)
            {
				final TrackingValues v = trackingValues.get(i);
                String trackingStringEState = v.getState().toString();
                MetaioDebug.log("oTE: BLOCK 0:*** TRACKING STRING: " + trackingStringEState);
				MetaioDebug.log("oTE: BLOCK 0:Tracking state for COS "+v.getCoordinateSystemID()+" is "+v.getState());
				String sensorType = v.getSensor();
                String cosName = v.getCosName();
                if (sensorType.equalsIgnoreCase("FeatureBasedSensorSource"))
                {
                    initialCoordinateSystemTrackedInPoseI = Integer.parseInt(cosName.replace("MarkerlessCOS", ""));
                    idTrackingLoaded = false;
                }
                if (sensorType.equalsIgnoreCase("SLAMSensorSource"))
                {
                    initialCoordinateSystemTrackedInPoseI = Integer.parseInt(cosName.replace("COS", ""));
                    idTrackingLoaded = false;
                }
                if (sensorType.equalsIgnoreCase("MarkerBasedSensorSource"))
                {
                    initialCoordinateSystemTrackedInPoseI = Integer.parseInt(cosName.replace("COS",""));
                }
				if (sensorType.equalsIgnoreCase("DummySensorSource"))
				{
					initialCoordinateSystemTrackedInPoseI = 0;
				}
                MetaioDebug.log("oTE: BLOCK 0:*** initialCoordinateSystemTrackedInPoseI:"+initialCoordinateSystemTrackedInPoseI);
                MetaioDebug.log("oTE: BLOCK 0:*** Sensor Type:"+sensorType);
				MetaioDebug.log("oTE: BLOCK 0:*** cosName:"+v.getCosName());
                MetaioDebug.log("oTE: BLOCK 0:*** idTrackingLoaded:"+idTrackingLoaded);

                // BLOCK 1
                // ETS found
                // ANY TRACKING
                if (v.getState()== ETRACKING_STATE.ETS_FOUND)
                {
                    MetaioDebug.log("oTE: BLOCK 1:=================== ANY Marker Found");
                }

                // BLOCK 2
                // ETS found
				// MARKERLESS TRACKING
				// NOT WAITING TO CAPTURE VP AFTER SUCCESSFUL DISAMBIGUATION AS THE DISAMBIGUATION IS YET TO START
				if ((v.getState()== ETRACKING_STATE.ETS_FOUND) && (sensorType.equalsIgnoreCase("FeatureBasedSensorSource")) && (!waitingToCaptureVpAfterDisambiguationProcedureSuccessful) )
				{
                    etsInitializedAndNotFound=false;
                    idTrackingLoaded = false;
                    MetaioDebug.log("oTE: BLOCK 2:=================== FeatureBased Marker Found");
				    //MetaioDebug.log("Before Disambiguation procedure = COS" + initialCoordinateSystemTrackedInPoseI);
					//MetaioDebug.log("Before Disambiguation procedure - Sensor:"+v.getSensor());
					//MetaioDebug.log("Wait State:"+waitingToCaptureVpAfterDisambiguationProcedureSuccessful);
					// DISAMBIGUATION PROCEDURE STARTS
					// Verify if the initialCoordinateSystemTrackedInPoseI is contained in the list of ambiguous VPs 
					// if so activate the marker id checking      DummySensorSource
					if (lastVpPhotoRejected)
					{
						MetaioDebug.log("oTE: BLOCK 2:=================== lastVpPhotoRejected!!!!! calling setMarkerlessTrackingConfiguration(markerlesstrackingConfigFileContents)");
						calledIdTrackingLoad = true;
						idTrackingLoaded = metaioSDK.setTrackingConfiguration(idMarkersTrackingConfigFileContents, false);
						if (idTrackingLoaded) MetaioDebug.log("oTE: BLOCK 2:lastVpPhotoRejected!!!!! - Tracking with ID Marker load success - calledIdTrackingLoad="+calledIdTrackingLoad);
						lastVpPhotoRejected = false;
					}
					else
					{
						if (vpIsAmbiguous[initialCoordinateSystemTrackedInPoseI-1])
						{
							// If an ambiguousVP is detected then load the ID Markers to disambiguate
                        	calledIdTrackingLoad = true;
                        	idTrackingLoaded = metaioSDK.setTrackingConfiguration(idMarkersTrackingConfigFileContents, false);
                        	if (idTrackingLoaded) MetaioDebug.log("oTE: BLOCK 2:ambiguous VP detected - init Disambiguation procedure - Tracking with ID Marker load success - calledIdTrackingLoad="+calledIdTrackingLoad);
                    	}
						else
                    	{
							// If it is NOT an ambiguousVP then assign
							coordinateSystemTrackedInPoseI = initialCoordinateSystemTrackedInPoseI;
							//MetaioDebug.log("After Disambiguation procedure = coordinateSystemTrackedInPoseI =" + coordinateSystemTrackedInPoseI);
							//MetaioDebug.log("After Disambiguation procedure = disambiguatedCoordinateSystemTrackedInPoseI =" + disambiguatedCoordinateSystemTrackedInPoseI);
						}
					}

				}

                // BLOCK 3
				// ETS found
				// ID MARKER TRACKING
				// if an ID Marker is detected in the disambiguation procedure then mark the VP as disambiguated and load the markerless setup to acquire.
				if ((v.getState()== ETRACKING_STATE.ETS_FOUND) && (sensorType.equalsIgnoreCase("MarkerBasedSensorSource")) )
                {
                    etsInitializedAndNotFound=false;
                    MetaioDebug.log("oTE: BLOCK 3:=================== MarkerBased  Marker Found");
                    disambiguatedCoordinateSystemTrackedInPoseI = v.getCoordinateSystemID();
                    //MetaioDebug.log("Disambiguation procedure - ID Marker:"+disambiguatedCoordinateSystemTrackedInPoseI);
                    //MetaioDebug.log("Disambiguation procedure - Sensor:"+v.getSensor());
                    //MetaioDebug.log("Wait State:"+waitingToCaptureVpAfterDisambiguationProcedureSuccessful);
                    coordinateSystemTrackedInPoseI = disambiguatedCoordinateSystemTrackedInPoseI;
                    for (int j=0; j<qtyVps; j++)
                    {
                        MetaioDebug.log("oTE: BLOCK 3:SUPERTRACKING: j =" + j+" vpSuperMarkerId[j]="+vpSuperMarkerId[j]+" disambiguatedCoordinateSystemTrackedInPoseI="+disambiguatedCoordinateSystemTrackedInPoseI);
                        if (vpSuperMarkerId[j]==disambiguatedCoordinateSystemTrackedInPoseI)
                        {
                            if (vpIsSuperSingle[j]) coordinateSystemTrackedInPoseI=(j+1);
                            MetaioDebug.log("oTE: BLOCK 3:SUPERTRACKING: After Disambiguation procedure = coordinateSystemTrackedInPoseI =" + coordinateSystemTrackedInPoseI);
                        }
                    }
                    vpIsDisambiguated=true;
					if (doubleCheckingProcedureStarted) doubleCheckingProcedureFinalized=true;
                    MetaioDebug.log("oTE: BLOCK 3:After Disambiguation procedure = disambiguatedCoordinateSystemTrackedInPoseI =" + disambiguatedCoordinateSystemTrackedInPoseI);
                    MetaioDebug.log("oTE: BLOCK 3:After Disambiguation procedure = coordinateSystemTrackedInPoseI =" + coordinateSystemTrackedInPoseI);
                    //Checking if coordinateSystemTrackedInPoseI is a normalVP or a superVPSingle;
                    MetaioDebug.log("oTE: BLOCK 3:vpIsSuperSingle["+coordinateSystemTrackedInPoseI+"-1] =" + vpIsSuperSingle[coordinateSystemTrackedInPoseI-1]);
                    if (!vpIsSuperSingle[coordinateSystemTrackedInPoseI-1])
                    {
                        // Re-loading the markerless tracking
                        resultSpecialTrk = setSpecialDisambiguationTrackingConfiguration(   coordinateSystemTrackedInPoseI,
                                                                                            vpMarkerlessMarkerWidth[coordinateSystemTrackedInPoseI-1],
                                                                                            vpMarkerlessMarkerHeigth[coordinateSystemTrackedInPoseI-1],
                                                                                            trkLocalFilePath+"markervp"+coordinateSystemTrackedInPoseI+".jpg");
                        //boolean result = metaioSDK.setTrackingConfiguration(markerlessTrackingConfigFilePath,true);
                        //MetaioDebug.log("Tracking data re-loaded: " + result);
                        waitingToCaptureVpAfterDisambiguationProcedureSuccessful = true;
                        //MetaioDebug.log("Wait State:"+waitingToCaptureVpAfterDisambiguationProcedureSuccessful);
                    }
                    else
                    {
                        if (vpIsSuperSingle[coordinateSystemTrackedInPoseI-1])
                        {
                            //vpIsSuperSingle routine here
                            MetaioDebug.log("oTE: BLOCK 3: vpIsSuperSingle: MARKER_ID="+vpSuperMarkerId[coordinateSystemTrackedInPoseI-1]+" SIZE=20 ="+vpSuperIdIs20mm[coordinateSystemTrackedInPoseI-1]+" SIZE=100 ="+vpSuperIdIs100mm[coordinateSystemTrackedInPoseI-1]);
                            //resultSpecialTrk = setSuperSLAMExtendedTrackingConfiguration(   coordinateSystemTrackedInPoseI,
                            //                                                                    vpMarkerlessMarkerWidth[coordinateSystemTrackedInPoseI-1],
                            //                                                                   vpMarkerlessMarkerHeigth[coordinateSystemTrackedInPoseI-1],
                            //                                                                    trkLocalFilePath+seaMensorMarker);
                            //if (resultSpecialTrk) MetaioDebug.log("oTE: BLOCK 3: Single SLAM EXTENDED LOADED");
                            waitingToCaptureVpAfterDisambiguationProcedureSuccessful = true;
                            calledSuperTrackingSingleLoad = true;
                        }
                    }
				}


                // BLOCK 4
				// ETS lost
				// DURING MARKERLESS TRACKING
				// WHILE WAITING TO CAPTURE VP AFTER SUCCESSFUL DISAMBIGUATION
				// IF IT HAPPENS THE DISAMBIGUATION PROCEDURE MUST RESTART
                /*
				if ((v.getState()== ETRACKING_STATE.ETS_LOST) && (sensorType.equalsIgnoreCase("FeatureBasedSensorSource")) && (waitingToCaptureVpAfterDisambiguationProcedureSuccessful))
				{
					MetaioDebug.log("oTE: BLOCK 4:=================== FeatureBasedSensorSource LOST TRACKING");
					// RESTART THE DISAMBIGUATION PROCEDURE
                    vpIsDisambiguated = false;
					waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
				}
                */

				// ETS LOST
				if (v.getState()== ETRACKING_STATE.ETS_LOST)
					{
                        MetaioDebug.log("oTE: BLOCK 4:=================== ANY SOURCE LOST TRACKING: resultSpecialTrk ="+resultSpecialTrk+" calledIdTrackingLoad="+calledIdTrackingLoad+" idTrackingLoaded="+idTrackingLoaded);
                        if (lastVpPhotoRejected)
						{
							MetaioDebug.log("oTE: BLOCK 4:=================== ETS LOST TRACKING: lastVpPhotoRejected=true now setting to false!!!!");
							lastVpPhotoRejected=false;
						}
						if ((resultSpecialTrk) || ((calledIdTrackingLoad)&&(!idTrackingLoaded)) || (calledSuperTrackingSingleLoad))
                        {
                            MetaioDebug.log("oTE: BLOCK 4:== ANY SOURCE LOST TRACKING: Wait State:"+waitingToCaptureVpAfterDisambiguationProcedureSuccessful);
                            setMarkerlessTrackingConfiguration(markerlesstrackingConfigFileContents);
                            resultSpecialTrk = false;
                            calledIdTrackingLoad = false;
                            vpIsDisambiguated = false;
                            calledSuperTrackingSingleLoad = false;
                            if (waitingToCaptureVpAfterDisambiguationProcedureSuccessful) waitingToCaptureVpAfterDisambiguationProcedureSuccessful = false;
							if (doubleCheckingProcedureStarted)
							{
								doubleCheckingProcedureStarted = false;
								doubleCheckingProcedureFinalized = false;
							}
                        }
                        runOnUiThread(new Runnable()
                        {
                        @Override
                        public void run()
                        {
                            if (!showingVpLocationPhoto)
                            {
                                MetaioDebug.log("oTE: BLOCK 4: turning radar ON and crosshair OFF");
                                radarScanImageView.setVisibility(View.VISIBLE);
                                radarScanImageView.startAnimation(rotationRadarScan);
                                MetaioDebug.log("oTE: BLOCK 4:radarScanImageView.isShown()=" + radarScanImageView.isShown());
                                // TURNING OFF THE ATTITUDE INDICATOR SYSTEM
                                targetImageView.setVisibility(View.GONE);
                                swayRightImageView.setVisibility(View.GONE);
                                swayLeftImageView.setVisibility(View.GONE);
                                heaveUpImageView.setVisibility(View.GONE);
                                heaveDownImageView.setVisibility(View.GONE);
                                surgeImageView.setVisibility(View.GONE);
                                yawImageView.setVisibility(View.GONE);
                                pitchImageView.setVisibility(View.GONE);
                                rollCImageView.setVisibility(View.GONE);
                                roll01ImageView.setVisibility(View.GONE);
                                roll02ImageView.setVisibility(View.GONE);
                                roll03ImageView.setVisibility(View.GONE);
                                roll04ImageView.setVisibility(View.GONE);
                                roll05ImageView.setVisibility(View.GONE);
                                roll06ImageView.setVisibility(View.GONE);
                                roll07ImageView.setVisibility(View.GONE);
                                roll08ImageView.setVisibility(View.GONE);
                                roll09ImageView.setVisibility(View.GONE);
                                roll10ImageView.setVisibility(View.GONE);
                                roll11ImageView.setVisibility(View.GONE);
                                roll12ImageView.setVisibility(View.GONE);
                                roll13ImageView.setVisibility(View.GONE);
                                roll14ImageView.setVisibility(View.GONE);
                                roll15ImageView.setVisibility(View.GONE);
                            }
                        }
                        });
                        /*
                        Camera camera = IMetaioSDKAndroid.getCamera(getParent());
                        Camera.Parameters params = camera.getParameters();
                        if ((camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH)) && (!torchModeOn))
                        {
                            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                            camera.setParameters(params);
                        }
                        */
					}

                // BLOCK 5
                if (v.getState()== ETRACKING_STATE.ETS_REGISTERED)
                {
                    MetaioDebug.log("oTE: BLOCK 5:=================== ETS REGISTERED");
                }

                // BLOCK 6
                if (v.getState()== ETRACKING_STATE.ETS_INITIALIZATION_FAILED)
                {
                    MetaioDebug.log("oTE: BLOCK 6:=================== ETS INITIALIZATION_FAILED");
                }

                // BLOCK 7
                if (v.getState()== ETRACKING_STATE.ETS_UNKNOWN)
                {
                    MetaioDebug.log("oTE: BLOCK 7:=================== ETS UNKNOWN");
                }

                // BLOCK 8
                if (v.getState()== ETRACKING_STATE.ETS_INITIALIZED)
                {

                    MetaioDebug.log("oTE: BLOCK 8:=================== ETS INITIALIZED");
					MetaioDebug.log("oTE: BLOCK 8:ETS INITIALIZED : "+sensorType);
                    MetaioDebug.log("oTE: BLOCK 8:ETS INITIALIZED : etsInitializedAndNotFound = "+etsInitializedAndNotFound);
                    MetaioDebug.log("oTE: BLOCK 8:ETS INITIALIZED : resultSpecialTrk = "+resultSpecialTrk);
                    MetaioDebug.log("oTE: BLOCK 8:ETS INITIALIZED : idTrackingLoaded = "+idTrackingLoaded);
                    MetaioDebug.log("oTE: BLOCK 8:ETS INITIALIZED : calledIdTrackingLoad = "+calledIdTrackingLoad);
					if (sensorType.equalsIgnoreCase("DummySensorSource"))
					{
						waitingForMarkerlessTrackingConfigurationToLoad = true;
						MetaioDebug.log("oTE: BLOCK 8:=================== DummySensorSource: ETS INITIALIZED: CALLING setMarkerlessTrackingConfiguration");
						setMarkerlessTrackingConfiguration(markerlesstrackingConfigFileContents);
					}
                }

                // BLOCK 9
                if (((v.getState()== ETRACKING_STATE.ETS_INITIALIZED)&&(resultSpecialTrk))||((v.getState()== ETRACKING_STATE.ETS_INITIALIZED)&&(idTrackingLoaded)))
                {
                    etsInitializedAndNotFound = true;
                    millisWhenEtsWasInitialized = System.currentTimeMillis();
                    MetaioDebug.log("oTE: BLOCK 9:=================== ETS INITIALIZED FROM SPECIAL TRK OR IDTRK - millisWhenEtsWasInitialized="+millisWhenEtsWasInitialized);
                    MetaioDebug.log("oTE: BLOCK 9:ETS INITIALIZED : etsInitializedAndNotFound = "+etsInitializedAndNotFound);
                    MetaioDebug.log("oTE: BLOCK 9:ETS INITIALIZED : resultSpecialTrk = "+resultSpecialTrk);
                    MetaioDebug.log("oTE: BLOCK 9:ETS INITIALIZED : idTrackingLoaded = "+idTrackingLoaded);
                    MetaioDebug.log("oTE: BLOCK 9:ETS INITIALIZED : calledIdTrackingLoad = "+calledIdTrackingLoad);
                }

                // BLOCK 10
                if (v.getState()== ETRACKING_STATE.ETS_FOUND)
                {
                    MetaioDebug.log("oTE: BLOCK 10:=================== ETS FOUND");
                    if (etsInitializedAndNotFound)
                    {
                        elapsedMillisSinceEtsWasInitialized = 0;
                        etsInitializedAndNotFound=false;
                    }

                    MetaioDebug.log("oTE: BLOCK 10:ETS FOUND : etsInitializedAndNotFound = "+etsInitializedAndNotFound+" elapsedMillisSinceEtsWasInitialized="+elapsedMillisSinceEtsWasInitialized);
                }

                // BLOCK 11
                // ETS not tracking
				if (v.getState()== ETRACKING_STATE.ETS_NOT_TRACKING)
					{
					MetaioDebug.log("oTE: BLOCK 11:=================== ETS NOT TRACKING");
                    /*
					runOnUiThread(new Runnable() 
					{
					@Override
					public void run() 
					{
						if (!showingVpLocationPhoto)
						{
							radarScanImageView.setVisibility(View.VISIBLE);
							radarScanImageView.startAnimation(rotationRadarScan);
						}
						MetaioDebug.log("oTE: radarScanImageView.isShown()="+radarScanImageView.isShown());
					}
					});*/
					}


			}
		}
	}
}