package prefiltering;

import java.util.List;
import converters.ngrams.Ngram;
import main.Config;
import main.WildcardConfig;

public class PrefilterFacade {
	
	public List<Ngram> prefilterAction(List<Ngram> ngrams, List<WildcardConfig> config) {
		
		PrefilterSystem prefilter = new PrefilterSystem(ngrams);
		int prefilterCounter = 0;
		
		for(WildcardConfig c: config) {
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
