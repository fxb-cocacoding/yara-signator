package ranking_system;

import java.util.List;

import converters.ngrams.Ngram;
import smtx_handler.Instruction;

public class RankingPrototype extends Calculator {

	@Override
	public int calc(Ngram ngram, List<Ngram> ngramList) {
		int specialInstructionCounter = 0;
		int addressCounter = 0;
		//final int addressBuster = 500;
		//final int specialInstructionBooster = 500;
		final int addressBuster = 100;
		final int specialInstructionBooster = 10;
		
		for(Instruction ins: ngram.getNgramInstructions()) {
			String asm = "";
			List<String> mnemonics = ins.getMnemonics();
			String s = mnemonics.get(0);
			if(s.equalsIgnoreCase("and") || s.equalsIgnoreCase("neg") || s.equalsIgnoreCase("or") || 
					s.equalsIgnoreCase("shl") || s.equalsIgnoreCase("xor") || s.equalsIgnoreCase("shr") ||
					s.equalsIgnoreCase("not") || s.equalsIgnoreCase("div") || s.equalsIgnoreCase("mul") || s.equalsIgnoreCase("rol") || s.equalsIgnoreCase("ror")) {
				ngram.score +=specialInstructionBooster;
			}
			asm = asm.concat(mnemonics.get(0) + ", " + mnemonics.get(1));
			
			if( (asm.contains(" ptr ") && ins.getAssemblySize() >= 5) || (asm.contains("call,") && ins.getAssemblySize() >= 5) || (asm.contains("lea,") && ins.getAssemblySize() >= 5) ) {
				/*
				 * instruction seems to has an address
				 */
				addressCounter++;
			}
		}
		
		ngram.score = ngram.score + (specialInstructionCounter * specialInstructionBooster) - (addressCounter * addressBuster);
		return ngram.score;
	}

}
