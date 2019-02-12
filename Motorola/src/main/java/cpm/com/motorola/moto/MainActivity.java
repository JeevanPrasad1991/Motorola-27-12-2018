package cpm.com.motorola.moto;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.dailyentry.SaleTeamTrainingActivity;
import cpm.com.motorola.dailyentry.StoreListActivity;
import cpm.com.motorola.database.MotorolaDatabase;
import cpm.com.motorola.download.CompleteDownloadActivity;
import cpm.com.motorola.gatemeeeting.GateMeetingActivity;
import cpm.com.motorola.upload.CheckoutNUpload;
import cpm.com.motorola.upload.UploadActivity;
import cpm.com.motorola.xmlgettersetter.CoverageBean;
import cpm.com.motorola.xmlgettersetter.JCPGetterSetter;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private SharedPreferences preferences = null;
    private String user_name, user_type, visit_date;
    MotorolaDatabase db;
    ImageView img_logo;
    RecyclerView rec_store_data;
    String str;
    ArrayList<JCPGetterSetter> jcplist;
    ArrayList<CoverageBean> coveragedata;
    JCPGetterSetter storestatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        img_logo = (ImageView) findViewById(R.id.img_logo);
        rec_store_data = (RecyclerView) findViewById(R.id.rec_store_data);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        user_name = preferences.getString(CommonString.KEY_USERNAME, null);
        user_type = preferences.getString(CommonString.KEY_USER_TYPE, null);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        db = new MotorolaDatabase(getApplicationContext());
        db.open();
        str = CommonString.FILE_PATH;

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        View headerView = LayoutInflater.from(this).inflate(R.layout.nav_header_layout, navigationView, false);
        navigationView.addHeaderView(headerView);

        TextView tv_username = (TextView) headerView.findViewById(R.id.nav_user_name);
        TextView tv_usertype = (TextView) headerView.findViewById(R.id.nav_user_type);

        tv_username.setText(user_name);
        tv_usertype.setText(user_type);
        setTitle("Notice Board");

        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        db = new MotorolaDatabase(getApplicationContext());
        db.open();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_daily_entry) {
            Intent in = new Intent(getApplicationContext(), StoreListActivity.class);
            startActivity(in);
            overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
        } else if (id == R.id.nav_download) {
            // Download data
            Handler h = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what != 1) { // code if not connected
                        Snackbar.make(rec_store_data, CommonString.NO_INTERNET_CONNECTION, Snackbar.LENGTH_SHORT).show();

                    } else { // code if connected
                        if (db.isCoverageDataFilled(visit_date)) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Parinaam");
                            builder.setMessage("Please Upload Previous Data First")
                                    .setCancelable(false)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent startUpload = new Intent(MainActivity.this, CheckoutNUpload.class);
                                            startActivity(startUpload);
                                            overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
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
                            overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                        }

                    }
                }
            };
            isNetworkAvailable(h, 5000);

        } else if (id == R.id.nav_upload) {
            //Upload data
            boolean flag = true;
            jcplist = db.getStoreData(visit_date);
            if (jcplist.size() == 0) {
                Snackbar.make(rec_store_data, "Please Download Data First", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
            } else {
                db.open();
                coveragedata = db.getCoverageData(visit_date);
                if (coveragedata.size() > 0) {
                    for (int i = 0; i < coveragedata.size(); i++) {
                        if (coveragedata.get(i).getStatus().equalsIgnoreCase(CommonString.KEY_VALID) || coveragedata.get(i).getStatus().equalsIgnoreCase(CommonString.KEY_CHECK_IN)) {
                            Snackbar.make(rec_store_data, "First checkout of store", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        if ((validate_data(coveragedata))) {
                            Handler h = new Handler() {
                                @Override
                                public void handleMessage(Message msg) {
                                    if (msg.what != 1) { // code if not connected
                                        Snackbar.make(rec_store_data, CommonString.NO_INTERNET_CONNECTION, Snackbar.LENGTH_SHORT).show();
                                    } else {
                                        Intent startUpload = new Intent(getApplicationContext(), UploadActivity.class);
                                        startActivity(startUpload);
                                        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
                                    }
                                }
                            };
                            isNetworkAvailable(h, 5000);
                        } else {
                            Snackbar.make(rec_store_data, CommonString.MESSAGE_NO_DATA, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                        }
                    }
                } else {
                    Snackbar.make(rec_store_data, CommonString.MESSAGE_NO_DATA, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
                }
            }

        } else if (id == R.id.nav_sTeam_training) {
            jcplist = db.getStoreData(visit_date);
            if (jcplist.size() == 0) {
                Snackbar.make(rec_store_data, "Please Download Data First", Snackbar.LENGTH_SHORT).setAction("Action", null).show();
            } else {
                startActivity(new Intent(MainActivity.this, SaleTeamTrainingActivity.class));
                overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
            }
        } else if (id == R.id.nav_exit) {
            //Exit to login
            Intent startDownload = new Intent(this, LoginActivity.class);
            startActivity(startDownload);
            overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
            finish();

        } else if (id == R.id.nav_gatemeeting) {
            //Exit to login
            Intent startDownload = new Intent(this, GateMeetingActivity.class);
            startActivity(startDownload);
            overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
        } else if (id == R.id.nav_help) {
            //Open Help Fragment
            Intent startDownload = new Intent(this, HelpActivity.class);
            startActivity(startDownload);
            overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
        } else if (id == R.id.nav_export_database) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this).setMessage("Are you sure you want to take the backup of your data ?").setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @SuppressWarnings("resource")
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    try {
                                        File file = new File(Environment.getExternalStorageDirectory(), "Moto_backup");
                                        if (!file.isDirectory()) {
                                            file.mkdir();
                                        }
                                        File sd = Environment.getExternalStorageDirectory();
                                        File data = Environment.getDataDirectory();
                                        if (sd.canWrite()) {
                                            long date = System.currentTimeMillis();
                                            SimpleDateFormat sdf = new SimpleDateFormat("MMM/dd/yy");
                                            String dateString = sdf.format(date);
                                            String currentDBPath = "//data//cpm.com.motorola//databases//" + MotorolaDatabase.DATABASE_NAME;
                                            String backupDBPath = "Moto_backup" + dateString.replace('/', '-');
                                            File currentDB = new File(data, currentDBPath);
                                            File backupDB = new File("/mnt/sdcard/Moto_backup/", backupDBPath);
                                            Snackbar.make(rec_store_data, "Database Exported Successfully", Snackbar.LENGTH_SHORT).show();
                                            if (currentDB.exists()) {
                                                FileChannel src = new FileInputStream(currentDB).getChannel();
                                                FileChannel dst = new FileOutputStream(backupDB).getChannel();
                                                dst.transferFrom(src, 0, src.size());
                                                src.close();
                                                dst.close();
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.out.println(e.getMessage());
                                    }

                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    dialog.cancel();
                                }
                            });
            AlertDialog alert1 = builder1.create();
            alert1.show();


        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    public static void isNetworkAvailable(final Handler handler, final int timeout) {
        // ask fo message '0' (not connected) or '1' (connected) on 'handler'
        // the answer must be send before before within the 'timeout' (in milliseconds)

        new Thread() {
            private boolean responded = false;

            @Override
            public void run() {
                // set 'responded' to TRUE if is able to connect with google mobile (responds fast)
                new Thread() {
                    @Override
                    public void run() {
                        HttpGet requestForTest = new HttpGet("http://m.google.com");
                        try {
                            new DefaultHttpClient().execute(requestForTest); // can last...
                            responded = true;
                        } catch (Exception e) {
                        }
                    }
                }.start();

                try {
                    int waited = 0;
                    while (!responded && (waited < timeout)) {
                        sleep(100);
                        if (!responded) {
                            waited += 100;
                        }
                    }
                } catch (InterruptedException e) {
                } // do nothing
                finally {
                    if (!responded) {
                        handler.sendEmptyMessage(0);
                    } else {
                        handler.sendEmptyMessage(1);
                    }
                }
            }
        }.start();
    }

    public boolean validate_data(ArrayList<CoverageBean> cdata) {
        boolean result = false;
        for (int i = 0; i < cdata.size(); i++) {
            storestatus = db.getStoreStatus(cdata.get(i).getStoreId());
            if (!storestatus.getUPLOAD_STATUS().get(0).equalsIgnoreCase(CommonString.KEY_U)) {
                if ((storestatus.getCHECKOUT_STATUS().get(0).equalsIgnoreCase(
                        CommonString.KEY_C)
                        || storestatus.getUPLOAD_STATUS().get(0).equalsIgnoreCase(
                        CommonString.KEY_P) || storestatus.getUPLOAD_STATUS().get(0)
                        .equalsIgnoreCase(CommonString.STORE_STATUS_LEAVE)) || storestatus.getCHECKOUT_STATUS().get(0).equalsIgnoreCase(
                        CommonString.STORE_STATUS_LEAVE)) {
                    result = true;
                    break;

                }
            }
        }

        return result;
    }

}
