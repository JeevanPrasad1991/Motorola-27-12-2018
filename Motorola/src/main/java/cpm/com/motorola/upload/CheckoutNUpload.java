package cpm.com.motorola.upload;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import cpm.com.motorola.R;
import cpm.com.motorola.constants.CommonString;
import cpm.com.motorola.database.MotorolaDatabase;
import cpm.com.motorola.geocode.GeotaggingBeans;
import cpm.com.motorola.xmlHandler.FailureXMLHandler;
import cpm.com.motorola.xmlgettersetter.AddNewEmployeeGetterSetter;
import cpm.com.motorola.xmlgettersetter.AuditChecklistGetterSetter;
import cpm.com.motorola.xmlgettersetter.CoverageBean;
import cpm.com.motorola.xmlgettersetter.FailureGetterSetter;
import cpm.com.motorola.xmlgettersetter.GateMettingGetterSetter;
import cpm.com.motorola.xmlgettersetter.JCPGetterSetter;
import cpm.com.motorola.xmlgettersetter.QuizAnwserGetterSetter;
import cpm.com.motorola.xmlgettersetter.TrainingTopicGetterSetter;

/**
 * Created by  on 16-09-2016.
 */
public class CheckoutNUpload extends Activity {
    private String visit_date, username, app_ver, errormsg = "", prev_date, Path, datacheck = "", validity,gatemeetingfoldername = "GateMeetingImage";
    boolean up_success_flag = true;
    boolean isDialogShowing = false;
    private Dialog dialog;
    private ProgressBar pb;
    MotorolaDatabase database;
    private SharedPreferences preferences;
    private TextView percentage, message;
    ArrayList<JCPGetterSetter> jcplist;
    ArrayList<AuditChecklistGetterSetter> auditData;
    ArrayList<AddNewEmployeeGetterSetter> newEmpData;
    private FailureGetterSetter failureGetterSetter = null;
    ArrayList<CoverageBean> coverageBean = new ArrayList<>();
    ArrayList<GeotaggingBeans> geodata = new ArrayList<GeotaggingBeans>();
    ArrayList<CoverageBean> coverageBeanlist = new ArrayList<>();
    ArrayList<TrainingTopicGetterSetter> traineCDataList = new ArrayList<>();
    ArrayList<TrainingTopicGetterSetter> trainingTopicList = new ArrayList<>();
    GateMettingGetterSetter gatemeetingManagerGetterSetter = new GateMettingGetterSetter();
    String[] words;
    Data data;
    int mid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        visit_date = preferences.getString(CommonString.KEY_DATE, null);
        username = preferences.getString(CommonString.KEY_USERNAME, null);
        app_ver = preferences.getString(CommonString.KEY_VERSION, "");
        database = new MotorolaDatabase(this);
        database.open();
        Path = CommonString.FILE_PATH;
        if (!isCheckoutDataExist()) {
            new UploadDataTask(this).execute();
        }
    }


    public boolean isCheckoutDataExist() {
        boolean flag = false;
        jcplist = database.getAllJCPData();
        for (int i = 0; i < jcplist.size(); i++) {
            if (!jcplist.get(i).getVISIT_DATE().get(0).equals(visit_date)) {
                prev_date = jcplist.get(i).getVISIT_DATE().get(0);
                coverageBean = database.getCoverageSpecificData(jcplist.get(i).getSTORE_CD().get(0), jcplist.get(i).getVISIT_DATE().get(0));
                if (coverageBean.size() > 0 && coverageBean.get(0).getStatus().equals(CommonString.KEY_VALID)) {
                    flag = true;
                    new BackgroundTask(this).execute();
                    break;
                } else {
                    database.open();
                    database.deleteSpecificStoreData(coverageBean.get(0).getStoreId());
                }
            }
        }

        return flag;
    }

    public String UploadImage(String path, String folder_name) throws Exception {
        errormsg = "";
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(Path + path, o);
        // The new size we want to scale to
        final int REQUIRED_SIZE = 1639;
        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp < REQUIRED_SIZE && height_tmp < REQUIRED_SIZE)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeFile(Path + path, o2);
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bao);
        byte[] ba = bao.toByteArray();
        String ba1 = Base64.encodeBytes(ba);
        SoapObject request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_IMAGE);
        String[] split = path.split("/");
        String path1 = split[split.length - 1];
        request.addProperty("img", ba1);
        request.addProperty("name", path1);
        request.addProperty("FolderName", folder_name);

        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.dotNet = true;
        envelope.setOutputSoapObject(request);
        HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
        androidHttpTransport.call(CommonString.SOAP_ACTION_UPLOAD_IMAGE, envelope);
        Object result = (Object) envelope.getResponse();
        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
            if (result.toString().equalsIgnoreCase(CommonString.KEY_FALSE)) {
                return CommonString.KEY_FALSE;
            }

            SAXParserFactory saxPF = SAXParserFactory.newInstance();
            SAXParser saxP = saxPF.newSAXParser();
            XMLReader xmlR = saxP.getXMLReader();
            // for failure
            FailureXMLHandler failureXMLHandler = new FailureXMLHandler();
            xmlR.setContentHandler(failureXMLHandler);
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(result.toString()));
            xmlR.parse(is);
            failureGetterSetter = failureXMLHandler.getFailureGetterSetter();

            if (failureGetterSetter.getStatus().equalsIgnoreCase(
                    CommonString.KEY_FAILURE)) {
                errormsg = failureGetterSetter.getErrorMsg();
                return CommonString.KEY_FAILURE;
            }
        } else {
            new File(Path + path).delete();

        }

        return result.toString();
    }


    public void showMessage(String msg) {
        if (!isDialogShowing) {
            isDialogShowing = true;
            new AlertDialog.Builder(CheckoutNUpload.this)
                    .setTitle("Alert Dialog")
                    .setMessage(msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with delete
                            //isDialogShowing = false;
                            finish();
                        }
                    }).setIcon(R.drawable.parinaam_logo_ico).show();
        }
    }

    private class UploadDataTask extends AsyncTask<Void, Data, String> {
        private Context context;

        UploadDataTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            dialog = new Dialog(context);
            dialog.setContentView(R.layout.custom_dialog);
            dialog.setTitle("Uploading Data");
            dialog.setCancelable(false);
            dialog.show();
            pb = (ProgressBar) dialog.findViewById(R.id.progressBar1);
            percentage = (TextView) dialog.findViewById(R.id.percentage);
            message = (TextView) dialog.findViewById(R.id.message);
        }

        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub
            try {
                data = new Data();
                data.value = 10;
                data.name = "Uploading Data";
                publishProgress(data);
                database.open();
                coverageBeanlist = database.getCoverageData(prev_date);
                if (coverageBeanlist.size() > 0) {
                    for (int i = 0; i < coverageBeanlist.size(); i++) {
                        if (!coverageBeanlist.get(i).getStatus().equalsIgnoreCase(CommonString.KEY_U)) {
                            String onXML = "[DATA][USER_DATA][STORE_CD]"
                                    + coverageBeanlist.get(i).getStoreId()
                                    + "[/STORE_CD]" + "[VISIT_DATE]"
                                    + coverageBeanlist.get(i).getVisitDate()
                                    + "[/VISIT_DATE][LATITUDE]"
                                    + coverageBeanlist.get(i).getLatitude()
                                    + "[/LATITUDE][APP_VERSION]"
                                    + app_ver
                                    + "[/APP_VERSION][LONGITUDE]"
                                    + coverageBeanlist.get(i).getLongitude()
                                    + "[/LONGITUDE][IN_TIME]"
                                    + coverageBeanlist.get(i).getInTime()
                                    + "[/IN_TIME][OUT_TIME]"
                                    + coverageBeanlist.get(i).getOutTime()
                                    + "[/OUT_TIME][UPLOAD_STATUS]"
                                    + "N"
                                    + "[/UPLOAD_STATUS][USER_ID]" + username
                                    + "[/USER_ID][TMODE_CD]" + coverageBeanlist.get(i).getTraining_mode_cd() +
                                    "[/TMODE_CD][MANAGE]" + "0" +
                                    "[/MANAGE][IMAGE_URL]" + coverageBeanlist.get(i).getImage()
                                    + "[/IMAGE_URL][REASON_ID]"
                                    + coverageBeanlist.get(i).getReasonid()
                                    + "[/REASON_ID][REASON_REMARK]"
                                    + coverageBeanlist.get(i).getRemark()
                                    + "[/REASON_REMARK][/USER_DATA][/DATA]";


                            SoapObject request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_DR_STORE_COVERAGE);
                            request.addProperty("onXML", onXML);
                            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                            envelope.dotNet = true;
                            envelope.setOutputSoapObject(request);
                            HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
                            androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_DR_STORE_COVERAGE, envelope);
                            Object result = (Object) envelope.getResponse();
                            datacheck = result.toString();
                            datacheck = datacheck.replace("\"", "");
                            words = datacheck.split("\\;");
                            validity = (words[0]);
                            if (validity.equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                database.updateCoverageStatus(coverageBeanlist.get(i).getMID(), CommonString.KEY_P);
                                database.updateStoreStatusOnLeave(coverageBeanlist.get(i).getStoreId(), coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_P);
                            } else {
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_FALSE)) {
                                    return CommonString.METHOD_UPLOAD_DR_STORE_COVERAGE;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                                    return CommonString.METHOD_UPLOAD_DR_STORE_COVERAGE;
                                }

                            }
                            mid = Integer.parseInt((words[1]));
                            data.value = 20;
                            data.name = "Uploading Coverage Data";
                            publishProgress(data);
                            String final_xml = "";


                            //uploading New Employee data
                            final_xml = "";
                            onXML = "";
                            database.open();
                            newEmpData = database.getNewEmployeeInsertedData(coverageBeanlist.get(i).getStoreId());
                            if (newEmpData.size() > 0) {
                                for (int j = 0; j < newEmpData.size(); j++) {
                                    String isIsd = "0";
                                    if (newEmpData.get(j).isIsd()) {
                                        isIsd = "1";
                                    } else {
                                        isIsd = "0";
                                    }
                                    onXML = "[NEW_EMPLOYEE_DATA][ISD_CD]"
                                            + "0"
                                            + "[/ISD_CD]"
                                            + "[MID]"
                                            + mid
                                            + "[/MID]"
                                            + "[CREATED_BY]"
                                            + username
                                            + "[/CREATED_BY]"
                                            + "[NAME]"
                                            + newEmpData.get(j).getName()
                                            + "[/NAME]"
                                            + "[EMAIL]"
                                            + newEmpData.get(j).getEmail()
                                            + "[/EMAIL]"
                                            + "[PHONE_NO]"
                                            + newEmpData.get(j).getPhone()
                                            + "[/PHONE_NO]"
                                            + "[KEY_ID]"
                                            + newEmpData.get(j).getKey_id()
                                            + "[/KEY_ID]"
                                            + "[IS_ISD]"
                                            + isIsd
                                            + "[/IS_ISD]"
                                            + "[ISD_IMAGE]"
                                            + newEmpData.get(j).getImage()
                                            + "[/ISD_IMAGE]"
                                            + "[/NEW_EMPLOYEE_DATA]";

                                    final_xml = final_xml + onXML;

                                }

                                final String employee_xml = "[DATA]" + final_xml + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", employee_xml);
                                request.addProperty("KEYS", "NEW_EMPLOYEE_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }

                                data.value = 35;
                                data.name = "NEW EMPLOYEE DATA";
                                publishProgress(data);
                            }


                            final_xml = "";
                            onXML = "";
                            database.open();
                            ArrayList<TrainingTopicGetterSetter> trainingTopic_new_employeeList = database.getTrainningTopicfORNEWEMPLOYEEData(coverageBeanlist.get(i).getStoreId());
                            if (trainingTopic_new_employeeList.size() > 0) {
                                for (int j = 0; j < trainingTopic_new_employeeList.size(); j++) {
                                    onXML = "[TRAINING_TOPIC_NEW_EMPLOYEE_DATA][ISD_CD]"
                                            + trainingTopic_new_employeeList.get(j).getIsd_cd()
                                            + "[/ISD_CD]"
                                            + "[MID]"
                                            + mid
                                            + "[/MID]"
                                            + "[CREATED_BY]"
                                            + username
                                            + "[/CREATED_BY]"
                                            + "[KEY_ID]"
                                            + trainingTopic_new_employeeList.get(j).getKey_id()
                                            + "[/KEY_ID]"
                                            + "[TOPIC_CD]"
                                            + trainingTopic_new_employeeList.get(j).getTOPIC_CD().get(0)
                                            + "[/TOPIC_CD]"
                                            + "[VISIT_DATE]"
                                            + coverageBeanlist.get(i).getVisitDate()
                                            + "[/VISIT_DATE]"
                                            + "[/TRAINING_TOPIC_NEW_EMPLOYEE_DATA]";

                                    final_xml = final_xml + onXML;

                                }

                                final String audit_xml = "[DATA]" + final_xml + "[/DATA]";

                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", audit_xml);
                                request.addProperty("KEYS", "TRAINING_TOPIC_NEW_EMPLOYEE_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }

                                data.value = 40;
                                data.name = "TRAINING T N EMPLOYEE DATA";
                                publishProgress(data);

                            }


                            //		uploading Audit data for New Employee
                            final_xml = "";
                            onXML = "";
                            database.open();
                            auditData = database.getAuditInsertedNewEmpData(coverageBeanlist.get(i).getStoreId());
                            if (auditData.size() > 0) {
                                for (int j = 0; j < auditData.size(); j++) {
                                    onXML = "[AUDIT_DATA_NEW_EMPLOYEE][ISD_CD]"
                                            + auditData.get(j).getIsd_cd()
                                            + "[/ISD_CD]"
                                            + "[MID]"
                                            + mid
                                            + "[/MID]"
                                            + "[CREATED_BY]"
                                            + username
                                            + "[/CREATED_BY]"
                                            + "[KEY_ID]"
                                            + auditData.get(j).getKey_id()
                                            + "[/KEY_ID]"
                                            + "[CHECKLIST_CD]"
                                            + auditData.get(j).getCHECKLIST_CD().get(0)
                                            + "[/CHECKLIST_CD]"
                                            + "[AVAILABILITY]"
                                            + auditData.get(j).getAvailability()
                                            + "[/AVAILABILITY]"
                                            + "[/AUDIT_DATA_NEW_EMPLOYEE]";

                                    final_xml = final_xml + onXML;

                                }

                                final String audit_xml = "[DATA]" + final_xml + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", audit_xml);
                                request.addProperty("KEYS", "AUDIT_DATA_NEW_EMPLOYEE");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }

                                if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                data.value = 50;
                                data.name = "AUDIT DATA NEW EMPLOYEE";
                                publishProgress(data);
                            }


                            //		uploading TRAINING_TOPIC_DATA
                            final_xml = "";
                            onXML = "";
                            database.open();
                            CoverageBean getremarkData = database.getremarkData(coverageBeanlist.get(i).getStoreId(), coverageBeanlist.get(i).getVisitDate());
                            if (getremarkData != null && getremarkData.getRemark() != null) {
                                onXML = "[REMARK_DATA]"
                                        + "[MID]"
                                        + mid
                                        + "[/MID]"
                                        + "[CREATED_BY]"
                                        + username
                                        + "[/CREATED_BY]"
                                        + "[REMARK]"
                                        + getremarkData.getRemark()
                                        + "[/REMARK]"
                                        + "[VISIT_DATE]"
                                        + coverageBeanlist.get(i).getVisitDate()
                                        + "[/VISIT_DATE]"
                                        + "[/REMARK_DATA]";

                                final_xml = final_xml + onXML;


                                final String audit_xml = "[DATA]" + final_xml + "[/DATA]";

                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", audit_xml);
                                request.addProperty("KEYS", "REMARK_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }

                                data.value = 60;
                                data.name = "REMARK DATA";
                                publishProgress(data);
                            }


                            //		uploading TRAINING_TOPIC_DATA
                            final_xml = "";
                            onXML = "";
                            database.open();
                            trainingTopicList = database.getTrainningTopicData(coverageBeanlist.get(i).getStoreId());
                            if (trainingTopicList.size() > 0) {
                                for (int j = 0; j < trainingTopicList.size(); j++) {
                                    onXML = "[TRAINING_TOPIC_DATA][ISD_CD]"
                                            + trainingTopicList.get(j).getIsd_cd()
                                            + "[/ISD_CD]"
                                            + "[MID]"
                                            + mid
                                            + "[/MID]"
                                            + "[CREATED_BY]"
                                            + username
                                            + "[/CREATED_BY]"
                                            + "[KEY_ID]"
                                            + "0"
                                            + "[/KEY_ID]"
                                            + "[ISD_CD]"
                                            + trainingTopicList.get(j).getIsd_cd()
                                            + "[/ISD_CD]"
                                            + "[TOPIC_CD]"
                                            + trainingTopicList.get(j).getTOPIC_CD().get(0)
                                            + "[/TOPIC_CD]"

                                            + "[ISD_IMAGE]"
                                            + trainingTopicList.get(j).getIsd_image()
                                            + "[/ISD_IMAGE]"
                                            + "[VISIT_DATE]"
                                            + coverageBeanlist.get(i).getVisitDate()
                                            + "[/VISIT_DATE]"
                                            + "[/TRAINING_TOPIC_DATA]";

                                    final_xml = final_xml + onXML;

                                }

                                final String audit_xml = "[DATA]" + final_xml + "[/DATA]";

                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", audit_xml);
                                request.addProperty("KEYS", "TRAINING_TOPIC_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }

                                data.value = 70;
                                data.name = "TRAINING TOPIC DATA";
                                publishProgress(data);
                            }


                            //		uploading ISD SKILLLLL DATA
                            final_xml = "";
                            onXML = "";
                            database.open();
                            auditData = database.getAuditInsertedData(coverageBeanlist.get(i).getStoreId());
                            if (auditData.size() > 0) {
                                for (int j = 0; j < auditData.size(); j++) {
                                    onXML = "[AUDIT_DATA][ISD_CD]"
                                            + auditData.get(j).getIsd_cd()
                                            + "[/ISD_CD]"
                                            + "[MID]"
                                            + mid
                                            + "[/MID]"
                                            + "[CREATED_BY]"
                                            + username
                                            + "[/CREATED_BY]"
                                            + "[CHECKLIST_CD]"
                                            + auditData.get(j).getCHECKLIST_CD().get(0)
                                            + "[/CHECKLIST_CD]"
                                            + "[AVAILABILITY]"
                                            + auditData.get(j).getAvailability()
                                            + "[/AVAILABILITY]"
                                            + "[/AUDIT_DATA]";
                                    final_xml = final_xml + onXML;

                                }
                                final String audit_xml = "[DATA]" + final_xml + "[/DATA]";
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", audit_xml);
                                request.addProperty("KEYS", "AUDIT_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", mid);
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                data.value = 80;
                                data.name = "AUDIT DATA";
                                publishProgress(data);
                            }


                            database.open();
                            geodata = database.getGeotaggingData(coverageBeanlist.get(i).getStoreId());
                            if (geodata.size() > 0) {
                                final_xml = "";
                                onXML = "";
                                for (int i1 = 0; i1 < geodata.size(); i1++) {
                                    onXML = "[DATA][USER_DATA][STORE_ID]"
                                            + Integer.parseInt(geodata.get(i1).getStoreId())
                                            + "[/STORE_ID]"
                                            + "[USERNAME]"
                                            + username
                                            + "[/USERNAME]"
                                            + "[Image1]"
                                            + geodata.get(i1).getUrl1()
                                            + "[/Image1][Latitude]"
                                            + Double.toString(geodata.get(i1).getLatitude())
                                            + "[/Latitude][Longitude]"
                                            + Double.toString(geodata.get(i1).getLongitude())
                                            + "[/Longitude][/USER_DATA][/DATA]";
                                    final_xml = onXML;
                                    request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                    request.addProperty("XMLDATA", final_xml);
                                    request.addProperty("KEYS", "GeoXML");
                                    request.addProperty("USERNAME", username);
                                    envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                    envelope.dotNet = true;
                                    envelope.setOutputSoapObject(request);
                                    androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                    androidHttpTransport.call(CommonString.NAMESPACE + CommonString.METHOD_UPLOAD_XML, envelope);
                                    result = (Object) envelope.getResponse();
                                    if (result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    }
                                }
                                data.value = 85;
                                data.name = "GeoXML DATA";
                                publishProgress(data);
                            }


                            ///uploading SALES_TEAM_TRAINEE_DATA
                            final_xml = "";
                            onXML = "";
                            database.open();
                            traineCDataList = database.getSALETEAMTRaineeInsertedDATA(coverageBeanlist.get(i).getVisitDate());
                            if (traineCDataList.size() > 0) {
                                boolean status = false;
                                for (int j = 0; j < traineCDataList.size(); j++) {
                                    if (traineCDataList.get(j).getStaus().equalsIgnoreCase("N")) {
                                        status = true;
                                        onXML = "[SALES_TEAM_TRAINEE_DATA][SALES_TEAM_TRAINEE_CD]"
                                                + traineCDataList.get(j).getTrainee_cd()
                                                + "[/SALES_TEAM_TRAINEE_CD]"
                                                + "[CREATED_BY]"
                                                + username
                                                + "[/CREATED_BY]"
                                                + "[TOPIC_CD]"
                                                + traineCDataList.get(j).getTOPIC_CD().get(0)
                                                + "[/TOPIC_CD]"
                                                + "[VISIT_DATE]"
                                                + coverageBeanlist.get(i).getVisitDate()
                                                + "[/VISIT_DATE]"
                                                + "[MID]"
                                                + "0"
                                                + "[/MID]"
                                                + "[/SALES_TEAM_TRAINEE_DATA]";
                                        final_xml = final_xml + onXML;
                                    }
                                }

                                if (status) {
                                    final String audit_xml = "[DATA]" + final_xml + "[/DATA]";
                                    request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                    request.addProperty("XMLDATA", audit_xml);
                                    request.addProperty("KEYS", "SALES_TEAM_TRAINEE_DATA");
                                    request.addProperty("USERNAME", username);
                                    request.addProperty("MID", "0");
                                    envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                    envelope.dotNet = true;
                                    envelope.setOutputSoapObject(request);
                                    androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                    androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                    result = (Object) envelope.getResponse();
                                    if (result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                        database.updateSaleTeamTraineeStatus(coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_U);
                                    }
                                    if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                                        return CommonString.METHOD_UPLOAD_XML;
                                    }
                                    if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                                        return CommonString.METHOD_UPLOAD_XML;
                                    }
                                    data.value = 90;
                                    data.name = "SALES_TEAM_TRAINEE_DATA";
                                    publishProgress(data);
                                }
                            }

                            ///uploading SALES_TEAM_TRAINEE_DATA
                            final_xml = "";
                            onXML = "";
                            database.open();
                            gatemeetingManagerGetterSetter = database.getGatemeetingManagerData(coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_N);
                            if (!gatemeetingManagerGetterSetter.getLocation_text().equals("")) {
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
                                request = new SoapObject(CommonString.NAMESPACE, CommonString.METHOD_UPLOAD_XML);
                                request.addProperty("XMLDATA", employee_xml);
                                request.addProperty("KEYS", "GATE_MEETING_DATA");
                                request.addProperty("USERNAME", username);
                                request.addProperty("MID", "0");
                                envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                                envelope.dotNet = true;
                                envelope.setOutputSoapObject(request);
                                androidHttpTransport = new HttpTransportSE(CommonString.URL);
                                androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.METHOD_UPLOAD_XML, envelope);
                                result = (Object) envelope.getResponse();
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                    database.updateSaleTeamTraineeStatus(coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_U);
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                                    return CommonString.METHOD_UPLOAD_XML;
                                }
                                data.value = 91;
                                data.name = "GATE_MEETING_DATA";
                                publishProgress(data);
                            }




                            ArrayList<GeotaggingBeans> geolist = database.getGeotaggingDataforima(coverageBeanlist.get(i).getStoreId());
                            if (geolist.size() > 0) {
                                for (int k = 0; k < geolist.size(); k++) {
                                    if (geolist.get(k).getUrl1() != null && !geolist.get(k).getUrl1().equals("")) {
                                        if (new File(CommonString.FILE_PATH + geolist.get(k).getUrl1()).exists()) {
                                            result = UploadImage(geolist.get(k).getUrl1(), "GeoTagImage");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                return "GeoTagImages";
                                            }
                                            runOnUiThread(new Runnable() {
                                                public void run() {
                                                    message.setText("GeoTagImages Uploaded");
                                                }
                                            });
                                        }
                                    }
                                }
                                data.value = 92;
                                data.name = "GeoTagImages";
                                publishProgress(data);
                            }

                            //Uploading store Images
                            if (coverageBeanlist.get(i).getImage() != null && !coverageBeanlist.get(i).getImage().equals("")) {
                                if (new File(CommonString.FILE_PATH + coverageBeanlist.get(i).getImage()).exists()) {
                                    result = UploadImage(coverageBeanlist.get(i).getImage(), "StoreImages");
                                    if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                        return "StoreImages";
                                    }
                                    runOnUiThread(new Runnable() {

                                        public void run() {
                                            message.setText("Store Image Uploaded");
                                        }
                                    });
                                    data.value = 94;
                                    data.name = "StoreImages";
                                    publishProgress(data);
                                }
                            }

                            if (newEmpData.size() > 0) {
                                for (int m = 0; m < newEmpData.size(); m++) {
                                    if (newEmpData.get(m).getImage() != null && !newEmpData.get(m).getImage().equals("")) {
                                        if (new File(CommonString.FILE_PATH + newEmpData.get(m).getImage()).exists()) {
                                            result = UploadImage(newEmpData.get(m).getImage(), "IsdImages");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                return "IsdImages";
                                            }
                                            runOnUiThread(new Runnable() {

                                                public void run() {
                                                    message.setText("New Employee Image Uploaded");
                                                }
                                            });
                                            data.value = 95;
                                            data.name = "IsdImages";
                                            publishProgress(data);
                                        }
                                    }
                                }
                            }

                            if (trainingTopicList.size() > 0) {
                                for (int m = 0; m < trainingTopicList.size(); m++) {
                                    if (!trainingTopicList.get(m).getIsd_image().equals("")) {
                                        if (new File(CommonString.FILE_PATH + trainingTopicList.get(m).getIsd_image()).exists()) {
                                            result = UploadImage(trainingTopicList.get(m).getIsd_image(), "IsdImages");
                                            if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                                return "IsdImages";
                                            }
                                            runOnUiThread(new Runnable() {

                                                public void run() {
                                                    message.setText("Isd Image Uploaded");
                                                }
                                            });
                                            data.value = 95;
                                            data.name = "IsdImages";
                                            publishProgress(data);
                                        }
                                    }
                                }
                            }

                            if (!gatemeetingManagerGetterSetter.getLocation_text().equals("")) {
                                if (!gatemeetingManagerGetterSetter.getTeam_picure1().equals("")) {
                                    if (new File(CommonString.FILE_PATH + gatemeetingManagerGetterSetter.getTeam_picure1()).exists()) {
                                        result = UploadImage(gatemeetingManagerGetterSetter.getTeam_picure1(), gatemeetingfoldername);
                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            return gatemeetingfoldername;
                                        }
                                        runOnUiThread(new Runnable() {

                                            public void run() {
                                                message.setText("Gate Meeting Image Uploaded");
                                            }
                                        });
                                        data.value = 96;
                                        data.name = "GateMeetingImages";
                                        publishProgress(data);
                                    }
                                }

                                if (!gatemeetingManagerGetterSetter.getTeam_picure2().equals("")) {
                                    if (new File(CommonString.FILE_PATH + gatemeetingManagerGetterSetter.getTeam_picure2()).exists()) {
                                        result = UploadImage(gatemeetingManagerGetterSetter.getTeam_picure2(), gatemeetingfoldername);
                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            return gatemeetingfoldername;
                                        }
                                        runOnUiThread(new Runnable() {

                                            public void run() {
                                                message.setText("Gate Meeting Image Uploaded");
                                            }
                                        });
                                        data.value = 97;
                                        data.name = "GateMeetingImages";
                                        publishProgress(data);
                                    }
                                }
                                if (!gatemeetingManagerGetterSetter.getTeam_picure3().equals("")) {
                                    if (new File(CommonString.FILE_PATH + gatemeetingManagerGetterSetter.getTeam_picure3()).exists()) {
                                        result = UploadImage(gatemeetingManagerGetterSetter.getTeam_picure3(), gatemeetingfoldername);
                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            return gatemeetingfoldername;
                                        }
                                        runOnUiThread(new Runnable() {

                                            public void run() {
                                                message.setText("Gate Meeting Image Uploaded");
                                            }
                                        });
                                        data.value = 98;
                                        data.name = "GateMeetingImages";
                                        publishProgress(data);
                                    }
                                }
                                if (!gatemeetingManagerGetterSetter.getTeam_picure4().equals("")) {
                                    if (new File(CommonString.FILE_PATH + gatemeetingManagerGetterSetter.getTeam_picure4()).exists()) {
                                        result = UploadImage(gatemeetingManagerGetterSetter.getTeam_picure4(), gatemeetingfoldername);
                                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                                            return gatemeetingfoldername;
                                        }
                                        runOnUiThread(new Runnable() {

                                            public void run() {
                                                message.setText("Gate Meeting Image Uploaded");
                                            }
                                        });
                                        data.value = 99;
                                        data.name = "GateMeetingImages";
                                        publishProgress(data);
                                    }
                                }
                            }
                        }

                        data.value = 99;
                        data.name = "COVERAGE_STATUS";
                        publishProgress(data);


                        // SET COVERAGE STATUS

                        String final_xml = "";
                        String onXML = "";
                        onXML = "[COVERAGE_STATUS][STORE_ID]"
                                + coverageBeanlist.get(i).getStoreId()
                                + "[/STORE_ID]"
                                + "[VISIT_DATE]"
                                + coverageBeanlist.get(i).getVisitDate()
                                + "[/VISIT_DATE]"
                                + "[USER_ID]"
                                + coverageBeanlist.get(i).getUserId()
                                + "[/USER_ID]"
                                + "[STATUS]"
                                + CommonString.KEY_U
                                + "[/STATUS]"
                                + "[/COVERAGE_STATUS]";

                        final_xml = final_xml + onXML;

                        final String sos_xml = "[DATA]" + final_xml
                                + "[/DATA]";

                        SoapObject request = new SoapObject(CommonString.NAMESPACE, CommonString.MEHTOD_UPLOAD_COVERAGE_STATUS);
                        request.addProperty("onXML", sos_xml);
                        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
                        envelope.dotNet = true;
                        envelope.setOutputSoapObject(request);
                        HttpTransportSE androidHttpTransport = new HttpTransportSE(CommonString.URL);
                        androidHttpTransport.call(CommonString.SOAP_ACTION + CommonString.MEHTOD_UPLOAD_COVERAGE_STATUS, envelope);
                        Object result = (Object) envelope.getResponse();
                        if (!result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                            return CommonString.MEHTOD_UPLOAD_COVERAGE_STATUS;
                        }
                        if (result.toString().equalsIgnoreCase(CommonString.KEY_NO_DATA)) {
                            return CommonString.MEHTOD_UPLOAD_COVERAGE_STATUS;
                        }
                        if (result.toString().equalsIgnoreCase(CommonString.KEY_FAILURE)) {
                            return CommonString.MEHTOD_UPLOAD_COVERAGE_STATUS;
                        }
                        database.open();
                        database.updateCoverageStatus(coverageBeanlist.get(i).getMID(), CommonString.KEY_U);
                        database.updateStoreStatusOnLeave(coverageBeanlist.get(i).getStoreId(), coverageBeanlist.get(i).getVisitDate(), CommonString.KEY_U);

                        data.value = 100;
                        data.name = "COVERAGE STATUS UPDATING";
                        publishProgress(data);

                    }
                } else {
                    return CommonString.KEY_SUCCESS;
                }
            } catch (MalformedURLException e) {
                dialog.dismiss();
                up_success_flag = false;
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_EXCEPTION);
                    }
                });

            } catch (IOException e) {
                dialog.dismiss();
                up_success_flag = false;
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_SOCKETEXCEPTION);
                    }
                });

            } catch (Exception e) {
                dialog.dismiss();
                up_success_flag = false;


                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        showMessage(CommonString.MESSAGE_EXCEPTION);
                    }
                });

            }
            if (up_success_flag == true) {
                return CommonString.KEY_SUCCESS;
            } else {
                return CommonString.KEY_FAILURE;
            }
        }

        @Override
        protected void onProgressUpdate(Data... values) {
            // TODO Auto-generated method stub
            pb.setProgress(values[0].value);
            percentage.setText(values[0].value + "%");
            message.setText(values[0].name);
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            dialog.dismiss();
            if (result.equals(CommonString.KEY_SUCCESS)) {
                database.open();
                database.deleteAllTables();
                showMessage(CommonString.MESSAGE_UPLOAD_DATA);
            } else {
                showMessage("Error in Upload :- " + " " + result);
            }
        }
    }

    class Data {
        int value;
        String name;
    }

    public String getCurrentTime() {
        Calendar m_cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        String cdate = formatter.format(m_cal.getTime());
        return cdate;

    }

    private class BackgroundTask extends AsyncTask<Void, Data, String> {
        private Context context;

        BackgroundTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            dialog = new Dialog(context);
            dialog.setContentView(R.layout.custom);
            dialog.setTitle("Sending Checkout Data");
            dialog.setCancelable(false);
            dialog.show();
            pb = (ProgressBar) dialog.findViewById(R.id.progressBar1);
            percentage = (TextView) dialog.findViewById(R.id.percentage);
            message = (TextView) dialog.findViewById(R.id.message);
        }

        @SuppressWarnings("deprecation")
        @Override
        protected String doInBackground(Void... params) {
            // TODO Auto-generated method stub

            try {

                data = new Data();
                data.value = 20;
                data.name = "Checked out Data Uploading";
                publishProgress(data);
                if (coverageBean.size() > 0) {
                    String onXML = "[STORE_CHECK_OUT_STATUS][USER_ID]"
                            + username
                            + "[/USER_ID]" + "[STORE_ID]"
                            + coverageBean.get(0).getStoreId()
                            + "[/STORE_ID][LATITUDE]"
                            + coverageBean.get(0).getLatitude()
                            + "[/LATITUDE][LOGITUDE]"
                            + coverageBean.get(0).getLongitude()
                            + "[/LOGITUDE][CHECKOUT_DATE]"
                            + coverageBean.get(0).getVisitDate()
                            + "[/CHECKOUT_DATE][CHECK_OUTTIME]"
                            + getCurrentTime()
                            + "[/CHECK_OUTTIME][CHECK_INTIME]"
                            + coverageBean.get(0).getInTime()
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
                    if (result.toString().equalsIgnoreCase(CommonString.KEY_SUCCESS)) {
                        database.updateCoverageStoreOutTime(coverageBean.get(0).getStoreId(), coverageBean.get(0).getVisitDate(), coverageBean.get(0).getInTime(), CommonString.KEY_C);
                        database.updateStoreStatusOnCheckout(coverageBean.get(0).getStoreId(), coverageBean.get(0).getVisitDate(), CommonString.KEY_C);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(CommonString.KEY_STORE_CD, "");
                        editor.commit();
                        data.value = 100;
                        data.name = "Checkout Done";
                        publishProgress(data);
                        return CommonString.KEY_SUCCESS;
                    } else {
                        return "Upload_Store_ChecOut_Status";
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
                        showMessage(CommonString.MESSAGE_SOCKETEXCEPTION);
                        // TODO Auto-generated method stub
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
            return "";
        }

        @Override
        protected void onProgressUpdate(Data... values) {
            // TODO Auto-generated method stub
            pb.setProgress(values[0].value);
            percentage.setText(values[0].value + "%");
            message.setText(values[0].name);
        }

        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            super.onPostExecute(result);
            dialog.dismiss();
            if (result.equals(CommonString.KEY_SUCCESS)) {
                new UploadDataTask(CheckoutNUpload.this).execute();
            } else if (!result.equals("")) {
                Toast.makeText(getApplicationContext(), "Network Error Try Again", Toast.LENGTH_SHORT).show();
                finish();

            }

        }

    }
}
