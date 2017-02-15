package com.zackmatthews.droppin;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Vibrator;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PinActivity extends AppCompatActivity implements LocationSource, LocationListener,
        InputTextDialogFragment.InputTextDialogListener, PinOptionsDialogFragment.PinOptionsDialogListener{

    @Bind(R.id.pinDropButton)  protected ImageButton pinDropButton;

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private SharedPreferences sharedPref;

    private boolean isPinDropped;

    private static String STATUS_READY = "Pin ready to drop!";
    private static String STATUS_NAVIGATE = "Pin dropped. Ready to navigate!";

    private HashMap<Boolean, String> status = new HashMap<>();

    private PopupWindow navPopup;
    private static int SPLASH_TIME = 1500;

    private Handler handler = new Handler();
    private TextView statusText, latLongDisplay;

    private LayoutInflater inflater;

    private LocationManager locationManager;
    private LocationListener locationListener;


    private static String sPin1ID = "pin1";
    private static String sPin2ID = "pin2";
    private static String sPin3ID = "pin3";
    private static String sPin4ID = "pin4";
    private static String sPin5ID = "pin5";
    private static String sPin6ID = "pin6";

    private static String latID = "lat";
    private static String lonID = "lon";

    private static String tagID = "text";
    protected static String locationProvider = null;

    private static boolean isAnonymous = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        setupLocationManager();

        status.put(true, STATUS_NAVIGATE);
        status.put(false, STATUS_READY);

        sharedPref = getSharedPreferences(getPackageName(), MODE_PRIVATE);

        isPinDropped = sharedPref.getBoolean("isPinDropped", false);

        statusText = (TextView)findViewById(R.id.statusText);
        statusText.setText(status.get(isPinDropped));
        statusText.setVisibility(View.GONE);

        latLongDisplay = (TextView)findViewById(R.id.latLongText);

        setUpMapIfNeeded();

        inflater = getLayoutInflater();


        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                ((ImageView) findViewById(R.id.splash)).setVisibility(View.GONE);
                ((ImageView) findViewById(R.id.splash)).setEnabled(false);

                ((ImageView) findViewById(R.id.logo)).setVisibility(View.GONE);
                ((ImageView) findViewById(R.id.logo)).setEnabled(false);

                ButterKnife.bind(PinActivity.this);


                //mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
                //mDrawerList = (ListView) findViewById(R.id.left_drawer);


            }
        }, SPLASH_TIME);


        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sharedPref = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        isPinDropped = sharedPref.getBoolean("isPinDropped", false);

        if(locationManager != null){
            if(locationProvider != null) {
                locationManager.requestLocationUpdates(locationProvider, 1500, 5, this);
            }
        }
        setUpMapIfNeeded();

    }

    @Override
    protected void onPause() {
        super.onPause();

        mMap.setLocationSource(null);
        locationManager.removeUpdates(this);

        if(isPopupActive){
            navPopup.dismiss();
            isPopupActive = false;
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_pin, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_view_devinfo:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.zackmatthews.com"));
                startActivity(browserIntent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void setupLocationManager(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        System.out.println(locationManager.getAllProviders());


        List<String> providers = locationManager.getProviders(true);
        if(providers == null || providers.size() == 0 ){
            createGpsDisabledAlert();
        }

        if(locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER) && providers.size() == 1){
            createGpsDisabledAlert();
        }

        else{

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationProvider = LocationManager.NETWORK_PROVIDER;
                System.out.println("NETWORK PROVIDER ACTIVE");
            }

             if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationProvider = LocationManager.GPS_PROVIDER;
                 System.out.println("GPS ACTIVE");
            }
        }



        if(locationProvider != null) {
            locationManager.requestLocationUpdates(locationProvider, 1500,
                    5, this);
        }

    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */


    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
                mMap.setLocationSource(this);
            }
        }

    }


    private void savePinMarker(final String pinID){

        float prevlat = sharedPref.getFloat(pinID+latID, -1);
        float prevlon = sharedPref.getFloat(pinID+lonID, -1);

        if(prevlat + prevlon == -2) {
            dropMarker();

            float lat = (float) getLattitude(), lon = (float) getLongitude();
            sharedPref.edit().putFloat(pinID + latID, lat).commit();
            sharedPref.edit().putFloat(pinID + lonID, lon).commit();

            vibrate(80);

            Toast.makeText(PinActivity.this, "Pin saved. Tap to navigate.", Toast.LENGTH_SHORT).show();

            navPopup.dismiss();
            isPopupActive = false;
        }

        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(PinActivity.this);

            builder.setTitle("Would you like to clear your saved pin?").setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            }).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    sharedPref.edit().putFloat(pinID+latID, -1).commit();
                    sharedPref.edit().putFloat(pinID+lonID, -1).commit();

                    Toast.makeText(PinActivity.this, "Pin cleared", Toast.LENGTH_SHORT).show();

                    navPopup.dismiss();
                    isPopupActive = false;

                    dialog.dismiss();
                    //savePinMarker(pinID);
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.setMyLocationEnabled(true);

        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);

        mMap.setBuildingsEnabled(true);
        mMap.setIndoorEnabled(true);




        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(isPopupActive){
                    navPopup.dismiss();
                    isPopupActive = false;
                }
            }
        });

        setLatLongStatusText();

        if(locationProvider != null && locationProvider != null) {
            mMap.setLocationSource(this);
        }
    }

    private double lat, lon;
    private Marker mMarker;

    protected static boolean isPopupActive = false;

    @OnClick(R.id.pinDropButton) void actionButtonPressed(){
        // onLocationAction();

        if(isPopupActive){
            if(navPopup != null) {
                navPopup.dismiss();
                isPopupActive = false;
            }
            else{
                isPopupActive = false;
            }
        }

        else {
            FrameLayout popupLayout = (FrameLayout) inflater.inflate(R.layout.nav_popup, null);
            popupLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            int pinCount = 6;
            List<ImageButton> pins = new ArrayList<ImageButton>();
            pins.add((ImageButton) popupLayout.findViewById(R.id.pin1));
            pins.add((ImageButton)popupLayout.findViewById(R.id.pin2));
            pins.add((ImageButton)popupLayout.findViewById(R.id.pin3));
            pins.add((ImageButton)popupLayout.findViewById(R.id.pin4));
            pins.add((ImageButton)popupLayout.findViewById(R.id.pin5));
            pins.add((ImageButton)popupLayout.findViewById(R.id.pin6));

            TextView label1 = (TextView)popupLayout.findViewById(R.id.pin1text);
            label1.setText(sharedPref.getString(sPin1ID + tagID, "Pin 1"));
            TextView label2 = (TextView)popupLayout.findViewById(R.id.pin2text);
            label2.setText(sharedPref.getString(sPin2ID + tagID, "Pin 2"));
            TextView label3 = (TextView)popupLayout.findViewById(R.id.pin3text);
            label3.setText(sharedPref.getString(sPin3ID + tagID, "Pin 3"));
            TextView label4 = (TextView)popupLayout.findViewById(R.id.pin4text);
            label4.setText(sharedPref.getString(sPin4ID + tagID, "Pin 4"));
            TextView label5 = (TextView)popupLayout.findViewById(R.id.pin5text);
            label5.setText(sharedPref.getString(sPin5ID + tagID, "Pin 5"));
            TextView label6 = (TextView)popupLayout.findViewById(R.id.pin6text);
            label6.setText(sharedPref.getString(sPin6ID + tagID, "Pin 6"));

            for(int i = 1; i <= pinCount; i++){
                float lat = sharedPref.getFloat("pin" + String.valueOf(i) + latID, -1);
                float lon = sharedPref.getFloat("pin" + String.valueOf(i) + lonID, -1);

                ImageButton pinButton = pins.get(i - 1);

                if(lat + lon != -2){
                    pinButton.setImageResource(R.mipmap.ic_action_pin);
                    pinButton.setAlpha(1.0f);
                }

                pinButton.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        onLongClickSavedMarker(v);
                        return true;
                    }
                });
            }

            navPopup = new PopupWindow(popupLayout,
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, false);

            navPopup.showAtLocation(pinDropButton, Gravity.CENTER, 0, 0);//AsDropDown(pinDropButton, pinDropButton.getWidth(), pinDropButton.getHeight());
            navPopup.setOutsideTouchable(true);
            isPopupActive = true;

            setIsAnonymous(isAnonymous);
        }

    }


    public void onClickSavedMarker(View v){

        float lat = sharedPref.getFloat(v.getTag().toString() + latID, -1),
                lon = sharedPref.getFloat(v.getTag().toString() + lonID, -1);

        if(lat + lon != -2) {
            PinOptionsDialogFragment optionsDialog = new PinOptionsDialogFragment();
            optionsDialog.show(getFragmentManager(), v.getTag().toString());
        }

        else {
            savePinMarker(v.getTag().toString());
        }
    }

    public void onLongClickSavedMarker(View v){

        navPopup.setTouchable(false);
        InputTextDialogFragment inputTextDialogFragment = new InputTextDialogFragment();
        inputTextDialogFragment.setTargetID(v.getTag().toString() + tagID);

        TextView label = (TextView)navPopup.getContentView().findViewWithTag(v.getTag().toString() + tagID);
        inputTextDialogFragment.setInitialText(label.getText().toString());
        inputTextDialogFragment.show(getFragmentManager(), v.getTag().toString() + tagID);

    }

    private void dropMarker(){
        if(mMarker != null){
            mMarker.remove();
        }
        LatLng latLng = new LatLng(getLattitude(), getLongitude());
        mMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Pin"));
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 20);
        mMap.animateCamera(cameraUpdate);
    }

    private void setLatLongStatusText(){
        try {
            latLongDisplay.setText("Lat:" + String.valueOf(getLattitude()) + "\nLong:" + String.valueOf(getLongitude()));
        }

        catch(NullPointerException e){
            latLongDisplay.setText("Lat:"  + "\nLong:" );
        }
    }

    private double getLattitude(){

        try{
            lat = mMap.getMyLocation().getLatitude();
        }

        catch(NullPointerException e){

            if(locationManager == null || locationProvider == null) {
                lat = sharedPref.getFloat("lat", 0);
            }

            else {
                Location lastLocation = locationManager.getLastKnownLocation(locationProvider);
                if(lastLocation != null){
                    lat = lastLocation.getLatitude();
                }
            }
        }


        catch(IllegalStateException e){

        }
        return lat;
    }

    private double getLongitude(){

        try{
            lon = mMap.getMyLocation().getLongitude();
        }

        catch(NullPointerException e){
            if(locationManager == null || locationProvider  == null){
                lon = sharedPref.getFloat("lon", 0);
            }

            else {
                Location lastLocation = locationManager.getLastKnownLocation(locationProvider);
                if(lastLocation != null){
                    lon = lastLocation.getLongitude();
                }
            }
        }
        return lon;
    }


    public void onLocationAction(View v) {

        if (!isPinDropped) {

            if (sharedPref.getBoolean("didNavigate", true) == false) {
                evaluatePinDrop();
            }
            else{
                storeLocation();
            }

        }

        else {
            evaluateNavigation();
        }


        statusText.setText(status.get(isPinDropped));

    }


    public void evaluatePinDrop(){
        AlertDialog.Builder builder = new AlertDialog.Builder(PinActivity.this);
        AlertDialog dialog;

        builder.setTitle("Previous pin found.").setMessage("Navigate, drop new pin, or cancel?")
                .setPositiveButton("Navigate", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isPinDropped = false;
                        sharedPref.edit().putBoolean("isPinDropped", isPinDropped).commit();
                        sharedPref.edit().putBoolean("didNavigate", true).commit();
                        navigateTo();
                    }
                }).setNegativeButton("Drop pin", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isPinDropped = true;
                sharedPref.edit().putBoolean("isPinDropped", isPinDropped).commit();
                sharedPref.edit().putBoolean("didNavigate", false).commit();

                storeLocation();
            }
        }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                AlertDialog.Builder builder = new AlertDialog.Builder(PinActivity.this);

                builder.setTitle("Would you like to clear your last pin?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        isPinDropped = false;
                        sharedPref.edit().putBoolean("isPinDropped", isPinDropped).commit();
                        sharedPref.edit().putBoolean("didNavigate", true).commit();

                        mMarker.remove();

                       // pinDropButton.setImageResource(R.mipmap.ic_action_pin);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sharedPref.edit().putBoolean("didNavigate", false).commit();
                        dialog.dismiss();
                    }
                });

                AlertDialog clearDialog = builder.create();
                clearDialog.show();
            }
        });

        dialog = builder.create();
        dialog.show();
    }
        public void storeLocation(){

         //   Location mLocation = mMap.getMyLocation();
            try {
                float lat = (float) getLattitude(), lon = (float) getLongitude();

                sharedPref.edit().putFloat("lat", lat).commit();
                sharedPref.edit().putFloat("lon", lon).commit();

                isPinDropped = true;
                sharedPref.edit().putBoolean("isPinDropped", isPinDropped).commit();
                sharedPref.edit().putBoolean("didNavigate", false).commit();

                dropMarker();
                setLatLongStatusText();

              //  pinDropButton.setImageResource(R.mipmap.ic_action_navigate);
                vibrate(80);
                Toast.makeText(PinActivity.this, "Pin dropped!", Toast.LENGTH_SHORT).show();

                if(isPopupActive){
                    navPopup.dismiss();
                    isPopupActive = false;
                }

            } catch (NullPointerException e) {
                Toast.makeText(PinActivity.this, "Failed to get location. Wait a few seconds and try again.", Toast.LENGTH_SHORT).show();
            }
        }



    public void evaluateNavigation(){
        AlertDialog.Builder builder = new AlertDialog.Builder(PinActivity.this);
        AlertDialog dialog;


        builder.setTitle("Navigate to last pin?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                navigateTo();
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isPinDropped = false;
                sharedPref.edit().putBoolean("isPinDropped", isPinDropped).commit();
                sharedPref.edit().putBoolean("didNavigate", false).commit();

            }
        });

        dialog = builder.create();
        dialog.show();

    }

    public void evaluateNavigation(final String pinID){

        float prevlat = sharedPref.getFloat(pinID+latID, -1);
        float prevlon = sharedPref.getFloat(pinID+lonID, -1);

        if(prevlat + prevlon == -2){
            Toast.makeText(PinActivity.this, "Long press to save pin location.", Toast.LENGTH_SHORT).show();
        }

        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(PinActivity.this);
            AlertDialog dialog;


            builder.setTitle("Navigate to saved pin?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigateTo(pinID);
                }
            }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            dialog = builder.create();
            dialog.show();
        }
    }

    public void navigateTo(){

        isPinDropped = false;
        sharedPref.edit().putBoolean("isPinDropped", isPinDropped).commit();
        sharedPref.edit().putBoolean("didNavigate", true).commit();

//        pinDropButton.setImageResource(R.mipmap.ic_action_pin);

        float lat = sharedPref.getFloat("lat", 0);
        float lon = sharedPref.getFloat("lon", 0);

        Uri gmmIntentUri = Uri.parse("google.navigation:q="
                + String.valueOf(lat) + ","
                + String.valueOf(lon));

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);


    }

    public void navigateTo(String pinID){

            float lat = sharedPref.getFloat(pinID+latID, -1);
            float lon = sharedPref.getFloat(pinID + lonID, -1);

            Uri gmmIntentUri = Uri.parse("google.navigation:q="
                    + String.valueOf(lat) + ","
                    + String.valueOf(lon));

            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
    }

    private void showGpsOptions() {
        Intent gpsOptionsIntent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(gpsOptionsIntent);
    }
    private void createGpsDisabledAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setMessage(
                        "Your GPS is disabled! Would you like to enable it?")
                .setCancelable(false).setPositiveButton("Enable GPS",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showGpsOptions();
                    }
                });
        builder.setNegativeButton("Do nothing",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {

        dropMarker();
    }

    @Override
    public void deactivate() {

    }

    @Override
    public void onLocationChanged(Location location) {

        setLatLongStatusText();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

       // locationManager.removeUpdates(this);

        locationProvider = provider;
        locationManager.requestLocationUpdates(locationProvider, 1500,
                5, this);
       // dropMarker();
        mMap.setLocationSource(this);
        Toast.makeText(PinActivity.this, provider + " location services enabled", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        //createGpsDisabledAlert();
    }

    public void vibrate(int length){
        Vibrator v = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(length);
    }

    protected void setIsAnonymous(boolean isAnon){
        ImageButton anonPin = (ImageButton)navPopup.getContentView().findViewById(R.id.anonPinButton);
        ImageButton setAnonButton = (ImageButton)navPopup.getContentView().findViewById(R.id.anonButton);
        if(!isAnon){
            setAnonButton.setAlpha(0.7f);
            anonPin.setVisibility(View.GONE);
            anonPin.setClickable(false);
            isAnonymous = false;


        }

        else{
            setAnonButton.setAlpha(1f);
            anonPin.setVisibility(View.VISIBLE);
            anonPin.setClickable(true);
            isAnonymous = true;


        }

    }

    public void toggleAnonymousPinning(View v){
        isAnonymous = !isAnonymous;
        setIsAnonymous(isAnonymous);

        if(!isAnonymous){
            Toast.makeText(PinActivity.this, "Anonymous pinning disabled", Toast.LENGTH_SHORT).show();
        }

        else{
            Toast.makeText(PinActivity.this, "Anonymous pinning enabled", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onInputTextDialogSubmitted(InputTextDialogFragment sender, String text) {


        ((TextView)navPopup.getContentView().findViewWithTag(sender.getTag())).setText(text);
        sharedPref.edit().putString(sender.getTag().toString(), text).commit();
        navPopup.setTouchable(true);
    }

    @Override
    public void onPinOptionsItemSelected(PinOptionsDialogFragment sender, String option) {
        if(option.equals("Delete")){
            ImageButton pin = (ImageButton)navPopup.getContentView().findViewWithTag(sender.getTagID().toString());
            pin.setImageResource(android.R.drawable.ic_menu_add);
            pin.setAlpha(0.5f);

            sharedPref.edit().putFloat(pin.getTag() + latID, -1).commit();
            sharedPref.edit().putFloat(pin.getTag() + lonID, -1).commit();
        }

        else if(option.equals("Navigate")){
             evaluateNavigation(sender.getTagID().toString());
        }
    }
}
