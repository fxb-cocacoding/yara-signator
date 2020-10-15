package main;

import java.util.ArrayList;
import java.util.LinkedList;

public class Config {
	
	/*
	 * This file is a container for all elements of the config file.
	 * Do not change anything without thinking. This requires a change in
	 * every config file.
	 * 
	 * Do not implement any methods in this class. This container is build by gson
	 * library at start-up.
	 */
	
	public String smda_path = "/absolute/path/to/your/smda_reports/";
	public String malpedia_path = "/absolute/path/to/your/git/copy/of/malpedia/";
	public String output_path = "/absolute/path/to/your/output/";
	public String yaraBinary = "/usr/bin/yara";
	public String yaracBinary = "/usr/bin/yarac";
	public String malpediaEvalScript = "";
	
	public String malpediaVersioningFile = "";
	
	
	public String malpediaEvalScriptOutput = "/tmp/95268496.json";
	public String resumeFolder = "";
	
	public String db_connection_string = "jdbc:postgresql://127.0.0.1/";
	public String db_user = "postgres";
	public String db_password = "";
	public String db_name = "caching_db";
	
	public String yara_signator_version = "0.4.0";
	
	public boolean skipSMDAInsertions = false;
	public boolean skipYaraRuleGeneration = false;
	public boolean skipUniqueNgramTableCreation = false;
	public boolean skipRuleValidation = false;
	public boolean skipNextGen = false;
	
	public boolean makeBlacklistPersistent = true;
	
	public int insertion_threads = 16;
	public int rulebuilder_threads = 8;
	
	public int shuffle_seed;
	public long minInstructions = 10;
	public int batchSize = 5000;
	public int instructionLimitPerFamily = 750000;
	public int ng_recursion_limit = 5;
	
	public boolean reportStatistics = true;
	public String reportFileName = "report.csv";
	
	public boolean duplicatesInsideSamplesEnabled = false;
	public boolean wildcardConfigEnabled = true;
	public boolean rankingOptimizerEnabled = true;
	public boolean scoreCommentEnabled = true;
	public boolean prettifyEnabled = true;
	
	public boolean reduceInputForDebugging = false;
	public boolean permitOverlappingNgrams = true;
	
	private LinkedList<WildcardConfig> wildcardConfig = new LinkedList<>();
	private LinkedList<RankingConfig> rankingConfig = new LinkedList<>();
	private LinkedList<NextGenConfig> nextGenConfig = new LinkedList<>();
	private ArrayList<Integer> n = new ArrayList<>();

	public LinkedList<NextGenConfig> getNextGenConfig() {
		return nextGenConfig;
	}
	public void setNextGenConfig(LinkedList<NextGenConfig> nextGenConfig) {
		this.nextGenConfig = nextGenConfig;
	}
	public LinkedList<RankingConfig> getRankingConfig() {
		return rankingConfig;
	}
	public void addRanker(RankingConfig e) {
		rankingConfig.add(e);
	}
	public void setRankingConfig(LinkedList<RankingConfig> rankingConfig) {
		this.rankingConfig = rankingConfig;
	}
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("smda path:     " + this.smda_path + "\n");
		sb.append("malpedia path: " + this.malpedia_path + "\n");
		return sb.toString();
	}
	public ArrayList<Integer> getNs() {
		// TODO Auto-generated method stub
		return this.n;
	}
	public void addN(int i) {
		this.n.add(i);
	}


	public LinkedList<WildcardConfig> getWildcardConfigConfig() {
		return wildcardConfig;
	}
	public void setWildcardConfigConfig(LinkedList<WildcardConfig> wildcardConfig) {
		this.wildcardConfig = wildcardConfig;
	}

}
