package prefiltering;

import java.util.List;
import java.util.Random;

import converters.ngrams.Ngram;
import main.Config;
import main.RankingConfig;
import main.WildcardConfig;
import ranking_system.RankingSystem;

public class PrefilterFacade {
	
	public List<Ngram> prefilterAction(List<Ngram> ngrams, Config config) {
		
		PrefilterSystem prefilter = new PrefilterSystem(ngrams);
		int prefilterCounter = 0;
		
		for(WildcardConfig c: config.getWildcardConfigConfig()) {
			ngrams = prefilter.process(c.wildcardOperator);
			
			if(prefilterCounter == 0) {
				//ngrams = rank.initMetaData(ngrams);
				prefilter.setNgramList(ngrams);
			}
			
			prefilterCounter++;
		}
		return ngrams;
	}
}
