package ranking_system;

public class RankingFactory {

	public Calculator getRankingCalculator(String function) {
		if(function.equalsIgnoreCase("dummyRanking")) {
			return new RankingDummy();
		} else if(function.equalsIgnoreCase("rankPerNgramScore")) {
			return new RankingPerNgramScore();
		} else if(function.equalsIgnoreCase("rankPrototype")) {
			return new RankingPrototype();
		} else if(function.equalsIgnoreCase("rankOverlappingNgrams")) {
			return new RankingOverlappingNgrams();
		} else if(function.equalsIgnoreCase("RankingDifferentInstructions")) {
			return new RankingDifferentInstructions();
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	
}
