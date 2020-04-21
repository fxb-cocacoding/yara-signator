package prefiltering;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converters.ngrams.Ngram;
import smtx_handler.Instruction;
import utils.HexTools;

public class WildcardDataRefs extends Prefilter {

	private static final Logger logger = LoggerFactory.getLogger(WildcardDataRefs.class);
	
	/*
	 * Concept and Instruction Opcodes widely forked from python code from Daniel Plohmann.
	 */
	
	private static String escapeBinaryPtrRef(Instruction ins) {
		String regex = "ptr \\[(?<dwordoffset>0x[a-fA-F0-9]+)\\]";
		
		// regex64 should also be able to cover 32 bit cases.
		// I will keep that in an OR in the following if/else to be sure that at least the 32-one works properly
		String regex64 = "ptr \\[(rip \\+ )?(?<dwordoffset>0x[a-fA-F0-9]+)\\]";
		
		String escaped_sequence = ins.getOpcodes();
		StringBuilder ret = new StringBuilder();
		
		Pattern pattern = Pattern.compile(regex);
		Pattern pattern64 = Pattern.compile(regex64);
		
		Matcher matcher = pattern.matcher(ins.getMnemonics().get(1));
		Matcher matcher64 = pattern64.matcher(ins.getMnemonics().get(1));
		

		
		if(matcher.find()) {
						
			String dword_offset = matcher.group("dwordoffset").substring(2);

			
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
			
		} else if(matcher64.find()) {
			
			String dword_offset = matcher64.group("dwordoffset").substring(2);

			
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
			
		} else {
			
			return ins.getOpcodes();
		}
		
		return ret.toString();
	}

	
	@Override
	public Ngram calc(Ngram ngram) {
		Ngram ret = ngram;
		List<Instruction> ins = ret.getNgramInstructions();
		for(int i=0; i<ins.size(); i++) {
			String opcode = ins.get(i).getOpcodes();
			if(opcode.contains("?")) {
				continue;
			}
			/*
			 * It should be not a problem since these methods are mutual exclusive and return in case they do not 'hit' just the regular opcodes.
			 */
			String new_opcode = escapeBinaryPtrRef(ins.get(i));
			
			if((new_opcode.length() & 1) != 0) {
				throw new UnsupportedOperationException("FATAL: sanity check failed: new opcode length is not a valid byte! new: " 
						+ new_opcode + " - old: " + opcode);
			}
			
			if(new_opcode.length() != opcode.length()) {
				throw new UnsupportedOperationException("FATAL: sanity check failed: new opcode length differs from the old one! new: " 
						+ new_opcode + " - old: " + opcode);
			}
			
			ins.get(i).setOpcodes(new_opcode);
			
		}
		ret.setNgramInstructions(ins);
		return ret;

	}

}
