package ranking_system;

import java.util.List;
import java.util.Random;

import converters.ngrams.Ngram;
import main.Config;
import main.RankingConfig;

public class RankingSystemFacade {
	public List<Ngram> rankingAction(List<Ngram> ngrams, Config config) {
		
		/*
		 * Step 5
		 * Ranking System:
		 * 
		 */
		RankingSystem rank = new RankingSystem(ngrams);
		/*
		 * This is not random! And there is no need for randomness, since we only
		 * want shuffling. This seed is used to get the possibility of reproducibility.
		 * You can take the values from the output folder for recreation of the rules. (theory, to be tested)
		 */
		rank.randomize(new Random(config.shuffle_seed));
		
		int rankerCounter = 0;
		for(RankingConfig c: config.getRankingConfig()) {
			ngrams = rank.rank(c.limit, c.ranker);
			
			if(rankerCounter == 0) {
				//ngrams = rank.initMetaData(ngrams);
				rank.setNgramList(ngrams);
			}
			
			rankerCounter++;
		}
		return ngrams;
	}
}
