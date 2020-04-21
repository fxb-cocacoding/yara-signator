package prefiltering;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converters.ngrams.Ngram;
import smtx_handler.Instruction;



public class WildcardIntraJumps extends Prefilter {

	private static final Logger logger = LoggerFactory.getLogger(WildcardIntraJumps.class);
	
	/*
	 * Concept and Instruction Opcodes widely forked from python code from Daniel Plohmann.
	 */
		
	private static String escapeBinaryJumpCall(Instruction ins) {
        String clean_bytes = getByteWithoutPrefixes(ins).toLowerCase();
                
        /*
         * 7 masks 0x70 til 0x7F for jump if condition branch types, these are short jumps, maybe move them
         * 
         * I N T R A PROCEDURE CALLS BEGINNING WITH 7*!
         */
        if ((clean_bytes.indexOf("7") == 0) || 
        		(clean_bytes.indexOf("e0") == 0) || 
        		(clean_bytes.indexOf("e1") == 0) ||
        		(clean_bytes.indexOf("e2") == 0) ||
        		(clean_bytes.indexOf("e3") == 0) ||
        		(clean_bytes.indexOf("eb") == 0)) {
        	String return_bytes = "" + clean_bytes.charAt(0) + clean_bytes.charAt(1);
        	String bytes = "";
        	
        	for(int i=2;i<clean_bytes.length(); i++) {
        		bytes = bytes.concat("?");
        	}
        	
            return return_bytes + bytes;
        }
        
        return ins.getOpcodes();
	}

	
	
	private static String getByteWithoutPrefixes(Instruction ins) {
        boolean is_cleaning = true;
		String cleaned = "";
	
		HashSet<String> subset = new HashSet<>();
		subset.addAll(Arrays.asList("26", "2E", "36", "3E", "64", "65", "66", "67", "F2", "F3"));
		int i=0;
		
		while(i<ins.getOpcodes().length()-1) {
			//System.out.println("length: " + ins.getOpcodes().length());
			//System.out.println("i: " + i);
			String cmp_partial = "" + ins.getOpcodes().charAt(i) + ins.getOpcodes().charAt(i+1);
			
			if(is_cleaning && subset.contains(cmp_partial.toUpperCase())) {
				i = i + 2;
				continue;
			} else {
				is_cleaning = false;
				cleaned = cleaned + cmp_partial;
			}
			i = i + 2;
		}
        return cleaned;
	}
	
	@Override
	public Ngram calc(Ngram ngram) {
		Ngram ret = ngram;
		List<Instruction> ins = ret.getNgramInstructions();
		for(int i=0; i<ins.size(); i++) {
			String opcode = ins.get(i).getOpcodes();
			
			/*
			 * It should be not a problem since these methods are mutual exclusive and return in case they do not 'hit' just the regular opcodes.
			 */
			
			String new_opcode = escapeBinaryJumpCall(ins.get(i));
			
			int len = new_opcode.length();
			if((len & 1) != 0) {
				throw new UnsupportedOperationException("FATAL: sanity check failed: new opcode length is not a valid byte: " + new_opcode);
			}
			
			ins.get(i).setOpcodes(new_opcode);
			
		}
		ret.setNgramInstructions(ins);
		return ret;
	}

}