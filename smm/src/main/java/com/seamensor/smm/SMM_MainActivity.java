package com.seamensor.smm;

import com.seamensor.smm.AlertDialogRadio.AlertPositiveListener;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class SMM_MainActivity extends AppCompatActivity implements AlertPositiveListener
{

    private DrawerLayout navigationDrawerLayout;
    private ListView navigationDrawerList;

    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    public String seamensorAccount = null;

    public boolean notSeamensorAccount = false;
    public boolean seamensorAdminPresent = false;

    public final String seamensorDomain = "@seamensor.com";
    public final String adminOne = "seamensor01";
    public final String adminTwo = "seamensor02";
    public final String adminThree = "seamensor03";
    public final String adminFour = "seamensor04";
    public final String adminFive = "seamensor05";

    public Connection sqlServerConnection;
    int selectedShipIndex = 0;
    private static long back_pressed;
    public short vpIndex = 1;

    public String[] navigationDrawerOptionsList = new String[4];

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // Retrieving SeaMensor Account information, if account does not exist then app is closed
        try
        {
            Log.i("OnCreate","OnCreate: READING ACCOUNTS INFORMATION");
            AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            Account[] list = manager.getAccounts();
            for(Account account: list)
            {
                Log.i("OnCreate","OnCreate: Account: Name="+account.name+"Type="+account.type);
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
                Log.i("OnCreate","OnCreate: Seamensor Account: "+seamensorAccount);
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
            Log.e("OnCreate", "OnCreate: Seamensor user not present in this device");
        }

        setContentView(R.layout.main_layout);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(mToolbar);

        navigationDrawerOptionsList[0] = getString(R.string.select_ship);
        navigationDrawerOptionsList[1] = getString(R.string.read_tags_from_selected_ship);
        navigationDrawerOptionsList[2] = getString(R.string.configure_selected_ship);
        navigationDrawerOptionsList[3] = getString(R.string.configure_dcis_from_selected_ship);

        mTitle = mDrawerTitle = getTitle();
        navigationDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationDrawerList = (ListView) findViewById(R.id.left_drawer);

        navigationDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        navigationDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, navigationDrawerOptionsList));

        navigationDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        navigationDrawerList.setItemChecked(vpIndex-1,true);


        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle
                (
                this,                   /* host Activity */
                navigationDrawerLayout, /* DrawerLayout object */
                mToolbar,               /* toolbar appcompat v7 */
                R.string.drawer_open,   /* "open drawer" description for accessibility */
                R.string.drawer_close   /* "close drawer" description for accessibility */
                )

            {
                public void onDrawerClosed(View view)
                {
                    Log.i("onDrawerClosed","getSupportActionBar().setTitle(mTitle);");
                    getSupportActionBar().setTitle(mTitle);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }

                public void onDrawerOpened(View drawerView)
                {
                    Log.i("onDrawerOpened","getSupportActionBar().setTitle(mDrawerTitle);");
                    getSupportActionBar().setTitle(mDrawerTitle);
                    invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                }
            };

        navigationDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null)
        {
            selectItem(0);
        }

    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = navigationDrawerLayout.isDrawerOpen(navigationDrawerList);
        //menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_smm_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        int id = item.getItemId();
        switch (id)
        {
            case R.id.action_settings:
                return true;
            case R.id.action_usrsigin:
                return true;
            case R.id.action_selectship:
                shipSelector();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed()
    {
        if (navigationDrawerLayout.isDrawerOpen(GravityCompat.START)) navigationDrawerLayout.closeDrawers();
        if (back_pressed + 2000 > System.currentTimeMillis())
            super.onBackPressed();
        else
            Toast.makeText(getBaseContext(), getString(R.string.double_bck_exit), Toast.LENGTH_SHORT).show();
        back_pressed = System.currentTimeMillis();
    }

    @Override
    public void onStart()
    {
        super.onStart();
        if (notSeamensorAccount)
        {
            showNotSeamensorAccountError();
            finish();
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        if (sqlServerConnection != null)
            try
            {
                if (!sqlServerConnection.isClosed())
                {
                    sqlServerConnection.close();
                    Log.i("onDestroy", "sqlServerConnection.isClosed(): " + sqlServerConnection.isClosed());
                }
            }
            catch (SQLException e)
            {
                Log.d("ERR","ERROR CONNECTION 1 = " + e.getMessage());
                e.printStackTrace();
            }
            catch (Exception ex )
            {
                Log.d("ERR", "ERROR CONNECTION 2 = " + ex.getMessage());
                ex.printStackTrace();
            }
    }

    protected void showNotSeamensorAccountError()
    {
        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.no_seamensor_account), Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER|Gravity.CENTER_HORIZONTAL, 0, 30);
        toast.show();
    }

    /* The click listener for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id)
        {
            Log.i("onItemClick", "Adapter: " + parent);
            Log.i("onItemClick", "View: " + view);
            Log.i("onItemClick", "Position: " + position);
            Log.i("onItemClick", "Row id: " + id);
            vpIndex = (short) (position + 1);
            selectItem(position);
        }
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position)
    {
        Fragment fragment = null;
        Bundle args = new Bundle();

        switch (position)
        {
            case 0:
                fragment = new selectShipFragment();
                break;
            case 1:
                fragment = new readTagsFromSelectedShipFragment();
                break;
            case 2:
                fragment = new configureSelectedShipFragment();
                break;
            case 3:
                fragment = new configureDcisFromSelectedShipFragment();
                break;
            default:
                break;
        }


        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        navigationDrawerList.setItemChecked(position, true);
        setTitle(navigationDrawerOptionsList[position]);
        navigationDrawerLayout.closeDrawer(navigationDrawerList);
    }





    public void shipSelector()
    {
        Log.d("shipSelector", "shipSelector called");
        /** Getting the fragment manager */
        FragmentManager manager = getFragmentManager();
        /** Instantiating the DialogFragment class */
        AlertDialogRadio shipSelectorRadioButtonDialog = new AlertDialogRadio();
        /** Creating a bundle object to store the selected item's index */
        Bundle b  = new Bundle();
        /** Storing the selected item's index in the bundle object */
        b.putInt("position", selectedShipIndex);
        b.putStringArray("listOfItems", navigationDrawerOptionsList);
        b.putString("alertDialogTitle",getString(R.string.select_ship));

        /** Setting the bundle object to the dialog fragment object */
        shipSelectorRadioButtonDialog.setArguments(b);

        /** Creating the dialog fragment object, which will in turn open the alert dialog window */
        shipSelectorRadioButtonDialog.show(manager, "alertRadioDialog");

    }

    /** Defining button click listener for the OK button of the alert dialog window */
    @Override
    public void onPositiveClick(int position_ship_selector)
    {
        Log.d("shipSelector","shipSelector called "+position_ship_selector);
        selectedShipIndex = position_ship_selector;
        Log.d("shipSelector","selectedShipIndex = "+selectedShipIndex);
        connectToSqlServer();
    }

    public void connectToSqlServer()
    {
        new AsyncTask<Void, Void, Void>()
        {
            @Override
            protected Void doInBackground(Void... params)
            {
                String connectionString = "jdbc:jtds:sqlserver://ec2-54-232-197-243.sa-east-1.compute.amazonaws.com:1433/;instance=MSSQLSERVER";
                String userName = "sa";
                String password = "aA1234";

                //WIN-GTJE1KQP88N     MSSQLSERVER

                //String connectionString = "jdbc:jtds:sqlserver://ec2-54-94-154-158.sa-east-1.compute.amazonaws.com:1433/;instance=SQLEXPRESS";
                //String userName = "sa";
                //String password = "aA123";



                try
                {
                    Class.forName("net.sourceforge.jtds.jdbc.Driver");
                    sqlServerConnection = DriverManager.getConnection(connectionString, userName, password);
                    Log.i("Success", "Connected to DB");
                    Log.i("Success", "sqlServerConnection.isClosed(): " + sqlServerConnection.isClosed());
                    //sqlServerConnection.close();
                }
                catch (SQLException e)
                {
                    Log.d("ERR","ERROR CONNECTION 1 = " + e.getMessage());
                    e.printStackTrace();
                }
                catch (Exception ex )
                {
                    Log.d("ERR", "ERROR CONNECTION 2 = " + ex.getMessage());
                    ex.printStackTrace();
                }
                return null;
            }


        }.execute();
    }




}
