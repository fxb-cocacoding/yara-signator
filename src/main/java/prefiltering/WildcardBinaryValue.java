package prefiltering;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converters.ngrams.Ngram;
import smtx_handler.Instruction;
import utils.HexTools;

public class WildcardBinaryValue extends Prefilter {

	private static final Logger logger = LoggerFactory.getLogger(WildcardBinaryValue.class);
	
	/*
	 * Concept and Instruction Opcodes widely forked from python code from Daniel Plohmann.
	 */
	
	private static String escapeBinaryValue(Instruction ins, long lowerAddressLimit, long higherAddressLimit) {
		
		/*
		 * We break if we have not met our conditions for this marker:
		 * 
		 * there must be operands (no single operation like 'ret' is allowed)
		 * and it must start with 0x or has 0x as n-th argument, after a ',' character
		 */
		if(ins.getMnemonics().size() < 2 || ins.getOpcodes().contains("?")) {
			return ins.getOpcodes();
		}
		if(! (ins.getMnemonics().get(1).startsWith("0x") || ins.getMnemonics().get(1).contains(", 0x"))) {
			return ins.getOpcodes();
		}
		
		String operands = ins.getMnemonics().get(1);
		
		String regex = "(?<dwordoffset>0x[a-fA-F0-9]+)";
		
		String escaped_sequence = ins.getOpcodes();
		StringBuilder ret = new StringBuilder();
		
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(ins.getMnemonics().get(1));
		
		if(matcher.find()) {
			
			String dword_offset = matcher.group("dwordoffset").substring(2);
			long potential_address = 0;
			try {
				potential_address = Long.parseLong(dword_offset, 16);
			} catch (NumberFormatException e) {
				logger.warn("Detected NumberFormatException for " + dword_offset + " at instruction: " + ins.toString());
				logger.warn(e.getLocalizedMessage());
				logger.warn("We continue and skip this address.");
				if(!dword_offset.equalsIgnoreCase("ffffffffffffffff")) {
					logger.warn("Trigger for non-ffffffffffffffff string: " + dword_offset + " - " + ins.toString());
				}
				return ins.getOpcodes();
			}
			
			// If the address is not between them:
			if(! (potential_address >= lowerAddressLimit && potential_address <= higherAddressLimit)) {
				return ins.getOpcodes();
			} else {
				/*
				 * logger.info("we continue to filter with the new special filter - this instruction " + ins.toString() + " " 
				 * + Long.toHexString(lowerAddressLimit) + " / " + Long.toHexString(higherAddressLimit));
				 * 
				 */
			}
			/*
			 * We assume that the length in every opcode for the dword pointer is exactly 4 bytes aka 8 letters:
			 * 
			 * len(dword_offset) should be 8, if smaller, then there are some leading zeros left out:
			 */
			final int len = 8;
			
			if(dword_offset.length() < len) {
				int fill = len - dword_offset.length();
				StringBuilder sb = new StringBuilder();
				
				for(int i = 0; i< fill; i++) {
					sb.append("0");
				}
				
				sb.append(dword_offset);
				dword_offset = sb.toString();
			}
			
			String swapped = HexTools.swapHexBytes(dword_offset);
			
			/*
			 * Now we have in 'swapped' our opcode representation of the respective bytes.
			 * The length for this is correct now. We may (!) have following bytes, so only overwrite the first one: c7 05 ?? .. ?? aa bb
			 */
			
			int count = StringUtils.countMatches(escaped_sequence, swapped);
			
			if(count < 1) {
				//throw new UnsupportedOperationException("count was below 1!");
				//logger.info("Not affected, count was: " + count + " for: " + ins.toString());
				return ins.getOpcodes();
			} else if (count == 1) {
				//this is the estimated case

				int start = escaped_sequence.indexOf(swapped);
				
				for(int i=0; i<start; i++) {
					ret.append(escaped_sequence.charAt(i));
				}
				
				for(int i=0; i<len; i++) {
					ret.append('?');
				}
				
				int temporalSizeStringBuilder = ret.length();
				
				for(int i=temporalSizeStringBuilder; i<escaped_sequence.length(); i++) {
					ret.append(escaped_sequence.charAt(i));
				}

				return ret.toString();
			} else if (count == 2) {
				//expected, but rare:
				
				int start = escaped_sequence.indexOf(swapped);
				
				for(int i=0; i<start; i++) {
					ret.append(escaped_sequence.charAt(i));
				}
				
				for(int i=0; i<len; i++) {
					ret.append('?');
				}
				
				int temporalSizeStringBuilder = ret.length();
				
				for(int i=temporalSizeStringBuilder; i<escaped_sequence.length(); i++) {
					ret.append(escaped_sequence.charAt(i));
				}
				
				// We replace the second occurence
				start = ret.indexOf(swapped);
				ret.replace(start, swapped.length(), swapped);
				
				// Since this case is so special we log them all:
				
				logger.info("special case during filtering: " + ret.toString() + " was derived from: " + escaped_sequence + " from: " + ins.toString());
				return ret.toString();
				
			} else {
				// > 2 , should not happen
				throw new UnsupportedOperationException("count was larger than 2!");
			}
			
			
		} else {
			logger.warn("This point should never be reached");
			return ins.getOpcodes();
		}
		
	}

	
	public Ngram calc(Ngram ngram) {
		Ngram ret = ngram;
		List<Instruction> ins = ret.getNgramInstructions();
		for(int i=0; i<ins.size(); i++) {
			String opcode = ins.get(i).getOpcodes();
			
			/*
			 * It should be not a problem since these methods are mutual exclusive and return in case they do not 'hit' just the regular opcodes.
			 */
			String new_opcode = escapeBinaryValue(ins.get(i), ngram.lowerAddressLimit, ngram.higherAddressLimit);
		
						
			if((new_opcode.length() & 1) != 0) {
				logger.error("FATAL: sanity check failed: new opcode length is not a valid byte! new: " 
						+ new_opcode + " - old: " + opcode);
				throw new UnsupportedOperationException("FATAL: sanity check failed: new opcode length is not a valid byte! new: " 
						+ new_opcode + " - old: " + opcode);
			}
			
			if(new_opcode.length() != opcode.length()) {
				logger.error("FATAL: sanity check failed: new opcode length differs from the old one! new: " 
						+ new_opcode + " - old: " + opcode);
				throw new UnsupportedOperationException("FATAL: sanity check failed: new opcode length differs from the old one! new: " 
						+ new_opcode + " - old: " + opcode);
			}
			
			ins.get(i).setOpcodes(new_opcode);
			
		}
		ret.setNgramInstructions(ins);
		return ret;

	}

}
