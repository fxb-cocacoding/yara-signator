package prefiltering;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import converters.ngrams.Ngram;
import smtx_handler.Instruction;

public class WildcardInterJumps extends Prefilter {

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
	

	private static String escapeBinaryJumpCall(Instruction ins) {
		
        String clean_bytes = getByteWithoutPrefixes(ins).toLowerCase();
		
        //BEGIN  I N T E R PROCEDURE CALLS  -> these should cover most cross-function references and absolute offsets
        if ((clean_bytes.indexOf("e8") == 0) || (clean_bytes.indexOf("e9") == 0)) {
        	
        	String return_bytes = "" + clean_bytes.charAt(0) + clean_bytes.charAt(1);
        	String bytes = "";
        	
        	for(int i=2;i<clean_bytes.length(); i++) {
        		bytes = bytes.concat("?");
        	}
        	
            return return_bytes + bytes;
        }
        if((clean_bytes.indexOf("0f8") == 0)) {
        	String return_bytes = "" + clean_bytes.charAt(0) + clean_bytes.charAt(1) + clean_bytes.charAt(2);
        	String bytes = "";
        	
        	for(int i=3;i<clean_bytes.length(); i++) {
        		bytes = bytes.concat("?");
        	}
        	
            return return_bytes + bytes;
        }
        if(clean_bytes.indexOf("ff") == 0) {
            if((clean_bytes.indexOf("ff14") == 0) || 
            		(clean_bytes.indexOf("ff15") == 0) || 
            		(clean_bytes.indexOf("ff24") == 0) || 
            		(clean_bytes.indexOf("ff25")== 0)) {
                //return clean_bytes.substring(0, clean_bytes.length() - 8) + "????????";
            	/*
            	if(clean_bytes.length() < 12) {
            		logger.debug("Instruction is very short, maybe you want to inspect this: " + ins.toString());
            	}
            	*/       	
            	String bytes = "" + clean_bytes.charAt(0) + clean_bytes.charAt(1) + clean_bytes.charAt(2) + clean_bytes.charAt(3);
            	for(int i=4;i<clean_bytes.length(); i++) {
            		bytes = bytes.concat("?");
            	}
            	        	
                return bytes;
            }
            if((clean_bytes.indexOf("ff9") == 0) ||
            		clean_bytes.indexOf("ffa") == 0) {
                // call dword ptr [<reg> + <offset>]
                return ins.getOpcodes();
            }
            if(clean_bytes.length() <= 8) {
                // these seem to be all relative or register based instructions and need no escaping
                return ins.getOpcodes();
            }
        }
        if((clean_bytes.indexOf("ea") == 0) ||
        		clean_bytes.indexOf("9a") == 0) {
        	/*
        	 * Errors: This will throw errors, for example on opcodes like this: 9a681a70b5 and 9a681a70b5, missing 4 characters.
        	 */
        	/*
        	if(clean_bytes.length() < 12) {
        		logger.debug("Instruction is very short, maybe you want to inspect this: " + ins.toString());
        	}
        	*/       	
        	String bytes = "" + clean_bytes.charAt(0) + clean_bytes.charAt(1);
        	for(int i=2;i<clean_bytes.length(); i++) {
        		bytes = bytes.concat("?");
        	}
        	        	
            return bytes;
        }
        return ins.getOpcodes();		
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
