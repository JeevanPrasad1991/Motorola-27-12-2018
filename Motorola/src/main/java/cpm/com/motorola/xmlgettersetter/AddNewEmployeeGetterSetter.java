package cpm.com.motorola.xmlgettersetter;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by yadavendras on 09-09-2016.
 */
public class AddNewEmployeeGetterSetter implements Parcelable {

    String name;
    String email;
    String phone;
    String key_id;

    public String getManneged() {
        return manneged;
    }

    public void setManneged(String manneged) {
        this.manneged = manneged;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    String manneged = "";
    String image = "";
    boolean isIsd = false;

    public AddNewEmployeeGetterSetter() {

    }

    public AddNewEmployeeGetterSetter(String name, String email, String phone, boolean isIsd) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.isIsd = isIsd;
    }

    public AddNewEmployeeGetterSetter(Parcel in) {
        name = in.readString();
        email = in.readString();
        phone = in.readString();
        isIsd = in.readByte() != 0;
    }

    public static final Creator<AddNewEmployeeGetterSetter> CREATOR = new Creator<AddNewEmployeeGetterSetter>() {
        @Override
        public AddNewEmployeeGetterSetter createFromParcel(Parcel in) {
            return new AddNewEmployeeGetterSetter(in);
        }

        @Override
        public AddNewEmployeeGetterSetter[] newArray(int size) {
            return new AddNewEmployeeGetterSetter[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(email);
        dest.writeString(phone);
        dest.writeByte((byte) (isIsd ? 1 : 0));
    }

    public boolean isIsd() {
        return isIsd;
    }

    public void setIsIsd(boolean isIsd) {
        this.isIsd = isIsd;
    }

    public String getKey_id() {
        return key_id;
    }

    public void setKey_id(String key_id) {
        this.key_id = key_id;
    }

    String isd_cd;

    public String getIsd_cd() {
        return isd_cd;
    }

    public void setIsd_cd(String isd_cd) {
        this.isd_cd = isd_cd;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    String mid;
}
