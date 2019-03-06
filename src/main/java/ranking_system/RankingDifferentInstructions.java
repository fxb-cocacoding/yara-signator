package ranking_system;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import converters.ngrams.Ngram;

public class RankingDifferentInstructions extends Calculator {
	/*
	 * The more different instructions you have in your ngram,
	 * the better is your ranking
	 */
	public int calc(Ngram ngram, List<Ngram> ngramList) {
		//System.out.println("dif. instr.");
		Set<String> instructions = new HashSet<String>();
		
		for(int i=0; i<ngram.getNgramInstructions().size(); i++) {
			
			if(instructions.contains(ngram.getNgramInstructions().get(i))) {
				instructions.add(ngram.getNgramInstructions().get(i).getMnemonics().get(0));
			}
		}
		
		return instructions.size();
	}
}
