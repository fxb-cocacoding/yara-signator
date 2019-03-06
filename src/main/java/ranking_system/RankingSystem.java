package ranking_system;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import javax.naming.OperationNotSupportedException;

import converters.ngrams.Ngram;
import mongodb.MongoHandler;

public class RankingSystem {
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
	
	public void randomize(Random rnd) {
		Collections.shuffle(ngramList, rnd);
	}
	
	@Deprecated
	public List<Ngram> initMetaData(List<Ngram> ngramsRO) {
		List<Ngram> ngrams = null;
		try {
			//old one:
			//ngrams = new MongoHandler().getInstruction(ngramsRO);
			//new one:
			ngrams = new MongoHandler().getInstructionWithoutLookupTable(ngramsRO);
			System.out.print("verbose: ");
			System.out.println(ngrams.toString());
		} catch (OperationNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(ngrams.get(ngrams.size()-1));
		return ngrams;
	}
	
}
