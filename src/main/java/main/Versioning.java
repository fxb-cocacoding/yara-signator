package main;

public enum Versioning {
	INSTANCE;
	
	/*
	 * ToDo:
	 * 
	 * Generate the versions from environment.
	 */
	
	
	public String VERSION = "0.3.1";
	public String MALPEDIA_COMMIT = "7417966c1d6f23c4f25b351209781b60086b6f9e";
	
	public String MALPEDIA_COMMIT_EIGHT = MALPEDIA_COMMIT.substring(0, 8);
}
