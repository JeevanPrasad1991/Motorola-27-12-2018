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
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionMenu;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonFunctions;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.database.MotorolaDatabase;
import cpm.com.motorola.xmlHandler.XMLHandlers;
import cpm.com.motorola.xmlgettersetter.AddNewEmployeeGetterSetter;
import cpm.com.motorola.xmlgettersetter.AllIsdNEmployeeGetterSetter;
import cpm.com.motorola.xmlgettersetter.EmpCdIsdGetterSetter;
import cpm.com.motorola.xmlgettersetter.StoreISDGetterSetter;

public class StoreIsdActivity extends AppCompatActivity implements View.OnClickListener {
    private ArrayAdapter<CharSequence> isdAdapter;
    Spinner spinner_isd;
    MotorolaDatabase db;
    boolean enabled;
    ArrayList<StoreISDGetterSetter> storeISDGetterSetters;
    private SharedPreferences preferences;
    String store_cd, training_mode_cd, visit_date, username;
    Button btn_next;
    Button btn_add_isd, btn_cancel;
    String emp_id, isd_cd = "0", isd_name = "", name, phone, email;
    boolean isIsd = false;
    EmpCdIsdGetterSetter empCdIsdGetterSetter;
    RelativeLayout lay_spin, lay_next;
    LinearLayout lay_add_isd;
    TextView tv_isd, tv_emp_cd;
    FloatingActionMenu materialDesignFAM;
    com.github.clans.fab.FloatingActionButton fab_add_new_employee, fab_add_existing;
    RecyclerView rec_isd;
    ArrayList<AllIsdNEmployeeGetterSetter> allIsdNEmployeeList;
    private SharedPreferences.Editor editor = null;
    boolean is_new_isd_flag = false;
    CardView cardView_isd;
    ImageView isd_image, isdimage;
    String _pathforcheck = "", _path, str, img1 = "";
    RelativeLayout layout_camera;
    String trainning_mode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_store_isd);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        rec_isd = (RecyclerView) findViewById(R.id.rec_isd_added);
        btn_next = (Button) findViewById(R.id.btn_next);
        btn_add_isd = (Button) findViewById(R.id.btn_add_isd);
        btn_cancel = (Button) findViewById(R.id.btn_cancel);
        lay_spin = (RelativeLayout) findViewById(R.id.layout_isd_spin);
        lay_next = (RelativeLayout) findViewById(R.id.layout_next);
        lay_add_isd = (LinearLayout) findViewById(R.id.layout_show_isd);
        tv_isd = (TextView) findViewById(R.id.tv_isd);
        tv_emp_cd = (TextView) findViewById(R.id.tv_emp_cd);
        cardView_isd = (CardView) findViewById(R.id.card_view_isd);
        materialDesignFAM = (FloatingActionMenu) findViewById(R.id.material_design_android_floating_action_menu);
        fab_add_new_employee = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_add_new_employee);
        fab_add_existing = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.fab_add_existing);
        isd_image = (ImageView) findViewById(R.id.isd_image);
        layout_camera = (RelativeLayout) findViewById(R.id.layout_camera);
        str = CommonString.FILE_PATH;
        materialDesignFAM.setClosedOnTouchOutside(true);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        store_cd = preferences.getString(CommonString.KEY_STORE_CD, null);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        trainning_mode = preferences.getString(CommonString.KEY_TRAINING_MODE, null);
        training_mode_cd = preferences.getString(CommonString.KEY_TRAINING_MODE_CD, null);
        username = preferences.getString(CommonString.KEY_USERNAME, null);
        setTitle("Select Training - " + visit_date);
        fab_add_existing.setOnClickListener(this);
        fab_add_new_employee.setOnClickListener(this);
        db = new MotorolaDatabase(getApplicationContext());
        db.open();
        allIsdNEmployeeList = db.getAllIsdNEmployeeData(store_cd);

        if (allIsdNEmployeeList.size() > 0) {
            IsdAddedAdapter isdAddedAdapter = new IsdAddedAdapter(this, allIsdNEmployeeList);
            rec_isd.setAdapter(isdAddedAdapter);
            rec_isd.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        }
        storeISDGetterSetters = db.getStoreIsdData(store_cd);
        isdAdapter = new ArrayAdapter<CharSequence>(this, R.layout.spinner_custom_item);
        spinner_isd = (Spinner) findViewById(R.id.spinisd);
        isdAdapter.add("-Select-");
        for (int i = 0; i < storeISDGetterSetters.size(); i++) {
            isdAdapter.add(storeISDGetterSetters.get(i).getISD_NAME().get(0));
        }
        spinner_isd.setAdapter(isdAdapter);
        spinner_isd.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                materialDesignFAM.close(true);
                if (position != 0) {
                    isd_cd = storeISDGetterSetters.get(position - 1).getISD_CD().get(0);
                    isd_name = storeISDGetterSetters.get(position - 1).getISD_NAME().get(0);
                } else {
                    isd_cd = "0";
                    isd_name = "";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        btn_next.setOnClickListener(this);
        btn_add_isd.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
        isd_image.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btn_add_isd:
                is_new_isd_flag = true;
            case R.id.btn_next:
                materialDesignFAM.close(true);
                if (trainning_mode.equalsIgnoreCase("Remote")) {
                    if (isd_cd.equals("0")) {
                        Snackbar.make(lay_spin, "First select an ISD", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } else {
                        boolean is_isd_exists = false;
                        for (int i = 0; i < allIsdNEmployeeList.size(); i++) {
                            if (allIsdNEmployeeList.get(i).getIsd_cd().equals(isd_cd)) {
                                is_isd_exists = true;
                                break;
                            }
                        }
                        if (is_isd_exists) {
                            if (is_new_isd_flag) {
                                lay_next.setVisibility(View.VISIBLE);
                                lay_spin.setVisibility(View.VISIBLE);
                                lay_add_isd.setVisibility(View.GONE);
                                isd_cd = "0";
                                isd_name = "";
                                empCdIsdGetterSetter = null;
                                is_new_isd_flag = false;
                            } else {
                                isd_cd = "0";
                                isd_name = "";
                                spinner_isd.setSelection(0);
                            }
                            Snackbar.make(lay_spin, "ISD already done", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        } else {
                            startAuditOrTopic(null, is_new_isd_flag);
                        }
                    }
                } else {
                    if (isd_cd.equals("0")) {
                        Snackbar.make(lay_spin, "First select an ISD", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } else if (img1.equals("")) {
                        Snackbar.make(lay_spin, "Please capture image", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } else {
                        boolean is_isd_exists = false;
                        for (int i = 0; i < allIsdNEmployeeList.size(); i++) {
                            if (allIsdNEmployeeList.get(i).getIsd_cd().equals(isd_cd)) {
                                is_isd_exists = true;
                                break;
                            }
                        }
                        if (is_isd_exists) {
                            if (is_new_isd_flag) {
                                lay_next.setVisibility(View.VISIBLE);
                                lay_spin.setVisibility(View.VISIBLE);
                                lay_add_isd.setVisibility(View.GONE);
                                isd_cd = "0";
                                isd_name = "";
                                empCdIsdGetterSetter = null;
                                is_new_isd_flag = false;
                            } else {
                                isd_cd = "0";
                                isd_name = "";
                                spinner_isd.setSelection(0);
                            }
                            Snackbar.make(lay_spin, "ISD already done", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                        } else {
                            startAuditOrTopic(null, is_new_isd_flag);
                        }
                    }
                }
                break;
            case R.id.btn_cancel:
                lay_next.setVisibility(View.VISIBLE);
                lay_spin.setVisibility(View.VISIBLE);
                lay_add_isd.setVisibility(View.GONE);
                isd_cd = "0";
                isd_name = "";
                empCdIsdGetterSetter = null;
                is_new_isd_flag = false;
                break;

            case R.id.fab_add_existing:
                materialDesignFAM.close(true);
                final Dialog dialog_emp = new Dialog(StoreIsdActivity.this);
                dialog_emp.setTitle("Get ISD");
                dialog_emp.setContentView(R.layout.enter_empid_dialog_layout);
                final EditText editEmpCd = (EditText) dialog_emp.findViewById(R.id.et_empid);
                Button btngetIsd = (Button) dialog_emp.findViewById(R.id.btn_get_isd);
                final LinearLayout layout_existing_employee = (LinearLayout) dialog_emp.findViewById(R.id.existing_emp_layout);
                btngetIsd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        emp_id = editEmpCd.getText().toString();
                        if (emp_id.equals("")) {
                            Snackbar.make(layout_existing_employee, "First enter employee id .", Snackbar.LENGTH_SHORT).show();
                        } else {
                            dialog_emp.cancel();
                            new GetISDTask().execute();
                        }
                    }
                });
                dialog_emp.show();
                break;

            case R.id.fab_add_new_employee:
                materialDesignFAM.close(true);
                showAddNewEmployee();
                break;
            case R.id.isd_image:
                _pathforcheck = store_cd + "_ISD_IMG_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
                _path = CommonString.FILE_PATH + _pathforcheck;
                CommonFunctions.startAnncaCameraActivity(StoreIsdActivity.this, _path);
                break;

        }
    }


    //ISD Asynctask
    class GetISDTask extends AsyncTask<Void, Void, String> {
        private ProgressDialog dialog = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog = new ProgressDialog(StoreIsdActivity.this);
            dialog.setTitle("Get ISD");
            dialog.setMessage("Submitting Employee Id.....");
            dialog.setCancelable(false);
            dialog.show();
        }


        @Override
        protected String doInBackground(Void... params) {

            try {
                // STore ISD data
                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                factory.setNamespaceAware(true);
                XmlPullParser xpp = factory.newPullParser();
                SoapObject request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_NAME_UNIVERSAL_DOWNLOAD);
                request.addProperty("UserName", emp_id);
                request.addProperty("Type", "SEARCH_ISD");
                SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                envelope.dotNet = true;
                envelope.setOutputSoapObject(request);
                HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
                androidHttpTransport.call(CommonString.SOAP_ACTION_UNIVERSAL, envelope);
                Object result = (Object) envelope.getResponse();
                if (result.toString() != null) {
                    xpp.setInput(new StringReader(result.toString()));
                    xpp.next();
                    int eventType = xpp.getEventType();
                    empCdIsdGetterSetter = XMLHandlers.empcdXMLHandler(xpp, eventType);
                    if (empCdIsdGetterSetter.getEmp_cd() != null) {
                        return CommonString.KEY_SUCCESS;

                    } else {
                        return CommonString.KEY_NO_DATA;
                    }

                }


            } catch (MalformedURLException e) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_EXCEPTION);
                    }
                });


            } catch (IOException e) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_SOCKETEXCEPTION);
                    }
                });

            } catch (Exception e) {

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
            dialog.cancel();
            if (result.equals(CommonString.KEY_NO_DATA)) {
                Snackbar.make(lay_add_isd, "Invalid Employee Id", Snackbar.LENGTH_SHORT).show();
            } else {
                lay_spin.setVisibility(View.GONE);
                lay_next.setVisibility(View.GONE);
                lay_add_isd.setVisibility(View.VISIBLE);
                tv_emp_cd.setText("Emp CD - " + empCdIsdGetterSetter.getEmp_cd());
                tv_isd.setText("ISD - " + empCdIsdGetterSetter.getIsd());
                isd_cd = empCdIsdGetterSetter.getIsd_cd();
                isd_name = empCdIsdGetterSetter.getIsd();
            }
        }

    }

    public void showMessage(String msg) {
        new AlertDialog.Builder(StoreIsdActivity.this)
                .setTitle("Alert Dialog")
                .setMessage(msg)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        finish();
                    }
                })

                .setIcon(R.drawable.parinaam_logo_ico)
                .show();
    }

    public class IsdAddedAdapter extends RecyclerView.Adapter<IsdAddedAdapter.MyViewHolder> {

        private LayoutInflater inflator;

        List<AllIsdNEmployeeGetterSetter> data = Collections.emptyList();

        public IsdAddedAdapter(Context context, List<AllIsdNEmployeeGetterSetter> data) {
            inflator = LayoutInflater.from(context);
            this.data = data;

        }

        @Override
        public IsdAddedAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int i) {
            View view = inflator.inflate(R.layout.item_isd_added_layout, parent, false);
            MyViewHolder holder = new MyViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(final IsdAddedAdapter.MyViewHolder viewHolder, final int position) {
            final AllIsdNEmployeeGetterSetter current = data.get(position);
            viewHolder.tv_name.setText(current.getName());
            viewHolder.tv_type.setText(current.getType());
        }

        @Override
        public int getItemCount() {
            return data.size();
        }


        class MyViewHolder extends RecyclerView.ViewHolder {
            TextView tv_name, tv_topic, tv_type;

            public MyViewHolder(View itemView) {
                super(itemView);
                tv_name = (TextView) itemView.findViewById(R.id.tv_isd);
                // tv_topic = (TextView) itemView.findViewById(R.id.tv_topic);
                tv_type = (TextView) itemView.findViewById(R.id.tv_training_type);

            }
        }

    }


    public void startAuditOrTopic(AddNewEmployeeGetterSetter addNewEmployeeGetterSetter, boolean is_new_isd) {
        Intent in1;
        editor = preferences.edit();
        editor.putString(CommonString.KEY_ISD_NAME, isd_name);
        editor.commit();
        if (training_mode_cd.equals("1")) {
            in1 = new Intent(getApplicationContext(), IsdSkillActivity.class);
            in1.putExtra(CommonString.KEY_ISD_CD, isd_cd);
            in1.putExtra(CommonString.KEY_TRAINING_MODE_CD, training_mode_cd);
            in1.putExtra(CommonString.KEY_ISD_IMAGE, img1);
            if (addNewEmployeeGetterSetter != null)
                in1.putExtra(CommonString.KEY_NEW_EMPLOYEE, addNewEmployeeGetterSetter);
        } else {
            in1 = new Intent(getApplicationContext(), TrainingActivity.class);
            in1.putExtra(CommonString.KEY_ISD_CD, isd_cd);
            in1.putExtra(CommonString.KEY_TRAINING_MODE_CD, training_mode_cd);
            in1.putExtra(CommonString.KEY_ISD_IMAGE, img1);
            if (addNewEmployeeGetterSetter != null)
                in1.putExtra(CommonString.KEY_NEW_EMPLOYEE, addNewEmployeeGetterSetter);
        }
        startActivity(in1);
        StoreIsdActivity.this.finish();
        overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }

    public void showAddNewEmployee() {
        final Dialog dialog_new_emp = new Dialog(StoreIsdActivity.this);
        dialog_new_emp.setTitle("Add Employee");
        dialog_new_emp.setContentView(R.layout.add_new_employee_dialog);
        final EditText editName = (EditText) dialog_new_emp.findViewById(R.id.et_name);
        final EditText editPhone = (EditText) dialog_new_emp.findViewById(R.id.et_phone);
        final EditText editEmail = (EditText) dialog_new_emp.findViewById(R.id.et_email_id);
        final CheckBox cb_isd = (CheckBox) dialog_new_emp.findViewById(R.id.cb_isisd);
        final RelativeLayout rl_camera_dialog = (RelativeLayout) dialog_new_emp.findViewById(R.id.rl_camera_dialog);
        if (trainning_mode.equalsIgnoreCase("Remote")) {
            rl_camera_dialog.setVisibility(View.GONE);
        }

        isdimage = (ImageView) dialog_new_emp.findViewById(R.id.isd_image);
        cb_isd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isIsd = isChecked;
            }
        });
        Button btn_add_emp = (Button) dialog_new_emp.findViewById(R.id.btn_get_isd);
        Button btn_cancel = (Button) dialog_new_emp.findViewById(R.id.btn_cancel);
        isdimage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                _pathforcheck = store_cd + "_ISD_IMG_" + visit_date.replace("/", "") + "_" + getCurrentTime().replace(":", "") + ".jpg";
                _path = CommonString.FILE_PATH + _pathforcheck;
                CommonFunctions.startAnncaCameraActivity(StoreIsdActivity.this, _path);
            }
        });

        btn_add_emp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = editName.getText().toString();
                phone = editPhone.getText().toString();
                email = editEmail.getText().toString();
                if (trainning_mode.equalsIgnoreCase("Remote")) {
                    if (name.isEmpty()) {
                        Snackbar.make(rl_camera_dialog, "First enter name ", Snackbar.LENGTH_SHORT).show();
                    } else if (phone.isEmpty()) {
                        Snackbar.make(rl_camera_dialog, "First enter mobile number ", Snackbar.LENGTH_SHORT).show();
                    } else if (phone.length() < 10) {
                        Snackbar.make(rl_camera_dialog, "First enter 10 digit mobile number ", Snackbar.LENGTH_SHORT).show();
                    } else if (email.isEmpty()) {
                        Snackbar.make(rl_camera_dialog, "First enter email id ", Snackbar.LENGTH_SHORT).show();
                    } else if (!isValidEmail(email)) {
                        Snackbar.make(rl_camera_dialog, "First enter valid email id ", Snackbar.LENGTH_SHORT).show();
                    } else {
                        dialog_new_emp.cancel();
                        isd_cd = "0";
                        AddNewEmployeeGetterSetter addNewEmployee = new AddNewEmployeeGetterSetter(name, email, phone, isIsd);
                        startAuditOrTopic(addNewEmployee, false);
                    }

                } else {
                    if (name.isEmpty()) {
                        Snackbar.make(rl_camera_dialog, "First enter name ", Snackbar.LENGTH_SHORT).show();
                    } else if (phone.isEmpty()) {
                        Snackbar.make(rl_camera_dialog, "First enter mobile number ", Snackbar.LENGTH_SHORT).show();
                    } else if (phone.length() < 10) {
                        Snackbar.make(rl_camera_dialog, "First enter 10 digit mobile number ", Snackbar.LENGTH_SHORT).show();
                    } else if (email.isEmpty()) {
                        Snackbar.make(rl_camera_dialog, "First enter email id ", Snackbar.LENGTH_SHORT).show();
                    } else if (!isValidEmail(email)) {
                        Snackbar.make(rl_camera_dialog, "First enter valid email id ", Snackbar.LENGTH_SHORT).show();
                    } else if (img1.equals("")) {
                        Snackbar.make(rl_camera_dialog, "Please capture image", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                    } else {
                        dialog_new_emp.cancel();
                        isd_cd = "0";
                        AddNewEmployeeGetterSetter addNewEmployee = new AddNewEmployeeGetterSetter(name, email, phone, isIsd);
                        startAuditOrTopic(addNewEmployee, false);
                    }
                }

            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog_new_emp.cancel();
            }
        });
        dialog_new_emp.show();

    }

    public String getCurrentTime() {
        Calendar m_cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String cdate = formatter.format(m_cal.getTime());
        return cdate;

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
                            if (!isd_cd.equals("0")) {
                                String metadata = CommonFunctions.setMetadataAtImages(preferences.getString(CommonString.KEY_STORE_NAME, ""), store_cd, "Isd Image", username);
                                CommonFunctions.addMetadataAndTimeStampToImage(StoreIsdActivity.this, _path, metadata, visit_date);
                                isd_image.setBackgroundResource(R.drawable.camera_icon_done);
                                img1 = _pathforcheck;
                                _pathforcheck = "";
                            } else {
                                String metadata = CommonFunctions.setMetadataAtImages(preferences.getString(CommonString.KEY_STORE_NAME, ""), store_cd, "Isd Image", username);
                                CommonFunctions.addMetadataAndTimeStampToImage(StoreIsdActivity.this, _path, metadata, visit_date);
                                isdimage.setBackgroundResource(R.drawable.camera_icon_done);
                                img1 = _pathforcheck;
                                _pathforcheck = "";
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }


    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(StoreIsdActivity.this);
        builder.setMessage(CommonString.ONBACK_ALERT_MESSAGE)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                        StoreIsdActivity.this.finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            AlertDialog.Builder builder = new AlertDialog.Builder(StoreIsdActivity.this);
            builder.setMessage(CommonString.ONBACK_ALERT_MESSAGE)
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                            StoreIsdActivity.this.finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
        return super.onOptionsItemSelected(item);
    }


}