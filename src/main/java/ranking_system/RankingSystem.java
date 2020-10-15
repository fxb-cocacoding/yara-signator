package ranking_system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converters.ngrams.Ngram;

public class RankingSystem {
	
	private static final Logger logger = LoggerFactory.getLogger(RankingSystem.class);
	
	private List<Ngram> ngramList;
	public List<Ngram> getNgramList() {
		return ngramList;
	}

	public void setNgramList(List<Ngram> ngramList) {
		this.ngramList = ngramList;
	}

	private RankingFactory rankingSystem = new RankingFactory();
	
	public RankingSystem(List<Ngram> ngrams) {
		this.ngramList = new ArrayList<>(ngrams);
	}
	
	public List<Ngram> rank(int candidatePoolSize, String function) {
		Calculator calc = rankingSystem.getRankingCalculator(function);
		
		logger.info("ranking with " + function + " the candidate pool of size: " + candidatePoolSize);
		
		//TODO unset the value to config.seed or something
		// We randomize to reduce the risk of getting overlapping ngrams although this is still not ideal
		randomize(new Random(1337));
		
		for(Ngram ngram : ngramList) {
			calc.calc(ngram, ngramList);
		}
		
		Collections.sort(ngramList, new Comparator<Ngram>() {
			@Override
			public int compare(Ngram o2, Ngram o1) {
				
				/*
				 * Generate for both compared ngrams a rank score and order them by this score.
				 */
				return Integer.compare(o1.score , o2.score);
			}
		});
		
		
		removeSlowSequences();

		
		//System.out.println("candidatePoolSize: " + candidatePoolSize + "  -  ngramList.size(): " + ngramList.size());
		if(ngramList.size() > candidatePoolSize) {
			//System.out.println(" [][] REMOVAL - size: " + ngramList.size() + "");
			int limit = ngramList.size();
			for(int i=ngramList.size()-1; i>candidatePoolSize-1; i--) {
				//System.out.println("i : " + i + " until " + (limit-1) + " realSize: " + ngramList.size());
				ngramList.remove(i);
			}
			//System.out.println("NEW SIZE: " + ngramList.size());
		}
		//System.out.println("ngramList size after removal: " + ngramList.size());
		return ngramList;
	}

	
	private void removeSlowSequences() {
		/*
		 * BugFix:
		 * Remove all Sequences (ngrams are n instructions with opcodes -> 1 sequence) where we have not at least three hex bytes in a row.
		 */
		for(int i=ngramList.size()-1; i>=0; i--) {
			Ngram n = ngramList.get(i);
			String opcodes = n.getOpcodes().replaceAll("\\s+","");
			String[] splitted = StringUtils.split(opcodes, '?');
			boolean remove = true;
			for(String s: splitted) {
				// Ensure that at least 6 chars are no '?' because otherwise the YARA Rule would slow down the scanner.
				if(s.length() >= 6) {
					remove = false;
					break;
				}
			}
			if(remove == true) {
				ngramList.remove(i);
			}
		}
	}
	
	public void randomize(Random rnd) {
		Collections.shuffle(ngramList, rnd);
	}	
}
