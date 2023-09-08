public class Course {
	private int id;
	private String name;
	private int account_id;
	private String uuid;
	private int gradingStandardID;
	private boolean isPublic;
	private String courseCode;

	public int getGradingStandardID() {
		return gradingStandardID;
	}

	public void setGradingStandardID(int gradingStandardID) {
		this.gradingStandardID = gradingStandardID;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setPublic(boolean aPublic) {
		isPublic = aPublic;
	}

	public String getCourseCode() {
		return courseCode;
	}

	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAccount_id() {
		return account_id;
	}

	public void setAccount_id(int account_id) {
		this.account_id = account_id;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
