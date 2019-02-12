package cpm.com.motorola.geocode;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonFunctions;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.database.MotorolaDatabase;

public class LocationActivity extends AppCompatActivity implements OnMapReadyCallback, ConnectionCallbacks, OnConnectionFailedListener, LocationListener {
    protected static final String PHOTO_TAKEN = "photo_taken";
    LocationManager locationManager;
    Geocoder geocoder;
    protected Button _buttonsave;
    File file;
    protected ImageView _image;
    protected boolean _taken;
    Button StoreFront;
    public String text;
    public View view;
    GeotaggingBeans data = new GeotaggingBeans();
    private LocationManager locmanager = null;
    protected String _path;
    boolean enabled;
    protected String _pathforcheck = "";
    public ArrayList<Storenamebean> storedetails = new ArrayList<Storenamebean>();
    String storeid;
    String storelatitude = "0";
    String storelongitude = "0";
    private GoogleMap mMap;
    double lat;
    double longitude;
    private GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    String visit_date, username;
    Marker currLocationMarker;
    LatLng latLng;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private static int UPDATE_INTERVAL = 1000; // 10 sec
    private static int FATEST_INTERVAL = 500; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters
    Location mLastLocation;
    private SharedPreferences preferences = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gpslocationscreen);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        storeid = preferences.getString(CommonString.KEY_STORE_CD, null);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        username = preferences.getString(CommonString.KEY_USERNAME, null);
        _image = (ImageView) findViewById(R.id.image);
        _buttonsave = (Button) findViewById(R.id.savedetails);
        StoreFront = (Button) findViewById(R.id.StoreFront);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        //for crate home button
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapview);
        mapFragment.getMapAsync(this);
        MotorolaDatabase data1 = new MotorolaDatabase(getApplicationContext());
        data1.open();
        storedetails = new ArrayList<>();
        ImageView img = new ImageView(getApplicationContext());
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        geocoder = new Geocoder(this);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();
        }
        if (checkPlayServices()) {
            // Building the GoogleApi client
            buildGoogleApiClient();

            createLocationRequest();
        }

        if (!(storelatitude.equals("0")) && !(storelongitude.equals("0"))) {
            int latiti = (int) (Double.parseDouble(storelatitude) * 1000000);
            int longi = (int) (Double.parseDouble(storelongitude) * 1000000);
        }

        _buttonsave.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (!_pathforcheck.equals("")) {
                    if (!(storelatitude.equals("0")) && !(storelongitude.equals("0"))) {
                        lat = Double.parseDouble(storelatitude);
                        longitude = Double.parseDouble(storelongitude);
                    } else {
                        lat = data.getLatitude();
                        longitude = data.getLongitude();
                    }
                    if (ImageUploadActivity.CheckGeotagImage(_pathforcheck)) {
                        MotorolaDatabase data = new MotorolaDatabase(getApplicationContext());
                        data.open();
                        data.updateLaTLONGAndSTATUS(CommonString.KEY_Y, storeid, visit_date, lat, longitude);
                        data.InsertStoregeotagging(storeid, lat, longitude, _pathforcheck, CommonString.KEY_N);
                        if (isNetworkOnline()) {
                            Intent intent2 = new Intent(LocationActivity.this, UploadGeotaggingActivity.class);
                            startActivity(intent2);
                            LocationActivity.this.finish();
                        }
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
                        builder.setMessage("Please take Store Front image").setCancelable(false).setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }

                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext()).setMessage("Please take Store Front image").setCancelable(false).setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            }


        });
        StoreFront.setOnClickListener(new ButtonClickHandler());


        locmanager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        enabled = locmanager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!enabled) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(LocationActivity.this);
            // Setting Dialog Title
            alertDialog.setTitle("GPS IS DISABLED...");
            // Setting Dialog Message
            alertDialog.setMessage("Click ok to enable GPS.");
            // Setting Positive "Yes" Button
            alertDialog.setPositiveButton("YES",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    });
            // Setting Negative "NO" Button
            alertDialog.setNegativeButton("NO",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Write your code here to invoke NO event
                            dialog.cancel();
                        }
                    });
            // Showing Alert Message
            alertDialog.show();

        }
    }

    public boolean isNetworkOnline() {
        boolean status = false;
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(0);
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                status = true;
            } else {
                netInfo = cm.getNetworkInfo(1);
                if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED)
                    status = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return status;
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("MakeMachine", "resultCode: " + resultCode);
        switch (resultCode) {
            case 0:
                Log.i("MakeMachine", "User cancelled");
                break;
            case -1:
                if (_pathforcheck != null && !_pathforcheck.equals("")) {
                    try {
                        if (new File(CommonString.FILE_PATH + _pathforcheck).exists()) {
                            String metadata = CommonFunctions.setMetadataAtImages(preferences.getString(CommonString.KEY_STORE_NAME, ""), storeid, "GeoTag Image", username);
                            CommonFunctions.addMetadataAndTimeStampToImage(LocationActivity.this, _path, metadata, visit_date);
                            StoreFront.setBackgroundResource(R.drawable.camera_icon_done);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    public class ButtonClickHandler implements OnClickListener {
        public void onClick(View view) {
            if (!(storelatitude.equals("0")) && !(storelongitude.equals("0"))) {
                if (view.getId() == R.id.StoreFront) {
                    _pathforcheck = storeid + "_GEOTAG_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
                    _path = CommonString.FILE_PATH + _pathforcheck;
                    CommonFunctions.startAnncaCameraActivity(LocationActivity.this, _path);
                }
            } else if (data.getLatitude() == 0 && data.getLongitude() == 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());

                builder.setMessage("Wait For Geo Location").setCancelable(false).setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();

            } else {
                if (view.getId() == R.id.StoreFront) {
                    _pathforcheck = storeid + "_GEOTAG_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
                    _path = CommonString.FILE_PATH + _pathforcheck;
                    CommonFunctions.startAnncaCameraActivity(LocationActivity.this, _path);
                }
            }
        }
    }


    public String getCurrentTime() {
        Calendar m_cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:mmm");
        String cdate = formatter.format(m_cal.getTime());
        return cdate;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        // TODO Auto-generated method stub
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getMaxZoomLevel();
        mMap.getMinZoomLevel();
        mMap.getUiSettings();
        mMap.animateCamera(CameraUpdateFactory.zoomIn());
        mMap.animateCamera(CameraUpdateFactory.zoomOut());
        mMap.animateCamera(CameraUpdateFactory.zoomTo(20));
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult arg0) {
        // TODO Auto-generated method stub

    }

    public void onConnected(Bundle bundle) {
        if (_pathforcheck.equals("")) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                String result = null;
                LatLng latLng;
                try {
                    List<Address> addressList = geocoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                    if (addressList != null && addressList.size() > 0) {
                        result = addressList.get(0).getAddressLine(0);
                    }
                    latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(latLng);
                    markerOptions.title(result);
                    markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                    currLocationMarker = mMap.addMarker(markerOptions);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                    data.setLatitude((mLastLocation.getLatitude()));
                    data.setLongitude((mLastLocation.getLongitude()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(5000); //5 seconds
            mLocationRequest.setFastestInterval(3000); //3 seconds
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        // TODO Auto-generated method stub

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
    }

    protected void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, (LocationListener) this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        mLastLocation = location;
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Snackbar.make(_buttonsave, "This device is not supported.", Snackbar.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    protected void onStart() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
        super.onStart();

    }

    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    public void onBackPressed() {
        // TODO Auto-generated method stub
        LocationActivity.this.finish();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
        }
        return super.onOptionsItemSelected(item);
    }


}