package cpm.com.motorola.xmlgettersetter;

public class CoverageBean {
	protected int MID;
	protected String storeId;
	protected String training_mode_cd;


	protected String Remark;
	
	public String getRemark() {
		return Remark;
	}

	public void setRemark(String remark) {
		Remark = remark;
	}
	protected String userId;
	
	protected String inTime;
	
	protected String outTime;
	
	protected String visitDate;

	private String latitude;
	
	private String longitude;
	
	private String reasonid="";
	private String reason="";
	
	private String status="N";
	
	private String image="";
	

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getMID() {
		return MID;
	}

	public void setMID(int mID) {
		MID = mID;
	}

	public String getStoreId() {
		return storeId;
	}

	public void setStoreId(String storeId) {
		this.storeId = storeId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getInTime() {
		return inTime;
	}

	public void setInTime(String inTime) {
		this.inTime = inTime;
	}

	public String getOutTime() {
		return outTime;
	}

	public void setOutTime(String outTime) {
		this.outTime = outTime;
	}

	public String getVisitDate() {
		return visitDate;
	}

	public void setVisitDate(String visitDate) {
		this.visitDate = visitDate;
	}


	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getReasonid() {
		return reasonid;
	}

	public void setReasonid(String reasonid) {
		this.reasonid = reasonid;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getTraining_mode_cd() {
		return training_mode_cd;
	}

	public void setTraining_mode_cd(String training_mode_cd) {
		this.training_mode_cd = training_mode_cd;
	}
}
