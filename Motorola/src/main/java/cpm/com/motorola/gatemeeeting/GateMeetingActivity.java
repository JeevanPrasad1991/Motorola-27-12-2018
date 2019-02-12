package cpm.com.motorola.gatemeeeting;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonFunctions;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.dailyentry.StoreImageActivity;
import cpm.com.motorola.dailyentry.StoreIsdActivity;
import cpm.com.motorola.database.MotorolaDatabase;
import cpm.com.motorola.geocode.ImageUploadActivity;
import cpm.com.motorola.moto.LoginActivity;
import cpm.com.motorola.xmlgettersetter.CoverageBean;
import cpm.com.motorola.xmlgettersetter.GateMettingGetterSetter;

/**
 * Created by jeevanp on 01-03-2017.
 */

public class GateMeetingActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    String cam_img_str1 = "", cam_img_str2 = "", cam_img_str3 = "", cam_img_str4 = "", _pathforcheck, _path, str, visit_date, username, gatemeetingfoldername = "GateMeetingImage";
    EditText gatemeeting_editlocation, gatemeeting_editremark;
    GateMettingGetterSetter gatemeetingManagerGetterSetter;
    ImageView img_cam1, img_cam2, img_cam3, img_cam4;
    FloatingActionButton savegate_meeting_fab;
    GoogleApiClient mGoogleApiClient;
    private SharedPreferences preferences;
    TextView gatemeeting_current_date, txt_uploadmsg;
    LinearLayout gatemeeting_rlall;
    boolean up_success_flag = true;
    double lat = 0.0, lon = 0.0;
    ProgressDialog loading;
    MotorolaDatabase db;
    AlertDialog alert;
    Context context;
    int img_cam_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gate_meeting);
        context = this;
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        username = preferences.getString(CommonString.KEY_USERNAME, null);
        db = new MotorolaDatabase(getApplicationContext());
        db.open();
        str = CommonString.FILE_PATH;
        gatemeeting_editremark = (EditText) findViewById(R.id.gatemeeting_editremark);
        gatemeeting_current_date = (TextView) findViewById(R.id.gatemeeting_current_date);
        txt_uploadmsg = (TextView) findViewById(R.id.txt_uploadmsg);
        gatemeeting_editlocation = (EditText) findViewById(R.id.gatemeeting_editlocation);
        savegate_meeting_fab = (FloatingActionButton) findViewById(R.id.savegate_meeting_fab);
        gatemeeting_rlall = (LinearLayout) findViewById(R.id.gatemeeting_rlall);
        img_cam1 = (ImageView) findViewById(R.id.img_cam_group1);
        img_cam2 = (ImageView) findViewById(R.id.img_cam_group2);
        img_cam3 = (ImageView) findViewById(R.id.img_cam_group3);
        img_cam4 = (ImageView) findViewById(R.id.img_cam_group4);

        savegate_meeting_fab.setOnClickListener(this);
        img_cam1.setOnClickListener(this);
        img_cam2.setOnClickListener(this);
        img_cam3.setOnClickListener(this);
        img_cam4.setOnClickListener(this);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        }
        if (Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        setTitle("Gate Meeting - " + visit_date);
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
                                    GateMeetingActivity.this.finish();
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
                                GateMeetingActivity.this.finish();
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
            case R.id.img_cam_group1:
                img_cam_id = R.id.img_cam_group1;
                _pathforcheck = "gatemeetingImageOne_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
                _path = CommonString.FILE_PATH + _pathforcheck;
                CommonFunctions.startAnncaCameraActivity(context, _path);
                break;

            case R.id.img_cam_group2:
                img_cam_id = R.id.img_cam_group2;
                _pathforcheck = "gatemeetingImageTwo_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
                _path = CommonString.FILE_PATH + _pathforcheck;
                CommonFunctions.startAnncaCameraActivity(context, _path);
                break;
            case R.id.img_cam_group3:
                img_cam_id = R.id.img_cam_group3;
                _pathforcheck = "gatemeetingImageThree_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
                _path = CommonString.FILE_PATH + _pathforcheck;
                CommonFunctions.startAnncaCameraActivity(context, _path);
                break;
            case R.id.img_cam_group4:
                img_cam_id = R.id.img_cam_group4;
                _pathforcheck = "gatemeetingImageFour_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
                _path = CommonString.FILE_PATH + _pathforcheck;
                CommonFunctions.startAnncaCameraActivity(context, _path);
                break;

            case R.id.savegate_meeting_fab:
                if (gatemeeting_editlocation.getText().toString().isEmpty()) {
                    Snackbar.make(savegate_meeting_fab, "Please Enter Location.", Snackbar.LENGTH_SHORT).show();
                } else if (gatemeeting_editremark.getText().toString().isEmpty()) {
                    Snackbar.make(savegate_meeting_fab, "Please Enter Remark.", Snackbar.LENGTH_SHORT).show();
                } else if (cam_img_str1.equals("") || cam_img_str2.equals("") || cam_img_str3.equals("") || cam_img_str4.equals("")) {
                    Snackbar.make(savegate_meeting_fab, "Please capture all the Images.", Snackbar.LENGTH_SHORT).show();
                } else {
                    if (!checkNetIsAvailable()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(R.string.parinaam).setMessage(R.string.alert_save).setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                        GateMettingGetterSetter gateGObject = new GateMettingGetterSetter();
                                        gateGObject.setVisit_date(visit_date);
                                        gateGObject.setLocation_text(gatemeeting_editlocation.getText().toString().trim().replaceAll("[(!@#$%^&*?)\"]", ""));
                                        gateGObject.setRemark(gatemeeting_editremark.getText().toString().trim().replaceAll("[(!@#$%^&*?)\"]", ""));
                                        gateGObject.setTeam_picure1(cam_img_str1);
                                        gateGObject.setTeam_picure2(cam_img_str2);
                                        gateGObject.setTeam_picure3(cam_img_str3);
                                        gateGObject.setTeam_picure4(cam_img_str4);
                                        gateGObject.setLatitude(String.valueOf(lat));
                                        gateGObject.setLongitue(String.valueOf(lon));
                                        db.insertGatemeetingManagerData(gateGObject);
                                        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                                        GateMeetingActivity.this.finish();
                                    }
                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                        alert = builder.create();
                        alert.show();
                    } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context).setTitle(R.string.parinaam).setMessage(R.string.alert_savewithupload).setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                        GateMettingGetterSetter gateGObject = new GateMettingGetterSetter();
                                        gateGObject.setVisit_date(visit_date);
                                        gateGObject.setLocation_text(gatemeeting_editlocation.getText().toString().trim().replaceAll("[(!@#$%^&*?)\"]", ""));
                                        gateGObject.setRemark(gatemeeting_editremark.getText().toString().trim().replaceAll("[(!@#$%^&*?)\"]", ""));
                                        gateGObject.setTeam_picure1(cam_img_str1);
                                        gateGObject.setTeam_picure2(cam_img_str2);
                                        gateGObject.setTeam_picure3(cam_img_str3);
                                        gateGObject.setTeam_picure4(cam_img_str4);
                                        gateGObject.setLatitude(String.valueOf(lat));
                                        gateGObject.setLongitue(String.valueOf(lon));
                                        db.insertGatemeetingManagerData(gateGObject);
                                        new BackgroundTask(context, gateGObject).execute();
                                    }
                                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                        alert = builder.create();
                        alert.show();
                    }

                }


                break;


        }

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
                            switch (img_cam_id) {
                                case R.id.img_cam_group1:
                                    String metadata = CommonFunctions.setmetadataforattendance("Gate Meeting One Image", username);
                                    CommonFunctions.addMetadataAndTimeStampToImage(context, _path, metadata, visit_date);
                                    img_cam1.setImageResource(R.drawable.camera_icon_done);
                                    cam_img_str1 = _pathforcheck;
                                    break;
                                case R.id.img_cam_group2:
                                    metadata = CommonFunctions.setmetadataforattendance("Gate Meeting Two Image", username);
                                    CommonFunctions.addMetadataAndTimeStampToImage(context, _path, metadata, visit_date);
                                    img_cam2.setImageResource(R.drawable.camera_icon_done);
                                    cam_img_str2 = _pathforcheck;
                                    break;
                                case R.id.img_cam_group3:
                                    metadata = CommonFunctions.setmetadataforattendance("Gate Meeting Three Image", username);
                                    CommonFunctions.addMetadataAndTimeStampToImage(context, _path, metadata, visit_date);
                                    img_cam3.setImageResource(R.drawable.camera_icon_done);
                                    cam_img_str3 = _pathforcheck;
                                    break;

                                case R.id.img_cam_group4:
                                    metadata = CommonFunctions.setmetadataforattendance("Gate Meeting Four Image", username);
                                    CommonFunctions.addMetadataAndTimeStampToImage(context, _path, metadata, visit_date);
                                    img_cam4.setImageResource(R.drawable.camera_icon_done);
                                    cam_img_str4 = _pathforcheck;
                                    break;
                            }

                            _pathforcheck = "";

                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setData();
    }


    public String getCurrentTime() {
        Calendar m_cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String cdate = formatter.format(m_cal.getTime());
        return cdate;

    }

    public void setData() {
        gatemeetingManagerGetterSetter = db.getGatemeetingManagerData(visit_date,"");
        if (!gatemeetingManagerGetterSetter.getVisit_date().equals("")) {
            cam_img_str1 = gatemeetingManagerGetterSetter.getTeam_picure1();
            cam_img_str2 = gatemeetingManagerGetterSetter.getTeam_picure2();
            cam_img_str3 = gatemeetingManagerGetterSetter.getTeam_picure3();
            cam_img_str4 = gatemeetingManagerGetterSetter.getTeam_picure4();
            img_cam1.setImageResource(R.drawable.camera_icon_done);
            img_cam2.setImageResource(R.drawable.camera_icon_done);
            img_cam3.setImageResource(R.drawable.camera_icon_done);
            img_cam4.setImageResource(R.drawable.camera_icon_done);
            gatemeeting_editremark.setText(gatemeetingManagerGetterSetter.getRemark());
            gatemeeting_editlocation.setText(gatemeetingManagerGetterSetter.getLocation_text());
            gatemeeting_current_date.setText("Current date - " + gatemeetingManagerGetterSetter.getVisit_date());
            if (gatemeetingManagerGetterSetter.getStatus().equalsIgnoreCase(CommonString.KEY_Y)) {
                txt_uploadmsg.setVisibility(View.VISIBLE);
                savegate_meeting_fab.setVisibility(View.GONE);
                img_cam1.setEnabled(false);
                img_cam2.setEnabled(false);
                img_cam3.setEnabled(false);
                img_cam4.setEnabled(false);
                gatemeeting_editremark.setEnabled(false);
                gatemeeting_editlocation.setEnabled(false);
            } else {
                txt_uploadmsg.setVisibility(View.GONE);
                savegate_meeting_fab.setVisibility(View.VISIBLE);
                img_cam1.setEnabled(true);
                img_cam2.setEnabled(true);
                img_cam3.setEnabled(true);
                img_cam4.setEnabled(true);
                gatemeeting_editremark.setEnabled(true);
                gatemeeting_editlocation.setEnabled(true);
            }
        } else {
            gatemeeting_current_date.setText("Current date - " + visit_date);
            savegate_meeting_fab.setVisibility(View.VISIBLE);
            txt_uploadmsg.setVisibility(View.GONE);
            img_cam1.setEnabled(true);
            img_cam2.setEnabled(true);
            img_cam3.setEnabled(true);
            img_cam4.setEnabled(true);
            gatemeeting_editremark.setEnabled(true);
            gatemeeting_editlocation.setEnabled(true);
        }
    }

    public boolean checkNetIsAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
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
                geocoder = new Geocoder(this, Locale.getDefault());
                geocoder.getFromLocation(lat, lon, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                //complete_locality = addresses.get(0).getAddressLine(0); // If any additional address line present than only, c// heck with max available address lines by getMaxAddressLineIndex()
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
        private GateMettingGetterSetter gatemeetingManagerGetterSetter;

        BackgroundTask(Context context, GateMettingGetterSetter gatemeetingManagerGetterSetter) {
            this.gatemeetingManagerGetterSetter = gatemeetingManagerGetterSetter;
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

                String final_xml = "", onXML = "";
                onXML = "[GATE_MEETING_DATA]"
                        + "[CREATED_BY]"
                        + username
                        + "[/CREATED_BY]"
                        + "[VISIT_DATE]"
                        + gatemeetingManagerGetterSetter.getVisit_date()
                        + "[/VISIT_DATE]"
                        + "[LOCATION]"
                        + gatemeetingManagerGetterSetter.getLocation_text()
                        + "[/LOCATION]"
                        + "[REMARK]"
                        + gatemeetingManagerGetterSetter.getRemark()
                        + "[/REMARK]"

                        + "[TEAM_PICTURE_ONE]"
                        + gatemeetingManagerGetterSetter.getTeam_picure1()
                        + "[/TEAM_PICTURE_ONE]"
                        + "[TEAM_PICTURE_TWO]"
                        + gatemeetingManagerGetterSetter.getTeam_picure2()
                        + "[/TEAM_PICTURE_TWO]"
                        + "[TEAM_PICTURE_THREE]"
                        + gatemeetingManagerGetterSetter.getTeam_picure3()
                        + "[/TEAM_PICTURE_THREE]"
                        + "[TEAM_PICTURE_FOUR]"
                        + gatemeetingManagerGetterSetter.getTeam_picure4()
                        + "[/TEAM_PICTURE_FOUR]"

                        + "[LATITUDE]"
                        + gatemeetingManagerGetterSetter.getLatitude()
                        + "[/LATITUDE]"
                        + "[LONGITUDE]"
                        + gatemeetingManagerGetterSetter.getLongitue()
                        + "[/LONGITUDE]"
                        + "[/GATE_MEETING_DATA]";

                final_xml = final_xml + onXML;

                final String employee_xml = "[DATA]" + final_xml + "[/DATA]";
                SoapObject request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                request.addProperty("XMLDATA", employee_xml);
                request.addProperty("KEYS", "GATE_MEETING_DATA");
                request.addProperty("USERNAME", username);
                request.addProperty("MID", "0");
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;
                envelope.setOutputSoapObject(request);
                HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                Object result = (Object) envelope.getResponse();
                if (result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {

                    if (!gatemeetingManagerGetterSetter.getTeam_picure1().equals("")) {
                        ImageUploadActivity.UploadGeoTaggingImage(gatemeetingManagerGetterSetter.getTeam_picure1(), gatemeetingfoldername);
                    }
                    if (!gatemeetingManagerGetterSetter.getTeam_picure2().equals("")) {
                        ImageUploadActivity.UploadGeoTaggingImage(gatemeetingManagerGetterSetter.getTeam_picure2(), gatemeetingfoldername);
                    }
                    if (!gatemeetingManagerGetterSetter.getTeam_picure3().equals("")) {
                        ImageUploadActivity.UploadGeoTaggingImage(gatemeetingManagerGetterSetter.getTeam_picure3(), gatemeetingfoldername);
                    }
                    if (!gatemeetingManagerGetterSetter.getTeam_picure4().equals("")) {
                        ImageUploadActivity.UploadGeoTaggingImage(gatemeetingManagerGetterSetter.getTeam_picure4(), gatemeetingfoldername);
                    }

                } else {
                    return "Error in uploading GateMeeting Data. Please Upload again.";
                }
            } catch (MalformedURLException e) {
                loading.dismiss();
                up_success_flag = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_EXCEPTION);
                    }
                });

            } catch (IOException e) {
                up_success_flag = false;
                loading.dismiss();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_SOCKETEXCEPTION);
                    }
                });
            } catch (Exception e) {
                up_success_flag = false;
                loading.dismiss();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_EXCEPTION);
                    }
                });
            }

            if (up_success_flag) {
                return CommonString.KEY_SUCCESS;
            } else {
                return CommonString.KEY_FAILURE;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            loading.dismiss();
            if (up_success_flag) {
                if (result.equals(CommonString.KEY_SUCCESS)) {
                    db.open();
                    db.updategatemeeting(visit_date, CommonString.KEY_Y);
                    overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                    GateMeetingActivity.this.finish();
                } else {
                    showMessage("Error in uploading GateMeeting Data. Please Upload again.");
                }
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
