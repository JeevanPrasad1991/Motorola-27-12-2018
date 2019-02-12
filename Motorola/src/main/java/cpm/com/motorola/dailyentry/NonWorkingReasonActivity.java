package cpm.com.motorola.dailyentry;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonFunctions;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.database.MotorolaDatabase;
import cpm.com.motorola.upload.UploadActivity;
import cpm.com.motorola.xmlgettersetter.CoverageBean;
import cpm.com.motorola.xmlgettersetter.JCPGetterSetter;
import cpm.com.motorola.xmlgettersetter.NonWorkingReasonGetterSetter;

public class NonWorkingReasonActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    ArrayList<NonWorkingReasonGetterSetter> reasondata = new ArrayList();
    private Spinner reasonspinner;
    private SharedPreferences preferences;
    String _UserId, visit_date, store_id, training_mode_cd;
    protected boolean status = true;
    EditText text;
    AlertDialog alert;
    ImageButton camera;
    RelativeLayout reason_lay, rel_cam;
    MotorolaDatabase database;
    String str;
    protected String _pathforcheck = "", _path = "";
    private String image1 = "";
    String reasonname, reasonid, entry_allow, image, entry, reason_reamrk, intime;
    private ArrayAdapter<CharSequence> reason_adapter;
    ArrayList<JCPGetterSetter> jcp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_non_working_reason);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        reasonspinner = (Spinner) findViewById(R.id.spinner2);
        camera = (ImageButton) findViewById(R.id.imgcam);
        text = (EditText) findViewById(R.id.reasontxt);
        reason_lay = (RelativeLayout) findViewById(R.id.layout_reason);
        rel_cam = (RelativeLayout) findViewById(R.id.relimgcam);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        _UserId = preferences.getString(CommonString.KEY_USERNAME, "");
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        store_id = getIntent().getStringExtra(CommonString.KEY_STORE_CD);
        training_mode_cd = getIntent().getStringExtra(CommonString.KEY_TRAINING_MODE_CD);
        setTitle("Non Working - " + visit_date);
        database = new MotorolaDatabase(this);
        database.open();
        str = CommonString.FILE_PATH;

        if (database.ischeckedStatus(visit_date)) {
            reasondata = database.getNonWorkingData(true);
        } else {
            reasondata = database.getNonWorkingData(false);
        }
        intime = getCurrentTime();
        camera.setOnClickListener(this);
        reason_adapter = new ArrayAdapter<>(this, R.layout.spinner_custom_item);
        reason_adapter.add("-Select Reason-");
        for (int i = 0; i < reasondata.size(); i++) {
            reason_adapter.add(reasondata.get(i).getReason().get(0));
        }

        reasonspinner.setAdapter(reason_adapter);
        reason_adapter.setDropDownViewResource(R.layout.spinner_custom_item);
        reasonspinner.setOnItemSelectedListener(this);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validatedata()) {
                    if (imageAllowed()) {
                        if (textAllowed()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(NonWorkingReasonActivity.this).setTitle(getString(R.string.parinaam));
                            builder.setMessage(getString(R.string.alert_save))
                                    .setCancelable(false)
                                    .setPositiveButton("OK",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int id) {
                                                    alert.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                                    if (entry_allow.equals("0")) {
                                                        database.deleteAllTables();
                                                        jcp = database.getStoreData(visit_date);
                                                        for (int i = 0; i < jcp.size(); i++) {
                                                            CoverageBean cdata = new CoverageBean();
                                                            cdata.setStoreId(jcp.get(i).getSTORE_CD().get(0));
                                                            cdata.setVisitDate(visit_date);
                                                            cdata.setUserId(_UserId);
                                                            cdata.setInTime(intime);
                                                            cdata.setOutTime(getCurrentTime());
                                                            cdata.setReason(reasonname);
                                                            cdata.setReasonid(reasonid);
                                                            cdata.setLatitude("0.0");
                                                            cdata.setLongitude("0.0");
                                                            cdata.setImage(image1);
                                                            cdata.setRemark(text.getText().toString().replaceAll("[(!@#$%^&*?)\"]", ""));
                                                            cdata.setStatus(CommonString.STORE_STATUS_LEAVE);
                                                            cdata.setTraining_mode_cd(training_mode_cd);
                                                            database.InsertCoverageData(cdata);
                                                            database.updateStoreStatusOnLeave(store_id, visit_date, CommonString.STORE_STATUS_LEAVE);
                                                            SharedPreferences.Editor editor = preferences.edit();
                                                            editor.putString(CommonString.KEY_STORE_CD, "");
                                                        }

                                                    } else {
                                                        CoverageBean cdata = new CoverageBean();
                                                        cdata.setStoreId(store_id);
                                                        cdata.setVisitDate(visit_date);
                                                        cdata.setUserId(_UserId);
                                                        cdata.setInTime(intime);
                                                        cdata.setOutTime(getCurrentTime());
                                                        cdata.setReason(reasonname);
                                                        cdata.setReasonid(reasonid);
                                                        cdata.setLatitude("0.0");
                                                        cdata.setLongitude("0.0");
                                                        cdata.setImage(image1);
                                                        cdata.setRemark(text.getText().toString().replaceAll("[(!@#$%^&*?)\"]", ""));
                                                        cdata.setStatus(CommonString.STORE_STATUS_LEAVE);
                                                        cdata.setTraining_mode_cd("0");
                                                        database.InsertCoverageData(cdata);
                                                        database.updateStoreStatusOnLeave(store_id, visit_date, CommonString.STORE_STATUS_LEAVE);
                                                        SharedPreferences.Editor editor = preferences.edit();
                                                        editor.putString(CommonString.KEY_STORE_CD, "");
                                                    }

                                                    startActivity(new Intent(NonWorkingReasonActivity.this, UploadActivity.class));
                                                    overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                                                    NonWorkingReasonActivity.this.finish();
                                                }
                                            })
                                    .setNegativeButton("Cancel",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(
                                                        DialogInterface dialog,
                                                        int id) {
                                                    dialog.cancel();
                                                }
                                            });

                            alert = builder.create();
                            alert.show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Please enter required remark reason", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Please Capture Image", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please Select a Reason", Toast.LENGTH_SHORT).show();

                }
            }
        });

    }

    public String getCurrentTime() {
        Calendar m_cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String cdate = formatter.format(m_cal.getTime());
        return cdate;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // NavUtils.navigateUpFromSameTask(this);
            finish();
            overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.imgcam) {
            _pathforcheck = store_id + "_STORENONW_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
            _path = CommonString.FILE_PATH + _pathforcheck;
            CommonFunctions.startAnncaCameraActivity(NonWorkingReasonActivity.this, _path);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.spinner2:
                if (position != 0) {
                    reasonname = reasondata.get(position - 1).getReason().get(0);
                    reasonid = reasondata.get(position - 1).getReason_cd().get(0);
                    entry_allow = reasondata.get(position - 1).getEntry_allow().get(0);
                    if (reasonname.equalsIgnoreCase("Store Closed")) {
                        rel_cam.setVisibility(View.VISIBLE);
                        image = "true";
                    } else {
                        rel_cam.setVisibility(View.GONE);
                        image = "false";
                    }
                    reason_reamrk = "true";
                    if (reason_reamrk.equalsIgnoreCase("true")) {
                        reason_lay.setVisibility(View.VISIBLE);
                    } else {
                        reason_lay.setVisibility(View.GONE);
                    }
                } else {
                    reasonname = "";
                    reasonid = "";
                }
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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
                            String metadata = CommonFunctions.setMetadataAtImages(preferences.getString(CommonString.KEY_STORE_NAME, ""), store_id, "Store Nonwork", _UserId);
                            CommonFunctions.addMetadataAndTimeStampToImage(NonWorkingReasonActivity.this, _path, metadata, visit_date);
                            camera.setBackground(getResources().getDrawable(R.drawable.camera_icon_done));
                            image1 = _pathforcheck;
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

    public boolean imageAllowed() {
        boolean result = true;
        if (image.equalsIgnoreCase("true")) {
            if (image1.equals("")) {
                result = false;
            }
        }

        return result;

    }

    public boolean textAllowed() {
        boolean result = true;
        if (text.getText().toString().trim().equals("")) {
            result = false;
        }

        return result;
    }

    @Override
    public void onBackPressed() {
        // TODO Auto-generated method stub
        finish();

        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }

    public boolean validatedata() {
        boolean result = false;
        if (reasonid != null && !reasonid.equals("")) {
            result = true;
        }
        return result;

    }

}


