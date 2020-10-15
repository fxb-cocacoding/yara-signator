package iterative_improvement;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.Config;
import postgres.PostgresConnection;
import postgres.PostgresRequestUtils;

public enum IterativeImprovementDataContainer {
	INSTANCE;
	
	Config config;
	
	PostgresRequestUtils pr = new PostgresRequestUtils();
	
	public void setConfig(Config config) {
		this.config = config;
	}
	
	//public Map<String, List<Ngram>> currentSignatures;
	
	/*
	 * We use String instead of our Sequence class or N-Grams
	 * because this is a primitive which will be faster.
	 */
	private Set<String> blackListedSequenceCandidates = new HashSet<String>();
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

	public void addToBlackList(String s) throws SQLException {
		if(config.makeBlacklistPersistent) {
			pr.writeElementToBlacklist(s);
		} else {
			this.blackListedSequenceCandidates.add(s);
		}
	}
	
	public boolean isInBlacklist(String s) {
		if(config.makeBlacklistPersistent) {
			try {
				return pr.isStringAlreadyInBlacklist(s);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			return this.blackListedSequenceCandidates.contains(s);
		}
		return false;
	}

	public int getBlacklistSize() {
		if(config.makeBlacklistPersistent) {
			try {
				return pr.getBlacklistSize();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			return this.blackListedSequenceCandidates.size();
		}
		return -1;
	}
}
