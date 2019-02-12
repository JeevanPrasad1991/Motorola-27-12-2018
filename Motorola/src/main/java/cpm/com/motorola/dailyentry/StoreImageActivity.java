package cpm.com.motorola.dailyentry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonFunctions;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.database.MotorolaDatabase;
import cpm.com.motorola.moto.LoginActivity;
import cpm.com.motorola.moto.MainActivity;
import cpm.com.motorola.xmlgettersetter.CoverageBean;

public class StoreImageActivity extends AppCompatActivity implements
        View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    ImageView img_cam, img_clicked;
    FloatingActionButton btn_save;
    String _pathforcheck, _path, str;
    String store_cd, store_name, visit_date, username;
    private SharedPreferences preferences;
    AlertDialog alert;
    String img_str;
    private MotorolaDatabase database;
    double lat = 0.0, lon = 0.0;
    GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_LOCATION = 1;
    ProgressDialog loading;
    List<Address> addresses;
    String app_ver, complete_locality = "";
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_image);
        context = this;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        img_cam = (ImageView) findViewById(R.id.img_selfie);
        img_clicked = (ImageView) findViewById(R.id.img_cam_selfie);
        btn_save = (FloatingActionButton) findViewById(R.id.btn_save_selfie);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        store_cd = preferences.getString(CommonString.KEY_STORE_CD, null);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        username = preferences.getString(CommonString.KEY_USERNAME, null);
        store_name = preferences.getString(CommonString.KEY_STORE_NAME, null);
        app_ver = preferences.getString(CommonString.KEY_VERSION, "");
        getSupportActionBar().setTitle("Store image - " + visit_date);
        str = CommonString.FILE_PATH;
        database = new MotorolaDatabase(context);
        database.open();
        img_cam.setOnClickListener(this);
        img_clicked.setOnClickListener(this);
        btn_save.setOnClickListener(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        }
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(CommonString.ONBACK_ALERT_MESSAGE)
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                                    StoreImageActivity.this.finish();
                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert = builder.create();
            alert.show();

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(CommonString.ONBACK_ALERT_MESSAGE)
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                                StoreImageActivity.this.finish();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.img_cam_selfie:
                _pathforcheck = store_cd + "_STOREIMG_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
                _path = CommonString.FILE_PATH + _pathforcheck;
                CommonFunctions.startAnncaCameraActivity(context, _path);
                break;

            case R.id.btn_save_selfie:

                if (img_str != null) {
                    if (checkNetIsAvailable()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(getString(R.string.parinaam));
                        builder.setMessage(getString(R.string.alert_save))
                                .setCancelable(false)
                                .setPositiveButton("OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {

                                                try {
                                                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                                    CoverageBean cdata = new CoverageBean();
                                                    cdata.setStoreId(store_cd);
                                                    cdata.setVisitDate(visit_date);
                                                    cdata.setUserId(username);
                                                    cdata.setInTime(getCurrentTime());
                                                    cdata.setOutTime("");
                                                    cdata.setReason("");
                                                    cdata.setReasonid("0");
                                                    cdata.setLatitude(String.valueOf(lat));
                                                    cdata.setLongitude(String.valueOf(lon));
                                                    cdata.setStatus(CommonString.KEY_CHECK_IN);
                                                    cdata.setTraining_mode_cd(preferences.getString(CommonString.KEY_TRAINING_MODE_CD, ""));
                                                    cdata.setImage(img_str);
                                                    cdata.setRemark("");
                                                    new BackgroundTask(context, cdata).execute();
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        alert = builder.create();
                        alert.show();
                    } else {
                        Snackbar.make(btn_save, getString(R.string.nonetwork), Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    }
                } else {
                    Snackbar.make(btn_save, getString(R.string.store_selfie), Snackbar.LENGTH_LONG).show();
                }

                break;

        }

    }

    public boolean checkNetIsAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }


    @Override
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
                            String metadata = CommonFunctions.setMetadataAtImages(store_name, store_cd, "Store Image", username);
                            Bitmap bmp = CommonFunctions.addMetadataAndTimeStampToImage(context, _path, metadata, visit_date);
                            img_cam.setImageBitmap(bmp);
                            img_clicked.setVisibility(View.GONE);
                            img_cam.setVisibility(View.VISIBLE);
                            //Set Clicked image to Imageview
                            img_str = _pathforcheck;
                            _pathforcheck = "";
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case REQUEST_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_CANCELED: {
                        finish();
                    }
                    default: {
                        break;
                    }
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String getCurrentTime() {
        Calendar m_cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String cdate = formatter.format(m_cal.getTime());
        return cdate;
    }

    @Override
    public void onConnected(Bundle bundle) {
        try {
            Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                lat = mLastLocation.getLatitude();
                lon = mLastLocation.getLongitude();
                //get locality addresssss
                Geocoder geocoder;
                addresses = null;
                geocoder = new Geocoder(this, Locale.getDefault());
                addresses = geocoder.getFromLocation(lat, lon, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                complete_locality = addresses.get(0).getAddressLine(0); // If any additional address line present than only, c// heck with max available address lines by getMaxAddressLineIndex()
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    class BackgroundTask extends AsyncTask<Void, Void, String> {
        private Context context;
        private CoverageBean cdata;

        BackgroundTask(Context context, CoverageBean coverageBean) {
            this.cdata = coverageBean;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loading = ProgressDialog.show(context, "Processing", "Please wait...", false, false);
        }

        @Override
        protected String doInBackground(Void... params) {

            try {
                String onXML = "[DATA][USER_DATA][STORE_CD]"
                        + cdata.getStoreId()
                        + "[/STORE_CD]" + "[VISIT_DATE]"
                        + cdata.getVisitDate()
                        + "[/VISIT_DATE][LATITUDE]"
                        + cdata.getLatitude()
                        + "[/LATITUDE][APP_VERSION]"
                        + app_ver
                        + "[/APP_VERSION][LONGITUDE]"
                        + cdata.getLongitude()
                        + "[/LONGITUDE][IN_TIME]"
                        + cdata.getInTime()
                        + "[/IN_TIME][OUT_TIME]"
                        + ""
                        + "[/OUT_TIME][UPLOAD_STATUS]"
                        + CommonString.KEY_CHECK_IN
                        + "[/UPLOAD_STATUS][USER_ID]" + username
                        + "[/USER_ID][TMODE_CD]" + cdata.getTraining_mode_cd() +
                        "[/TMODE_CD][MANAGE]" + "0"+
                        "[/MANAGE][IMAGE_URL]" + cdata.getImage()
                        + "[/IMAGE_URL][REASON_ID]"
                        + "0"
                        + "[/REASON_ID][REASON_REMARK]"
                        + ""
                        + "[/REASON_REMARK][/USER_DATA][/DATA]";


                SoapObject request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_DR_STORE_COVERAGE);
                request.addProperty("onXML", onXML);
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;
                envelope.setOutputSoapObject(request);
                HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_DR_STORE_COVERAGE, envelope);
                Object result = (Object) envelope.getResponse();
                if (result.toString().contains(CommonString.KEY_SUCCESS)) {
                    database.open();
                    //Changessss
                    database.InsertCoverageData(cdata);
                    database.updateStoreStatusOnLeave(store_cd, visit_date, CommonString.KEY_CHECK_IN);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.commit();
                    return CommonString.KEY_SUCCESS;
                } else {
                    return CommonString.METHOD_UPLOAD_DR_STORE_COVERAGE;
                }

            } catch (MalformedURLException e) {
                loading.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_EXCEPTION);
                    }
                });

            } catch (IOException e) {
                loading.dismiss();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_SOCKETEXCEPTION);
                    }
                });
            } catch (Exception e) {
                loading.dismiss();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_EXCEPTION);
                    }
                });
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            loading.dismiss();
            if (result.equals(CommonString.KEY_SUCCESS)) {
                Intent in = new Intent(getApplicationContext(), StoreIsdActivity.class);
                startActivity(in);
                overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                StoreImageActivity.this.finish();
            } else {
            }
        }

    }

    public void showMessage(String msg) {
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.parinaam))
                .setMessage(msg).setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();


    }


}