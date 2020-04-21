package iterative_improvement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum IterativeImprovementDataContainer {
	INSTANCE;
	
	//public Map<String, List<Ngram>> currentSignatures;
	
	/*
	 * We use String instead of our Sequence class or N-Grams
	 * because this is a primitive which will be faster.
	 */
	public Set<String> blackListedSequenceCandidates = new HashSet<String>();
	public List<IterativeImprovementRuleCollectionEntity> candidates = new ArrayList<IterativeImprovementRuleCollectionEntity>();
	
	public IterativeImprovementRuleCollectionEntity getBestResultByF1() {
		double bestF1 = 0;
		IterativeImprovementRuleCollectionEntity bestCandidate = null;
		for(IterativeImprovementRuleCollectionEntity i: this.candidates) {
			if(i.f1 > bestF1) {
				bestCandidate = i;
				bestF1 = i.f1;
			}
		}
		return bestCandidate;
	}
	
	public IterativeImprovementRuleCollectionEntity getBestResultByPrecision() {
		double bestPrecision = 0;
		IterativeImprovementRuleCollectionEntity bestCandidate = null;
		for(IterativeImprovementRuleCollectionEntity i: this.candidates) {
			if(i.precision > bestPrecision) {
				bestCandidate = i;
				bestPrecision = i.precision;
			}
		}
		return bestCandidate;
	}
	
	public IterativeImprovementRuleCollectionEntity getBestResultByRecall() {
		double bestRecall = 0;
		IterativeImprovementRuleCollectionEntity bestCandidate = null;
		for(IterativeImprovementRuleCollectionEntity i: this.candidates) {
			if(i.recall > bestRecall) {
				bestCandidate = i;
				bestRecall = i.recall;
			}
		}
		return bestCandidate;
	}
}
