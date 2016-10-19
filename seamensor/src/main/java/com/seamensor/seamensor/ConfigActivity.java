package com.seamensor.seamensor;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
import com.seamensor.seamensor.cognitoclient.AwsUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConfigActivity extends ARViewActivity implements OnItemClickListener
{
    private static final String TAG = "ConfigActivity";

    private IGeometry mSeaMensorCube;
    private IGeometry mVpChecked;
    private MetaioSDKCallbackHandler mSDKCallback;
    private TrackingValues mTrackingValues;
    private Rotation rotation;
    private Vector3d translation;
    private Vector3d cameraEulerAngle;
    private float trackingQuality;

    public final int idMarkerStdSize = 20;
    public final String trackingConfigFileName = "TrackingDataMarkerless.xml";
    public final String idMarkersTrackingConfigFileName = "TrckMarkers.xml";
    public final String superIdMarkersTrackingConfigFileName = "SuperIdlTrckMarkers.xml";
    public final short standardMarkerlessMarkerWidth = 500;
    public final short standardMarkerlessMarkerHeigth = 500;
    public final String seaMensorMarker = "seamensormarker.jpg"; //"seamensormarker46.png"; //"seamensormarker.jpg";
    public final short seaMensorMarkerWidthWhenIdIs20mm = 46;
    public final short seaMensorMarkerHeigthWhenIdIs20mm = 46;
    public final short seaMensorMarkerWidthWhenIdIs100mm = 134;
    public final short seaMensorMarkerHeigthWhenIdIs100mm = 134;
    public String markerlessTrackingConfigFilePath;
    public String globalLocalFilePath;
    public String superIdMarkersTrackingConfigFileContents;
    public String geometry3dConfigFile = "CuboVP.obj";
    public String geometrySecondary3dConfigFile = "vpchecked.obj";
    public String cameraCalibrationFileName = "cameracalibration.xml";
    public final String vpsConfigFileDropbox = "vps.xml";

    public boolean[] vpChecked;
    public boolean[] vpAcquired;
    public long[] vpConfiguredTimeSMCMillis;
    public long[] vpAcquiredTimeSMCMillis;
    public boolean doCheckPositionToTarget = false;
    public boolean cameraPhotoRequested = false;
    public boolean vpDescAndMarkerImageOK = false;
    public boolean trackingConfigIsSet = false;
    public boolean slamTrackingConfigIsSet = false;
    public boolean superIdTrackingConfigIsSet = false;
    public boolean waitingForVpSuperMarkerId = false;
    public boolean vpSuperMarkerIdFound = false;

    public static float tolerancePosition;
    public static float toleranceRotation;

    public short shipId;
    public short qtyVps = 0;
    public short vpListOrder = 0;
    public short maxQtyVps = 150;
    public boolean inFocus = false;
    public short vpIndex;
    public short captureMarkerWidth = 240;
    public short captureMarkerHeight = 240;
    public String frequencyUnit;
    public int frequencyValue;
    public int im;
    public int superCoordinateSystemTrackedInPoseI;
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
    public boolean[] vpIsAmbiguous;
    public boolean[] vpFlashTorchIsOn;
    public boolean[] vpIsSuperSingle;
    public boolean[] vpSuperIdIs20mm;
    public boolean[] vpSuperIdIs100mm;
    public int[] vpSuperMarkerId;
    public String[] vpFrequencyUnit;
    public long[] vpFrequencyValue;
    public static Bitmap vpLocationDescImageFileContents;
    public static Bitmap vpMarkerImageFileContents;
    public float[] focus;

    public String seamensorAccount;
    public String userNumber;
    public String userName;
    public String camCalDropboxPath;
    public String descvpRemotePath;
    public String markervpRemotePath;
    public String trackingRemotePath;
    public String vpsRemotePath;
    public String vpsConfiguredRemotePath;
    public int dciNumber;

    private static long back_pressed;

    ListView listView;
    ImageView imageView;
    ImageView targetImageView;
    Button okButton;
    Button requestPhotoButton;
    Button increaseQtyVps;
    Button decreaseQtyVps;
    Button saveTrkVpsData;
    Button ambiguousVpToggle;
    Button flashTorchVpToggle;
    Button superSingleVpToggle;
    Button superVpIdIs20mmToggle;
    Button superVpIdIs100mmToggle;
    EditText vpLocationDesEditText;
    TextView vpIdNumber;
    TextView vpAcquiredStatus;
    TextView xPosView;
    TextView yPosView;
    TextView zPosView;
    TextView xRotView;
    TextView yRotView;
    TextView zRotView;
    TextView trkQualityView;

    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;

    SharedPreferences sharedPref;

    public long sntpTime;
    public long sntpTimeReference;
    public boolean clockSetSuccess;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        sharedPref = this.getSharedPreferences("com.mymensor.app", Context.MODE_PRIVATE);

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());



        seamensorAccount = getIntent().getExtras().get("seamensoraccount").toString();
        dciNumber = Integer.parseInt(getIntent().getExtras().get("dcinumber").toString());
        qtyVps = Short.parseShort(getIntent().getExtras().get("QtyVps").toString());
        sntpTime = Long.parseLong(getIntent().getExtras().get("sntpTime").toString());
        sntpTimeReference = Long.parseLong(getIntent().getExtras().get("sntpReference").toString());
        clockSetSuccess = Boolean.parseBoolean(getIntent().getExtras().get("clockSetSuccess").toString());
        MetaioDebug.log("SeaMensor Account: "+seamensorAccount);

        mSDKCallback = new MetaioSDKCallbackHandler();
        // Load VPs data
        vpIndex = 1;
        descvpRemotePath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"dsc"+"/";
        markervpRemotePath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"mrk"+"/";
        trackingRemotePath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"trk"+"/";
        vpsRemotePath = seamensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/";
        vpsConfiguredRemotePath = seamensorAccount+"/"+"chk"+"/"+dciNumber+"/";

        File trackingConfigFile = new File(getApplicationContext().getFilesDir(),superIdMarkersTrackingConfigFileName);

        if (!trackingConfigFile.exists()){
            saveSuperIdMarkersTrackingConfig();
        }

        loadConfigurationFile();
    }


    @Override
    protected void onStart()
    {
        super.onStart();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        String[] novaLista = new String[maxQtyVps];//qtyVps
        for (int i=0; i<maxQtyVps; i++)//qtyVps
        {
            novaLista[i] = getString(R.string.vp_name)+(i+1)+"";
        }
        listView = (ListView) mGUIView.findViewById(R.id.vp_list);
        MetaioDebug.log("Listview: "+listView);
        listView.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_multiple_choice,novaLista));
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setOnItemClickListener(this);

        vpLocationDesEditText = (EditText) mGUIView.findViewById(R.id.descVPEditText);
        vpIdNumber = (TextView) mGUIView.findViewById(R.id.textView2);
        vpAcquiredStatus = (TextView) mGUIView.findViewById(R.id.vpAcquiredStatus);
        imageView = (TouchImageView) mGUIView.findViewById(R.id.imageView1);
        targetImageView = (ImageView) mGUIView.findViewById(R.id.imageViewTarget);

        okButton = (Button) mGUIView.findViewById(R.id.button2);
        requestPhotoButton = (Button) mGUIView.findViewById(R.id.buttonRequestPhoto);

        increaseQtyVps = (Button) mGUIView.findViewById(R.id.buttonIncreaseQtyVps);
        decreaseQtyVps = (Button) mGUIView.findViewById(R.id.buttonDecreaseQtyVps);
        saveTrkVpsData = (Button) mGUIView.findViewById(R.id.buttonSaveTrackingVpsData);
        ambiguousVpToggle = (Button) mGUIView.findViewById(R.id.buttonAmbiguousVpToggle);
        flashTorchVpToggle = (Button) mGUIView.findViewById(R.id.buttonFlashTorchVpToggle);
        superSingleVpToggle= (Button) mGUIView.findViewById(R.id.buttonSuperSingleVpToggle);
        superVpIdIs20mmToggle=(Button) mGUIView.findViewById(R.id.buttonId20mmToggle);
        superVpIdIs100mmToggle=(Button) mGUIView.findViewById(R.id.buttonId100mmToggle);

        xPosView = (TextView) mGUIView.findViewById(R.id.xPosView);
        yPosView = (TextView) mGUIView.findViewById(R.id.yPosView);
        zPosView = (TextView) mGUIView.findViewById(R.id.zPosView);
        xRotView = (TextView) mGUIView.findViewById(R.id.xRotView);
        yRotView = (TextView) mGUIView.findViewById(R.id.yRotView);
        zRotView = (TextView) mGUIView.findViewById(R.id.zRotView);
        trkQualityView = (TextView) mGUIView.findViewById(R.id.trkQualityView);

    }


    @Override
    public void onBackPressed()
    {
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
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
            MetaioDebug.log("CameraSMC: Name: "+camera);
            MetaioDebug.log("CameraSMC: Res: "+camera.getResolution());
            MetaioDebug.log("CameraSMC: FPS: "+camera.getFps());
        }
        else
        {
            MetaioDebug.log(Log.WARN, "No camera found on the device!");
        }
    }


    @Override
    protected void onResume()
    {
        super.onResume();
        MetaioDebug.log("ARViewActivity.onResume SMC");
        setVpsChecked();
    }


    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mSDKCallback.delete();
        mSDKCallback = null;
    }

    @Override
    protected int getGUILayout()
    {
        // Attaching layout to the activity
        return R.layout.activity_config;
    }

    public void setVpsChecked()
    {
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
                        listView.setItemChecked(i, vpChecked[i]);
                        MetaioDebug.log("setItemChecked = "+i+" vpChecked = "+vpChecked[i]);
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

    public void checkPositionToTarget(TrackingValuesVector poses, final int i)
    {
        //mTrackingValues = metaioSDK.getTrackingValues(poses.get(i).getCoordinateSystemID());
        //MetaioDebug.log("checkPositionToTarget: Pose = "+i);
        //MetaioDebug.log("checkPositionToTarget: Called");
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                trkQualityView.setText("qL="+Float.toString(Math.round(trackingQuality)));
                trkQualityView.setVisibility(View.VISIBLE);
            }
        });

        if (mTrackingValues.isTrackingState())
        {
            translation = mTrackingValues.getTranslation();
            trackingQuality = mTrackingValues.getQuality();
            rotation = mTrackingValues.getRotation();
            //inverseRotation = rotation.inverse();
            //inverseRotation.getRotationMatrix(inverseRotationMatrix);
            //cameraPos =
            cameraEulerAngle = rotation.getEulerAngleDegrees();
            final float posX = translation.getX();
            final float posY = translation.getY();
            final float posZ = translation.getZ();
            final float rotX = cameraEulerAngle.getX();
            final float rotY = cameraEulerAngle.getY();
            final float rotZ = cameraEulerAngle.getZ();
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

            //MetaioDebug.log("checkPositionToTarget: Call setSuperIdMarkersTrackingConfig()???: "+((!superIdTrackingConfigIsSet)&&(!waitingForVpSuperMarkerId)));
            if((!superIdTrackingConfigIsSet)&&(!waitingForVpSuperMarkerId))
            {
                if (vpIsSuperSingle[vpIndex-1])
                {
                    vpSuperMarkerIdFound = false;
                    waitingForVpSuperMarkerId = true;
                    setSuperIdMarkersTrackingConfig();
                }
            }

            if ((!vpAcquired[vpIndex-1]) && (vpDescAndMarkerImageOK) && (slamTrackingConfigIsSet) && (vpIsSuperSingle[vpIndex-1]))
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        targetImageView.setVisibility(View.VISIBLE);
                        targetImageView.setOnTouchListener(new OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN)
                                {
                                    vpXCameraDistance[vpIndex-1] = Math.round(posX*(-1.0f));
                                    vpYCameraDistance[vpIndex-1] = Math.round(posY*(-1.0f));
                                    vpZCameraDistance[vpIndex-1] = Math.round(posZ);
                                    vpXCameraRotation[vpIndex-1] = Math.round(rotX);
                                    vpYCameraRotation[vpIndex-1] = Math.round(rotY);
                                    vpZCameraRotation[vpIndex-1] = Math.round(rotZ);
                                    vpAcquired[vpIndex-1]=true;
                                    vpAcquiredTimeSMCMillis[vpIndex-1]=System.currentTimeMillis();
                                    vpAcquiredStatus.setText(R.string.vpAcquiredStatus);
                                    MetaioDebug.log("checkPositionToTarget: OnTouchListener:Setting to true: VpAcquired: ["+(vpIndex-1)+"] = "+vpAcquired[vpIndex-1]);
                                    MetaioDebug.log("checkPositionToTarget: OnTouchListener:vpAcquiredTimeSMCMillis["+(vpIndex-1)+"] = "+vpAcquiredTimeSMCMillis[vpIndex-1]);
                                };
                                return false;
                            }
                        });
                    }
                });
            }

            if ((!vpAcquired[vpIndex-1]) && (vpDescAndMarkerImageOK) && (trackingConfigIsSet) && (!vpIsSuperSingle[vpIndex-1]) )
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        vpXCameraDistance[vpIndex-1] = Math.round(posX*(-1.0f));
                        vpYCameraDistance[vpIndex-1] = Math.round(posY*(-1.0f));
                        vpZCameraDistance[vpIndex-1] = Math.round(posZ);
                        vpXCameraRotation[vpIndex-1] = Math.round(rotX);
                        vpYCameraRotation[vpIndex-1] = Math.round(rotY);
                        vpZCameraRotation[vpIndex-1] = Math.round(rotZ);
                        vpAcquired[vpIndex-1]=true;
                        vpAcquiredTimeSMCMillis[vpIndex-1]=System.currentTimeMillis();
                        vpAcquiredStatus.setText(R.string.vpAcquiredStatus);
                        MetaioDebug.log("checkPositionToTarget: Setting to true: VpAcquired: ["+(vpIndex-1)+"] = "+vpAcquired[vpIndex-1]);
                        MetaioDebug.log("checkPositionToTarget: vpAcquiredTimeSMCMillis["+(vpIndex-1)+"] = "+vpAcquiredTimeSMCMillis[vpIndex-1]);
                    }
                });
            }
        }
    }


    @Override
    public void onDrawFrame()
    {
        super.onDrawFrame();
        if (metaioSDK != null)
        {
            TrackingValuesVector poses = metaioSDK.getTrackingValues();
            //if we have detected one, attach our 3d model to this coordinate system Id
            if (poses.size() > 0)
                for (im=0; im<poses.size(); im++)
                {
                    mTrackingValues = metaioSDK.getTrackingValues(poses.get(im).getCoordinateSystemID());
                    translation = mTrackingValues.getTranslation();
                    if (mTrackingValues.isTrackingState())
                    {
                        if (vpAcquired[vpIndex - 1]) {
                            mVpChecked.setTranslation(new Vector3d(0f, 0f, 0f), false);
                            mVpChecked.setVisible(true);
                            mVpChecked.setCoordinateSystemID(poses.get(im).getCoordinateSystemID());
                        } else {
                            mSeaMensorCube.setScale(new Vector3d(10f, 10f, 10f));
                            mSeaMensorCube.setTranslation(new Vector3d(0f, 0f, 0f), false);
                            mSeaMensorCube.setVisible(true);
                            mSeaMensorCube.setCoordinateSystemID(poses.get(im).getCoordinateSystemID());
                        }
                    }
                    if (doCheckPositionToTarget) checkPositionToTarget(poses,im);
                }
        }
    }


    public void setSuperIdMarkersTrackingConfig()
    {
        MetaioDebug.log("setSuperIdMarkersTrackingConfig: called: vpIndex="+vpIndex);
        if (metaioSDK.setTrackingConfiguration(superIdMarkersTrackingConfigFileContents, false))
        {
            superIdTrackingConfigIsSet = true;
            MetaioDebug.log("setSuperIdMarkersTrackingConfig: New Tracking Config Loaded: vpIndex="+vpIndex);
        }
        else
        {
            MetaioDebug.log(Log.ERROR,"setSuperIdMarkersTrackingConfig: Failed to load Marker Config: vpIndex="+vpIndex);
        };
    }


    public void setTrackingConfig()
    {
        MetaioDebug.log("setTrackingConfig: called: vpIndex="+vpIndex);
        trackingConfigIsSet = false;
        String markerFilePath = null;
        String trkcfg = null;
        markerFilePath = globalLocalFilePath+"markervp"+vpIndex+".jpg";
        MetaioDebug.log("setTrackingConfig: Normal Markerless File Path="+markerFilePath);
        trkcfg ="<?xml version=\"1.0\"?>"+
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
                "<ReferenceImage WidthMM=\""+vpMarkerlessMarkerWidth[vpIndex-1]+"\" HeigthMM=\""+vpMarkerlessMarkerHeigth[vpIndex-1]+"\">"+markerFilePath+"</ReferenceImage>"+
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
            trackingConfigIsSet = true;
            MetaioDebug.log("New Tracking Config Loaded: vpIndex="+vpIndex);
        }
        else
        {
            MetaioDebug.log(Log.ERROR,"Failed to load Marker for Markerless Traking Config for VP capture: vpIndex="+vpIndex);
        };
    }


    public void setSuperSingleIdMarkerTrackingConfig()
    {
        MetaioDebug.log("setSuperSingleIdMarkerTrackingConfig: called: vpIndex="+vpIndex);
        //"<ReferenceImage WidthMM=\""+vpMarkerlessMarkerWidth[vpIndex-1]+"\" HeigthMM=\""+vpMarkerlessMarkerHeigth[vpIndex-1]+"\">"+markerFilePath+"</ReferenceImage>"+
        slamTrackingConfigIsSet = false;
        String trkcfg = null;
        if (vpIsSuperSingle[vpIndex-1])
        {
            trkcfg ="<?xml version=\"1.0\"?>"+
                    "<TrackingData>"+
                    "<Sensors>"+
                        "<Sensor Type=\"MarkerBasedSensorSource\">"+
                            "<SensorID>MarkerTracking1</SensorID>"+
                            "<Parameters>"+
                                "<MarkerTrackingParameters>"+
                                    "<TrackingQuality>robust</TrackingQuality>"+
                                    "<ThresholdOffset>120</ThresholdOffset>"+
                                    "<NumberOfSearchIterations>5</NumberOfSearchIterations>"+
                                "</MarkerTrackingParameters>"+
                            "</Parameters>"+
                            "<SensorCOS>"+
                                "<SensorCosID>Marker"+vpIndex+"</SensorCosID>"+
                                "<Parameters>"+
                                    "<MarkerParameters>"+
                                        "<Size>";
            if ((!vpSuperIdIs20mm[vpIndex-1])&&(!vpSuperIdIs100mm[vpIndex-1]))
            {
                trkcfg=trkcfg+idMarkerStdSize;
            }
            else
            {
                if (vpSuperIdIs20mm[vpIndex-1])
                {
                    trkcfg=trkcfg+"20";
                }
                else
                {
                    if (vpSuperIdIs100mm[vpIndex-1]) trkcfg=trkcfg+"100";
                }
            }
            trkcfg = trkcfg + "</Size>"+
                                        "<MatrixID>"+vpSuperMarkerId[vpIndex-1]+"</MatrixID>"+
                                    "</MarkerParameters>"+
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
                                    "<AlphaTranslation>1.0</AlphaTranslation>"+
                                    "<GammaRotation>0.8</GammaRotation>"+
                                    "<GammaTranslation>1.0</GammaTranslation>"+
                                    "<KeepPoseForNumberOfFrames>3</KeepPoseForNumberOfFrames>"+
                                "</Parameters>"+
                            "</Fuser>"+
                            "<SensorSource>"+
                                "<SensorID>Markertracking1</SensorID>"+
                                "<SensorCosID>Marker"+vpIndex+"</SensorCosID>"+
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
        }

        if (metaioSDK.setTrackingConfiguration(trkcfg, false))
        {
            slamTrackingConfigIsSet = true;
            MetaioDebug.log("setSuperSingleIdMarkerTrackingConfig: New Tracking Config Loaded: vpIndex="+vpIndex);
        }
        else
        {
            MetaioDebug.log(Log.ERROR,"setSuperSingleIdMarkerTrackingConfig: Failed to load Marker for Markerless Traking Config for VP capture: vpIndex="+vpIndex);
        };
    }


    public void setSLAMTrackingConfig()
    {
        MetaioDebug.log("setTrackingConfig: called: vpIndex="+vpIndex);
        //"<ReferenceImage WidthMM=\""+vpMarkerlessMarkerWidth[vpIndex-1]+"\" HeigthMM=\""+vpMarkerlessMarkerHeigth[vpIndex-1]+"\">"+markerFilePath+"</ReferenceImage>"+
        slamTrackingConfigIsSet = false;
        String markerFilePath = null;
        String trkcfg = null;
        if (vpIsSuperSingle[vpIndex-1])
        {
            markerFilePath = globalLocalFilePath+seaMensorMarker;
            MetaioDebug.log("setSLAMTrackingConfig: SLAM Extended Markerless File Path="+markerFilePath);
            MetaioDebug.log("setSLAMTrackingConfig: vpMarkerlessMarkerWidth[vpIndex-1]="+vpMarkerlessMarkerWidth[vpIndex-1]);
            MetaioDebug.log("setSLAMTrackingConfig: vpMarkerlessMarkerHeigth[vpIndex-1]="+vpMarkerlessMarkerHeigth[vpIndex-1]);
            trkcfg = "<?xml version=\"1.0\"?>"+
                    "<TrackingData>"+
                        "<Sensors>"+
                            "<Sensor Type=\"SLAMSensorSource\" Version=\"2\">"+
                                "<SensorID>SLAM</SensorID>"+
                                "<Parameters>"+
                                    "<MaxObjectsToDetectPerFrame>5</MaxObjectsToDetectPerFrame>"+
                                    "<MaxObjectsToTrackInParallel>1</MaxObjectsToTrackInParallel>"+
                                "</Parameters>"+
                                "<SensorCOS>"+
                                    "<SensorCosID>world</SensorCosID>"+
                                    "<Parameters>"+
                                        "<Initialization type=\"image\">"+
                                            "<ReferenceImage WidthMM=\""+vpMarkerlessMarkerWidth[vpIndex-1]+"\" HeigthMM=\""+vpMarkerlessMarkerHeigth[vpIndex-1]+"\">"+markerFilePath+"</ReferenceImage>"+
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
                                        "<KeepPoseForNumberOfFrames>0</KeepPoseForNumberOfFrames>"+
                                    "</Parameters>"+
                                "</Fuser>"+
                                "<SensorSource>"+
                                    "<SensorID>SLAM</SensorID>"+
                                    "<SensorCosID>world</SensorCosID>"+
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
                                    "</COSOffset>"+
                                "</SensorSource>"+
                            "</COS>"+
                        "</Connections>"+
                    "</TrackingData>";
        }

        if (metaioSDK.setTrackingConfiguration(trkcfg, false))
        {
            slamTrackingConfigIsSet = true;
            MetaioDebug.log("setSLAMTrackingConfig: New Tracking Config Loaded: vpIndex="+vpIndex);
        }
        else
        {
            MetaioDebug.log(Log.ERROR,"setSLAMTrackingConfig: Failed to load Marker for Markerless Traking Config for VP capture: vpIndex="+vpIndex);
        };
    }


    public void saveTrackingConfig()
    {
        for (int i=0; i<qtyVps; i++)
        {
            MetaioDebug.log("Save Track Config  i="+i+" vpAcquired[i]="+vpAcquired[i]);
            if (!vpAcquired[i])
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String message = getString(R.string.not_all_vps_acquired);
                        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
                return;
            }
        }
        for (int i=0; i<qtyVps; i++)
        {
            MetaioDebug.log("saveTrackingConfig()  i="+i+" vpLocationDesText[i]="+vpLocationDesText[i]);
            if (vpLocationDesText[i]==null)
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        String message = getString(R.string.vp_location_des_text_missing);
                        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                });
                return;
            }
        }
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                String message = getString(R.string.button_text_save_tracking_vps_data);
                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });

        for (int i=0; i<qtyVps; i++)
        {
            if (vpMarkerlessMarkerWidth[i]==0)
            {
                if (vpIsSuperSingle[i])
                {
                    if (vpSuperIdIs20mm[i]) vpMarkerlessMarkerWidth[i]=seaMensorMarkerWidthWhenIdIs20mm;
                            else vpMarkerlessMarkerWidth[i]=seaMensorMarkerWidthWhenIdIs100mm;
                }
                else vpMarkerlessMarkerWidth[i]= standardMarkerlessMarkerWidth;
            }
            if (vpMarkerlessMarkerHeigth[i]==0)
            {
                if (vpIsSuperSingle[i])
                {
                    if (vpSuperIdIs20mm[i]) vpMarkerlessMarkerHeigth[i]=seaMensorMarkerHeigthWhenIdIs20mm;
                    else vpMarkerlessMarkerHeigth[i]=seaMensorMarkerHeigthWhenIdIs100mm;
                }
                else vpMarkerlessMarkerHeigth[i]= standardMarkerlessMarkerHeigth;
            }
        }

        // Saving Markerless 2d Tracking configuration for Seamensor.
        try
        {
            // Getting a file path for tracking configuration XML file


            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("","TrackingData");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Sensors");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Sensor");
            xmlSerializer.attribute("", "Type", "FeatureBasedSensorSource");
            xmlSerializer.attribute("", "Subtype", "Fast");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","SensorID");
            xmlSerializer.text("FeatureTracking1");
            xmlSerializer.endTag("","SensorID");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Parameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","FeatureDescriptorAlignment");
            xmlSerializer.text("regular");
            xmlSerializer.endTag("","FeatureDescriptorAlignment");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","MaxObjectsToDetectPerFrame");
            xmlSerializer.text("5");
            xmlSerializer.endTag("","MaxObjectsToDetectPerFrame");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","MaxObjectsToTrackInParallel");
            xmlSerializer.text("1");
            xmlSerializer.endTag("","MaxObjectsToTrackInParallel");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","SimilarityThreshold");
            xmlSerializer.text("0.7");
            xmlSerializer.endTag("","SimilarityThreshold");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Parameters");
            for (int i=1; i<(qtyVps+1); i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorCOS");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorCosID");
                xmlSerializer.text("Patch"+i);
                xmlSerializer.endTag("","SensorCosID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","ReferenceImage");
                xmlSerializer.attribute("", "WidthMM", Short.toString(vpMarkerlessMarkerWidth[i-1]));
                xmlSerializer.attribute("", "HeigthMM", Short.toString(vpMarkerlessMarkerHeigth[i-1]));
                if (vpIsSuperSingle[i-1])
                {
                    xmlSerializer.text(seaMensorMarker);
                }
                else
                {
                    xmlSerializer.text("markervp"+i+".jpg");
                }
                xmlSerializer.endTag("","ReferenceImage");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SimilarityThreshold");
                xmlSerializer.text("0.7");
                xmlSerializer.endTag("","SimilarityThreshold");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","SensorCOS");
            }
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Sensor");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Sensors");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Connections");
            for (int i=1; i<(qtyVps+1); i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","COS");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Name");
                xmlSerializer.text("MarkerlessCOS"+i);
                xmlSerializer.endTag("","Name");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Fuser");
                xmlSerializer.attribute("", "Type", "SmoothingFuser");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","KeepPoseForNumberOfFrames");
                xmlSerializer.text("10");
                xmlSerializer.endTag("","KeepPoseForNumberOfFrames");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","AlphaTranslation");
                xmlSerializer.text("0.8");
                xmlSerializer.endTag("","AlphaTranslation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","GammaTranslation");
                xmlSerializer.text("0.5");
                xmlSerializer.endTag("","GammaTranslation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","AlphaRotation");
                xmlSerializer.text("0.5");
                xmlSerializer.endTag("","AlphaRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","GammaRotation");
                xmlSerializer.text("0.5");
                xmlSerializer.endTag("","GammaRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","ContinueLostTrackingWithOrientationSensor");
                xmlSerializer.text("false");
                xmlSerializer.endTag("","ContinueLostTrackingWithOrientationSensor");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Fuser");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorSource");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorID");
                xmlSerializer.text("FeatureTracking1");
                xmlSerializer.endTag("","SensorID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorCosID");
                xmlSerializer.text("Patch"+i);
                xmlSerializer.endTag("","SensorCosID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","HandEyeCalibration");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","W");
                xmlSerializer.text("1");
                xmlSerializer.endTag("","W");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","HandEyeCalibration");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","COSOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","W");
                xmlSerializer.text("1");
                xmlSerializer.endTag("","W");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","COSOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","SensorSource");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","COS");
            }
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Connections");
            xmlSerializer.text("\n");
            xmlSerializer.endTag("","TrackingData");
            xmlSerializer.endDocument();
            String trackingFileContents = writer.toString();
            File trackingConfigFile = new File(getApplicationContext().getFilesDir(),trackingConfigFileName);
            FileUtils.writeStringToFile(trackingConfigFile,trackingFileContents, UTF_8);
            ObjectMetadata myObjectMetadata = new ObjectMetadata();
            //create a map to store user metadata
            Map<String, String> userMetadata = new HashMap<String,String>();
            userMetadata.put("TimeStamp", Utils.timeNow(clockSetSuccess,sntpTime,sntpTimeReference).toString());
            myObjectMetadata.setUserMetadata(userMetadata);
            TransferObserver observer = Utils.storeRemoteFile(transferUtility, (trackingRemotePath + trackingConfigFileName), Constants.BUCKET_NAME, trackingConfigFile, myObjectMetadata);
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
                }

                @Override
                public void onError(int id, Exception ex) {
                    Log.e(TAG, "saveTrackingConfig(): trackingConfigFile saving failed, see stack trace"+ ex.toString());
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "Seamensor Tracking Data Markerless file data saving to Dropbox failed, see stack trace");
        }
    }

    public void saveVpsData()
    {
        // Saving Vps Data configuration.
        try
        {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("","VpsData");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Parameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","ShipId");
            xmlSerializer.text(Short.toString(shipId));
            xmlSerializer.endTag("","ShipId");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","FrequencyUnit");
            xmlSerializer.text(frequencyUnit);
            xmlSerializer.endTag("","FrequencyUnit");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","FrequencyValue");
            xmlSerializer.text(Integer.toString(frequencyValue));
            xmlSerializer.endTag("","FrequencyValue");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","QtyVps");
            xmlSerializer.text(Short.toString(qtyVps));
            xmlSerializer.endTag("","QtyVps");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","TolerancePosition");
            xmlSerializer.text(Float.toString(tolerancePosition));
            xmlSerializer.endTag("","TolerancePosition");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","ToleranceRotation");
            xmlSerializer.text(Float.toString(toleranceRotation));
            xmlSerializer.endTag("","ToleranceRotation");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Parameters");
            for (int i=1; i<(qtyVps+1); i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Vp");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpNumber");
                xmlSerializer.text(Integer.toString(i));
                xmlSerializer.endTag("","VpNumber");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpXCameraDistance");
                xmlSerializer.text(Integer.toString(vpXCameraDistance[i-1]));
                xmlSerializer.endTag("","VpXCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpYCameraDistance");
                xmlSerializer.text(Integer.toString(vpYCameraDistance[i-1]));
                xmlSerializer.endTag("","VpYCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpZCameraDistance");
                xmlSerializer.text(Integer.toString(vpZCameraDistance[i-1]));
                xmlSerializer.endTag("","VpZCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpXCameraRotation");
                xmlSerializer.text(Integer.toString(vpXCameraRotation[i-1]));
                xmlSerializer.endTag("","VpXCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpYCameraRotation");
                xmlSerializer.text(Integer.toString(vpYCameraRotation[i-1]));
                xmlSerializer.endTag("","VpYCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpZCameraRotation");
                xmlSerializer.text(Integer.toString(vpZCameraRotation[i-1]));
                xmlSerializer.endTag("","VpZCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpLocDescription");
                xmlSerializer.text(vpLocationDesText[i-1]);
                xmlSerializer.endTag("","VpLocDescription");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpMarkerlessMarkerWidth");
                xmlSerializer.text(Short.toString(vpMarkerlessMarkerWidth[i-1]));
                xmlSerializer.endTag("","VpMarkerlessMarkerWidth");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpMarkerlessMarkerHeigth");
                xmlSerializer.text(Short.toString(vpMarkerlessMarkerHeigth[i-1]));
                xmlSerializer.endTag("","VpMarkerlessMarkerHeigth");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsAmbiguous");
                xmlSerializer.text(Boolean.toString(vpIsAmbiguous[i-1]));
                xmlSerializer.endTag("","VpIsAmbiguous");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpFlashTorchIsOn");
                xmlSerializer.text(Boolean.toString(vpFlashTorchIsOn[i-1]));
                xmlSerializer.endTag("","VpFlashTorchIsOn");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsSuperSingle");
                xmlSerializer.text(Boolean.toString(vpIsSuperSingle[i-1]));
                xmlSerializer.endTag("","VpIsSuperSingle");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpSuperIdIs20mm");
                xmlSerializer.text(Boolean.toString(vpSuperIdIs20mm[i-1]));
                xmlSerializer.endTag("","VpSuperIdIs20mm");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","vpSuperIdIs100mm");
                xmlSerializer.text(Boolean.toString(vpSuperIdIs100mm[i-1]));
                xmlSerializer.endTag("","vpSuperIdIs100mm");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpSuperMarkerId");
                if (vpIsSuperSingle[i-1])
                {
                    xmlSerializer.text(Integer.toString(vpSuperMarkerId[i-1]));
                }
                else
                {
                    xmlSerializer.text(Integer.toString(0));
                }
                xmlSerializer.endTag("","VpSuperMarkerId");
                if (vpFrequencyUnit[i-1]!=frequencyUnit)
                {
                    xmlSerializer.text("\n");
                    xmlSerializer.text("\t");
                    xmlSerializer.text("\t");
                    xmlSerializer.startTag("","VpFrequencyUnit");
                    xmlSerializer.text(vpFrequencyUnit[i-1]);
                    xmlSerializer.endTag("","VpFrequencyUnit");
                }
                if (vpFrequencyValue[i-1]!=frequencyValue)
                {
                    xmlSerializer.text("\n");
                    xmlSerializer.text("\t");
                    xmlSerializer.text("\t");
                    xmlSerializer.startTag("","VpFrequencyValue");
                    xmlSerializer.text(Long.toString(vpFrequencyValue[i-1]));
                    xmlSerializer.endTag("","VpFrequencyValue");
                }
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Vp");
            }
            xmlSerializer.text("\n");
            xmlSerializer.endTag("","VpsData");
            xmlSerializer.endDocument();
            String vpsConfigFileContents = writer.toString();
            File vpsConfigFile = new File(getApplicationContext().getFilesDir(),vpsConfigFileDropbox);
            FileUtils.writeStringToFile(vpsConfigFile,vpsConfigFileContents, UTF_8);
            ObjectMetadata myObjectMetadata = new ObjectMetadata();
            //create a map to store user metadata
            Map<String, String> userMetadata = new HashMap<String,String>();
            userMetadata.put("TimeStamp", Utils.timeNow(clockSetSuccess,sntpTime,sntpTimeReference).toString());
            myObjectMetadata.setUserMetadata(userMetadata);
            TransferObserver observer = Utils.storeRemoteFile(transferUtility, (vpsRemotePath + vpsConfigFileDropbox), Constants.BUCKET_NAME, vpsConfigFile, myObjectMetadata);
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
                    Log.e(TAG, "saveVpsData(): vpsConfigFile saving failed, see stack trace"+ ex.toString());
                }

            });

        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "Vps Data file saving to Dropbox failed, see stack trace");
        }
        saveIdMarkersTrackingConfig();
    }


    public void saveIdMarkersTrackingConfig()
    {
        // Saving ID Markers Tracking configuration.

        try
        {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("","TrackingData");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Sensors");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Sensor");
            xmlSerializer.attribute("", "Type", "MarkerBasedSensorSource");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","SensorID");
            xmlSerializer.text("MarkerTracking1");
            xmlSerializer.endTag("","SensorID");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Parameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","MarkerTrackingParameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","TrackingQuality");
            xmlSerializer.text("robust");
            xmlSerializer.endTag("","TrackingQuality");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","ThresholdOffset");
            xmlSerializer.text("120");
            xmlSerializer.endTag("","ThresholdOffset");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","NumberOfSearchIterations");
            xmlSerializer.text("5");
            xmlSerializer.endTag("","NumberOfSearchIterations");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","MarkerTrackingParameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Parameters");
            for (int i=1; i<(qtyVps+1); i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorCOS");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorCosID");
                xmlSerializer.text("Marker"+i);
                xmlSerializer.endTag("","SensorCosID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","MarkerParameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Size");
                if ((!vpSuperIdIs20mm[i-1])&&(!vpSuperIdIs100mm[i-1]))
                {
                    xmlSerializer.text(Integer.toString(idMarkerStdSize));
                }
                else
                {
                    if (vpSuperIdIs20mm[i-1])
                    {
                        xmlSerializer.text("20");
                    }
                    else
                    {
                        if (vpSuperIdIs100mm[i-1]) xmlSerializer.text("100");
                    }
                }
                xmlSerializer.endTag("","Size");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","MatrixID");
                if (vpIsSuperSingle[i-1])
                {
                    xmlSerializer.text(Integer.toString(vpSuperMarkerId[i-1]));
                }
                else
                {
                    xmlSerializer.text(Integer.toString(i));
                }
                xmlSerializer.endTag("","MatrixID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","MarkerParameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","SensorCOS");
            }
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Sensor");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Sensors");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Connections");
            for (int i=1; i<(qtyVps+1); i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","COS");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Name");
                xmlSerializer.text("COS"+i);
                xmlSerializer.endTag("","Name");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Fuser");
                xmlSerializer.attribute("", "Type", "SmoothingFuser");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","AlphaRotation");
                xmlSerializer.text("0.8");
                xmlSerializer.endTag("","AlphaRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","AlphaTranslation");
                xmlSerializer.text("1.0");
                xmlSerializer.endTag("","AlphaTranslation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","GammaRotation");
                xmlSerializer.text("0.8");
                xmlSerializer.endTag("","GammaRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","GammaTranslation");
                xmlSerializer.text("1.0");
                xmlSerializer.endTag("","GammaTranslation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","KeepPoseForNumberOfFrames");
                xmlSerializer.text("3");
                xmlSerializer.endTag("","KeepPoseForNumberOfFrames");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Fuser");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorSource");
                //xmlSerializer.attribute("", "trigger", "1");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorID");
                xmlSerializer.text("Markertracking1");
                xmlSerializer.endTag("","SensorID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorCosID");
                xmlSerializer.text("Marker"+i);
                xmlSerializer.endTag("","SensorCosID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","HandEyeCalibration");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","W");
                xmlSerializer.text("1");
                xmlSerializer.endTag("","W");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","HandEyeCalibration");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","COSOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","W");
                xmlSerializer.text("1");
                xmlSerializer.endTag("","W");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","COSOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","SensorSource");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","COS");
            }
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Connections");
            xmlSerializer.text("\n");
            xmlSerializer.endTag("","TrackingData");
            xmlSerializer.endDocument();
            String trackingFileContents = writer.toString();
            File trackingConfigFile = new File(getApplicationContext().getFilesDir(),idMarkersTrackingConfigFileName);
            FileUtils.writeStringToFile(trackingConfigFile,trackingFileContents, UTF_8);
            ObjectMetadata myObjectMetadata = new ObjectMetadata();
            //create a map to store user metadata
            Map<String, String> userMetadata = new HashMap<String,String>();
            userMetadata.put("TimeStamp", Utils.timeNow(clockSetSuccess,sntpTime,sntpTimeReference).toString());
            myObjectMetadata.setUserMetadata(userMetadata);
            TransferObserver observer = Utils.storeRemoteFile(transferUtility, (trackingRemotePath + idMarkersTrackingConfigFileName), Constants.BUCKET_NAME, trackingConfigFile, myObjectMetadata);
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
                }

                @Override
                public void onError(int id, Exception ex) {
                    Log.e(TAG, "saveIdMarkersTrackingConfig(): trackingConfigFile saving failed, see stack trace"+ ex.toString());
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "ID Tracking Markers Data file saving to Dropbox failed, see stack trace");
        }
    }


    public void saveSuperIdMarkersTrackingConfig()
    {
        // Saving Total ID Markers Tracking configuration.

        try
        {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("","TrackingData");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Sensors");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Sensor");
            xmlSerializer.attribute("", "Type", "MarkerBasedSensorSource");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","SensorID");
            xmlSerializer.text("MarkerTracking1");
            xmlSerializer.endTag("","SensorID");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Parameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","MarkerTrackingParameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","TrackingQuality");
            xmlSerializer.text("robust");
            xmlSerializer.endTag("","TrackingQuality");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","ThresholdOffset");
            xmlSerializer.text("120");
            xmlSerializer.endTag("","ThresholdOffset");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","NumberOfSearchIterations");
            xmlSerializer.text("5");
            xmlSerializer.endTag("","NumberOfSearchIterations");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","MarkerTrackingParameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Parameters");
            for (int i=301; i<501; i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorCOS");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorCosID");
                xmlSerializer.text("Marker"+i);
                xmlSerializer.endTag("","SensorCosID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","MarkerParameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Size");
                if (i<401)
                {
                    xmlSerializer.text("20");
                }
                else
                {
                    xmlSerializer.text("100");
                }
                xmlSerializer.endTag("","Size");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","MatrixID");
                xmlSerializer.text(Integer.toString(i));
                xmlSerializer.endTag("","MatrixID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","MarkerParameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","SensorCOS");
            }
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Sensor");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Sensors");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Connections");
            for (int i=301; i<501; i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","COS");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Name");
                xmlSerializer.text("COS"+i);
                xmlSerializer.endTag("","Name");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Fuser");
                xmlSerializer.attribute("", "Type", "SmoothingFuser");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","AlphaRotation");
                xmlSerializer.text("0.8");
                xmlSerializer.endTag("","AlphaRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","AlphaTranslation");
                xmlSerializer.text("1.0");
                xmlSerializer.endTag("","AlphaTranslation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","GammaRotation");
                xmlSerializer.text("0.8");
                xmlSerializer.endTag("","GammaRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","GammaTranslation");
                xmlSerializer.text("1.0");
                xmlSerializer.endTag("","GammaTranslation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","KeepPoseForNumberOfFrames");
                xmlSerializer.text("3");
                xmlSerializer.endTag("","KeepPoseForNumberOfFrames");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Parameters");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Fuser");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorSource");
                //xmlSerializer.attribute("", "trigger", "1");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorID");
                xmlSerializer.text("Markertracking1");
                xmlSerializer.endTag("","SensorID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","SensorCosID");
                xmlSerializer.text("Marker"+i);
                xmlSerializer.endTag("","SensorCosID");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","HandEyeCalibration");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","W");
                xmlSerializer.text("1");
                xmlSerializer.endTag("","W");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","HandEyeCalibration");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","COSOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","TranslationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","X");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","X");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Y");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Y");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Z");
                xmlSerializer.text("0");
                xmlSerializer.endTag("","Z");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","W");
                xmlSerializer.text("1");
                xmlSerializer.endTag("","W");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","RotationOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","COSOffset");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","SensorSource");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","COS");
            }
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Connections");
            xmlSerializer.text("\n");
            xmlSerializer.endTag("","TrackingData");
            xmlSerializer.endDocument();
            String trackingFileContents = writer.toString();
            File trackingConfigFile = new File(getApplicationContext().getFilesDir(),superIdMarkersTrackingConfigFileName);
            FileUtils.writeStringToFile(trackingConfigFile,trackingFileContents, UTF_8);
            ObjectMetadata myObjectMetadata = new ObjectMetadata();
            //create a map to store user metadata
            Map<String, String> userMetadata = new HashMap<String,String>();
            userMetadata.put("TimeStamp", Utils.timeNow(clockSetSuccess,sntpTime,sntpTimeReference).toString());
            myObjectMetadata.setUserMetadata(userMetadata);
            TransferObserver observer = Utils.storeRemoteFile(transferUtility, (trackingRemotePath + superIdMarkersTrackingConfigFileName), Constants.BUCKET_NAME, trackingConfigFile, myObjectMetadata);
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
                }

                @Override
                public void onError(int id, Exception ex) {
                    Log.e(TAG, "saveSuperIdMarkersTrackingConfig(): trackingConfigFile saving failed, see stack trace"+ ex.toString());
                }
            });



        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "Total ID Tracking Markers Data file saving to Dropbox failed, see stack trace");
        }

    }

    public void vpLocationPhotoRequester(final short vpNumber)
    {
        MetaioDebug.log("vpLocationPhotoRequester(): called");
        vpChecked[(vpNumber-1)] = true;
        inFocus = false;
        final Camera camera = IMetaioSDKAndroid.getCamera(this);
        final Camera.Parameters params = camera.getParameters();
        if (vpFlashTorchIsOn[(vpNumber-1)]) params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        params.setSceneMode(Camera.Parameters.SCENE_MODE_STEADYPHOTO);
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);
        Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback()
        {
            @Override
            public void onAutoFocus(boolean success, Camera camera)
            {
                MetaioDebug.log("autofocuscallback called");
                inFocus = true;
            }
        };
        camera.autoFocus(autoFocusCallback);
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                targetImageView.setVisibility(View.VISIBLE);
                targetImageView.setOnTouchListener(new OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event)
                    {
                        if (event.getAction() == MotionEvent.ACTION_DOWN)
                        {
                            if (inFocus)
                            {
                                MetaioDebug.log("inFocus ="+inFocus);
                                metaioSDK.requestCameraImage();
                                // a toast message to alert the user
                                String message = getString(R.string.vp_name) + vpNumber + " " + getString(R.string.camera_image_captured);
                                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                setVpsChecked();
                                if (camera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_TORCH))
                                {
                                    params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                                    camera.setParameters(params);
                                }
                            }
                        }
                        return false;
                    }
                });
            }
        });
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
        //MetaioDebug.log("Adapter: "+adapter);
        //MetaioDebug.log("View: "+view);
        MetaioDebug.log("Position: "+position);
        MetaioDebug.log("Row id: "+id);
        vpLocationDescImageFileContents = null;
        vpIndex = (short) (position+1);
        if (position > (qtyVps-1))
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    listView.setItemChecked(position, false);
                    String message = getString(R.string.vp_name)+vpIndex+" "+getString(R.string.vp_out_of_bounds);
                    Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
            return;
        }
        // Dropbox file path of VP Location Picture Image
        try
        {
            InputStream fis = Utils.getLocalFile("descvp"+(position+1)+".png",getApplicationContext());
            if (!(fis==null)){
                vpLocationDescImageFileContents = BitmapFactory.decodeStream(fis);
                fis.close();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "vpLocationDescImageFileDropbox failed, see stack trace");
        }
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                listView.setItemChecked(position, vpChecked[position]);
                // VP Location Description TextView
                vpLocationDesEditText.setText(vpLocationDesText[position]);
                vpLocationDesEditText.setVisibility(View.VISIBLE);
                vpLocationDesEditText.setOnEditorActionListener(new OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE)
                        {
                            vpLocationDesText[position] = vpLocationDesEditText.getText().toString();
                        }
                        return false;
                    }

                });
                // VP Location # TextView
                String vpId = Integer.toString(position+1);
                vpId = getString(R.string.vp_name)+vpId;
                vpIdNumber.setText(vpId);
                vpIdNumber.setVisibility(View.VISIBLE);
                // VP Acquired
                if (vpAcquired[vpIndex-1]) vpAcquiredStatus.setText(R.string.vpAcquiredStatus);
                if (!vpAcquired[vpIndex-1]) vpAcquiredStatus.setText(R.string.vpNotAcquiredStatus);
                vpAcquiredStatus.setVisibility(View.VISIBLE);
                // VP Location Picture ImageView
                if (!(vpLocationDescImageFileContents==null))
                {
                    imageView.setImageBitmap(vpLocationDescImageFileContents);
                    imageView.setVisibility(View.VISIBLE);
                }
                if (targetImageView.isShown()) targetImageView.setVisibility(View.GONE);
                // Dismiss Location Description Button and other buttons
                okButton.setVisibility(View.VISIBLE);
                requestPhotoButton.setVisibility(View.VISIBLE);
                increaseQtyVps.setVisibility(View.VISIBLE);
                decreaseQtyVps.setVisibility(View.VISIBLE);
                saveTrkVpsData.setVisibility(View.VISIBLE);
                ambiguousVpToggle.setVisibility(View.VISIBLE);
                if (vpIsAmbiguous[vpIndex-1]) ambiguousVpToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                if (!vpIsAmbiguous[vpIndex-1]) ambiguousVpToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                flashTorchVpToggle.setVisibility(View.VISIBLE);
                if (vpFlashTorchIsOn[vpIndex-1]) flashTorchVpToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                if (!vpFlashTorchIsOn[vpIndex-1]) flashTorchVpToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                superSingleVpToggle.setVisibility(View.VISIBLE);
                if (vpIsSuperSingle[vpIndex-1]) superSingleVpToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                if (!vpIsSuperSingle[vpIndex-1]) superSingleVpToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                superVpIdIs20mmToggle.setVisibility(View.VISIBLE);
                if (vpSuperIdIs20mm[vpIndex-1]) superVpIdIs20mmToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                if (!vpSuperIdIs20mm[vpIndex-1]) superVpIdIs20mmToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                superVpIdIs100mmToggle.setVisibility(View.VISIBLE);
                if (vpSuperIdIs100mm[vpIndex-1]) superVpIdIs100mmToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                if (!vpSuperIdIs100mm[vpIndex-1]) superVpIdIs100mmToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
            }
        });

    }

    public void onButtonClick(View v)
    {
        if (v.getId() == R.id.button1)
        {
            MetaioDebug.log("Closing ARViewactivity");
            if (back_pressed + 2000 > System.currentTimeMillis())
            {
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
            vpLocationDesEditText.setVisibility(View.GONE);
            vpIdNumber.setVisibility(View.GONE);
            vpAcquiredStatus.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            okButton.setVisibility(View.GONE);
            requestPhotoButton.setVisibility(View.GONE);
            increaseQtyVps.setVisibility(View.GONE);
            decreaseQtyVps.setVisibility(View.GONE);
            saveTrkVpsData.setVisibility(View.GONE);
            ambiguousVpToggle.setVisibility(View.GONE);
            flashTorchVpToggle.setVisibility(View.GONE);
            superSingleVpToggle.setVisibility(View.GONE);
            superVpIdIs20mmToggle.setVisibility(View.GONE);
            superVpIdIs100mmToggle.setVisibility(View.GONE);
            if (targetImageView.isShown()) targetImageView.setVisibility(View.GONE);
        }
        if (v.getId() == R.id.button3)
        {
            MetaioDebug.log("Show Program Version");
            runOnUiThread(new Runnable()
            {

                @Override
                public void run()
                {
                    String message = getString(R.string.app_version_smc);
                    Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        }
        if (v.getId() == R.id.buttonRequestPhoto)
        {
            MetaioDebug.log("Requesting VP"+vpIndex+" location photo");
            vpLocationDesEditText.setVisibility(View.GONE);
            //vpIdNumber.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            okButton.setVisibility(View.GONE);
            requestPhotoButton.setVisibility(View.GONE);
            increaseQtyVps.setVisibility(View.GONE);
            decreaseQtyVps.setVisibility(View.GONE);
            saveTrkVpsData.setVisibility(View.GONE);
            ambiguousVpToggle.setVisibility(View.GONE);
            flashTorchVpToggle.setVisibility(View.GONE);
            superSingleVpToggle.setVisibility(View.GONE);
            superVpIdIs20mmToggle.setVisibility(View.GONE);
            superVpIdIs100mmToggle.setVisibility(View.GONE);
            cameraPhotoRequested = true;
            doCheckPositionToTarget=false;
            vpSuperMarkerIdFound = false;
            waitingForVpSuperMarkerId = false;
            if (vpAcquired[vpIndex-1])
            {
                vpAcquired[vpIndex-1]=false;
                vpAcquiredStatus.setText(R.string.vpNotAcquiredStatus);
            }
            vpLocationPhotoRequester(vpIndex);
        }
        if (v.getId() == R.id.buttonIncreaseQtyVps)
        {
            MetaioDebug.log("Increase Qty Vps");
            qtyVps++;
            if (qtyVps>maxQtyVps) qtyVps = maxQtyVps;
            increaseArraysLength(qtyVps);
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    String message = getString(R.string.button_text_increase_qtyvps)+" Old="+(qtyVps-1)+" New="+qtyVps+"";
                    Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        }
        if (v.getId() == R.id.buttonDecreaseQtyVps)
        {
            MetaioDebug.log("Decrease Qty Vps");
            qtyVps--;
            if (qtyVps<1) qtyVps = 1;
            decreaseArraysLength(qtyVps);
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    String message = getString(R.string.button_text_decrease_qtyvps)+" Old="+(qtyVps+1)+" New="+qtyVps+"";
                    Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
        }
        if (v.getId() == R.id.buttonSaveTrackingVpsData)
        {
            MetaioDebug.log("Saving Tracking & Vps Data");
            if (targetImageView.isShown()) targetImageView.setVisibility(View.GONE);
            saveTrackingConfig();
            saveVpsData();
        }
        if (v.getId() == R.id.buttonAmbiguousVpToggle)
        {
            MetaioDebug.log("Toggle Ambiguous VP");
            if (vpIsAmbiguous[vpIndex-1])
            {
                vpIsAmbiguous[vpIndex-1] = false;
            }
            else
            {
                vpIsAmbiguous[vpIndex-1] = true;
            }
            if (vpIsAmbiguous[vpIndex-1]) ambiguousVpToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
            if (!vpIsAmbiguous[vpIndex-1])
            {
                if (!vpIsSuperSingle[vpIndex-1])
                {
                    ambiguousVpToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                }
                else
                {
                    vpIsAmbiguous[vpIndex-1] = true;
                    ambiguousVpToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                }
            }
        }
        if (v.getId() == R.id.buttonFlashTorchVpToggle)
        {
            MetaioDebug.log("Toggle Flash Torch VP");
            if (vpFlashTorchIsOn[vpIndex-1])
            {
                vpFlashTorchIsOn[vpIndex-1] = false;
            }
            else
            {
                vpFlashTorchIsOn[vpIndex-1] = true;
            }
            if (vpFlashTorchIsOn[vpIndex-1]) flashTorchVpToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
            if (!vpFlashTorchIsOn[vpIndex-1]) flashTorchVpToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
        }
        if (v.getId() == R.id.buttonSuperSingleVpToggle)
        {
            MetaioDebug.log("Toggle Super Single VP");
            if (vpIsSuperSingle[vpIndex-1])
            {
                vpIsSuperSingle[vpIndex-1] = false;
            }
            else
            {
                vpIsSuperSingle[vpIndex-1] = true;
            }
            if (vpIsSuperSingle[vpIndex-1])
            {
                superSingleVpToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                if (!vpIsAmbiguous[vpIndex-1])
                {
                    vpIsAmbiguous[vpIndex-1] = true;
                    ambiguousVpToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                }
                if ((!vpSuperIdIs20mm[vpIndex-1])&&(!vpSuperIdIs100mm[vpIndex-1]))
                {
                    vpSuperIdIs100mm[vpIndex-1]=true;
                    superVpIdIs100mmToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                    vpMarkerlessMarkerWidth[vpIndex-1] = seaMensorMarkerWidthWhenIdIs100mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = seaMensorMarkerHeigthWhenIdIs100mm;
                }
            }
            if (!vpIsSuperSingle[vpIndex-1])
            {
                superSingleVpToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                if ((vpSuperIdIs20mm[vpIndex-1])||(vpSuperIdIs100mm[vpIndex-1]))
                {
                    vpSuperIdIs20mm[vpIndex-1]=false;
                    vpSuperIdIs100mm[vpIndex-1]=false;
                    superVpIdIs20mmToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                    superVpIdIs100mmToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                    vpMarkerlessMarkerWidth[vpIndex-1] = standardMarkerlessMarkerWidth;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = standardMarkerlessMarkerHeigth;
                }

            }
        }
        if (v.getId() == R.id.buttonId20mmToggle)
        {
            MetaioDebug.log("Toggle vpSuperIdIs20mm");
            if (vpSuperIdIs20mm[vpIndex-1])
            {
                if (vpIsSuperSingle[vpIndex-1])
                {
                    vpSuperIdIs20mm[vpIndex-1] = false;
                    vpSuperIdIs100mm[vpIndex-1] = true;
                    superVpIdIs100mmToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                    vpMarkerlessMarkerWidth[vpIndex-1] = seaMensorMarkerWidthWhenIdIs100mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = seaMensorMarkerHeigthWhenIdIs100mm;
                }
            }
            else
            {
                if (vpIsSuperSingle[vpIndex-1])
                {
                    vpSuperIdIs20mm[vpIndex-1] = true;
                    vpMarkerlessMarkerWidth[vpIndex-1] = seaMensorMarkerWidthWhenIdIs20mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = seaMensorMarkerHeigthWhenIdIs20mm;
                }
            }
            if (vpSuperIdIs20mm[vpIndex-1])
            {
                superVpIdIs20mmToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                if (vpSuperIdIs100mm[vpIndex-1])
                {
                    vpSuperIdIs100mm[vpIndex-1] = false;
                    superVpIdIs100mmToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                }
            }
            if (!vpSuperIdIs20mm[vpIndex-1]) superVpIdIs20mmToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
        }
        if (v.getId() == R.id.buttonId100mmToggle)
        {
            MetaioDebug.log("Toggle vpSuperIdIs100mm");
            if (vpSuperIdIs100mm[vpIndex-1])
            {
                if (vpIsSuperSingle[vpIndex-1])
                {
                    vpSuperIdIs100mm[vpIndex-1] = false;
                    vpSuperIdIs20mm[vpIndex-1] = true;
                    superVpIdIs20mmToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                    vpMarkerlessMarkerWidth[vpIndex-1] = seaMensorMarkerWidthWhenIdIs20mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = seaMensorMarkerHeigthWhenIdIs20mm;
                }
            }
            else
            {
                if (vpIsSuperSingle[vpIndex-1])
                {
                    vpSuperIdIs100mm[vpIndex-1] = true;
                    vpMarkerlessMarkerWidth[vpIndex-1] = seaMensorMarkerWidthWhenIdIs100mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = seaMensorMarkerHeigthWhenIdIs100mm;
                }
            }
            if (vpSuperIdIs100mm[vpIndex-1])
            {
                superVpIdIs100mmToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                if (vpSuperIdIs20mm[vpIndex-1])
                {
                    vpSuperIdIs20mm[vpIndex-1] = false;
                    superVpIdIs20mmToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
                }
            }
            if (!vpSuperIdIs100mm[vpIndex-1]) superVpIdIs100mmToggle.setBackgroundResource(R.drawable.custom_button_option_notselected);
        }

    }


    public void loadConfigurationFile()
    {
        vpLocationDesText = new String[qtyVps];
        vpXCameraDistance = new int[qtyVps];
        vpYCameraDistance = new int[qtyVps];
        vpZCameraDistance = new int[qtyVps];
        vpXCameraRotation = new int[qtyVps];
        vpYCameraRotation = new int[qtyVps];
        vpZCameraRotation = new int[qtyVps];
        vpConfiguredTimeSMCMillis = new long[qtyVps];
        vpNumber = new short[qtyVps];
        vpFrequencyUnit = new String[qtyVps];
        vpFrequencyValue = new long[qtyVps];
        vpChecked = new boolean[qtyVps];
        vpMarkerlessMarkerWidth = new short[qtyVps];
        vpMarkerlessMarkerHeigth = new short[qtyVps];
        vpIsAmbiguous = new boolean[qtyVps];
        vpFlashTorchIsOn = new boolean[qtyVps];
        vpIsSuperSingle = new boolean[qtyVps];
        vpSuperIdIs20mm = new boolean[qtyVps];
        vpSuperIdIs100mm = new boolean[qtyVps];
        vpSuperMarkerId = new int[qtyVps];
        vpAcquired = new boolean[qtyVps];
        vpAcquiredTimeSMCMillis = new long[qtyVps];

        MetaioDebug.log("loadConfigurationFile() started");

        for (int i=0; i<qtyVps; i++)
        {
            vpFrequencyUnit[i] = "";
            vpFrequencyValue[i] = 0;
            vpConfiguredTimeSMCMillis[i] = 0;
            vpMarkerlessMarkerWidth[i] = standardMarkerlessMarkerWidth;
            vpMarkerlessMarkerHeigth[i] = standardMarkerlessMarkerHeigth;
            vpIsAmbiguous[i] = false;
            vpFlashTorchIsOn[i] = false;
            vpIsSuperSingle[i] = false;
            vpSuperIdIs20mm[i] = false;
            vpSuperIdIs100mm[i] = false;
            vpSuperMarkerId[i] = 0;
        }

        // Load Initialization Values from file
        try
        {
            // Getting a file path for vps configuration XML file
            Log.d(TAG,"Vps Config Dropbox path = "+vpsConfigFileDropbox);
            File vpsFile = new File(getApplicationContext().getFilesDir(),vpsConfigFileDropbox);
            InputStream fis = Utils.getLocalFile(vpsConfigFileDropbox, getApplicationContext());
            try
            {
                XmlPullParserFactory xmlFactoryObject = XmlPullParserFactory.newInstance();
                XmlPullParser myparser = xmlFactoryObject.newPullParser();
                myparser.setInput(fis, null);
                int eventType = myparser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT)
                {
                    if(eventType == XmlPullParser.START_DOCUMENT)
                    {
                        MetaioDebug.log("Start document");
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
                            MetaioDebug.log("VpListOrder: "+vpListOrder);
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpNumber"))
                        {
                            eventType = myparser.next();
                            vpNumber[vpListOrder-1] = Short.parseShort(myparser.getText());
                            MetaioDebug.log("VpNumber"+(vpListOrder-1)+": "+vpNumber[vpListOrder-1]);
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpXCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpXCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
                            MetaioDebug.log("vpXCameraDistance["+(vpListOrder-1)+"]"+vpXCameraDistance[vpListOrder-1]);
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpYCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpYCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
                            MetaioDebug.log("vpYCameraDistance["+(vpListOrder-1)+"]"+vpYCameraDistance[vpListOrder-1]);
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpZCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpZCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
                            MetaioDebug.log("vpZCameraDistance["+(vpListOrder-1)+"]"+vpZCameraDistance[vpListOrder-1]);
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpXCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpXCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
                            MetaioDebug.log("vpXCameraRotation["+(vpListOrder-1)+"]"+vpXCameraRotation[vpListOrder-1]);
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpYCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpYCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
                            MetaioDebug.log("vpYCameraRotation["+(vpListOrder-1)+"]"+vpYCameraRotation[vpListOrder-1]);
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpZCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpZCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
                            MetaioDebug.log("vpZCameraRotation["+(vpListOrder-1)+"]"+vpZCameraRotation[vpListOrder-1]);
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
                        MetaioDebug.log("End tag "+myparser.getName());
                    }
                    else if(eventType == XmlPullParser.TEXT)
                    {
                        MetaioDebug.log("Text "+myparser.getText());
                    }
                    eventType = myparser.next();
                }
                fis.close();
            }
            finally
            {
                MetaioDebug.log("Vps Config DROPBOX file = "+vpsFile);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "Vps data loading failed, see stack trace");
            MetaioDebug.log("Vps Config DROPBOX file loading FAILED:"+e.getStackTrace());
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(getBaseContext(), "Vps Config DROPBOX file loading FAILED", Toast.LENGTH_LONG).show();
                }
            });
        }

        for (int i=0; i<qtyVps; i++)
        {
            vpChecked[i] = false;
            vpAcquired[i] = true;
            if (vpFrequencyUnit[i]=="")
            {
                vpFrequencyUnit[i]=frequencyUnit;
                MetaioDebug.log("vpFrequencyUnit["+i+"]="+vpFrequencyUnit[i]);
            }

            if (vpFrequencyValue[i]==0)
            {
                vpFrequencyValue[i]=frequencyValue;
                MetaioDebug.log("vpFrequencyValue["+i+"]="+vpFrequencyValue[i]);
            }

        }

    }


    public void decreaseArraysLength (int newlength)
    {
        vpChecked = Arrays.copyOf(vpChecked, newlength);
        vpAcquired = Arrays.copyOf(vpAcquired, newlength);
        vpConfiguredTimeSMCMillis =Arrays.copyOf(vpConfiguredTimeSMCMillis, newlength);
        vpAcquiredTimeSMCMillis =Arrays.copyOf(vpAcquiredTimeSMCMillis, newlength);
        vpLocationDesText = Arrays.copyOf(vpLocationDesText, newlength);
        vpMarkerlessMarkerWidth = Arrays.copyOf(vpMarkerlessMarkerWidth, newlength);
        vpMarkerlessMarkerHeigth = Arrays.copyOf(vpMarkerlessMarkerHeigth, newlength);
        vpIsAmbiguous = Arrays.copyOf(vpIsAmbiguous, newlength);
        vpFlashTorchIsOn = Arrays.copyOf(vpFlashTorchIsOn, newlength);
        vpIsSuperSingle = Arrays.copyOf(vpIsSuperSingle, newlength);
        vpSuperIdIs20mm = Arrays.copyOf(vpSuperIdIs20mm, newlength);
        vpSuperIdIs100mm = Arrays.copyOf(vpSuperIdIs100mm, newlength);
        vpSuperMarkerId = Arrays.copyOf(vpSuperMarkerId, newlength);
        vpXCameraDistance = Arrays.copyOf(vpXCameraDistance, newlength);
        vpYCameraDistance = Arrays.copyOf(vpYCameraDistance, newlength);
        vpZCameraDistance = Arrays.copyOf(vpZCameraDistance, newlength);
        vpXCameraRotation = Arrays.copyOf(vpXCameraRotation, newlength);
        vpYCameraRotation = Arrays.copyOf(vpYCameraRotation, newlength);
        vpZCameraRotation = Arrays.copyOf(vpZCameraRotation, newlength);
        vpFrequencyUnit = Arrays.copyOf(vpFrequencyUnit, newlength);
        vpFrequencyValue = Arrays.copyOf(vpFrequencyValue, newlength);
    }


    public void increaseArraysLength (int newlength)
    {
        vpChecked = Arrays.copyOf(vpChecked, newlength);
        vpAcquired = Arrays.copyOf(vpAcquired, newlength);
        vpConfiguredTimeSMCMillis =Arrays.copyOf(vpConfiguredTimeSMCMillis, newlength);
        vpAcquiredTimeSMCMillis =Arrays.copyOf(vpAcquiredTimeSMCMillis, newlength);
        vpLocationDesText = Arrays.copyOf(vpLocationDesText, newlength);
        vpMarkerlessMarkerWidth = Arrays.copyOf(vpMarkerlessMarkerWidth, newlength);
        vpMarkerlessMarkerHeigth = Arrays.copyOf(vpMarkerlessMarkerHeigth, newlength);
        vpIsAmbiguous = Arrays.copyOf(vpIsAmbiguous, newlength);
        vpFlashTorchIsOn = Arrays.copyOf(vpFlashTorchIsOn, newlength);
        vpIsSuperSingle = Arrays.copyOf(vpIsSuperSingle, newlength);
        vpSuperIdIs20mm = Arrays.copyOf(vpSuperIdIs20mm, newlength);
        vpSuperIdIs100mm = Arrays.copyOf(vpSuperIdIs100mm, newlength);
        vpSuperMarkerId = Arrays.copyOf(vpSuperMarkerId, newlength);
        vpXCameraDistance = Arrays.copyOf(vpXCameraDistance, newlength);
        vpYCameraDistance = Arrays.copyOf(vpYCameraDistance, newlength);
        vpZCameraDistance = Arrays.copyOf(vpZCameraDistance, newlength);
        vpXCameraRotation = Arrays.copyOf(vpXCameraRotation, newlength);
        vpYCameraRotation = Arrays.copyOf(vpYCameraRotation, newlength);
        vpZCameraRotation = Arrays.copyOf(vpZCameraRotation, newlength);
        vpFrequencyUnit = Arrays.copyOf(vpFrequencyUnit, newlength);
        vpFrequencyValue = Arrays.copyOf(vpFrequencyValue, newlength);
        vpFrequencyUnit[newlength-1] = frequencyUnit;
        vpFrequencyValue[newlength-1] = frequencyValue;
        vpMarkerlessMarkerWidth[newlength-1] = standardMarkerlessMarkerWidth;
        vpMarkerlessMarkerHeigth[newlength-1] = standardMarkerlessMarkerHeigth;
    }


    @Override
    protected void loadContents()
    {
        try
        {
            // Running AssetsManager.extractAllAssets method from Metaio SDK to establish the local app files path
            AssetsManager.extractAllAssets(getApplicationContext(), true);
            // Load 3d Geometry.
            String seaMensorCubeModel = AssetsManager.getAssetPath(getApplicationContext(), geometry3dConfigFile);
            String vpCheckedModel = AssetsManager.getAssetPath(getApplicationContext(), geometrySecondary3dConfigFile);
            MetaioDebug.log("Models : "+ AssetsManager.getAbsolutePath());
            MetaioDebug.log("Model loaded: "+getApplicationContext()+"  " + seaMensorCubeModel);
            // Continuing to Load 3d Geometry.
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
                    com.metaio.sdk.jni.Rotation rot = mSeaMensorCube.getRotation();
                    rot.setFromEulerAngleDegrees(new Vector3d(0f, 0f, 0f));
                    mSeaMensorCube.setRotation(rot, false);
                    MetaioDebug.log("Loaded geometry (name):"+mSeaMensorCube);
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
                    rot.setFromEulerAngleDegrees(new Vector3d(0f, 0f, 0f));
                    mVpChecked.setRotation(rot, false);
                    MetaioDebug.log("Loaded geometry (name):"+mVpChecked);
                    mVpChecked.setVisible(false);
                }
                else
                    MetaioDebug.log(Log.ERROR, "Error loading geometry: "+mVpChecked);
            }
            // defining the localFilePath to be used for all assets downloaded from DROPBOX
            String localFilePath = AssetsManager.getAbsolutePath()+"/";
            globalLocalFilePath = localFilePath;
            String trackingConfigFileContents = "";
            try
            {
                InputStream fis = Utils.getLocalFile(trackingConfigFileName,getApplicationContext());
                trackingConfigFileContents = IOUtils.toString(fis, UTF_8);
                fis.close();
            }
            catch (Exception e)
            {
                Log.e(TAG,"Error when loading markerlesstrackingConfigFileContents:"+e.toString());
            }
            trackingConfigFileContents = trackingConfigFileContents.replace("markervp",localFilePath+"markervp");
            trackingConfigFileContents = trackingConfigFileContents.replace(seaMensorMarker,localFilePath+seaMensorMarker);

            try
            {
                InputStream fis = Utils.getLocalFile(superIdMarkersTrackingConfigFileName,getApplicationContext());
                superIdMarkersTrackingConfigFileContents = IOUtils.toString(fis, UTF_8);
                fis.close();
            }
            catch (Exception e)
            {
                Log.e(TAG,"Error when loading markerlesstrackingConfigFileContents:"+e.toString());
            }



            //Reading Markers from Dropbox and Writing to Local assets path
            /*
            for (int i=0; i<qtyVps; i++)
            {
                try
                {
                    DbxPath vpMarkerImageFilePath = new DbxPath(DbxPath.ROOT, markervpRemotePath +(i+1)+".jpg");
                    MetaioDebug.log("vpMarkerImageFilePath: "+"markervp"+(i+1)+".jpg");
                    DbxFile vpMarkerImageFileDropbox = dbxFs.open(vpMarkerImageFilePath);
                    vpMarkerImageFileContents = BitmapFactory.decodeStream(vpMarkerImageFileDropbox.getReadStream());
                    vpMarkerImageFileDropbox.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try
                {
                    File markerFile = new File(getFilesDir(),"markervp"+(i+1)+".jpg");
                    FileOutputStream fos = new FileOutputStream(markerFile);
                    vpMarkerImageFileContents.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.close();
                } catch (FileNotFoundException e) {
                    MetaioDebug.log("File not found: " + e.getMessage());
                } catch (IOException e) {
                    MetaioDebug.log("Error accessing file: " + e.getMessage());
                }
            }
            */
            // Assigning tracking configuration
            boolean result = metaioSDK.setTrackingConfiguration(trackingConfigFileContents,false);
            MetaioDebug.log("Tracking data loaded: " + result);
            MetaioDebug.log("# of Def COS: " +metaioSDK.getNumberOfDefinedCoordinateSystems());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            MetaioDebug.log(Log.ERROR, "loadContents failed, see stack trace");
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(getBaseContext(), "LoadContents failed, see stack trace", Toast.LENGTH_LONG).show();
                }
            });
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
                        MetaioDebug.log("Touched CoordinateSystemID = "+((poses.get(im).getCoordinateSystemID())));
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                String message = getString(R.string.vp_touched)+((poses.get(im).getCoordinateSystemID()))+"\n"+getString(R.string.vp_touched_info);
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
                String cameraCalibration = 	"<?xml version=\"1.0\"?>"+
                        "<Camera>"+
                        "<Name>LGG2 camera 1280x720</Name>"+
                        "<Info>Calibration results generated with 3DF Lapyx v. 1.0</Info>"+
                        "<CalibrationResolution>"+
                        "<X>1280</X>"+
                        "<Y>720</Y>"+
                        "</CalibrationResolution>"+
                        "<FocalLength>"+
                        "<X>1054.42</X>"+
                        "<Y>1014.44</Y>"+
                        "</FocalLength>"+
                        "<PrincipalPoint>"+
                        "<X>640.735</X>"+
                        "<Y>334.981</Y>"+
                        "</PrincipalPoint>"+
                        "<Distortion>"+
                        "<K1>0.105051</K1>"+
                        "<K2>-0.167575</K2>"+
                        "<P1>-0.00409267</P1>"+
                        "<P2>-0.0021432</P2>"+
                        "</Distortion>"+
                        "</Camera>";
                boolean result = metaioSDK.setCameraParameters(cameraCalibration);
                MetaioDebug.log("Camera Calibration data loaded: " + result);
            }
            catch (Exception e)
            {
                MetaioDebug.log(Log.ERROR, "Load camera calibration failed, see stack trace");
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(getBaseContext(), "Load camera calibration failed, see stack trace", Toast.LENGTH_LONG).show();
                    }
                });
            }

        }

        @Override
        public void onAnimationEnd(IGeometry geometry, String animationName)
        {
            MetaioDebug.log("animation ended" + animationName);
        }

        // Callback that receives imagestruct from requestcameraimage
        @Override
        public void onNewCameraFrame(ImageStruct cameraFrame)
        {
            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
            // bitmap to descvp and markervp
            Bitmap bitmapImage = null;
            Bitmap markerFromBitmapImage = null;
            // creating temp local storage files
            File pictureFile = new File(getApplicationContext().getFilesDir(), "descvp"+vpIndex+".png");
            File markerFile = new File(getApplicationContext().getFilesDir(), "markervp"+vpIndex+".jpg");
            MetaioDebug.log("SDK callback: onNewCameraFrame: a new camera frame image is delivered " + cameraFrame.getTimestamp());
            if(cameraFrame != null)
            {
                // getting bitmap from cameraframe
                MetaioDebug.log("SDK callback: onNewCameraFrame: cameraFrame != null");
                bitmapImage = cameraFrame.getBitmap();
                final int width = bitmapImage.getWidth();
                final int height = bitmapImage.getHeight();
                int markerWidthLocal = 0;
                int markerHeightLocal = 0;
                if (!vpIsSuperSingle[vpIndex-1])
                {
                    if (vpMarkerlessMarkerWidth[vpIndex-1]>1)
                    {
                        markerWidthLocal = vpMarkerlessMarkerWidth[vpIndex-1];
                    }
                    else
                    {
                        markerWidthLocal = standardMarkerlessMarkerWidth;
                    }
                    if (vpMarkerlessMarkerHeigth[vpIndex-1]>1)
                    {
                        markerHeightLocal = vpMarkerlessMarkerHeigth[vpIndex-1];
                    }
                    else
                    {
                        markerHeightLocal = standardMarkerlessMarkerHeigth;
                    }
                }
                else
                {
                    markerWidthLocal = standardMarkerlessMarkerWidth;
                    markerHeightLocal = standardMarkerlessMarkerHeigth;
                }
                if (vpMarkerlessMarkerWidth[vpIndex-1]>width) markerWidthLocal=width;
                if (vpMarkerlessMarkerHeigth[vpIndex-1]>height) markerHeightLocal=height;
                int x = (width - markerWidthLocal)/2;
                int y = (height - markerHeightLocal)/2;
                // getting marker from bitmap, centering the marker in the original bitmap
                markerFromBitmapImage = Bitmap.createBitmap(bitmapImage, x, y, markerWidthLocal, markerHeightLocal);
                //markerFromBitmapImage = greyScaler(markerFromBitmapImage);
                markerFromBitmapImage = markerFromBitmapImage.createScaledBitmap(markerFromBitmapImage, captureMarkerWidth, captureMarkerHeight, false);
                MetaioDebug.log("Camera frame width: "+width+" height: "+height);
            }
            if (pictureFile == null)
            {
                MetaioDebug.log("Error creating PICTURE media file, check storage permissions. ");
                return;
            }
            if (markerFile == null)
            {
                MetaioDebug.log("Error creating MARKER media file, check storage permissions. ");
                return;
            }
            try
            {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                bitmapImage.compress(Bitmap.CompressFormat.PNG, 95, fos);
                fos.close();
            }
            catch (FileNotFoundException e)
            {
                MetaioDebug.log("File not found: " + e.getMessage());
            }
            catch (IOException e)
            {
                MetaioDebug.log("Error accessing file: " + e.getMessage());
            }
            try
            {
                FileOutputStream fos = new FileOutputStream(markerFile);
                markerFromBitmapImage.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                fos.close();
            }
            catch (FileNotFoundException e)
            {
                MetaioDebug.log("File not found: " + e.getMessage());
            }
            catch (IOException e)
            {
                MetaioDebug.log("Error accessing file: " + e.getMessage());
            }
            // if bitmapimage is OK it is saved to dropbox
            if (bitmapImage != null)
            {
                try
                {
                    ObjectMetadata myObjectMetadata = new ObjectMetadata();
                    //create a map to store user metadata
                    Map<String, String> userMetadata = new HashMap<String,String>();
                    userMetadata.put("VP", ""+(vpIndex));
                    userMetadata.put("seamensorAccount", seamensorAccount);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String formattedDateTime = sdf.format(Utils.timeNow(clockSetSuccess,sntpTime,sntpTimeReference));
                    userMetadata.put("DateTime", formattedDateTime);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    myObjectMetadata.setUserMetadata(userMetadata);
                    //uploading the objects
                    TransferObserver observer = Utils.storeRemoteFile(
                            transferUtility,
                            descvpRemotePath+pictureFile.getName(),
                            Constants.BUCKET_NAME,
                            pictureFile,
                            myObjectMetadata);
                    Log.d(TAG, "AWS s3 Observer: "+observer.getState().toString());
                    Log.d(TAG, "AWS s3 Observer: "+observer.getAbsoluteFilePath());
                    Log.d(TAG, "AWS s3 Observer: "+observer.getBucket());
                    Log.d(TAG, "AWS s3 Observer: "+observer.getKey());
                }
                catch (Exception e)
                {
                    vpChecked[(vpIndex-1)] = false;
                    e.printStackTrace();
                }
            }
            // if marker from bitmap image is ok and not a super VP then save it to dropbox
            if (markerFromBitmapImage != null)
            {
                // Set the tracking configuration using the markerFile currently in the app data folder
                cameraPhotoRequested =false;
                vpDescAndMarkerImageOK = true;
                if (vpIsSuperSingle[vpIndex-1])
                {
                    //MetaioDebug.log("OnNewCameraFrame: calling setSLAMTrackingConfig()");
                    //setSLAMTrackingConfig();
                    MetaioDebug.log("SDK callback: onNewCameraFrame: calling setSuperSingleIdMarkerTrackingConfig()");
                    waitingForVpSuperMarkerId=true;
                    setSuperIdMarkersTrackingConfig();
                    //setSuperSingleIdMarkerTrackingConfig();
                }
                else
                {
                    MetaioDebug.log("SDK callback: onNewCameraFrame: calling setTrackingConfig()");
                    setTrackingConfig();
                }
                try
                {
                    ObjectMetadata myObjectMetadata = new ObjectMetadata();
                    //create a map to store user metadata
                    Map<String, String> userMetadata = new HashMap<String,String>();
                    userMetadata.put("VP", ""+(vpIndex));
                    userMetadata.put("seamensorAccount", seamensorAccount);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String formattedDateTime = sdf.format(Utils.timeNow(clockSetSuccess,sntpTime,sntpTimeReference));
                    userMetadata.put("DateTime", formattedDateTime);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    myObjectMetadata.setUserMetadata(userMetadata);
                    //uploading the objects
                    TransferObserver observer = Utils.storeRemoteFile(
                            transferUtility,
                            markervpRemotePath+pictureFile.getName(),
                            Constants.BUCKET_NAME,
                            pictureFile,
                            myObjectMetadata);
                    Log.d(TAG, "AWS s3 Observer: "+observer.getState().toString());
                    Log.d(TAG, "AWS s3 Observer: "+observer.getAbsoluteFilePath());
                    Log.d(TAG, "AWS s3 Observer: "+observer.getBucket());
                    Log.d(TAG, "AWS s3 Observer: "+observer.getKey());
                }
                catch (Exception e)
                {
                    vpChecked[(vpIndex-1)] = false;
                    e.printStackTrace();
                }
            }

            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (targetImageView.isShown()) targetImageView.setVisibility(View.GONE);
                }
            });

        }

        @Override
        public void onScreenshotImage(ImageStruct image)
        {
            MetaioDebug.log("screenshot image is received" + image.getTimestamp());
        }

        @Override
        public void onTrackingEvent(TrackingValuesVector trackingValues)
        {
            if (trackingValues.size()>0)
            {
                final TrackingValues v = trackingValues.get(0);
                String sensorType = v.getSensor();
                String cosName = v.getCosName();
                MetaioDebug.log("oTE: BLOCK 0:Tracking state for COS "+v.getCoordinateSystemID()+" is "+v.getState());
                MetaioDebug.log("oTE: BLOCK 0:Tracking state for COS "+v.getCoordinateSystemID()+" is "+sensorType);
                MetaioDebug.log("oTE: BLOCK 0:Tracking state for COS "+v.getCoordinateSystemID()+" is "+cosName);

                if ((sensorType.equalsIgnoreCase("MarkerBasedSensorSource"))&&(v.getState()== ETRACKING_STATE.ETS_FOUND)&&(waitingForVpSuperMarkerId))
                {
                    superCoordinateSystemTrackedInPoseI = Integer.parseInt(cosName.replace("COS",""));
                    MetaioDebug.log("oTE: BLOCK 1:*** superCoordinateSystemTrackedInPoseI:"+superCoordinateSystemTrackedInPoseI);
                    vpSuperMarkerId[vpIndex-1]=superCoordinateSystemTrackedInPoseI;
                    waitingForVpSuperMarkerId = false;
                    vpSuperMarkerIdFound = true;
                    //setSLAMTrackingConfig();
                    setSuperSingleIdMarkerTrackingConfig();
                    MetaioDebug.log("oTE: BLOCK 1:*** setSLAMTrackingConfig()");
                }

                if ((v.getState()== ETRACKING_STATE.ETS_INITIALIZED) && (vpDescAndMarkerImageOK))
                {
                    MetaioDebug.log("oTE: BLOCK 2:ETRACKING_STATE.ETS_INITIALIZED && doCheckPositionToTarget=true");
                    doCheckPositionToTarget=true;
                }
            }
        }
    }
}