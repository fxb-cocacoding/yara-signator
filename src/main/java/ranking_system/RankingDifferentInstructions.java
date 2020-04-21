package ranking_system;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import converters.ngrams.Ngram;
import smtx_handler.Instruction;

public class RankingDifferentInstructions extends Calculator {
	/*
	 * The more different instructions you have in your ngram,
	 * the better is your ranking
	 */
	public int calc(Ngram ngram, List<Ngram> ngramList) {
		//System.out.println("dif. instr.");
		Set<String> instructions = new HashSet<String>();
		
		for(int i=0; i<ngram.getNgramInstructions().size(); i++) {
			
			Instruction instruction = ngram.getNgramInstructions().get(i);
			if(instructions.contains(instruction)) {
				instructions.add(instruction.getMnemonics().get(0));
			}
		}
		
		return instructions.size();
	}
}
