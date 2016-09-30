package com.seamensor.seamensor;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static long back_pressed;

    LinearLayout mainLinearLayout;
    ImageButton exitButton;
    ImageView appLogo;
    Button logInOut;
    Button startConfig;
    Button startCap;


    TextView vpIdNumber;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainLinearLayout = (LinearLayout)findViewById(R.id.MainActivityLinearLayout);
        exitButton = (ImageButton) findViewById(R.id.exitbutton);
        appLogo = (ImageView) findViewById(R.id.mainactivity_logo);
        logInOut = (Button) findViewById(R.id.buttonlog);
        startConfig = (Button) findViewById(R.id.buttonconfig);
        startCap = (Button) findViewById(R.id.buttoncap);
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

    public void onButtonClick(View v) {
        if (v.getId() == R.id.exitbutton) {
            if (back_pressed + 2000 > System.currentTimeMillis()) {
                Log.d(TAG, "Closing");
                finish();
            } else
                Toast.makeText(getBaseContext(), getString(R.string.double_bck_exit), Toast.LENGTH_SHORT).show();
            back_pressed = System.currentTimeMillis();
        }
        if (v.getId() == R.id.buttonlog) {

        }
        if (v.getId() == R.id.buttonconfig) {
            Intent intent = new Intent(getApplicationContext(), LoaderActivity.class);
            intent.putExtra("activitytobecalled", "SMC");
            startActivity(intent);
            finish();
        }
        if (v.getId() == R.id.buttoncap) {
            Intent intent = new Intent(getApplicationContext(), LoaderActivity.class);
            intent.putExtra("activitytobecalled", "seamensor");
            startActivity(intent);
            finish();
        }
    }

}
