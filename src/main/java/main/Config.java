package main;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
	
	public String db_connection_string = "jdbc:postgresql://127.0.0.1/";
	public String db_user = "postgres";
	public String db_password = "";
	public String db_name = "caching_db";
	
	public boolean skipSMDAInsertions = false;
	public boolean skipYaraRuleGeneration = false;
	public boolean skipUniqueNgramTableCreation = false;
	public boolean skipRuleValidation = false;
	
	public int insertion_threads = 16;
	public int rulebuilder_threads = 8;
	
	public int shuffle_seed;
	public long minInstructions = 10;
	public int batchSize = 5000;
	public int instructionLimitPerFamily = 750000;
	
	public boolean duplicatesInsideSamplesEnabled = false;
	public boolean wildcardConfigEnabled = true;
	public boolean rankingOptimizerEnabled = true;
	public boolean scoreCommentEnabled = true;
	public boolean prettifyEnabled = true;
	
	private LinkedList<WildcardConfig> wildcardConfig = new LinkedList<>();
	private LinkedList<RankingConfig> rankingConfig = new LinkedList<>();
	private ArrayList<Integer> n = new ArrayList<>();

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


	/*
	 * Deprecated mongo constants
	 */
	public int mongoQueryBufferSize = 20000;
	
}
