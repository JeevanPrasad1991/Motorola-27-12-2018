package cpm.com.motorola.dailyentry;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.database.MotorolaDatabase;
import cpm.com.motorola.multiselectionspinnerfortopic.TopicMultiSInterface;
import cpm.com.motorola.multiselectionspinnerfortopic.TopicMultiSelectionSpinner;
import cpm.com.motorola.xmlgettersetter.AddNewEmployeeGetterSetter;
import cpm.com.motorola.xmlgettersetter.AuditChecklistGetterSetter;
import cpm.com.motorola.xmlgettersetter.CoverageBean;
import cpm.com.motorola.xmlgettersetter.EmpCdIsdGetterSetter;
import cpm.com.motorola.xmlgettersetter.QuizAnwserGetterSetter;
import cpm.com.motorola.xmlgettersetter.TrainingTopicGetterSetter;

public class TrainingActivity extends AppCompatActivity {
    MotorolaDatabase db;
    ArrayList<TrainingTopicGetterSetter> trainingTopicGetterSetterArrayList;
    ArrayList<TrainingTopicGetterSetter> selectedTopicList = new ArrayList<>();
    HashMap<AuditChecklistGetterSetter, ArrayList<AuditChecklistGetterSetter>> listDataChild = new HashMap<>();
    ArrayList<AuditChecklistGetterSetter> listDataHeader = new ArrayList<>();
    private ArrayAdapter<CharSequence> topicAdapter;
    // Spinner spinner_topic;
    String topic_cd = "0", isd_cd, training_mode_cd, isd_image;
    String store_cd, visit_date, username;
    private SharedPreferences preferences = null;
    // LogCat tag
    private static final String TAG = TrainingActivity.class.getSimpleName();
    AddNewEmployeeGetterSetter addNewEmployeeGetterSetter;
    TopicMultiSelectionSpinner spinner_topic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        spinner_topic = (TopicMultiSelectionSpinner) findViewById(R.id.spintopic);

        training_mode_cd = getIntent().getStringExtra(CommonString.KEY_TRAINING_MODE_CD);
        isd_cd = getIntent().getStringExtra(CommonString.KEY_ISD_CD);
        isd_image = getIntent().getStringExtra(CommonString.KEY_ISD_IMAGE);
        addNewEmployeeGetterSetter = getIntent().getParcelableExtra(CommonString.KEY_NEW_EMPLOYEE);
        ///get audit data using intent
        listDataChild = (HashMap<AuditChecklistGetterSetter, ArrayList<AuditChecklistGetterSetter>>) getIntent().getSerializableExtra(CommonString.KEY_AUDIT_DATA);
        db = new MotorolaDatabase(getApplicationContext());
        db.open();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        store_cd = preferences.getString(CommonString.KEY_STORE_CD, null);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        username = preferences.getString(CommonString.KEY_USERNAME, null);
        setTitle("Training Topic" + " - " + visit_date);
        ///topic data from database
        trainingTopicGetterSetterArrayList = db.getTopicData();
        topicAdapter = new ArrayAdapter<CharSequence>(this, R.layout.spinner_custom_item);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setImageResource(R.drawable.save_icon);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (selectedTopicList.size() == 0) {
                    Snackbar.make(spinner_topic, CommonString.MESSAGE_SELECT_TRAINING_TOPIC, Snackbar.LENGTH_SHORT).show();
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(TrainingActivity.this).setTitle(getString(R.string.parinaam))
                            .setMessage(getString(R.string.alert_save)).setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    try {
                                        db.open();
                                        db.updateCoverageStoreOutTime(store_cd, visit_date, "", CommonString.KEY_VALID);
                                        ArrayList<QuizAnwserGetterSetter> answered_list = new ArrayList<>();
                                        QuizAnwserGetterSetter ans = new QuizAnwserGetterSetter();
                                        ans.setIsd_cd(isd_cd);
                                        ans.setTopic_cd(topic_cd);
                                        ans.setTraining_mode_cd(training_mode_cd);
                                        ans.setAnswer_cd("0");
                                        ans.setAnswer("");
                                        ans.setQuestion_cd("0");
                                        answered_list.add(ans);
                                        long mid = 0;
                                        if (isd_cd.equals("0") && addNewEmployeeGetterSetter != null)
                                            mid = db.insertNewEmployeeData(addNewEmployeeGetterSetter, store_cd, isd_image);
                                        db.open();
                                        db.insertAnsweredData(answered_list, store_cd, mid);
                                        ///////////////change..............
                                        if (listDataChild != null && listDataChild.size() > 0) {
                                            for (AuditChecklistGetterSetter key : listDataChild.keySet()) {
                                                listDataHeader.add(key);
                                            }
                                            db.open();
                                            db.insertAuditChecklistWithCategoryData(store_cd, isd_cd, mid, listDataChild, listDataHeader);
                                        }
                                        db.open();

                                        if (!isd_cd.equals("") && !isd_cd.equals("0")){
                                            db.insertNewIsdData(isd_cd, String.valueOf(mid),isd_image, store_cd,visit_date,preferences.getString(CommonString.KEY_ISD_NAME, ""));
                                        }

                                        db.open();
                                        if (selectedTopicList.size() > 0) {
                                            db.insertTrainningTopicMultiData(selectedTopicList, store_cd, isd_cd, mid, visit_date);
                                        }
                                        finish();
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
                }


            }
        });
        uiInfaceData();
    }


    public String getCurrentTime() {
        Calendar m_cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String cdate = formatter.format(m_cal.getTime());
        return cdate;

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void uiInfaceData() {
        if (trainingTopicGetterSetterArrayList.size() > 0) {
            topicAdapter = new ArrayAdapter<CharSequence>(this, R.layout.spinner_custom_item);
            topicAdapter.add("-Select Topic-");
            for (int i = 0; i < trainingTopicGetterSetterArrayList.size(); i++) {
                topicAdapter.add(trainingTopicGetterSetterArrayList.get(i).getTOPIC().get(0));
            }
            spinner_topic.setAdapter(topicAdapter);
        }
        ////////// sale team trainee list interface
        TraineeNamInterfaceData();
    }

    private void TraineeNamInterfaceData() {
        spinner_topic.setItems(trainingTopicGetterSetterArrayList, -1, new TopicMultiSInterface() {
            @Override
            public void onItemsSelected(ArrayList<TrainingTopicGetterSetter> items) {
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).isSelected()) {
                        Log.i(TAG, i + " : " + items.get(i).getTOPIC().get(0) + " : " + items.get(i).isSelected());
                        selectedTopicList.add(items.get(i));
                    }
                }
            }
        });
    }
}
