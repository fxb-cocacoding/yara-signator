package iterative_improvement;

import java.util.List;
import java.util.Map;

import converters.ngrams.Ngram;
import main.Config;
import main.NextGenConfig;

public class IterativeImprovementRuleCollectionEntity {
	Map<String, List<Ngram>> currentSignatures;
	List<String> families;
	
	double precision;
	double recall;
	double f1;
	public NextGenConfig currentNGConfig;
	public Config config;
	
	@Override
	public String toString() {
		return "NextGenRuleCollectionEntity [currentSignatures=" + currentSignatures + ", families=" + families
				+ ", precision=" + precision + ", recall=" + recall + ", f1=" + f1 + ", currentNGConfig="
				+ currentNGConfig + ", config=" + config + "]";
	}
	
	
}
