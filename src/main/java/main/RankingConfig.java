package main;

public class RankingConfig {
	@Override
	public String toString() {
		return "RankingConfig [ranker=" + ranker + ", limit=" + limit + "]";
	}
	public RankingConfig(int limit, String ranker) {
		this.limit = limit;
		this.ranker = ranker;
	}
	
	public String ranker;
	public int limit;
}
