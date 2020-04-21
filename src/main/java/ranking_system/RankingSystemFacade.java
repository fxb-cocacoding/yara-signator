package ranking_system;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converters.ngrams.Ngram;
import main.Config;
import main.RankingConfig;

public class RankingSystemFacade {
	private static final Logger logger = LoggerFactory.getLogger(RankingSystemFacade.class);
	
	public List<Ngram> rankingAction(List<Ngram> ngrams, Config config, LinkedList<RankingConfig> rankers) {
		
		
		/*
		//this treeset is ordered by the score, works only when the ranker is only rankByNgramScore.
		HashSet<Ngram> ngramDuplicatesChecker = new HashSet<Ngram>();
		
		for(Ngram ngram : ngrams) {
			boolean ret = ngramDuplicatesChecker.add(ngram);
			if(ret == false) {
				logger.info("Duplicates detected in " + ngram.toString());
				logger.info("contains?: " + ngramDuplicatesChecker.contains(ngram));
				logger.info("hash: " + ngram.hashCode());
				logger.info("");
				for(Ngram ngr : ngramDuplicatesChecker) {
					if(ngr.equals(ngram)) {
						logger.info("Duplicate detected: ");
						logger.info("ngr: " + ngr.toString());
						logger.info("ngram: " + ngram.toString());
						throw new NullPointerException();
					}
				}
				logger.info("COULD NOT BE FOUND!");
			}
		}
		 */
		
		
		
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
		for(RankingConfig c: rankers) {
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
