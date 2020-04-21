package main;

import java.util.LinkedList;

public class NextGenConfig {
	public String nextGenOperator = "";
	public String yara_condition = "";
	public int yara_condition_limit = 0;
	public int rounds = 0;
	public boolean permitOverlappingNgrams = true;
	private LinkedList<RankingConfig> rankingConfig = new LinkedList<>();
	public NextGenBreakout nextGenBreakout = new NextGenBreakout();
	
	@Override
	public String toString() {
		return "NextGenConfig [nextGenOperator=" + nextGenOperator + ", yara_condition=" + yara_condition
				+ ", yara_condition_limit=" + yara_condition_limit + ", rounds=" + rounds + ", permitOverlappingNgrams="
				+ permitOverlappingNgrams + ", rankingConfig=" + rankingConfig.toString() + ", nextGenBreakout=" + nextGenBreakout.toString()
				+ "]";
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

}
