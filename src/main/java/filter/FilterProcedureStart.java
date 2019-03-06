package filter;

import java.util.Iterator;
import java.util.List;

import converters.ngrams.Ngram;
import smtx_handler.Instruction;

public class FilterProcedureStart {
	public synchronized void apply(List<Ngram> ngrams) {
		//System.out.println("before: " + ngrams.size());
		int counter = 0;
		Iterator<Ngram> iter = ngrams.iterator();
		while(iter.hasNext()) {
			Ngram ngram = iter.next();
			List<Instruction> instr = ngram.getNgramInstructions();
			assert(instr.size() == ngram.n);
			
			if(instr.size() < 2) {
				iter.remove();
				continue;
			}
			/*
			 * remove all 
			 */
			if(! (instr.get(0).getOpcodes().equalsIgnoreCase("55") && instr.get(1).getOpcodes().equalsIgnoreCase("8bec")) ) {
				iter.remove();
			}
		}
		//System.out.println("after: " + ngrams.size());
		//return ngrams;
	}
}
