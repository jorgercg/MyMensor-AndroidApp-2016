package com.mymensor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.mymensor.cognitoclient.AwsUtil;
import com.mymensor.filters.ARFilter;
import com.mymensor.filters.Filter;
import com.mymensor.filters.NoneARFilter;
import com.mymensor.filters.VpConfigFilter;

import org.apache.commons.io.FileUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConfigActivity extends Activity implements
        CameraBridgeViewBase.CvCameraViewListener2,
        AdapterView.OnItemClickListener {

    private static final String TAG = "ConfigActivity";

    private static long back_pressed;

    private String descvpRemotePath;
    private String markervpRemotePath;
    private String vpsRemotePath;
    private String vpsCheckedRemotePath;
    private String capRemotePath;

    private short qtyVps = 0;
    private short vpIndex;

    private String mymensorAccount;
    private int dciNumber;

    private boolean[] vpChecked;
    private boolean[] vpAcquired;

    private short[] vpNumber;
    private String[] vpLocationDesText;
    private int[] vpXCameraDistance;
    private int[] vpYCameraDistance;
    private int[] vpZCameraDistance;
    private int[] vpXCameraRotation;
    private int[] vpYCameraRotation;
    private int[] vpZCameraRotation;
    private short[] vpMarkerlessMarkerWidth;
    private short[] vpMarkerlessMarkerHeigth;
    private boolean[] vpIsAmbiguous;
    private boolean[] vpFlashTorchIsOn;
    private boolean[] vpIsSuperSingle;
    private boolean[] vpSuperIdIs20mm;
    private boolean[] vpSuperIdIs100mm;
    private int[] vpSuperMarkerId;
    private String[] vpFrequencyUnit;
    private long[] vpFrequencyValue;
    private static Bitmap vpLocationDescImageFileContents;

    private static float tolerancePosition;
    private static float toleranceRotation;

    private short shipId;
    private String frequencyUnit;
    private int frequencyValue;

    ListView vpsListView;
    ImageView mProgress;
    ImageView targetImageView;
    TouchImageView imageView;

    EditText vpLocationDesEditTextView;
    TextView vpIdNumber;
    TextView vpAcquiredStatus;

    Animation rotationMProgress;

    Button acceptVpPhotoButton;
    Button rejectVpPhotoButton;
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

    private AmazonS3Client s3Client;
    private TransferUtility transferUtility;

    SharedPreferences sharedPref;

    public long sntpTime;
    public long sntpTimeReference;
    public boolean clockSetSuccess;
    private long acquisitionStartTime;

    // The camera view.
    private CameraBridgeViewBase mCameraView;

    // A matrix that is used when saving photos.
    private Mat mBgr;

    // Whether the next camera frame should be saved as a photo and other boolean controllers
    private boolean cameraPhotoRequested;
    private boolean vpDescAndMarkerImageOK = false;
    private boolean doCheckPositionToTarget;
    private boolean vpSuperMarkerIdFound;
    private boolean waitingForVpSuperMarkerId;
    private boolean waitingForTrackingAcquisition = false;
    private boolean trackingConfigDone = false;

    // The filters.
    private ARFilter[] mVpConfigureFilters;

    // The indices of the active filters.
    private int mVpConfigureFilterIndex;

    // The index of the active camera.
    private int mCameraIndex;

    // Whether the active camera is front-facing.
    // If so, the camera view should be mirrored.
    private boolean mIsCameraFrontFacing;

    // The number of cameras on the device.
    private int mNumCameras;

    // The image sizes supported by the active camera.
    private List<Camera.Size> mSupportedImageSizes;

    // The index of the active image size.
    private int mImageSizeIndex;

    // A key for storing the index of the active camera.
    private static final String STATE_CAMERA_INDEX = "cameraIndex";

    // A key for storing the index of the active image size.
    private static final String STATE_IMAGE_SIZE_INDEX =
            "imageSizeIndex";

    // Keys for storing the indices of the active filters.
    private static final String STATE_IMAGE_DETECTION_FILTER_INDEX =
            "imageDetectionFilterIndex";

    // Whether an asynchronous menu action is in progress.
    // If so, menu interaction should be disabled.
    private boolean mIsMenuLocked;

    // Matrix to hold camera calibration
    // initially with absolute compute values
    private MatOfDouble mCameraMatrix;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = this.getSharedPreferences("com.mymensor.app", Context.MODE_PRIVATE);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_config);

        s3Client = CognitoSyncClientManager.getInstance();

        transferUtility = AwsUtil.getTransferUtility(s3Client, getApplicationContext());

        mymensorAccount = getIntent().getExtras().get("mymensoraccount").toString();
        dciNumber = Integer.parseInt(getIntent().getExtras().get("dcinumber").toString());
        qtyVps = Short.parseShort(getIntent().getExtras().get("QtyVps").toString());
        sntpTime = Long.parseLong(getIntent().getExtras().get("sntpTime").toString());
        sntpTimeReference = Long.parseLong(getIntent().getExtras().get("sntpReference").toString());
        clockSetSuccess = Boolean.parseBoolean(getIntent().getExtras().get("clockSetSuccess").toString());

        descvpRemotePath = mymensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"dsc"+"/";
        markervpRemotePath = mymensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/"+"mrk"+"/";
        vpsRemotePath = mymensorAccount+"/"+"cfg"+"/"+dciNumber+"/"+"vps"+"/";
        vpsCheckedRemotePath = mymensorAccount + "/" + "chk" + "/" + dciNumber + "/";
        capRemotePath = mymensorAccount+"/"+"cap"+"/";

        if (savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(STATE_IMAGE_SIZE_INDEX, 0);
            mVpConfigureFilterIndex = savedInstanceState.getInt(STATE_IMAGE_DETECTION_FILTER_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
            mVpConfigureFilterIndex = 0;
        }

        final Camera camera;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraIndex, cameraInfo);
        mIsCameraFrontFacing =
                (cameraInfo.facing ==
                        Camera.CameraInfo.CAMERA_FACING_FRONT);
        mNumCameras = Camera.getNumberOfCameras();
        camera = Camera.open(mCameraIndex);

        final Camera.Parameters parameters = camera.getParameters();
        camera.release();
        mSupportedImageSizes = parameters.getSupportedPreviewSizes();
        final Camera.Size size = mSupportedImageSizes.get(mImageSizeIndex);

        mCameraView = (CameraBridgeViewBase) findViewById(R.id.config_javaCameraView);
        mCameraView.setCameraIndex(mCameraIndex);
        mCameraView.setMaxFrameSize(Constants.cameraWidthInPixels, Constants.cameraHeigthInPixels);
        mCameraView.setCvCameraViewListener(this);

        Camera.Size tmpCamProjSize = mSupportedImageSizes.get(mImageSizeIndex);;

        tmpCamProjSize.width = Constants.cameraWidthInPixels;
        tmpCamProjSize.height = Constants.cameraHeigthInPixels;

        final Camera.Size camProjSize = tmpCamProjSize;

        loadConfigurationFile();

        mVpConfigureFilterIndex = 1;

        String[] newVpsList = new String[qtyVps];
        for (int i=0; i<qtyVps; i++)
        {
            newVpsList[i] = getString(R.string.vp_name)+vpNumber[i];
        }
        vpsListView = (ListView) this.findViewById(R.id.vp_list);
        vpsListView.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, newVpsList));
        vpsListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        vpsListView.setOnItemClickListener(this);
        vpsListView.setVisibility(View.VISIBLE);

        vpLocationDesEditTextView = (EditText) this.findViewById(R.id.descVPEditText);
        vpIdNumber = (TextView) this.findViewById(R.id.textView2);

        vpAcquiredStatus = (TextView) this.findViewById(R.id.vpAcquiredStatus);
        targetImageView = (ImageView) this.findViewById(R.id.imageViewTarget);

        okButton = (Button) this.findViewById(R.id.button2);
        requestPhotoButton = (Button) this.findViewById(R.id.buttonRequestPhoto);

        increaseQtyVps = (Button) this.findViewById(R.id.buttonIncreaseQtyVps);
        decreaseQtyVps = (Button) this.findViewById(R.id.buttonDecreaseQtyVps);
        saveTrkVpsData = (Button) this.findViewById(R.id.buttonSaveTrackingVpsData);
        ambiguousVpToggle = (Button) this.findViewById(R.id.buttonAmbiguousVpToggle);
        flashTorchVpToggle = (Button) this.findViewById(R.id.buttonFlashTorchVpToggle);
        superSingleVpToggle= (Button) this.findViewById(R.id.buttonSuperSingleVpToggle);
        superVpIdIs20mmToggle=(Button) this.findViewById(R.id.buttonId20mmToggle);
        superVpIdIs100mmToggle=(Button) this.findViewById(R.id.buttonId100mmToggle);

        acceptVpPhotoButton = (Button) this.findViewById(R.id.buttonAcceptVpPhoto);
        rejectVpPhotoButton = (Button) this.findViewById(R.id.buttonRejectVpPhoto);


        mProgress = (ImageView) this.findViewById(R.id.waitingTrkLoading);
        rotationMProgress = AnimationUtils.loadAnimation(this, R.anim.clockwise_rotation);
        mProgress.setVisibility(View.GONE);
        mProgress.startAnimation(rotationMProgress);

        imageView = (TouchImageView) this.findViewById(R.id.imageView1);


    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current camera index.
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);

        // Save the current image size index.
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX, mImageSizeIndex);

        // Save the current filter indices.
        savedInstanceState.putInt(STATE_IMAGE_DETECTION_FILTER_INDEX, mVpConfigureFilterIndex);

        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    protected void onStart()
    {
        super.onStart();


    }


    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG,"onResume CALLED");
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, mLoaderCallback);
        if (!OpenCVLoader.initDebug()) {
            Log.d("ERROR", "Unable to load OpenCV");
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        mIsMenuLocked = false;
        //if (mGoogleApiClient.isConnected()) startLocationUpdates();
        setVpsChecked();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                }

            }
        });
    }


    // The OpenCV loader callback.
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(final int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded successfully");
                    //TODO: Fix this
                    mCameraMatrix = MymUtils.getCameraMatrix(Constants.cameraWidthInPixels, Constants.cameraHeigthInPixels);
                    mCameraView.enableView();
                    //mCameraView.enableFpsMeter();

                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    private void configureTracking(Mat referenceImage){

        ARFilter trackFilter = null;
        try {
            Log.d(TAG," configureTracking(): DONE 1 ");
            trackFilter = new VpConfigFilter(
                    ConfigActivity.this,
                    referenceImage,
                    mCameraMatrix, Constants.standardMarkerlessMarkerWidth);
            trackingConfigDone = true;
        } catch (IOException e) {
            Log.e(TAG, "Failed to configure tracking:"+ e.toString());
            trackingConfigDone = false;
        }
        if (trackFilter!=null){
            mVpConfigureFilters = new ARFilter[] {
                    new NoneARFilter(),
                    trackFilter
            };
            acquisitionStartTime = System.currentTimeMillis();
            if (!trackingConfigDone) trackingConfigDone=true;
            Log.d(TAG," configureTracking(): DONE 2 ="+acquisitionStartTime);
        } else {
            Log.e(TAG," configureTracking(): FAILED ");
        }

    }


    @Override
    public Mat onCameraFrame(final CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        final Mat rgba = inputFrame.rgba();
        float[] trckValues;

        // Apply the active filters.
        //Log.d(TAG,"1 waitingForTrackingAcquisition="+waitingForTrackingAcquisition);
        if ((mVpConfigureFilters != null)&& waitingForTrackingAcquisition) {
            mVpConfigureFilters[mVpConfigureFilterIndex].apply(rgba);
            trckValues = mVpConfigureFilters[mVpConfigureFilterIndex].getPose();
            Log.d(TAG,"(trckValues!=null)="+(trckValues!=null));
            if (trckValues!=null){
                Log.d(TAG,"(System.currentTimeMillis()-acquisitionStartTime)="+(System.currentTimeMillis()-acquisitionStartTime));
                Log.d(TAG,"trckValues: Translations = "+trckValues[0]+" | "+trckValues[1]+" | "+trckValues[2]);
                Log.d(TAG,"trckValues: Rotations = "+trckValues[3]*(180.0f/Math.PI)+" | "+trckValues[4]*(180.0f/Math.PI)+" | "+trckValues[5]*(180.0f/Math.PI));
                vpXCameraDistance[vpIndex-1] = Math.round(trckValues[0])+Constants.xAxisTrackingCorrection;
                vpYCameraDistance[vpIndex-1] = Math.round(trckValues[1])+Constants.yAxisTrackingCorrection;
                vpZCameraDistance[vpIndex-1] = Math.round(trckValues[2]);
                vpXCameraRotation[vpIndex-1] = (int) Math.round(trckValues[3]*(180.0f/Math.PI));
                vpYCameraRotation[vpIndex-1] = (int) Math.round(trckValues[4]*(180.0f/Math.PI));
                vpZCameraRotation[vpIndex-1] = (int) Math.round(trckValues[5]*(180.0f/Math.PI));
                vpAcquired[vpIndex-1]=true;
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        vpAcquiredStatus.setText(R.string.vpAcquiredStatus);
                    }
                });
                Log.d(TAG, "onCameraFrame:Setting to true: VpAcquired: ["+(vpIndex-1)+"] = "+vpAcquired[vpIndex-1]);
                vpChecked[vpIndex-1]=true;
                setVpsChecked();
                saveVpsData();
                saveTrackingConfig();
                if (vpDescAndMarkerImageOK){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), getString(R.string.vp_capture_success), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                Log.d(TAG,"2 waitingForTrackingAcquisition="+waitingForTrackingAcquisition);
                waitingForTrackingAcquisition = false;
                trackingConfigDone = false;
            } else {
                if (vpDescAndMarkerImageOK && ((System.currentTimeMillis()-acquisitionStartTime)>2000)) {
                    Log.d(TAG, "(System.currentTimeMillis()-acquisitionStartTime)=" + (System.currentTimeMillis() - acquisitionStartTime));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getBaseContext(), getString(R.string.vp_acquisition_failure), Toast.LENGTH_SHORT).show();
                        }
                    });
                    // Giving up tracking acquisition as it is taking too long>>>> need to change marker or method
                    Log.d(TAG, "3 waitingForTrackingAcquisition=" + waitingForTrackingAcquisition);
                    waitingForTrackingAcquisition = false;
                    trackingConfigDone = false;
                    vpChecked[vpIndex-1]=false;
                    setVpsChecked();
                }
            }
        }

        if (cameraPhotoRequested) {
            cameraPhotoRequested = false;
            if ((!vpAcquired[vpIndex-1]))
            {
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        targetImageView.setVisibility(View.VISIBLE);
                        targetImageView.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                if (event.getAction() == MotionEvent.ACTION_DOWN)
                                {
                                    takePhoto(rgba);
                                };
                                return false;
                            }
                        });
                    }
                });
            }

        }

        return rgba;
    }


    private void takePhoto(final Mat rgba) {
        try {
            mBgr = new Mat();
            // bitmap to descvp and markervp
            Bitmap bitmapImage = null;
            Bitmap markerFromBitmapImage = null;
            // creating temp local storage files
            File pictureFile = new File(getApplicationContext().getFilesDir(), "descvp"+vpIndex+".png");
            File markerFile = new File(getApplicationContext().getFilesDir(), "markervp"+vpIndex+".png");
            if(rgba != null)
            {
                // getting bitmap from cameraframe
                Log.d(TAG, "takePhoto: a new camera frame image was delivered");
                bitmapImage = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(rgba,bitmapImage);
                final int width = bitmapImage.getWidth();
                final int height = bitmapImage.getHeight();
                int markerWidthLocal = 0;
                int markerHeightLocal = 0;
                if (!vpIsSuperSingle[vpIndex-1]) {
                    if (vpMarkerlessMarkerWidth[vpIndex-1]>1) {
                        markerWidthLocal = vpMarkerlessMarkerWidth[vpIndex-1];
                    } else {
                        markerWidthLocal = Constants.standardMarkerlessMarkerWidth;
                    }
                    if (vpMarkerlessMarkerHeigth[vpIndex-1]>1) {
                        markerHeightLocal = vpMarkerlessMarkerHeigth[vpIndex-1];
                    } else {
                        markerHeightLocal = Constants.standardMarkerlessMarkerHeigth;
                    }
                } else {
                    markerWidthLocal = Constants.standardMarkerlessMarkerWidth;
                    markerHeightLocal = Constants.standardMarkerlessMarkerHeigth;
                }
                if (vpMarkerlessMarkerWidth[vpIndex-1]>width) markerWidthLocal=width;
                if (vpMarkerlessMarkerHeigth[vpIndex-1]>height) markerHeightLocal=height;
                int x = (width - markerWidthLocal)/2;
                int y = (height - markerHeightLocal)/2;
                // getting marker from bitmap, centering the marker in the original bitmap
                markerFromBitmapImage = Bitmap.createBitmap(bitmapImage, x, y, markerWidthLocal, markerHeightLocal);
                //markerFromBitmapImage = greyScaler(markerFromBitmapImage);
                markerFromBitmapImage = markerFromBitmapImage.createScaledBitmap(markerFromBitmapImage, Constants.captureMarkerWidth, Constants.captureMarkerHeight, false);
                Utils.bitmapToMat(markerFromBitmapImage,mBgr);
                Imgproc.cvtColor(mBgr, mBgr, Imgproc.COLOR_BGR2GRAY);
                Utils.matToBitmap(mBgr, markerFromBitmapImage);
                Log.d(TAG, "Camera frame width: "+width+" height: "+height);
            }
            if (pictureFile == null)
            {
                Log.e(TAG, "Error creating PICTURE media file, check storage permissions. ");
                return;
            }
            if (markerFile == null)
            {
                Log.e(TAG, "Error creating MARKER media file, check storage permissions. ");
                return;
            }
            FileOutputStream fos_d = new FileOutputStream(pictureFile);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos_d);
            fos_d.close();
            FileOutputStream fos_m = new FileOutputStream(markerFile);
            markerFromBitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos_m);
            fos_m.close();
            // if marker from bitmap image is ok and not a super VP then save it to remote storage
            if (markerFromBitmapImage != null)
            {
                // Set the tracking configuration using the markerFile currently in the app data folder
                if (vpIsSuperSingle[vpIndex-1])
                {
                    //setSLAMTrackingConfig();
                    Log.d(TAG, "takePhoto: calling setSuperSingleIdMarkerTrackingConfig()");
                    waitingForVpSuperMarkerId=true;
                    setSuperIdMarkersTrackingConfig();
                    //setSuperSingleIdMarkerTrackingConfig();
                }
                else {
                    Log.d(TAG, "takePhoto: calling configureTracking()");
                    Log.d(TAG,"4 waitingForTrackingAcquisition="+waitingForTrackingAcquisition);
                    Log.d(TAG,"4 trackingConfigDone="+trackingConfigDone);
                    waitingForTrackingAcquisition = true;
                    if (!trackingConfigDone) configureTracking(mBgr);

                    ObjectMetadata myObjectMetadata = new ObjectMetadata();
                    //create a map to store user metadata
                    Map<String, String> userMetadata = new HashMap<String, String>();
                    userMetadata.put("VP", "" + (vpIndex));
                    userMetadata.put("mymensorAccount", mymensorAccount);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String formattedDateTime = sdf.format(MymUtils.timeNow(clockSetSuccess, sntpTime, sntpTimeReference));
                    userMetadata.put("DateTime", formattedDateTime);
                    //call setUserMetadata on our ObjectMetadata object, passing it our map
                    myObjectMetadata.setUserMetadata(userMetadata);
                    //uploading the objects
                    TransferObserver observer = MymUtils.storeRemoteFile(
                            transferUtility,
                            markervpRemotePath + markerFile.getName(),
                            Constants.BUCKET_NAME,
                            markerFile,
                            myObjectMetadata);
                    Log.d(TAG, "AWS s3 Observer: " + observer.getState().toString());
                    Log.d(TAG, "AWS s3 Observer: " + observer.getAbsoluteFilePath());
                    Log.d(TAG, "AWS s3 Observer: " + observer.getBucket());
                    Log.d(TAG, "AWS s3 Observer: " + observer.getKey());
                }
            }
            // if bitmapimage is OK it is saved to remote storage
            if (bitmapImage != null)
            {
                cameraPhotoRequested =false;
                vpDescAndMarkerImageOK = true;
                ObjectMetadata myObjectMetadata = new ObjectMetadata();
                //create a map to store user metadata
                Map<String, String> userMetadata = new HashMap<String,String>();
                userMetadata.put("VP", ""+(vpIndex));
                userMetadata.put("mymensorAccount", mymensorAccount);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String formattedDateTime = sdf.format(MymUtils.timeNow(clockSetSuccess,sntpTime,sntpTimeReference));
                userMetadata.put("DateTime", formattedDateTime);
                //call setUserMetadata on our ObjectMetadata object, passing it our map
                myObjectMetadata.setUserMetadata(userMetadata);
                //uploading the objects
                TransferObserver observer = MymUtils.storeRemoteFile(
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
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    if (targetImageView.isShown()) targetImageView.setVisibility(View.GONE);
                }
            });
        } catch (Exception e){
            vpChecked[(vpIndex-1)] = false;
            setVpsChecked();
            mIsMenuLocked = false;
            cameraPhotoRequested =true;
            vpDescAndMarkerImageOK = false;
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getBaseContext(), getString(R.string.vp_capture_failure), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    @Override
    public void onCameraViewStarted(final int width,
                                    final int height) {
    }


    @Override
    public void onCameraViewStopped() {
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
    public void recreate() {
        super.recreate();
    }


    @Override
    protected void onRestart()
    {
        super.onRestart();
        Log.d(TAG,"onRestart CALLED");
        setVpsChecked();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                {
                    mProgress.clearAnimation();
                    mProgress.setVisibility(View.GONE);
                }

            }
        });
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG,"onPause CALLED");
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
        //stopLocationUpdates();
        finish();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        Log.d(TAG,"onStop CALLED");
        //mGoogleApiClient.disconnect();
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG,"onDestroy CALLED");
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        // Dispose of native resources.
        disposeFilters(mVpConfigureFilters);
        super.onDestroy();
    }

    private void disposeFilters(Filter[] filters) {
        if (filters!=null) {
            for (Filter filter : filters) {
                filter.dispose();
            }
        }
    }


    private void setVpsChecked()
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
                        vpsListView.setItemChecked(i, vpChecked[i]);
                    }
                }
            });
        }
        catch (Exception e)
        {
            Log.e(TAG , "SetVpsChecked failed: "+e.toString());
        }
    }


    private void saveTrackingConfig()
    {

    }

    private void saveVpsData()
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
            File vpsConfigFile = new File(getApplicationContext().getFilesDir(),vpsRemotePath);
            FileUtils.writeStringToFile(vpsConfigFile,vpsConfigFileContents, UTF_8);
            ObjectMetadata myObjectMetadata = new ObjectMetadata();
            //create a map to store user metadata
            Map<String, String> userMetadata = new HashMap<String,String>();
            userMetadata.put("TimeStamp", MymUtils.timeNow(clockSetSuccess,sntpTime,sntpTimeReference).toString());
            myObjectMetadata.setUserMetadata(userMetadata);
            TransferObserver observer = MymUtils.storeRemoteFile(transferUtility, (vpsRemotePath + Constants.vpsConfigFileName), Constants.BUCKET_NAME, vpsConfigFile, myObjectMetadata);
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
            Log.e(TAG, "saveVpsData(): failed, see stack trace: "+e.toString());
        }

    }

    private void setSuperIdMarkersTrackingConfig(){

    }

    private void setTrackingConfig(){

    }



    @Override
    public void onItemClick(AdapterView<?> adapter, View view, final int position, long id)
    {
        vpLocationDescImageFileContents = null;
        vpIndex = (short) (position+1);
        if (position > (qtyVps-1))
        {
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    vpsListView.setItemChecked(position, false);
                    String message = getString(R.string.vp_name)+vpIndex+" "+getString(R.string.vp_out_of_bounds);
                    Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            });
            return;
        }
        // Local file path of VP Location Picture Image
        try
        {
            InputStream fis = MymUtils.getLocalFile("descvp"+(position+1)+".png",getApplicationContext());
            if (!(fis==null)){
                vpLocationDescImageFileContents = BitmapFactory.decodeStream(fis);
                fis.close();
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "vpLocationDescImageFile failed, see stack trace"+e.toString());
        }
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                Log.d(TAG, "Showing vpLocationDescImageFile for VP="+vpIndex+"(vpLocationDescImageFileContents==null)"+(vpLocationDescImageFileContents==null));
                // VP Location Picture ImageView
                if (!(vpLocationDescImageFileContents==null))
                {
                    imageView.setImageBitmap(vpLocationDescImageFileContents);
                    imageView.setVisibility(View.VISIBLE);
                }
                vpsListView.setItemChecked(position, vpChecked[position]);
                // VP Location Description TextView
                vpLocationDesEditTextView.setText(vpLocationDesText[position]);
                vpLocationDesEditTextView.setVisibility(View.VISIBLE);
                vpLocationDesEditTextView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_DONE)
                        {
                            vpLocationDesText[position] = vpLocationDesEditTextView.getText().toString();
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
        if (v.getId() == R.id.button2)
        {
            vpLocationDesEditTextView.setVisibility(View.GONE);
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
            vpLocationDesEditTextView.setVisibility(View.GONE);
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

        }
        if (v.getId() == R.id.buttonIncreaseQtyVps)
        {
            qtyVps++;
            if (qtyVps>Constants.maxQtyVps) qtyVps = Constants.maxQtyVps;
            increaseArraysLength(qtyVps);

        }
        if (v.getId() == R.id.buttonDecreaseQtyVps)
        {
            qtyVps--;
            if (qtyVps<1) qtyVps = 1;
            decreaseArraysLength(qtyVps);

        }
        if (v.getId() == R.id.buttonSaveTrackingVpsData)
        {
            if (targetImageView.isShown()) targetImageView.setVisibility(View.GONE);
            saveTrackingConfig();
            saveVpsData();
        }
        if (v.getId() == R.id.buttonAmbiguousVpToggle)
        {
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
                    vpMarkerlessMarkerWidth[vpIndex-1] = Constants.seaMensorMarkerWidthWhenIdIs100mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = Constants.seaMensorMarkerHeigthWhenIdIs100mm;
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
                    vpMarkerlessMarkerWidth[vpIndex-1] = Constants.standardMarkerlessMarkerWidth;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = Constants.standardMarkerlessMarkerHeigth;
                }

            }
        }
        if (v.getId() == R.id.buttonId20mmToggle)
        {
            if (vpSuperIdIs20mm[vpIndex-1])
            {
                if (vpIsSuperSingle[vpIndex-1])
                {
                    vpSuperIdIs20mm[vpIndex-1] = false;
                    vpSuperIdIs100mm[vpIndex-1] = true;
                    superVpIdIs100mmToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                    vpMarkerlessMarkerWidth[vpIndex-1] = Constants.seaMensorMarkerWidthWhenIdIs100mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = Constants.seaMensorMarkerHeigthWhenIdIs100mm;
                }
            }
            else
            {
                if (vpIsSuperSingle[vpIndex-1])
                {
                    vpSuperIdIs20mm[vpIndex-1] = true;
                    vpMarkerlessMarkerWidth[vpIndex-1] = Constants.seaMensorMarkerWidthWhenIdIs20mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = Constants.seaMensorMarkerHeigthWhenIdIs20mm;
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
            if (vpSuperIdIs100mm[vpIndex-1])
            {
                if (vpIsSuperSingle[vpIndex-1])
                {
                    vpSuperIdIs100mm[vpIndex-1] = false;
                    vpSuperIdIs20mm[vpIndex-1] = true;
                    superVpIdIs20mmToggle.setBackgroundResource(R.drawable.custom_button_option_selected);
                    vpMarkerlessMarkerWidth[vpIndex-1] = Constants.seaMensorMarkerWidthWhenIdIs20mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = Constants.seaMensorMarkerHeigthWhenIdIs20mm;
                }
            }
            else
            {
                if (vpIsSuperSingle[vpIndex-1])
                {
                    vpSuperIdIs100mm[vpIndex-1] = true;
                    vpMarkerlessMarkerWidth[vpIndex-1] = Constants.seaMensorMarkerWidthWhenIdIs100mm;
                    vpMarkerlessMarkerHeigth[vpIndex-1] = Constants.seaMensorMarkerHeigthWhenIdIs100mm;
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
        short vpListOrder = 0;

        vpLocationDesText = new String[qtyVps];
        vpXCameraDistance = new int[qtyVps];
        vpYCameraDistance = new int[qtyVps];
        vpZCameraDistance = new int[qtyVps];
        vpXCameraRotation = new int[qtyVps];
        vpYCameraRotation = new int[qtyVps];
        vpZCameraRotation = new int[qtyVps];
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

        Log.d(TAG, "loadConfigurationFile() started");

        for (int i=0; i<qtyVps; i++)
        {
            vpFrequencyUnit[i] = "";
            vpFrequencyValue[i] = 0;
            vpMarkerlessMarkerWidth[i] = Constants.standardMarkerlessMarkerWidth;
            vpMarkerlessMarkerHeigth[i] = Constants.standardMarkerlessMarkerHeigth;
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


            Log.d(TAG,"Vps Config Local name = "+Constants.vpsConfigFileName);
            File vpsFile = new File(getApplicationContext().getFilesDir(),Constants.vpsConfigFileName);
            InputStream fis = MymUtils.getLocalFile(Constants.vpsConfigFileName, getApplicationContext());
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
                        //
                    }
                    else if(eventType == XmlPullParser.START_TAG)
                    {
                        //
                        if(myparser.getName().equalsIgnoreCase("Parameters"))
                        {
                            //
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
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpNumber"))
                        {
                            eventType = myparser.next();
                            vpNumber[vpListOrder-1] = Short.parseShort(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpXCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpXCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpYCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpYCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpZCameraDistance"))
                        {
                            eventType = myparser.next();
                            vpZCameraDistance[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpXCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpXCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpYCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpYCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
                        }
                        else if(myparser.getName().equalsIgnoreCase("VpZCameraRotation"))
                        {
                            eventType = myparser.next();
                            vpZCameraRotation[vpListOrder-1] = Integer.parseInt(myparser.getText());
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
                        Log.d(TAG, "End tag "+myparser.getName());
                    }
                    else if(eventType == XmlPullParser.TEXT)
                    {
                        Log.d(TAG, "Text "+myparser.getText());
                    }
                    eventType = myparser.next();
                }
                fis.close();
            }
            finally
            {
                Log.d(TAG, "Vps Config file = "+vpsFile);
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "Vps data loading failed:"+e.toString());
            runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    Toast.makeText(getBaseContext(), "Vps Config file loading FAILED", Toast.LENGTH_LONG).show();
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
            }

            if (vpFrequencyValue[i]==0)
            {
                vpFrequencyValue[i]=frequencyValue;
            }

        }

    }


    public void decreaseArraysLength (int newlength)
    {
        vpChecked = Arrays.copyOf(vpChecked, newlength);
        vpAcquired = Arrays.copyOf(vpAcquired, newlength);
        vpNumber =Arrays.copyOf(vpNumber, newlength);
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
        final int newlengthF = newlength;
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                String[] newVpsList = new String[newlengthF];
                for (int i=0; i<newlengthF; i++)
                {
                    newVpsList[i] = getString(R.string.vp_name)+vpNumber[i];
                }
                vpsListView.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, newVpsList));
                String message = getString(R.string.button_text_decrease_qtyvps)+" Old="+(newlengthF+1)+" New="+newlengthF+"";
                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }


    public void increaseArraysLength (int newlength)
    {
        vpChecked = Arrays.copyOf(vpChecked, newlength);
        vpAcquired = Arrays.copyOf(vpAcquired, newlength);
        vpNumber =Arrays.copyOf(vpNumber, newlength);
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
        vpMarkerlessMarkerWidth[newlength-1] = Constants.standardMarkerlessMarkerWidth;
        vpMarkerlessMarkerHeigth[newlength-1] = Constants.standardMarkerlessMarkerHeigth;
        vpNumber[newlength-1]= (short) newlength;
        vpLocationDesText[newlength-1]= getString(R.string.vp_capture_placeholder_description)+newlength;
        final int newlengthF = newlength;
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                String[] newVpsList = new String[newlengthF];
                for (int i=0; i<newlengthF; i++)
                {
                    newVpsList[i] = getString(R.string.vp_name)+vpNumber[i];
                }
                vpsListView.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, newVpsList));
                String message = getString(R.string.button_text_increase_qtyvps)+" Old="+(newlengthF-1)+" New="+newlengthF+"";
                Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
            }
        });
    }












}
