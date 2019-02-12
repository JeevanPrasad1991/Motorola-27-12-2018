package cpm.com.motorola.dailyentry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonFunctions;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.database.MotorolaDatabase;
import cpm.com.motorola.upload.UploadActivity;
import cpm.com.motorola.xmlgettersetter.AuditChecklistGetterSetter;
import cpm.com.motorola.xmlgettersetter.CoverageBean;
import cpm.com.motorola.xmlgettersetter.QuizAnwserGetterSetter;


/**
 * Created by jeevap on 01-04-2019.
 */
public class CheckOutStoreActivity extends AppCompatActivity implements View.OnClickListener {
    private SharedPreferences preferences = null;
    private String username, visit_date, store_cd;
    private MotorolaDatabase db;
    FloatingActionButton btn_save_selfie_checkout;
    EditText edt_remark;
    ProgressDialog loading;
    private boolean data = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout_activty);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        btn_save_selfie_checkout = (FloatingActionButton) findViewById(R.id.btn_save_selfie_checkout);
        edt_remark = (EditText) findViewById(R.id.edt_remark);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        username = preferences.getString(CommonString.KEY_USERNAME, "");
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        setTitle("Checkout - " + visit_date);
        db = new MotorolaDatabase(this);
        db.open();
        store_cd = getIntent().getStringExtra(CommonString.KEY_STORE_CD);

        btn_save_selfie_checkout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_save_selfie_checkout:
                if (CommonFunctions.CheckNetAvailability(this)) {
                    if (!edt_remark.getText().toString().isEmpty()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(CheckOutStoreActivity.this).setTitle(getString(R.string.parinaam))
                                .setMessage(getString(R.string.alert_save)).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        try {
                                            new BackgroundTask(CheckOutStoreActivity.this).execute();
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        builder.show();
                    } else {
                        Snackbar.make(btn_save_selfie_checkout, "Please fill remark", Snackbar.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    private class BackgroundTask extends AsyncTask<Void, String, String> {
        private Context context;

        BackgroundTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            loading = ProgressDialog.show(context, "Processing", "Please wait...", false, false);
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub

            try {

                String onXML = "[STORE_CHECK_OUT_STATUS][USER_ID]"
                        + username
                        + "[/USER_ID]" + "[STORE_ID]"
                        + store_cd
                        + "[/STORE_ID][LATITUDE]"
                        + db.getCoverageSpecificData(store_cd, visit_date).get(0).getLatitude()
                        + "[/LATITUDE][LOGITUDE]"
                        + db.getCoverageSpecificData(store_cd, visit_date).get(0).getLongitude()
                        + "[/LOGITUDE][CHECKOUT_DATE]"
                        + visit_date
                        + "[/CHECKOUT_DATE][CHECK_OUTTIME]"
                        + getCurrentTime()
                        + "[/CHECK_OUTTIME][CHECK_INTIME]"
                        + db.getCoverageSpecificData(store_cd, visit_date).get(0).getInTime()
                        + "[/CHECK_INTIME][CREATED_BY]"
                        + username
                        + "[/CREATED_BY][/STORE_CHECK_OUT_STATUS]";


                final String sos_xml = "[DATA]" + onXML + "[/DATA]";

                SoapObject request = new SoapObject(CommonString.NAMESPACE, "Upload_Store_ChecOut_Status");
                request.addProperty("onXML", sos_xml);
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;
                envelope.setOutputSoapObject(request);
                HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
                androidHttpTransport.call(CommonString.SOAP_ACTION + "Upload_Store_ChecOut_Status", envelope);
                Object result = (Object) envelope.getResponse();
                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                    return "Upload_Store_ChecOut_Status";
                }
                if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                    return "Upload_Store_ChecOut_Status";
                }
                if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                    return "Upload_Store_ChecOut_Status";
                }

                if (result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                    db.open();
                    db.updateCoverageStoreOutTime(store_cd, visit_date, getCurrentTime(), CommonString.KEY_C);
                    db.updateStoreStatusOnCheckout(store_cd, visit_date, CommonString.KEY_C);
                    db.insertremarkdata(edt_remark.getText().toString().trim().replaceAll("[(!@#$%^&*?)\"]", ""), store_cd, visit_date);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(CommonString.KEY_STORE_CD, "");
                    editor.putString(CommonString.KEY_MANAGED, "");
                    editor.putString(CommonString.KEY_TRAINING_MODE_CD, "");
                    editor.commit();
                } else {
                    if (result.toString().equalsIgnoreCase(CommonString.KEY_FALSE)) {
                        return "Upload_Store_ChecOut_Status";
                    }
                }
                return CommonString.KEY_SUCCESS;

            } catch (MalformedURLException e) {
                loading.dismiss();
                data = false;
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_EXCEPTION);
                    }
                });

            } catch (IOException e) {
                data = false;
                loading.dismiss();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        showMessage(CommonString.MESSAGE_SOCKETEXCEPTION);
                    }
                });
            } catch (Exception e) {
                data = false;
                loading.dismiss();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_EXCEPTION);
                    }
                });

            }

            return "";
        }


        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            loading.dismiss();
            if (data) {
                if (!result.equals("") && result.equals(CommonString.KEY_SUCCESS)) {
                    Toast.makeText(getApplicationContext(), CommonString.MESSAGE_CHECKOUT, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(CheckOutStoreActivity.this, UploadActivity.class));
                    CheckOutStoreActivity.this.finish();
                } else if (!result.equals("")) {
                    Toast.makeText(getApplicationContext(), "Network Error Try Again", Toast.LENGTH_SHORT).show();
                }
            }

        }

    }

    public String getCurrentTime() {
        Calendar m_cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String cdate = formatter.format(m_cal.getTime());
        return cdate;
    }

    public void showMessage(String msg) {
        new AlertDialog.Builder(CheckOutStoreActivity.this)
                .setTitle("Alert Dialog")
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(R.drawable.parinaam_logo_ico)
                .show();


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CheckOutStoreActivity.this);
            builder.setMessage(CommonString.ONBACK_ALERT_MESSAGE)
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {
                                    overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                                    CheckOutStoreActivity.this.finish();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(CheckOutStoreActivity.this);
        builder.setMessage(CommonString.ONBACK_ALERT_MESSAGE)
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                                CheckOutStoreActivity.this.finish();
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


}
