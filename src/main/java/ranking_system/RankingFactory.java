package ranking_system;

import java.util.List;

import converters.ngrams.Ngram;

public class RankingFactory {

	public Calculator getRankingCalculator(String function) {
		if(function.equalsIgnoreCase("dummyRanking")) {
			return new RankingDummy();
		} else if(function.equalsIgnoreCase("rankPerNgramScore")) {
			return new RankingPerNgramScore();
		} else if(function.equalsIgnoreCase("rankPrototype")) {
			return new RankingPrototype();
		} else if(function.equalsIgnoreCase("RankingDifferentInstructions")) {
			return new RankingDifferentInstructions();
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	
}
