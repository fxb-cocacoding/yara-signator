package main;

public enum Versioning {
	INSTANCE;
	
	/*
	 * ToDo:
	 * 
	 * Generate the versions from environment.
	 */
	
	
	public String VERSION = "0.5.0";
	public String MALPEDIA_COMMIT = "0000000000000000000000000000000000000000";
	
	public String MALPEDIA_DATE = "00000000";
	
	public String MALPEDIA_COMMIT_EIGHT = MALPEDIA_COMMIT.substring(0, 8);
}
