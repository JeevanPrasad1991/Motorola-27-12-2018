package cpm.com.motorola.dailyentry;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonFunctions;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.database.MotorolaDatabase;
import cpm.com.motorola.download.CompleteDownloadActivity;
import cpm.com.motorola.geocode.LocationActivity;
import cpm.com.motorola.moto.LoginActivity;
import cpm.com.motorola.upload.CheckoutNUpload;
import cpm.com.motorola.xmlgettersetter.CoverageBean;
import cpm.com.motorola.xmlgettersetter.JCPGetterSetter;

public class StoreListActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    MotorolaDatabase db;
    ArrayList<JCPGetterSetter> storedataList = new ArrayList<>();
    ArrayList<CoverageBean> coverageList = new ArrayList<>();
    MyItemRecyclerViewAdapter myItemRecyclerViewAdapter;
    RecyclerView rec_store_data;
    LinearLayout linearLayout;
    private SharedPreferences preferences = null;
    private String user_name, user_type, visit_date;
    private SharedPreferences.Editor editor = null;
    FloatingActionButton fab;
    Dialog dialog;
    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 500; // 5 sec
    private static int FATEST_INTERVAL = 100; // 1 sec
    private static int DISPLACEMENT = 5; // 10 meters
    Location mLastLocation;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    // LogCat tag
    private static final String TAG = StoreListActivity.class.getSimpleName();
    GoogleApiClient mGoogleApiClient;
    Double lat = 0.0, lon = 0.0;
    private LocationRequest mLocationRequest;
    private LocationManager locmanager = null;
    String training_mode_cd, trainning_mode = "";
    //private Data data;
    private GoogleApiClient googleApiClient;
    private static final int REQUEST_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rec_store_data = (RecyclerView) findViewById(R.id.rec_store_data);
        linearLayout = (LinearLayout) findViewById(R.id.no_data_lay);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        user_name = preferences.getString(CommonString.KEY_USERNAME, null);
        user_type = preferences.getString(CommonString.KEY_USER_TYPE, null);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        setTitle("Store List - " + visit_date);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Download data
                if (checkNetIsAvailable()){
                    if (db.isCoverageDataFilled(visit_date)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(StoreListActivity.this);
                        builder.setTitle("Parinaam");
                        builder.setMessage("Please Upload Previous Data First")
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Intent startUpload = new Intent(StoreListActivity.this, CheckoutNUpload.class);
                                        startActivity(startUpload);
                                        finish();

                                    }
                                });

                        AlertDialog alert = builder.create();
                        alert.show();

                    } else {
                        try {
                            db.open();
                            db.deletePreviousUploadedData(visit_date);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Intent startDownload = new Intent(getApplicationContext(), CompleteDownloadActivity.class);
                        startActivity(startDownload);
                    }
                }else {
                    Snackbar.make(rec_store_data, CommonString.NO_INTERNET_CONNECTION, Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        locmanager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // First we need to check availability of play services
        checkgpsEnableDevice();
        // First we need to check availability of play services
        if (checkPlayServices()) {
            // Building the GoogleApi client
            //buildGoogleApiClient();
            createLocationRequest();
        }

        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(getApplicationContext(),
                        android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Date date = new Date();
        if (!dateFormat.format(date).equalsIgnoreCase(visit_date)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(StoreListActivity.this).setTitle("Alert Dialog");
            builder.setMessage("Your Device date does not match login Date. You will be logged out now");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(StoreListActivity.this, LoginActivity.class));
                    overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                    StoreListActivity.this.finish();
                    dialog.dismiss();
                }
            });
            builder.show();
        }
        checkPlayServices();
        db = new MotorolaDatabase(getApplicationContext());
        db.open();
        storedataList = db.getStoreData(visit_date);
        coverageList = db.getCoverageData(visit_date);
        if (storedataList.size() > 0) {
            rec_store_data.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
            myItemRecyclerViewAdapter = new MyItemRecyclerViewAdapter(storedataList);
            rec_store_data.setAdapter(myItemRecyclerViewAdapter);
            rec_store_data.setVisibility(View.VISIBLE);
            linearLayout.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
        }
    }

    public class MyItemRecyclerViewAdapter extends RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder> {
        private final List<JCPGetterSetter> mValues;

        public MyItemRecyclerViewAdapter(List<JCPGetterSetter> items) {
            mValues = items;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.store_item_layout, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.mItem = mValues.get(position);
            holder.tv_store_name.setText(mValues.get(position).getSTORENAME().get(0) + " ( Id : " + holder.mItem.getSTORE_CD().get(0) + " )");
            holder.tv_city.setText(mValues.get(position).getCITY().get(0));
            holder.tv_store_type.setText(mValues.get(position).getSTORETYPE().get(0));
            final String upload_status = storedataList.get(position).getUPLOAD_STATUS().get(0);
            final String checkout_status = storedataList.get(position).getCHECKOUT_STATUS().get(0);

            ArrayList<CoverageBean> coverage_data = db.getCoverageSpecificData(storedataList.get(position).getSTORE_CD().get(0), storedataList.get(position).getVISIT_DATE().get(0));

            if (upload_status.equals(CommonString.KEY_U)) {
                holder.img.setBackgroundResource(R.drawable.tick_u);
                holder.img.setVisibility(View.VISIBLE);
                holder.btn_checkout.setVisibility(View.GONE);
            } else if (checkout_status.equals(CommonString.KEY_C)) {
                holder.img.setBackgroundResource(R.drawable.tick_c);
                holder.img.setVisibility(View.VISIBLE);
                holder.btn_checkout.setVisibility(View.GONE);
            } else if (coverage_data.size() > 0) {
                if (coverage_data.get(0).getStatus().equals(CommonString.STORE_STATUS_LEAVE)) {
                    holder.img.setBackgroundResource(R.drawable.leave_tick);
                    holder.img.setVisibility(View.VISIBLE);
                    holder.btn_checkout.setVisibility(View.GONE);
                } else if (coverage_data.get(0).getStatus().equals(CommonString.KEY_VALID)) {
                    holder.btn_checkout.setVisibility(View.VISIBLE);
                    holder.img.setVisibility(View.INVISIBLE);
                } else if (coverage_data.get(0).getStatus().equals(CommonString.KEY_CHECK_IN)) {
                    holder.btn_checkout.setVisibility(View.GONE);
                    holder.img.setVisibility(View.VISIBLE);
                    holder.img.setBackgroundResource(R.drawable.checkin_ico);
                } else {
                    holder.img.setVisibility(View.INVISIBLE);
                }
            } else {
                holder.img.setVisibility(View.INVISIBLE);
            }

            holder.parentLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ArrayList<CoverageBean> coverage_data = db.getCoverageData(visit_date);
                        final String store_cdss = storedataList.get(position).getSTORE_CD().get(0);
                        final double storelat = Double.valueOf(storedataList.get(position).getLAT().get(0));
                        final double storelongt = Double.valueOf(storedataList.get(position).getLONG().get(0));
                        training_mode_cd = storedataList.get(position).getTMODE_CD().get(0);
                        trainning_mode = storedataList.get(position).getTRAINING_MODE().get(0);

                        if (storedataList.get(position).getUPLOAD_STATUS().get(0).equalsIgnoreCase(CommonString.KEY_U)) {
                            Snackbar.make(rec_store_data, CommonString.MESSAGE_DATA_ALREADY_UPLOADED, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        } else if (storedataList.get(position).getCHECKOUT_STATUS().get(0).equals(CommonString.KEY_C)) {
                            Snackbar.make(rec_store_data, CommonString.MESSAGE_STORE_ALREADY_CHECKED_OUT, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        } else {
                            boolean enteryflag = true;
                            if (coverage_data.size() > 0) {
                                for (int i2 = 0; i2 < coverage_data.size(); i2++) {
                                    if (coverage_data.get(i2).getStoreId().equals(store_cdss)) {
                                        if (coverage_data.get(i2).getStatus().equals(CommonString.STORE_STATUS_LEAVE)) {
                                            Snackbar.make(rec_store_data, CommonString.MESSAGE_SOTORE_ALREADY_CLOSED, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                                            enteryflag = false;
                                            break;
                                        }
                                    }

                                }
                                if (enteryflag) {
                                    for (int i = 0; i < coverage_data.size(); i++) {
                                        if (coverage_data.get(i).getStatus().equalsIgnoreCase(CommonString.KEY_VALID) || coverage_data.get(i).getStatus().equalsIgnoreCase(CommonString.KEY_CHECK_IN)) {
                                            if (!coverage_data.get(i).getStoreId().equals(store_cdss)) {
                                                Snackbar.make(rec_store_data, CommonString.MESSAGE_FIRST_CHECKOUT, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                                                enteryflag = false;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                            if (enteryflag) {
                                dialog = new Dialog(StoreListActivity.this);
                                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                dialog.setContentView(R.layout.dialog_layout);
                                RadioGroup radioGroup = (RadioGroup) dialog.findViewById(R.id.radiogrpvisit);
                                radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                                    @Override
                                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                                        // find which radio button is selected
                                        if (checkedId == R.id.yes) {
                                            dialog.dismiss();
                                            editor = preferences.edit();
                                            editor.putString(CommonString.KEY_STORE_CD, store_cdss);
                                            editor.putString(CommonString.KEY_STORE_NAME, holder.mItem.getSTORENAME().get(0));
                                            editor.putString(CommonString.KEY_TRAINING_MODE, trainning_mode);
                                            editor.putString(CommonString.KEY_TRAINING_MODE_CD, holder.mItem.getTMODE_CD().get(0));
                                            editor.commit();

                                            if (!holder.mItem.getGEOTAG().get(0).equalsIgnoreCase(CommonString.KEY_Y)) {
                                                AlertDialog.Builder builder = new AlertDialog.Builder(StoreListActivity.this).setTitle(getString(R.string.parinaam)).setMessage(getString(R.string.title_store_list_geo_tag)).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog_c, int which) {
                                                        startActivity(new Intent(StoreListActivity.this, LocationActivity.class));
                                                        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                                                        dialog_c.dismiss();
                                                    }
                                                });
                                                builder.show();
                                            } else {
                                                if (checkgpsEnableDevice()) {
                                                    if (storelat != 0 && storelongt != 0 && lat != 0.0 && lon != 0.0) {
                                                        int distance = distFrom(storelat, storelongt, lat, lon);
                                                        if (distance <= 200) {
                                                            ArrayList<CoverageBean> specdata;
                                                            specdata = db.getCoverageSpecificData(holder.mItem.getSTORE_CD().get(0), holder.mItem.getVISIT_DATE().get(0));
                                                            if (specdata.size() == 0) {
                                                                Intent in = new Intent(getApplicationContext(), StoreImageActivity.class);
                                                                startActivity(in);
                                                                overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                                                            } else {
                                                                Intent in = new Intent(getApplicationContext(), StoreIsdActivity.class);
                                                                startActivity(in);
                                                                overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                                                            }
                                                        } else {
                                                            Snackbar.make(rec_store_data, "You should be less than 200 meters from the store to enter data for the store.", Snackbar.LENGTH_LONG).show();
                                                        }
                                                    } else {
                                                        ArrayList<CoverageBean> specdata;
                                                        specdata = db.getCoverageSpecificData(holder.mItem.getSTORE_CD().get(0), holder.mItem.getVISIT_DATE().get(0));
                                                        if (specdata.size() == 0) {
                                                            Intent in = new Intent(getApplicationContext(), StoreImageActivity.class);
                                                            startActivity(in);
                                                            overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                                                        } else {
                                                            Intent in = new Intent(getApplicationContext(), StoreIsdActivity.class);
                                                            startActivity(in);
                                                            overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (checkedId == R.id.no) {
                                            dialog.cancel();
                                            if (db.getCoverageSpecificData(holder.mItem.getSTORE_CD().get(0), holder.mItem.getVISIT_DATE().get(0)).size()>0){
                                                AlertDialog.Builder builder = new AlertDialog.Builder(StoreListActivity.this).setTitle(getString(R.string.parinaam)).setMessage(CommonString.DATA_DELETE_ALERT_MESSAGE)
                                                        .setCancelable(false)
                                                        .setPositiveButton("Yes",
                                                                new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                        UpdateData(store_cdss);
                                                                        editor = preferences.edit();
                                                                        Intent in = new Intent(getApplicationContext(), NonWorkingReasonActivity.class);
                                                                        in.putExtra(CommonString.KEY_STORE_CD, store_cdss);
                                                                        editor.putString(CommonString.KEY_STORE_NAME, holder.mItem.getSTORENAME().get(0));
                                                                        in.putExtra(CommonString.KEY_TRAINING_MODE, holder.mItem.getTRAINING_MODE().get(0));
                                                                        editor.putString(CommonString.KEY_TRAINING_MODE_CD, holder.mItem.getTMODE_CD().get(0));
                                                                        startActivity(in);
                                                                        editor.commit();

                                                                    }
                                                                })
                                                        .setNegativeButton("No",
                                                                new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int id) {
                                                                        dialog.cancel();
                                                                    }
                                                                });
                                                AlertDialog alert = builder.create();
                                                alert.show();
                                            }else {
                                                editor = preferences.edit();
                                                Intent in = new Intent(getApplicationContext(), NonWorkingReasonActivity.class);
                                                in.putExtra(CommonString.KEY_STORE_CD, store_cdss);
                                                editor.putString(CommonString.KEY_STORE_NAME, holder.mItem.getSTORENAME().get(0));
                                                in.putExtra(CommonString.KEY_TRAINING_MODE, holder.mItem.getTRAINING_MODE().get(0));
                                                editor.putString(CommonString.KEY_TRAINING_MODE_CD, holder.mItem.getTMODE_CD().get(0));
                                                startActivity(in);
                                                editor.commit();
                                            }
                                        }

                                    }
                                });

                                dialog.show();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            holder.btn_checkout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(StoreListActivity.this).setMessage("Are you sure you want to Checkout ?").setCancelable(false).setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    if (CommonFunctions.CheckNetAvailability(StoreListActivity.this)) {
                                        Intent i = new Intent(StoreListActivity.this, CheckOutStoreActivity.class);
                                        i.putExtra(CommonString.KEY_STORE_CD, storedataList.get(position).getSTORE_CD().get(0));
                                        startActivity(i);
                                    } else {
                                        Snackbar.make(holder.btn_checkout, getString(R.string.nonetwork), Snackbar.LENGTH_LONG).show();
                                    }

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
            });


            if (mValues.get(position).
                    getTRAINING_MODE().
                    get(0).

                    equalsIgnoreCase("Remote"))

            {
                holder.img_tick.setVisibility(View.VISIBLE);
            } else

            {
                holder.img_tick.setVisibility(View.GONE);
            }

        }

        @Override
        public int getItemCount() {
            return mValues.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public final View mView;
            public final LinearLayout parentLayout;
            public final TextView tv_store_name;
            public final TextView tv_city;
            public final TextView tv_store_type;
            public final ImageView img;
            public final ImageView img_tick;
            public final Button btn_checkout;
            public JCPGetterSetter mItem;

            public ViewHolder(View view) {
                super(view);
                mView = view;

                tv_store_name = (TextView) view.findViewById(R.id.tv_store_name);
                tv_city = (TextView) view.findViewById(R.id.tv_city);
                tv_store_type = (TextView) view.findViewById(R.id.tv_store_type);
                img = (ImageView) view.findViewById(R.id.img);
                img_tick = (ImageView) view.findViewById(R.id.img_tick);
                btn_checkout = (Button) view.findViewById(R.id.btn_checkout);
                parentLayout = (LinearLayout) view.findViewById(R.id.parent_layout);
            }

        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
        }
        return super.onOptionsItemSelected(item);
    }

    public void UpdateData(String storeCd) {
        db.open();
        db.deleteSpecificStoreData(storeCd);
        db.updateStoreStatusOnCheckout(storeCd, storedataList.get(0).getVISIT_DATE().get(0), "N");
    }


    public String getCurrentTime() {
        Calendar m_cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String cdate = formatter.format(m_cal.getTime());
        return cdate;

    }

    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mLastLocation != null) {
                lat = mLastLocation.getLatitude();
                lon = mLastLocation.getLongitude();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Method to verify google play services on the device
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates() {

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        }
    }


    /**
     * Creating google api client object
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
    }

    /**
     * Creating location request object
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private boolean checkgpsEnableDevice() {
        boolean flag = true;
        googleApiClient = null;
        if (!hasGPSDevice(StoreListActivity.this)) {
            Toast.makeText(StoreListActivity.this, "Gps not Supported", Toast.LENGTH_SHORT).show();
        }
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && hasGPSDevice(StoreListActivity.this)) {
            enableLoc();
            flag = false;
        } else if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER) && hasGPSDevice(StoreListActivity.this)) {
            flag = true;
        }
        return flag;
    }

    private boolean hasGPSDevice(Context context) {
        final LocationManager mgr = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (mgr == null)
            return false;
        final List<String> providers = mgr.getAllProviders();
        if (providers == null)
            return false;
        return providers.contains(LocationManager.GPS_PROVIDER);
    }

    private void enableLoc() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {
                            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            if (mLastLocation != null) {
                                lat = mLastLocation.getLatitude();
                                lon = mLastLocation.getLongitude();
                            }
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            googleApiClient.connect();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {
                            // googleApiClient.connect();
                            Log.d("Location error", "Location error " + connectionResult.getErrorCode());
                        }
                    }).build();
            googleApiClient.connect();

            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationRequest.setInterval(30 * 1000);
            locationRequest.setFastestInterval(5 * 1000);
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
            builder.setAlwaysShow(true);
            PendingResult<LocationSettingsResult> result =
                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                @Override
                public void onResult(LocationSettingsResult result) {
                    final Status status = result.getStatus();
                    switch (status.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                // Show the dialog by calling startResolutionForResult(),
                                status.startResolutionForResult(StoreListActivity.this, REQUEST_LOCATION);
                            } catch (IntentSender.SendIntentException e) {
                                // Ignore the error.
                            }
                            break;
                    }
                }
            });
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_LOCATION:
                switch (resultCode) {
                    case Activity.RESULT_CANCELED: {
                        googleApiClient = null;
                    }

                    default: {
                        break;
                    }
                }
                break;
        }

    }

    public static int distFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; //meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        int dist = (int) (earthRadius * c);

        return dist;
    }


    public boolean checkNetIsAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }


}
