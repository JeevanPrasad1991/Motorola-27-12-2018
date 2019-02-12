package cpm.com.motorola.xmlgettersetter;

/**
 * Created by jeevanp on 01-04-2016.
 */
public class GateMettingGetterSetter {
    String key_id = "";
    String latitude="0.0",longitue="0.0";

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitue() {
        return longitue;
    }

    public void setLongitue(String longitue) {
        this.longitue = longitue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    String status = "N";

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    String remark="";
    String visit_date = "";
    String team_picure1 = "";
    String team_picure2 = "";
    String team_picure3 = "";
    String team_picure4 = "";

    public String getTeam_picure4() {
        return team_picure4;
    }

    public void setTeam_picure4(String team_picure4) {
        this.team_picure4 = team_picure4;
    }

    public String getLocation_text() {
        return location_text;
    }

    public void setLocation_text(String location_text) {
        this.location_text = location_text;
    }

    String location_text = "";

    public String getKey_id() {
        return key_id;
    }

    public void setKey_id(String key_id) {
        this.key_id = key_id;
    }

    public String getVisit_date() {
        return visit_date;
    }

    public void setVisit_date(String visit_date) {
        this.visit_date = visit_date;
    }

    public String getTeam_picure1() {
        return team_picure1;
    }

    public void setTeam_picure1(String team_picure1) {
        this.team_picure1 = team_picure1;
    }

    public String getTeam_picure2() {
        return team_picure2;
    }

    public void setTeam_picure2(String team_picure2) {
        this.team_picure2 = team_picure2;
    }

    public String getTeam_picure3() {
        return team_picure3;
    }

    public void setTeam_picure3(String team_picure3) {
        this.team_picure3 = team_picure3;
    }
}
