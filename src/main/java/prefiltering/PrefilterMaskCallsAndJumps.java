package prefiltering;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converters.ngrams.Ngram;
import smtx_handler.Instruction;

public class PrefilterMaskCallsAndJumps extends Prefilter {

	private static final Logger logger = LoggerFactory.getLogger(PrefilterMaskCallsAndJumps.class);
	
	@Override
	public Ngram calc(Ngram ngram) {
		Ngram ret = ngram;
		List<Instruction> ins = ret.getNgramInstructions();
		for(int i=0; i<ins.size(); i++) {
			String opcode = ins.get(i).getOpcodes();
			
			if(opcode.contains("?")) {
				continue;
			}
			
			int opcode_size = opcode.length();
			StringBuilder sb = new StringBuilder();
			sb.append(opcode.charAt(0)).append(opcode.charAt(1));
			
			String cmp = sb.toString().toUpperCase();
			boolean alreadyWildcarded = false;
			
			switch(cmp) {
				case "E8": //if we have more than 
				case "E9":
				case "EA":
				//case "EB":
					if(opcode_size > 2*5) {
						/*
						 * 
						 * seems wrong to have instructions like E8 AA AA AA AA XX,
						 * should not have more than 4 bytes after e8, do still
						 * the masking to filter all those mistakes with bad
						 * scoring away
						 * 
						 */
						// If you want to filter, do it in the ranking system!
						// And not in the wildcard system!
						// TODO: Maybe this is 64 bit?
						// If these opcodes are garbage write a garbage detector!
						//ret.score = ret.score - 50;
					}
					for(int j=2; j<opcode_size; j++) {
						sb.append('?');
					}
					alreadyWildcarded = true;
					break;
			}
			
			if(opcode.length() > 4 && alreadyWildcarded == false) {
				sb.append(opcode.charAt(2)).append(opcode.charAt(3));
				
				cmp = sb.toString().toUpperCase();
				switch(cmp) {
					case "FF15":
					case "FF25":
					//case "FF35":
						for(int j=4; j<opcode_size; j++) {
							sb.append('?');
						}
						alreadyWildcarded = true;
						break;
				}
			}
			
			
			// add the rest '?' chars:
			if(sb.toString().length()<opcode_size && alreadyWildcarded == false) {
				for(int j=sb.toString().length(); j<opcode_size; j++) {
					sb.append(opcode.charAt(j));
				}
			}
			
			long count = sb.toString().chars().filter(ch -> ch == '?').count();
			
			if( (sb.toString().length() & 1) != 0 || sb.toString().length() != opcode_size) {
				try {
					logger.error("Error!: this should never happen since it all should be even");
					logger.error(opcode.toString());
					logger.error(sb.toString());
					logger.error(ins.toString());
					logger.error(ngram.toString());
					logger.error("Returning the ngram simply.");
					throw new Exception("Wrong nibble wildcards - should never happen!");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			} else if((count & 1) != 0 ) {
				try {
					logger.error("We have uneven amount of '?' which is bad for performance and even probably wrong...");
					logger.error("Previous: " + opcode);
					logger.error("Now: " + sb.toString());
					throw new Exception("Potentially wrong amount of wildcards - should never happen!");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				continue;
			} else {
			
				if(opcode_size != sb.toString().length()) {
					try {
						logger.error("Returning the ngram simply.");
						throw new Exception("Length of the former opcodes differs now - should never happen!");
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
			}
			ins.get(i).setOpcodes(sb.toString());
		}
		return ngram;
	}

}
