package main;

public class RankingConfig {
	public RankingConfig(int limit, String ranker) {
		this.limit = limit;
		this.ranker = ranker;
	}
	
	public String ranker;
	public int limit;
}
